# Component Inventory

**Last updated:** 2026-05-23

## Web App Pages

| Area | Files |
| --- | --- |
| Auth | `login`, `register`, `forgot-password`, `reset-password`, `verify-email` pages under `apps/web/src/app/(auth)` |
| Dashboard | `home`, `profiles`, `health-records` pages under `apps/web/src/app/(dashboard)` |
| Admin | `admin/login`, admin dashboard, audit log, reference data pages under `apps/web/src/app/admin` |
| Account deletion | `apps/web/src/app/cancel-deletion`, `settings/delete-account` |

## Web Components

| Category | Components |
| --- | --- |
| Consent | `ConsentModal` |
| Health records | `DeleteRecordModal`, `HealthMetricCard` |
| Profiles | `CreateProfileModal`, `EditProfileModal`, `InviteMemberModal`, `ProfileCard`, `ProfileScopeSelector` |
| Settings | `SettingsAccountNav`, `SettingsAccountSidebar`, `SettingsDirectContactCard` |
| Upload/OCR | `UploadButton`, `OcrFailureScreen` |
| Layout | `DashboardPageShell`, root providers |

## Web State And API Layer

- `apps/web/src/stores/authStore.ts` owns persisted auth/session state.
- `apps/web/src/lib/api/apiClient.ts` owns Axios setup, bearer token injection, refresh retry queue, and profile-access-revoked redirect behavior.
- `apps/web/src/lib/api/routes.ts` re-exports shared API paths and adds frontend-only routes.
- `apps/web/src/hooks/useAuthBootstrap.ts` and related hooks coordinate client bootstrap behavior.

## Dashboard Workflow Components (Epic 11.4)

| Component | Import | Consumers |
| --- | --- | --- |
| `ProfileScopeSelector` | `@/components/features/profiles/ProfileScopeSelector` | `follow-up-reminders/page.tsx`, `visit-summary/page.tsx` |
| `SettingsAccountSidebar` | `@/components/features/settings/SettingsAccountSidebar` | `settings/privacy`, `security`, `change-password`, `about`, `notifications`, `profile`, `delete-account` |
| `SettingsAccountNav` | `@/components/features/settings/SettingsAccountNav` | *(dùng qua `SettingsAccountSidebar`)* |
| `SettingsDirectContactCard` | `@/components/features/settings/SettingsDirectContactCard` | `settings/privacy`, `security`, `change-password`, `about`, `profile`, `notifications`, `delete-account` |

## Shared Package

- API constants: `packages/shared/constants/api.ts`
- Consent/status/error constants: `packages/shared/constants/`
- Auth/profile/user Zod schemas: `packages/shared/schemas/`
- Shared config/types barrel exports: `packages/shared/index.ts`
