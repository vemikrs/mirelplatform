# 📋 修正計画書: ProMarker バリデーション機能の適正化

**Issue**: #29 - Migration frontend to React  
**作成日**: 2025年11月4日  
**ステータス**: 計画中

## 🔍 問題の本質

### 現状の問題点

#### 1. フロントエンドが独自にハードコードされたバリデーションルールを実装

**場所**: `apps/frontend-v3/src/features/promarker/schemas/parameter.ts`

```typescript
// 問題のコード
stringSchema = stringSchema.min(3, '3文字以上入力してください');  // ← 根拠なし
stringSchema = stringSchema.max(100, '100文字以内で入力してください');

if (param.id === 'userName') {
  stringSchema = stringSchema.regex(/^[a-zA-Z0-9]+$/, '半角英数字のみ使用できます');
}

if (param.id === 'language') {
  stringSchema = stringSchema.regex(/^[a-z]{2}$/, '2文字の言語コードを入力してください');
}
```

**問題点**:
- すべてのテキストフィールドに「最小3文字」を要求（根拠なし）
- 特定フィールド（userName, language）に正規表現を強制（ステンシル定義と無関係）
- これらのルールはフロントエンドのコードにハードコード
- ステンシル作成者が意図したバリデーションを定義できない

**影響**:
- Generateボタンがdisabledになる（画像参照）
- バージョン番号「1.0」が3文字以上ルールで弾かれる可能性
- バリデーションルール変更にコード修正が必要

#### 2. ステンシル定義（YAML）にバリデーション情報がない

**現在のYAML構造**:
```yaml
dataDomain:
  - id: "message"
    name: "メッセージ"
    value: "Hello, World!"
    type: "text"
    placeholder: "メッセージを入力してください"
    note: "テンプレートで使用されるメッセージです。"
    # ❌ バリデーションルール（required, min, max, pattern等）が欠落
```

#### 3. バックエンドのバリデーション機能が活用されていない

**既存機能**:
- `TemplateEngineProcessor.java` には `valueEmptyValidate()` が存在
- `nullable` や `reference` などの仕組みは既にある

**問題**:
- フロントエンドに伝達されていない
- バックエンドとフロントエンドでバリデーションロジックが分断

### あるべき姿

**「バリデーション定義はステンシル定義から取得されるべき」**

```
ステンシルYAML定義 → Backend API → Frontend
   (Source of Truth)    (中継)      (表示・適用)
```

**設計原則**:
- **Single Source of Truth**: バリデーションルールはYAMLに一元管理
- **宣言的バリデーション**: コードではなく設定で定義
- **拡張性**: 新しいバリデーションルールを簡単に追加可能
- **後方互換性**: validation未定義の既存ステンシルも動作

## 📐 設計方針

### Phase 1: データモデル拡張（バックエンド主導）

#### 1.1 ステンシルYAML定義の拡張

**拡張後のスキーマ**:
```yaml
dataDomain:
  - id: "message"
    name: "メッセージ"
    value: "Hello, World!"
    type: "text"
    placeholder: "メッセージを入力してください"
    note: "テンプレートで使用されるメッセージです。"
    # 👇 新規追加
    validation:
      required: false          # 必須フィールドか（デフォルト: false）
      minLength: 1             # 最小文字数（デフォルト: なし）
      maxLength: 200           # 最大文字数（デフォルト: なし）
      pattern: null            # 正規表現パターン（デフォルト: なし）
      errorMessage: "カスタムエラーメッセージ"  # オプション
      
  - id: "userName"
    name: "ユーザー名"
    value: "Developer"
    type: "text"
    placeholder: "ユーザー名を入力してください"
    note: "挨拶に使用されるユーザー名です。"
    validation:
      required: true
      minLength: 2
      maxLength: 50
      pattern: "^[a-zA-Z0-9_-]+$"
      errorMessage: "半角英数字、ハイフン、アンダースコアのみ使用できます"
      
  - id: "language"
    name: "言語"
    value: "ja"
    type: "select"
    placeholder: "言語を選択してください"
    note: "生成されるメッセージの言語です。"
    validation:
      required: true
      pattern: "^(ja|en)$"
      
  - id: "version"
    name: "バージョン"
    value: "1.0"
    type: "text"
    placeholder: "バージョン番号を入力"
    note: "バージョン番号です。"
    validation:
      required: false      # 必須ではない
      minLength: 1         # 最小1文字（"1.0"は2文字なのでOK）
      maxLength: 20
      pattern: "^[0-9.]+$"
      errorMessage: "数字とピリオドのみ使用できます"
```

**バリデーションルールの仕様**:

| フィールド | 型 | デフォルト | 説明 |
|----------|------|----------|------|
| `required` | boolean | false | 必須フィールドか |
| `minLength` | number | null | 最小文字数（null=制限なし） |
| `maxLength` | number | null | 最大文字数（null=制限なし） |
| `pattern` | string | null | 正規表現パターン（null=制限なし） |
| `errorMessage` | string | null | カスタムエラーメッセージ |

#### 1.2 DataElement型の拡張（バックエンド）

**新規クラス追加**:
```java
package jp.vemi.ste.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * バリデーションルール定義
 * ステンシルのdataDomain.validationフィールドに対応
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRule {
    /** 必須フィールドか */
    private Boolean required;
    
    /** 最小文字数 */
    private Integer minLength;
    
    /** 最大文字数 */
    private Integer maxLength;
    
    /** 正規表現パターン */
    private String pattern;
    
    /** カスタムエラーメッセージ */
    private String errorMessage;
}
```

**既存クラスの拡張**:
```java
// StencilSettings.java または相当クラス
public class DataDomain {
    private String id;
    private String name;
    private String type;
    private String value;
    private String placeholder;
    private String note;
    
    // 👇 追加
    private ValidationRule validation;
    
    // getters/setters...
}
```

#### 1.3 Suggest APIレスポンス拡張

**変更不要**: 既存の構造で `validation` フィールドが自動的に含まれる

YAMLパーサーが `validation` オブジェクトを読み込めば、それがそのままJSON化されてフロントエンドに送信されます。

**確認ポイント**:
- YAMLパーサー（SnakeYAML）が `validation` オブジェクトを正しくマッピング
- Jackson（JSON serializer）が `ValidationRule` を正しくシリアライズ

### Phase 2: フロントエンド適応

#### 2.1 TypeScript型定義の拡張

**ファイル**: `apps/frontend-v3/src/features/promarker/types/api.ts`

```typescript
/**
 * Validation Rule Definition
 * Corresponds to backend ValidationRule class
 */
export interface ValidationRule {
  required?: boolean;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  errorMessage?: string;
}

/**
 * Data Element (Parameter definition)
 * Describes a single input parameter for the stencil
 */
export interface DataElement {
  id: string;
  name: string;
  valueType: 'text' | 'file' | 'select';
  value: string;
  placeholder: string;
  note: string;
  nodeType: 'ELEMENT';
  
  // 👇 追加
  validation?: ValidationRule;  // Optional - 後方互換性のため
  
  // select type用（将来の拡張）
  options?: Array<{ value: string; text: string }>;
}
```

#### 2.2 動的バリデーションスキーマの実装

**ファイル**: `apps/frontend-v3/src/features/promarker/schemas/parameter.ts`

**完全書き換え**:

```typescript
import { z } from 'zod';
import type { DataElement } from '../types/api';

/**
 * Dynamic parameter validation schema builder
 * Creates Zod schema based on parameter definitions from API
 * 
 * **重要**: バリデーションルールはステンシル定義（YAML）から取得
 * ハードコードされたルールは使用しない
 */

/**
 * Create validation schema for a single parameter
 * @param param - Parameter definition from API (includes validation rules)
 */
function createParameterSchema(param: DataElement): z.ZodTypeAny {
  // Get validation rules from API (defaults to empty object)
  const validation = param.validation || {};
  
  // Base schema based on valueType
  switch (param.valueType?.toLowerCase()) {
    case 'text':
    case 'string': {
      let schema: z.ZodString = z.string();
      
      // Apply minLength if specified in YAML
      if (validation.minLength !== undefined && validation.minLength > 0) {
        const message = validation.errorMessage || `${validation.minLength}文字以上入力してください`;
        schema = schema.min(validation.minLength, message);
      }
      
      // Apply maxLength if specified in YAML
      if (validation.maxLength !== undefined && validation.maxLength > 0) {
        const message = `${validation.maxLength}文字以内で入力してください`;
        schema = schema.max(validation.maxLength, message);
      }
      
      // Apply pattern if specified in YAML
      if (validation.pattern) {
        const message = validation.errorMessage || '入力形式が正しくありません';
        schema = schema.regex(new RegExp(validation.pattern), message);
      }
      
      // Handle required flag
      if (validation.required) {
        // Required field - must have at least 1 character
        return schema.min(1, validation.errorMessage || '必須項目です');
      } else {
        // Optional field - allow empty string
        return schema.optional().or(z.literal(''));
      }
    }
    
    case 'number': {
      let schema = z.coerce.number();
      
      // Apply min/max if specified
      if (validation.minLength !== undefined) {
        schema = schema.min(validation.minLength, 
          validation.errorMessage || `${validation.minLength}以上の値を入力してください`);
      }
      
      if (validation.maxLength !== undefined) {
        schema = schema.max(validation.maxLength, 
          `${validation.maxLength}以下の値を入力してください`);
      }
      
      return validation.required ? schema : schema.optional();
    }
    
    case 'file': {
      // File IDs are strings (uploaded file references)
      let schema = z.string();
      
      if (validation.required) {
        return schema.min(1, validation.errorMessage || 'ファイルをアップロードしてください');
      } else {
        return schema.optional().or(z.literal(''));
      }
    }
    
    case 'select': {
      let schema = z.string();
      
      if (validation.required) {
        return schema.min(1, validation.errorMessage || '選択してください');
      } else {
        return schema.optional().or(z.literal(''));
      }
    }
    
    default: {
      // Unknown type - treat as text with minimal validation
      let schema = z.string();
      
      if (validation.required) {
        return schema.min(1, validation.errorMessage || '必須項目です');
      } else {
        return schema.optional().or(z.literal(''));
      }
    }
  }
}

/**
 * Create validation schema for all parameters
 * @param parameters - Parameter definitions from API
 * @returns Zod object schema
 */
export function createParameterValidationSchema(parameters: DataElement[]) {
  const schemaShape: Record<string, z.ZodTypeAny> = {};

  parameters.forEach((param) => {
    schemaShape[param.id] = createParameterSchema(param);
  });

  return z.object(schemaShape);
}

/**
 * Parameter form values type
 */
export type ParameterFormValues = Record<string, string>;

/**
 * Validation error type
 */
export interface ValidationError {
  field: string;
  message: string;
}

/**
 * Extract validation errors from Zod error
 */
export function extractValidationErrors(error: z.ZodError): ValidationError[] {
  return error.errors.map((err) => ({
    field: err.path.join('.'),
    message: err.message,
  }));
}

/**
 * Validate parameter values against schema
 * @param values - Form values
 * @param parameters - Parameter definitions
 * @returns Validation result
 */
export function validateParameters(
  values: Record<string, string>,
  parameters: DataElement[]
): { success: boolean; errors?: ValidationError[] } {
  const schema = createParameterValidationSchema(parameters);

  try {
    schema.parse(values);
    return { success: true };
  } catch (error) {
    if (error instanceof z.ZodError) {
      return {
        success: false,
        errors: extractValidationErrors(error),
      };
    }
    return {
      success: false,
      errors: [{ field: '_global', message: 'バリデーションエラーが発生しました' }],
    };
  }
}

/**
 * Get default values for parameters
 * @param parameters - Parameter definitions
 * @returns Default values object
 */
export function getDefaultValues(parameters: DataElement[]): ParameterFormValues {
  const defaults: ParameterFormValues = {};

  parameters.forEach((param) => {
    defaults[param.id] = param.value || '';
  });

  return defaults;
}
```

**変更のポイント**:
1. ❌ ハードコードされた `min(3)` を削除
2. ❌ ハードコードされた `max(100)` を削除
3. ❌ 特定フィールド名（userName, language）への依存を削除
4. ✅ `param.validation` から動的にルールを取得
5. ✅ validation未定義でも動作（後方互換性）

#### 2.3 フォーム初期化の改善

**ファイル**: `apps/frontend-v3/src/features/promarker/hooks/useParameterForm.ts`

```typescript
export function useParameterForm(parameters: DataElement[]) {
  // ... 既存コード ...

  // Initialize form
  const form = useForm<ParameterFormValues>({
    resolver: zodResolver(schema),
    defaultValues,
    mode: 'all',  // 👈 変更: 'onBlur' → 'all' (即座にバリデーション)
    reValidateMode: 'onChange',
  });

  // Update form when parameters change
  useEffect(() => {
    if (parameters.length > 0) {
      const newDefaults = getDefaultValues(parameters);
      form.reset(newDefaults, {
        keepErrors: false,
        keepDirty: false,
        keepIsValid: false,
      });
      
      // 👇 追加: 初期値がある場合は即座にバリデーション実行
      setTimeout(() => {
        form.trigger();
      }, 100);
    }
  }, [parameters]); // eslint-disable-line react-hooks/exhaustive-deps

  // ... 既存コード ...
}
```

### Phase 3: バックエンドバリデーション強化（オプション）

#### 3.1 Generate APIでのサーバーサイドバリデーション

**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceImp.java`

```java
/**
 * validate with stencil-defined validation rules
 * 
 * @param param パラメータ
 * @return メッセージ一覧
 */
public List<String> validate(Map<String, Object> param) {
    List<String> valids = Lists.newArrayList();

    // 既存のバリデーション（ステンシルが選択されているか等）
    final Object stencilCanonicalNameObject = param.get("stencilCanonicalName");
    final String stencilCanonicalName = stencilCanonicalNameObject == null ? StringUtils.EMPTY
            : stencilCanonicalNameObject.toString();
    if (StringUtils.isEmpty(stencilCanonicalName) || "*".equals(stencilCanonicalName)) {
        valids.add("ステンシルが指定されていません。");
    }

    final Object serialNoObject = param.get("serialNo");
    final String serialNo = serialNoObject == null ? StringUtils.EMPTY : serialNoObject.toString();
    if (StringUtils.isEmpty(serialNo) || "*".equals(serialNo)) {
        valids.add("シリアルが指定されていません。");
    }

    // 👇 追加: ステンシル定義からバリデーションルールを取得して検証
    try {
        StencilSettings settings = getStencilSettings(stencilCanonicalName, serialNo);
        List<DataDomain> dataDomains = settings.getStencil().getDataDomain();
        
        for (DataDomain domain : dataDomains) {
            ValidationRule rule = domain.getValidation();
            if (rule == null) continue;  // validation未定義はスキップ
            
            String value = (String) param.get(domain.getId());
            
            // Required check
            if (Boolean.TRUE.equals(rule.getRequired())) {
                if (StringUtils.isEmpty(value)) {
                    valids.add(domain.getName() + "は必須です");
                    continue;
                }
            }
            
            // 値が空の場合、以降のチェックはスキップ
            if (StringUtils.isEmpty(value)) continue;
            
            // MinLength check
            if (rule.getMinLength() != null && value.length() < rule.getMinLength()) {
                String msg = rule.getErrorMessage() != null 
                    ? rule.getErrorMessage() 
                    : domain.getName() + "は" + rule.getMinLength() + "文字以上必要です";
                valids.add(msg);
            }
            
            // MaxLength check
            if (rule.getMaxLength() != null && value.length() > rule.getMaxLength()) {
                valids.add(domain.getName() + "は" + rule.getMaxLength() + "文字以内にしてください");
            }
            
            // Pattern check
            if (rule.getPattern() != null && !value.matches(rule.getPattern())) {
                String msg = rule.getErrorMessage() != null 
                    ? rule.getErrorMessage() 
                    : domain.getName() + "の形式が正しくありません";
                valids.add(msg);
            }
        }
    } catch (Exception e) {
        // ステンシル定義取得エラーは既存のバリデーションで検出済み
        logger.warn("Stencil validation failed", e);
    }

    return valids;
}
```

## 🚀 実装ステップ（TDD: Test-Driven Development）

### TDD原則

本プロジェクトでは**テスト駆動開発（TDD）**を採用します：

1. **Red**: テストを先に書く（失敗することを確認）
2. **Green**: 最小限のコードでテストを通す
3. **Refactor**: コードを改善・リファクタリング

各ステップで**テストファースト**を徹底します。

---

### Step 1: 緊急対応（即座実施） ⚡

**目的**: 現在の問題（Generateボタンdisabled）を即座に解決

#### 1.1 テスト作成（Red）

**新規ファイル**: `apps/frontend-v3/src/features/promarker/schemas/parameter.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import { createParameterValidationSchema } from './parameter';
import type { DataElement } from '../types/api';

describe('Parameter Validation Schema - Emergency Fix', () => {
  it('デフォルト値が入っている場合、バリデーションエラーにならない', () => {
    const parameters: DataElement[] = [
      {
        id: 'version',
        name: 'バージョン',
        valueType: 'text',
        value: '1.0',  // 2文字のデフォルト値
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ version: '1.0' });

    expect(result.success).toBe(true);
  });

  it('空文字でもバリデーションエラーにならない（必須でない場合）', () => {
    const parameters: DataElement[] = [
      {
        id: 'optionalField',
        name: 'オプション',
        valueType: 'text',
        value: '',
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ optionalField: '' });

    expect(result.success).toBe(true);
  });

  it('3文字未満でもバリデーションエラーにならない', () => {
    const parameters: DataElement[] = [
      {
        id: 'shortText',
        name: '短いテキスト',
        valueType: 'text',
        value: 'ab',  // 2文字
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ shortText: 'ab' });

    expect(result.success).toBe(true);
  });
});
```

**テスト実行**: `pnpm --filter frontend-v3 test parameter.test.ts`
→ **Red**: テストが失敗することを確認（現在のコードはmin(3)があるため）

#### 1.2 実装（Green）

**作業内容**:
```typescript
// apps/frontend-v3/src/features/promarker/schemas/parameter.ts
// ハードコードされた min(3) を削除または緩和

function createParameterSchema(param: DataElement): z.ZodTypeAny {
  switch (param.valueType?.toLowerCase()) {
    case 'text':
    case 'string': {
      let schema = z.string();
      
      // ❌ 削除: stringSchema = stringSchema.min(3, '3文字以上入力してください');
      
      // ✅ 必須の場合のみ最小1文字チェック
      const isRequired = !param.value || param.note?.includes('必須');
      if (isRequired) {
        schema = schema.min(1, '必須項目です');
      }
      
      // 最大文字数は残す（妥当な制限）
      schema = schema.max(100, '100文字以内で入力してください');
      
      return isRequired ? schema : schema.optional();
    }
    // ...
  }
}
```

**テスト実行**: `pnpm --filter frontend-v3 test parameter.test.ts`
→ **Green**: すべてのテストが通過することを確認

#### 1.3 手動確認

- [ ] Generateボタンが正常に動作
- [ ] 既存のE2Eテストが通過

#### 1.4 コミット

```bash
git add apps/frontend-v3/src/features/promarker/schemas/parameter.ts
git add apps/frontend-v3/src/features/promarker/schemas/parameter.test.ts
git commit -m "fix(promarker): 不要な最小文字数制限を削除 (refs #29)

- ハードコードされたmin(3)を削除
- 必須フィールドのみmin(1)を適用
- テストファーストで実装"
```

---

### Step 2: ステンシルYAML定義拡張（TDD）

**優先度**: 高  
**期間**: 1-2日

#### 2.1 テスト作成（Red）

**新規ファイル**: `backend/src/test/java/jp/vemi/ste/domain/model/ValidationRuleTest.java`

```java
package jp.vemi.ste.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationRuleTest {
    
    @Test
    void testValidationRuleCreation() {
        ValidationRule rule = ValidationRule.builder()
            .required(true)
            .minLength(2)
            .maxLength(50)
            .pattern("^[a-zA-Z0-9_-]+$")
            .errorMessage("半角英数字、ハイフン、アンダースコアのみ使用できます")
            .build();
        
        assertTrue(rule.getRequired());
        assertEquals(2, rule.getMinLength());
        assertEquals(50, rule.getMaxLength());
        assertEquals("^[a-zA-Z0-9_-]+$", rule.getPattern());
        assertEquals("半角英数字、ハイフン、アンダースコアのみ使用できます", rule.getErrorMessage());
    }
    
    @Test
    void testValidationRuleDefaults() {
        ValidationRule rule = new ValidationRule();
        
        assertNull(rule.getRequired());
        assertNull(rule.getMinLength());
        assertNull(rule.getMaxLength());
        assertNull(rule.getPattern());
        assertNull(rule.getErrorMessage());
    }
}
```

**新規ファイル**: `backend/src/test/java/jp/vemi/ste/domain/model/StencilSettingsValidationTest.java`

```java
package jp.vemi.ste.domain.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StencilSettingsValidationTest {
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Test
    void testYAMLParsingWithValidation() throws Exception {
        // YAML with validation definition
        String yamlContent = """
            stencil:
              dataDomain:
                - id: "userName"
                  name: "ユーザー名"
                  type: "text"
                  value: "Developer"
                  validation:
                    required: true
                    minLength: 2
                    maxLength: 50
                    pattern: "^[a-zA-Z0-9_-]+$"
                    errorMessage: "半角英数字、ハイフン、アンダースコアのみ使用できます"
            """;
        
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);
        
        assertNotNull(data);
        Map<String, Object> stencil = (Map<String, Object>) data.get("stencil");
        assertNotNull(stencil);
        // ... さらに詳細な検証
    }
    
    @Test
    void testYAMLParsingWithoutValidation() throws Exception {
        // YAML without validation (backward compatibility)
        String yamlContent = """
            stencil:
              dataDomain:
                - id: "message"
                  name: "メッセージ"
                  type: "text"
                  value: "Hello"
            """;
        
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);
        
        assertNotNull(data);
        // validation未定義でもパースエラーにならないことを確認
    }
}
```

**テスト実行**: `./gradlew test --tests ValidationRuleTest --tests StencilSettingsValidationTest`
→ **Red**: テストが失敗（ValidationRuleクラスがまだ存在しない）

#### 2.2 実装（Green）

**新規ファイル**: `backend/src/main/java/jp/vemi/ste/domain/model/ValidationRule.java`

```java
package jp.vemi.ste.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRule {
    private Boolean required;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private String errorMessage;
}
```

**ファイル更新**: サンプルステンシルYAMLに validation を追加

**テスト実行**: `./gradlew test`
→ **Green**: すべてのテストが通過

#### 2.3 統合テスト作成（Red → Green）

**新規ファイル**: `backend/src/test/java/jp/vemi/mirel/apps/mste/api/SuggestApiValidationTest.java`

```java
package jp.vemi.mirel.apps.mste.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SuggestApiValidationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testSuggestApiReturnsValidation() {
        Map<String, Object> request = Map.of(
            "content", Map.of(
                "stencilCategoy", "/samples",
                "stencilCanonicalName", "/samples/hello-world",
                "serialNo", "250913A"
            )
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/apps/mste/api/suggest",
            request,
            Map.class
        );
        
        assertEquals(200, response.getStatusCode().value());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> model = (Map<String, Object>) data.get("model");
        Map<String, Object> params = (Map<String, Object>) model.get("params");
        
        // validation定義が含まれることを確認
        // 実際の構造に合わせて検証ロジックを追加
    }
}
```

#### 2.4 コミット

```bash
git add backend/src/main/java/jp/vemi/ste/domain/model/ValidationRule.java
git add backend/src/test/java/jp/vemi/ste/domain/model/ValidationRuleTest.java
git add backend/src/main/resources/promarker/stencil/samples/samples/hello-world/250913A/stencil-settings.yml
git commit -m "feat(promarker): ValidationRuleモデルとYAML定義拡張 (refs #29)

- ValidationRuleクラスの作成
- ステンシルYAMLにvalidation定義を追加
- TDDでテストファースト実装
- 後方互換性を確保"
```

---

### Step 3: バックエンドモデル拡張（TDD）

**優先度**: 高  
**期間**: 1日

#### 3.1 テスト作成（Red）

**ファイル**: `backend/src/test/java/jp/vemi/mirel/apps/mste/domain/service/SuggestServiceValidationTest.java`

```java
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dto.SuggestParameter;
import jp.vemi.framework.web.api.ApiRequest;
import jp.vemi.framework.web.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SuggestServiceValidationTest {
    
    @Autowired
    private SuggestService suggestService;
    
    @Test
    void testSuggestServiceReturnsParametersWithValidation() {
        SuggestParameter param = new SuggestParameter();
        param.stencilCategory = "/samples";
        param.stencilCd = "/samples/hello-world";
        param.serialNo = "250913A";
        
        ApiRequest<SuggestParameter> request = ApiRequest.<SuggestParameter>builder()
            .model(param)
            .build();
        
        ApiResponse<?> response = suggestService.invoke(request);
        
        assertNotNull(response);
        assertNotNull(response.getData());
        
        // ModelWrapper構造を考慮
        // validation定義がレスポンスに含まれることを確認
    }
    
    @Test
    void testBackwardCompatibilityWithoutValidation() {
        // validation未定義のステンシルでもエラーにならないことを確認
        SuggestParameter param = new SuggestParameter();
        param.stencilCategory = "*";
        param.stencilCd = "*";
        param.serialNo = "*";
        
        ApiRequest<SuggestParameter> request = ApiRequest.<SuggestParameter>builder()
            .model(param)
            .build();
        
        assertDoesNotThrow(() -> {
            suggestService.invoke(request);
        });
    }
}
```

**テスト実行**: `./gradlew test --tests SuggestServiceValidationTest`
→ **Red**: ValidationRuleがレスポンスに含まれていない

#### 3.2 実装（Green）

既存モデルに validation フィールドを追加し、YAMLマッピングを設定。

**テスト実行**: `./gradlew test`
→ **Green**: すべてのテストが通過

#### 3.3 コミット

```bash
git add backend/src/main/java/jp/vemi/ste/domain/model/
git add backend/src/test/java/jp/vemi/mirel/apps/mste/domain/service/SuggestServiceValidationTest.java
git commit -m "feat(promarker): DataDomainにvalidation追加 (refs #29)

- DataDomainクラスにvalidationフィールド追加
- YAMLパーサーの設定更新
- テストで後方互換性を確認"
```

---

### Step 4: フロントエンド完全対応（TDD）

**優先度**: 高  
**期間**: 1-2日

#### 4.1 型定義テスト作成（Red）

**新規ファイル**: `apps/frontend-v3/src/features/promarker/types/api.test.ts`

```typescript
import { describe, it, expect } from 'vitest';
import type { DataElement, ValidationRule } from './api';

describe('API Types - ValidationRule', () => {
  it('ValidationRuleインターフェースが正しく定義されている', () => {
    const validationRule: ValidationRule = {
      required: true,
      minLength: 2,
      maxLength: 50,
      pattern: '^[a-zA-Z0-9_-]+$',
      errorMessage: 'カスタムメッセージ',
    };

    expect(validationRule.required).toBe(true);
    expect(validationRule.minLength).toBe(2);
  });

  it('DataElementがvalidationを持つことができる', () => {
    const dataElement: DataElement = {
      id: 'userName',
      name: 'ユーザー名',
      valueType: 'text',
      value: 'Developer',
      placeholder: '',
      note: '',
      nodeType: 'ELEMENT',
      validation: {
        required: true,
        minLength: 2,
      },
    };

    expect(dataElement.validation).toBeDefined();
    expect(dataElement.validation?.required).toBe(true);
  });

  it('validationがundefinedでも動作する（後方互換性）', () => {
    const dataElement: DataElement = {
      id: 'message',
      name: 'メッセージ',
      valueType: 'text',
      value: '',
      placeholder: '',
      note: '',
      nodeType: 'ELEMENT',
      // validation: undefined
    };

    expect(dataElement.validation).toBeUndefined();
  });
});
```

#### 4.2 バリデーションスキーマテスト作成（Red）

**ファイル**: `apps/frontend-v3/src/features/promarker/schemas/parameter.test.ts` - 拡張

```typescript
describe('Parameter Validation Schema - Dynamic from API', () => {
  describe('required validation', () => {
    it('required=trueの場合、空文字はエラー', () => {
      const parameters: DataElement[] = [
        {
          id: 'userName',
          name: 'ユーザー名',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            required: true,
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ userName: '' });

      expect(result.success).toBe(false);
    });

    it('required=falseの場合、空文字はOK', () => {
      const parameters: DataElement[] = [
        {
          id: 'optionalField',
          name: 'オプション',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            required: false,
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ optionalField: '' });

      expect(result.success).toBe(true);
    });
  });

  describe('minLength validation', () => {
    it('minLength指定がある場合、それより短いとエラー', () => {
      const parameters: DataElement[] = [
        {
          id: 'userName',
          name: 'ユーザー名',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            minLength: 3,
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ userName: 'ab' });

      expect(result.success).toBe(false);
    });

    it('minLength指定がない場合、どんな長さでもOK', () => {
      const parameters: DataElement[] = [
        {
          id: 'message',
          name: 'メッセージ',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {},
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ message: 'a' });

      expect(result.success).toBe(true);
    });
  });

  describe('pattern validation', () => {
    it('patternに一致しない場合はエラー', () => {
      const parameters: DataElement[] = [
        {
          id: 'userName',
          name: 'ユーザー名',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            pattern: '^[a-zA-Z0-9]+$',
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ userName: 'user@name' });

      expect(result.success).toBe(false);
    });
  });

  describe('backward compatibility', () => {
    it('validation未定義でも動作する', () => {
      const parameters: DataElement[] = [
        {
          id: 'legacyField',
          name: 'レガシー',
          valueType: 'text',
          value: 'default',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          // validation未定義
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ legacyField: 'any value' });

      expect(result.success).toBe(true);
    });
  });
});
```

**テスト実行**: `pnpm --filter frontend-v3 test`
→ **Red**: テストが失敗（まだ実装していない）

#### 4.3 実装（Green）

**ファイル**: `apps/frontend-v3/src/features/promarker/types/api.ts` - ValidationRule追加
**ファイル**: `apps/frontend-v3/src/features/promarker/schemas/parameter.ts` - 完全書き換え

**テスト実行**: `pnpm --filter frontend-v3 test`
→ **Green**: すべてのテストが通過

#### 4.4 統合テスト（E2E）

**ファイル**: `packages/e2e/tests/promarker-validation.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('ProMarker Validation - Dynamic from Stencil', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/promarker');
    await page.waitForLoadState('networkidle');
  });

  test('バリデーション定義に基づいて入力チェックされる', async ({ page }) => {
    // カテゴリ選択
    await page.selectOption('[data-testid="category-select"]', '/samples');
    await page.waitForTimeout(500);

    // ステンシル選択
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world');
    await page.waitForTimeout(500);

    // シリアル選択
    await page.selectOption('[data-testid="serial-select"]', '250913A');
    await page.waitForTimeout(1000);

    // パラメータが表示されることを確認
    const userNameField = page.locator('input[name="userName"]');
    await expect(userNameField).toBeVisible();

    // 必須フィールドを空にしてみる
    await userNameField.clear();
    await userNameField.blur();

    // エラーメッセージが表示されることを確認
    await expect(page.locator('text=/必須/')).toBeVisible();

    // Generateボタンがdisabledであることを確認
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeDisabled();

    // 正しい値を入力
    await userNameField.fill('TestUser');

    // Generateボタンが有効になることを確認
    await expect(generateBtn).toBeEnabled();
  });

  test('validation未定義のステンシルでも動作する（後方互換性）', async ({ page }) => {
    // validation未定義のステンシルを選択
    // ... テストロジック
  });

  test.describe('spring-boot-service sample - 包括的なvalidationテスト', () => {
    test.beforeEach(async ({ page }) => {
      await page.selectOption('[data-testid="category-select"]', '/samples/springboot');
      await page.waitForTimeout(500);
      await page.selectOption('[data-testid="stencil-select"]', '/samples/springboot/spring-boot-service');
      await page.waitForTimeout(500);
      await page.selectOption('[data-testid="serial-select"]', '250101A');
      await page.waitForTimeout(1000);
    });

    test('Javaパッケージ名の正規表現バリデーション', async ({ page }) => {
      const packageGroupField = page.locator('input[name="packageGroup"]');
      
      // 不正な形式：大文字開始
      await packageGroupField.fill('Com.example');
      await packageGroupField.blur();
      await expect(page.locator('text=/小文字英数字とドット/')).toBeVisible();
      
      // 正しい形式
      await packageGroupField.fill('com.example');
      await packageGroupField.blur();
      await expect(page.locator('text=/小文字英数字とドット/')).not.toBeVisible();
    });

    test('キャメルケース形式の正規表現バリデーション', async ({ page }) => {
      const serviceIdField = page.locator('input[name="serviceId"]');
      
      // 不正な形式：大文字開始
      await serviceIdField.fill('UserService');
      await serviceIdField.blur();
      await expect(page.locator('text=/ローワーキャメルケース/')).toBeVisible();
      
      // 正しい形式
      await serviceIdField.fill('userService');
      await serviceIdField.blur();
      await expect(page.locator('text=/ローワーキャメルケース/')).not.toBeVisible();
    });

    test('バージョン番号「1.0」が有効（min(3)削除の確認）', async ({ page }) => {
      const versionField = page.locator('input[name="version"]');
      
      // 2文字のバージョン番号が許容されることを確認
      await versionField.fill('1.0');
      await versionField.blur();
      
      // エラーが表示されないことを確認
      await expect(page.locator('[data-testid="version-error"]')).not.toBeVisible();
      
      // Generateボタンが有効であることを確認（全必須フィールド入力後）
      await fillAllRequiredFields(page);
      const generateBtn = page.locator('[data-testid="generate-btn"]');
      await expect(generateBtn).toBeEnabled();
    });

    test('必須フィールドと任意フィールドの区別', async ({ page }) => {
      // 必須フィールド（packageGroup）を空にする
      const packageGroupField = page.locator('input[name="packageGroup"]');
      await packageGroupField.clear();
      await packageGroupField.blur();
      await expect(page.locator('text=/必須/')).toBeVisible();
      
      // 任意フィールド（author）を空にしてもエラーが出ない
      const authorField = page.locator('input[name="author"]');
      await authorField.clear();
      await authorField.blur();
      await expect(authorField).not.toHaveAttribute('aria-invalid', 'true');
    });

    test('カスタムエラーメッセージが正しく表示される', async ({ page }) => {
      const packageGroupField = page.locator('input[name="packageGroup"]');
      
      // 不正な形式を入力
      await packageGroupField.fill('123invalid');
      await packageGroupField.blur();
      
      // カスタムエラーメッセージが表示されることを確認
      await expect(page.locator('text=小文字英数字とドット（.）のみ使用可能です（例：com.example）')).toBeVisible();
    });

    test('全パラメータを正しく入力してコード生成成功', async ({ page }) => {
      // 全パラメータに有効な値を入力
      await page.locator('input[name="packageGroup"]').fill('com.example');
      await page.locator('input[name="applicationId"]').fill('sampleApp');
      await page.locator('input[name="serviceId"]').fill('userService');
      await page.locator('input[name="serviceName"]').fill('ユーザーサービス');
      await page.locator('input[name="eventId"]').fill('get');
      await page.locator('input[name="eventName"]').fill('取得');
      await page.locator('input[name="version"]').fill('1.0');
      await page.locator('input[name="author"]').fill('ProMarker Platform');
      await page.locator('input[name="vendor"]').fill('Open Source Community');
      
      // Generateボタンをクリック
      const generateBtn = page.locator('[data-testid="generate-btn"]');
      await expect(generateBtn).toBeEnabled();
      await generateBtn.click();
      
      // 生成成功を確認
      await expect(page.locator('text=/生成が完了しました/')).toBeVisible({ timeout: 10000 });
    });
  });
});
```

**テスト実行**: `pnpm --filter e2e test promarker-validation.spec.ts`

#### 4.5 リファクタリング（Refactor）
- コードの重複を削除
- 可読性を向上
- パフォーマンスを最適化

#### 4.6 コミット

```bash
git add apps/frontend-v3/src/features/promarker/types/api.ts
git add apps/frontend-v3/src/features/promarker/schemas/parameter.ts
git add apps/frontend-v3/src/features/promarker/schemas/parameter.test.ts
git add packages/e2e/tests/promarker-validation.spec.ts
git commit -m "feat(promarker): 動的バリデーションスキーマ実装 (refs #29)

- ステンシル定義からバリデーションルール取得
- ハードコードされたルールを完全削除
- TDDでテストファースト実装
- E2Eテストで動作確認"
```

---

### Step 5: バックエンドバリデーション強化（TDD・オプション）

**優先度**: 中  
**期間**: 1日

#### 5.1 テスト作成（Red）

**新規ファイル**: `backend/src/test/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceValidationTest.java`

```java
package jp.vemi.mirel.apps.mste.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GenerateServiceValidationTest {
    
    @Autowired
    private GenerateServiceImp generateService;
    
    @Test
    void testValidateRequiredField() {
        Map<String, Object> param = new HashMap<>();
        param.put("stencilCanonicalName", "/samples/hello-world");
        param.put("serialNo", "250913A");
        param.put("userName", "");  // 必須だが空
        
        List<String> errors = generateService.validate(param);
        
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("ユーザー名")));
    }
    
    @Test
    void testValidateMinLength() {
        Map<String, Object> param = new HashMap<>();
        param.put("stencilCanonicalName", "/samples/hello-world");
        param.put("serialNo", "250913A");
        param.put("userName", "a");  // minLength=2だが1文字
        
        List<String> errors = generateService.validate(param);
        
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("文字以上")));
    }
    
    @Test
    void testValidatePattern() {
        Map<String, Object> param = new HashMap<>();
        param.put("stencilCanonicalName", "/samples/hello-world");
        param.put("serialNo", "250913A");
        param.put("userName", "user@name");  // パターン不一致
        
        List<String> errors = generateService.validate(param);
        
        assertFalse(errors.isEmpty());
    }
    
    @Test
    void testValidateValidInput() {
        Map<String, Object> param = new HashMap<>();
        param.put("stencilCanonicalName", "/samples/hello-world");
        param.put("serialNo", "250913A");
        param.put("userName", "TestUser");
        param.put("message", "Hello");
        param.put("language", "ja");
        
        List<String> errors = generateService.validate(param);
        
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testBackwardCompatibility() {
        // validation未定義のステンシルでもエラーにならない
        Map<String, Object> param = new HashMap<>();
        param.put("stencilCanonicalName", "/legacy/stencil");
        param.put("serialNo", "000000A");
        
        assertDoesNotThrow(() -> {
            generateService.validate(param);
        });
    }
}
```

**テスト実行**: `./gradlew test --tests GenerateServiceValidationTest`
→ **Red**: テストが失敗（まだバリデーションロジックを実装していない）

#### 5.2 実装（Green）

**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceImp.java`

設計方針セクションのコードを実装。

**テスト実行**: `./gradlew test`
→ **Green**: すべてのテストが通過

#### 5.3 統合テスト（Red → Green）

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GenerateApiValidationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testGenerateWithInvalidInput() {
        Map<String, Object> request = Map.of(
            "content", Map.of(
                "stencilCategoy", "/samples",
                "stencilCanonicalName", "/samples/hello-world",
                "serialNo", "250913A",
                "userName", ""  // 必須だが空
            )
        );
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/apps/mste/api/generate",
            request,
            Map.class
        );
        
        Map<String, Object> body = response.getBody();
        List<String> errors = (List<String>) body.get("errors");
        
        assertNotNull(errors);
        assertFalse(errors.isEmpty());
    }
}
```

#### 5.4 コミット

```bash
git add backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceImp.java
git add backend/src/test/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceValidationTest.java
git commit -m "feat(promarker): サーバーサイドバリデーション強化 (refs #29)

- ステンシル定義ベースのバリデーション実装
- required, minLength, maxLength, pattern対応
- TDDで実装
- 多層防御の実現"
```

---

### Step 6: 総合テスト＆検証（TDD継続）

**優先度**: 高  
**期間**: 1-2日

#### 6.1 統合テストスイート

**新規ファイル**: `backend/src/test/java/jp/vemi/mirel/apps/mste/ValidationIntegrationTestSuite.java`

```java
@Suite
@SelectClasses({
    ValidationRuleTest.class,
    StencilSettingsValidationTest.class,
    SuggestServiceValidationTest.class,
    GenerateServiceValidationTest.class,
    SuggestApiValidationTest.class,
    GenerateApiValidationIntegrationTest.class
})
public class ValidationIntegrationTestSuite {
    // すべてのバリデーション関連テストを一括実行
}
```

**実行**: `./gradlew test --tests ValidationIntegrationTestSuite`

#### 6.2 E2Eテストスイート

**新規ファイル**: `packages/e2e/tests/validation-test-suite.spec.ts`

```typescript
import { test } from '@playwright/test';

test.describe('ProMarker Validation - Complete Test Suite', () => {
  // hello-worldステンシルのテスト
  test.describe('hello-world stencil', () => {
    // ... 各種バリデーションテスト
  });

  // Spring Boot Serviceサンプルステンシルのテスト（包括的なvalidation定義）
  test.describe('spring-boot-service sample stencil', () => {
    // ... 各種バリデーションテスト
    // - Javaパッケージ名の正規表現
    // - キャメルケースの正規表現
    // - バージョン番号の形式
    // - 必須/任意フィールド
    // - カスタムエラーメッセージ
  });

  // 後方互換性テスト
  test.describe('Backward compatibility', () => {
    // ... validation未定義のステンシル
  });

  // エラーメッセージ確認
  test.describe('Error messages', () => {
    // ... カスタムエラーメッセージ表示確認
  });
});
```

**実行**: `pnpm --filter e2e test validation-test-suite.spec.ts`

#### 6.3 パフォーマンステスト

**新規ファイル**: `backend/src/test/java/jp/vemi/mirel/apps/mste/ValidationPerformanceTest.java`

```java
@SpringBootTest
class ValidationPerformanceTest {
    
    @Test
    void testValidationPerformance() {
        // 大量のパラメータでパフォーマンス測定
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            // バリデーション実行
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 1000回のバリデーションが1秒以内に完了することを確認
        assertTrue(duration < 1000, "Validation should complete in less than 1 second");
    }
}
```

#### 6.4 テストカバレッジ確認

```bash
# バックエンド
./gradlew jacocoTestReport
open backend/build/reports/jacoco/test/html/index.html

# フロントエンド
pnpm --filter frontend-v3 test:coverage
open apps/frontend-v3/coverage/index.html
```

**目標**: 80%以上のカバレッジ

#### 6.5 最終確認チェックリスト

**ユニットテスト**:
- [ ] ValidationRuleTest - すべて通過
- [ ] StencilSettingsValidationTest - すべて通過
- [ ] SuggestServiceValidationTest - すべて通過
- [ ] GenerateServiceValidationTest - すべて通過
- [ ] parameter.test.ts - すべて通過
- [ ] api.test.ts - すべて通過

**統合テスト**:
- [ ] SuggestApiValidationTest - すべて通過
- [ ] GenerateApiValidationIntegrationTest - すべて通過

**E2Eテスト**:
- [ ] promarker-validation.spec.ts - すべて通過
- [ ] validation-test-suite.spec.ts - すべて通過
- [ ] 既存の全E2Eテスト - すべて通過

**手動テスト**:
- [ ] hello-worldステンシルでバリデーション動作確認
- [ ] spring-boot-serviceサンプルで包括的なvalidation確認
  - Javaパッケージ名の正規表現バリデーション
  - キャメルケース形式の正規表現バリデーション
  - バージョン番号形式のバリデーション
  - 必須/任意フィールドの動作確認
  - カスタムエラーメッセージの表示確認
- [ ] validation未定義のステンシルでも動作（後方互換性）
- [ ] エラーメッセージが分かりやすいか確認
- [ ] パフォーマンスに問題なし

**カバレッジ**:
- [ ] バックエンド: 80%以上
- [ ] フロントエンド: 80%以上

---

## 📊 TDDのメリット

### 本プロジェクトにおけるTDDの効果

1. **品質保証**
   - バグの早期発見
   - リグレッションの防止
   - 仕様の明確化

2. **設計の改善**
   - テスタブルなコード
   - 疎結合な設計
   - インターフェースの明確化

3. **ドキュメント化**
   - テストがドキュメントの役割
   - 使用例が明確
   - 期待される動作が明示的

4. **リファクタリングの安全性**
   - テストが安全網
   - 自信を持って変更可能
   - パフォーマンス改善も安心

5. **チーム開発の効率化**
   - コードレビューが容易
   - 新メンバーのオンボーディング
   - 変更の影響範囲が明確

---

## 🔄 TDD実践のポイント

### テスト作成の順序

1. **失敗するテストを書く（Red）**
   ```bash
   # テストが失敗することを確認
   pnpm test  # または ./gradlew test
   ```

2. **最小限のコードで通す（Green）**
   ```bash
   # テストが通ることを確認
   pnpm test  # または ./gradlew test
   ```

3. **リファクタリング（Refactor）**
   ```bash
   # リファクタリング後もテストが通ることを確認
   pnpm test  # または ./gradlew test
   ```

### コミットのタイミング

- **Red → Green**: 各ステップでコミット
- **Refactor**: リファクタリング後にコミット
- **コミットメッセージ**: TDDのサイクルを明記

```bash
git commit -m "test(promarker): validationルールのテスト追加 (Red) (refs #29)"
git commit -m "feat(promarker): validationルール実装 (Green) (refs #29)"
git commit -m "refactor(promarker): バリデーションコード整理 (Refactor) (refs #29)"
```

### CI/CDとの統合

**.github/workflows/test.yml**:
```yaml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      # Backend tests
      - name: Run backend tests
        run: ./gradlew test
      
      # Frontend tests  
      - name: Run frontend tests
        run: pnpm --filter frontend-v3 test
      
      # E2E tests
      - name: Run E2E tests
        run: pnpm --filter e2e test
      
      # Coverage report
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

## ✅ TDDチェックリスト

### 各ステップで確認すること

**Red（テスト作成）**:
- [ ] テストが失敗することを確認
- [ ] テストが意図した動作を検証している
- [ ] テストが読みやすく理解しやすい
- [ ] エッジケースもカバーしている

**Green（実装）**:
- [ ] テストが通ることを確認
- [ ] 最小限のコードで実装
- [ ] 過剰な実装をしていない
- [ ] すべてのテストが通る

**Refactor（リファクタリング）**:

**ファイル**: `backend/src/main/resources/promarker/stencil/samples/samples/hello-world/250913A/stencil-settings.yml`

```yaml
stencil:
  config:
    categoryId: "/samples"
    categoryName: "Sample Stencils"
    id: "/samples/hello-world"
    name: "Hello World Generator"
    serial: "250913A"
    lastUpdate: "2025/11/04"
    lastUpdateUser: "mirelplatform"
    description: |
      シンプルなHello Worldジェネレーターです。
      バリデーション機能のサンプルとしても使用できます。
  dataElement:
    - id: "message"
    - id: "userName" 
    - id: "language"
  dataDomain:
    - id: "message"
      name: "メッセージ"
      value: "Hello, World!"
      type: "text"
      placeholder: "メッセージを入力してください"
      note: "テンプレートで使用されるメッセージです。"
      validation:
        required: false
        minLength: 1
        maxLength: 200
    - id: "userName"
      name: "ユーザー名"
      value: "Developer"
      type: "text"
      placeholder: "ユーザー名を入力してください"
      note: "挨拶に使用されるユーザー名です。"
      validation:
        required: true
        minLength: 2
        maxLength: 50
        pattern: "^[a-zA-Z0-9_-]+$"
        errorMessage: "半角英数字、ハイフン、アンダースコアのみ使用できます"
    - id: "language"
      name: "言語"
      value: "ja"
      type: "select"
      placeholder: "言語を選択してください"
      note: "生成されるメッセージの言語です。"
      validation:
        required: true
        pattern: "^(ja|en)$"
      options:
        - value: "ja"
          text: "日本語"
        - value: "en"
          text: "English"
  codeInfo:
    copyright: Copyright(c) 2025 mirelplatform.
    versionNo: "1.0"
    author: mirelplatform
    vendor: mirelplatform
```

- [ ] テストが通ることを確認
- [ ] コードの重複を削除
- [ ] 変数名・関数名を改善
- [ ] コメントを更新
- [ ] パフォーマンスを最適化

**コミット**:
- [ ] 意味のある単位でコミット
- [ ] コミットメッセージにTDDフェーズを明記
- [ ] PR作成前にすべてのテストが通過

---

## 📋 実装ステップサマリー（TDD版）

| Step | フェーズ | 期間 | テスト数 | 主な成果物 |
|------|---------|------|----------|-----------|
| 1 | 緊急対応 | 即座 | 3+ tests | parameter.test.ts, 修正版parameter.ts |
| 2 | YAML拡張 | 1-2日 | 10+ tests | ValidationRule.java, 更新版YAML |
| 3 | Backend拡張 | 1日 | 5+ tests | DataDomain拡張, SuggestService対応 |
| 4 | Frontend対応 | 1-2日 | 20+ tests | 型定義, 動的スキーマ, E2E |
| 5 | Backend強化 | 1日 | 10+ tests | GenerateService拡張 |
| 6 | 総合テスト | 1-2日 | All tests | 統合テスト, E2E, カバレッジ |

**合計**: 約1-2週間、50+テストケース

---

## 📊 影響範囲

### 変更が必要なファイル

#### バックエンド（5ファイル + オプション1 + サンプルステンシル8ファイル）

**コアファイル**:
1. ✅ `backend/src/main/resources/promarker/stencil/samples/samples/hello-world/250913A/stencil-settings.yml` - validation追加
2. 🆕 `backend/src/main/resources/promarker/stencil/samples/springboot/spring-boot-service/250101A/stencil-settings.yml` - **包括的なvalidation定義サンプル**
3. 🆕 `backend/src/main/java/jp/vemi/ste/domain/model/ValidationRule.java` - 新規クラス
4. ✅ `backend/src/main/java/jp/vemi/ste/domain/model/DataDomain.java` - validation追加
5. ✅ `backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/GenerateServiceImp.java` - バリデーション強化（オプション）

**Spring Boot Serviceサンプルステンシル（8テンプレート）**:
- 📄 `_serviceId.ucc()_Service.java.ftl` - サービスクラス
- 📄 `_serviceId.ucc()_Controller.java.ftl` - コントローラ
- 📄 `_serviceId.ucc()__eventId.ucc()_Request.java.ftl` - リクエストDTO
- 📄 `_serviceId.ucc()__eventId.ucc()_Response.java.ftl` - レスポンスDTO
- 📄 `_serviceId.ucc()__eventId.ucc()_ParamModel.java.ftl` - パラメータモデル
- 📄 `_serviceId.ucc()__eventId.ucc()_ResultModel.java.ftl` - 結果モデル
- 📄 `_serviceId.ucc()_Mapper.java.ftl` - MyBatisマッパー
- 📄 `_serviceId.ucc()_Mapper.xml.ftl` - MyBatis XMLマッピング
- 📖 `README.md` - 汎化手順とテスト用途

**注**: 
- 本番環境の既存ステンシルへのvalidation追加は、各ステンシル作成者が必要に応じて実施してください
- `spring-boot-service`サンプルは、Spring Boot Service層の典型的なコード構成を生成するサンプルステンシルです
  - **特徴**: 
    - 実践的なSpring Boot構成（Controller、Service、Model、Mapper）
    - 9種類のパラメータに対する包括的なvalidation定義
    - 正規表現、必須/任意、カスタムエラーメッセージのすべてのパターンをカバー
  - **用途**: 
    - validation機能の包括的なテストケース
    - 実践的な使用例のデモンストレーション
    - E2Eテストで実際のSpring Boot構成を検証

#### フロントエンド（3ファイル）
1. ✅ `apps/frontend-v3/src/features/promarker/types/api.ts` - 型定義拡張
2. ✅ `apps/frontend-v3/src/features/promarker/schemas/parameter.ts` - **完全書き換え**
3. ✅ `apps/frontend-v3/src/features/promarker/hooks/useParameterForm.ts` - 初期バリデーション追加

#### ドキュメント（2ファイル）
1. 🆕 `docs/issue/#33/validation-improvement-plan.md` - 本計画書
2. ✅ `.github/docs/api-reference.md` - validation定義の追加

### 後方互換性の確保

**重要**: validation定義が無い古いステンシルでも動作すること

**対策**:
```typescript
// フロントエンド
const validation = param.validation || {};  // デフォルト空オブジェクト
const isRequired = validation.required ?? false;  // デフォルトfalse

// バックエンド
ValidationRule rule = domain.getValidation();
if (rule == null) continue;  // validation未定義はスキップ
```

**確認項目**:
- [ ] validation未定義のステンシルが読み込める
- [ ] validation未定義のパラメータが入力できる
- [ ] 既存のE2Eテストが通過

## 🎯 期待される効果

### Before（現状の問題）

❌ フロントエンドに根拠のないルールがハードコード
```typescript
stringSchema = stringSchema.min(3);  // なぜ3文字？
```

❌ ステンシル作成者が意図したバリデーションを定義できない
- バージョン番号「1.0」が弾かれる
- パッケージ名の形式を指定できない

❌ バリデーションルール変更にコード修正が必要
- フロントエンドのコードを修正
- ビルド・デプロイが必要

### After（修正後の効果）

✅ ステンシル定義がバリデーションの唯一の真実（Single Source of Truth）
```yaml
validation:
  required: true
  minLength: 2
  pattern: "^[a-zA-Z0-9_-]+$"
```

✅ YAML編集だけでバリデーションルールを変更可能
- ステンシル作成者が自由に定義
- コード変更不要

✅ ステンシル作成者が意図したバリデーションを実現
- 必須/任意を明示的に指定
- 適切な文字数制限を設定
- 独自の正規表現パターンを定義
- カスタムエラーメッセージを表示

✅ フロントエンドは定義を受け取って適用するだけ（薄いレイヤー）
- ビジネスロジックをフロントエンドに持たない
- 保守性の向上

### 実例：Spring Boot Serviceサンプルステンシル

汎化された`spring-boot-service`サンプルステンシルでは、以下のような実践的なvalidationが実現されます：

**生成されるファイル構成**:
```
com/
└── example/
    └── sampleApp/
        ├── app/
        │   ├── controller/
        │   │   └── UserServiceController.java
        │   ├── request/
        │   │   └── UserServiceGetRequest.java
        │   └── response/
        │       └── UserServiceGetResponse.java
        └── domain/
            ├── service/
            │   └── UserServiceService.java
            ├── model/
            │   ├── UserServiceGetParamModel.java
            │   └── UserServiceGetResultModel.java
            └── mapper/
                ├── UserServiceMapper.java
                └── UserServiceMapper.xml
```

**validationの実例**:

1. **Javaパッケージ名** (`packageGroup`):
   ```yaml
   validation:
     required: true
     pattern: "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$"
     errorMessage: "小文字英数字とドット（.）のみ使用可能です（例：com.example）"
   ```
   - ✅ `com.example` → 有効
   - ❌ `Com.Example` → エラー（大文字不可）
   - ❌ `123.example` → エラー（数字開始不可）

2. **キャメルケース** (`serviceId`, `applicationId`, `eventId`):
   ```yaml
   validation:
     required: true
     pattern: "^[a-z][a-zA-Z0-9]*$"
     errorMessage: "ローワーキャメルケースで入力してください（例：userService）"
   ```
   - ✅ `userService` → 有効
   - ❌ `UserService` → エラー（大文字開始不可）
   - ❌ `user_service` → エラー（アンダースコア不可）

3. **バージョン番号** (`version`):
   ```yaml
   validation:
     required: false
     minLength: 1
     pattern: "^[0-9]+\\.[0-9]+(\\.[0-9]+)?$"
     errorMessage: "バージョン形式で入力してください（例：1.0、1.0.0）"
   ```
   - ✅ `1.0` → 有効（**min(3)削除により可能に**）
   - ✅ `1.0.0` → 有効
   - ❌ `v1.0` → エラー（文字列プレフィックス不可）

4. **任意フィールド** (`author`, `vendor`):
   ```yaml
   validation:
     required: false
     maxLength: 100
   ```
   - 空欄でも生成可能
   - 最大100文字まで入力可能

## 🔄 移行戦略

### 段階的な移行

1. **Phase 0: 緊急対応**（即座）
   - ハードコードされた min(3) を削除
   - 現在の問題を解決

2. **Phase 1: 基盤整備**（1-2週間）
   - YAML定義拡張
   - バックエンドモデル拡張
   - サンプルステンシルの更新

3. **Phase 2: フロントエンド完全対応**（1-2週間）
   - 型定義拡張
   - バリデーションスキーマ完全書き換え
   - E2Eテスト更新

4. **Phase 3: 全体最適化**（1週間）
   - バックエンドバリデーション強化
   - パフォーマンス最適化
   - ドキュメント整備

### ロールバック計画

万が一問題が発生した場合の切り戻し手順:

1. **フロントエンドのみロールバック**
   - `parameter.ts` を旧版に戻す
   - validation定義を無視する実装に戻す

2. **YAML定義のみロールバック**
   - validation フィールドを削除
   - フロントエンドは後方互換性で動作

3. **完全ロールバック**
   - ブランチを切り戻し
   - 既存のE2Eテストで確認

## 📝 備考

### 関連Issue
- #29 - Migration frontend to React

### 参考資料
- `.github/docs/api-reference.md` - API仕様
- `.github/docs/frontend-architecture.md` - フロントエンド設計
- `backend/src/main/java/jp/vemi/ste/domain/engine/TemplateEngineProcessor.java` - 既存バリデーション

### 今後の拡張可能性

#### 1. より高度なバリデーション
```yaml
validation:
  type: "email"  # 型ベースのバリデーション
  custom: "customValidator1"  # カスタムバリデーター参照
  depends: "otherFieldId"  # 他フィールドとの依存関係
```

#### 2. 動的バリデーション
```yaml
validation:
  requiredIf: "language === 'ja'"  # 条件付き必須
  visibleIf: "mode === 'advanced'"  # 条件付き表示
```

#### 3. 非同期バリデーション
```yaml
validation:
  asyncValidator: "checkDuplicatePackageName"  # API呼び出しで重複チェック
```

## ✅ チェックリスト

### 設計レビュー
- [ ] プロダクトオーナー承認
- [ ] テックリード承認
- [ ] セキュリティレビュー完了

### 実装
- [ ] Step 1: 緊急対応完了
- [ ] Step 2: YAML定義拡張完了
- [ ] Step 3: バックエンドモデル拡張完了
- [ ] Step 4: フロントエンド完全対応完了
- [ ] Step 5: バックエンドバリデーション強化完了（オプション）

### テスト
- [ ] ユニットテスト完了
- [ ] 統合テスト完了
- [ ] E2Eテスト完了
- [ ] 手動テスト完了
- [ ] 後方互換性確認完了

### ドキュメント
- [ ] 本計画書作成完了
- [ ] API仕様書更新完了
- [ ] ステンシル作成ガイド更新完了
- [ ] リリースノート作成完了

### デプロイ
- [ ] ステージング環境デプロイ
- [ ] ステージング環境動作確認
- [ ] 本番環境デプロイ
- [ ] 本番環境動作確認

---

**作成者**: GitHub Copilot 🤖  
**最終更新**: 2025年11月4日
