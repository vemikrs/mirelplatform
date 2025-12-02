# mirel Studio Form Designer Widget 仕様

> モデル定義に基づく標準 UI コンポーネント（ウィジェット）体系

---

## 1. 概要

mirel Studio Form Designer は、モデル定義に基づき **標準 UI コンポーネント（ウィジェット）** を自動割当てし、ユーザーは必要最小限の調整だけでフォームを構築できる。

---

## 2. ウィジェット体系の全体構造

```
ウィジェット
├── Primitive Widgets（基礎）
├── Composite Widgets（複合）
└── System Widgets（構造）
```

---

## 3. Primitive Widgets（基礎ウィジェット）

基本となるフィールド単体の UI。

| データ型 | ウィジェット | 説明 |
|---------|--------------|------|
| string | TextBox | 文字列入力 |
| number | NumberBox | 数値入力 |
| date | DatePicker | 日付入力 |
| time | TimePicker | 時刻入力 |
| datetime | DateTimePicker | 日時入力 |
| boolean | Checkbox | 真偽値 |
| html | HtmlEditor | HTML 編集 |
| file | FileUpload | ファイル添付 |

**補足：**
- number には数値専用キーボード
- date/time は標準 UI を利用

---

## 4. Composite Widgets（複合ウィジェット）

複合モデル（Domain/Embedded）に対応。

| ウィジェット | 説明 |
|-------------|------|
| EmbeddedForm | サブモデルを展開して入力 |
| TableDetail | 明細行（複数行） |
| TagInput | 可変長タグ入力 |
| TreeSelect | 階層構造の選択 |

**特徴：**
- EmbeddedForm はモデル階層に基づき自動生成
- TableDetail は繰り返し構造を表現可能

---

## 5. System Widgets（構造ウィジェット）

画面構造を形成するためのコンポーネント。

| ウィジェット | 説明 |
|-------------|------|
| Title | 大見出し |
| Subtitle | 小見出し |
| Break | 区切り線 |
| Section | エリア分割 |
| Tabs | タブ構造 |
| Accordion | 折りたたみ |

---

## 6. 自動割当ルール

```
① Model のデータ型 → 標準ウィジェット
② コードグループが存在 → SelectBox
③ relationType = many → TableDetail
④ html → HtmlEditor
⑤ file → FileUpload
```

---

## 7. 権限による表示制御

各ウィジェットには RBAC ロールに基づく表示制御プロパティが存在する。

| プロパティ | 説明 |
|------------|------|
| `visibleTo` | 指定したロールにのみ表示（他ロールでは非表示） |
| `editableTo` | 指定したロールのみ編集可能（他ロールでは ReadOnly） |
| `maskTo` | 指定したロールに対して値をマスク表示（例: `****`） |

これらは Runtime 側で評価され、権限がない場合は DOM 自体が生成されないか、無効化される。

---

## 8. Designer で変更可能な範囲

### 変更可能（フォーム担当者の裁量）

- ラベル
- プレースホルダ
- 表示条件（Flow に引き渡し）
- セクション分割
- グリッド配置（1〜12）
- タブ構造

### 変更不可（Model へ影響）

- データ型
- 必須
- キー
- 複合構造（DomainType）

---

## 8. プレビュー仕様

- Form Designer は左ペインの選択に応じて即時反映
- 表示条件・非表示項目も動的に判定
- Flow の onLoad は実行される

---

## 9. 設計目的

- モデル中心の UI 自動生成
- フォーム作成の生産性向上
- 担当者ごとのばらつきを抑制
- UI 統一性の確保

---

## 関連ドキュメント

- [レイアウトアルゴリズム](./layout-algorithm.md)
- [Modeler 連携](./modeler-sync.md)
- [Modeler 概要](../03_modeler/overview.md)

---

*Powered by Copilot 🤖*
