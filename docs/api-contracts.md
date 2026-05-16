# API Contracts

**Last updated:** 2026-05-16

## Base Paths

| API area | Base path |
| --- | --- |
| Auth | `/api/v1/auth` |
| Users | `/api/v1/users` |
| Consent | `/api/v1/users/me/consent` |
| Profiles | `/api/v1/profiles` |
| Invitations | `/api/v1/invitations` |
| Shared profiles | `/api/v1/shared-profiles` |
| Health records | `/api/v1/health-records` |
| Reference data | `/api/v1/reference-data` |
| Admin auth | `/api/v1/admin/auth` |
| Admin reference data | `/api/v1/admin/reference-data` |
| OCR proxy/API | `/api/ocr` |
| OCR service direct | `/health`, `/ocr` |

## Auth

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Register account |
| POST | `/api/v1/auth/login` | Login and issue session payload |
| POST | `/api/v1/auth/verify-email` | Verify email token |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | Logout/revoke refresh token |
| POST | `/api/v1/auth/forgot-password` | Request reset email |
| POST | `/api/v1/auth/reset-password` | Complete password reset |

## Users And Consent

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/users/me` | Current user profile |
| PUT | `/api/v1/users/me` | Update user profile |
| POST | `/api/v1/users/me/deletion-request` | Request account deletion |
| DELETE | `/api/v1/users/deletion-requests/cancel?token=...` | Public email-token cancellation |
| GET | `/api/v1/users/me/consent` | Current consent state |
| GET | `/api/v1/users/me/consent/active-version` | Active consent version |
| POST | `/api/v1/users/me/consent` | Record consent |

## Profiles, Invitations, Sharing

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/profiles` | List profiles accessible to user |
| POST | `/api/v1/profiles` | Create profile |
| POST | `/api/v1/profiles/ensure-default` | Ensure default profile |
| PUT | `/api/v1/profiles/{profileId}` | Update profile |
| GET | `/api/v1/profiles/{profileId}/health-records` | List profile records |
| POST | `/api/v1/profiles/{profileId}/invitations` | Invite member |
| GET | `/api/v1/profiles/{profileId}/invitations` | List profile invitations |
| DELETE | `/api/v1/profiles/{profileId}/invitations/{invitationId}` | Cancel invitation |
| POST | `/api/v1/profiles/{profileId}/invitations/{invitationId}/resend` | Resend invitation |
| DELETE | `/api/v1/profiles/{profileId}/shares/{viewerId}` | Revoke share |
| POST | `/api/v1/invitations/accept?token=...` | Accept invitation |
| GET | `/api/v1/invitations/incoming` | Pending incoming invitations |
| POST | `/api/v1/invitations/{invitationId}/reject` | Reject invitation |
| GET | `/api/v1/shared-profiles` | List shared profiles |

## Health Records

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/health-records/upload-url` | Create presigned upload URL |
| POST | `/api/v1/health-records/{recordId}/confirm-upload` | Confirm uploaded file |
| GET | `/api/v1/health-records/{recordId}/status` | Get processing status |
| GET | `/api/v1/health-records/{recordId}` | Get record detail |
| GET | `/api/v1/health-records/{recordId}/metrics/{metricName}/explanation` | Legacy path metric explanation |
| GET | `/api/v1/health-records/{recordId}/metrics/explanation?metricName=...` | Preferred metric explanation |
| GET | `/api/v1/health-records/{recordId}/recommendations` | Get recommendations |
| POST | `/api/v1/health-records/{recordId}/confirm` | Confirm analyzed record |
| PUT | `/api/v1/health-records/{recordId}/metrics` | Update extracted metrics |
| DELETE | `/api/v1/health-records/{recordId}` | Soft-delete record |
| GET | `/api/v1/health-records/profiles/{profileId}` | List records by profile |

## Reference Data

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/reference-data/{id}/index` | Index reference data |
| GET | `/api/v1/reference-data/search` | Search reference data |
| GET | `/api/v1/reference-data/ranges` | Query metric ranges |
| GET | `/api/v1/reference-data/metrics` | List metrics |

## Admin

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/admin/auth/login` | Admin login |
| POST | `/api/v1/admin/auth/totp/setup` | TOTP setup |
| POST | `/api/v1/admin/auth/totp/verify` | TOTP verify |
| GET | `/api/v1/admin/reference-data/metrics` | List admin metrics |
| POST | `/api/v1/admin/reference-data/metrics` | Create metric draft/change |
| PUT | `/api/v1/admin/reference-data/metrics/{metricId}` | Update metric |
| DELETE | `/api/v1/admin/reference-data/metrics/{metricId}` | Deactivate metric |
| POST | `/api/v1/admin/reference-data/metrics/{metricId}/reactivate` | Reactivate metric |
| GET | `/api/v1/admin/reference-data/change-sets` | List change sets |
| POST | `/api/v1/admin/reference-data/change-sets/{changeSetId}/approve` | Approve change set |
| POST | `/api/v1/admin/reference-data/change-sets/{changeSetId}/reject` | Reject change set |
| POST | `/api/v1/admin/reference-data/change-sets/{changeSetId}/submit` | Submit change set |
| POST | `/api/v1/admin/reference-data/change-sets/{changeSetId}/publish` | Publish change set |
| GET | `/api/v1/admin/reference-data/config` | Admin reference-data config |

## OCR Service Direct Contract

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | Returns service health and whether EasyOCR reader is loaded |
| POST | `/ocr` | Extracts text from an image URL |

`POST /ocr` accepts:

```json
{
  "image_url": "https://example.com/test-image.png"
}
```

It returns extracted text, average confidence, detected language, processing time in milliseconds, and detected block count.

