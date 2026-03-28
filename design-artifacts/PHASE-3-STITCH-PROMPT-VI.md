# Phase 3: Custom Health-Specific Components
# Hướng dẫn Chi tiết + Prompt cho Stitch AI

**Phiên bản:** 1.0  
**Ngôn ngữ:** Tiếng Việt  
**Thời gian:** ~2-3 giờ  
**Mục tiêu:** Tạo 8 custom health components cho HealthLens Platform

---

## 📋 PHẦN 1: CHUẨN BỊ PROMPT CHO PHASE 3

### Prompt cho Stitch AI - Phase 3

**Copy nguyên bản prompt này:**

```
---START PROMPT---

# HealthLens Design System - Phase 3
# Custom Health-Specific Components

## Project Context

Dự án: HealthLens (Health Intelligence Platform)
Phase: 3 (Phase 1 + 2 đã hoàn thành)
Mục tiêu: Tạo 8 custom health components cho health tracking & display
Design System: "Calm Healthcare" - Ấm áp, thân thiện, dành cho người già
Thời gian: Phase 3
Target Users: Người Việt 58+ quản lý bệnh mãn tính

## Reuse Foundation Components from Phase 1 + 2

Sử dụng lại tất cả:
- Phase 1: Colors, Typography, Spacing, Foundation Components (Button, Input, Label, etc.)
- Phase 2: Card, Alert, Badge, Chip, Modal, Tooltip, Toast, Tabs, Breadcrumb, etc.
- All Phase 1 + 2 tokens (not hard-coded)

---

## HEALTH-SPECIFIC COMPONENTS (8 components)

### 1. Health Status Badge Component

**Purpose:** Visual indicator of health status (normal, monitor, concern, critical)

**Design Philosophy:**
- Use 3-part redundancy: Color + Icon + Text
- Large enough for elderly users (min 16px text)
- Color-blind friendly (not just color)
- Accessible and clear at a glance

**Visual Properties:**
├── Container:
│   ├── Height: 36px (can vary with content)
│   ├── Padding: 8px horizontal, 6px vertical
│   ├── Border-radius: md (8px)
│   ├── Display: Inline-flex
│   ├── Gap icon-text: sm (8px)
│   └── Alignment: Center
├── Icon: 20×20px (left)
├── Text: 14px, weight 600 (center)
└── Shadow: Optional shadow-1

**4 Health Status Types:**

Type 1: Normal Status
├── Background: green-50 (#F0FDF4)
├── Border: 1px solid green-200 (#DCFCE7)
├── Icon: Checkmark circle (✓) - green-600 (#059669)
├── Text: "Bình thường" - green-900
├── Meaning: Value is within normal range
└── Example use: Blood pressure normal, glucose ok

Type 2: Monitor Status
├── Background: yellow-50 (#FFFBEB)
├── Border: 1px solid yellow-200 (#FEF3C7)
├── Icon: Alert/triangle (△) - yellow-600 (#D97706)
├── Text: "Cần chú ý" - yellow-900
├── Meaning: Value is slightly elevated, needs monitoring
└── Example use: Blood pressure slightly high, cholesterol borderline

Type 3: Concern Status
├── Background: orange-50 (#FFF7ED)
├── Border: 1px solid orange-200 (#FEEDDE)
├── Icon: Alert (!) - orange-600 (#EA580C)
├── Text: "Cần hành động" - orange-900
├── Meaning: Value is elevated, action needed
└── Example use: Blood pressure elevated, weight gain

Type 4: Critical Status
├── Background: red-50 (#FEF2F2)
├── Border: 1px solid red-200 (#FECACA)
├── Icon: X or alert (✕/!) - red-600 (#DC2626)
├── Text: "Nguy hiểm" - red-900
├── Meaning: Value is critical, immediate action needed
└── Example use: High blood pressure, critical glucose

**States (3 states per type):**
- Default (showing)
- Hover (slight shadow, cursor pointer)
- Focus (focus ring 2px teal-500)

**Component Naming:**
- HealthStatusBadge / Normal
- HealthStatusBadge / Monitor
- HealthStatusBadge / Concern
- HealthStatusBadge / Critical
- HealthStatusBadge / Normal-Hover
- HealthStatusBadge / Monitor-Hover
- (etc. for each type + state)

**Real examples to show:**
- "✓ Bình thường" (green)
- "△ Cần chú ý" (yellow)
- "! Cần hành động" (orange)
- "✕ Nguy hiểm" (red)

---

### 2. Health Metric Card Component

**Purpose:** Display single health metric with value, reference range, status

**Visual Properties:**
├── Container:
│   ├── Width: 280px (default, responsive)
│   ├── Background: white
│   ├── Border: 1px slate-200
│   ├── Border-radius: lg (12px)
│   ├── Shadow: shadow-2
│   ├── Padding: lg (16px)
│   └── Min-height: 200px
├── Icon area (top): 40×40px circle, teal-100 background
├── Spacing: md (12px) between elements
└── Font: Body 16px for values, Caption 14px for labels

**Structure:**
- Icon area (top-left): Health metric icon in circle
- Title: "Huyết áp tâm trương" (16px, weight 600)
- Value section: Large number (24px, weight 700, teal-600)
- Unit: "(mmHg)" (14px, slate-600)
- Reference range: "Bình thường: 60-100" (14px, slate-600)
- Status badge: Health status (Normal/Monitor/Concern/Critical)
- Timestamp: "Cập nhật lúc 14:30 hôm nay" (12px, slate-500)
- Optional: Trend indicator (↑ ↓ = icon next to value)

**Visual Example Layout:**
┌─────────────────────────────────┐
│ ❤️                              │ (Icon: 40×40px, teal-100 bg)
│                                 │
│ Huyết áp tâm trương             │ (Title: 16px, weight 600)
│                                 │
│ 85 mmHg                         │ (Value: 24px, weight 700, teal-600)
│                                 │
│ Bình thường: 60-100 mmHg        │ (Reference: 14px, slate-600)
│                                 │
│ [✓ Bình thường]                 │ (Status badge: green)
│                                 │
│ Cập nhật: 14:30 hôm nay         │ (Timestamp: 12px, slate-500)
└─────────────────────────────────┘

**Metric Types (show 3-4 variants):**

Metric 1: Blood Pressure (Huyết áp)
├── Icon: Heart icon (red heart)
├── Value: "120/80 mmHg"
├── Reference: "Bình thường: <120/<80"
├── Status: Depends on systolic/diastolic
└── Color: Red for heart icon

Metric 2: Glucose (Đường huyết)
├── Icon: Droplet icon
├── Value: "95 mg/dL"
├── Reference: "Bình thường: 70-100"
├── Status: Depends on fasting/non-fasting
└── Color: Orange for glucose

Metric 3: Cholesterol (Cholesterol)
├── Icon: Molecule/chart icon
├── Value: "200 mg/dL"
├── Reference: "Bình thường: <200"
├── Status: Green if normal
└── Color: Yellow for cholesterol

Metric 4: BMI (Chỉ số BMI)
├── Icon: Scale icon
├── Value: "24.5 kg/m²"
├── Reference: "Bình thường: 18.5-24.9"
├── Status: Warning if overweight
└── Color: Purple for weight

**States (3 states):**
- Default (resting)
- Hover (shadow-3, cursor pointer)
- Focus (focus ring 2px teal-500)

**Component Naming:**
- HealthMetricCard / BloodPressure
- HealthMetricCard / Glucose
- HealthMetricCard / Cholesterol
- HealthMetricCard / BMI
- HealthMetricCard / BloodPressure-Hover
- (etc. for each metric + state)

**Trend Indicator (Optional add-on):**
├── Arrow up (↑ red-600): Value increased
├── Arrow down (↓ green-600): Value decreased
├── Dash (→ slate-400): Value stable
├── Position: Right of value number
└── Size: 16px

---

### 3. Health Result Summary Component

**Purpose:** Display health test result with multiple metrics and overall status

**Visual Properties:**
├── Container:
│   ├── Width: Full content width (min 320px, max 600px)
│   ├── Background: white
│   ├── Border: 1px slate-200
│   ├── Border-radius: lg (12px)
│   ├── Shadow: shadow-2
│   ├── Padding: 2xl (24px)
│   └── Spacing: lg (16px) between sections
├── Header section:
│   ├── Test type icon (40×40px)
│   ├── Test name (18px, weight 600)
│   ├── Test date (14px, slate-600)
│   └── Overall status badge
├── Metrics grid: 2-3 columns
├── Footer: Action buttons
└── Font: Body 16px for values

**Structure:**
- Header section:
  - Icon (left, 40×40px circle with background)
  - Test info (title + date, right)
  - Overall status badge (far right)

- Metrics section:
  - Grid of health metric items (2-3 per row)
  - Each metric: Name, Value, Status indicator
  - Spacing: md (12px) between items

- Footer section:
  - "Xem chi tiết" button (primary, teal)
  - "Chia sẻ" button (secondary, slate)
  - "Xóa" button (danger, red)

**Visual Example:**
┌────────────────────────────────────────────┐
│ 🧪 Xét nghiệm máu     15/03/2026  [✓ Bình thường]
│
│ Huyết áp: 120/80 ✓  |  Đường huyết: 95 ✓  |
│ BMI: 24.5 ✓         |  Cholesterol: 200 ✓  |
│
│ [Xem chi tiết] [Chia sẻ] [Xóa]
└────────────────────────────────────────────┘

**Result Type Variants (3 types):**

Type 1: Blood Test (Xét nghiệm máu)
├── Icon: Droplet icon (red-100 background)
├── Metrics: Blood pressure, Glucose, Cholesterol, Triglycerides, etc.
├── Grid: 2-3 columns
└── Status: Based on most critical metric

Type 2: Urine Test (Xét nghiệm nước tiểu)
├── Icon: Flask/beaker icon (blue-100 background)
├── Metrics: Protein, Glucose, Bilirubin, etc.
├── Grid: 2-3 columns
└── Status: Based on results

Type 3: Imaging/Ultrasound (Siêu âm)
├── Icon: Scan/ultrasound icon (purple-100 background)
├── Metrics: Dimensions, Notes, Doctor findings
├── Grid: 1-2 columns (more text)
└── Status: Based on findings

**States (3 states):**
- Default (resting)
- Hover (shadow-3, cursor pointer)
- Selected (border-2 teal-600, shadow-3)

**Component Naming:**
- HealthResultSummary / BloodTest
- HealthResultSummary / UrineTest
- HealthResultSummary / Imaging
- HealthResultSummary / Default-Hover
- HealthResultSummary / Default-Selected

---

### 4. Profile Card Component (User/Family Member)

**Purpose:** Display user or family member profile info (name, age, avatar, health status)

**Visual Properties:**
├── Container:
│   ├── Width: 240px (can vary)
│   ├── Background: white
│   ├── Border: 1px slate-200
│   ├── Border-radius: lg (12px)
│   ├── Shadow: shadow-2
│   ├── Padding: lg (16px)
│   └── Text-align: center
├── Avatar: 64×64px circle
├── Name: 18px, weight 600, slate-900
├── Age info: 14px, slate-600
├── Health status: Badge or text (14px)
├── Action button: 40px height, full width
└── Spacing: md (12px) between elements

**Structure:**
- Avatar (top center)
  - 64×64px circle with border
  - Initials or photo
  - Online indicator (optional green dot)

- Profile info
  - Name (18px, weight 600): "Trần Văn A"
  - Age (14px, slate-600): "65 tuổi"
  - Gender (14px, slate-600): "Nam"
  - Last check-up (14px, slate-500): "Cập nhật lúc 15/03/2026"

- Health status
  - Large badge showing overall health status
  - Color-coded (green/yellow/orange/red)
  - Text: "Bình thường", "Cần chú ý", "Cần hành động", "Nguy hiểm"

- Action buttons
  - Primary: "Xem chi tiết" (go to profile)
  - Secondary: "Chỉnh sửa" (edit profile)
  - Or: "Xem kết quả" (view results)

**Visual Example:**
┌──────────────────────┐
│                      │
│      👤 [Avatar]     │ (64×64px circle)
│                      │
│   Trần Văn A         │ (Name: 18px, bold)
│   65 tuổi, Nam       │ (Age info: 14px)
│                      │
│ [✓ Bình thường]      │ (Health status: green badge)
│                      │
│ Cập nhật: 15/03/26   │ (Last update: 14px)
│                      │
│ [Xem chi tiết]       │ (Primary button)
│                      │
└──────────────────────┘

**Variants (3 types):**

Type 1: My Profile Card
├── Title: "Hồ sơ của tôi"
├── Badge: Your health status
├── Button: "Chỉnh sửa hồ sơ"
├── Info: Full detail (DOB, age, gender, contact)
└── Color: Teal-50 background variant

Type 2: Family Member Card
├── Title: "Thành viên gia đình"
├── Badge: Their health status
├── Button: "Xem chi tiết"
├── Info: Name, age, gender, relationship
├── Relationship label: "Vợ", "Mẹ", "Chồng", etc.
└── Color: White background

Type 3: Elder Care Profile
├── Special for caregiver view
├── Add caregiver phone icon
├── Button: "Liên hệ"
├── Info: Emergency contact
└── Color: Slate-50 variant

**States (3 states):**
- Default (resting)
- Hover (shadow-3, cursor pointer)
- Selected (border-2 teal-600, shadow-3)

**Component Naming:**
- ProfileCard / MyProfile
- ProfileCard / FamilyMember
- ProfileCard / ElderCare
- ProfileCard / Default-Hover
- ProfileCard / Default-Selected

---

### 5. Family Member List Item Component

**Purpose:** Compact list item showing family member (for list view)

**Visual Properties:**
├── Height: 64px
├── Padding: 12px horizontal, 8px vertical
├── Background: white
├── Border-bottom: 1px slate-200
├── Display: Flex (horizontal)
├── Gap: md (12px)
├── Font: 16px for name, 14px for role
└── Avatar: 48×48px left

**Structure (left to right):**
- Avatar: 48×48px circle (left)
- Profile info: Name, Age, Relationship, Last update (middle)
- Health status badge: Color indicator (right)
- Action menu: More options (...) far right

**Layout:**
[Avatar] Name (Age)  Relationship  [Status] [...]
         Updated: 15/03/2026

**Content:**
- Avatar (48×48px circle)
- Name (16px, weight 600): "Trần Văn A"
- Age (14px, slate-600): "65 tuổi"
- Relationship (12px, slate-500): "Vợ", "Mẹ", "Chồng"
- Last update (12px, slate-500): "Cập nhật: 15/03"
- Health status badge: Color code (green/yellow/orange/red)
- Action menu: Dropdown or icon buttons

**States (3 states):**
- Default (resting)
- Hover (background slate-50, shadow-1)
- Selected (background teal-50, border-left 4px teal-600)

**Component Naming:**
- FamilyMemberListItem / Default
- FamilyMemberListItem / Hover
- FamilyMemberListItem / Selected

---

### 6. Reference Range Indicator Component

**Purpose:** Show normal range, highlight where current value falls

**Visual Properties:**
├── Container:
│   ├── Width: Full (min 200px)
│   ├── Height: 80px (includes labels)
│   ├── Padding: lg (16px)
│   └── Background: slate-50
├── Range bar: 8px height, teal-100 background
├── Current value marker: 12px circle, teal-600
├── Labels: 12px font, slate-600
└── Spacing: md (12px) vertical

**Structure:**
- Title: "Giá trị bình thường" (16px, weight 600)
- Range bar:
  - Background: teal-100 (normal range)
  - Left section (optional): red-100 (low/critical)
  - Right section (optional): red-100 (high/critical)
- Current value marker: Circle positioned on bar
- Value label: "Giá trị hiện tại: 85 mmHg" (14px)
- Min/Max labels: "60" and "100" (12px, slate-600)

**Visual Example:**
Huyết áp tâm trương (Diastolic)

60  |←──── ✓ Normal Range (60-100) ────→|  100
    |─────────────────────────────────────|
    |                           ● 85 ✓   |

Giá trị hiện tại: 85 mmHg (Bình thường)

**Variants (3 types):**

Type 1: Normal Range (value within range)
├── Range bar: teal-100 background
├── Marker: teal-600 circle on bar
├── Status: Green checkmark
├── Text: "Bình thường"
└── Example: Glucose 95 (normal 70-100)

Type 2: Out of Range High (value above range)
├── Range bar: teal-100 background
├── Marker: red-600 circle right of bar
├── Status: Orange warning
├── Text: "Cao hơn bình thường"
└── Example: Glucose 150 (normal 70-100)

Type 3: Out of Range Low (value below range)
├── Range bar: teal-100 background
├── Marker: red-600 circle left of bar
├── Status: Orange warning
├── Text: "Thấp hơn bình thường"
└── Example: Glucose 60 (normal 70-100)

**States (2 states):**
- Default (showing)
- Hover on marker (tooltip showing value)

**Component Naming:**
- ReferenceRangeIndicator / Normal
- ReferenceRangeIndicator / High
- ReferenceRangeIndicator / Low
- ReferenceRangeIndicator / Hover

---

### 7. Health Status Timeline Component

**Purpose:** Show health events chronologically (test dates, appointments, notes)

**Visual Properties:**
├── Container:
│   ├── Width: Full (min 300px)
│   ├── Background: white
│   ├── Border: 1px slate-200
│   ├── Border-radius: lg (12px)
│   ├── Padding: 2xl (24px)
│   └── Min-height: 300px
├── Timeline line: 2px solid slate-300 (left side)
├── Timeline item height: 80px each
├── Icon size: 32×32px (on timeline)
├── Font: 16px for title, 14px for date/description
└── Spacing: lg (16px) between items

**Structure:**
- Title: "Lịch sử kiểm tra" (18px, weight 600, slate-900)
- Timeline items (vertical list):
  - Left: Icon (32×32px circle) on vertical line
  - Right: Content (title, date, description)
- Each item: Title + Date + Optional description

**Timeline Item Structure:**
● ← Icon (32×32px circle, color coded)
║
┝─ Title: "Xét nghiệm máu"           (16px, weight 600)
│  Date: "15/03/2026"                (14px, slate-600)
│  Description: "Kết quả bình thường" (14px, slate-500)
│
● ← Next icon

**Item Type Variants (3-4 types):**

Type 1: Test Result
├── Icon: Droplet (red-600 on red-100 circle)
├── Title: "Xét nghiệm máu"
├── Date: "15/03/2026"
├── Description: "Huyết áp bình thường, đường huyết 95"
├── Status indicator: Color badge
└── Action: "Xem chi tiết" link

Type 2: Doctor Appointment
├── Icon: Doctor/calendar (blue-600 on blue-100 circle)
├── Title: "Khám với BS Nguyễn"
├── Date: "10/03/2026"
├── Time: "14:00 - 14:45"
├── Location: "Phòng khám 301"
└── Action: "Xem ghi chú" link

Type 3: Medication Started
├── Icon: Pill/medicine (green-600 on green-100 circle)
├── Title: "Bắt đầu dùng thuốc"
├── Date: "05/03/2026"
├── Description: "Huyết áp cao - Thuốc: Lisinopril 10mg"
└── Status: "Hoạt động"

Type 4: Reminder Note
├── Icon: Notification/bell (yellow-600 on yellow-100 circle)
├── Title: "Nhắc nhở kiểm tra"
├── Date: "30/03/2026"
├── Description: "Cần kiểm tra huyết áp hàng tuần"
└── Status: "Sắp tới"

**Timeline Line Colors:**
├── Completed (past): slate-300
├── Current (today): teal-600 (thicker, 3px)
└── Future: slate-300 (dashed)

**States (2 states):**
- Default (showing)
- Hover on item (background slate-50, shadow-1)

**Component Naming:**
- HealthStatusTimeline / Default
- HealthStatusTimeline / With-Icon
- HealthStatusTimeline / Item-Hover
- HealthStatusTimeline / With-Dates

---

### 8. Quick Action Card Component

**Purpose:** Fast action shortcuts (upload, schedule, view, contact)

**Visual Properties:**
├── Container:
│   ├── Width: 140px (small, can vary)
│   ├── Height: 140px (square)
│   ├── Background: white
│   ├── Border: 2px solid slate-200
│   ├── Border-radius: lg (12px)
│   ├── Shadow: shadow-1
│   ├── Padding: lg (16px)
│   ├── Display: Flex (column center)
│   └── Text-align: center
├── Icon: 48×48px
├── Label: 14px, weight 500
├── Hover state: border teal-600, shadow-3
└── Transition: 200ms ease-out

**Structure:**
- Icon (top center): 48×48px
- Label (bottom center): 14px, weight 500

**Visual Example:**
┌──────────────┐
│              │
│      📤      │ (Icon: 48×48px)
│              │
│  Tải kết quả │ (Label: 14px)
│              │
└──────────────┘

**Quick Action Types (6 types):**

Action 1: Upload Result
├── Icon: Upload arrow (📤 or ↑) - teal-600
├── Label: "Tải kết quả"
├── Background: white
├── Border: slate-200
└── Action: Open file upload modal

Action 2: Schedule Checkup
├── Icon: Calendar (📅) - blue-600
├── Label: "Đặt lịch khám"
├── Background: white
├── Border: slate-200
└── Action: Open appointment scheduler

Action 3: View Results
├── Icon: Document/chart (📊) - orange-600
├── Label: "Xem kết quả"
├── Background: white
├── Border: slate-200
└── Action: Navigate to results page

Action 4: Share with Doctor
├── Icon: Share/arrow (📤) - green-600
├── Label: "Chia sẻ"
├── Background: white
├── Border: slate-200
└── Action: Open share modal

Action 5: Contact Doctor
├── Icon: Phone/message (📞) - purple-600
├── Label: "Liên hệ BS"
├── Background: white
├── Border: slate-200
└── Action: Open contact modal

Action 6: Get Help
├── Icon: Question mark (❓) - slate-600
├── Label: "Trợ giúp"
├── Background: white
├── Border: slate-200
└── Action: Open help/FAQ

**States (3 states):**
- Default (resting)
- Hover (border-2 teal-600, shadow-3, cursor pointer)
- Focus (focus ring 2px teal-500, border teal-600)

**Component Naming:**
- QuickActionCard / Upload
- QuickActionCard / Schedule
- QuickActionCard / ViewResults
- QuickActionCard / Share
- QuickActionCard / Contact
- QuickActionCard / Help
- QuickActionCard / Upload-Hover
- (etc. for each action + state)

---

## COMPONENT SUMMARY

**Custom Health Components (8):**
1. Health Status Badge (4 status types)
2. Health Metric Card (4 metric types)
3. Health Result Summary (3 test types)
4. Profile Card (3 user types)
5. Family Member List Item
6. Reference Range Indicator (3 range types)
7. Health Status Timeline (4 event types)
8. Quick Action Card (6 action types)

**Total: 8 new custom health components** (with 20+ variants for different states/types)

## OUTPUT EXPECTATIONS

1. **Figma file** with all new health components
2. **Page organization:**
   - Page 10: COMPONENTS - Health (All 8 custom components)
3. **Each component** with all states visible
4. **Proper naming** following patterns
5. **Reuse Phase 1 + 2 styles** (not hard-code colors/typography)
6. **Color-blind friendly** - use icons + color
7. **Large text for elderly** - min 14px for labels, min 16px for values
8. **Accessibility** - Focus rings on interactive elements

## DESIGN QUALITY CHECKLIST

- [ ] All colors from Phase 1 styles (not hard-coded)
- [ ] Typography uses Phase 1 text styles
- [ ] Spacing aligns to 8px grid (4, 8, 12, 16, 20, 24, 32, 40px)
- [ ] Border radius matches spec (sm/md/lg/xl/full)
- [ ] Shadows use Phase 1 shadow tokens
- [ ] Focus rings on all interactive elements (2-3px teal-500)
- [ ] States clearly differentiated
- [ ] Icons are 3-part redundancy (icon + color + text)
- [ ] Text sizes min 14px for labels, min 16px for values
- [ ] Component naming consistent
- [ ] All states documented

---END PROMPT---
```

---

## 📍 PHẦN 2: BỨC TIẾP THEO

### Bước 1-7: Giống Phase 2
(Copy prompt → Paste vào Stitch AI → Generate → Import → QA)

### Timeline Phase 3
```
Phase 3 Total Time: ~2-3 giờ

0:00-0:15 | Prepare + Copy prompt
0:15-1:15 | Stitch AI generate (wait)
1:15-1:45 | Import + Organize in Figma
1:45-2:30 | QA + Verify all health components
2:30-3:00 | Fix issues + Documentation

Total: 2-3 giờ
```

---

**Document Version:** 1.0  
**Language:** Tiếng Việt  
**Created:** March 2026  
**Status:** Ready for Phase 3 Execution
