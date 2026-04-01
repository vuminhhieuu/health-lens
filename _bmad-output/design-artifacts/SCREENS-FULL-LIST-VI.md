# HealthLens - Danh sách Tất cả Screens

**Phiên bản:** 1.0  
**Cập nhật:** Tháng 3 năm 2026  
**Mục đích:** Liệt kê toàn bộ screens cần thiết kế cho Web + Mobile

---

## 📱 PHẦN 1: WEB DASHBOARD SCREENS (11 screens chính)

### 1.1 AUTHENTICATION FLOWS (3 screens)

#### Screen 1.1.1: Đăng Nhập (Login)
```
Path: /login
Stitch ID: 09409d972eaa45f592c396b6d14469aa
├── Header: Logo HealthLens + "Đăng Nhập"
├── Form fields:
│   ├── Email address
│   ├── Password
│   └── "Ghi nhớ tôi" (checkbox)
├── Buttons:
│   ├── Primary: "Đăng Nhập"
│   └── Secondary: "Quên mật khẩu?"
├── Footer: "Chưa có tài khoản? Đăng ký"
└── Visual: Teal background, warm welcome

Responsive:
├── Desktop: Centered form (400px width)
├── Tablet: 2-column layout (form + image)
└── Mobile: Full width form
```

#### Screen 1.1.2: Đăng Ký (Register)
```
Path: /register
Stitch ID: a3b22ddf15924e8c860ea3239e312044
├── Header: Logo + "Tạo Tài Khoản"
├── Form fields:
│   ├── Họ tên (Full name)
│   ├── Email address
│   ├── Ngày sinh (Date of birth)
│   ├── Password
│   ├── Xác nhận mật khẩu
│   └── Chấp nhận điều khoản (checkbox)
├── Validation: Real-time feedback
├── Button: "Đăng Ký" (Primary)
├── Footer: "Đã có tài khoản? Đăng Nhập"
└── Visual: Same as Login screen

Responsive:
├── Desktop: Centered form (500px width)
├── Tablet: 2-column layout
└── Mobile: Full width form
```

#### Screen 1.1.3: Đặt Lại Mật Khẩu (Password Reset)
```
Path: /reset-password
Stitch ID: f1b10549c80e4f98ab45fa121a50f009 (Forgot Password), c8952e57aec54ba0aa5da4bdd913cddb (Success State)
├── Step 1: Email verification
│   ├── "Nhập email để đặt lại mật khẩu"
│   ├── Email input
│   └── Button: "Gửi liên kết"
│
├── Step 2: Check email (show message)
│   ├── "Kiểm tra email của bạn"
│   ├── Illustration: Mail icon
│   └── "Không nhận được email? Gửi lại"
│
└── Step 3: Enter new password
    ├── "Đặt mật khẩu mới"
    ├── New password input
    ├── Confirm password input
    └── Button: "Cập nhật mật khẩu"
```

#### Screen 1.1.4: Đồng Ý Điều Khoản (Consent Screen) ⭐ NEW
```
Path: /consent
Stitch ID: fb972f816f334c73b426f17cd20bd58d
├── Header: Logo HealthLens + "Điều Khoản Sử Dụng"
├── Content area (scrollable):
│   ├── Section 1: Giới thiệu dịch vụ
│   │   └── Mô tả ngắn về HealthLens và mục đích
│   │
│   ├── Section 2: Thu thập và xử lý dữ liệu (NĐ 13/2023/NĐ-CP)
│   │   ├── Loại dữ liệu thu thập
│   │   ├── Mục đích xử lý
│   │   ├── Thời gian lưu trữ
│   │   └── Quyền của người dùng
│   │
│   ├── Section 3: Chia sẻ dữ liệu
│   │   ├── Với ai (gia đình, bác sĩ)
│   │   └── Điều kiện chia sẻ
│   │
│   ├── Section 4: Bảo mật dữ liệu
│   │   ├── Mã hóa end-to-end
│   │   └── Biện pháp bảo vệ
│   │
│   └── Section 5: Giới hạn trách nhiệm
│       ├── ⚠️ Alert: "Khuyến nghị y tế từ HealthLens chỉ mang tính 
│       │   tham khảo, KHÔNG thay thế tư vấn từ bác sĩ"
│       └── Disclaimer về độ chính xác OCR
│
├── Consent checkboxes (required):
│   ├── [ ] Tôi đã đọc và hiểu Điều khoản sử dụng
│   ├── [ ] Tôi đồng ý cho HealthLens thu thập và xử lý dữ liệu 
│   │       sức khỏe của tôi theo NĐ 13/2023/NĐ-CP
│   └── [ ] Tôi hiểu rằng khuyến nghị y tế chỉ mang tính tham khảo
│
├── Buttons:
│   ├── Primary: "Đồng ý và Tiếp tục" (disabled until all checked)
│   └── Secondary: "Từ chối" (leads to exit/logout)
│
├── Footer links:
│   ├── "Xem Chính sách Bảo mật đầy đủ"
│   └── "Tải về bản PDF"
│
└── Visual: Clean white background, teal accents, clear typography

Responsive:
├── Desktop: Centered content (700px max-width)
├── Tablet: Full width with padding
└── Mobile: Full width, larger checkboxes for touch

Accessibility:
├── Scrollable content with visible scrollbar
├── Checkboxes: 44px touch target
├── Clear focus states
└── Screen reader: Proper heading hierarchy
```

---

### 1.2 USER DASHBOARD (3 screens)

#### Screen 1.2.1: Trang Chủ (Home/Dashboard)
```
Path: /dashboard
Stitch ID: 010095343e6c46b4969b42c4ab93165a
├── Top Navigation:
│   ├── Logo (left)
│   ├── Search bar (center) - tìm kiếm hồ sơ/kết quả
│   ├── Notifications bell icon (right)
│   └── User menu (avatar dropdown)
│
├── Sidebar Navigation (left, collapsible):
│   ├── Dashboard (active)
│   ├── Hồ sơ của tôi
│   ├── Chia sẻ gia đình
│   ├── Cài đặt
│   └── Đăng xuất
│
├── Main content area:
│   ├── Welcome card: "Chào <name>, kết quả gần đây của bạn"
│   │
│   ├── Quick stats:
│   │   ├── Last test date
│   │   ├── Number of tests
│   │   └── Health status overview
│   │
│   ├── Recent results (grid):
│   │   ├── HealthResultSummary card #1
│   │   ├── HealthResultSummary card #2
│   │   ├── HealthResultSummary card #3
│   │   └── "Xem tất cả" link
│   │
│   ├── Family shared profiles:
│   │   ├── FamilyMemberCard (if any)
│   │   └── "Mời gia đình" button
│   │
│   └── CTA buttons:
│       ├── "Tải kết quả mới"
│       └── "Xem phân tích"
│
└── Desktop width: Sidebar (250px) + Content (flex)

Responsive:
├── Desktop: 3-column (Sidebar + Main + Optional right panel)
├── Tablet: Sidebar collapsed to icon, full width main
└── Mobile: Sidebar as drawer, full width main
```

#### Screen 1.2.2: Danh sách Kết quả Sức khỏe (Health Records List)
```
Path: /dashboard/health-records
Stitch ID: 2f0902b360ee4d9fb7be1cb8d51c2640
├── Header: "Kết quả Sức khỏe"
├── Filters/Sort (optional):
│   ├── Filter by date range
│   ├── Filter by test type
│   └── Sort: Newest first, Oldest first
│
├── Main content: Table or Card list
│   ├── For desktop: Table
│   │   ├── Columns:
│   │   │   ├── Ngày xét nghiệm
│   │   │   ├── Loại xét nghiệm
│   │   │   ├── Trạng thái (status badge)
│   │   │   ├── Hành động (view, delete icons)
│   │   │   └── Click row → go to detail
│   │   │
│   │   └── Rows: 10-20 items
│   │
│   └── For mobile: Card stack
│       ├── HealthResultSummary card per item
│       ├── Click card → go to detail
│       └── Swipe right → delete option
│
├── Pagination (if >20 items):
│   ├── Previous/Next buttons
│   ├── Page numbers
│   └── "Showing 1-10 of 45 results"
│
└── Empty state (if no results):
    ├── Illustration: Empty folder icon
    ├── "Chưa có kết quả nào"
    └── CTA: "Tải kết quả đầu tiên"
```

#### Screen 1.2.3: Chi tiết Kết quả (Health Record Detail)
```
Path: /dashboard/health-records/:id
Stitch ID: 3c9f3f9c951b4e45aa713c6da552be29
├── Header:
│   ├── Breadcrumb: "Kết quả Sức khỏe / <Date>"
│   ├── Date and test type: "Xét nghiệm - 15/03/2026"
│   └── Action buttons: Edit, Delete, Share, Download PDF
│
├── Tabs (optional):
│   ├── Tổng quan (Overview) - default
│   ├── Chi tiết (Details)
│   ├── Giải thích (Explanations)
│   └── Khuyến nghị (Recommendations)
│
├── Main content (Overview tab):
│   │
│   ├── Overall status card:
│   │   ├── HealthStatusBadge (Normal/Warning/Critical)
│   │   ├── Summary text
│   │   └── Last updated time
│   │
│   ├── Source info badge (NEW - FR15): ⭐
│   │   ├── Icon: 🏥
│   │   ├── "Nguồn: Bệnh viện ABC"
│   │   └── "Ngày: 15/03/2026"
│   │
│   ├── Metrics grid (responsive):
│   │   ├── HealthMetricCard #1 (expandable)
│   │   │   ├── Metric name: "Huyết áp tâm thu"
│   │   │   ├── Value: "120"
│   │   │   ├── Unit: "mmHg"
│   │   │   ├── Status: Green checkmark
│   │   │   ├── Reference range: "90-120"
│   │   │   └── Click to expand: Show explanation + recommendations
│   │   │
│   │   ├── HealthMetricCard #2
│   │   ├── HealthMetricCard #3
│   │   └── ... more metrics
│   │
│   ├── Abnormal values section (if any):
│   │   ├── Alert box (red or yellow)
│   │   ├── List of abnormal values
│   │   └── Recommendations for each
│   │
│   ├── Recommendations section (with Disclaimer - FR21): ⭐
│   │   ├── Title: "Khuyến nghị"
│   │   ├── ⚠️ Disclaimer Alert (Info style, always visible):
│   │   │   ├── Icon: ℹ️
│   │   │   ├── Title: "Lưu ý quan trọng"
│   │   │   └── Text: "Các khuyến nghị dưới đây chỉ mang tính 
│   │   │       chất THAM KHẢO và KHÔNG thay thế tư vấn y tế 
│   │   │       chuyên nghiệp. Vui lòng tham khảo ý kiến bác sĩ 
│   │   │       trước khi thực hiện bất kỳ thay đổi nào về sức khỏe."
│   │   │
│   │   ├── Recommendation cards:
│   │   │   ├── Card 1: "Theo dõi huyết áp hàng ngày"
│   │   │   ├── Card 2: "Giảm muối trong chế độ ăn"
│   │   │   └── Card 3: "Tập thể dục nhẹ 30 phút/ngày"
│   │   │
│   │   └── Footer link: "Tìm bác sĩ gần bạn →"
│   │
│   └── Related actions:
│       ├── "Chia sẻ kết quả"
│       ├── "Tải về PDF"
│       └── "Xóa kết quả"
│
└── Sidebar (desktop only):
    ├── Exam info card
    ├── Facility name
    ├── Doctor name (if available)
    └── Share status
```

---

### 1.3 PROFILE MANAGEMENT (3 screens)

#### Screen 1.3.1: Danh sách Hồ sơ (My Profiles)
```
Path: /profiles
Stitch ID: 68716ba836ae433e82006a112d724e12
├── Header: "Hồ sơ Của Tôi"
├── Description: "Quản lý các hồ sơ sức khỏe của bạn"
│
├── Profiles grid/list:
│   ├── ProfileCard #1 (Primary)
│   │   ├── Avatar (large)
│   │   ├── Name: "Tôi"
│   │   ├── Age: "45 tuổi"
│   │   ├── Latest status: Green badge
│   │   ├── Last update: "Cập nhật 2 ngày trước"
│   │   ├── Actions: Edit, View records, Delete
│   │   └── Badge: "Hồ sơ chính"
│   │
│   ├── ProfileCard #2 (Family member)
│   │   ├── Avatar
│   │   ├── Name: "Mẹ"
│   │   ├── Relationship: "Người nhà"
│   │   ├── Latest status: Yellow badge
│   │   └── Actions: View, Edit, Delete, Share
│   │
│   └── ProfileCard #3 (Empty)
│       ├── Large "+" icon
│       └── "Tạo hồ sơ mới"
│
├── Button at bottom:
│   └── Primary: "+ Tạo Hồ sơ Mới"
│
└── Responsive:
    ├── Desktop: 3-column grid
    ├── Tablet: 2-column grid
    └── Mobile: 1-column stack
```

#### Screen 1.3.2: Tạo/Chỉnh sửa Hồ sơ (Create/Edit Profile)
```
Path: /profiles/new or /profiles/:id/edit
├── Header: "Tạo Hồ sơ Mới" or "Chỉnh sửa Hồ sơ"
│
├── Form:
│   ├── Profile picture:
│   │   ├── Avatar picker (click to upload or use initials)
│   │   └── Current image preview
│   │
│   ├── Họ tên (Full name) - required
│   ├── Ngày sinh (Date of birth) - required
│   ├── Giới tính (Gender) - select
│   ├── Mối quan hệ (Relationship) - select
│   │   ├── Tôi
│   │   ├── Cha/Mẹ
│   │   ├── Con
│   │   ├── Anh/Chị/Em
│   │   └── Khác
│   │
│   ├── Bệnh lý nền (Existing conditions) - multi-select
│   ├── Dị ứng (Allergies) - text field
│   ├── Ghi chú (Notes) - textarea
│   │
│   └── Privacy settings:
│       ├── "Cho phép chia sẻ hồ sơ này" - toggle
│       └── Selected by default for family profiles
│
├── Buttons:
│   ├── Primary: "Lưu Hồ sơ"
│   └── Secondary: "Hủy"
│
└── Validation:
    └── Show errors in real-time, with helpful messages
```

---

### 1.4 FAMILY SHARING (2 screens)

#### Screen 1.4.1: Chia sẻ Gia đình (Family Sharing Management)
```
Path: /family-sharing
Stitch ID: 6c7c4797727240cda249483e8307e4cf
├── Header: "Chia sẻ Gia đình"
├── Description: "Quản lý quyền truy cập hồ sơ của gia đình"
│
├── Shared profiles section:
│   ├── "Đang chia sẻ với tôi:"
│   ├── FamilyMemberCard #1 (mẹ)
│   │   ├── Avatar
│   │   ├── Name
│   │   ├── Relationship
│   │   ├── Latest test status
│   │   ├── Shared since date
│   │   └── Actions: View details, Revoke access
│   │
│   └── FamilyMemberCard #2, #3, ...
│
├── Family members section:
│   ├── "Gia đình của tôi:"
│   ├── Show current family members (if added to contacts)
│   └── "Mời người mới" button
│
├── Pending invitations:
│   ├── "Lời mời chưa được chấp nhận:"
│   ├── List with cancel option
│   └── Or empty if none
│
├── CTA area:
│   ├── Primary: "+ Mời Gia Đình"
│   └── Secondary: "Cài đặt quyền riêng tư"
│
└── Responsive: Full width, card layout
```

#### Screen 1.4.2: Dashboard Gia đình (Family Dashboard - Web only)
```
Path: /family-dashboard
├── Header: "Sức khỏe Gia đình"
├── Description: "Xem tình trạng sức khỏe các thành viên"
│
├── Family members overview:
│   ├── Grid of FamilyMemberCard items:
│   │   ├── FamilyMemberCard (mẹ - Normal status)
│   │   │   ├── Avatar
│   │   │   ├── Name
│   │   │   ├── Latest test status (Green badge)
│   │   │   ├── Last update date
│   │   │   └── Click → See full profile details
│   │   │
│   │   ├── FamilyMemberCard (bố - Warning status)
│   │   │   ├── Avatar
│   │   │   ├── Yellow warning badge
│   │   │   ├── "Cần chú ý" indicator
│   │   │   └── Quick action: "Xem chi tiết"
│   │   │
│   │   └── FamilyMemberCard #3, ...
│   │
│   └── Cards are read-only (view only, no edit)
│
├── Recent activity timeline:
│   ├── "Hoạt động gần đây:"
│   ├── "Ngày 15/03 - Mẹ tải kết quả xét nghiệm"
│   ├── "Ngày 14/03 - Bố thêm ghi chú"
│   └── "Xem tất cả hoạt động"
│
└── Responsive: Desktop optimized, 3-column grid
```

---

### 1.5 ADMIN SCREENS (? screens - if applicable)

#### Screen 1.5.1: Admin Dashboard (Analytics Overview)
```
Path: /admin/dashboard
Stitch ID: 1cfbb9fe88734e629a044add27adf1f1
├── Header: "Admin Dashboard"
├── Role indicator: "Admin" badge
│
├── Key metrics cards:
│   ├── "Người dùng hoạt động"
│   ├── "Tổng hồ sơ"
│   ├── "Kết quả xét nghiệm"
│   ├── "Yêu cầu chia sẻ"
│   └── Numbers + trend (↑ or ↓)
│
├── Charts (if any):
│   ├── User growth chart (line graph)
│   └── Activity heatmap
│
└── Quick links to admin sections
```

#### Screen 1.5.2: Quản lý Dữ liệu Tham chiếu (Reference Data Management)
```
Path: /admin/reference-data
Stitch ID: bb3084187f0e41d5b4ac0617a52da22c
├── Header: "Quản lý Dữ liệu Tham chiếu"
├── Description: "Cập nhật giá trị bình thường cho các chỉ số"
│
├── Search/Filter:
│   ├── Search by metric name
│   └── Filter by category
│
├── Table of reference data:
│   ├── Columns:
│   │   ├── Tên chỉ số (Metric name)
│   │   ├── Loại (Type)
│   │   ├── Giá trị bình thường (Range)
│   │   ├── Đơn vị (Unit)
│   │   ├── Lần cập nhật cuối
│   │   └── Hành động (Edit, Delete)
│   │
│   └── Each row clickable to edit
│
├── Buttons:
│   ├── "+ Thêm Chỉ số"
│   ├── "Nhập CSV"
│   └── "Xuất PDF"
│
└── Pagination if many items
```

#### Screen 1.5.3: Editor Dữ liệu (Reference Data Editor)
```
Path: /admin/reference-data/:id/edit
Stitch ID: 7c23ef03075048afb2fddab20e12afd6
├── Header: "Chỉnh sửa Chỉ số"
│
├── Form fields:
│   ├── Tên chỉ số (Metric name)
│   ├── Loại (Type): Input/Ratio/Count
│   ├── Giá trị tối thiểu (Min normal)
│   ├── Giá trị tối đa (Max normal)
│   ├── Đơn vị (Unit)
│   ├── Giải thích (Explanation text)
│   ├── Khuyến nghị (Recommendations)
│   ├── Danh mục (Category)
│   └── Status: Active/Inactive
│
├── Approval Workflow Section (FR36): ⭐
│   ├── Current status badge:
│   │   ├── "Bản nháp" (Draft) - gray
│   │   ├── "Chờ duyệt" (Pending) - yellow
│   │   ├── "Đã duyệt" (Approved) - green
│   │   └── "Từ chối" (Rejected) - red
│   │
│   ├── Approval history timeline:
│   │   ├── "15/03 10:30 - Tạo bởi Admin A"
│   │   ├── "15/03 11:00 - Gửi duyệt"
│   │   ├── "15/03 14:00 - Duyệt bởi Super Admin"
│   │   └── "15/03 14:05 - Xuất bản"
│   │
│   ├── Reviewer actions (if pending):
│   │   ├── Button: "Duyệt" (green)
│   │   ├── Button: "Từ chối" (red)
│   │   └── Textarea: "Ghi chú cho người tạo"
│   │
│   └── Author actions:
│       ├── Button: "Gửi duyệt" (if draft)
│       ├── Button: "Thu hồi" (if pending)
│       └── Button: "Chỉnh sửa và gửi lại" (if rejected)
│
├── Buttons:
│   ├── Primary: "Lưu bản nháp" / "Gửi duyệt"
│   └── Secondary: "Hủy" / "Xóa"
│
└── Visual: Clear status indicators, timeline format
```

#### Screen 1.5.4: Nhật ký Kiểm tra (Audit Logs)
```
Path: /admin/audit-logs
Stitch ID: d0690e9e60b74787b60020aaadd6c0e4
├── Header: "Nhật ký Hoạt động"
│
├── Filters:
│   ├── Filter by user
│   ├── Filter by action type
│   ├── Filter by date range
│   └── Filter by status
│
├── Table:
│   ├── Columns:
│   │   ├── Thời gian (Timestamp)
│   │   ├── Người dùng (User)
│   │   ├── Hành động (Action)
│   │   ├── Dữ liệu (Data changed)
│   │   └── Kết quả (Success/Error)
│   │
│   └── Click row to see full details
│
└── Export option: "Xuất CSV/JSON"
```

#### Screen 1.5.5: Xác Thực Hai Yếu Tố Admin (Admin MFA Setup) ⭐ NEW
```
Path: /admin/mfa-setup
Stitch ID: 07e884d40df14c2b8558038dabda2e56 (Setup), 397d87fb9f5f44d29e6ea84f12d81200 (Method Selection), 4624e45abb214f2ea30b2d010918f9f7 (Verification)
├── Header: "Xác Thực Hai Yếu Tố (MFA)"
├── Role indicator: "Admin" badge + Security icon
│
├── Step indicator (Stepper component):
│   ├── Step 1: Chọn phương thức ✓
│   ├── Step 2: Thiết lập (active)
│   └── Step 3: Xác nhận
│
├── Step 1: Chọn phương thức MFA
│   ├── Option cards (radio selection):
│   │   ├── [📱] Ứng dụng Authenticator (Khuyến nghị)
│   │   │   └── "Google Authenticator, Microsoft Authenticator..."
│   │   ├── [📧] Email OTP
│   │   │   └── "Mã xác thực gửi qua email"
│   │   └── [📞] SMS OTP
│   │       └── "Mã xác thực gửi qua SMS"
│   └── Button: "Tiếp tục"
│
├── Step 2: Thiết lập (for Authenticator app)
│   ├── QR Code display (200×200px)
│   │   └── "Quét mã QR bằng ứng dụng Authenticator"
│   ├── Manual entry code (if can't scan):
│   │   └── "XXXX-XXXX-XXXX-XXXX" (copyable)
│   ├── Input: "Nhập mã 6 chữ số từ ứng dụng"
│   └── Buttons: "Xác nhận" | "Quay lại"
│
├── Step 3: Backup codes
│   ├── Title: "Mã dự phòng"
│   ├── Warning alert: "Lưu các mã này ở nơi an toàn. 
│   │   Mỗi mã chỉ sử dụng được một lần."
│   ├── Grid of 10 backup codes:
│   │   └── XXXX-XXXX (×10)
│   ├── Buttons:
│   │   ├── "Tải xuống" (download .txt)
│   │   ├── "Sao chép tất cả"
│   │   └── "In ra"
│   └── Checkbox: "Tôi đã lưu các mã dự phòng"
│   └── Button: "Hoàn tất thiết lập"
│
├── Success state:
│   ├── ✓ Icon (green, large)
│   ├── "MFA đã được kích hoạt thành công!"
│   ├── "Từ giờ bạn sẽ cần nhập mã xác thực khi đăng nhập"
│   └── Button: "Quay về Dashboard"
│
├── MFA Management (if already enabled):
│   ├── Status: "✓ MFA đang hoạt động"
│   ├── Phương thức: "Ứng dụng Authenticator"
│   ├── Thiết lập lúc: "15/03/2026 10:30"
│   ├── Actions:
│   │   ├── "Tạo mã dự phòng mới"
│   │   ├── "Đổi phương thức MFA"
│   │   └── "Tắt MFA" (danger, requires confirmation)
│   └── Recent MFA activity log
│
└── Visual: 
    ├── Clean card-based layout
    ├── Progress stepper at top
    ├── QR code prominent and scannable
    └── Backup codes in monospace font

Responsive:
├── Desktop: Centered content (600px max-width)
├── Tablet: Full width with padding
└── Mobile: Stack vertically, larger touch targets

Accessibility:
├── QR code has text alternative (manual code)
├── Backup codes: Monospace, high contrast
├── Clear step indicators
└── Keyboard navigation through steps
```

---

### 1.6 SETTINGS (1 screen)

#### Screen 1.6.1: Cài đặt (Account Settings)
```
Path: /settings
Stitch ID: fc0abf8742984269b5a25a0e0ab20782 (Security), f6198b62522b49d88bd34fad6c9cee84 (System)
├── Sidebar (left):
│   ├── Tài khoản (Account)
│   ├── Riêng tư & Bảo mật (Privacy & Security)
│   ├── Thông báo (Notifications)
│   ├── Về ứng dụng (About)
│   └── Liên hệ Hỗ trợ (Support)
│
├── Main content:
│   │
│   ├── Tab 1: Account Settings
│   │   ├── Profile info
│   │   │   ├── Avatar
│   │   │   ├── Name
│   │   │   ├── Email
│   │   │   └── Phone (optional)
│   │   │
│   │   ├── Change password
│   │   │   ├── Current password
│   │   │   ├── New password
│   │   │   └── Confirm
│   │   │
│   │   ├── Two-factor authentication
│   │   │   ├── Status: Enabled/Disabled toggle
│   │   │   └── Setup button if disabled
│   │   │
│   │   └── Delete account
│   │       └── Danger button with confirmation
│   │
│   ├── Tab 2: Privacy & Security
│   │   ├── Data sharing preferences
│   │   ├── Consent management
│   │   ├── Download my data
│   │   └── Delete my data
│   │
│   ├── Tab 3: Notifications
│   │   ├── Email notifications (toggles)
│   │   ├── Push notifications (toggles)
│   │   └── Notification preferences
│   │
│   └── Tab 4: About
│       ├── Version info
│       ├── Terms of service
│       ├── Privacy policy
│       └── Contact support
│
└── Responsive: Sidebar collapses to tabs on mobile
```

---

## 📱 PHẦN 2: MOBILE APP SCREENS (9 screens chính)

**Lưu ý:** React Native/Expo - responsive và native feel

### 2.1 AUTHENTICATION (3 screens - same as web)

#### Screen 2.1.1: Đăng Nhập Mobile
```
Path: /login
├── Header: Logo + "Đăng Nhập"
├── Form:
│   ├── Email field
│   ├── Password field
│   └── Buttons: "Đăng Nhập" + "Quên mật khẩu?"
│
├── Layout: Full width form (95% screen width)
└── Safe area padding
```

#### Screen 2.1.2: Đăng Ký Mobile
```
Path: /register
├── Scrollable form
├── All fields same as web version
├── Full width buttons
└── Error messages inline
```

#### Screen 2.1.3: Onboarding (New feature for mobile)
```
Path: /onboarding
├── Slide 1: Welcome to HealthLens
├── Slide 2: Upload health records
├── Slide 3: Get personalized insights
├── Slide 4: Share with family
├── Bottom: Dots indicator + "Skip" / "Next" buttons
└── Final button: "Bắt đầu"
```

---

### 2.2 HOME & MAIN NAVIGATION (1 screen with tabs)

#### Screen 2.2.1: Home Tab (Bottom Navigation)
```
Bottom Tab Navigation (5 tabs):
├── Home (house icon) - selected
├── Upload (cloud upload icon)
├── Profiles (person icon)
├── Family (people icon)
└── Settings (gear icon)

Home Tab Content:
├── Header: "HealthLens" + Notification bell
│
├── Welcome card: "Chào <name>"
│
├── Quick actions:
│   ├── "Tải kết quả"
│   ├── "Xem hồ sơ"
│   └── "Chia sẻ gia đình"
│
├── Recent results section:
│   ├── "Kết quả gần đây"
│   ├── HealthResultSummary card (horizontal scroll)
│   ├── Each card clickable → Detail view
│   └── "Xem tất cả" link
│
├── Family section:
│   ├── "Gia đình của tôi"
│   ├── FamilyMemberCard (horizontal scroll)
│   └── "+ Mời" button
│
└── Responsive: Full screen, scrollable
```

---

### 2.3 UPLOAD FLOW (3 screens)

#### Screen 2.3.1: Upload Options
```
Path: /upload
Stitch ID: 0e048c6789e64ceab2f78815dac26939
├── Header: "Tải kết quả"
│
├── Profile Selector (NEW - FR17): ⭐
│   ├── Label: "Tải kết quả cho hồ sơ:"
│   ├── Dropdown/Select:
│   │   ├── "Tôi" (default, primary profile)
│   │   ├── "Mẹ - Trần Thị B"
│   │   ├── "Bố - Trần Văn C"
│   │   └── "+ Tạo hồ sơ mới"
│   └── Selected profile avatar + name preview
│
├── Options:
│   ├── Button 1: "Chụp ảnh từ camera"
│   │   └── Opens device camera
│   │
│   ├── Button 2: "Chọn từ thư viện"
│   │   └── Opens file picker (photo/PDF)
│   │
│   └── Button 3: "Tải PDF"
│       └── Opens file picker (PDF only)
│
├── Info text:
│   └── "Tải ảnh hoặc PDF của kết quả xét nghiệm"
│
└── Safe area padding, full width buttons
```

#### Screen 2.3.2: Processing/Loading
```
Path: /upload/:id/processing
Stitch ID: 661b65268f614bacab4dee0690eb1790
├── Header: "Đang xử lý..."
│
├── Progress indicator:
│   ├── Animated spinner
│   ├── Step indication:
│   │   ├── "Đang đọc ảnh..." (50%)
│   │   ├── "Đang phân tích..." (70%)
│   │   └── "Đang tạo báo cáo..." (90%)
│   │
│   └── Progress bar (visual)
│
├── Status message: "Vui lòng chờ..."
│
└── Can't dismiss (or "Cancel" button)
```

#### Screen 2.3.3: Review & Confirm
```
Path: /upload/:id/review
Stitch ID: a9f6ae144fa7402bac66d0c56b36f4c0
├── Header: "Xác nhận Kết quả"
│
├── Source Tagging (NEW - FR15): ⭐
│   ├── Label: "Nguồn kết quả xét nghiệm"
│   ├── Input fields:
│   │   ├── Cơ sở y tế: [Autocomplete/Input]
│   │   ├── Bác sĩ: [Input] (optional)
│   │   └── Ngày xét nghiệm: [Date picker]
│   └── Badge: "Nguồn: [Tên cơ sở]" (preview)
│
├── OCR Results preview:
│   ├── Extracted metrics table:
│   │   ├── Row: Metric | Value | Unit | Status
│   │   ├── Row: Huyết áp | 120 | mmHg | ✓
│   │   └── Row: Glucose | 110 | mg/dL | ⚠
│   │
│   └── Editable (can fix OCR errors)
│
├── Buttons:
│   ├── Primary: "Lưu kết quả"
│   ├── Secondary: "Chỉnh sửa"
│   └── "Hủy"
│
└── Full width form, scrollable
```

#### Screen 2.3.4: OCR Failure Recovery (NEW - FR16) ⭐
```
Path: /upload/:id/manual-entry
Stitch ID: ace87425151148448083728c199470c7 (Recovery), 1ba2a148df7d427d89fdaaf223deaafd (Manual Entry)
├── Header: "Nhập Kết quả Thủ công"
│
├── Alert (Warning):
│   ├── Icon: ⚠️
│   ├── Title: "Không thể đọc tự động"
│   └── Message: "Chúng tôi không thể trích xuất dữ liệu từ 
│       hình ảnh. Vui lòng nhập thủ công hoặc thử lại với 
│       ảnh rõ hơn."
│
├── Options:
│   ├── Button 1: "Chụp lại ảnh"
│   │   └── Returns to camera
│   ├── Button 2: "Chọn ảnh khác"
│   │   └── Opens file picker
│   └── Divider: "─── hoặc ───"
│
├── Manual Entry Form:
│   ├── Title: "Nhập kết quả thủ công"
│   ├── Test type selector:
│   │   └── Dropdown: Xét nghiệm máu, Nước tiểu, Siêu âm...
│   │
│   ├── Dynamic metric fields (based on test type):
│   │   ├── Metric row 1:
│   │   │   ├── Tên chỉ số: [Autocomplete]
│   │   │   ├── Giá trị: [Number input]
│   │   │   └── Đơn vị: [Select]
│   │   ├── Metric row 2...
│   │   └── "+ Thêm chỉ số" button
│   │
│   ├── Source info:
│   │   ├── Cơ sở y tế: [Input]
│   │   ├── Ngày xét nghiệm: [Date picker]
│   │   └── Ghi chú: [Textarea]
│   │
│   └── Buttons:
│       ├── Primary: "Lưu kết quả"
│       └── Secondary: "Hủy"
│
├── Help section:
│   ├── "Mẹo chụp ảnh tốt hơn:"
│   ├── • Đảm bảo ánh sáng đủ
│   ├── • Giữ camera song song với giấy
│   ├── • Tránh bóng đổ
│   └── • Chụp toàn bộ kết quả
│
└── Visual: Clean form, helpful guidance

Responsive:
├── Desktop: 2-column (options + form)
├── Tablet: Single column
└── Mobile: Full width, scrollable

Accessibility:
├── Clear error messaging
├── Autocomplete for metric names
├── Large touch targets
└── Keyboard navigation
```

---

### 2.4 RESULT DETAIL (1 screen)

#### Screen 2.4.1: Kết quả Chi tiết
```
Path: /health-records/:id
├── Header: Date + Test type (collapsible on scroll)
│
├── Status card:
│   ├── HealthStatusBadge (Large)
│   ├── "Kết quả bình thường" / "Cần chú ý" / "Cần hành động"
│   └── Summary
│
├── Metrics section (scrollable):
│   ├── HealthMetricCard #1 (tap to expand)
│   │   ├── Metric name
│   │   ├── Value + Unit
│   │   ├── Status badge
│   │   ├── Tap → Shows explanation + recommendations
│   │   └── Can collapse/expand
│   │
│   ├── HealthMetricCard #2, #3, ...
│   └── Can swipe to scroll metrics
│
├── Actions (bottom sticky):
│   ├── Share button
│   ├── More (⋮) menu:
│   │   ├── Download PDF
│   │   └── Delete
│   │
│   └── Back button (iOS style)
│
└── Full screen, scrollable
```

---

### 2.5 PROFILES TAB (2 screens)

#### Screen 2.5.1: Danh sách Hồ sơ
```
Path: /profiles (tab-based)
├── Header: "Hồ sơ Của Tôi"
│
├── Profile cards (vertical stack):
│   ├── ProfileCard #1 (Tôi)
│   │   ├── Avatar (large)
│   │   ├── Name
│   │   ├── Age / Status
│   │   └── Tap → Edit profile
│   │
│   ├── ProfileCard #2 (Family member)
│   └── More profiles...
│
├── Add profile button:
│   └── "+ Tạo Hồ sơ Mới"
│
└── Full screen, scrollable, card layout
```

#### Screen 2.5.2: Tạo/Chỉnh sửa Hồ sơ
```
Path: /profiles/new or /profiles/:id/edit
├── Header: "Tạo Hồ sơ" / "Chỉnh sửa"
│
├── Form fields (scrollable):
│   ├── Avatar picker (tap to select)
│   ├── Họ tên
│   ├── Ngày sinh (date picker)
│   ├── Giới tính (picker)
│   ├── Mối quan hệ (picker)
│   ├── Bệnh lý nền (multi-select)
│   ├── Dị ứng
│   ├── Ghi chú
│   ├── Privacy toggle
│   │
│   └── Buttons:
│       ├── "Lưu"
│       └── "Hủy"
│
└── Full width form, keyboard-aware
```

---

### 2.6 FAMILY TAB (1 screen)

#### Screen 2.6.1: Gia đình & Chia sẻ
```
Path: /family (tab-based)
├── Header: "Gia Đình"
│
├── Shared profiles section:
│   ├── "Hồ sơ được chia sẻ:"
│   ├── FamilyMemberCard #1
│   │   ├── Avatar
│   │   ├── Name
│   │   ├── Status badge
│   │   ├── Tap → View profile
│   │   └── Swipe left → Revoke access (optional)
│   │
│   └── More cards...
│
├── Invite section:
│   ├── "Mời thành viên gia đình"
│   ├── Input: Email address
│   ├── Select profiles to share
│   └── Button: "Gửi lời mời"
│
└── Full screen, scrollable
```

---

### 2.7 SETTINGS TAB (2 screens accessible from here)

#### Screen 2.7.1: Settings Menu
```
Path: /settings (tab-based)
├── Header: "Cài đặt"
│
├── Menu items:
│   ├── Profile (name, email) - tap for details
│   ├── Notifications - toggle options
│   ├── Privacy & Security - tap for details
│   ├── About HealthLens
│   ├── Contact Support
│   ├── Terms & Privacy Policy
│   └── Logout (red color)
│
└── Each clickable (except toggles) → Subscreen
```

#### Screen 2.7.2: Thay đổi Mật khẩu (Subscreen)
```
Path: /settings/change-password
├── Form:
│   ├── Current password
│   ├── New password (with strength indicator)
│   ├── Confirm password
│   └── Button: "Cập nhật"
│
└── Back button (navigation)
```

---

## 📊 PHẦN 3: COMPONENT REFERENCES (Có trong cả Web + Mobile)

### Custom Health Components:

```
1. HealthStatusBadge
   ├── Displays: Color + Icon + Text label
   ├── States: Normal (green) / Warning (yellow) / Critical (red)
   └── Sizes: Small, Medium, Large

2. HealthMetricCard
   ├── Expandable metric display
   ├── Shows: Name, Value, Unit, Status, Reference range
   ├── Expandable: Tap/Click → Show explanation + recommendations
   └── Mobile: Full width, Desktop: Grid

3. HealthResultSummary
   ├── Quick overview of test results
   ├── Shows: Date, Type, Status badge, Key metrics
   ├── Clickable → Go to detail view
   └── Mobile: Vertical card, Desktop: Horizontal card

4. HealthMetricsGrid
   ├── Grid layout of HealthMetricCard items
   ├── Desktop: 3 columns
   ├── Tablet: 2 columns
   └── Mobile: 1 column (full width)

5. ProfileCard
   ├── Shows: Avatar, Name, Age, Latest status
   ├── Mobile: Full width card
   ├── Desktop: Grid item (3 per row)
   └── Actions: Tap → Edit or View

6. FamilyMemberCard
   ├── Similar to ProfileCard
   ├── Relationship label
   ├── Quick status indicator
   └── Tap → View shared records

7. ReferenceRangeIndicator
   ├── Visual bar showing:
   │   ├── Min | Normal range | Max
   │   └── Current value indicator
   ├── Color coded
   └── Tooltips on hover/tap

8. HealthStatusTimeline
   ├── Vertical timeline of tests over time
   ├── Each event: Date, Status, Metrics changed
   ├── Mobile: Scrollable vertically
   └── Desktop: Full view
```

---

## 🎯 TÓNG KẾT SCREENS

```
WEB DASHBOARD: 13 screens chính (Updated)
├── Authentication: 4 (Login, Register, Reset password, Consent ⭐NEW)
├── Dashboard: 3 (Home, Records list, Record detail)
├── Profiles: 3 (List, Create/Edit, Settings)
├── Family: 2 (Management, Dashboard)
├── Admin: 5 (Dashboard, Reference data, Editor, Audit logs, MFA Setup ⭐NEW)
└── Settings: 1 (All settings in one page)

MOBILE APP: 10 screens chính (+ sub-screens) (Updated)
├── Authentication: 3 (Login, Register, Onboarding)
├── Home: 1 (With bottom tabs)
├── Upload: 4 (Options, Processing, Review, Manual Entry ⭐NEW)
├── Results: 1 (Detail view)
├── Profiles: 2 (List, Create/Edit)
├── Family: 1 (Sharing management)
└── Settings: 2 (Menu + Sub-screens)

TOTAL: 23+ unique screen designs
+ Multiple responsive breakpoints (Desktop, Tablet, Mobile)
+ Custom components used across all screens
+ Component library with 30+ components + variants

NEW SCREENS ADDED (March 2026):
├── Screen 1.1.4: Consent Screen (FR8 - NĐ 13/2023/NĐ-CP compliance)
├── Screen 1.5.5: Admin MFA Setup (FR33 - Security requirement)
└── Screen 2.3.4: OCR Failure Recovery (FR16 - Manual entry fallback)

ENHANCED SCREENS:
├── Screen 1.2.3: Added recommendation disclaimer (FR21)
├── Screen 1.5.3: Added approval workflow UI (FR36)
├── Screen 2.3.1: Added profile selector (FR17)
└── Screen 2.3.3: Added source tagging UI (FR15)

GRAND TOTAL MOCKUPS/DESIGNS: ~55-65 design artboards
(Accounting for different states, breakpoints, variants)
```

---

## 📋 DANH SÁCH SCREENS CHO FIGMA

**Khi import vào Figma, tạo pages/frames với tên sau:**

```
PAGES (Figma):
├── 0. Cover Page (done)
├── 1. TOKENS - Colors (done)
├── 2. TOKENS - Typography (done)
├── 3. TOKENS - Spacing & Grid (done)
├── 4. TOKENS - Shadows & Radius (done)
├── 5. TOKENS - Animation (done)
├── 6. COMPONENTS - Foundation (Phase 1 - done)
├── 7. COMPONENTS - Composite (Phase 2)
├── 8. COMPONENTS - Layout (Phase 2)
├── 9. COMPONENTS - Data Display (Phase 2)
├── 10. COMPONENTS - Health (Phase 3)
├── 11. PATTERNS & States (Phase 2-3)
├── 12. SCREENS - Authentication (Phase 4)
├── 13. SCREENS - Web Dashboard (Phase 4)
├── 14. SCREENS - Web Profiles (Phase 4)
├── 15. SCREENS - Web Family & Admin (Phase 4)
├── 16. SCREENS - Mobile Auth & Home (Phase 4)
├── 17. SCREENS - Mobile Upload & Results (Phase 4)
├── 18. SCREENS - Mobile Profiles & Settings (Phase 4)
└── 19. Accessibility & QA (Ongoing)
```

---

**Document Version:** 1.0  
**Created:** March 2026  
**For:** Design Planning and Implementation
