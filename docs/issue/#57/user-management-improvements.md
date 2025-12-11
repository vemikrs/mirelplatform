# ユーザー管理機能の改善実装 (#57 関連)

実装日: 2025-12-12

## 概要

ユーザー管理（管理者向け）のユーザー編集ダイアログをリッチUIに改修し、モバイル対応を強化しました。

## 実装内容

### 1. バックエンド改修

#### 1.1 AdminUserDto へ avatarUrl フィールド追加

- **ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/dto/AdminUserDto.java`
- **変更内容**: `avatarUrl` フィールドを追加し、ユーザーのアバター画像URLを返却可能にした。

#### 1.2 AdminUserService の変換処理改善

- **ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java`
- **変更内容**: `convertToAdminUserDto` メソッドで、`SystemUser` からアバターURLを取得するように修正。

```java
// SystemUserからavatarUrlを取得
String avatarUrl = null;
if (user.getSystemUserId() != null) {
    SystemUser systemUser = systemUserRepository.findById(user.getSystemUserId()).orElse(null);
    if (systemUser != null) {
        avatarUrl = systemUser.getAvatarUrl();
    }
}
```

### 2. フロントエンド改修

#### 2.1 AdminUser 型への avatarUrl 追加

- **ファイル**: `apps/frontend-v3/src/features/admin/api.ts`
- **変更内容**: `AdminUser` インターフェースに `avatarUrl?: string` を追加。

#### 2.2 UserFormDialog のリッチUI化

- **ファイル**: `apps/frontend-v3/src/features/admin/components/UserFormDialog.tsx`
- **主な変更点**:
  - **2カラムレイアウト**: 左側に基本情報、右側にロール・テナント情報を配置
  - **アバター表示**: ダイアログ上部にユーザーのアバターを表示
  - **カードベースのUI**: 各セクションを `Card` コンポーネントで区切り、視認性を向上
  - **テナント情報表示**: ユーザーの所属テナント一覧とロールを表示
  - **アカウント情報表示**: 最終ログイン、作成日時、メール認証状態などを表示
  - **モバイル対応**: レスポンシブデザインで、モバイルでは1カラムに自動調整

#### 2.3 ユーザー一覧のモバイル最適化

- **ファイル**: `apps/frontend-v3/src/features/admin/pages/UserManagementPage.tsx`
- **主な変更点**:
  - **デスクトップ**: テーブル形式で表示（`md:` ブレークポイント以上）
  - **モバイル**: カード形式で表示（`md` 未満）
  - 横スクロール問題を解決し、すべての情報を適切に表示
  - カードレイアウトでは、アバター、名前、ステータス、ロール、最終ログインを見やすく配置

#### 2.4 Separator コンポーネントの追加

- **ファイル**: `packages/ui/src/components/Separator.tsx`
- **変更内容**: Radix UI の Separator コンポーネントをラップして、@mirel/ui に追加。
- **依存関係**: `@radix-ui/react-separator` を `packages/ui/package.json` に追加。

### 3. コードクリーンアップ

- UserFormDialog と UserManagementPage の ESLint 警告を修正
- 未使用変数の削除
- `useEffect` 内の setState 呼び出しを最適化

## 実装の特徴

### レスポンシブデザイン

- **デスクトップ**: 広い画面でテーブル形式とダイアログの2カラムレイアウト
- **タブレット**: 適切なブレークポイントで自動調整
- **モバイル**: カード形式と1カラムレイアウトで最適化

### UX改善

- アバター表示でユーザーを視覚的に識別しやすく
- カード形式で情報を整理し、可読性を向上
- テナント情報、ロール、アカウント状態を一目で確認可能

### 技術スタック

- Radix UI: Dialog, Card, Avatar, Separator などのアクセシブルなコンポーネント
- Tailwind CSS 4: レスポンシブデザインとユーティリティクラス
- TypeScript: 型安全性の確保

## テスト項目

1. **ユーザー一覧の表示**
   - デスクトップでテーブル形式が正しく表示されるか
   - モバイルでカード形式が正しく表示されるか
   - アバターが適切に表示されるか

2. **ユーザー編集ダイアログ**
   - アバターが上部に表示されるか
   - 2カラムレイアウトが適切に動作するか
   - テナント情報とロールが正しく表示されるか
   - モバイルで1カラムに切り替わるか
   - フォーム送信と更新が正常に動作するか

3. **新規ユーザー作成**
   - アバター表示がない状態で正常に動作するか
   - ロール設定が正しく機能するか

4. **バックエンドAPI**
   - `/admin/users` が avatarUrl を含めて返却するか
   - SystemUser から正しくアバターURLを取得できるか

### 4. テナント割り当て編集機能の実装 (2025-12-12 追加)

#### 4.1 バックエンド実装

**新規DTO追加**:
- **ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/dto/UserTenantAssignmentRequest.java`
- **内容**: テナント割り当てリクエスト用DTO。`List<TenantAssignment>` を持ち、各テナントのID、ロール、デフォルト設定を含む。

**AdminUserService拡張**:
- **ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java`
- **メソッド**: `updateUserTenants(String userId, UserTenantAssignmentRequest request)`
  - 既存のテナント割り当てを削除
  - デフォルトテナントが必ず1つだけ存在することを検証
  - テナントの存在確認
  - 新しいテナント割り当てを作成・保存
  - 更新後のユーザー情報を返却

**AdminUserController拡張**:
- **ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/controller/AdminUserController.java`
- **エンドポイント**: `PUT /admin/users/{id}/tenants`
- AdminRole 必須、リクエストボディで `UserTenantAssignmentRequest` を受け取る

#### 4.2 フロントエンド実装

**APIクライアント拡張**:
- **ファイル**: `apps/frontend-v3/src/features/admin/api.ts`
- **追加型**: `TenantAssignment`, `UserTenantAssignmentRequest`
- **追加関数**: `updateUserTenants(userId, data)` - PUT リクエストで `/admin/users/{id}/tenants` を呼び出し

**UserFormDialog UI拡張**:
- **ファイル**: `apps/frontend-v3/src/features/admin/components/UserFormDialog.tsx`
- **主な変更**:
  - 全テナントリストを `getTenants()` で取得（useQuery）
  - `tenantAssignments` state で選択状態（Map形式）を管理
  - チェックボックスでテナントを選択/解除
  - Select でテナント内ロール（USER/ADMIN/MEMBER）を選択
  - ラジオボタンでデフォルトテナントを設定（1つのみ）
  - 「テナント割り当てを保存」ボタンで `updateUserTenants` API を即座に呼び出し
  - バリデーション: 少なくとも1テナント、デフォルト1つ必須
  - 保存成功後、ダイアログを閉じてリスト再取得

**実装の特徴**:
- ユーザー編集時のみ表示（新規作成時は「ユーザー作成後に割り当て可能」と表示）
- チェックボックスON時、デフォルトロールは「USER」で、他にデフォルトがなければ自動的にデフォルト設定
- 複数テナントを一括設定可能
- UIはスクロール可能で多数のテナントにも対応（max-h-64 overflow-y-auto）

## 残課題・今後の改善案

- ユーザー編集ダイアログからアバターをアップロードする機能の追加
- ロール変更の詳細な権限チェック
- ユーザー一覧のページネーション改善
- テナント割り当て保存後の成功トーストメッセージ追加

## 関連ファイル

### バックエンド
- `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/dto/AdminUserDto.java`
- `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java`

### フロントエンド
- `apps/frontend-v3/src/features/admin/api.ts`
- `apps/frontend-v3/src/features/admin/components/UserFormDialog.tsx`
- `apps/frontend-v3/src/features/admin/pages/UserManagementPage.tsx`
- `packages/ui/src/components/Separator.tsx`
- `packages/ui/src/index.ts`

Powered by Copilot 🤖
