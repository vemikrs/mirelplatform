import { Card, Badge, Button } from '@mirel/ui';
import { AlertTriangle, CheckCircle, Circle, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * SaaS実装ステータスページ
 * Phase 1-4の実装状況を表示
 */
export function SaaSStatusPage() {
  return (
    <div className="container mx-auto p-8 max-w-6xl">
      {/* Developer Warning Banner */}
      <div 
        className="mb-8 p-4 rounded-xl border flex items-start gap-3"
        style={{
          background: 'hsl(var(--warning) / 0.1)',
          borderColor: 'hsl(var(--warning) / 0.3)',
        }}
      >
        <AlertTriangle className="size-5 text-amber-600 flex-shrink-0 mt-0.5" />
        <div>
          <h3 className="font-semibold text-amber-800">開発者向けページ</h3>
          <p className="text-sm text-amber-700 mt-1">
            このページはSaaS化実装の進捗状況を開発チーム・ステークホルダーと共有するためのものです。
            実装完了後は削除または非公開化を検討してください。
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold">ProMarker SaaS化対応 実装状況</h1>
        <Button asChild variant="outline">
          <Link to="/admin/features">
            フィーチャーフラグ管理
            <ArrowRight className="size-4 ml-2" />
          </Link>
        </Button>
      </div>

      {/* Phase 1: データモデル */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4 text-green-600">✅ Phase 1: データモデル・マイグレーション</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>User エンティティ拡張（email, displayName, firstName, lastName等）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>Tenant エンティティ拡張（displayName, description, settings等）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>UserTenant エンティティ作成（マルチテナント関連）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>ApplicationLicense エンティティ作成（FREE/PRO/MAX）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>RefreshToken エンティティ作成（トークン管理）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>AuditLog エンティティ作成（監査ログ）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>全Repositoryインターフェース実装</span>
          </div>
        </div>
      </Card>

      {/* Phase 2: ExecutionContext */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4 text-green-600">✅ Phase 2: ExecutionContext & 認証基盤</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>ExecutionContext 実装（リクエストスコープBean）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>ExecutionContextFilter 実装（コンテキスト自動解決）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>テナントID解決ロジック（Header → JWT → User Default → "default"）</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>@RequireLicense アノテーション実装</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>LicenseCheckAspect（AOP）実装</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>Spring AOP有効化</span>
          </div>
        </div>
      </Card>

      {/* Phase 3: API実装 */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4 text-green-600">✅ Phase 3: バックエンドAPI実装（完了）</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>POST /auth/login - ログイン、JWT発行</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>POST /auth/signup - サインアップ、デフォルトテナント付与</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>POST /auth/refresh - トークンリフレッシュ</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>POST /auth/logout - ログアウト</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>POST /auth/switch-tenant - テナント切替</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>GET /auth/me - 現在のユーザー情報取得</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>GET /admin/users - 管理者：ユーザー一覧</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>GET /admin/users/:id - 管理者：ユーザー詳細</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>PUT /admin/users/:id - 管理者：ユーザー更新</span>
          </div>
        </div>
      </Card>

      {/* Phase 4: フロントエンド */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4 text-green-600">✅ Phase 4: フロントエンド実装（完了）</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>authStore（Zustand）実装</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>ログイン画面（/login）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>サインアップ画面（/signup）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>OTP認証（Email + 6桁コード）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>OAuth2ログイン（GitHub）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>設定画面（Profile, Security）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>ホームダッシュボード（ライセンス・機能表示）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>フィーチャーフラグ管理画面（/admin/features）</span>
            <Badge variant="success" className="text-xs">新機能</Badge>
          </div>
          <div className="flex items-center gap-2">
            <Circle className="size-4 text-gray-400" />
            <span>管理画面（Users/Tenants/Licenses）</span>
            <Badge variant="neutral" className="text-xs">計画中</Badge>
          </div>
        </div>
      </Card>

      {/* Feature Flag System */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4 text-green-600">✅ フィーチャーフラグ管理システム</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>FeatureFlag エンティティ（mir_feature_flag テーブル）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>FeatureFlagRepository 実装</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>FeatureFlagService CRUD操作</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>AdminFeatureFlagController（/admin/features）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>FeatureController（/features/available, /features/in-development）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>ExecutionContext.hasFeature() メソッド</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>CSV初期データ投入（8件のフィーチャーフラグ）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>ステータス管理（STABLE/BETA/ALPHA/PLANNING/DEPRECATED）</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-green-500" />
            <span>ライセンスティア連携（FREE/TRIAL/PRO/MAX）</span>
          </div>
          <div className="flex items-center gap-2">
            <Circle className="size-4 text-gray-400" />
            <span>カナリアリリース（rolloutPercentage）</span>
            <Badge variant="neutral" className="text-xs">Phase 2+</Badge>
          </div>
          <div className="flex items-center gap-2">
            <Circle className="size-4 text-gray-400" />
            <span>ライセンス解決戦略（licenseResolveStrategy）</span>
            <Badge variant="neutral" className="text-xs">Phase 2+</Badge>
          </div>
        </div>
      </Card>

      {/* アーキテクチャ図 */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4">アーキテクチャ概要</h2>
        <div className="bg-gray-50 p-4 rounded-lg font-mono text-sm">
          <pre className="whitespace-pre-wrap">
{`リクエスト
    ↓
ExecutionContextFilter (OncePerRequestFilter)
    ├─ Spring Security → Authentication取得
    ├─ User情報をDBから取得
    ├─ テナントID解決（Header > JWT > User Default > System Default）
    ├─ Tenant情報をDBから取得
    ├─ ApplicationLicense一覧を取得（USER/TENANTスコープ両方）
    └─ ExecutionContext に設定
    ↓
Controller / Service Layer
    ├─ ExecutionContext を @Autowired で参照
    ├─ executionContext.getCurrentUser()
    ├─ executionContext.getCurrentTenant()
    └─ executionContext.hasLicense(app, tier)
    ↓
@RequireLicense AOP
    └─ LicenseCheckAspect がライセンスチェック
    ↓
リクエスト処理`}
          </pre>
        </div>
      </Card>

      {/* 技術スタック */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4">技術スタック</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <h3 className="font-semibold mb-2">Backend</h3>
            <ul className="space-y-1 text-sm">
              <li>• Spring Boot 3.3</li>
              <li>• Java 21</li>
              <li>• JPA/Hibernate</li>
              <li>• Spring AOP</li>
              <li>• Spring Security</li>
            </ul>
          </div>
          <div>
            <h3 className="font-semibold mb-2">Frontend</h3>
            <ul className="space-y-1 text-sm">
              <li>• React 19</li>
              <li>• Vite</li>
              <li>• Zustand (State Management)</li>
              <li>• TanStack Query</li>
              <li>• @mirel/ui (Radix UI)</li>
            </ul>
          </div>
        </div>
      </Card>

      {/* データモデル */}
      <Card className="mb-6 p-6">
        <h2 className="text-2xl font-semibold mb-4">データモデル</h2>
        <div className="bg-gray-50 p-4 rounded-lg font-mono text-xs">
          <pre className="whitespace-pre-wrap">
{`User ──────< UserTenant >────── Tenant
  │                               │
  │                               │
  └──< ApplicationLicense >───────┘
       (subjectType: USER)   (subjectType: TENANT)

User ────< RefreshToken

User/Tenant ────< AuditLog

エンティティ詳細:
- User: userId, email, displayName, firstName, lastName, passwordHash, attributes, roles
- Tenant: tenantId, tenantName, displayName, description, orgId, settings
- UserTenant: userId, tenantId, roleInTenant (OWNER/MANAGER/MEMBER/GUEST), isDefault
- ApplicationLicense: subjectType (USER/TENANT), subjectId, applicationId, tier (FREE/PRO/MAX)
- RefreshToken: userId, tokenHash, deviceInfo, expiresAt
- AuditLog: userId, tenantId, eventType, resourceType, metadata, ipAddress`}
          </pre>
        </div>
      </Card>
    </div>
  );
}
