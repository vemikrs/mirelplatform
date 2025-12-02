# Route Protection Analysis & Guest Mode Strategy

## 1. Current Situation Analysis

### 1.1 Root Cause of Unprotected Access
The application currently allows unauthenticated users to access the Home page (`/`) and the ProMarker application (`/promarker`) because:
1.  **Router Configuration**: In `apps/frontend-v3/src/app/router.config.tsx`, the main routes are defined as children of `RootLayout`.
2.  **Layout Behavior**: `RootLayout` (`apps/frontend-v3/src/layouts/RootLayout.tsx`) renders the `<Outlet />` unconditionally. It only hides specific UI elements (User Menu, Tenant Switcher) based on `isAuthenticated`.
3.  **Missing Guards**: The `ProtectedRoute` wrapper is currently **only** applied to:
    -   `/settings/profile`
    -   `/settings/security`

### 1.2 Current "Guest Mode" State
Effectively, the current state **is** a "Guest Mode" where:
-   Guests can view all pages.
-   Guests cannot see user-specific menu items.
-   Guests might encounter errors if pages assume `user` object exists (as seen with the `UserMenu` crash earlier).

## 2. Proposed Strategy

To properly implement security while supporting a potential "Guest Mode", we should categorize routes into three tiers:

### Tier 1: Public Routes (Always Accessible)
-   `/login`, `/signup`, `/password-reset/*` (Auth pages)
-   `/saas-status` (System status)
-   `/auth/*` (OTP/OAuth callbacks)

### Tier 2: Guest/Landing Routes (Configurable)
*These could be public OR protected depending on business requirements.*
-   `/` (Home): Currently acts as a dashboard. Should this be a Landing Page for guests?
-   `/catalog`: UI Catalog.
-   `/sitemap`: Site map.

### Tier 3: Protected Routes (Authenticated Only)
-   `/promarker/*`: The core application.
-   `/settings/*`: User settings.

## 3. Implementation Plan

### Step 1: Refactor Router Configuration
We should introduce a `ProtectedLayout` or use `ProtectedRoute` as a layout wrapper in `router.config.tsx`.

```tsx
// Conceptual Change in router.config.tsx

{
  id: 'app-root',
  path: '/',
  element: <RootLayout />, // Provides Header/Footer
  children: [
    // --- Public / Guest Routes ---
    { index: true, element: <HomePage /> }, // Home remains public?
    { path: 'catalog', element: <UiCatalogPage /> },
    
    // --- Protected Routes ---
    {
      element: <ProtectedRoute><Outlet /></ProtectedRoute>, // Wrap multiple routes
      children: [
        { path: 'promarker', element: <ProMarkerPageWithErrorBoundary /> },
        { path: 'promarker/*', element: <StencilEditor /> },
        { path: 'settings/*', element: ... }
      ]
    }
  ]
}
```

### Step 2: Guest Mode Refinement
If "Guest Mode" implies allowing limited access to ProMarker (e.g., View Only), then Route Guards are too blunt. We would need:
1.  **Feature Flags / Permissions**: Check `isAuthenticated` inside `ProMarkerPage`.
2.  **Read-Only UI**: Disable "Save", "Edit" buttons for guests.

**Recommendation**: For now, assume "Guest Mode" refers to the **Home/Landing page** access, and strictly protect the **ProMarker App** (`/promarker`).

## 4. Next Steps
1.  Confirm which routes belong to which Tier.
2.  Apply `ProtectedRoute` wrapper to the Protected Tier in `router.config.tsx`.
3.  Verify redirection to `/login` works for protected routes.
