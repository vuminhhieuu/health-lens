---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories, step-04-final-validation]
inputDocuments: ['prd.md', 'architecture.md', 'ux-design-specification.md']
workflowType: 'epics'
project_name: health-lens
user_name: ie303
date: '2026-03-17'
---

# health-lens - Epic Breakdown

## Overview

Tài liệu này cung cấp phân tích epics và stories hoàn chỉnh cho dự án health-lens, phân rã các yêu cầu từ PRD, UX Design và Architecture thành các stories có thể triển khai được.

## Requirements Inventory

### Functional Requirements

**Quản Lý Tài Khoản & Hồ Sơ (FR1-FR8)**
- FR1: Người dùng có thể đăng ký tài khoản bằng email và mật khẩu
- FR2: Người dùng có thể đăng nhập và đăng xuất
- FR3: Người dùng có thể đặt lại mật khẩu qua email
- FR4: Người dùng có thể cập nhật thông tin cá nhân (tên, ngày sinh, giới tính)
- FR5: Người dùng có thể tạo và quản lý nhiều hồ sơ sức khỏe trong một tài khoản
- FR6: Người dùng có thể đặt tên và ghi chú cho từng hồ sơ
- FR7: Người dùng có thể yêu cầu xóa toàn bộ dữ liệu tài khoản (right-to-delete, NĐ 13/2023)
- FR8: Người dùng phải xác nhận đồng ý thu thập dữ liệu y tế nhạy cảm trước khi sử dụng

**Upload & Xử Lý Kết Quả Khám (FR9-FR17)**
- FR9: Người dùng có thể upload file PDF kết quả khám
- FR10: Người dùng có thể upload ảnh kết quả khám từ thư viện
- FR11: Người dùng (mobile) có thể chụp ảnh tờ kết quả bằng camera
- FR12: Người dùng có thể xem chỉ số y tế được tự động trích xuất từ ảnh/PDF qua OCR
- FR13: Người dùng có thể xem danh sách chỉ số trích xuất trước khi xác nhận lưu
- FR14: Người dùng có thể chỉnh sửa hoặc nhập thủ công chỉ số khi OCR thất bại
- FR15: Người dùng có thể xem nguồn gốc từng chỉ số (OCR-extracted vs. nhập tay)
- FR16: Người dùng nhận phản hồi rõ ràng khi OCR thất bại, kèm hướng dẫn tiếp theo
- FR17: Người dùng có thể gán kết quả khám cho một hồ sơ cụ thể

**Hiển Thị & Giải Thích Kết Quả (FR18-FR23)**
- FR18: Người dùng có thể xem từng chỉ số kèm giá trị và ngưỡng tham chiếu chuẩn
- FR19: Người dùng có thể xem trạng thái phân loại từng chỉ số (bình thường / cần chú ý / bất thường)
- FR20: Người dùng có thể xem giải thích tiếng Việt đơn giản cho từng chỉ số — ý nghĩa và tầm quan trọng (Mức A)
- FR21: Người dùng nhận gợi ý lối sống và dinh dưỡng dựa trên chỉ số cá nhân (Mức B) kèm disclaimer bắt buộc
- FR22: Người dùng nhận cảnh báo nổi bật khi chỉ số nằm ngoài ngưỡng bất thường
- FR23: Người dùng nhận ngưỡng tham chiếu context-aware theo tuổi và giới tính của hồ sơ

**Lịch Sử & Theo Dõi (FR24-FR28)**
- FR24: Người dùng có thể xem lịch sử kết quả khám của một hồ sơ theo timeline
- FR25: Người dùng có thể xem chi tiết bất kỳ kết quả khám trong lịch sử
- FR26: Người dùng có thể xóa một kết quả khám
- FR27: Người dùng (mobile) có thể xem lịch sử đã đồng bộ khi offline
- FR28: Người dùng có thể thấy dữ liệu được đồng bộ tự động khi kết nối mạng được khôi phục

**Chia Sẻ Gia Đình (FR29-FR32)**
- FR29: Người dùng có thể mời thành viên gia đình theo dõi một hồ sơ qua email
- FR30: Người được mời có thể xem lịch sử và kết quả khám của hồ sơ được chia sẻ qua web
- FR31: Chủ hồ sơ có thể thu hồi quyền truy cập của thành viên gia đình bất kỳ lúc nào
- FR32: Người dùng web nhận cập nhật khi hồ sơ được chia sẻ có kết quả mới

**Quản Trị (FR33-FR38)**
- FR33: Admin có thể đăng nhập admin panel với MFA
- FR34: Admin có thể xem, thêm, sửa, xóa chỉ số y tế và ngưỡng tham chiếu
- FR35: Admin có thể import reference data từ file CSV/JSON
- FR36: Admin có thể approve thay đổi reference data trước khi có hiệu lực
- FR37: Admin có thể xem audit log toàn bộ thay đổi reference data (ai, khi nào, thay đổi gì, nguồn)
- FR38: Admin có thể xem dashboard thống kê bao gồm: tổng số user đăng ký (all-time và theo tháng), Weekly Active Users (WAU), tổng số lượt upload (all-time, theo ngày, theo tuần), tỷ lệ upload thành công vs. thất bại

### NonFunctional Requirements

**Performance**
- NFR-P1: Flow upload → OCR → hiển thị chỉ số hoàn thành trong **≤10 giây** (ảnh chất lượng tốt, mạng bình thường)
- NFR-P2: Tải trang/màn hình (không bao gồm OCR/LLM) trong **≤2 giây** ở 4G/WiFi thông thường
- NFR-P3: LLM explanation hiển thị trong **≤5 giây** (bao gồm cache lookup)
- NFR-P4: Web dashboard cập nhật kết quả mới trong **≤30 giây** (polling) kể từ khi upload thành công

**Security**
- NFR-S1: Dữ liệu y tế mã hóa **at rest** (AES-256) và **in transit** (TLS 1.2+)
- NFR-S2: Mọi truy cập dữ liệu y tế ghi vào **audit log** (ai, khi nào, hành động, IP)
- NFR-S3: Phiên đăng nhập hết hạn sau **30 phút** không hoạt động; refresh token rotation bắt buộc
- NFR-S4: Admin bắt buộc **MFA** — không ngoại lệ
- NFR-S5: Right-to-delete thực thi hoàn toàn trong **≤72 giờ**
- NFR-S6: File ảnh/PDF lưu tách biệt với dữ liệu chỉ số; người dùng có thể xóa file gốc sau OCR

**Scalability**
- NFR-SC1: Hỗ trợ **500 concurrent users** không suy giảm hiệu năng (giai đoạn growth)
- NFR-SC2: OCR pipeline xử lý đồng thời **≥50 jobs** không timeout hoặc mất request
- NFR-SC3: Database và storage mở rộng horizontal hỗ trợ **10x user growth** không cần refactor kiến trúc

**Accessibility**
- NFR-A1: Giao diện hiển thị kết quả y tế tuân thủ **WCAG 2.1 cấp AA**
- NFR-A2: Font size tối thiểu **16px** cho body text trên mobile
- NFR-A3: Phân loại trạng thái chỉ số không phụ thuộc hoàn toàn vào màu sắc — phải có icon hoặc text label bổ sung

**Integration & Reliability**
- NFR-I1: OCR external API timeout **≤15 giây**; vượt quá → thông báo lỗi + hướng dẫn nhập thủ công
- NFR-I2: LLM API retry **≤3 lần** nếu lỗi tạm thời; cache theo hash(chỉ số + reference range)
- NFR-I3: Uptime **≥99%** trong giờ cao điểm (6:00–22:00 ICT)
- NFR-I4: Mobile offline mode hoạt động read-only; UI hiển thị trạng thái offline và thời gian đồng bộ cuối

### Additional Requirements (từ Architecture)

**Starter Template & Project Setup**
- Monorepo structure với apps/api, apps/web, apps/mobile, packages/shared
- Backend: Spring Boot 4.0.3 với Gradle Kotlin DSL, Java 21
- Web: Next.js 16.x với TypeScript, Tailwind CSS, Turbopack
- Mobile: Expo SDK 55 với React Native, TypeScript

**Infrastructure**
- PostgreSQL 16 làm database chính với JSONB cho health records
- Redis 7 cho caching, session store, job queue
- MinIO (dev) / S3 (prod) cho object storage
- Docker Compose cho development, Kubernetes-ready cho production

**Authentication & Security**
- JWT với Access + Refresh tokens (15 phút / 7 ngày)
- TOTP MFA cho admin
- RBAC với resource ownership checking
- Audit logging với Spring AOP

**API Design**
- REST với OpenAPI 3.1
- URL versioning (/api/v1/...)
- RFC 7807 Problem Details cho error responses
- Pre-signed URLs cho file upload

**State Management**
- TanStack Query v5 + Zustand v4 cho frontend
- React Hook Form v7 + Zod v3 cho forms
- Expo SQLite cho mobile offline cache

**CI/CD**
- GitLab CI với stages: test, build, deploy
- Container Registry cho Docker images
- Kubernetes deployments cho staging/production

### UX Design Requirements

**UX-DR1: Design System Implementation**
- Triển khai Shadcn/ui components cho Web (Button, Card, Input, Select, Dialog, Alert, Tabs, Skeleton, Toast, Progress, Avatar, Badge)
- Tạo custom React Native components tương đương cho Mobile với shared design tokens
- Semantic color tokens cho health status: 🟢 Bình thường (#10B981), 🟡 Cần chú ý (#F59E0B), 🔴 Bất thường (#EF4444)

**UX-DR2: Custom Health Components**
- HealthStatusBadge: Hiển thị trạng thái 3 mức với icon + text + color (accessibility triple redundancy)
- HealthMetricCard: Card expandable với metric name, value, unit, status, explanation, recommendation
- HealthResultSummary: Overview card với test type, date, overall status, key metrics preview
- ProfileCard: Avatar, name, latest status, last update info
- FamilyMemberCard: Glanceable status cho family dashboard

**UX-DR3: Camera & Upload UX**
- Camera screen với guide frame overlay "Đặt giấy vào khung"
- Auto-detect edges với green highlight khi aligned
- Preview screen với "Dùng ảnh này" / "Chụp lại" options
- Processing screen với progress bar + text updates (0-2s: "Đang nhận dạng...", 2-5s: "Đang đọc chỉ số...", 5-8s: "Đang phân tích kết quả...", 8-10s: "Hoàn tất!")

**UX-DR4: OCR Failure Recovery UX**
- Partial result screen khi OCR confidence 50-84% với "Vui lòng kiểm tra" highlight
- OCR fail screen khi <50% với 3 options: "Chụp lại rõ hơn", "Nhập thủ công", "Giữ những gì có"
- Camera retry screen với tips: "Đặt giấy phẳng, ánh sáng đều"
- Manual input form với dropdown + number input cho từng chỉ số
- Source tagging UI: "OCR detected" vs "Manual input" badges

**UX-DR5: Result Display UX**
- Progressive disclosure: Summary first → expandable details
- Header: Test type + Date + Profile name (always visible)
- Summary section: Overall status badge + 1-line summary
- Key Metrics section: Top 3-5 abnormal/important values (expanded by default)
- All Results section: Full list (collapsed, tap to expand)
- Recommendations section: 2-3 actionable tips at bottom
- Success celebration: Subtle confetti + "Đã lưu!" toast

**UX-DR6: Family Dashboard UX (Web)**
- Glanceable cards grid layout cho multiple family members
- Color-coded status cards: immediate visual status recognition
- 1-click depth to member details
- AI summary với actionable language: "Nhắc ông..." not just "HbA1c cao"
- Activity feed: Recent uploads from family members

**UX-DR7: Navigation Pattern**
- Mobile: Bottom tabs (Home, Upload, Profiles, Settings) + FAB "+" button
- Web Dashboard: Sidebar navigation + header
- Admin Panel: Sidebar + breadcrumb navigation
- Back + Title header cho detail screens

**UX-DR8: Accessibility Implementation**
- 4.5:1 minimum color contrast (WCAG 2.1 AA)
- 48x48px minimum touch targets
- 2px visible focus indicators
- Proper ARIA labels cho screen readers
- Support 200% font scaling
- Respect prefers-reduced-motion

**UX-DR9: Responsive Design**
- Mobile-first approach
- Breakpoints: sm (640px), md (768px), lg (1024px), xl (1280px), 2xl (1536px)
- Mobile: Single column, stacked cards
- Tablet: 2-column grid for dashboard
- Desktop: 3-column max, sidebar navigation

**UX-DR10: Animation & Feedback**
- Duration tokens: fast (150ms), normal (200ms), slow (300ms)
- Processing animation: Pulsing progress bar với text updates
- Card expand: Smooth height transition (200ms)
- Toast: Slide in from bottom (200ms), auto-dismiss (3s)
- Reduced motion: animation-duration: 0.01ms khi prefers-reduced-motion

### FR Coverage Map

FR1: Epic 1 - Đăng ký tài khoản email/mật khẩu
FR2: Epic 1 - Đăng nhập, đăng xuất và quản lý phiên
FR3: Epic 1 - Đặt lại mật khẩu qua email
FR4: Epic 2 - Cập nhật thông tin cá nhân
FR5: Epic 2 - Tạo và quản lý nhiều hồ sơ sức khỏe
FR6: Epic 2 - Đặt tên và ghi chú cho từng hồ sơ
FR7: Epic 1 - Yêu cầu xóa dữ liệu tài khoản theo NĐ 13/2023
FR8: Epic 1 - Thu thập consent dữ liệu y tế nhạy cảm
FR9: Epic 3 - Upload PDF kết quả khám
FR10: Epic 3 - Upload ảnh từ thư viện
FR11: Epic 3 - Chụp ảnh bằng camera mobile
FR12: Epic 3 - OCR trích xuất chỉ số
FR13: Epic 3 - Xem danh sách chỉ số trước khi lưu
FR14: Epic 3 - Chỉnh sửa/nhập tay khi OCR lỗi
FR15: Epic 3 - Gắn nguồn chỉ số OCR vs Manual
FR16: Epic 3 - Hiển thị hướng dẫn khi OCR thất bại
FR17: Epic 3 - Gán kết quả khám vào hồ sơ cụ thể
FR18: Epic 4 - Hiển thị chỉ số, giá trị và ngưỡng tham chiếu
FR19: Epic 4 - Phân loại trạng thái chỉ số
FR20: Epic 4 - Giải thích tiếng Việt đơn giản (Mức A)
FR21: Epic 4 - Gợi ý lối sống/dinh dưỡng + disclaimer
FR22: Epic 4 - Cảnh báo nổi bật cho chỉ số bất thường
FR23: Epic 4 - Ngưỡng tham chiếu theo tuổi/giới tính
FR24: Epic 5 - Timeline lịch sử kết quả khám
FR25: Epic 5 - Xem chi tiết bản ghi lịch sử
FR26: Epic 5 - Xóa một kết quả khám
FR27: Epic 5 - Mobile offline read-only lịch sử
FR28: Epic 5 - Tự động đồng bộ khi có mạng
FR29: Epic 6 - Mời thành viên gia đình qua email
FR30: Epic 6 - Thành viên xem hồ sơ chia sẻ trên web
FR31: Epic 6 - Thu hồi quyền truy cập
FR32: Epic 6 - Web nhận cập nhật kết quả mới
FR33: Epic 7 - Admin đăng nhập với MFA
FR34: Epic 7 - CRUD chỉ số/ngưỡng tham chiếu
FR35: Epic 7 - Import reference data CSV/JSON
FR36: Epic 7 - Approval workflow cho thay đổi reference data
FR37: Epic 7 - Audit log thay đổi reference data
FR38: Epic 8 - Dashboard thống kê user và upload

## Epic List

### Epic 1: Truy Cập An Toàn và Quyền Riêng Tư Dữ Liệu
Người dùng có thể tạo tài khoản, đăng nhập an toàn, quản lý phiên, đồng ý xử lý dữ liệu y tế và yêu cầu xóa dữ liệu theo quy định pháp lý.
**FRs covered:** FR1, FR2, FR3, FR7, FR8.

### Epic 2: Quản Lý Hồ Sơ Sức Khỏe Cá Nhân
Người dùng có thể duy trì nhiều hồ sơ sức khỏe trong một tài khoản và cập nhật thông tin nhận diện cho từng hồ sơ.
**FRs covered:** FR4, FR5, FR6.

### Epic 3: Thu Thập Kết Quả Khám và Khôi Phục OCR
Người dùng có thể upload/chụp kết quả khám, xem dữ liệu OCR, sửa lỗi nhanh và lưu kết quả chính xác vào đúng hồ sơ.
**FRs covered:** FR9, FR10, FR11, FR12, FR13, FR14, FR15, FR16, FR17.

### Epic 4: Diễn Giải Chỉ Số và Cảnh Báo Sức Khỏe
Người dùng có thể hiểu ý nghĩa chỉ số qua ngưỡng tham chiếu theo ngữ cảnh cá nhân, phân loại mức độ và gợi ý hành động an toàn.
**FRs covered:** FR18, FR19, FR20, FR21, FR22, FR23.

### Epic 5: Lịch Sử Khám và Đồng Bộ Đa Trạng Thái
Người dùng có thể theo dõi timeline khám bệnh, xem lại chi tiết, xóa bản ghi và tiếp tục truy cập dữ liệu khi offline.
**FRs covered:** FR24, FR25, FR26, FR27, FR28.

### Epic 6: Chia Sẻ Gia Đình và Cập Nhật Theo Thời Gian
Người dùng có thể mời người thân theo dõi hồ sơ, kiểm soát quyền truy cập và nhận cập nhật kết quả mới trên web.
**FRs covered:** FR29, FR30, FR31, FR32.

### Epic 7: Quản Trị Dữ Liệu Y Khoa Có Kiểm Soát
Admin có thể đăng nhập MFA, quản lý reference data, import dữ liệu và phê duyệt thay đổi với truy vết đầy đủ.
**FRs covered:** FR33, FR34, FR35, FR36, FR37.

### Epic 8: Dashboard Vận Hành và Chỉ Số Sử Dụng
Admin có thể theo dõi tăng trưởng người dùng, hoạt động hệ thống và hiệu quả luồng upload qua dashboard thống kê.
**FRs covered:** FR38.

## Epic 1: Truy Cập An Toàn và Quyền Riêng Tư Dữ Liệu
Goal: Thiết lập nền tảng truy cập an toàn, tuân thủ quyền riêng tư và đủ điều kiện để các epic sau hoạt động.

### Story 1.1: Thiết lập dự án ban đầu từ starter template

As a nhóm phát triển sản phẩm,
I want khởi tạo monorepo từ starter template chuẩn kiến trúc,
So that các tính năng xác thực và hồ sơ có thể được triển khai nhất quán trên web, mobile và backend.

**Acceptance Criteria:**

**Given** repository trống hoặc chưa chuẩn hóa cấu trúc
**When** thực hiện setup theo starter template đã chọn trong architecture
**Then** tạo đầy đủ apps/api, apps/web, apps/mobile, packages/shared cùng cấu hình build mặc định
**And** các app chạy được ở môi trường dev với dependency đã cài đặt và baseline CI kiểm tra build thành công.

### Story 1.2: Đăng ký tài khoản bằng email và mật khẩu

As a người dùng mới,
I want tạo tài khoản bằng email và mật khẩu hợp lệ,
So that tôi có thể bắt đầu sử dụng HealthLens.

**Acceptance Criteria:**

**Given** người dùng chưa có tài khoản
**When** nhập email hợp lệ, mật khẩu đạt policy và bấm đăng ký
**Then** hệ thống tạo tài khoản mới và gửi email xác thực
**And** trả về lỗi RFC7807 nếu email đã tồn tại.

### Story 1.3: Đăng nhập/đăng xuất với quản lý phiên bảo mật

As a người dùng,
I want đăng nhập và đăng xuất an toàn,
So that tôi truy cập đúng dữ liệu của mình và kiểm soát phiên đăng nhập.

**Acceptance Criteria:**

**Given** tài khoản đã xác thực
**When** đăng nhập đúng thông tin
**Then** hệ thống cấp access token và refresh token theo TTL kiến trúc
**And** tự hết phiên sau 30 phút không hoạt động và hỗ trợ đăng xuất mọi phiên.

### Story 1.4: Đặt lại mật khẩu qua email

As a người dùng quên mật khẩu,
I want yêu cầu đặt lại mật khẩu qua email,
So that tôi khôi phục quyền truy cập mà không cần hỗ trợ thủ công.

**Acceptance Criteria:**

**Given** email đã đăng ký
**When** gửi yêu cầu quên mật khẩu
**Then** hệ thống phát hành reset token có hạn và gửi email đặt lại
**And** token chỉ dùng một lần, hết hạn đúng cấu hình bảo mật.

### Story 1.5: Thu thập consent dữ liệu y tế nhạy cảm

As a người dùng,
I want xác nhận đồng ý xử lý dữ liệu y tế trước khi upload,
So that tôi hiểu rõ phạm vi sử dụng dữ liệu và quyền của mình.

**Acceptance Criteria:**

**Given** người dùng đăng nhập lần đầu hoặc chưa consent
**When** mở tính năng upload/kết quả
**Then** hệ thống hiển thị màn hình consent rõ ràng và yêu cầu xác nhận
**And** ghi log consent theo thời gian, phiên bản nội dung và tài khoản.

### Story 1.6: Quy trình yêu cầu xóa toàn bộ dữ liệu tài khoản

As a người dùng,
I want gửi yêu cầu right-to-delete,
So that dữ liệu của tôi được xóa theo NĐ 13/2023.

**Acceptance Criteria:**

**Given** người dùng đã xác thực danh tính
**When** gửi yêu cầu xóa tài khoản
**Then** hệ thống tạo yêu cầu xóa và thông báo thời hạn hoàn tất <=72 giờ
**And** sau khi hoàn tất, toàn bộ dữ liệu cá nhân/y tế và file gốc bị xóa theo chính sách lưu trữ.

## Epic 2: Quản Lý Hồ Sơ Sức Khỏe Cá Nhân
Goal: Cho phép người dùng quản lý nhiều hồ sơ trong một tài khoản để hỗ trợ theo dõi bản thân và người thân.

### Story 2.1: Cập nhật thông tin cá nhân tài khoản

As a người dùng,
I want cập nhật tên, ngày sinh, giới tính,
So that hệ thống cá nhân hóa hiển thị và diễn giải phù hợp.

**Acceptance Criteria:**

**Given** người dùng đã đăng nhập
**When** chỉnh sửa thông tin cá nhân và lưu
**Then** dữ liệu được cập nhật và hiển thị nhất quán trên web/mobile
**And** validate định dạng ngày sinh và giá trị giới tính hợp lệ.

### Story 2.2: Tạo hồ sơ sức khỏe mới trong tài khoản

As a người dùng,
I want tạo nhiều hồ sơ sức khỏe,
So that tôi quản lý dữ liệu theo từng người.

**Acceptance Criteria:**

**Given** người dùng ở màn hình Profiles
**When** chọn tạo hồ sơ và nhập thông tin bắt buộc
**Then** hệ thống tạo hồ sơ mới gắn ownership đúng tài khoản
**And** hồ sơ mới xuất hiện ngay trong danh sách profile card.

### Story 2.3: Đặt tên và ghi chú cho từng hồ sơ

As a người dùng,
I want chỉnh tên hiển thị và ghi chú từng hồ sơ,
So that tôi phân biệt nhanh giữa nhiều hồ sơ.

**Acceptance Criteria:**

**Given** hồ sơ đã tồn tại
**When** người dùng cập nhật tên hoặc ghi chú
**Then** hệ thống lưu thay đổi và hiển thị ở profile list/detail
**And** giới hạn độ dài ghi chú và chống lưu nội dung rỗng không hợp lệ.

## Epic 3: Thu Thập Kết Quả Khám và Khôi Phục OCR
Goal: Hoàn thiện end-to-end upload + OCR + manual recovery, tránh dead-end khi OCR không chính xác.

### Story 3.1: Upload file PDF và ảnh từ thư viện

As a người dùng,
I want upload PDF hoặc ảnh từ thiết bị,
So that tôi đưa kết quả khám vào hệ thống nhanh chóng.

**Acceptance Criteria:**

**Given** người dùng đã consent và chọn hồ sơ
**When** upload file PDF/JPG/PNG hợp lệ
**Then** hệ thống nhận file qua pre-signed URL và tạo processing job
**And** hiển thị tiến trình upload và lỗi định dạng nếu file không hợp lệ.

### Story 3.2: Chụp ảnh kết quả khám trên mobile với camera UX

As a người dùng mobile,
I want chụp giấy khám với guide frame,
So that ảnh đủ rõ cho OCR.

**Acceptance Criteria:**

**Given** người dùng mở camera capture
**When** căn giấy vào khung và chụp ảnh
**Then** hệ thống hiển thị preview với lựa chọn "Dùng ảnh này" hoặc "Chụp lại"
**And** hỗ trợ overlay căn giấy và edge highlight theo UX-DR3.

### Story 3.3: OCR trích xuất chỉ số và hiển thị danh sách xác nhận

As a người dùng,
I want xem danh sách chỉ số OCR trước khi lưu,
So that tôi kiểm tra tính đúng đắn của kết quả trích xuất.

**Acceptance Criteria:**

**Given** job OCR hoàn tất trong thời gian timeout cấu hình
**When** mở màn hình review chỉ số
**Then** hệ thống hiển thị metric name, value, unit và confidence
**And** cho phép xác nhận lưu theo hồ sơ đã chọn.

### Story 3.4: Chỉnh sửa thủ công khi OCR thiếu hoặc sai

As a người dùng,
I want sửa chỉ số OCR hoặc nhập tay chỉ số bị thiếu,
So that bản ghi cuối cùng phản ánh đúng kết quả khám.

**Acceptance Criteria:**

**Given** màn hình review OCR có dữ liệu thiếu/sai
**When** người dùng chỉnh sửa hoặc thêm chỉ số mới
**Then** hệ thống lưu giá trị đã chỉnh theo schema chuẩn
**And** validate số liệu, đơn vị và ngăn lưu bản ghi không hợp lệ.

### Story 3.5: OCR failure recovery flow có hướng dẫn rõ ràng

As a người dùng,
I want nhận hướng dẫn cụ thể khi OCR thất bại,
So that tôi biết bước tiếp theo thay vì bị kẹt.

**Acceptance Criteria:**

**Given** OCR confidence < ngưỡng chấp nhận hoặc OCR timeout
**When** hệ thống trả về trạng thái thất bại/partial
**Then** hiển thị màn hình recovery với 3 lựa chọn: chụp lại, nhập thủ công, giữ phần đã đọc
**And** kèm tips camera và partial highlight theo UX-DR4.

### Story 3.6: Gán nguồn dữ liệu từng chỉ số và lưu vào đúng hồ sơ

As a người dùng,
I want mỗi chỉ số có tag nguồn OCR/manual và gắn đúng hồ sơ,
So that tôi truy vết độ tin cậy dữ liệu về sau.

**Acceptance Criteria:**

**Given** người dùng xác nhận lưu bản ghi
**When** hệ thống persist health result
**Then** mỗi metric được lưu kèm source tag (OCR/Manual)
**And** bản ghi được gắn profile_id chính xác để phục vụ timeline và chia sẻ.

## Epic 4: Diễn Giải Chỉ Số và Cảnh Báo Sức Khỏe
Goal: Biến dữ liệu thô thành thông tin dễ hiểu, có cảnh báo và khuyến nghị có trách nhiệm.

### Story 4.1: Hiển thị metric card với trạng thái và ngưỡng tham chiếu

As a người dùng,
I want xem từng chỉ số kèm ngưỡng chuẩn và trạng thái,
So that tôi hiểu ngay chỉ số nào cần chú ý.

**Acceptance Criteria:**

**Given** bản ghi khám đã lưu
**When** mở màn hình chi tiết kết quả
**Then** hiển thị HealthMetricCard gồm tên chỉ số, giá trị, đơn vị, reference range, status badge
**And** status không chỉ dùng màu mà có icon/text theo UX-DR1/2/8.

### Story 4.2: Áp dụng ngưỡng tham chiếu theo tuổi và giới tính

As a người dùng,
I want ngưỡng tham chiếu phù hợp hồ sơ,
So that đánh giá bình thường/bất thường chính xác hơn.

**Acceptance Criteria:**

**Given** hồ sơ có tuổi và giới tính hợp lệ
**When** hệ thống phân loại chỉ số
**Then** reference range được chọn theo rule context-aware từ reference data
**And** log nguồn rule đã áp dụng để audit.

### Story 4.3: Giải thích tiếng Việt đơn giản cho từng chỉ số

As a người dùng không chuyên y khoa,
I want đọc giải thích dễ hiểu cho từng chỉ số,
So that tôi hiểu ý nghĩa sức khỏe của kết quả.

**Acceptance Criteria:**

**Given** metric card có dữ liệu đầy đủ
**When** người dùng mở phần giải thích
**Then** hệ thống trả về diễn giải tiếng Việt mức A, rõ ràng và dễ đọc
**And** thời gian phản hồi đáp ứng mục tiêu <=5 giây với cache.

### Story 4.4: Gợi ý lối sống kèm disclaimer bắt buộc

As a người dùng,
I want nhận khuyến nghị dinh dưỡng/lối sống có trách nhiệm,
So that tôi biết hành động tiếp theo an toàn.

**Acceptance Criteria:**

**Given** kết quả có chỉ số cần chú ý hoặc bất thường
**When** hiển thị phần recommendations
**Then** hệ thống đưa 2-3 gợi ý khả thi theo ngữ cảnh
**And** luôn hiển thị disclaimer "không thay thế tư vấn y khoa".

### Story 4.5: Cảnh báo nổi bật và progressive disclosure

As a người dùng,
I want thấy cảnh báo ưu tiên cho chỉ số bất thường,
So that tôi không bỏ sót rủi ro quan trọng.

**Acceptance Criteria:**

**Given** bản ghi có metric bất thường
**When** màn hình kết quả được tải
**Then** key metrics bất thường hiển thị expanded mặc định và có cảnh báo nổi bật
**And** giữ cấu trúc summary-first, details-expand theo UX-DR5.

## Epic 5: Lịch Sử Khám và Đồng Bộ Đa Trạng Thái
Goal: Cho phép truy cập lịch sử liên tục trên mobile/web kể cả khi mạng gián đoạn.

### Story 5.1: Timeline lịch sử theo từng hồ sơ

As a người dùng,
I want xem timeline các lần khám theo hồ sơ,
So that tôi theo dõi diễn tiến sức khỏe theo thời gian.

**Acceptance Criteria:**

**Given** hồ sơ có từ một bản ghi trở lên
**When** mở tab history
**Then** hệ thống hiển thị danh sách theo thứ tự thời gian mới nhất trước
**And** mỗi item có summary status, loại xét nghiệm, ngày khám.

### Story 5.2: Xem chi tiết một bản ghi từ timeline

As a người dùng,
I want mở chi tiết từ một item timeline,
So that tôi xem đầy đủ chỉ số và giải thích của lần khám đó.

**Acceptance Criteria:**

**Given** timeline đã hiển thị
**When** chọn một item lịch sử
**Then** điều hướng tới màn hình chi tiết kết quả tương ứng
**And** dữ liệu hiển thị nhất quán với bản ghi đã lưu.

### Story 5.3: Xóa một kết quả khám

As a người dùng,
I want xóa bản ghi khám không còn cần thiết,
So that lịch sử của tôi luôn chính xác và gọn gàng.

**Acceptance Criteria:**

**Given** người dùng là owner của hồ sơ
**When** xác nhận xóa một bản ghi
**Then** hệ thống xóa mềm bản ghi và cập nhật timeline ngay
**And** ghi audit log cho hành động xóa.

### Story 5.4: Offline read-only cho lịch sử trên mobile

As a người dùng mobile,
I want xem dữ liệu lịch sử khi không có mạng,
So that tôi vẫn tra cứu thông tin đã đồng bộ trước đó.

**Acceptance Criteria:**

**Given** app đã cache dữ liệu trên thiết bị
**When** thiết bị mất kết nối mạng
**Then** timeline và chi tiết vẫn xem được ở chế độ read-only
**And** UI hiển thị trạng thái offline + thời điểm sync cuối.

### Story 5.5: Tự động đồng bộ khi kết nối được khôi phục

As a người dùng,
I want dữ liệu tự sync lại khi có mạng,
So that lịch sử luôn cập nhật mà không cần thao tác thủ công.

**Acceptance Criteria:**

**Given** app đang offline rồi chuyển online
**When** mạng ổn định trở lại
**Then** app tự trigger đồng bộ dữ liệu nền
**And** hiển thị trạng thái đồng bộ thành công hoặc lỗi retry rõ ràng.

## Epic 6: Chia Sẻ Gia Đình và Cập Nhật Theo Thời Gian
Goal: Mở rộng giá trị HealthLens sang use case chăm sóc người thân, nhưng vẫn đảm bảo kiểm soát truy cập.

### Story 6.1: Mời thành viên gia đình vào hồ sơ qua email

As a chủ hồ sơ,
I want gửi lời mời chia sẻ qua email,
So that người thân có thể theo dõi kết quả khám.

**Acceptance Criteria:**

**Given** hồ sơ thuộc quyền sở hữu của tôi
**When** nhập email người nhận và gửi lời mời
**Then** hệ thống tạo invitation token và gửi email mời
**And** lời mời có trạng thái pending/accepted/expired.

### Story 6.2: Người được mời xem dữ liệu hồ sơ chia sẻ trên web

As a thành viên gia đình được mời,
I want xem lịch sử và kết quả hồ sơ được chia sẻ,
So that tôi theo dõi sức khỏe người thân kịp thời.

**Acceptance Criteria:**

**Given** lời mời đã được chấp nhận
**When** người nhận đăng nhập web dashboard
**Then** họ xem được dữ liệu hồ sơ được cấp quyền
**And** không truy cập được hồ sơ không được chia sẻ (RBAC ownership check).

### Story 6.3: Thu hồi quyền truy cập bất cứ lúc nào

As a chủ hồ sơ,
I want thu hồi quyền truy cập của thành viên,
So that tôi kiểm soát chia sẻ dữ liệu linh hoạt.

**Acceptance Criteria:**

**Given** thành viên đang có quyền xem hồ sơ
**When** chủ hồ sơ chọn revoke access
**Then** quyền truy cập bị vô hiệu lực ngay
**And** mọi request sau đó bị từ chối và được ghi audit log.

### Story 6.4: Cập nhật kết quả mới trên web dashboard

As a thành viên gia đình,
I want nhận cập nhật khi có kết quả mới,
So that tôi nắm bắt thay đổi sức khỏe nhanh chóng.

**Acceptance Criteria:**

**Given** hồ sơ chia sẻ có bản ghi mới
**When** người nhận mở dashboard web
**Then** hệ thống hiển thị cập nhật trong <=30 giây theo cơ chế polling
**And** card dashboard thể hiện trạng thái glanceable theo UX-DR6.

## Epic 7: Quản Trị Dữ Liệu Y Khoa Có Kiểm Soát
Goal: Đảm bảo reference data có chất lượng cao, truy vết đầy đủ và giảm rủi ro vận hành.

### Story 7.1: Admin đăng nhập vào panel với MFA bắt buộc

As an admin,
I want đăng nhập bằng mật khẩu + TOTP,
So that khu vực quản trị được bảo vệ đúng mức.

**Acceptance Criteria:**

**Given** tài khoản admin hợp lệ
**When** nhập đúng mật khẩu và mã TOTP
**Then** hệ thống cho phép truy cập admin panel
**And** từ chối toàn bộ luồng không có MFA.

### Story 7.2: CRUD chỉ số y tế và ngưỡng tham chiếu

As an admin,
I want quản lý danh mục chỉ số và reference ranges,
So that hệ thống diễn giải kết quả đúng chuẩn.

**Acceptance Criteria:**

**Given** admin đã đăng nhập hợp lệ
**When** tạo/sửa/xóa chỉ số hoặc ngưỡng
**Then** thay đổi được lưu theo version có hiệu lực
**And** validate schema đơn vị, giới hạn giá trị và phạm vi tuổi/giới tính.

### Story 7.3: Import reference data từ CSV/JSON

As an admin,
I want import dữ liệu chuẩn hàng loạt,
So that cập nhật danh mục nhanh và nhất quán.

**Acceptance Criteria:**

**Given** file import đúng format
**When** upload CSV hoặc JSON
**Then** hệ thống parse, validate và hiển thị preview trước khi áp dụng
**And** trả về danh sách dòng lỗi nếu dữ liệu không hợp lệ.

### Story 7.4: Phê duyệt thay đổi trước khi có hiệu lực

As an admin reviewer,
I want approve/reject thay đổi reference data,
So that không có chỉnh sửa chưa kiểm soát đi thẳng vào production.

**Acceptance Criteria:**

**Given** có change set ở trạng thái chờ duyệt
**When** reviewer chọn approve hoặc reject
**Then** trạng thái change set được cập nhật tương ứng
**And** chỉ change set approved mới được dùng cho rule engine.

### Story 7.5: Audit log đầy đủ cho reference data

As an admin,
I want xem lịch sử thay đổi reference data,
So that tôi truy vết ai đã sửa gì và khi nào.

**Acceptance Criteria:**

**Given** có thao tác quản trị trên reference data
**When** mở trang audit log
**Then** hệ thống hiển thị actor, timestamp, diff, source và IP
**And** hỗ trợ lọc theo thời gian, chỉ số và người thực hiện.

## Epic 8: Dashboard Vận Hành và Chỉ Số Sử Dụng
Goal: Cung cấp số liệu vận hành giúp theo dõi tăng trưởng và chất lượng hệ thống theo thời gian.

### Story 8.1: Dashboard tổng quan tăng trưởng người dùng

As an admin,
I want xem tổng user all-time và theo tháng,
So that tôi theo dõi đà tăng trưởng sản phẩm.

**Acceptance Criteria:**

**Given** dữ liệu người dùng đã được ghi nhận
**When** mở trang thống kê admin
**Then** hiển thị total registered users và monthly trend chart
**And** số liệu được tính từ nguồn dữ liệu đã chuẩn hóa.

### Story 8.2: Dashboard hoạt động WAU và upload volume

As an admin,
I want xem WAU và số lượt upload theo ngày/tuần,
So that tôi đo mức sử dụng thực tế của hệ thống.

**Acceptance Criteria:**

**Given** sự kiện sử dụng đã được tracking
**When** chọn khoảng thời gian cần xem
**Then** dashboard hiển thị WAU và upload counts theo granularity tương ứng
**And** hỗ trợ so sánh với kỳ trước.

### Story 8.3: Tỷ lệ upload thành công vs thất bại

As an admin,
I want theo dõi success/failure rate của upload pipeline,
So that tôi phát hiện sớm vấn đề OCR hoặc hạ tầng.

**Acceptance Criteria:**

**Given** các upload job có trạng thái xử lý
**When** mở phần quality metrics
**Then** hiển thị tỷ lệ thành công/thất bại theo ngày và tuần
**And** cho phép drill-down theo nguyên nhân lỗi chính.
