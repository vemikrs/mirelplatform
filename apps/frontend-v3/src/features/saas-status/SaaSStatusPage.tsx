import React from 'react';
import { Card } from '@mirel/ui';

/**
 * SaaS実装ステータスページ
 * Phase 1-3の実装状況を表示
 */
export function SaaSStatusPage() {
  return (
    <div className="container mx-auto p-8 max-w-6xl">
      <h1 className="text-4xl font-bold mb-8">ProMarker SaaS化対応 実装状況</h1>

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
        <h2 className="text-2xl font-semibold mb-4 text-blue-600">🔄 Phase 3: バックエンドAPI実装（進行中）</h2>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>認証API DTOs 作成</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>AuthenticationController 基本実装</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>GET /auth/me エンドポイント</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-green-500">✓</span>
            <span>GET /auth/health エンドポイント</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-gray-400">○</span>
            <span>ユーザAPI、テナントAPI、ライセンスAPI（今後実装予定）</span>
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
