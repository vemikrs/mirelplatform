# Phase 2-0 検証レポート: itemsToNode()簡素化

**日時**: 2025-11-15 19:34  
**Issue**: #36  
**Phase**: Phase 2-0 (緊急バグ修正)  
**担当**: GitHub Copilot  
**Status**: ✅ **COMPLETED**

---

## 1. 実施内容

### 変更概要
`itemsToNode()`メソッドを簡素化し、`mergeStencilDeAndDd()`を使用せず`dataDomain`を直接使用するように修正。

### 変更前のコード
```java
protected static Node itemsToNode(StencilSettingsYml settings){
    Node root = new RootNode();
    
    // ❌ mergeStencilDeAndDd()がdataElement(14個)ベースで動作
    List<Map<String, Object>> elems = mergeStencilDeAndDd(
        settings.getStencil().getDataElement(),  // 14個
        settings.getStencil().getDataDomain()    // 19個
    );
    
    elems.forEach(entry -> {
        root.addChild(convertItemToNodeItem(entry));
    });
    
    return root;
}
```

**問題点**:
- `mergeMapList()`がdataElement（14個）をベースにループ
- dataDomainのみのアイテム（5個）が無視される
- 結果: 14個のみ返却 → **5個のパラメータ欠落**

### 変更後のコード
```java
protected static Node itemsToNode(StencilSettingsYml settings){
    Node root = new RootNode();
    
    if(null == settings || null == settings.getStencil()){
        logger.debug("[ITEMS_TO_NODE] settings or stencil is null");
        return root;
    }
    
    logger.debug("[ITEMS_TO_NODE] Processing stencil settings:");
    logger.debug("[ITEMS_TO_NODE]   dataElement size: {}", ...);
    logger.debug("[ITEMS_TO_NODE]   dataDomain size: {}", ...);
    
    // ✅ dataDomainを直接使用（マージ不要）
    List<Map<String, Object>> elems = settings.getStencil().getDataDomain();
    
    logger.debug("[ITEMS_TO_NODE]   final elems size: {}", ...);
    
    if (elems != null) {
        elems.forEach(entry -> {
            root.addChild(convertItemToNodeItem(entry));
        });
    }
    
    return root;
}
```

**改善点**:
- ✅ dataDomainには親ステンシルから継承された定義と子の値が含まれている
- ✅ マージロジック不要でシンプル化
- ✅ すべてのパラメータ（19個）を正確に処理

---

## 2. 検証方法

### テストAPI
```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/suggest \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "/category1",
      "stencilCanonicalName": "/category1/test_service",
      "serialNo": "201221A"
    }
  }'
```

### 検証項目
1. ✅ API responseで`params.childs`が19個含まれているか
2. ✅ ログで`[ITEMS_TO_NODE] final elems size: 19`が出力されるか
3. ✅ すべてのパラメータが正しく変換されているか

---

## 3. 検証結果

### ログ出力
```
2025-11-15 19:31:46.798 [http-nio-3000-exec-1] DEBUG - [SUGGEST] === invoke() called ===
2025-11-15 19:31:46.798 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Parameter: stencilCategory=/category1, stencilCd=/category1/test_service, serialNo=201221A
2025-11-15 19:31:46.817 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Category: selected=/category1, items=13
2025-11-15 19:31:46.835 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Stencil: selected=/category1/test_service, items=3
2025-11-15 19:31:46.841 [http-nio-3000-exec-1] DEBUG - [SUGGEST] engine.getSerialNos() returned: size=1, values=[201221A]
2025-11-15 19:31:46.842 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Using requested serial: 201221A
2025-11-15 19:31:46.843 [http-nio-3000-exec-1] DEBUG - [SUGGEST] fltStrSerialNo: selected='201221A', items=1
2025-11-15 19:31:46.843 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Fetching final stencil settings and params...
2025-11-15 19:31:46.891 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Got settingsYaml: not null
2025-11-15 19:31:46.891 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Set stencil: not null
2025-11-15 19:31:46.892 [http-nio-3000-exec-1] DEBUG - [SUGGEST] Set params: not null
2025-11-15 19:31:46.892 [http-nio-3000-exec-1] DEBUG - [SUGGEST] === invoke() returning with complete result ===

[ITEMS_TO_NODE] Processing stencil settings:
[ITEMS_TO_NODE]   dataElement size: 14
[ITEMS_TO_NODE]   dataDomain size: 19
[ITEMS_TO_NODE]   final elems size: 19  ✅

[WRAP] Input model: not null
[WRAP]   model.params: not null
[WRAP]   model.stencil: not null
[WRAP]   model.fltStrSerialNo: selected='201221A'
[WRAP] Created ModelWrapper: not null
[WRAP]   ModelWrapper.model: not null
[WRAP] Created ApiResponse: not null
[WRAP]   response.data: not null
```

### API Response
```json
{
  "data": {
    "model": {
      "params": {
        "childs": [
          { "id": "appId", "name": "アプリケーションID", ... },
          { "id": "appName", "name": "アプリケーション名", ... },
          { "id": "modId", "name": "モジュールID", ... },
          { "id": "modName", "name": "モジュール名", ... },
          { "id": "ucId", "name": "ユースケースID", ... },
          { "id": "ucName", "name": "ユースケース名", ... },
          { "id": "psId", "name": "プロセスID", ... },
          { "id": "psName", "name": "プロセス名", ... },
          { "id": "layoutId", "name": "画面ID", ... },
          { "id": "layoutName", "name": "画面名", ... },
          { "id": "since", "name": "Since", "value": "1.0", ... },
          { "id": "grp", "name": "パッケージグループ", "value": "jp.co.takt", ... },
          { "id": "copyright", "name": "Copyright", "value": "Copyright(c) 2021 TAKT Systems, INC.", ... },
          { "id": "version", "name": "バージョン", "value": "1.0", ... },
          { "id": "author", "name": "作成者", "value": "TAKT", ... },
          { "id": "vendor", "name": "ベンダー", "value": "TAKT Systems, INC.", ... },
          { "id": "applicationId", "reference": "appId", ... },
          { "id": "moduleId", "reference": "modId", ... },
          { "id": "basePackageName", "reference": "grp", ... }
        ],
        "nodeType": "ROOT"
      },
      "stencil": { ... },
      "fltStrStencilCategory": { "selected": "/category1", "items": 13 },
      "fltStrStencilCd": { "selected": "/category1/test_service", "items": 3 },
      "fltStrSerialNo": { "selected": "201221A", "items": 1 }
    }
  },
  "messages": [],
  "errors": []
}
```

**パラメータ数確認**:
```bash
$ jq '.data.data.model.params.childs | length' response.json
19  ✅
```

---

## 4. 検証結果サマリー

| 検証項目 | 期待値 | 実際の値 | 結果 |
|---------|--------|---------|------|
| dataElement size | 14 | 14 | ✅ |
| dataDomain size | 19 | 19 | ✅ |
| final elems size | 19 | **19** | ✅ |
| params.childs length | 19 | **19** | ✅ |
| API response params | not null | not null | ✅ |
| API response stencil | not null | not null | ✅ |
| API response fltStrSerialNo.selected | "201221A" | "201221A" | ✅ |

---

## 5. デバッグログ追加

### 追加したデバッグポイント
1. **[SUGGEST]**: invoke()メソッドの詳細フロー
   - Category/Stencil/Serial選択状態
   - TemplateEngineProcessor作成
   - engine.getSerialNos()結果
   - serial決定ロジック
   - params/stencil設定確認

2. **[ITEMS_TO_NODE]**: パラメータ変換処理
   - dataElement/dataDomain サイズ
   - final elems サイズ
   - 変換前後の状態確認

3. **[WRAP]**: ModelWrapper処理
   - 入力model状態
   - ModelWrapper作成確認
   - ApiResponse作成確認

---

## 6. 根本原因分析

### 発見された問題
**mergeStencilDeAndDd()のロジックバグ**:
```java
// mergeMapList()の実装（抜粋）
list1.forEach(dataElement -> {  // ← dataElementベース（14個）
    Map<String, Object> target = Maps.newLinkedHashMap(dataElement);
    final String id = (String) target.get("id");
    
    list2.forEach(list2item -> {  // dataDomain（19個）から対応するものを探す
        if(false == id.equals(list2item.get("id"))) {
            return;  // IDが一致しない場合スキップ
        }
        // マッチしたものだけマージ
        ...
    });
    
    elems.add(target);  // ← dataElementの14個のみ追加
});
```

**問題点**:
- dataElement（14個）をベースにループ
- dataDomainのみに存在するアイテム（5個）は処理されない
- 結果: 14個のみ返却

### 解決策
**dataDomainを直接使用**:
```java
// ✅ dataDomainには親から継承された定義と子の値が含まれている
List<Map<String, Object>> elems = settings.getStencil().getDataDomain();
```

**理由**:
- `TemplateEngineProcessor.getStencilSettings()`は既に親ステンシルの設定をマージ済み
- `dataDomain`には完全なパラメータ定義が含まれている
- 追加のマージ処理は不要

---

## 7. 今後の対応

### 完了したこと
- ✅ itemsToNode()簡素化
- ✅ デバッグログ追加
- ✅ API動作確認（19個パラメータ出力）
- ✅ コミット完了（commit: ceca839）

### 次のステップ（work-plan.md準拠）
1. **Phase 2-1**: mergeParentStencilSettingsUnified()実装
   - 親ステンシル設定マージロジックの改善
   - TemplateEngineProcessor.java修正

2. **Phase 3**: 手動テスト・検証
   - Suggest API経由でパラメータ取得確認
   - Generate API動作確認
   - E2Eテスト実行

---

## 8. 結論

✅ **Phase 2-0完了**: itemsToNode()簡素化により、mergeStencilDeAndDd()バグを解決し、19個すべてのパラメータが正常に出力されることを確認しました。

**成果**:
- **パラメータ欠落問題解決**: 14個 → 19個（+5個）
- **コード簡素化**: マージロジック削除、dataDomain直接使用
- **デバッグログ強化**: 詳細なフロートレース可能に

**次のフォーカス**: Phase 2-1（mergeParentStencilSettingsUnified()実装）に進みます。

---

**Powered by Copilot 🤖**
