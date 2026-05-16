# Component Inventory

**Last updated:** 2026-05-16

## Web App Pages

| Area | Files |
| --- | --- |
| Auth | `login`, `register`, `forgot-password`, `reset-password`, `verify-email` pages under `apps/web/src/app/(auth)` |
| Dashboard | `home`, `profiles`, `health-records` pages under `apps/web/src/app/(dashboard)` |
| Admin | `admin/login`, admin dashboard, audit log, reference data pages under `apps/web/src/app/admin` |
| Account deletion | `apps/web/src/app/cancel-deletion` and settings delete-account tests |

## Web Components

| Category | Components |
| --- | --- |
| Consent | `ConsentModal` |
| Health records | `DeleteRecordModal`, `HealthMetricCard` |
| Profiles | `CreateProfileModal`, `EditProfileModal`, `InviteMemberModal`, `ProfileCard` |
| Upload/OCR | `UploadButton`, `OcrFailureScreen` |
| Layout | `DashboardPageShell`, root providers |

## Web State And API Layer

- `apps/web/src/stores/authStore.ts` owns persisted auth/session state.
- `apps/web/src/lib/api/apiClient.ts` owns Axios setup, bearer token injection, refresh retry queue, and profile-access-revoked redirect behavior.
- `apps/web/src/lib/api/routes.ts` re-exports shared API paths and adds frontend-only routes.
- `apps/web/src/hooks/useAuthBootstrap.ts` and related hooks coordinate client bootstrap behavior.

## Mobile App

The Expo app is present but lighter than the web app.

| Category | Files |
| --- | --- |
| Routes | `apps/mobile/app/index.tsx`, `apps/mobile/src/app/index.tsx`, `apps/mobile/src/app/explore.tsx`, `_layout.tsx` |
| Components | themed text/view, tabs, external link, hint row, animated icon, web badge |
| Hooks/theme | color-scheme hooks, theme constants, global CSS |
| API/state | `apps/mobile/lib/api/index.ts`, `apps/mobile/stores/index.ts` |

## Shared Package

- API constants: `packages/shared/constants/api.ts`
- Consent/status/error constants: `packages/shared/constants/`
- Auth/profile/user Zod schemas: `packages/shared/schemas/`
- Shared config/types barrel exports: `packages/shared/index.ts`

