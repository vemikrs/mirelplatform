# Mira Design Philosophy

Mira の設計思想は、mirelplatform が対象とするエンタープライズ環境の特性を踏まえて定義する。

---

## 1. Complexity Reduction
mirelplatform は多層構造を持ち、機能数が増加しやすい。  
Mira は複雑性を「分かりやすく翻訳する」役割に徹する。

- UI の目的・役割を説明する  
- エラー要因を因果構造で示す  
- 設定手順を段階化して提示する  

「理解にかかるコスト」を最も軽減する存在とする。

---

## 2. Context Awareness
Mira の回答は常に「状況依存」であるべきであり、一般的な AI チャットとは異なる。

- 現在のアプリケーション  
- 画面 ID  
- 操作対象  
- ユーザロール（システムロール + アプリロール）  
- 実行中の設定・操作フロー  

これらの情報を前提に回答を生成することで、  
「mirelplatform 専用のアシスタント」としての価値を確立する。

> **ロール構造**: mirelplatform では `ROLE_ADMIN` / `ROLE_USER`（システムロール）と、Studio 等のアプリケーション固有ロール（`SystemAdmin` / `Builder` / `Operator` / `Viewer`）の2層で権限を管理する。

---

## 3. Trust and Safety
エンタープライズ向け基盤として、信頼性と制御性を重視する。

- 権限に応じた回答制御（[RBAC モデル](../../studio/09_operations/rbac-model.md) 準拠）  
- 秘匿情報の遮断  
- ログの安全保存（`MiraAuditLog`）  
- モデルの切り替え制御

"広く答える AI" ではなく、"適切に答える AI" を目指す。

---

## 4. Assist, Not Replace
Mira はユーザの操作を奪うのではなく、支援に徹する。

- 操作の「道筋」を提示する  
- 実行すべき画面や項目を案内する  
- 判断はユーザが行う  

自動化は段階的に導入し、ユーザの意図を中心に据える。

---

## 5. Seamless Integration
Mira は mirelplatform の一部であり、外付け機能ではない。

- 画面右部のサイドパネル  
- 「?」ヘルプボタンの統合  
- Studio / Workflow の専用エージェント  
- Admin メニューとの統合

「どこでも同じ体験」であることが UX の中核となる。
