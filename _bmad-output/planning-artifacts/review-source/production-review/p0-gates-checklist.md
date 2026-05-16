# HealthLens P0 Gates Checklist

Mục tiêu: chỉ các mục dưới đây mới được coi là điều kiện go-live cho production web.

## 1) Functional completeness

### Core record flow
- Pass khi PDF, image, và manual input đều đi qua `upload -> OCR -> review/edit -> confirm -> detail/history`.
- Pass khi user luôn xem được file gốc/original document ở review, processing, failed, và done states.
- Verify bằng E2E cho PDF, image, manual fallback và kiểm tra lại detail/history sau khi confirm.
- Owner: Backend/API + Web + QA.

### Account, consent, delete
- Pass khi auth, consent rõ ràng, profile CRUD, và right-to-delete chạy end-to-end.
- Pass khi cancel-delete và delete request không lộ email/token/PII trong URL hoặc browser history.
- Verify bằng smoke test login/consent/create profile/delete request/cancel-delete và kiểm tra DB cleanup.
- Owner: Backend/API + Web + Privacy.

### Family sharing
- Pass khi invite, accept, revoke, và enforcement quyền truy cập đều đúng.
- Fail nếu token leak hoặc revoke xong mà access vẫn còn.
- Verify bằng E2E owner/viewer trên invite -> accept -> revoke.
- Owner: Backend/API + Web.

## 2) Security / Privacy

### Consent + external OCR controls
- Pass khi external OCR chỉ chạy nếu có consent versioned + retention mode + kill-switch.
- Pass khi audit có thể truy vết provider used, request id, and retention mode.
- Verify bằng revoke consent rồi retry upload và kiểm tra metadata persisted.
- Owner: Backend/API + Security/Privacy.

### Token hygiene
- Pass khi invite/cancel links không mang token trong query params.
- Pass khi admin auth không dùng JS-readable storage.
- Verify bằng kiểm tra URL, history, referer, storage, và redirect path.
- Owner: Backend/API + Web + Security.

### Right-to-delete purge
- Pass khi request quá hạn purge PII, health records, original files, consent logs, và related artifacts trong SLA 72h.
- Pass khi cancellation chỉ hợp lệ trong grace period.
- Verify bằng test tạo request -> chờ scheduler -> kiểm DB/storage/audit trail.
- Owner: Backend/API + DevOps + Privacy.

## 3) Ops readiness

### Telemetry and alerting
- Pass khi API/OCR/Redis/DB/storage có metrics, alerts, structured logs, correlation ids.
- Fail nếu chỉ có health check và failure mode bị silent.
- Verify bằng dashboard review và synthetic OCR failure drill.
- Owner: DevOps/SRE.

### Backup, restore, runbook
- Pass khi có backup policy, restore drill, và incident runbook trước go-live.
- Fail nếu chỉ có deployment docs nhưng chưa chứng minh restore path.
- Verify bằng staged restore và ghi lại RTO/RPO.
- Owner: DevOps + Backend.

### SLOs and deletion SLA
- Pass khi latency/error/deletion targets được định nghĩa và theo dõi.
- Pass khi deletion SLA <= 72h được enforce bởi job và logs.
- Verify bằng một reporting cycle có alert và job logs.
- Owner: Product + DevOps.

## 4) Admin minimum

### Admin auth and RBAC
- Pass khi admin login dùng password + MFA và mọi admin route yêu cầu session hợp lệ.
- Fail nếu dùng `sessionStorage` token hoặc không có MFA.
- Verify bằng positive/negative login tests và route-access checks.
- Owner: Backend/API + Web + Security.

### Reference-data governance
- Pass khi CRUD/import/approval/reject flows live và không bypass được approval.
- Fail nếu direct endpoint có thể publish mà không review.
- Verify bằng seeded end-to-end flow create/update/deactivate/approve/reject.
- Owner: Backend/API + Admin FE.

### Audit log + analytics
- Pass khi audit log viewer filter được và Epic 8.1/8.2/8.3 charts là DB-backed.
- Fail nếu page vẫn chỉ là stub.
- Verify bằng page load với seeded data và query-backed charts.
- Owner: Backend/API + Admin FE + Analytics.

## 5) Top risks if open

- PDF OCR còn image-coupled sẽ tạo sai metrics và review hỏng.
- Token trong URL hoặc JS storage có thể làm lộ quyền truy cập.
- Thiếu telemetry/restore sẽ khiến recovery và incident response không đáng tin.
- Thiếu audit/analytics sẽ làm admin governance mù dữ liệu.
