# Phase 4: Web + Mobile Screens
# Hướng dẫn Chi tiết + Prompt cho Stitch AI

**Phiên bản:** 1.0  
**Ngôn ngữ:** Tiếng Việt  
**Thời gian:** ~4-6 giờ (nhiều screens)  
**Mục tiêu:** Thiết kế 20+ screens (11 web + 9 mobile)

---

## 📋 PHẦN 1: CHUẨN BỊ PROMPT CHO PHASE 4

### Prompt cho Stitch AI - Phase 4

**Copy nguyên bản prompt này:**

```
---START PROMPT---

# HealthLens Design System - Phase 4
# Web Dashboard + Mobile App Screens

## Project Context

Dự án: HealthLens (Health Intelligence Platform)
Phase: 4 (Phase 1 + 2 + 3 đã hoàn thành - Components complete)
Mục tiêu: Thiết kế 20+ screens (Web 11 + Mobile 9)
Design System: "Calm Healthcare" - Ấm áp, thân thiện, dành cho người già
Target Users: Người Việt 58+ quản lý bệnh mãn tính
Platforms: Web (1280px+) + Mobile (360px width, iOS/Android)

## Reuse All Phase 1, 2, 3 Components

Use all foundation, composite, layout, data display, and health components:
- All Phase 1 tokens (colors, typography, spacing, shadows)
- Phase 2 components (Card, Alert, Badge, Modal, Tabs, Table, etc.)
- Phase 3 health components (HealthStatusBadge, HealthMetricCard, ProfileCard, etc.)
- Do NOT create new components - only use existing ones

---

## WEB DASHBOARD SCREENS (11 screens)

### WEB AUTHENTICATION FLOWS (3 screens)

#### Screen W1.1: Login Page (/login)

**Layout:**
- Centered form layout
- 400px width (desktop)
- Left: Form + logo + text
- Right: Illustration or gradient background (optional)

**Responsive:**
- Desktop (1280px+): Side-by-side form + image
- Tablet (768px): 2-column, form + image
- Mobile: Full width form

**Components:**
- TopNav: Logo + "HealthLens"
- Card: Main form container
- Form fields:
  - Input: Email (type: email, placeholder: "email@example.com")
  - Input: Password (type: password, placeholder: "Nhập mật khẩu")
  - Checkbox: "Ghi nhớ tôi"
- Buttons:
  - Primary: "Đăng Nhập" (full width, 48px height)
  - Secondary link: "Quên mật khẩu?" (under form)
- Footer link: "Chưa có tài khoản? Đăng Ký" (teal-600 link)
- Alert: Optional "Tài khoản hoặc mật khẩu không đúng" (error state)

**Layout Structure:**
┌─────────────────────────────────────────────┐
│ [HealthLens Logo]                           │
│                                             │
│ ĐĂNG NHẬP                                   │
│ Quản lý sức khỏe của bạn                   │
│                                             │
│ [Input: Email]                              │
│ [Input: Password]                           │
│ [ ] Ghi nhớ tôi                            │
│                                             │
│ [Đăng Nhập Button]                          │
│ Quên mật khẩu?                             │
│                                             │
│ Chưa có tài khoản? Đăng Ký                 │
└─────────────────────────────────────────────┘

**Content (Tiếng Việt):**
- Title: "Đăng Nhập"
- Subtitle: "Quản lý sức khỏe của bạn"
- Email label: "Email"
- Password label: "Mật khẩu"
- Checkbox: "Ghi nhớ tôi"
- Button: "Đăng Nhập"
- Links: "Quên mật khẩu?" | "Đăng Ký"

**Visual Design:**
- Background: White or light teal gradient
- Form bg: White card (shadow-2)
- Primary color: Teal-600
- Font: Inter
- Logo: 48×48px

---

#### Screen W1.2: Register Page (/register)

**Similar to Login but:**
- Title: "Tạo Tài Khoản"
- Form fields:
  - Input: Họ tên (Full name)
  - Input: Email address
  - Input: Ngày sinh (Date picker, format: DD/MM/YYYY)
  - Input: Mật khẩu (Password)
  - Input: Xác nhận mật khẩu (Confirm password)
  - Checkbox: "Tôi đã đọc và chấp nhận Điều khoản" (required)
- Validation: Show error/success states
- Button: "Đăng Ký"
- Footer: "Đã có tài khoản? Đăng Nhập"

**Visual Feedback:**
- Input states: Default, Focus (focus ring), Error (red border), Success (green border)
- Error message below field: "Mật khẩu phải có ít nhất 8 ký tự"
- Success message: Green text or checkmark

---

#### Screen W1.3: Password Reset Page (/reset-password)

**3-step flow shown as separate states:**

Step 1: Enter Email
- Title: "Đặt lại mật khẩu"
- Subtitle: "Nhập email để nhận liên kết đặt lại"
- Input: Email address
- Button: "Gửi liên kết"
- Info text: "Chúng tôi sẽ gửi liên kết tới email của bạn"

Step 2: Check Email (Confirmation)
- Icon: Mail illustration (120×120px, teal-300)
- Title: "Kiểm tra email của bạn"
- Message: "Chúng tôi đã gửi liên kết đặt lại mật khẩu..."
- Button: "Quay lại đăng nhập"
- Link: "Không nhận được email? Gửi lại"

Step 3: New Password
- Title: "Đặt mật khẩu mới"
- Input: New password
- Input: Confirm password
- Checkbox: Password requirements checklist
  - ✓ Ít nhất 8 ký tự
  - ✓ Chứa chữ hoa
  - ✓ Chứa chữ thường
  - ✓ Chứa số hoặc ký tự đặc biệt
- Button: "Cập nhật mật khẩu"

---

### WEB USER DASHBOARD (3 screens)

#### Screen W2.1: Home/Dashboard Page (/dashboard)

**Top-level overview of user health**

**Layout:**
- Top Navigation Bar (64px height)
  - Logo + app name (left)
  - Search bar (center) - tìm kiếm hồ sơ/kết quả
  - Notification bell (right)
  - User avatar dropdown (right)
- Sidebar Navigation (250px, left)
  - Logo + app name
  - Nav items: Dashboard (active), Hồ sơ của tôi, Chia sẻ gia đình, Cài đặt, Đăng xuất
- Main content (flex: 1)
  - Welcome section
  - Quick stats
  - Recent results
  - Quick actions

**Welcome Section:**
- Greeting: "Chào, Trần Văn A! 👋"
- Subtitle: "Đây là tình trạng sức khỏe của bạn hôm nay"
- Date: "Thứ ba, 24 tháng 3 năm 2026"

**Quick Health Stats:**
- Grid of 4 HealthMetricCard components (responsive: 2 columns on tablet, 1 on mobile)
  - Blood pressure: "120/80 mmHg" + "✓ Bình thường" badge
  - Glucose: "95 mg/dL" + "✓ Bình thường" badge
  - Cholesterol: "200 mg/dL" + "✓ Bình thường" badge
  - BMI: "24.5 kg/m²" + "✓ Bình thường" badge
- Each card clickable to see details

**Recent Results Section:**
- Title: "Kết quả gần đây"
- HealthResultSummary components (3-4 results as Cards)
  - Xét nghiệm máu (15/03/2026)
  - Xét nghiệm nước tiểu (10/03/2026)
  - Xét nghiệm chức năng gan (05/03/2026)
- "Xem tất cả" link (teal-600)

**Quick Actions:**
- Grid of QuickActionCard components (6 actions: 3 columns)
  - 📤 Tải kết quả
  - 📅 Đặt lịch khám
  - 📊 Xem kết quả
  - 📤 Chia sẻ
  - 📞 Liên hệ BS
  - ❓ Trợ giúp

**Visual Layout:**
┌─ TopNav ─────────────────────────────────┐
├─ Sidebar ──────────────────────────────┐ │
│  Dashboard  │ Welcome Card             │ │
│  Hồ sơ      │ Chào Trần Văn A!         │ │
│  Gia đình   │ 24/03/2026               │ │
│  Cài đặt    │                          │ │
│  Đăng xuất  ├──────────────────────────┤ │
│             │ Quick Stats (4 cards)    │ │
│             │ BP  │ Glucose │ BMI │ Chol
│             ├──────────────────────────┤ │
│             │ Recent Results (3 cards)  │ │
│             │ [Xét nghiệm] [Test] [Test
│             ├──────────────────────────┤ │
│             │ Quick Actions (6 cards)   │ │
│             │ [Upload] [Schedule] ...   │ │
└──────────────────────────────────────────┘

**Responsive:**
- Desktop: Full layout with sidebar (250px) + content
- Tablet (768px): Sidebar collapses to icons (60px)
- Mobile: Sidebar hidden, hamburger menu in TopNav

---

#### Screen W2.2: My Profile Page (/my-profile)

**User profile management**

**Breadcrumb:**
- "Dashboard / Hồ sơ của tôi"

**Main Layout:**
- Title: "Hồ sơ của tôi"
- 2 sections: Profile Info + Health History

**Section 1: Profile Information Card**
- Avatar (64×64px) with upload button
- Form fields:
  - Họ tên: "Trần Văn A"
  - Email: "tranurana@email.com"
  - Ngày sinh: "15/04/1960"
  - Giới tính: "Nam" (Radio: Nam/Nữ)
  - Số điện thoại: "0912345678"
  - Địa chỉ: "123 Đường Nguyễn, Q.1, TP.HCM"
- Buttons:
  - Primary: "Lưu thay đổi"
  - Secondary: "Hủy"
  - Tertiary link: "Đổi mật khẩu"
- Alert box (if edited): "Thay đổi đã được lưu"

**Section 2: Health Information**
- Title: "Thông tin sức khỏe"
- Form fields:
  - Bệnh mãn tính: Checkboxes
    - [ ] Tiểu đường
    - [ ] Huyết áp cao
    - [ ] Cholesterol cao
    - [ ] Bệnh tim
    - [ ] Hen suyễn
  - Thuốc hiện tại: Text area
  - Dị ứng: Checkboxes
  - Ghi chú khác: Text area
- Buttons: "Lưu" / "Hủy"

**Section 3: Account Settings**
- Change password link
- Two-factor authentication toggle
- Delete account button (red-600, danger)

**Visual Structure:**
┌─ Breadcrumb ──────────────────────┐
├─ Title ────────────────────────────┤
│ Hồ sơ của tôi                     │
├─ Card 1: Profile Info ────────────┤
│ [Avatar] [Upload]                 │
│ Họ tên: [Input] ✓                │
│ Email: [Input]                    │
│ ...                               │
│ [Lưu thay đổi] [Hủy]             │
├─ Card 2: Health Info ─────────────┤
│ Bệnh mãn tính:                    │
│ [ ] Tiểu đường [ ] Huyết áp cao  │
│ ...                               │
│ [Lưu] [Hủy]                      │
├─ Card 3: Account ─────────────────┤
│ Đổi mật khẩu →                   │
│ Two-factor auth: [Toggle]         │
│ [Xóa tài khoản] (red)            │
└────────────────────────────────────┘

---

#### Screen W2.3: Family Sharing Page (/family)

**Manage family members and shared health records**

**Title:** "Chia sẻ gia đình"

**Section 1: Invite New Member**
- Card with form:
  - Input: Email address
  - Dropdown: Relationship (Vợ, Chồng, Mẹ, Cha, Con, Khác)
  - Button: "Gửi lời mời"
- Alert: "Lời mời đã được gửi" (success) or "Gửi lại" link

**Section 2: Family Members List**
- Title: "Thành viên gia đình"
- List of FamilyMemberListItem components
  - Each item shows: Avatar, Name, Age, Relationship, Last check-up, Health status badge
  - Actions (hover): View, Edit, Remove
- If no members: EmptyState component
  - Icon: Family icon
  - Message: "Chưa có thành viên gia đình"
  - Button: "Mời thành viên đầu tiên"

**Section 3: Pending Invitations**
- Title: "Lời mời đang chờ"
- List of pending invites:
  - Email, Relationship, Sent date
  - Actions: "Gửi lại", "Hủy"
- If no pending: Gray text "Không có lời mời đang chờ"

**Visual Structure:**
┌─ Title: Chia sẻ gia đình ──────────┐
├─ Card: Mời thành viên ────────────┤
│ Email: [Input]                    │
│ Quan hệ: [Dropdown]               │
│ [Gửi lời mời]                    │
├─ List: Thành viên ────────────────┤
│ [Avatar] Trần Thị B, 60            │
│ Vợ | Cập nhật: 23/03 | [✓ Bình t] │
│ [View] [Edit] [Remove]            │
│                                   │
│ [Avatar] Trần Văn C, 30            │
│ Con | Cập nhật: 20/03 | [✓ Bình t] │
│ [View] [Edit] [Remove]            │
├─ Pending Invitations ─────────────┤
│ john@email.com (Mẹ) - Gửi: 22/03 │
│ [Gửi lại] [Hủy]                  │
└────────────────────────────────────┘

---

### WEB HEALTH DATA SCREENS (4 screens)

#### Screen W3.1: Health Results List Page (/results)

**Display all health test results**

**Breadcrumb:** "Dashboard / Kết quả"

**Filter/Search Section:**
- Tabs: "Tất cả" | "Xét nghiệm máu" | "Xét nghiệm nước tiểu" | "Siêu âm"
- Search input: "Tìm kiếm kết quả..."
- Filter dropdowns (optional):
  - Ngày (Last 30 days, 90 days, year, all)
  - Status (All, Normal, Monitor, Concern)
- Sort: By date (newest first)

**Results Display:**
- HealthResultSummary components in a grid (2 columns on desktop, 1 on mobile)
  - Each card shows: Type, Date, Metrics, Status badge
  - Clickable to view full details
  - Hover: Shadow increase, cursor pointer
- If no results: EmptyState
  - Icon: Document/chart icon
  - Message: "Chưa có kết quả nào"
  - Button: "Tải kết quả đầu tiên"

**Pagination:**
- Show 10 results per page
- Pagination component at bottom (if 10+ results)

**Visual Structure:**
┌─ Breadcrumb + Title ──────────────┐
├─ Tabs ────────────────────────────┤
│ [Tất cả] [Xét nghiệm máu] ...    │
├─ Search + Filters ────────────────┤
│ [Search Input] [Filter] [Sort]   │
├─ Results Grid ────────────────────┤
│ ┌─────────────┐  ┌─────────────┐ │
│ │ Xét nghiệm  │  │ Xét nghiệm  │ │
│ │ 15/03/2026  │  │ 10/03/2026  │ │
│ │ ✓ Bình t.   │  │ ⚠ Chú ý     │ │
│ │ [View]      │  │ [View]      │ │
│ └─────────────┘  └─────────────┘ │
├─ Pagination ──────────────────────┤
│ ← [1] [2] [3] → (if 10+ results) │
└────────────────────────────────────┘

---

#### Screen W3.2: Health Result Detail Page (/results/:id)

**Show full details of single result**

**Breadcrumb:** "Dashboard / Kết quả / [Result Type]"

**Header Section:**
- Icon + Type name + Date
- Overall status badge (large)
- Doctor name (if provided)
- Action buttons: "Chia sẻ" | "In" | "Xóa"

**Health Metrics Section:**
- Grid of metrics with values, reference ranges
- Use ReferenceRangeIndicator components
- Each metric: Name | Value | Unit | Status | Reference range

**Doctor Notes Section:**
- If present: Display doctor notes in Card
- Italic, slate-600 color
- Title: "Ghi chú của bác sĩ"

**Timeline Section (optional):**
- Use HealthStatusTimeline component
- Show: When test was done, when results received, any follow-ups

**Related Results Section:**
- Show 3-4 previous similar tests
- Link to "Xem lịch sử"

**Action Buttons (bottom):**
- Primary: "Tải xuống PDF"
- Secondary: "Chia sẻ với bác sĩ"
- Tertiary: "Xóa kết quả"

**Visual Structure:**
┌─ Breadcrumb ──────────────────────┐
├─ Header ──────────────────────────┤
│ 🧪 Xét nghiệm máu      15/03/2026 │
│         [✓ Bình thường]            │
│ BS Nguyễn Văn A                   │
│ [Chia sẻ] [In] [Xóa]             │
├─ Metrics ────────────────────────┤
│ Huyết áp tâm trương               │
│ 80 mmHg | Bình thường: 60-100    │
│ [Visual range indicator]          │
│                                   │
│ Đường huyết                       │
│ 95 mg/dL | Bình thường: 70-100   │
│ [Visual range indicator]          │
├─ Doctor Notes ────────────────────┤
│ "Kết quả bình thường. Tiếp tục    │
│  theo dõi hàng tuần"              │
├─ Related Results ─────────────────┤
│ [Previous test 1] [Previous 2] ... │
│ [Xem lịch sử →]                   │
└────────────────────────────────────┘

---

#### Screen W3.3: Upload Results Page (/upload)

**Upload new health test results**

**Breadcrumb:** "Dashboard / Tải kết quả"

**Title Section:**
- Title: "Tải kết quả xét nghiệm"
- Subtitle: "Tải ảnh hoặc PDF từ kết quả xét nghiệm của bạn"

**File Upload Section:**
- Drag-and-drop area (dashed border, teal-500)
  - Icon: Upload arrow (📤, 48×48px)
  - Text: "Kéo và thả tệp ở đây hoặc nhấp để chọn"
  - Supported formats: JPG, PNG, PDF (max 5MB)
- Progress bar (while uploading)
- File list (after upload):
  - File name | File size | Upload date | Status (✓ Success)
  - Remove button (X icon)

**Form Section (Post Upload):**
- Dropdown: Test type (Xét nghiệm máu, Nước tiểu, Siêu âm, Khác)
- Input: Doctor name (optional)
- Input: Facility/Hospital (optional)
- Date picker: Test date (default: today)
- Notes: Text area (optional)

**Buttons:**
- Primary: "Tải lên" (disabled until file selected)
- Secondary: "Hủy"

**Success State:**
- Alert: "Tệp đã được tải lên thành công!" (green)
- Link: "Xem kết quả"
- Offer: "Tải kết quả khác"

**Error State:**
- Alert: "Tệp quá lớn hoặc định dạng không hỗ trợ" (red)
- Show supported formats again

**Visual Structure:**
┌─ Title ────────────────────────────┐
├─ Upload Area ─────────────────────┤
│  ┌──────────────────────────────┐ │
│  │                              │ │
│  │        📤                    │ │
│  │                              │ │
│  │ Kéo và thả tệp ở đây         │ │
│  │ hoặc nhấp để chọn            │ │
│  │                              │ │
│  │ JPG, PNG, PDF (Max 5MB)      │ │
│  └──────────────────────────────┘ │
├─ Form Section ────────────────────┤
│ Loại xét nghiệm: [Dropdown]      │
│ Bác sĩ: [Input]                 │
│ Cơ sở y tế: [Input]             │
│ Ngày xét nghiệm: [Date picker]   │
│ Ghi chú: [Text area]            │
├─ Buttons ────────────────────────┤
│ [Tải lên] [Hủy]                 │
└────────────────────────────────────┘

---

### WEB ADMIN SCREENS (1 screen - optional for Phase 4)

#### Screen W4.1: Settings Page (/settings)

**User preferences and account settings**

**Breadcrumb:** "Dashboard / Cài đặt"

**Tabs:**
- "Chung" (General) - active
- "Thông báo" (Notifications)
- "Bảo mật" (Security)
- "Quyền riêng tư" (Privacy)

**Tab 1: General Settings**
- Language: English / Tiếng Việt (radio)
- Theme: Light / Dark mode (toggle)
- Font size: Normal / Large / Extra large (radio buttons)
- Time zone: [Dropdown] (GMT+7)
- Temperature unit: °C / °F (radio)

**Tab 2: Notifications**
- Email notifications toggle
- SMS notifications toggle
- Push notifications toggle
- Notification preferences:
  - [ ] Test results ready
  - [ ] Appointment reminders
  - [ ] Health tips
  - [ ] Weekly summary
- Quiet hours: From XX:XX to XX:XX

**Tab 3: Security**
- Change password button
- Two-factor authentication toggle
- Active sessions list
  - Current device: Chrome on Windows
  - Last activity: Today 14:30
  - [Sign out]
- Login activity
  - Latest login: 24/03/2026 09:15 from IP 192.168.1.1

**Tab 4: Privacy**
- Data sharing toggle
- Third-party access toggle
- Export data button
- Delete account button (red-600, requires confirmation)

**Visual Structure:**
┌─ Breadcrumb ──────────────────────┐
├─ Tabs ────────────────────────────┤
│ [Chung] [Thông báo] [Bảo mật] ... │
├─ Tab Content: General ────────────┤
│ Ngôn ngữ:                         │
│ ◉ English  ○ Tiếng Việt          │
│                                   │
│ Giao diện:                        │
│ ◉ Sáng  ○ Tối                    │
│                                   │
│ Kích thước chữ:                   │
│ ○ Bình thường  ○ Lớn  ◉ Rất lớn │
│                                   │
│ Múi giờ: [GMT+7 HCM]              │
│ Đơn vị nhiệt độ: [°C] [°F]       │
│                                   │
│ [Lưu thay đổi]                   │
└────────────────────────────────────┘

---

---

## MOBILE APP SCREENS (9 screens)

Mobile screens are 360px width (iPhone 12 size), showing full vertical layout.

### MOBILE AUTHENTICATION (2 screens)

#### Screen M1.1: Mobile Login (/login)

**Similar to web but mobile-optimized:**
- Full width (360px)
- No sidebar
- Top: HealthLens logo (32×32px)
- Form centered:
  - Input: Email
  - Input: Password
  - Checkbox: Remember me
  - Button: Login (full width, 48px height)
  - Link: Forgot password
- Bottom: Sign up link

**Visual:**
- White background
- Form in card (shadow-1)
- Padding: 16px all sides
- Spacing: md (12px) between elements

---

#### Screen M1.2: Mobile Register (/register)

**Similar to web but mobile version:**
- Form fields: Name, Email, DOB, Password, Confirm, Terms checkbox
- Scroll vertically if needed
- Button: Register (full width)
- Link: Already have account? Login

---

### MOBILE DASHBOARD (3 screens)

#### Screen M2.1: Mobile Home (/dashboard)

**Mobile-optimized home screen**

**Top Navigation:**
- Left: Hamburger menu (3 lines icon)
- Center: "HealthLens" title (16px)
- Right: Notification bell icon

**Sidebar (when opened):**
- Overlays main content (transparent backdrop)
- Navigation items: Dashboard, Profile, Family, Settings, Logout
- Avatar + name at top

**Main Content:**
- Welcome card: "Chào, Trần Văn A"
- Quick stats: Stack vertically (1 column)
  - HealthMetricCard components (stacked)
- Recent results: Stack vertically (cards)
- Quick actions: Grid 2×3 (2 columns, 3 rows)
  - QuickActionCard components

**Visual (scrollable content):**
┌─ TopNav ──────────────────────────┐
│ ☰  HealthLens  🔔               │
├─ Welcome Card ───────────────────┤
│ Chào, Trần Văn A                 │
│ 24/03/2026                       │
├─ Quick Stats ─────────────────────┤
│ [Metric Card: BP]                │
│ [Metric Card: Glucose]           │
│ [Metric Card: BMI]               │
│ [Metric Card: Cholesterol]       │
├─ Recent Results ──────────────────┤
│ [Result Summary Card 1]          │
│ [Result Summary Card 2]          │
├─ Quick Actions ───────────────────┤
│ [Upload] [Schedule]              │
│ [View] [Share]                   │
│ [Contact] [Help]                 │
└────────────────────────────────────┘

---

#### Screen M2.2: Mobile Profile (/my-profile)

**Mobile-optimized profile page**

**Top:** Back arrow + "Hồ sơ của tôi" title

**Profile Section:**
- Avatar (64×64px) with upload button overlay
- Name, email, DOB, gender, phone, address
- Edit button: Opens edit form

**Health Section:**
- Chronic diseases checkboxes
- Current medications
- Allergies
- Notes
- Edit button

**Account Section:**
- Change password link
- Two-factor toggle
- Delete account link

**Action:**
- Save button (at bottom, fixed or floating)
- Cancel button

**Visual:**
┌─ TopNav ──────────────────────────┐
│ ← Hồ sơ của tôi                  │
├─ Profile Card ────────────────────┤
│ [Avatar +]                       │
│ Trần Văn A                       │
│ Email: tranurana@email.com       │
│ DOB: 15/04/1960                  │
│ [Edit]                           │
├─ Health Section ──────────────────┤
│ Bệnh mãn tính:                   │
│ [ ] Tiểu đường [ ] Huyết áp cao │
│ ...                              │
├─ Account ────────────────────────┤
│ Đổi mật khẩu →                   │
│ Two-factor: [Toggle]             │
│ [Xóa tài khoản] (red)            │
├─ Action Buttons (fixed) ──────────┤
│ [Lưu] [Hủy]                     │
└────────────────────────────────────┘

---

#### Screen M2.3: Mobile Family (/family)

**Mobile version of family sharing**

**Invite Section:**
- Title: "Mời thành viên"
- Input: Email
- Dropdown: Relationship
- Button: "Gửi"

**Members List:**
- Title: "Thành viên gia đình"
- List of family members (vertical stack)
  - FamilyMemberListItem components
  - Tap to see options: View, Edit, Remove

**Pending Invites:**
- Title: "Đang chờ"
- List of pending invites
- Actions: Resend, Cancel

---

### MOBILE HEALTH DATA (3 screens)

#### Screen M3.1: Mobile Results List (/results)

**Mobile-optimized results list**

**Tabs:**
- Swipeable horizontal tabs: "Tất cả" | "Máu" | "Nước tiểu" | "Siêu âm"
- Or collapsible filter section

**Search:**
- Search input (full width)

**Results:**
- Stack vertically (full width minus padding)
- HealthResultSummary components (1 per row)
- Infinite scroll or pagination

**Empty state:**
- Icon: Document icon
- Message: "Chưa có kết quả"
- Button: "Tải lên"

---

#### Screen M3.2: Mobile Result Detail (/results/:id)

**Full result details optimized for mobile**

**Header:**
- Back arrow + result type
- Status badge (large)
- Date

**Metrics:**
- Stack vertically (full width)
- ReferenceRangeIndicator components
- Each metric: Name | Value | Range | Status

**Doctor notes:**
- If present: Card with text

**Actions:**
- Bottom buttons (fixed or floating):
  - Share
  - Download
  - Delete

---

#### Screen M3.3: Mobile Upload (/upload)

**Mobile file upload optimized**

**File Upload:**
- Drag-and-drop area (tappable for file picker)
- Or camera button (take photo)
- File list after upload

**Form:**
- Test type dropdown
- Doctor name input
- Date picker
- Notes textarea

**Buttons:**
- Upload (full width)
- Cancel

---

---

## RESPONSIVE DESIGN NOTES

**Desktop (1280px+):**
- Sidebar visible (250px fixed)
- Content area responsive
- Multiple columns where applicable
- TopNav 64px height

**Tablet (768px - 1279px):**
- Sidebar collapses to icons (60px)
- Content adjusts to fit
- 2 columns for grids
- TopNav 64px height

**Mobile (360px - 767px):**
- No sidebar (hamburger menu instead)
- Full width content
- 1-2 columns for grids (single recommended)
- TopNav 56px height
- Bottom navigation optional for mobile
- Fixed buttons/action areas

---

## COLOR & TYPOGRAPHY STANDARDS

All screens MUST use:
- Phase 1 color tokens (no hard-coded hex)
- Phase 1 typography styles
- Phase 1 spacing (8px grid)
- Phase 1 shadows
- Phase 1 border radius
- Phase 2 & 3 components

---

## OUTPUT EXPECTATIONS

1. **Figma file** with all 20 screens
2. **Page organization:**
   - Pages 12-18: SCREENS - Web (11 screens on one or two pages)
   - Pages 19-20: SCREENS - Mobile (9 screens on one or two pages)
3. **Each screen** fully designed with real content
4. **Proper layers** organized and labeled
5. **Responsive variants** (desktop/tablet/mobile) shown
6. **Component reuse** - use all Phase 1, 2, 3 components
7. **Accessibility** - Focus states on interactive elements
8. **Vietnamese content** - All text in Tiếng Việt

## DESIGN QUALITY CHECKLIST

- [ ] All 11 web screens designed
- [ ] All 9 mobile screens designed (360px width)
- [ ] All screens use Phase 1, 2, 3 components
- [ ] No hard-coded colors (use Phase 1 styles)
- [ ] Typography from Phase 1 (no manual fonts)
- [ ] Spacing 8px grid aligned
- [ ] Focus rings on interactive elements
- [ ] Responsive variants shown (desktop/mobile)
- [ ] Vietnamese text throughout
- [ ] Screens properly organized in pages
- [ ] Component instances linked correctly
- [ ] All content realistic and complete

---END PROMPT---
```

---

## 📍 PHẦN 2: THỰC HIỆN PHASE 4

### Bước 1-7: Giống Phase 2 & 3
(Copy prompt → Stitch AI → Generate → Import → QA)

### Lưu ý: SCREENS có nhiều chi tiết hơn

Stitch AI sẽ cần:
- Thời gian lâu hơn (20-30 phút, không phải 10 phút)
- Chi tiết cao (tất cả text, form fields, buttons)
- Responsive variants (desktop/tablet/mobile)
- Tất cả components từ Phase 1, 2, 3

### Timeline Phase 4
```
Phase 4 Total Time: ~4-6 giờ

0:00-0:15 | Prepare + Copy prompt
0:15-1:45 | Stitch AI generate (20-30 min)
1:45-3:00 | Import + Organize in Figma
3:00-4:30 | QA + Verify all screens
4:30-5:30 | Fix content + Documentation
5:30-6:00 | Final export + Team sharing

Total: 4-6 giờ
```

---

## 📊 FULL DESIGN SYSTEM TIMELINE

```
Phase 1: Foundation      3-4 hours  ✅ COMPLETE
Phase 2: Components      3-4 hours  🔄 READY TO START
Phase 3: Health Comps    2-3 hours  📋 READY (after Phase 2)
Phase 4: Screens         4-6 hours  📋 READY (after Phase 3)
─────────────────────────
TOTAL: 12-17 hours

All files will be in:
/home/vmhieu/Workspace/UIT/health-lens/design-artifacts/
```

---

**Document Version:** 1.0  
**Language:** Tiếng Việt  
**Created:** March 2026  
**Status:** Ready for Phase 3 → Phase 4 Execution
