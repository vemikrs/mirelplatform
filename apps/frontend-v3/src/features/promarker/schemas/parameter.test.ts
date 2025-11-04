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
