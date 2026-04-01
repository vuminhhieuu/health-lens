# Phase 2: Composite + Layout + Data Display Components
# Hướng dẫn Chi tiết + Prompt cho Stitch AI

**Phiên bản:** 1.0  
**Ngôn ngữ:** Tiếng Việt  
**Thời gian:** ~3-4 giờ  
**Mục tiêu:** Tạo 24+ components (Composite, Layout, Data Display) trong Figma

---

## 📋 PHẦN 1: CHUẨN BỊ PROMPT CHO PHASE 2

### Prompt cho Stitch AI - Phase 2

**Copy nguyên bản prompt này:**

```
---START PROMPT---

# HealthLens Design System - Phase 2
# Composite + Layout + Data Display Components

## Project Context

Dự án: HealthLens (Health Intelligence Platform)
Phase: 2 (Phase 1 Foundation Components đã hoàn thành)
Mục tiêu: Tạo 24+ Composite, Layout, Data Display components
Design System: "Calm Healthcare" - Ấm áp, thân thiện, dành cho người già
Thời gian: Phase 2

## Reuse Foundation Components from Phase 1

Sử dụng lại tất cả colors, typography, spacing từ Phase 1:
- Colors/Primary/Teal-* (all variants)
- Colors/Status/* (Success, Warning, Critical, Info)
- Colors/Neutral/Slate-* (all variants)
- Colors/Functional/* (Focus, Error, Success, Disabled)
- Typography/* (Caption to Display)
- All spacing tokens (4px to 64px)
- All radius tokens (0px to full)
- All shadow tokens (shadow-1 to shadow-5)
- Foundation components: Button, Input, Label, Checkbox, Radio, Toggle, Select

## COMPOSITE COMPONENTS (7 components)

### 1. Card Component

**Purpose:** Container for content with consistent styling

**Visual Properties:**
- Background: white (#FFFFFF)
- Border: 1px solid slate-200 (#E2E8F0)
- Padding: lg (16px) - can be customizable
- Border radius: lg (12px)
- Shadow: shadow-2 (default)

**States (4 total):**

State 1: Resting (Default)
├── Shadow: shadow-2
├── Background: white
├── Border: 1px slate-200
└── Ready to interact

State 2: Hover
├── Shadow: shadow-3 (elevated)
├── Background: slate-50 (subtle tint)
├── Cursor: pointer
└── Indicates interactivity

State 3: Focus (Keyboard)
├── Border: 2px solid teal-500
├── Shadow: shadow-3
├── Focus ring: 2-3px teal-500
└── Clear keyboard focus

State 4: Selected/Active
├── Background: teal-50
├── Border: 2px teal-500
├── Shadow: shadow-3
└── Indicates selection

**Structure:**
- Card container (frame)
- Header section (optional)
- Content area (flex, scrollable)
- Footer section (optional)
- Can contain any content (text, forms, cards)

**Component Naming:**
- Card / Default
- Card / Hover
- Card / Focus
- Card / Selected

**Real-world examples to show:**
- Health result card (white bg, with metrics inside)
- Profile card (avatar + name + info)
- Info card (icon + title + description)
- Form card (with form fields inside)

---

### 2. Alert Component (4 types)

**Purpose:** Display important messages/notifications

**General Structure:**
- Icon (left side) - 24×24px
- Content area (title + description)
- Close button (optional, right side)
- Color-coded by type

**Type 1: Alert / Info**
├── Icon: Info circle (ⓘ) - blue-600
├── Background: blue-50 (#EFF6FF)
├── Border-left: 4px solid blue-600 (#2563EB)
├── Title font: 16px, weight 600, blue-900
├── Description font: 14px, weight 400, blue-700
├── Close button: Optional (X icon)
└── Use: Informational messages

**Type 2: Alert / Warning**
├── Icon: Alert triangle (⚠) - yellow-600
├── Background: yellow-50 (#FFFBEB)
├── Border-left: 4px solid yellow-600 (#D97706)
├── Title font: 16px, weight 600, yellow-900
├── Description font: 14px, weight 400, yellow-700
├── Close button: Optional
└── Use: Caution/Monitor messages

**Type 3: Alert / Error**
├── Icon: X or alert (✕/!) - red-600
├── Background: red-50 (#FEF2F2)
├── Border-left: 4px solid red-600 (#DC2626)
├── Title font: 16px, weight 600, red-900
├── Description font: 14px, weight 400, red-700
├── Close button: Yes (dismissible)
└── Use: Error messages

**Type 4: Alert / Success**
├── Icon: Check mark (✓) - green-600
├── Background: green-50 (#F0FDF4)
├── Border-left: 4px solid green-600 (#059669)
├── Title font: 16px, weight 600, green-900
├── Description font: 14px, weight 400, green-700
├── Close button: Optional
├── Animation: Fade in (200ms ease-out)
└── Use: Success/confirmation messages

**States for each alert type (3 states):**
- Default (with message)
- Hover (if dismissible, show highlight on close button)
- Closed/Dismissed (animation: fade out 200ms)

**Example content:**
- Info: "Cập nhật sẽ có hiệu lực trong 24 giờ"
- Warning: "Giá trị này cao hơn bình thường, vui lòng chú ý"
- Error: "Không thể tải dữ liệu, vui lòng thử lại"
- Success: "Kết quả đã được lưu thành công!"

**Component Naming:**
- Alert / Info
- Alert / Info-Hover
- Alert / Info-Closed
- Alert / Warning
- Alert / Warning-Hover
- Alert / Error
- Alert / Error-Hover
- Alert / Success
- Alert / Success-Hover

---

### 3. Badge Component (4 semantic types)

**Purpose:** Labels, status indicators, tags

**Visual Properties:**
├── Padding: 4px vertical, 8px horizontal (xs)
├── Border radius: sm (4px) or md (8px)
├── Font: Inter 12px, weight 500
├── Height: ~24px (auto)
└── Display: inline-block

**Type 1: Badge / Neutral**
├── Background: slate-200 (#E2E8F0)
├── Text: slate-900 (#0F172A)
├── Use: Generic labels, tags
└── Example: "Tag", "Label"

**Type 2: Badge / Success**
├── Background: green-100 or green-50
├── Text: green-900 or green-700
├── Icon: Optional ✓ (green-600)
├── Use: "Bình thường", "Hoàn thành", status OK
└── Example: Badge with green background and checkmark

**Type 3: Badge / Warning**
├── Background: yellow-100 or yellow-50
├── Text: yellow-900 or yellow-700
├── Icon: Optional ⚠ (yellow-600)
├── Use: "Cần chú ý", "Caution", warning status
└── Example: Badge with yellow background and warning icon

**Type 4: Badge / Error**
├── Background: red-100 or red-50
├── Text: red-900 or red-700
├── Icon: Optional ✕ or ! (red-600)
├── Use: "Cần hành động", "Error", error status
└── Example: Badge with red background and X icon

**Additional Variant: Removable Badge**
├── Add X button on right side
├── Hover: Show close indicator
├── Click: Remove/dismiss badge
├── Animation: Fade out (150ms ease-out)
└── Useful for: Tag inputs, filter chips

**States for each badge type (2 states):**
- Default (showing)
- Removable (with X button visible)

**Component Naming:**
- Badge / Neutral
- Badge / Success
- Badge / Warning
- Badge / Error
- Badge / Success-Removable
- Badge / Warning-Removable
- Badge / Error-Removable

---

### 4. Chip Component

**Purpose:** Selectable tags, removable tokens in inputs

**Visual Properties:**
├── Padding: 6px horizontal, 4px vertical
├── Border radius: full (9999px) - pill shape
├── Font: Inter 13px, weight 500
├── Height: ~28px
├── Avatar (optional, left): 20×20px circle
└── Remove button (optional, right): X icon

**States (4 total):**

State 1: Unselected
├── Background: slate-200 (#E2E8F0)
├── Text: slate-900 (#0F172A)
├── Border: 1px slate-300 (#CBD5E1)
├── Cursor: pointer
└── Ready to select

State 2: Selected
├── Background: teal-600 (#0D9488) or teal-100
├── Text: white (if teal-600) or teal-900 (if teal-100)
├── Border: 2px teal-600
├── Cursor: pointer
└── Selected appearance

State 3: Hover
├── Background: shade darker/lighter
├── Opacity: 90%
└── Indicates hover state

State 4: Removable (on hover)
├── Show X button (right side)
├── Click X: Remove chip
├── Animation: Fade out (150ms ease-out)
└── Useful for: Form inputs with removable items

**Use Cases:**
- Filter chips in search/filters
- Removable tags in input fields
- Selectable options in multi-select

**Example content:**
- Chip: "Tiểu đường" (unselected)
- Chip: "Huyết áp cao" (selected, teal)
- Chip with remove: "Dị ứng" × (teal with X)

**Component Naming:**
- Chip / Unselected
- Chip / Selected
- Chip / Hover
- Chip / Removable

---

### 5. Modal / Dialog Component

**Purpose:** Important actions, confirmations, forms in overlay

**Visual Properties:**
├── Overlay/Backdrop: Semi-transparent dark (rgba(0,0,0,0.5))
├── Modal box: Center position
├── Width: 480px (desktop) / 95% (mobile)
├── Max-height: 90vh
├── Border radius: lg (12px)
├── Shadow: shadow-4 (prominent)
├── Padding: 24px (2xl)
└── Focus trap: Keyboard navigation inside modal only

**Structure:**
- Header section (title + close button)
- Content section (scrollable if long)
- Footer section (action buttons)
- Animation: Slide up + fade in (200ms ease-out)

**Header:**
├── Title: "Xác nhận hành động" (18px-24px, weight 600)
├── Close button: X icon (top right, teal-600 on hover)
└── Divider: Optional (1px slate-200)

**Content:**
├── Message or form fields
├── Padding: 24px top/bottom
├── Font: 16px body text
├── Scrollable if > 50% viewport
└── Max-height: 60vh (to show footer)

**Footer:**
├── Action buttons (typically 2):
│   ├── Primary button (right): "Xác nhận", "Lưu", etc. - teal-600
│   └── Secondary button (left): "Hủy" - slate-200
├── Full width buttons on mobile
├── Gap between buttons: md (12px)
├── Button height: 40px
└── Padding: 16px top

**3 Modal Variants:**

Variant 1: Modal / Confirmation
├── Title: "Xác nhận"
├── Content: "Bạn có chắc muốn xóa?" (warning message)
├── Buttons: "Xóa" (danger/red), "Hủy"
├── Tone: Clear warning
└── Example: Delete confirmation

Variant 2: Modal / Form
├── Title: "Tạo hồ sơ mới"
├── Content: Form fields (scrollable)
├── Buttons: "Lưu", "Hủy"
├── Tone: Neutral, constructive
└── Example: Create/Edit modal

Variant 3: Modal / Info
├── Title: "Thông báo"
├── Content: Information message
├── Buttons: "Đóng" or "OK"
├── Tone: Neutral, informational
└── Example: Info/success notification

**States for each modal (3 states):**
- Closed (not visible, reference)
- Open (visible, with animation)
- Loading (primary button shows spinner, disabled)

**Component Naming:**
- Modal / Confirmation
- Modal / Confirmation-Loading
- Modal / Form
- Modal / Form-Loading
- Modal / Info
- Modal / Info-Loading

---

### 6. Tooltip Component

**Purpose:** Contextual help on hover/focus

**Visual Properties:**
├── Box: 
│   ├── Background: slate-900 (#0F172A)
│   ├── Text: white
│   ├── Padding: 8px 12px
│   ├── Border radius: md (8px)
│   ├── Shadow: shadow-3
│   ├── Font: 12px, weight 400
│   └── Max-width: 250px
├── Arrow: 6×6px triangle (pointing to trigger)
└── Animation: Fade in + slide (150ms ease-out)

**Structure:**
- Trigger element (icon, text, button)
- Tooltip box (appears on hover/focus)
- Arrow pointing from tooltip to trigger

**Position Variants (4 positions):**

Position 1: Top
├── Arrow: Points down
├── Tooltip: Above trigger
└── Default positioning

Position 2: Bottom
├── Arrow: Points up
├── Tooltip: Below trigger
└── DEFAULT most common

Position 3: Left
├── Arrow: Points right
├── Tooltip: Left of trigger
└── For edge cases

Position 4: Right
├── Arrow: Points left
├── Tooltip: Right of trigger
└── For edge cases

**Behavior:**
- Trigger: Hover or keyboard focus
- Show: Instant or slight delay (100ms)
- Hide: On mouse out or focus lost
- Dismiss: Press Escape key
- No animation on mobile (auto-show on tap)

**Example tooltips:**
- On info icon: "Giá trị này so sánh với bình thường"
- On settings icon: "Cài đặt ứng dụng"
- On help button: "Trợ giúp và hỗ trợ"
- On warning icon: "Cần theo dõi thêm"

**Component Naming:**
- Tooltip / Top
- Tooltip / Bottom
- Tooltip / Left
- Tooltip / Right

---

### 7. Toast / Notification Component

**Purpose:** Brief messages, feedback (appears and auto-dismisses)

**Visual Properties:**
├── Container:
│   ├── Width: 100% (mobile) / 380px (desktop)
│   ├── Padding: 12px 16px
│   ├── Border radius: lg (12px)
│   ├── Shadow: shadow-3
│   ├── Position: Bottom-center or top-right
│   └── Stack: Multiple toasts stack vertically
├── Content:
│   ├── Icon (left): 24×24px
│   ├── Message text: 14px
│   ├── Close button (right): X icon, optional
│   └── Auto-dismiss: 3-8 seconds

**3 Toast Types:**

Type 1: Toast / Success
├── Icon: ✓ (green-600)
├── Background: green-600 (#059669)
├── Text: white
├── Message: "Kết quả đã được lưu!"
├── Duration: 3-5 seconds (auto-dismiss)
└── Color: Green

Type 2: Toast / Error
├── Icon: ✕ (white)
├── Background: red-600 (#DC2626)
├── Text: white
├── Message: "Có lỗi xảy ra, vui lòng thử lại"
├── Duration: 5-8 seconds (user may need to read)
└── Color: Red

Type 3: Toast / Info
├── Icon: ℹ️ (circle i, blue-600)
├── Background: blue-600 (#2563EB)
├── Text: white
├── Message: "Dữ liệu đang được cập nhật"
├── Duration: 3 seconds
└── Color: Blue

**States for each toast (3 states):**
- Default (showing)
- Hover: Show close (X) button
- Dismissing: Slide down + fade out (200ms ease-in)

**User Interactions:**
- Click X: Dismiss immediately
- Click toast: Optional action (e.g., undo)
- Auto-dismiss: After duration expires
- Mobile: Swipe down to dismiss
- Keyboard: No keyboard action (background)

**Example toasts:**
- Success: "✓ Kết quả đã được lưu!"
- Error: "✕ Không thể tải dữ liệu"
- Info: "ℹ️ Đang đồng bộ hóa..."

**Component Naming:**
- Toast / Success
- Toast / Error
- Toast / Info
- Toast / Success-Dismissing
- Toast / Error-Dismissing
- Toast / Info-Dismissing

---

## LAYOUT COMPONENTS (5 components)

### 1. Sidebar Navigation

**Purpose:** Primary navigation for web dashboard (left side)

**Visual Properties:**
├── Width: 250px (fixed, desktop)
├── Height: Full viewport (sticky/fixed)
├── Background: white or slate-50
├── Border-right: 1px slate-200
├── Shadow: shadow-1 (subtle)
└── Position: Fixed/Sticky on scroll

**Structure:**
- Header section (Logo + app name)
- Navigation items list
- Optional: Collapse/expand button
- Optional: User profile section (footer)

**Header:**
├── Logo: 32×32px (top-left)
├── App name: "HealthLens" (18px, weight 600, slate-900)
├── Padding: lg (16px)
├── Gap: md (12px) between logo + text
└── Border-bottom: 1px slate-200 (optional)

**Navigation Item (48px height each):**

Default (inactive):
├── Icon: 24px (left)
├── Label: 16px, weight 400 (right)
├── Padding: 12px vertical, 16px horizontal
├── Text color: slate-700
├── Icon color: slate-600
├── Background: transparent
├── Gap icon-text: md (12px)
└── Cursor: pointer

Hover:
├── Background: slate-100 (#F1F5F9)
├── Text: slate-900
├── Icon: slate-700
└── Smooth transition (200ms)

Active (Current page):
├── Background: teal-50 (#F0FDFA)
├── Text: teal-600 (#0D9488) - BOLD, weight 600
├── Icon: teal-600
├── Border-left: 4px teal-600 (indicator)
└── Clear active state

Focus (Keyboard):
├── Focus ring: 2px teal-500
└── Other: Same as hover

**Navigation Items to show:**
- 🏠 Dashboard
- 👤 Hồ sơ của tôi
- 👥 Chia sẻ gia đình
- ⚙️ Cài đặt
- 🚪 Đăng xuất (bottom, red text, red-600)

**Collapsed Variant (Tablet width < 768px):**
├── Width: 60px (icon-only)
├── Icons: Centered
├── Labels: Hidden
├── Hover: Show label in tooltip
├── Toggle: Hamburger menu to expand

**Component Naming:**
- SidebarNav / Default (250px)
- SidebarNav / Collapsed (60px icon-only)
- SidebarNav / Item-Active
- SidebarNav / Item-Hover

---

### 2. Top Navigation Bar

**Purpose:** Header for web dashboard with logo, search, actions

**Visual Properties:**
├── Height: 64px (fixed)
├── Background: white
├── Border-bottom: 1px slate-200
├── Shadow: shadow-1
├── Position: Sticky/Fixed on scroll
└── Z-index: High (above content)

**3 Sections:**

Left Section:
├── Logo: 32×32px (desktop only)
├── App name: "HealthLens" (16px, weight 600, slate-900) (desktop only)
├── Back button: ← (mobile only, on certain pages)
└── Gap: md (12px)

Center Section:
├── Page title: Heading size (32px, weight 600, slate-900)
│   └── Examples: "Kết quả Sức khỏe", "Hồ sơ", "Cài đặt"
├── OR: Search bar (on dashboard)
│   ├── Input: 300px width
│   ├── Placeholder: "Tìm kiếm hồ sơ..."
│   ├── Icon: Search icon (slate-600)
│   └── Radius: md (8px)
└── Flex: 1 (takes available space)

Right Section:
├── Notification bell icon (24px)
│   ├── Icon: bell outline (slate-600)
│   ├── Badge: Red dot if unread
│   └── Click: Open notification dropdown
├── Gap: md (12px)
└── User menu (avatar dropdown)
    ├── Avatar: 40×40px circle
    ├── Click: Open dropdown menu
    └── Menu items:
        ├── "Hồ sơ của tôi"
        ├── "Cài đặt"
        ├── Divider
        └── "Đăng xuất" (red-600)

**Responsive Variants:**

Desktop (1280px+):
├── Full logo + name visible (left)
├── Search bar visible (center)
├── All icons visible (right)
├── Normal spacing
└── Height: 64px

Tablet (768px-1279px):
├── Logo visible
├── Title only (no search)
├── Notification + user menu only (right)
├── Compact spacing
└── Height: 64px

Mobile (below 768px):
├── Hamburger menu or back button (left)
├── Title or search toggle (center)
├── Notification only (right)
├── User avatar dropdown
└── Height: 56px

**Component Naming:**
- TopNav / Default
- TopNav / With-Search
- TopNav / Mobile
- TopNav / With-Notifications

---

### 3. Breadcrumb Navigation

**Purpose:** Show page hierarchy, allow quick navigation

**Visual Properties:**
├── Height: 24px
├── Padding: 8px horizontal
├── Font: 14px, weight 400
├── Gap between items: 8px
└── Display: Inline-flex

**Structure:**
- Item 1: Link (teal-600)
- Separator: "/" (slate-400)
- Item 2: Link (teal-600)
- Separator: "/"
- Item 3: Current (slate-700, bold, not link)

**Item States:**

Link (clickable):
├── Text: teal-600 (#0D9488)
├── Cursor: pointer
├── Hover: Underline
└── Click: Navigate

Current page (not clickable):
├── Text: slate-700 (#334155), weight 600 (bold)
├── Cursor: default
├── Hover: No change
└── Represents current location

Disabled (optional):
├── Text: slate-400
├── Cursor: not-allowed
└── No interaction

**Separator:**
├── Text: "/"
├── Color: slate-400
├── Font: 14px
└── No interaction

**Example breadcrumbs:**
- "Dashboard / Health Records / Results Detail"
- "Profiles / My Profile / Edit"
- "Settings / Privacy & Security"
- "Admin / Reference Data / Editor"

**Component Naming:**
- Breadcrumb / Default
- Breadcrumb / With-Icon (optional, add icon before item)

---

### 4. Tabs Component (Horizontal)

**Purpose:** Switch between related content panels

**Visual Properties:**
├── Border-bottom: 1px slate-200
├── Height: Auto (tab height + border)
├── Background: white or slate-50
├── Tab height: ~48px
└── Display: Flex (horizontal)

**Tab Item:**

Default (inactive):
├── Padding: 12px 16px
├── Font: 16px, weight 500
├── Text: slate-600 (#475569)
├── Background: transparent
├── Cursor: pointer
├── Hover: Text slate-900, background slate-50
└── Border-bottom: None

Active (selected):
├── Text: teal-600 (#0D9488), weight 600
├── Background: slate-50 (optional)
├── Border-bottom: 3px solid teal-600 (indicator)
└── Smooth transition (200ms ease-out)

**Tab Indicator:**
├── Position: Bottom of active tab
├── Height: 3px
├── Color: teal-600
├── Animates to position (200ms ease-out)
└── Width: Match tab width

**Tab Panel:**
├── Shows below tab list
├── Content of active tab only
├── Padding: 2xl (24px)
├── Fade in animation (200ms ease-out)
└── Other panels hidden

**Example Tabs to show:**
- Tab 1: "Tổng quan" (active, with indicator line)
- Tab 2: "Chi tiết"
- Tab 3: "Giải thích"
- Tab 4: "Khuyến nghị"

**Show sample content under each tab**

**Component Naming:**
- Tabs / Default
- Tabs / Tab-1-Active
- Tabs / Tab-2-Active
- Tabs / Tab-3-Active
- Tabs / Tab-4-Active

---

### 5. Pagination Component

**Purpose:** Navigate between pages of data

**Visual Properties:**
├── Height: 40px
├── Padding: 8px (buttons)
├── Gap: 4px between buttons
├── Border: 1px slate-200
├── Border-radius: lg (12px)
├── Background: white
└── Display: Flex

**Button Types:**

Previous/Next Button:
├── Label: "← Trước" / "Sau →"
├── Size: 40×40px
├── Border: 1px slate-300
├── Font: 14px
├── Icon: Chevron icon

State - Disabled (on first/last page):
├── Color: slate-400
├── Cursor: not-allowed
├── Opacity: 50%
└── No interaction

State - Active:
├── Color: teal-600
├── Hover: teal-700, border teal-500
├── Cursor: pointer
└── Clickable

**Page Number Button:**

Regular page (not current):
├── Size: 40×40px
├── Text: "2"
├── Background: white
├── Border: 1px slate-300
├── Hover: border teal-500, shadow-1
├── Click: Go to page
└── Cursor: pointer

Current page (active):
├── Background: teal-600
├── Text: white
├── Border: 1px teal-600
├── Font-weight: 600
└── Not clickable, cursor: default

Ellipsis (...):
├── Shows skipped pages
├── Non-clickable
├── Text: "...", color slate-600
└── Font: 14px

**Example pagination layout:**
← [1] [2] (3) [4] [5] ... [20] →
Where (3) is current page, highlighted

**Component Naming:**
- Pagination / Default
- Pagination / First-Page (Previous disabled)
- Pagination / Last-Page (Next disabled)
- Pagination / Middle-Page
- Pagination / With-Ellipsis

---

## DATA DISPLAY COMPONENTS (5 components)

### 1. Table Component

**Purpose:** Display tabular data (health records, lists, etc.)

**Visual Properties:**
├── Background: white
├── Border: 1px slate-200
├── Border-radius: lg (12px) (outer corners only)
├── Shadow: shadow-1
├── Overflow: Auto scroll (horizontal on mobile)
└── Font: Body 16px by default

**Structure:**
- Table header row (th)
- Table body rows (td)
- Optional: Striped background
- Optional: Hover highlighting
- Optional: Sorting indicators

**Header Row:**
├── Background: slate-100 (#F1F5F9)
├── Border-bottom: 2px slate-300
├── Padding: 12px 16px
├── Font: 14px, weight 600
├── Text: slate-900
├── Icons: Lucide icons for sort arrows
└── Height: 48px

**Body Row:**

Default:
├── Background: white
├── Border-bottom: 1px slate-200
├── Padding: 12px 16px
├── Font: 16px, weight 400
├── Text: slate-700
├── Min-height: 44px (accessibility)
└── Wrapping: Text can wrap

Hover:
├── Background: slate-50
├── Shadow: shadow-1 (subtle)
├── Cursor: pointer (if clickable)
└── Indicates hover state

**Striped variant (alternating rows):**
├── Odd rows: white
├── Even rows: slate-50
└── Better readability for long lists

**Column Types:**

Text column:
├── Left-aligned
├── Normal text
└── Can wrap

Number column:
├── Right-aligned
├── Monospace font (optional)
└── Fixed width

Status column:
├── Badge or status indicator
├── Centered
├── Color-coded (green/yellow/red)

Action column:
├── Icons: Edit, Delete, View, etc.
├── Right-aligned
├── Hover: Show tooltip on each icon
├── Gap between icons: sm (8px)
└── Cursor: pointer

**Example Table to show:**
Columns: Ngày xét nghiệm | Loại | Trạng thái | Giá trị | Hành động

Row 1: 15/03/2026 | Xét nghiệm máu | ✓ Normal | 120 | [View] [Delete]
Row 2: 10/03/2026 | Xét nghiệm nước tiểu | ⚠ Monitor | 95 | [View] [Delete]
Row 3: 05/03/2026 | Xét nghiệm chức năng gan | ✓ Normal | 40 | [View] [Delete]

**Component Naming:**
- Table / Default
- Table / With-Striping
- Table / Row-Hover
- Table / Row-Selected
- Table / Empty (with empty state message)

---

### 2. Avatar Component

**Purpose:** User profile images with fallback to initials

**Visual Properties:**
├── Border-radius: full (100% circle)
├── Border: 2px slate-200
├── Shadow: shadow-1
└── No background (image shows)

**Size Variants (4 sizes):**

Small (32×32px):
├── Image: 32×32px
├── Initials: 12px font
├── Border-radius: full
└── Use: Inline, lists, thumbnails

Medium (40×40px):
├── Image: 40×40px
├── Initials: 14px font
├── Border-radius: full
└── Use: Default, most places

Large (48×48px):
├── Image: 48×48px
├── Initials: 16px font
├── Border-radius: full
└── Use: Profile cards, hero sections

XLarge (64×64px):
├── Image: 64×64px
├── Initials: 24px font
├── Border-radius: full
└── Use: Large profile displays

**Avatar Types:**

Type 1: Image Avatar
├── Real image
├── Border: 2px slate-200
├── Shadow: shadow-1
├── Alt text: Full name
└── Fallback: Initials if image fails

Type 2: Initials Fallback
├── Background: teal-600 (#0D9488)
├── Text: white, bold, centered
├── 2 letters (first + last name initial)
├── Example: "TA" for "Trần Văn A"
├── Font: 14px (for 40px avatar)
└── Border-radius: full

Type 3: Icon Avatar
├── Icon inside circle
├── Background: slate-200
├── Icon: slate-600, 20px size
├── Use: Unknown/generic users
└── No border

Type 4: Avatar Group (Multiple)
├── Stack 3-4 avatars
├── Overlap: -8px (negative margin to overlap)
├── Show first 3-4
├── Badge: "+2" for remaining (or "+N")
├── Use: Showing multiple users/family members

**Optional: Online/Status Badge**
├── Small green dot (bottom-right)
├── Size: 8×8px
├── Background: green-500
├── Border: 2px white
└── Indicates: Online status

**Component Naming:**
- Avatar / Image-Small
- Avatar / Image-Medium
- Avatar / Image-Large
- Avatar / Image-XLarge
- Avatar / Initials-Medium (teal background)
- Avatar / Icon-Medium (gray background)
- Avatar / Group (3+ avatars stacked)
- Avatar / With-Badge (online indicator)

---

### 3. Skeleton / Loading Component

**Purpose:** Placeholder while content loads

**Visual Properties:**
├── Background: slate-200 (#E2E8F0)
├── Animation: Shimmer (gradient left-to-right)
├── Duration: 1.5s infinite
├── Easing: Linear
└── Border-radius: Match final content

**Skeleton Types:**

Type 1: Line Skeleton
├── Height: 16px (for body text)
├── Width: 100% or partial (e.g., 80%)
├── Border-radius: sm (4px)
└── Use: Text loading

Type 2: Circle Skeleton
├── Size: 40×40px (or custom, e.g., 32×32px)
├── Border-radius: full (100% circle)
└── Use: Avatar loading

Type 3: Card Skeleton
├── Contains multiple elements:
│   ├── Circle (40×40px, top-left)
│   ├── Title line (90% width, 16px height)
│   ├── Description lines (3 lines, 100% width, 12px each)
│   └── Footer line (50% width, 14px height)
├── Padding: 16px
├── Spacing: Matches real card layout
└── Use: Card content loading

Type 4: Table Row Skeleton
├── Multiple columns (match table structure)
├── Each column: Line skeleton
├── Height: 44px (table row height)
├── Gap: Between columns
└── Use: Table data loading

**Shimmer Animation:**
```css
@keyframes shimmer {
  0%   { background-position: -1000px 0; }
  100% { background-position: 1000px 0; }
}

Background: linear-gradient(
  90deg,
  slate-200 0%,
  slate-100 50%,
  slate-200 100%
);
Background-size: 1000px 100%;
Animation: shimmer 1.5s infinite linear;

**Accessibility:**
├── aria-busy: true (indicates loading)
├── aria-label: "Loading..."
└── Hidden from screen readers (content not yet available)

**Example Skeleton Layouts to show:**
- Text loading (3 lines)
- Avatar + text loading
- Card skeleton
- Table row skeleton

**Component Naming:**
- Skeleton / Line
- Skeleton / Circle
- Skeleton / Card
- Skeleton / Table-Row
- Skeleton / Custom (container)

---

### 4. Empty State Component

**Purpose:** Friendly message when no data available

**Visual Properties:**
├── Container:
│   ├── Padding: 3xl (32px)
│   ├── Text-align: center
│   ├── Min-height: 300px
│   └── Background: Optional light background
├── Illustration: Icon or image (120×120px)
├── Font colors: slate-900 (title), slate-600 (description)
└── Border: Optional container border

**Structure:**
- Illustration/Icon (top)
- Title (heading)
- Description (body text)
- CTA Button (primary)
- Optional: Secondary link

**Visual Elements:**

Illustration (120×120px):
├── Icon: Lucide icon or simple SVG
├── Color: teal-300 or slate-300 (light, muted)
├── Examples:
│   ├── Empty folder (no records)
│   ├── Search icon with magnifier (no results)
│   ├── Plus icon (add first item)
│   └── Chart/graph (no data)
└── Top margin: 2xl (24px)

Title:
├── Font: Heading-3 (24px), weight 600
├── Color: slate-900
├── Text: "Chưa có kết quả nào"
├── Top margin: 2xl (24px)
└── Example: "Không tìm thấy kết quả"

Description:
├── Font: Body (16px), weight 400
├── Color: slate-600
├── Text: Friendly explanation
├── Top margin: lg (16px)
└── Example: "Hãy tải kết quả đầu tiên..."

CTA Button:
├── Primary button (teal-600)
├── Text: "Tạo mới", "Tải lên", "Bắt đầu", etc.
├── Top margin: xl (20px)
└── Action: Navigate to creation/upload flow

Optional Secondary Link:
├── Text link (teal-600)
├── Text: "Tìm hiểu thêm", "Cần giúp?", etc.
├── Top margin: md (12px)
└── Action: Navigate to docs/help

**4 Empty State Variants:**

Variant 1: No Records
├── Icon: Folder outline
├── Title: "Chưa có kết quả nào"
├── Description: "Hãy tải kết quả đầu tiên để xem chi tiết"
├── CTA: "Tải kết quả"
└── Use: Health records list when empty

Variant 2: No Search Results
├── Icon: Search icon
├── Title: "Không tìm thấy kết quả"
├── Description: "Thử từ khóa hoặc bộ lọc khác"
├── CTA: "Xóa bộ lọc"
└── Use: Search/filter results

Variant 3: First Use (Onboarding)
├── Icon: Plus or welcome icon
├── Title: "Bắt đầu với HealthLens"
├── Description: "Tải kết quả xét nghiệm..."
├── CTA: "Tải kết quả đầu tiên"
└── Use: New user onboarding

Variant 4: Error State
├── Icon: Warning or alert icon (red-600)
├── Title: "Có lỗi xảy ra"
├── Description: "Không thể tải dữ liệu..."
├── CTA: "Thử lại"
└── Use: API error fallback

**Component Naming:**
- EmptyState / No-Records
- EmptyState / No-Results
- EmptyState / First-Use
- EmptyState / Error

---

### 5. Error State Component

**Purpose:** Display API errors, failures, and exceptions

**Visual Properties:**
├── Container:
│   ├── Background: red-50 (#FEF2F2)
│   ├── Border: 2px solid red-600 (#DC2626)
│   ├── Border-radius: lg (12px)
│   ├── Padding: 2xl (24px)
│   ├── Margin: 2xl (24px)
│   └── Shadow: shadow-2
├── Icon: Error/X icon (48px, red-600)
├── Text colors: red-900 (title), red-700 (description)
└── Display: Flex (icon left, content right)

**Structure:**
- Error icon (left)
- Title (heading)
- Description (body)
- Optional: Technical details
- Action buttons (retry, go back)

**Error Icon:**
├── Icon: X (✕) or alert (!)
├── Size: 48×48px
├── Color: red-600
├── Position: Top-left of content
└── Optional: Rounded background (red-200)

**Error Title:**
├── Font: Heading-4 (18px), weight 600
├── Color: red-900 (#7F1D1D)
├── Text: "Không thể tải dữ liệu"
└── Margin: Top lg (16px) from icon

**Error Description:**
├── Font: Body (16px), weight 400
├── Color: red-700 (#B91C1C)
├── Text: Helpful explanation
├── Example: "Vui lòng kiểm tra kết nối internet của bạn..."
└── Margin: Top md (12px)

**Technical Details (Optional):**
├── Font: Monospace 12px
├── Color: red-600
├── Visible in dev mode only (optional)
├── Shows error code/message
└── Margin: Top md (12px)

**Action Buttons:**

Primary button:
├── Text: "Thử lại"
├── Color: red-600 (danger)
├── Action: Retry failed operation

Secondary button:
├── Text: "Quay lại" or "Trang chủ"
├── Color: slate-600 (secondary)
├── Action: Navigate away

Tertiary link:
├── Text: "Liên hệ hỗ trợ"
├── Color: teal-600 (link)
├── Action: Open help/support

Margin: Top xl (20px)

**3 Error State Variants:**

Variant 1: Form Error
├── Inline with form
├── Smaller size
├── Red border on form field
├── Error text below field
└── Use: Form validation

Variant 2: Page Error
├── Full page display
├── Large error container
├── Help center link
├── Return home button
└── Use: API failures, 500 errors

Variant 3: Network Error
├── Icon: Offline/connection icon
├── Title: "Mất kết nối"
├── Message: "Kiểm tra kết nối internet"
├── Retry button
└── Use: No internet connection

**Component Naming:**
- ErrorState / Default
- ErrorState / Form
- ErrorState / Page
- ErrorState / Network
- ErrorState / 404
- ErrorState / 500

---

## COMPONENT SUMMARY

**Composite Components (7):**
1. Card
2. Alert (4 types)
3. Badge (4 semantic types + removable)
4. Chip
5. Modal (3 types)
6. Tooltip
7. Toast (3 types)

**Layout Components (5):**
1. Sidebar Navigation
2. Top Navigation Bar
3. Breadcrumb
4. Tabs
5. Pagination

**Data Display Components (5):**
1. Table
2. Avatar
3. Skeleton/Loading
4. Empty State
5. Error State

**Total: 17 new components** (24+ when counting all variants)

## OUTPUT EXPECTATIONS

1. **Figma file** with all new components
2. **Page organization:**
   - Page 7: COMPONENTS - Composite (Card, Alert, Badge, Chip, Modal, Tooltip, Toast)
   - Page 8: COMPONENTS - Layout (Navigation, Tabs, Breadcrumb, Pagination)
   - Page 9: COMPONENTS - Data Display (Table, Avatar, Skeleton, EmptyState, ErrorState)
3. **Each component** with all states visible
4. **Proper naming** following patterns
5. **Reuse Phase 1 styles** (not hard-code colors/typography)
6. **Accessibility** - Focus rings on interactive elements
7. **Documentation** - Notes on usage

## DESIGN QUALITY CHECKLIST

- [ ] All colors from Phase 1 styles (not hard-coded)
- [ ] Typography uses Phase 1 text styles
- [ ] Spacing aligns to 8px grid (4, 8, 12, 16, 20, 24, 32, 40px)
- [ ] Border radius matches spec (sm/md/lg/xl/full)
- [ ] Shadows use Phase 1 shadow tokens
- [ ] Focus rings on all interactive elements (2-3px teal-500)
- [ ] States clearly differentiated
- [ ] Responsive considerations (desktop/mobile variants shown)
- [ ] Component naming consistent
- [ ] All states documented

---END PROMPT---
```

---

## 📍 PHẦN 2: BỨC TIẾP THEO

### Bước 1: Copy Prompt
- Copy nguyên bản prompt từ trên (từ START đến END)
- Lưu vào file text hoặc clipboard

### Bước 2: Vào Stitch AI
1. Login: https://www.stitchdesign.ai/
2. Click "Create New Project" hoặc "New"
3. Chọn "Generate with AI"
4. Paste prompt vào text box

### Bước 3: Cấu hình Stitch AI
```
Settings:
├── Framework: Figma component
├── Design System: HealthLens
├── Include Phase 1 styles: Yes
├── Generate variants: Yes
├── Generate all states: Yes
├── Color mode: Light
└── Spacing grid: 8px
```

### Bước 4: Generate
- Click "Generate"
- Chờ Stitch AI xử lý (5-20 phút)

### Bước 5: Import vào Figma
- Stitch AI → "Export to Figma" hoặc "Open in Figma"
- File tự động import vào Figma file của bạn
- Sắp xếp pages theo thứ tự

### Bước 6: QA & Verify
**Kiểm tra:**
- [ ] All 17 components có?
- [ ] States complete? (Default, Hover, Focus, Active, Disabled)
- [ ] Colors use Phase 1 styles?
- [ ] Typography correct?
- [ ] Spacing 8px aligned?
- [ ] Focus rings visible?
- [ ] Component naming consistent?

### Bước 7: Done! ✅
- Lưu Figma file
- Share link với team

---

## 📊 TIMELINE

```
Phase 2 Total Time: ~3-4 hours

0:00-0:15 | Prepare + Copy prompt
0:15-1:15 | Stitch AI generate (wait)
1:15-2:15 | Import + Organize in Figma
2:15-3:00 | QA + Verify all components
3:00-3:30 | Fix issues + Documentation
3:30-4:00 | Final export + Team sharing

Total: 3-4 giờ
```

---

**Document Version:** 1.0  
**Language:** Tiếng Việt  
**Created:** March 2026  
**Status:** Ready for Phase 2 Execution
