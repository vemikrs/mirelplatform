# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository topology

pnpm workspace + Gradle multi-project monorepo. Four loosely-coupled domains — always identify which you are touching before making changes:

| Path | Stack | Purpose |
| --- | --- | --- |
| `backend/` | Spring Boot 3.5 / Java 21 (Gradle) | REST API served under context path `/mipla2`. Root package `jp.vemi` (→ `mirel`, `framework`, `ste`, `extension`). Entry point: `jp.vemi.mirel.MiplaApplication`. |
| `apps/frontend-v3/` | React 19 + Vite 7 + TS, Tailwind 4, Radix, Zustand, TanStack Query | ProMarker / Studio / Mira UI. Features live under `src/features/{promarker,studio,mira,stencil-editor,catalog,...}`. Axios client in `src/lib/api/`. Routes registered in `src/app/router.config.tsx`. |
| `packages/ui/` (`@mireljs/ui-core`, legacy alias `@mirel/ui`) | Radix + shadcn design system | Shared components and theme tokens (`src/theme/index.css`, consumed as `hsl(var(--token))`). |
| `packages/e2e/` | Playwright 1.59 | E2E specs in `tests/specs/promarker-v3/` using page objects in `tests/page-objects/`. Auto-starts backend/frontend. |

Version is pinned in the root `VERSION` file (single source of truth — `backend/build.gradle` reads it at build time; `scripts/sync-version.sh` propagates to package.json files).

## Frontend ↔ Backend wiring (do not break)

- Vite dev server proxies `/mapi/*` → `http://localhost:3000/mipla2/*`. Frontend code MUST call `/mapi/...`, never `http://localhost:3000` directly.
- Example: `POST /mapi/apps/mste/api/generate` → `POST /mipla2/apps/mste/api/generate` in Spring.
- Do not change `server.servlet.context-path=/mipla2` in `backend/src/main/resources/config/application.yml`.
- Responses are unified as `ApiResponse<T>`; exceptions flow through `GlobalExceptionHandler` → `ApiError`.

## Common commands

Backend (run from repo root; `gradlew` is at the root, backend is subproject `:backend`):

```bash
SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun
./gradlew :backend:build          # compile + test + jar
./gradlew :backend:check          # includes tests
./gradlew :backend:test           # JUnit only (forkEvery=50, -Xmx3g)
./gradlew :backend:test --tests 'jp.vemi.mirel.apps.mira.*'   # filter
```

Frontend:

```bash
pnpm --filter frontend-v3 dev            # Vite on :5173
pnpm --filter frontend-v3 build          # tsc -b && vite build
pnpm --filter frontend-v3 lint
pnpm --filter frontend-v3 test           # vitest run
pnpm --filter frontend-v3 test <file>    # single test
pnpm --filter @mireljs/ui-core typecheck # tsc --noEmit
pnpm --filter @mireljs/ui-core test
```

E2E:

```bash
pnpm test:e2e                                                        # all
pnpm --filter @mirelplatform/e2e test tests/specs/promarker-v3/<f>.spec.ts  # one spec
pnpm test:e2e:ui
pnpm --filter @mirelplatform/e2e install:browsers                    # first-time setup
```

Combined scripts (launch both services, monitor, stop): `scripts/start-services.sh`, `scripts/watch-logs.sh`, `scripts/stop-services.sh`.

Local infra for dev (Redis, Postgres+pgvector, Mailpit): `docker compose -f docker-compose.dev.yml up -d`.

## Running long processes (VS Code / Codespaces convention)

The Copilot instructions mandate VS Code Tasks for long-lived servers (`Backend: Start Spring Boot`, `Frontend-v3: Start Vite`, etc.) and forbid `pkill -f node` / `killall`. If you must stop something manually, resolve port → PID → kill (`fuser -k 3000/tcp`, `fuser -k 5173/tcp`). When not in VS Code, prefer `run_in_background` for dev servers.

## Backend architecture notes

- Packages under `jp.vemi/`: `mirel` (product apps — `apps.mste` = ProMarker, `apps.mira` = AI assistant, Studio, etc.), `framework` (cross-cutting: crypto, context, persistence, web), `ste` (template/generation engine), `extension` (pluggable modules).
- Mira AI lives at `jp.vemi.mirel.apps.mira` with `infrastructure/ai/` provider abstraction (`AiProviderClient`, `AiProviderFactory`, `MockAiClient`, `AzureOpenAiClient`, Vertex/OpenAI variants). Config under `mira.ai.*`; set `mira.ai.mock.enabled=true` to develop without a live provider.
- Security (`jp.vemi.mirel.security` + `foundation.web.api.auth`): dual-mode auth — **JWT RS256** (preferred) with keys managed by `JwtKeyManagerService` and encrypted at rest via `AesCryptoService` (AES-256-GCM, master key from env `MIREL_MASTER_KEY`), plus session-based OTP/OAuth2 fallback. Tokens delivered via HttpOnly cookies `accessToken`/`refreshToken`; resolved by `CookieOrHeaderBearerTokenResolver`. Authorization/CSRF rules are in `WebSecurityConfig` — update the `permitAll` list there when adding unauthenticated endpoints.
- Tenant resolution order: `X-Tenant-ID` header → JWT `tenant_id` claim → user default → `"default"` (see `ExecutionContextFilter`).
- Persistence: Spring Data JPA. No Flyway — schema changes must be documented. Postgres (pgvector) for prod/dev, H2 for some tests. Redis used for Spring Session and distributed rate limiting. File storage under `data/storage/` is auto-cleaned after 72h — mock `FileManagementService` in tests.
- Swagger UI: `/mipla2/swagger-ui.html`. Annotate new endpoints with `@Operation`.

## Frontend architecture notes

- Feature-first layout (`src/features/<domain>`); Zustand stores co-located per feature (`stores/*.ts` or `features/**/store.ts`) — do not hoist to global.
- API: build on the shared axios instance in `src/lib/api/client.ts`. Interceptor handles 401 → `clearAuth()` + redirect-with-returnUrl. `ProtectedRoute` validates JWT `exp` (5s buffer).
- Design system: import from `@mireljs/ui-core` (legacy alias `@mirel/ui` still works via Vite alias). Use Tailwind tokens via `hsl(var(--token))`; dialogs stack on `z-110`/`z-120`. Preserve Radix `data-state` / `aria-*`.
- React is deduped via `resolve.dedupe` and `optimizeDeps.force=true` in `vite.config.ts` — do not remove; it prevents duplicate React instances across the workspace.

## Commit / workflow conventions (from `.github/copilot-instructions.md`)

- Commit format: `<type>(<scope>): <subject> (refs #<issue>)`, types `feat|fix|docs|style|refactor|perf|test|chore|ci|build|revert`, scopes include `frontend|backend|ui|e2e|infra|modal|deps`. Subject ≤50 chars, Japanese allowed, issue reference required.
- Never hand-edit `pnpm-lock.yaml`; never commit `dist/`, build outputs, or `.env`.
- Secrets go through `spring.config.import=optional:file:.env`, not committed config.
- PR/Actions live on **GitHub**. Azure DevOps is for Boards/Wiki only — do not touch ADO repos or pipelines.
- Working logs per issue: `docs/issue/#<issue>/*.md`.

## Scope-specific deep dives

When working inside a domain, also read the matching file — each has concrete rules and review checklists:

- `.github/copilot/frontend.md` — frontend-v3 policies, Mira UI wiring
- `.github/copilot/backend.md` — backend policies, Mira AI provider layer
- `.github/copilot/ui-package.md` — design-system conventions
- `.github/copilot/e2e.md` — Playwright setup
- `.github/copilot/security.md` — auth/JWT/CSRF/tenant details
