# デバッグ戦略 - Suggest API経路でのステンシル設定ロード問題

## 現状把握

### 確認済み事項 ✅

1. **StorageConfigの実装**: 正常動作
   - `getUserStencilDir()` → `"./data/storage/apps/promarker/stencil/user"`
   - 実際のパス: `./backend/data/storage/apps/promarker/stencil/user`
   - Spring Bootの作業ディレクトリが`./backend`のため、正しく解決される

2. **ファイルの存在**: 確認済み
   - `./backend/data/storage/apps/promarker/stencil/user/project/project_stencil-settings.yml` ✅
   - `./backend/data/storage/apps/promarker/stencil/user/project/module_service/201221A/stencil-settings.yml` ✅

3. **ReloadStencilMaster経路**: 正常動作
   - マージログ確認済み: "Successfully merged 19 dataDomain entries"
   - データベースには保存されない（payloadカラムなし）

4. **SuggestServiceImp.invoke()のフロー**:
   ```java
   // 1. Category選択 → resultModel.fltStrStencilCategory
   // 2. Stencil選択 → resultModel.fltStrStencilCd
   // 3. Serial確定後:
   engine = TemplateEngineProcessor.create(
       SteContext.standard(resultModel.fltStrStencilCd.selected, requestedSerial),
       resourcePatternResolver);
   
   StencilSettingsYml settingsYaml = engine.getStencilSettings();
   resultModel.params = itemsToNode(settingsYaml);
   ```

### 未確認事項 ❓

1. **TemplateEngineProcessor.create()の引数**:
   - `SteContext.standard(stencilCd, serialNo)` で何が設定されるか？
   - `context.getStencilCanonicalName()` の値は？
   - `context.getSerialNo()` の値は？

2. **getStencilSettings()の実行経路**:
   - `findStencilSettingsInLayers()` が呼ばれているか？
   - どのレイヤー（user/standard/samples）で検索されているか？
   - `findStencilSettingsInFileSystem()` まで到達しているか？

3. **ログが出力されない理由**:
   - ログ出力箇所まで到達していない
   - 別のクラスパスのTemplateEngineProcessorが使われている？
   - 例外発生で早期リターン？

## デバッグ戦略

### Phase 1: SteContext確認

**目的**: `context.getStencilCanonicalName()` と `context.getSerialNo()` の実際の値を確認

**方法**: `SteContext.standard()` にログ追加

**期待される値**:
- `stencilCanonicalName`: `/user/project/module_service`
- `serialNo`: `201221A`

**確認コマンド**:
```bash
grep -n "public static SteContext standard" backend/src/main/java/jp/vemi/ste/domain/context/SteContext.java
```

### Phase 2: getStencilSettings()実行確認

**目的**: `getStencilSettings()` が実際に実行されているか、どの経路を通るか確認

**方法**: メソッド冒頭に強制ログ出力

**実装箇所**:
```java
public StencilSettingsYml getStencilSettings() {
    // ✅ 既存ログあり（削除しないこと）
    logger.debug("=== getStencilSettings called ===");
    
    // ✅ 追加: 標準出力＋ファイル出力
    System.out.println("[GET_SETTINGS] Called at " + java.time.LocalDateTime.now());
    try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/get-settings.log", true)) {
        fw.write("[GET_SETTINGS] Called with context:\n");
        fw.write("  - stencilCanonicalName: " + context.getStencilCanonicalName() + "\n");
        fw.write("  - serialNo: " + context.getSerialNo() + "\n");
        fw.flush();
    } catch (Exception ignore) {}
    
    StencilSettingsYml settings = findStencilSettingsInLayers();
    // ...
}
```

### Phase 3: findStencilSettingsInLayers()フロー確認

**目的**: レイヤー検索のどの段階で失敗しているか特定

**方法**: 各レイヤーの検索結果をログ出力

**実装箇所**:
```java
private StencilSettingsYml findStencilSettingsInLayers() {
    System.out.println("[FIND_LAYERS] START");
    try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/find-layers.log", true)) {
        fw.write("[FIND_LAYERS] === START ===\n");
        fw.flush();
    } catch (Exception ignore) {}
    
    String[] searchLayers = {
        StorageConfig.getUserStencilDir(),
        StorageConfig.getStandardStencilDir(),
        StorageConfig.getSamplesStencilDir()
    };
    
    for (int i = 0; i < searchLayers.length; i++) {
        String layerDir = searchLayers[i];
        System.out.println("[FIND_LAYERS] Layer " + i + ": " + layerDir);
        
        StencilSettingsYml settings = findStencilSettingsInLayer(layerDir);
        
        if (settings != null) {
            System.out.println("[FIND_LAYERS] SUCCESS at layer " + i);
            try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/find-layers.log", true)) {
                fw.write("[FIND_LAYERS] Found at layer " + i + ": " + layerDir + "\n");
                fw.flush();
            } catch (Exception ignore) {}
            return settings;
        } else {
            System.out.println("[FIND_LAYERS] NULL at layer " + i);
        }
    }
    
    System.out.println("[FIND_LAYERS] FAILED - No settings found");
    return null;
}
```

### Phase 4: contextのstencilCanonicalName確認

**問題仮説**: `context.getStencilCanonicalName()` が期待と異なる値になっている

**確認方法**: SuggestServiceImpでの`TemplateEngineProcessor.create()`呼び出し前後にログ追加

**実装箇所**:
```java
// SuggestServiceImp.java - line 110付近
String stencilCd = resultModel.fltStrStencilCd.selected;
String serialNo = isWildcard(requestedSerial)?"":requestedSerial;

System.out.println("[SUGGEST] Creating TemplateEngineProcessor");
System.out.println("[SUGGEST]   stencilCd: " + stencilCd);
System.out.println("[SUGGEST]   serialNo: " + serialNo);

try (java.io.FileWriter fw = new java.io.FileWriter("/tmp/suggest-context.log", true)) {
    fw.write("[SUGGEST] Creating engine with:\n");
    fw.write("  - stencilCd: " + stencilCd + "\n");
    fw.write("  - serialNo: " + serialNo + "\n");
    fw.flush();
} catch (Exception ignore) {}

engine = TemplateEngineProcessor.create(
    SteContext.standard(stencilCd, serialNo),
    resourcePatternResolver);
```

**SteContext.standard()の確認**:
```java
// SteContext.java
public static SteContext standard(String stencilName, String serialNo) {
    System.out.println("[STE_CONTEXT] Creating context:");
    System.out.println("[STE_CONTEXT]   stencilName: " + stencilName);
    System.out.println("[STE_CONTEXT]   serialNo: " + serialNo);
    
    SteContext ctx = standard(stencilName);
    ctx.put("serialNo", serialNo);
    return ctx;
}

public static SteContext standard(String stencilName) {
    System.out.println("[STE_CONTEXT] standard() with stencilName: " + stencilName);
    
    SteContext ctx = standard();
    ctx.put("stencilCanonicalName", stencilName);
    
    System.out.println("[STE_CONTEXT]   After put, getStencilCanonicalName(): " + ctx.getStencilCanonicalName());
    return ctx;
}
```

## 実装計画

### ステップ1: 最小限のデバッグログ追加

**変更ファイル**:
1. `SteContext.java` - standard()メソッド
2. `TemplateEngineProcessor.java` - getStencilSettings()冒頭
3. `SuggestServiceImp.java` - create()呼び出し前

**期待される結果**:
- `/tmp/suggest-context.log` - SuggestServiceでのパラメータ
- `/tmp/get-settings.log` - getStencilSettings()の実行確認
- 標準出力にも同じ内容

### ステップ2: テスト実行

**手順**:
```bash
# 1. バックエンド再起動
fuser -k 3000/tcp
cd backend && ./gradlew bootRun

# 2. ログファイルクリア
rm -f /tmp/suggest-context.log /tmp/get-settings.log /tmp/find-layers.log

# 3. Suggest API実行
curl -X POST http://localhost:3000/mipla2/apps/mste/api/suggest \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "/user",
      "stencilCanonicalName": "/user/project/module_service",
      "serialNo": "201221A"
    }
  }'

# 4. ログ確認
cat /tmp/suggest-context.log
cat /tmp/get-settings.log
cat /tmp/find-layers.log
```

### ステップ3: 結果分析

**ケース1: ログファイルが作成されない**
→ `SuggestServiceImp.invoke()` が呼ばれていない
→ API経路の問題（ApiController, SuggestApi確認）

**ケース2: suggest-context.logのみ作成される**
→ `TemplateEngineProcessor.create()` でエラー発生
→ try-catch確認、例外ログ確認

**ケース3: get-settings.logまで作成される**
→ `findStencilSettingsInLayers()` でnull返却
→ レイヤー検索の詳細ログ追加（Phase 3）

**ケース4: 全ログが作成される**
→ マージが呼ばれていないことを確認
→ mergeParentStencilSettings()のログ確認

## 次のアクション

1. ✅ StorageConfig確認完了
2. ⏭️ SteContext.standard()にデバッグログ追加
3. ⏭️ TemplateEngineProcessor.getStencilSettings()にデバッグログ追加
4. ⏭️ バックエンド再起動 → Suggest API実行 → ログ確認
5. ⏭️ 結果に基づいて次のデバッグ戦略決定
