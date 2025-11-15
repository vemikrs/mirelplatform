# アーキテクチャ分析レポート - ステンシル設定ロードフローと親マージ問題

## エグゼクティブサマリー

ProMarkerのステンシル設定ロードシステムを詳細に分析した結果、**2つの独立したロード経路**が存在し、それぞれ異なる目的を持つことが判明しました:

1. **ReloadStencilMaster API経路**: YAMLファイル → **データベース**（メタデータのみ）
2. **Suggest/Generate API経路**: YAMLファイル → **メモリ内オブジェクト** → フロントエンドレスポンス

**現在の問題**: 親ステンシル設定のマージ機能を経路1に実装しましたが、**経路2では全く呼び出されていない**ため、Suggest APIでパラメータが0個のまま表示されています。

## 1. システム全体概要

### 1.1 コンポーネント構成図

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React)                          │
│  ProMarkerPage.tsx → StencilSelector + ParameterFields          │
└─────────────────┬───────────────────────────────────────────────┘
                  │ HTTP Requests
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend REST APIs                             │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ ReloadStencil    │  │ Suggest API      │                     │
│  │ Master API       │  │ Generate API     │                     │
│  └────────┬─────────┘  └────────┬─────────┘                     │
└───────────┼────────────────────────────────────────────────────┘
            │                      │
            ▼                      ▼
   ┌──────────────────┐   ┌──────────────────────┐
   │ ReloadStencil    │   │ TemplateEngine       │
   │ MasterServiceImp │   │ Processor            │
   └────────┬─────────┘   └────────┬─────────────┘
            │                      │
            ▼                      ▼
   ┌──────────────────┐   ┌──────────────────────┐
   │ MSTE_STENCIL     │   │ YAML Files           │
   │ (Database)       │   │ (Filesystem/         │
   │                  │   │  Classpath)          │
   │ - stencilCd      │   │                      │
   │ - stencilName    │   │ StencilSettingsYml   │
   │ - itemKind       │   │ ↓                    │
   │ - sort           │   │ SuggestResult        │
   │                  │   │ ↓                    │
   │ ❌ NO payload    │   │ Frontend Response    │
   └──────────────────┘   └──────────────────────┘
```

### 1.2 データフロー比較

#### 経路1: ReloadStencilMaster (マージ動作中 ✅)

```
YAML Files (user/standard/samples)
  ↓ 
ReloadStencilMasterServiceImp.collectStencilSettingsFromLayers()
  ↓ StorageConfig.getUserStencilDir() / getStandardStencilDir() / getSamplesStencilDir()
  ↓ ファイルシステム検索 → File一覧取得
  ↓
ReloadStencilMasterServiceImp.readYaml(File)
  ↓ new Yaml().loadAs(stream, StencilSettingsYml.class)
  ↓ ✅ TemplateEngineProcessor.mergeParentStencilSettings(resource, settings)
  ↓   → 親ディレクトリの *_stencil-settings.yml を検索・マージ
  ↓   → childSettings.appendDataElementSublist(parentSettings.getStencil().getDataDomain())
  ↓
StencilSettingsYml (dataElement + dataDomain マージ済み)
  ↓
MsteStencilエンティティ作成
  ↓ entry.setStencilCd(config.getId())
  ↓ entry.setStencilName(config.getName())
  ↓ entry.setItemKind("1")
  ↓ ❌ payload カラムなし → マージ結果は保存されない
  ↓
stencilRepository.save(entry)
  ↓
MSTE_STENCIL テーブル (メタデータのみ)
```

**結果**: マージは成功するが、データベースにはメタデータ（ID、名前）のみ保存され、**dataDomain情報は破棄される**。

#### 経路2: Suggest API (マージ未動作 ❌)

```
Frontend: POST /mapi/apps/mste/api/suggest
  ↓ {"content":{"stencilCategoy":"*","stencilCanonicalName":"/user/project/module_service","serialNo":"201221A"}}
  ↓
ApiController.index() → SuggestApi.service()
  ↓
SuggestServiceImp.invoke()
  ↓ TemplateEngineProcessor engine = TemplateEngineProcessor.create(...)
  ↓
TemplateEngineProcessor.getStencilSettings()
  ↓
findStencilSettingsInLayers()
  ↓ String[] searchLayers = {
  ↓   StorageConfig.getUserStencilDir(),      // "./data/storage/apps/promarker/stencil/user"
  ↓   StorageConfig.getStandardStencilDir(),  // "./data/storage/apps/promarker/stencil/standard"
  ↓   StorageConfig.getSamplesStencilDir()    // "classpath:stencil-samples"
  ↓ }
  ↓
findStencilSettingsInLayer(layerDir)
  ↓ if (layerDir.startsWith("classpath:"))
  ↓   → findStencilSettingsInClasspath(layerDir)
  ↓       ↓ resourcePatternResolver.getResources(searchPattern)
  ↓       ↓ loadStencilSettingsFromResource(resource)
  ↓       ↓   ↓ ✅ mergeParentStencilSettings(resource, settings) 呼び出し追加済み
  ↓       ↓   ↓   BUT: classpathリソースには親ディレクトリがない
  ↓       ↓   ↓   → resource.getURI().toString() = "jar:file:/path/to.jar!/stencil/..."
  ↓       ↓   ↓   → currentDir = jarファイル内パス（ファイルシステム外）
  ↓       ↓   ↓   → whileループで親検索しても見つからない
  ↓ else
  ↓   → findStencilSettingsInFileSystem(layerDir)
  ↓       ↓ File settingsFile = new File(layerDir + stencilCanonicalName + "/" + serialNo + "/stencil-settings.yml")
  ↓       ↓   例: "./data/storage/apps/promarker/stencil/user/project/module_service/201221A/stencil-settings.yml"
  ↓       ↓ getSsYmlRecurive(settingsFile)
  ↓       ↓   ↓ yaml.loadAs(stream, StencilSettingsYml.class)
  ↓       ↓   ↓ ✅ mergeParentStencilSettings(resource, settings) 呼び出し追加済み
  ↓       ↓   ↓   BUT: ❌❌❌ このメソッドが全く呼ばれていない ❌❌❌
  ↓       ↓   ↓
  ↓       ↓   return StencilSettingsYml (dataElement のみ、dataDomain なし)
  ↓
StencilSettingsYml settingsYaml = engine.getStencilSettings()
  ↓
resultModel.params = itemsToNode(settingsYaml)
  ↓ mergeStencilDeAndDd(
  ↓   settings.getStencil().getDataElement(),  // [{"id":"appId","value":"pas"}]
  ↓   settings.getStencil().getDataDomain()    // null または []
  ↓ )
  ↓ → dataDomainがないため、型定義・説明・placeholderなどが欠落
  ↓
resultModel.params.childs.length = dataElement.length (値のみ、型定義なし)
  ↓
Frontend: パラメータは表示されるが、入力フィールドが生成されない
```

**結果**: マージが呼ばれず、`dataDomain`が空のまま → パラメータ入力フィールドが0個。

## 2. 根本的な問題（解決済み ✅）

### 2.1 問題1: ストレージディレクトリのパス不整合 🔴 **CRITICAL**

**発見された事実**:

1. **application-dev.yml設定**:
   ```yaml
   mirel:
     storage-dir: ./data/storage
   ```

2. **実際のファイル配置**:
   ```bash
   ./backend/data/storage/apps/promarker/stencil/user/project/project_stencil-settings.yml
   ./backend/data/storage/apps/promarker/stencil/user/project/module_service/201221A/stencil-settings.yml
   ```

3. **ワークスペースルートディレクトリ確認**:
   ```bash
   $ pwd
   /home/nimaz/dev/mirelplatform
   
   $ ls -la data/
   data directory not found in workspace root
   
   $ ls -la backend/data/
   drwxr-xr-x  storage/
   ```

**問題の原因**:
- Spring Bootアプリケーションは`./backend`ディレクトリで実行される
- 相対パス`./data/storage`は`./backend/data/storage`として解決される
- しかし、設定ファイルでは`./data/storage`と記述されている
- 実際には`./backend/data/storage`にファイルが存在するため、アクセス可能

**StorageConfig.getUserStencilDir()の実際の動作**:
```java
// application-dev.yml
mirel.storage-dir: ./data/storage

// StorageConfigで処理
configuredUserStencilDir = userStencilDir.replace("${mirel.storage-dir}", storageDir);
// → "./data/storage/apps/promarker/stencil/user"

// Spring Bootの作業ディレクトリ: ./backend
// 実際に解決されるパス: ./backend/data/storage/apps/promarker/stencil/user
// ✅ ファイルが存在するため正常動作
```

**結論**: ストレージパスは**正常に動作している**。問題は別の箇所にある。

### 2.2 問題2: メソッドが呼ばれていない（本質的な問題）

**デバッグログの証拠**:
- `/tmp/find-layer.log` - 作成されない
- `/tmp/find-fs.log` - 作成されない  
- `/tmp/get-ss-yml.log` - 作成されない
- `/tmp/merge-parent.log` - ReloadStencilMaster経由のみ作成される

**原因の可能性**:
1. ✅ ~~`StorageConfig.getUserStencilDir()` が予期しない値~~ → 確認済み、正常動作
2. ❓ `findStencilSettingsInFileSystem()` がそもそも実行されていない
3. ❓ `context.getStencilCanonicalName()` が正しく設定されていない
4. ❓ **SuggestServiceImpでTemplateEngineProcessorが作成されていない**

**次の調査対象**: SuggestServiceImpでの`TemplateEngineProcessor.create()`呼び出しとcontext設定

### 2.3 問題3: データベース設計の制約

**MsteStencilエンティティ**:
```java
@Entity
@Table(name = "mste_stencil")
public class MsteStencil {
    @Id
    @Column
    public String stencilCd;      // ステンシルID
    
    @Column
    public String stencilName;    // ステンシル名
    
    @Column
    public String itemKind;       // 項目種類 (0: カテゴリ, 1: ステンシル)
    
    @Column
    public Integer sort;          // ソート順
    
    // ❌ payload カラムがない
    // → StencilSettingsYmlの全情報（dataElement, dataDomain, config詳細）を保存できない
}
```

**影響**:
- ReloadStencilMaster APIでマージした結果をデータベースに保存できない
- Suggest APIはデータベースからステンシル情報を取得**しない** → 直接YAMLファイルを読む
- データベースの役割: ドロップダウンのカテゴリ・ステンシル一覧のみ

### 2.3 問題3: マージロジックの重複実装

**同じマージロジックが3箇所に存在**:

1. `TemplateEngineProcessor.mergeParentStencilSettings()` (公開メソッド)
   - ReloadStencilMasterServiceImpから呼び出される ✅
   - loadStencilSettingsFromResource()から呼び出される ✅ (但しclasspathでは無効)
   - getSsYmlRecurive()から呼び出される ✅ (但し未実行)

2. `StencilSettingsYml.appendDataElementSublist()` (インスタンスメソッド)
   - `mergeMapList(this.stencil.dataDomain, dataDomains)` を呼び出す
   - TemplateEngineProcessorから間接的に呼ばれる

3. `SuggestServiceImp.mergeStencilDeAndDd()` (静的メソッド)
   - dataElement (子の値) と dataDomain (親の定義) をマージ
   - itemsToNode()から呼ばれる

**問題**: 
- マージのタイミングが統一されていない
- TemplateEngineProcessorでマージしても、SuggestServiceでも再度マージする
- 責任境界が曖昧

## 3. 設計の矛盾点

### 3.1 データベースの役割の曖昧さ

**現状の役割**:
- ✅ カテゴリ一覧の提供 (itemKind = "0")
- ✅ ステンシル一覧の提供 (itemKind = "1")
- ✅ ソート順の管理

**期待される役割（実装されていない）**:
- ❌ ステンシル設定の永続化
- ❌ マージ済みdataDomainの保存
- ❌ キャッシュ機能

**結果**: データベースは「インデックス」としてのみ機能し、実際のステンシル設定は常にYAMLファイルから直接ロードされる。

### 3.2 レイヤー検索の優先順位

**TemplateEngineProcessor.findStencilSettingsInLayers()**:
```java
String[] searchLayers = {
    StorageConfig.getUserStencilDir(),      // 優先度: 高
    StorageConfig.getStandardStencilDir(),  // 優先度: 中
    StorageConfig.getSamplesStencilDir()    // 優先度: 低
};
```

**問題**:
- `getSamplesStencilDir()` は `"classpath:stencil-samples"` を返す
- classpathリソースは**親ディレクトリ検索が不可能**（JARファイル内）
- samplesレイヤーにステンシルが見つかった場合、親マージは**常に失敗する**

**影響範囲**:
- `/samples/**` 配下のステンシル: 親マージ不可
- `/user/**` および `/standard/**`: 親マージ可能（但し現在未動作）

### 3.3 StorageConfig実装確認完了 ✅

**StorageConfig.java** の実装内容を確認しました:

```java
@Component
public class StorageConfig {
    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;
    
    @Value("${mirel.promarker.stencil.user:${mirel.storage-dir}/apps/promarker}/stencil/user")
    private String userStencilDir;
    
    @Value("${mirel.promarker.stencil.standard:${mirel.storage-dir}/apps/promarker}/stencil/standard")
    private String standardStencilDir;
    
    @Value("${mirel.promarker.stencil.samples:classpath:/promarker/stencil/samples}")
    private String samplesStencilDir;
    
    @PostConstruct
    public void init() {
        configuredStorageDir = storageDir;
        configuredUserStencilDir = userStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredStandardStencilDir = standardStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredSamplesStencilDir = samplesStencilDir;
    }
    
    public static String getUserStencilDir() {
        if (configuredUserStencilDir == null) {
            return getStorageDir() + "/user";
        }
        return configuredUserStencilDir;
    }
    
    public static String getStandardStencilDir() {
        if (configuredStandardStencilDir == null) {
            return getStorageDir() + "/standard";
        }
        return configuredStandardStencilDir;
    }
    
    public static String getSamplesStencilDir() {
        if (configuredSamplesStencilDir == null) {
            return "classpath:/promarker/stencil/samples";
        }
        return configuredSamplesStencilDir;
    }
}
```

**実際の戻り値（application.yml設定なしの場合）**:
```
getUserStencilDir()     → "./data/storage/apps/promarker/stencil/user"
getStandardStencilDir() → "./data/storage/apps/promarker/stencil/standard"
getSamplesStencilDir()  → "classpath:/promarker/stencil/samples"
```

**重要な発見**:

1. **パスが正しい**: ファイルシステムパスとclasspathプレフィックスが期待通り
2. **@PostConstruct実行順序**: StorageConfigのinitが実行されるタイミング
3. **classpathプレフィックス**: `/promarker/stencil/samples` ではなく `classpath:/promarker/stencil/samples`

**推測される問題**:
- `findStencilSettingsInLayer()` の `layerDir.startsWith("classpath:")` 判定は正しく動作するはず
- 問題は別の箇所にある可能性が高い:
  - `context.getStencilCanonicalName()` が `/user/project/module_service` になっているか？
  - `findStencilSettingsInFileSystem()` の `settingsFile` パス構築が正しいか？

## 4. 推奨される設計改善案

### 4.1 オプション1: TemplateEngineProcessor中心設計（推奨 ⭐）

**コンセプト**: ステンシル設定のロード・マージを`TemplateEngineProcessor`に一元化し、全API経路で共通利用する。

**変更点**:

1. **TemplateEngineProcessor.getStencilSettings()の強化**
   ```java
   public StencilSettingsYml getStencilSettings() {
       // レイヤード検索
       StencilSettingsYml settings = findStencilSettingsInLayers();
       
       // ✅ マージは常にここで実行（classpathでもfilesystemでも）
       if (settings != null) {
           mergeParentStencilSettingsUnified(settings);
       }
       
       return settings;
   }
   
   /**
    * 統一された親マージロジック
    * filesystem/classpath両対応
    */
   private void mergeParentStencilSettingsUnified(StencilSettingsYml childSettings) {
       String stencilCanonicalName = context.getStencilCanonicalName();
       String[] pathSegments = stencilCanonicalName.split("/");
       
       // 親階層を逆順に検索
       for (int i = pathSegments.length - 1; i >= 1; i--) {
           String parentPath = String.join("/", Arrays.copyOfRange(pathSegments, 0, i));
           StencilSettingsYml parentSettings = findParentSettings(parentPath);
           
           if (parentSettings != null && parentSettings.getStencil().getDataDomain() != null) {
               childSettings.appendDataElementSublist(parentSettings.getStencil().getDataDomain());
           }
       }
   }
   ```

2. **SuggestServiceImp.itemsToNode()の簡素化**
   ```java
   protected static Node itemsToNode(StencilSettingsYml settings) {
       Node root = new RootNode();
       
       if (settings == null || settings.getStencil() == null) {
           return root;
       }
       
       // ✅ マージはTemplateEngineProcessorで完了済み
       // ✅ ここではdataDomainをそのまま使う
       List<Map<String, Object>> elems = settings.getStencil().getDataDomain();
       
       if (elems != null) {
           elems.forEach(entry -> {
               root.addChild(convertItemToNodeItem(entry));
           });
       }
       
       return root;
   }
   ```

3. **ReloadStencilMasterServiceImp.readYaml()からマージ呼び出しを削除**
   ```java
   protected StencilSettingsYml readYaml(File file) {
       // ✅ マージは削除（TemplateEngineProcessorに一元化）
       try (InputStream stream = new FileInputStream(file)) {
           LoaderOptions loaderOptions = new LoaderOptions();
           Yaml yaml = new Yaml(loaderOptions);
           return yaml.loadAs(stream, StencilSettingsYml.class);
       } catch (Exception e) {
           // ...
       }
   }
   ```

**メリット**:
- ✅ マージロジックが1箇所に集約
- ✅ ReloadStencilMaster / Suggest / Generate 全APIで一貫性
- ✅ デバッグが容易
- ✅ 既存のResourcePatternResolver活用

**デメリット**:
- ⚠️ classpathリソースの親検索は依然として制限あり
  - 解決策: samplesレイヤーのステンシルは親マージを使わない設計とする

### 4.2 オプション2: データベース拡張設計

**コンセプト**: `MSTE_STENCIL`テーブルに`payload`カラムを追加し、マージ済み設定をキャッシュする。

**スキーマ変更**:
```sql
ALTER TABLE mste_stencil ADD COLUMN payload TEXT;
-- payload: StencilSettingsYmlのJSON/YAML文字列（マージ済み）
```

**変更点**:

1. **ReloadStencilMaster経路で完全なYAMLを保存**
   ```java
   MsteStencil entry = new MsteStencil();
   entry.setStencilCd(config.getId());
   entry.setStencilName(config.getName());
   entry.setItemKind("1");
   entry.setSort(0);
   
   // ✅ マージ済みStencilSettingsYmlをJSON化して保存
   ObjectMapper mapper = new ObjectMapper();
   String payload = mapper.writeValueAsString(mergedSettings);
   entry.setPayload(payload);
   
   stencilRepository.save(entry);
   ```

2. **Suggest API経路でデータベースから取得**
   ```java
   // データベースから取得
   MsteStencil stencilEntity = stencilRepository.findById(stencilCd).orElse(null);
   
   if (stencilEntity != null && stencilEntity.getPayload() != null) {
       // ✅ キャッシュヒット: payloadからデシリアライズ
       ObjectMapper mapper = new ObjectMapper();
       settingsYaml = mapper.readValue(stencilEntity.getPayload(), StencilSettingsYml.class);
   } else {
       // ❌ キャッシュミス: YAMLファイルから直接ロード
       settingsYaml = engine.getStencilSettings();
   }
   ```

**メリット**:
- ✅ パフォーマンス向上（YAMLファイルアクセス削減）
- ✅ ReloadStencilMasterでマージを1回実行すれば、全API経路で利用可能
- ✅ データベースの役割が明確化（キャッシュ）

**デメリット**:
- ❌ スキーマ変更が必要（マイグレーション）
- ❌ payload文字列のサイズ制限（TEXT型の上限）
- ❌ データベースとYAMLファイルの同期管理が必要
- ❌ ReloadStencilMasterを実行しないとpayloadが空

### 4.3 オプション3: キャッシュレイヤー追加設計

**コンセプト**: アプリケーションメモリ内にマージ済みステンシル設定をキャッシュし、データベースは変更しない。

**変更点**:

1. **StencilSettingsCacheサービス作成**
   ```java
   @Service
   public class StencilSettingsCache {
       private final Map<String, StencilSettingsYml> cache = new ConcurrentHashMap<>();
       
       @Autowired
       private TemplateEngineProcessor templateEngineProcessor;
       
       public StencilSettingsYml get(String stencilCd, String serialNo) {
           String cacheKey = stencilCd + ":" + serialNo;
           
           return cache.computeIfAbsent(cacheKey, key -> {
               // キャッシュミス: TemplateEngineProcessorでロード&マージ
               SteContext context = SteContext.standard(stencilCd, serialNo);
               TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);
               return processor.getStencilSettings(); // マージ済み
           });
       }
       
       public void clear() {
           cache.clear();
       }
   }
   ```

2. **SuggestServiceImpでキャッシュ利用**
   ```java
   @Autowired
   private StencilSettingsCache stencilCache;
   
   @Override
   public ApiResponse<SuggestResult> invoke(ApiRequest<SuggestParameter> parameter) {
       // ...
       
       // ✅ キャッシュから取得（初回はTemplateEngineProcessorで自動ロード&マージ）
       StencilSettingsYml settingsYaml = stencilCache.get(stencilCd, serialNo);
       
       resultModel.params = itemsToNode(settingsYaml);
       // ...
   }
   ```

3. **ReloadStencilMaster実行時にキャッシュクリア**
   ```java
   @Override
   public ApiResponse<ReloadStencilMasterResult> invoke(...) {
       stencilCache.clear(); // ✅ キャッシュを無効化
       read(); // データベース更新
       // ...
   }
   ```

**メリット**:
- ✅ データベーススキーマ変更不要
- ✅ パフォーマンス向上（2回目以降のアクセスで高速化）
- ✅ マージロジックはTemplateEngineProcessorに一元化
- ✅ ReloadStencilMaster実行でキャッシュ再構築

**デメリット**:
- ⚠️ アプリケーション再起動でキャッシュ消失（初回アクセスが遅い）
- ⚠️ メモリ使用量増加（多数のステンシルがある場合）
- ⚠️ 分散環境ではキャッシュ同期が課題

## 5. 優先推奨アプローチ

### 5.1 最優先: オプション1の実装

**理由**:
- データベーススキーマ変更不要
- 既存のTemplateEngineProcessor活用
- 設計がシンプル
- 全API経路で一貫性

**実装ステップ**:

1. **StorageConfig実装確認**
   ```bash
   grep -n "getUserStencilDir\|getStandardStencilDir\|getSamplesStencilDir" backend/src/main/java/jp/vemi/framework/config/StorageConfig.java
   ```

2. **findStencilSettingsInFileSystem()のデバッグ強化**
   - なぜ呼ばれないのかを特定
   - StorageConfigの戻り値をログ出力

3. **mergeParentStencilSettings()の改善**
   - classpathリソースでも動作するように修正
   - または samplesレイヤーでは親マージをスキップする明示的なロジック追加

4. **SuggestServiceImp.itemsToNode()の簡素化**
   - `mergeStencilDeAndDd(dataElement, dataDomain)` 削除
   - マージ済みdataDomainをそのまま使用

5. **ReloadStencilMasterからマージ呼び出し削除**
   - 重複実装の削減

### 5.2 次善策: オプション3（キャッシュ）の追加

オプション1実装後、パフォーマンス改善が必要な場合に実装。

### 5.3 長期的検討: オプション2（データベース拡張）

将来的な機能拡張（ウェブUI経由でのステンシル編集など）を見据えた場合に検討。

## 6. 次のアクション

### 6.1 即座に実施すべきこと

1. **StorageConfigの実装確認**
   - `getUserStencilDir()`, `getStandardStencilDir()`, `getSamplesStencilDir()` の戻り値
   - ファイルシステムパスか、classpathプレフィックスか

2. **findStencilSettingsInFileSystem()が呼ばれない原因特定**
   - `layerDir.startsWith("classpath:")` の判定結果
   - `findStencilSettingsInLayer()` への入力値

3. **getSsYmlRecurive()が実行されているか確認**
   - `/tmp/get-ss-yml.log` の存在確認
   - バックエンド起動後にSuggest API実行してログ確認

### 6.2 実装優先順位

**Phase 1: 原因特定（デバッグ）**
- [ ] StorageConfig.getUserStencilDir()の実装確認
- [ ] Suggest API実行時のレイヤー検索フロー追跡
- [ ] ログファイルが作成されない理由の特定

**Phase 2: オプション1実装**
- [ ] mergeParentStencilSettingsUnified()の実装
- [ ] SuggestServiceImp.itemsToNode()の簡素化
- [ ] ReloadStencilMasterからマージ呼び出し削除

**Phase 3: テスト**
- [ ] Suggest API経由でパラメータ取得確認
- [ ] 親ディレクトリのdataDomainが反映されるか確認
- [ ] ReloadStencilMaster実行後の動作確認

**Phase 4: ドキュメント更新**
- [ ] アーキテクチャドキュメント更新
- [ ] API仕様書更新
- [ ] トラブルシューティングガイド更新

## 7. 結論

ProMarkerのステンシル設定ロードシステムは、**2つの独立した経路**を持ち、それぞれ異なる実装となっています。現在の問題は、**親ディレクトリマージ機能がSuggest API経路で動作していない**ことにあります。

**推奨される解決策**は、**TemplateEngineProcessor中心設計（オプション1）**を採用し、マージロジックを一元化することです。この設計により、データベーススキーマ変更なしに、全API経路で一貫したマージ動作を実現できます。

次のステップは、**StorageConfigの実装確認**と**デバッグログの詳細分析**により、なぜ`findStencilSettingsInFileSystem()`が呼ばれないのかを特定することです。
