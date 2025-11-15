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

  it('1文字でも有効なデフォルト値として機能する', () => {
    const parameters: DataElement[] = [
      {
        id: 'singleChar',
        name: '1文字',
        valueType: 'text',
        value: 'a',
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ singleChar: 'a' });

    expect(result.success).toBe(true);
  });

  it('特定のフィールド名（userName）に依存したハードコードされた正規表現がない', () => {
    // userNameという名前のフィールドが、特別扱いされないことを確認
    const parameters: DataElement[] = [
      {
        id: 'userName',
        name: 'ユーザー名',
        valueType: 'text',
        value: 'user@name',  // 現在のコードでは正規表現で弾かれる
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ userName: 'user@name' });

    // ハードコードされた正規表現がなければ、この値は受け入れられるべき
    expect(result.success).toBe(true);
  });

  it('特定のフィールド名（language）に依存したハードコードされた正規表現がない', () => {
    const parameters: DataElement[] = [
      {
        id: 'language',
        name: '言語',
        valueType: 'text',
        value: 'japanese',  // 現在のコードでは /^[a-z]{2}$/ で弾かれる
        placeholder: '',
        note: '',
        nodeType: 'ELEMENT',
      },
    ];

    const schema = createParameterValidationSchema(parameters);
    const result = schema.safeParse({ language: 'japanese' });

    // ハードコードされた正規表現がなければ、この値は受け入れられるべき
    expect(result.success).toBe(true);
  });
});

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
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('必須');
      }
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

    it('validation未定義の場合、空文字はOK（後方互換性）', () => {
      const parameters: DataElement[] = [
        {
          id: 'legacyField',
          name: 'レガシー',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          // validation未定義
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ legacyField: '' });

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
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('3');
      }
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

    it('minLength=1でバージョン番号「1.0」が有効', () => {
      const parameters: DataElement[] = [
        {
          id: 'version',
          name: 'バージョン',
          valueType: 'text',
          value: '1.0',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            minLength: 1,
            pattern: '^[0-9.]+$',
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ version: '1.0' });

      expect(result.success).toBe(true);
    });
  });

  describe('maxLength validation', () => {
    it('maxLength指定がある場合、それより長いとエラー', () => {
      const parameters: DataElement[] = [
        {
          id: 'shortText',
          name: '短いテキスト',
          valueType: 'text',
          value: '',
          placeholder: '',
          note: '',
          nodeType: 'ELEMENT',
          validation: {
            maxLength: 5,
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ shortText: '123456' });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toContain('5');
      }
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
            errorMessage: '半角英数字のみ使用できます',
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ userName: 'user@name' });

      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0]!.message).toBe('半角英数字のみ使用できます');
      }
    });

    it('patternに一致する場合は成功', () => {
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
            pattern: '^[a-zA-Z0-9_-]+$',
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      const result = schema.safeParse({ userName: 'user_name-123' });

      expect(result.success).toBe(true);
    });
  });

  describe('combined validation', () => {
    it('required + minLength + maxLength + pattern すべて満たす', () => {
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
            minLength: 2,
            maxLength: 50,
            pattern: '^[a-zA-Z0-9_-]+$',
          },
        },
      ];

      const schema = createParameterValidationSchema(parameters);
      
      // 空文字はエラー (required)
      expect(schema.safeParse({ userName: '' }).success).toBe(false);
      
      // 1文字はエラー (minLength)
      expect(schema.safeParse({ userName: 'a' }).success).toBe(false);
      
      // 特殊文字はエラー (pattern)
      expect(schema.safeParse({ userName: 'user@name' }).success).toBe(false);
      
      // 有効な値は成功
      expect(schema.safeParse({ userName: 'user_name' }).success).toBe(true);
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
