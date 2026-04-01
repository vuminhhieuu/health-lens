# Phase 2: Components - Hướng dẫn Triển khai Chi tiết

**Thời gian:** Tuần 2 (5-7 ngày làm việc)  
**Mục tiêu:** Tạo xong tất cả Composite + Layout + Data Display components  
**Đầu ra:** Component library đầy đủ, sẵn sàng cho Screen design

---

## 📋 Phase 2 Overview

**Mục tiêu:**
- Xây dựng các component phức tạp hơn (Composite components)
- Tạo component layout cho navigation
- Tạo component data display
- Chuẩn bị pattern library

**Tasks chính:**
1. ✅ Composite Components (Card, Alert, Badge, Modal, etc.)
2. ✅ Layout Components (Navigation, Tabs, Pagination, Breadcrumb)
3. ✅ Data Display (Table, Avatar, Skeleton, Empty State)
4. ✅ Patterns & States (Loading, Error, Success)

**Deliverables:**
- Figma page: "7. COMPONENTS - Composite"
- Figma page: "8. COMPONENTS - Layout"
- Figma page: "9. COMPONENTS - Data Display"
- Figma page: "11. PATTERNS & States"
- Documentation cho mỗi component

---

## 🧩 TASK 1: Composite Components (3-4 giờ)

**Open Figma page:** "7. COMPONENTS - Composite"

### 1.1 Card Component

**Tên component:** `Card / Default`

```
Visual Properties:
├── Background: white (#FFFFFF)
├── Border: 1px solid slate-200 (#E2E8F0)
├── Padding: lg (16px)
├── Border radius: lg (12px)
├── Shadow: shadow-2

Structure:
├── Card container (frame)
├── Optional header
├── Content area
├── Optional footer

States to create (4 total):

1. Resting (Default)
   ├── Shadow: shadow-2
   └── Ready to interact

2. Hover
   ├── Shadow: shadow-3 (elevated)
   ├── Background: Subtle tint (slate-50)
   └── Cursor: pointer

3. Focus
   ├── Border: 2px solid teal-500
   ├── Shadow: shadow-3
   └── Focus ring: 2-3px teal-500 (optional)

4. Interactive/Selected
   ├── Background: teal-50
   ├── Border: 2px teal-500
   ├── Shadow: shadow-3
   └── Indicates selection

Component variants in Figma:
├── Card / Resting
├── Card / Hover
├── Card / Focus
└── Card / Selected
```

**Usage examples trong Figma:**
```
Show real-world examples:
- Health result card (with HealthResultSummary inside)
- Profile card (with name, avatar)
- Info card (with text + icon)
- Each with different content but same card structure
```

---

### 1.2 Alert Component

**Tên component:** `Alert / Info`, `Alert / Warning`, `Alert / Error`, `Alert / Success`

```
General Alert structure:
├── Icon (left side)
├── Content area (title + description)
├── Close button (optional, right side)
└── Color-coded by type

Variants (4 types):

TYPE 1: INFO (Blue)
├── Icon: Info circle (ⓘ) - blue-600
├── Background: blue-50 (#EFF6FF)
├── Border-left: 4px solid blue-600 (#2563EB)
├── Title: "Thông tin" or "Lưu ý"
├── Description: Informational message
└── Close: Optional

TYPE 2: WARNING (Yellow/Amber)
├── Icon: Alert triangle (⚠) - yellow-600
├── Background: yellow-50 (#FFFBEB)
├── Border-left: 4px solid yellow-600 (#D97706)
├── Title: "Cảnh báo" or "Chú ý"
├── Description: Warning message
└── Close: Optional

TYPE 3: ERROR (Red)
├── Icon: X or alert (✕/!) - red-600
├── Background: red-50 (#FEF2F2)
├── Border-left: 4px solid red-600 (#DC2626)
├── Title: "Lỗi" or "Có vấn đề"
├── Description: Error explanation
└── Close: Yes (dismissible)

TYPE 4: SUCCESS (Green)
├── Icon: Check mark (✓) - green-600
├── Background: green-50 (#F0FDF4)
├── Border-left: 4px solid green-600 (#059669)
├── Title: "Thành công"
├── Description: Success message
└── Animation: Fade in (200ms ease-out)

Each type needs 2-3 states:
├── Default (with message)
├── Hover (if dismissible)
└── Closed/Dismissed
```

**Ví dụ trong Figma:**
```
- Info: "Cập nhật sẽ có hiệu lực trong 24 giờ"
- Warning: "Giá trị này cao hơn bình thường, vui lòng chú ý"
- Error: "Không thể tải dữ liệu, vui lòng thử lại"
- Success: "Kết quả đã được lưu thành công!"
```

---

### 1.3 Badge Component

**Tên component:** `Badge / Neutral`, `Badge / Success`, `Badge / Warning`, `Badge / Error`

```
Badge structure:
├── Background (color-coded)
├── Text label
├── Optional icon (left)
├── Optional remove button (right, for removable variant)
└── Compact size

Size and spacing:
├── Padding: 4px vertical, 8px horizontal (xs)
├── Border radius: sm (4px) or md (8px)
├── Font: 12px, weight 500
├── Height: ~24px

Variants to create:

Base variants (4 types):

1. Neutral (Slate)
   ├── Background: slate-200
   ├── Text: slate-900
   ├── Use: Generic labels, tags

2. Success (Green) ⭐ MAIN
   ├── Background: green-100 or green-50
   ├── Text: green-900 or green-700
   ├── Icon: ✓ (optional)
   ├── Use: "Bình thường", status OK

3. Warning (Yellow) ⭐ MAIN
   ├── Background: yellow-100 or yellow-50
   ├── Text: yellow-900 or yellow-700
   ├── Icon: ⚠ (optional)
   ├── Use: "Cần chú ý", caution status

4. Error (Red) ⭐ MAIN
   ├── Background: red-100 or red-50
   ├── Text: red-900 or red-700
   ├── Icon: ✕ or ! (optional)
   ├── Use: "Cần hành động", error status

Additional state variants:

5. Removable Badge
   ├── X button on right
   ├── Hover: Add close indicator
   ├── Click: Remove badge
   └── Animation: Fade out

6. Outlined Badge (optional)
   ├── Border: 2px color
   ├── Background: transparent
   ├── Text: colored
   └── Use: Lighter appearance

Examples for Figma:
- Badge "Bình thường" (green with checkmark)
- Badge "Cần chú ý" (yellow with warning icon)
- Badge "Cần hành động" (red with X icon)
- Badge "Male" removable (neutral, with X)
```

---

### 1.4 Chip Component

**Tên component:** `Chip / Selectable`

```
Chip structure (like badge but interactive):
├── Avatar (optional, left)
├── Label text
├── Remove button (X, right)
└── Clickable/selectable

Usage:
├── Removable tags in form inputs
├── Selectable filter chips
├── Multi-select items

States:

1. Unselected
   ├── Background: slate-200
   ├── Text: slate-900
   └── Border: 1px slate-300

2. Selected
   ├── Background: teal-600 or teal-100
   ├── Text: white or teal-900
   ├── Border: 2px teal-600
   └── Indicates selection

3. Hover
   ├── Background: shade darker
   └── Cursor: pointer

4. Removable (on hover)
   ├── Show X button (right side)
   ├── Click X to remove
   └── Animation: Fade out when removed

Example in Figma:
- Chip: "Tiểu đường" (selected, teal)
- Chip: "Huyết áp cao" (unselected, gray)
- Chip with remove: "Dị ứng" × (teal with X)
```

---

### 1.5 Modal/Dialog Component

**Tên component:** `Modal / Confirmation`, `Modal / Form`, `Modal / Info`

```
Modal structure:
├── Overlay/Backdrop (semi-transparent dark)
├── Modal box (center, with shadow)
│   ├── Header (with close button)
│   ├── Content (scrollable if long)
│   ├── Footer (with action buttons)
│   └── Focus trap (keyboard navigation)
└── Animation: Slide up + fade in (200ms ease-out)

Specifications:

Desktop modal:
├── Width: 480px (standard)
├── Max-height: 90vh
├── Border radius: lg (12px)
├── Shadow: shadow-4
├── Padding: 24px (2xl)

Mobile modal:
├── Width: 95% (full minus margins)
├── Max-height: 90vh
├── Bottom sheet style or full screen
└── Padding: 16px (lg)

Header section:
├── Title: "Xác nhận hành động"
├── Close button (X icon, top right)
├── Font: heading-3 (24px)
└── Color: slate-900

Content section:
├── Message or form
├── Padding: 2xl (24px) top/bottom
├── Font: body (16px)
└── Scrollable if > 50% viewport

Footer section:
├── 2 buttons typically:
│   ├── Primary: "Xác nhận" (teal)
│   └── Secondary: "Hủy" (gray)
│
└── Full width buttons on mobile, side-by-side on desktop

3 variants to create:

TYPE 1: Confirmation Dialog
├── Title: "Xác nhận"
├── Content: "Bạn có chắc muốn xóa?"
├── Buttons: "Xóa" (danger/red), "Hủy"
└── Emphasis: Clear warning

TYPE 2: Form Modal
├── Title: "Tạo hồ sơ mới"
├── Content: Form fields
├── Buttons: "Lưu", "Hủy"
└── Scrollable content

TYPE 3: Info Modal
├── Title: "Thông báo"
├── Content: Information message
├── Buttons: "Đóng" or "OK"
└── Non-destructive

States for each:

1. Closed
   └── Not visible (reference only)

2. Open (Default)
   ├── Visible with animation
   ├── Focus on first input (form) or primary button
   └── Backdrop interactive (click closes - optional)

3. Loading
   ├── Primary button shows spinner
   ├── Content may be disabled
   └── Close button disabled

4. Error state
   ├── Red alert box in modal
   ├── Error message displayed
   └── User can retry
```

---

### 1.6 Tooltip Component

**Tên component:** `Tooltip / Default`

```
Tooltip structure:
├── Trigger element (icon, text, button)
├── Tooltip box (appears on hover/focus)
├── Arrow pointing to trigger
└── Text content

Specifications:

Box:
├── Background: slate-900 (#0F172A)
├── Text: white
├── Padding: 8px 12px (xs-md)
├── Border radius: md (8px)
├── Shadow: shadow-3
├── Font: 12px
├── Max-width: 250px

Arrow:
├── Small triangle
├── Color: slate-900 (match background)
├── Size: 6px × 6px
└── Positioned: Top, bottom, left, or right

Position variants:

1. Top
   ├── Arrow points down
   └── Tooltip above trigger

2. Bottom
   ├── Arrow points up
   └── Tooltip below trigger (DEFAULT)

3. Left
   ├── Arrow points right
   └── Tooltip left of trigger

4. Right
   ├── Arrow points left
   └── Tooltip right of trigger

Behavior:

Trigger:
├── Hover: Show tooltip
├── Focus: Show tooltip
├── Click outside: Hide tooltip
├── Keyboard Escape: Hide tooltip

Animation:
├── Show: Fade in + slide (150ms ease-out)
├── Hide: Fade out (150ms ease-in)
└── No animation on mobile (auto-show on tap)

Example in Figma:
- Tooltip on info icon: "Giá trị này so sánh với bình thường"
- Tooltip on settings icon: "Cài đặt ứng dụng"
- Tooltip on help button: "Trợ giúp và hỗ trợ"
```

---

### 1.7 Toast/Notification Component

**Tên component:** `Toast / Success`, `Toast / Error`, `Toast / Info`

```
Toast structure:
├── Icon (left)
├── Message
├── Close button (optional, right)
└── Auto-dismiss timer (optional)

Specifications:

Container:
├── Width: 100% (mobile) or 380px (desktop)
├── Padding: 12px 16px (md lg)
├── Border radius: lg (12px)
├── Shadow: shadow-3
├── Position: Bottom-center or top-right

Types (3 main):

SUCCESS Toast
├── Icon: ✓ (green)
├── Background: green-600 or green-500
├── Text: white
├── Message: "Kết quả đã được lưu!"
├── Duration: 3-5 seconds (auto-dismiss)
└── Color: green

ERROR Toast
├── Icon: ✕ (white/red)
├── Background: red-600
├── Text: white
├── Message: "Có lỗi xảy ra, vui lòng thử lại"
├── Duration: 5-8 seconds (user may need to read)
└── Color: red

INFO Toast
├── Icon: i (circle)
├── Background: blue-600
├── Text: white
├── Message: "Dữ liệu đang được cập nhật"
├── Duration: 3 seconds
└── Color: blue

Behavior:

Animation:
├── Enter: Slide up + fade in (200ms ease-out)
├── Exit: Slide down + fade out (200ms ease-in)
└── Stacking: Multiple toasts stack vertically

User interaction:
├── Click close (X): Dismiss immediately
├── Click toast: Can navigate (optional)
├── Auto-dismiss: After specified duration
└── Mobile: Swipe down to dismiss

Example toasts for Figma:
- Success: "✓ Kết quả đã được lưu!"
- Error: "✕ Không thể tải dữ liệu"
- Info: "ℹ️ Đang đồng bộ hóa..."
```

---

## 🏗️ TASK 2: Layout Components (2-3 giờ)

**Open Figma page:** "8. COMPONENTS - Layout"

### 2.1 Sidebar Navigation

**Tên component:** `SidebarNav / Desktop`, `SidebarNav / Collapsed`

```
Sidebar structure:
├── Header: Logo + app name
├── Navigation items (list)
├── Optional: Collapse/expand button
└── Optional: User profile section (footer)

Specifications (Desktop):

Layout:
├── Width: 250px (fixed)
├── Height: Full viewport (sticky)
├── Background: white or slate-50
├── Border-right: 1px slate-200
└── Shadow: shadow-1 (optional)

Header section:
├── Logo: 32×32px
├── App name: "HealthLens" (Inter 18px, weight 600)
├── Padding: lg (16px)
└── Gap: md (12px) between logo + text

Navigation items:
├── Each item: 48px height
├── Padding: 12px 16px (vertical, horizontal)
├── Font: 16px, weight 400
├── Icon (left): 24px
├── Text label (right)
├── Gap between icon + text: md (12px)

States per item:

1. Default
   ├── Text: slate-700
   ├── Icon: slate-600
   ├── Background: transparent
   └── Cursor: pointer

2. Hover
   ├── Background: slate-100
   ├── Text: slate-900
   └── Icon: slate-700

3. Active (Current page)
   ├── Background: teal-50
   ├── Text: teal-600
   ├── Icon: teal-600
   ├── Left border: 4px teal-600 (optional)
   └── Bold font (optional, weight 600)

4. Focus
   ├── Focus ring: 2px teal-500
   └── Other properties same as hover

Menu items to show:

- 🏠 Dashboard
- 👤 Hồ sơ của tôi
- 👥 Chia sẻ gia đình
- ⚙️ Cài đặt
- 🚪 Đăng xuất (bottom, red text)

Collapsed variant (when width < 768px):

├── Width: 60px (icon-only)
├── Icons centered
├── Labels hidden
├── Hover: Show label in tooltip
└── Toggle: Hamburger menu to expand

Responsive behavior:
├── Desktop (1280px+): Full width sidebar (250px)
├── Tablet (768px-1279px): Collapsed sidebar (60px)
└── Mobile: Hidden by default, drawer menu
```

---

### 2.2 Top Navigation Bar

**Tên component:** `TopNav / Default`

```
Top navigation structure:
├── Left: Logo or Back button
├── Center: Title or search
├── Right: Icons + user menu
└── Height: 64px

Specifications:

Container:
├── Height: 64px
├── Background: white
├── Border-bottom: 1px slate-200
├── Shadow: shadow-1
├── Position: Sticky/Fixed (on desktop)
└── Z-index: High (to be above content)

Left section (Desktop + Mobile):
├── Logo: 32×32px (desktop only)
├── App name: "HealthLens" (desktop only, 16px)
└── Back button (mobile only, on certain pages)

Center section:
├── Page title: "Kết quả Sức khỏe" (32px, weight 600)
├── Or search bar (on dashboard)
├── Search input: 300px width
└── Placeholder: "Tìm kiếm hồ sơ..."

Right section:
├── Notification bell icon (24px)
│   ├── Icon: bell outline (slate-600)
│   ├── Badge: Red dot if unread
│   └── Click: Open notification dropdown
│
├── Gap: md (12px)
│
└── User menu (dropdown)
    ├── Avatar: 40×40px circle
    ├── Name: "Trần Văn A"
    ├── Click: Open menu
    └── Menu items:
        ├── "Hồ sơ của tôi"
        ├── "Cài đặt"
        ├── Divider
        └── "Đăng xuất" (red)

Responsive variations:

Desktop:
├── Logo visible (left)
├── Search bar visible (center)
├── All icons visible (right)
└── Normal spacing

Tablet:
├── Logo visible
├── Title only (no search unless on search page)
├── Notification + user menu (right)
└── Compact spacing

Mobile:
├── Back button or menu toggle (left)
├── Title or search toggle (center)
├── Notification only (right)
└── Avatar dropdown menu
```

---

### 2.3 Breadcrumb Component

**Tên component:** `Breadcrumb / Default`

```
Breadcrumb structure:
├── Item 1: Link
├── Separator: /
├── Item 2: Link
├── Separator: /
└── Item 3: Current (not link, bold)

Specifications:

Layout:
├── Height: 24px (fits in header)
├── Padding: 8px horizontal
├── Gap between items: 8px
└── Font: 14px

Item states:

1. Link (clickable)
   ├── Text: teal-600
   ├── Hover: Underline
   ├── Cursor: pointer
   └── Click: Navigate

2. Current (not clickable)
   ├── Text: slate-700
   ├── Font-weight: 600 (bold)
   ├── Cursor: default
   └── No link styling

3. Disabled
   ├── Text: slate-400
   ├── Cursor: not-allowed
   └── No interaction

Separator:
├── Text: "/" (slash)
├── Color: slate-400
├── Font-size: 14px
└── Not clickable

Example in Figma:
├── Dashboard / Health Records / Results Detail
├── Profiles / My Profile / Edit
└── Settings / Privacy & Security
```

---

### 2.4 Tabs Component

**Tên component:** `Tabs / Horizontal`, `Tabs / Vertical` (optional)

```
Tabs structure:
├── Tab list (horizontal)
├── Each tab (clickable)
├── Tab indicator (active state)
└── Tab panels (content below)

Specifications (Horizontal):

Container:
├── Border-bottom: 1px slate-200
├── Height: Auto (tab height + border)
└── Background: white or slate-50

Tab item:

Default (inactive):
├── Padding: 12px 16px (md lg)
├── Font: 16px, weight 500
├── Text: slate-600
├── Background: transparent
├── Cursor: pointer
├── Hover: Text slate-900, background slate-50

Active (selected):
├── Text: teal-600
├── Font-weight: 600
├── Border-bottom: 3px solid teal-600
├── Indicator color: teal-600
└── Background: slate-50 (optional)

Tab indicator:
├── Animated line/bar (bottom)
├── Height: 3px
├── Color: teal-600
├── Animates to position of active tab (200ms ease-out)
└── Width: Same as tab width

Tab panel:
├── Shows content of active tab
├── Padding: 2xl (24px)
├── Fade in animation (200ms ease-out)
└── Only active panel visible

Example tabs in Figma:
├── Tab 1: "Tổng quan" (active)
├── Tab 2: "Chi tiết"
├── Tab 3: "Giải thích"
└── Tab 4: "Khuyến nghị"

Content changes based on active tab
```

---

### 2.5 Pagination Component

**Tên component:** `Pagination / Default`

```
Pagination structure:
├── Previous button
├── Page numbers
├── Next button
└── Optional: Jump to page

Specifications:

Layout:
├── Height: 40px
├── Padding: 8px
├── Gap between buttons: 4px
├── Border: 1px slate-200
├── Border-radius: lg (12px)
└── Background: white

Buttons:

Previous/Next button:
├── Label: "← Trước" / "Sau →"
├── Size: 40×40px
├── State (disabled):
│   ├── Color: slate-400
│   ├── Cursor: not-allowed
│   ├── Opacity: 50%
│   └── On first/last page
├── State (active):
│   ├── Color: teal-600
│   ├── Hover: teal-700
│   └── Clickable

Page number buttons:

1. Regular page (not current)
   ├── Size: 40×40px
   ├── Text: "2"
   ├── Background: white
   ├── Border: 1px slate-300
   ├── Hover: border teal-500
   └── Click: Go to page

2. Current page (active)
   ├── Background: teal-600
   ├── Text: white
   ├── Border: 1px teal-600
   ├── Font-weight: 600
   └── Not clickable

3. Ellipsis (...)
   ├── Non-clickable
   ├── Shows skipped pages
   └── Text: slate-600

Example in Figma:
├── Show: "← 1 2 (3) 4 5 ... 20 →"
├── Where 3 is current page (highlighted)
└── Previous button disabled on page 1, Next on last
```

---

## 📊 TASK 3: Data Display Components (2 giờ)

**Open Figma page:** "9. COMPONENTS - Data Display"

### 3.1 Table Component

**Tên component:** `Table / Default`

```
Table structure:
├── Header row (th)
├── Body rows (td)
├── Optional: Striped background
├── Optional: Hover highlighting
└── Optional: Sorting indicators

Specifications:

Table container:
├── Background: white
├── Border: 1px slate-200
├── Border-radius: lg (12px) (outer corners)
├── Shadow: shadow-1
└── Overflow: scroll (mobile)

Header row:
├── Background: slate-100 or slate-50
├── Border-bottom: 2px slate-300
├── Padding: 12px 16px (md lg)
├── Font: 14px, weight 600
├── Text: slate-900
└── Icons (for sort arrows)

Body rows:

Default:
├── Background: white
├── Border-bottom: 1px slate-200
├── Padding: 12px 16px
├── Font: 16px, weight 400
├── Text: slate-700
└── Min-height: 44px

Hover:
├── Background: slate-50
├── Highlight: light teal tint
└── Cursor: pointer (if clickable)

Striped (alternating):
├── Odd rows: white
├── Even rows: slate-50
└── Better readability

Column variants:

Text column:
├── Normal text
├── Left-aligned
└── Wrapping text

Number column:
├── Right-aligned
├── Monospace font (optional)
└── Fixed width

Status column:
├── Badge or status indicator
├── Centered
├── Color-coded

Action column:
├── Icons: Edit, Delete, View
├── Hover: Show tooltip
├── Right-aligned
└── Gap between icons: sm (8px)

Example table in Figma:
Columns: Date | Test Type | Status | Value | Actions
Rows show health record data
```

---

### 3.2 Avatar Component

**Tên component:** `Avatar / Image`, `Avatar / Initials`, `Avatar / Icon`

```
Avatar structure:
├── Circular image or fallback
├── Optional: Border
├── Optional: Badge (online status)
└── Optional: Group overlay

Specifications:

Sizes:

Small (32px):
├── Image: 32×32px
├── Border-radius: full
├── Font: 12px (if initials)
└── Use: Inline, lists

Medium (40px):
├── Image: 40×40px
├── Border-radius: full
├── Font: 14px (if initials)
└── Use: Default in most places

Large (48px):
├── Image: 48×48px
├── Border-radius: full
├── Font: 16px (if initials)
└── Use: Profile page, hero section

XLarge (64px):
├── Image: 64×64px
├── Border-radius: full
├── Font: 24px (if initials)
└── Use: Large profile display

Variants:

1. Image Avatar
   ├── Real image
   ├── Border: 2px slate-200
   ├── Shadow: shadow-1
   └── Alt text: Full name

2. Initials Fallback
   ├── Background: teal-600
   ├── Text: white (2 letters)
   ├── Font: Bold, centered
   ├── Example: "TA" for Trần Văn A
   └── Border-radius: full

3. Icon Avatar
   ├── Icon inside circle
   ├── Background: slate-200
   ├── Icon: slate-600
   └── Use: Unknown users

4. Group Avatar (Multiple)
   ├── Stack of 3-4 avatars
   ├── Overlap: -8px each
   ├── Show: 3 + "+2" indicator
   └── Size: Stacked

Online badge (optional):
├── Small green dot (bottom-right)
├── Size: 8×8px
├── Background: green-500
├── Border: 2px white
└── Indicates online status
```

---

### 3.3 Skeleton/Loading Component

**Tên component:** `Skeleton / Line`, `Skeleton / Circle`, `Skeleton / Card`

```
Skeleton structure:
├── Placeholder shape
├── Animated shimmer effect
└── Matches content shape

Specifications:

General skeleton:
├── Background: slate-200
├── Animation: Shimmer (gradient moving left to right)
├── Duration: 1.5s infinite
└── Border-radius: Matches final content

Skeleton variants:

1. Line Skeleton
   ├── Height: 16px (for body text)
   ├── Width: 100% or partial
   ├── Border-radius: sm (4px)
   └── Use: Text loading

2. Circle Skeleton
   ├── Size: 40×40px (or custom)
   ├── Border-radius: full
   └── Use: Avatar loading

3. Card Skeleton
   ├── Contains:
   │   ├── Circle (40×40px, avatar)
   │   ├── Title line (90% width)
   │   ├── Description lines (3 lines, 100% width)
   │   └── Footer line (50% width)
   │
   ├── Padding: 16px (lg)
   └── Use: Card content loading

4. Table Row Skeleton
   ├── Multiple columns
   ├── Each column: Line skeleton
   ├── Height: 44px
   └── Use: Table data loading

Animation (Shimmer):

```css
@keyframes shimmer {
  0% { background-position: -1000px 0; }
  100% { background-position: 1000px 0; }
}

Background: linear-gradient(
  90deg,
  slate-200 0%,
  slate-100 50%,
  slate-200 100%
);
Background-size: 1000px 100%;
Animation: shimmer 1.5s infinite;
```

Accessibility:
├── aria-busy: true
├── aria-label: "Loading..."
└── Semantic skeleton role
```

---

### 3.4 Empty State Component

**Tên component:** `EmptyState / NoResults`, `EmptyState / NoData`, `EmptyState / FirstUse`

```
Empty state structure:
├── Illustration/Icon
├── Title
├── Description
├── CTA button
└── Optional: Secondary action

Specifications:

Container:
├── Padding: 3xl (32px)
├── Text-align: center
├── Min-height: 300px
└── Responsive: Full width

Illustration:
├── Size: 120px × 120px
├── Color: teal-300 or gray
├── Icon: Lucide icon or custom SVG
├── Top margin: 2xl (24px)
└── Examples:
    ├── Empty folder (no records)
    ├── Search icon (no results)
    ├── Plus icon (add first item)

Title:
├── Font: heading-3 (24px), weight 600
├── Color: slate-900
├── Top margin: 2xl (24px)
├── Text: "Chưa có kết quả nào"

Description:
├── Font: body (16px), weight 400
├── Color: slate-600
├── Top margin: lg (16px)
├── Text: "Hãy tạo kết quả đầu tiên..."

CTA Button:
├── Primary button (teal-600)
├── Text: "Tạo mới" or "Tải lên"
├── Top margin: xl (20px)
├── Leads to creation flow

Optional secondary link:
├── Text link (teal-600)
├── Text: "Tìm hiểu thêm" or "Cần giúp?"
├── Top margin: md (12px)
└── Leads to docs

Variants:

1. No Records
   ├── Icon: Folder outline
   ├── Title: "Chưa có kết quả nào"
   ├── Description: "Hãy tải kết quả đầu tiên..."
   └── CTA: "Tải kết quả"

2. No Search Results
   ├── Icon: Search icon
   ├── Title: "Không tìm thấy kết quả"
   ├── Description: "Thử từ khóa khác..."
   └── CTA: "Xóa bộ lọc"

3. First Use (Onboarding)
   ├── Icon: Plus/welcome icon
   ├── Title: "Bắt đầu với HealthLens"
   ├── Description: "Tải kết quả xét nghiệm của bạn..."
   └── CTA: "Tải kết quả đầu tiên"

4. Error State
   ├── Icon: Warning/error icon
   ├── Title: "Có lỗi xảy ra"
   ├── Description: "Không thể tải dữ liệu..."
   └── CTA: "Thử lại"
```

---

### 3.5 Error State Component

**Tên component:** `ErrorState / Default`, `ErrorState / Form`, `ErrorState / Page`

```
Error state structure:
├── Error icon
├── Error message
├── Technical details (optional)
├── CTA button
└── Contact support link

Specifications:

Container:
├── Background: red-50
├── Border: 2px solid red-600
├── Border-radius: lg (12px)
├── Padding: 2xl (24px)
├── Margin: 2xl (24px)
└── Shadow: shadow-2

Icon:
├── X or alert icon
├── Color: red-600
├── Size: 48px × 48px
└── Top-left position

Message:
├── Font: heading-4 (18px), weight 600
├── Color: red-900
├── Text: Error title
├── Example: "Không thể tải dữ liệu"

Description:
├── Font: body (16px), weight 400
├── Color: red-700
├── Text: Explanation
├── Example: "Vui lòng kiểm tra kết nối internet..."

Technical details (optional):
├── Font: monospace 12px
├── Color: red-600
├── Code/error message
├── Only in dev mode (optional)

Action buttons:
├── Primary: "Thử lại" (red-600)
├── Secondary: "Quay lại" (gray)
└── Tertiary: "Liên hệ hỗ trợ" (link)

Variants:

1. Form Error
   ├── Inline with form field
   ├── Smaller size
   ├── Red border on field
   └── Error text below

2. Page Error
   ├── Full page
   ├── Large error state
   ├── Help center link
   └── Return home button

3. Network Error
   ├── Icon: Offline/connection
   ├── Title: "Mất kết nối"
   ├── Message: "Kiểm tra internet..."
   └── Retry button
```

---

## 🎭 TASK 4: Patterns & States (1-2 giờ)

**Open Figma page:** "11. PATTERNS & States"

### 4.1 Loading States

```
Create page showing all loading patterns:

1. Loading Spinner
   ├── Rotating circle animation
   ├── Size: 40×40px
   ├── Stroke: 4px, teal-600
   ├── Duration: 1s continuous
   └── Easing: linear

2. Loading with text
   ├── Spinner + "Loading..." text
   ├── Font: 16px
   ├── Text color: slate-600
   └── Gap: md (12px)

3. Loading skeleton (shown above)
   ├── Shimmer animation
   ├── Matches content shape
   └── Auto-replace with real content

4. Inline loading
   ├── Button with spinner
   ├── Text changes to "Loading..."
   ├── Button disabled
   └── Spinner inside button

5. Page loading
   ├── Full page overlay (50% dark)
   ├── Spinner centered
   ├── Optional text
   └── Blocks all interaction
```

---

### 4.2 Error Handling Patterns

```
Create page showing error patterns:

1. Form field error
   ├── Red border
   ├── Error icon
   ├── Error message below
   ├── Font: 12px, red-600
   └── Example shown

2. Form submit error
   ├── Alert box at top of form
   ├── Type: Error
   ├── Message: General error message
   └── List of field errors

3. Input error
   ├── Border: 2px red-600
   ├── Error text: Below field
   ├── Inline validation
   └── Clear, helpful message

4. API/Network error
   ├── Error state component (shown above)
   ├── Red background
   ├── Error message
   └── Retry button

5. Validation errors
   ├── Real-time feedback
   ├── Green checkmark when valid
   ├── Red X when invalid
   └── Helpful messages
```

---

### 4.3 Success Patterns

```
Create page showing success patterns:

1. Success alert
   ├── Type: Success
   ├── Icon: ✓ (green)
   ├── Message: "Thành công!"
   ├── Auto-dismiss: 3-5s
   └── Green background

2. Success badge
   ├── Green badge
   ├── Checkmark icon
   ├── Text: "Đã lưu"
   └── Small size

3. Success toast
   ├── Bottom-right corner
   ├── Green background
   ├── Auto-dismiss
   └── Slide in/out animation

4. Completion screen
   ├── Large checkmark animation
   ├── Title: "Thành công!"
   ├── Description: Action completed
   └── Next steps button
```

---

### 4.4 Form Patterns

```
Create page showing form patterns:

1. Required field indicator
   ├── Asterisk (*): red-600
   ├── Label with red *
   └── Example: "Họ tên *"

2. Optional field indicator
   ├── "(Optional)" text
   ├── Gray color: slate-500
   └── Example: "Số điện thoại (Optional)"

3. Field validation
   ├── Default: Neutral border
   ├── Focus: Teal border + focus ring
   ├── Valid: Green checkmark icon
   ├── Invalid: Red border + X icon
   └── Success: Green background tint

4. Help text pattern
   ├── Font: 12px, slate-500
   ├── Below field
   ├── Non-intrusive
   └── Helpful guidance

5. Complex form layout
   ├── Sections with dividers
   ├── Related fields grouped
   ├── Consistent spacing
   └── Clear visual hierarchy
```

---

### 4.5 State Combinations

```
Create page showing complex state combinations:

1. Button + Focus + Hover + Disabled
   ├── Show progression
   ├── Visual differences clear
   └── Responsive to user action

2. Input + Focus + Error + Disabled
   ├── Each state distinct
   ├── Error overrides other states
   └── Disabled looks inactive

3. Card + Hover + Selected + Loading
   ├── Card interaction states
   ├── Clear selection indication
   └── Loading overlay if needed

4. Table row + Hover + Selected
   ├── Row highlighting
   ├── Selection checkbox
   └── Action icons appear

5. Modal + Loading + Error
   ├── Modal with content loading
   ├── Skeleton in modal
   ├── Error state in modal
   └── Smooth transitions between states
```

---

## 📋 Phase 2 Completion Checklist

**Trước khi kết thúc Phase 2, kiểm tra:**

- [ ] Tất cả 6 Composite components (Card, Alert, Badge, Chip, Modal, Tooltip, Toast)
- [ ] Tất cả 5 Layout components (SidebarNav, TopNav, Breadcrumb, Tabs, Pagination)
- [ ] Tất cả 5 Data Display components (Table, Avatar, Skeleton, Empty State, Error State)
- [ ] Tất cả 5 Pattern types (Loading, Error, Success, Form, Complex States)
- [ ] Mỗi component có 2-5 states (Default, Hover, Focus, Active, Disabled)
- [ ] Tất cả components có documentation
- [ ] Naming convention consistent trong Figma
- [ ] Responsive variants cho desktop/tablet/mobile (nếu cần)
- [ ] WCAG AA contrast verified trên mỗi component
- [ ] Focus states visible trên tất cả interactive elements
- [ ] Animations specified (timing, easing)
- [ ] All color/typography/spacing tokens sử dụng (không hard-coded)

---

## 🎯 Deliverables Phase 2

**Figma pages completed:**
- [x] Page 6: COMPONENTS - Foundation (Phase 1)
- [ ] Page 7: COMPONENTS - Composite
- [ ] Page 8: COMPONENTS - Layout  
- [ ] Page 9: COMPONENTS - Data Display
- [ ] Page 11: PATTERNS & States

**Total components by end of Phase 2:**
- Foundation: 7 (Button, Input, Label, Checkbox, Radio, Toggle, Select)
- Composite: 7 (Card, Alert, Badge, Chip, Modal, Tooltip, Toast)
- Layout: 5 (SidebarNav, TopNav, Breadcrumb, Tabs, Pagination)
- Data Display: 5 (Table, Avatar, Skeleton, EmptyState, ErrorState)
- **TOTAL: 24+ components với 60+ variants/states**

---

**Estimated time:** 5-7 working days  
**Next phase:** Phase 3 - Custom Health Components + Screen Designs  
**Document version:** 1.0  
**Created:** March 2026
