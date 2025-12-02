# Agent Guidelines

## Language

- All responses and plans must be in Japanese.

## Project Context

- **Frontend**: `apps/frontend-v3` (React 19 + Vite)
- **UI Package**: `packages/ui` (Radix UI + shadcn/ui)
- **E2E**: `packages/e2e` (Playwright)
- **Backend**: `backend` (Spring Boot 3.3)
- **Package Manager**: `pnpm` (DO NOT use `npm` or `yarn`)
- **Start Command**: Use VS Code Tasks defined in `.vscode/tasks.json` (e.g., "Frontend-v3: Start Vite", "Backend: Start Spring Boot"). DO NOT run `pnpm dev` or `./gradlew bootRun` directly in the terminal unless necessary for debugging outside the standard flow.

## Commit Convention

- Format: `<type>(<scope>): <subject> (refs #<issue>)`
- Example: `feat(frontend): Change home routing to /home (refs #0)`
- Types: `feat|fix|docs|style|refactor|perf|test|chore|ci|build|revert`
- Scopes: `frontend`, `backend`, `ui`, `e2e`, `infra`, `modal`, `deps`

## Testing & Verification

- **Frontend**: `pnpm --filter frontend-v3 test`
- **E2E**: `pnpm test:e2e`
- **Backend**: `./gradlew :backend:test`
- **UI Package**: `pnpm --filter @mirel/ui typecheck`

## Domain Guidelines

### Frontend (`apps/frontend-v3`)

- **Routing**: Managed in `src/app/router.config.tsx`.
- **API**: Use `src/lib/api/client.ts` (axios instance) via `/mapi/*`.
- **Styles**: Use Tailwind CSS with HSL tokens from `@mirel/ui/theme/index.css`.
- **State**: Use Zustand/TanStack Query in feature directories (`src/features/**/store.ts`).
- **Review Checklist**:
  - [ ] UI changes have screenshots/demos.
  - [ ] Store updates are immutable.
  - [ ] `pnpm --filter frontend-v3 build`, `lint`, and `test` pass.

### UI Package (`packages/ui`)

- **Policy**: Wrap Radix primitives, preserve accessibility attributes (`role`, `aria-*`), use HSL tokens.
- **Review Checklist**:
  - [ ] `pnpm --filter @mirel/ui typecheck` and `test` pass.
  - [ ] Check for breaking changes in `apps/frontend-v3`.

### E2E (`packages/e2e`)

- **Policy**: Use `page-objects`, do not hardcode `E2E_BASE_URL` (let config handle dev server), mock secrets.
- **Review Checklist**:
  - [ ] `pnpm test:e2e` passes locally.
  - [ ] Test names describe user operations in Japanese.

## General Rules

- **Process**:
  1. Manage TODO list in `task.md`.
  2. Read files.
  3. Apply changes.
  4. Run verification (using VS Code Tasks for servers).
  5. Report results with "Powered by Antigravity 🤖".
- **Artifacts**: Keep them concise.
- **Communication**: Concise Japanese.
