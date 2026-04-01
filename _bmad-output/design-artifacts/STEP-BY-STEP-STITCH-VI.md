# Hướng dẫn Chi tiết: Từ 0 đến Stitch AI Design Xong

**Phiên bản:** 1.0  
**Ngôn ngữ:** Tiếng Việt  
**Thời gian:** ~2-3 giờ  
**Mục tiêu:** Tạo hoàn chỉnh Design System + Components trong Figma bằng Stitch AI

---

## 📋 PHẦN 1: CHUẨN BỊ BAN ĐẦU (15-20 phút)

### Bước 1.1: Đăng ký/Đăng nhập Stitch AI

1. **Vào trang web:** https://www.stitchdesign.ai/
2. **Đăng ký tài khoản (hoặc đăng nhập nếu có)**
   - Email/Google account
   - Xác nhận email
3. **Chọn plan:** Free hoặc paid (free đủ cho MVP)
4. **Đăng nhập thành công**

---

### Bước 1.2: Chuẩn bị Specifications

**Bạn sẽ copy tất cả specifications từ các file:**

```
Cần copy từ những file này:
├── STITCH-AI-SPECIFICATIONS.md (tất cả)
├── DESIGN-SYSTEM-PLAN.md (sections về tokens)
└── TOKENS-QUICK-REFERENCE.md (hex codes)

Thứ tự copy theo Phase:
1. Color System (Phần 1)
2. Typography (Phần 2)
3. Spacing (Phần 3)
4. Shadows & Radius (Phần 5)
5. Foundation Components (Phần 7)
```

---

### Bước 1.3: Chuẩn bị Prompt cho Stitch AI

**Tạo 1 file text ghi prompt** hoặc copy/paste từ dưới đây:

```
Prompt cho Stitch AI (Copy nguyên bản này):

---START---

# HealthLens Design System - Stitch AI Generation Request

## Project Overview
Dự án: HealthLens (Health Intelligence Platform cho người Việt)
Mục tiêu: Tạo Design System hoàn chỉnh trong Figma
Thời gian: 2-3 giờ
Đầu ra: Components + Tokens sẵn sàng cho developers

## Design Philosophy
- Tên: "Calm Healthcare" (Y tế Bình tĩnh)
- Ấm áp, thân thiện
- Không gây lo lắng
- Dành cho người già (58+) quản lý bệnh mãn tính
- Tiếng Việt hoàn toàn

## Token Specifications

### Colors
Tạo tất cả color palettes sau:

**Primary Brand (Teal) - 10 variants:**
- teal-50: #F0FDFA
- teal-100: #CCFBF1
- teal-200: #99F6E4
- teal-300: #5EEAD4
- teal-400: #2DD4BF
- teal-500: #14B8A6 (Focus ring)
- teal-600: #0D9488 (PRIMARY)
- teal-700: #0F766E
- teal-800: #134E4A
- teal-900: #0D3D3A

**Status Colors (Health) - 4 types, 5 variants each:**
1. Success (Green):
   - green-50: #F0FDF4
   - green-500: #10B981 (Primary badge)
   - green-600: #059669 (Hover)

2. Warning (Yellow):
   - yellow-50: #FFFBEB
   - yellow-500: #F59E0B (Primary badge)
   - yellow-600: #D97706 (Hover)

3. Critical (Red):
   - red-50: #FEF2F2
   - red-500: #EF4444 (Primary badge)
   - red-600: #DC2626 (Hover)

4. Info (Blue):
   - blue-50: #EFF6FF
   - blue-500: #3B82F6 (Primary badge)
   - blue-600: #2563EB (Hover)

**Neutral (Slate) - 12 variants:**
- slate-50: #F8FAFC
- slate-100: #F1F5F9
- slate-200: #E2E8F0
- slate-300: #CBD5E1
- slate-400: #94A3B8
- slate-500: #64748B
- slate-600: #475569
- slate-700: #334155
- slate-800: #1E293B
- slate-900: #0F172A
- slate-950: #020617

**Functional Colors:**
- Focus Ring: teal-500 (#14B8A6)
- Error: red-600 (#DC2626)
- Success: green-600 (#059669)
- Disabled: slate-400 (#94A3B8)

### Typography

**Font Family:** Inter

**Type Scale (8 sizes):**
1. Caption: 12px / 16px (1.33 line-height), weight 400
2. Body Small: 14px / 20px (1.43), weight 400
3. Body: 16px / 24px (1.5), weight 400 [WCAG AA minimum]
4. Heading 4: 18px / 28px (1.56), weight 600, letter-spacing -0.5px
5. Heading 3: 24px / 32px (1.33), weight 600, letter-spacing -0.5px
6. Heading 2: 28px / 36px (1.29), weight 700, letter-spacing -0.5px
7. Heading 1: 30px / 40px (1.33), weight 700, letter-spacing -1px
8. Display: 40px / 48px (1.2), weight 700, letter-spacing -1.5px

**Font Weights Used:** 400 (Regular), 500 (Medium), 600 (Semibold), 700 (Bold)

### Spacing (8px grid base)

Tokens: 4px, 8px, 12px, 16px, 20px, 24px, 32px, 40px, 48px

Naming: xs, sm, md, lg, xl, 2xl, 3xl, 4xl, 5xl

Common patterns:
- Button/Input height: 40px
- Card padding: 16px (lg)
- Page margins: 32px (3xl) desktop
- Gap between items: 12px (md)

### Border Radius

- none: 0px
- sm: 4px (inputs, checkboxes)
- md: 8px (form fields)
- lg: 12px (buttons, cards) - MOST COMMON
- xl: 16px (large components)
- full: 9999px (circles, pills)

### Shadows

5 levels:
- shadow-0: none
- shadow-1: 0 1px 2px 0 rgba(0,0,0,0.05)
- shadow-2: 0 1px 3px 0 rgba(0,0,0,0.1), 0 1px 2px -1px rgba(0,0,0,0.1) [DEFAULT]
- shadow-3: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1)
- shadow-4: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -4px rgba(0,0,0,0.1)
- shadow-5: 0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1)

## Foundation Components

Tạo 7 components sau với các states:

### 1. Button Component

**Variants:** Primary, Secondary, Danger, Ghost

**Primary Button States:**
- Default: Background teal-600, text white, shadow-2, radius lg (12px)
- Hover: Background teal-700, shadow-3
- Focus: teal-600 + focus ring (2-3px teal-500)
- Active: Background teal-800, shadow-1
- Disabled: Background slate-400, opacity 50%

**Sizes:** Small (32px), Medium (40px - DEFAULT), Large (48px)

**Secondary Button:**
- Background: slate-200
- Text: slate-900
- Border: 1px slate-300
- Hover: Background slate-300

**Danger Button:**
- Background: red-600
- Text: white
- Hover: Background red-700

### 2. Input Component

**Input Types:** Text, Email, Password, Number

**Default State:**
- Background: white
- Border: 1px slate-300
- Padding: 8px horizontal, 12px vertical
- Height: 40px
- Radius: md (8px)
- Font: Inter 16px

**States:**
- Default: slate-300 border
- Hover: slate-400 border, shadow-1
- Focus: 2px teal-500 border + focus ring
- Filled: Shows entered value
- Error: 2px red-600 border

**Add error message:** 12px text below, red-600, gap sm (8px)

### 3. Label Component

**Default State:**
- Font: Inter 14px, weight 600
- Color: slate-900
- Used above inputs

**States:**
- Default: slate-900
- Required: Add red asterisk (*)
- Disabled: slate-400

### 4. Form Field (Label + Input + Error)

**Structure:**
- Label (top)
- Gap: md (12px)
- Input (middle)
- Gap: sm (8px)
- Error text (bottom, if error)

**States:**
- Default: Clean form
- Focus: Input focused with teal border
- Error: Red border + error text
- Disabled: All grayed out

### 5. Checkbox Component

**Size:** 20×20px

**States:**
- Unchecked: white background, 2px slate-300 border, radius sm (4px)
- Checked: teal-600 background, white checkmark (✓)
- Hover: border teal-500, shadow-1
- Focus: 2px teal-500 border + focus ring
- Disabled: slate-100 background, opacity 50%

### 6. Radio Button Component

**Size:** 20×20px (circle)

**States:**
- Unselected: white background, 2px slate-300 border
- Selected: white background, 2px teal-600 border, 8px teal-600 dot inside
- Hover: border teal-500, shadow-1
- Focus: 2px teal-500 + focus ring
- Disabled: slate-100, opacity 50%

### 7. Toggle Switch Component

**Size:** 48×28px

**States:**
- Off: slate-300 background, white thumb on left
- On: teal-600 background, white thumb on right
- Animation: Smooth 200ms transition
- Hover: Slight opacity change
- Disabled: slate-200, opacity 50%

## Color Styles Naming in Figma

Create styles with this naming pattern:

Colors/Primary/Teal-50
Colors/Primary/Teal-100
... (all teal variants)

Colors/Status/Success-50
Colors/Status/Success-500
Colors/Status/Success-600
Colors/Status/Warning-50
Colors/Status/Warning-500
Colors/Status/Warning-600
Colors/Status/Critical-50
Colors/Status/Critical-500
Colors/Status/Critical-600
Colors/Status/Info-50
Colors/Status/Info-500
Colors/Status/Info-600

Colors/Neutral/Slate-50
Colors/Neutral/Slate-100
... (all slate variants)

Colors/Functional/Focus-Ring
Colors/Functional/Error
Colors/Functional/Success
Colors/Functional/Disabled

## Typography Styles Naming

Typography/Caption
Typography/Body-Small
Typography/Body
Typography/Heading-4
Typography/Heading-3
Typography/Heading-2
Typography/Heading-1
Typography/Display

## Component Naming

Components/Button/Primary
Components/Button/Secondary
Components/Button/Danger
Components/Button/Primary-Hover
Components/Button/Primary-Focus
Components/Button/Primary-Active
Components/Button/Primary-Disabled
(Similar for other components)

Components/Input/Text
Components/Input/Text-Hover
Components/Input/Text-Focus
Components/Input/Text-Error
Components/Input/Text-Disabled

Components/Label/Default
Components/Label/Required
Components/Label/Disabled

Components/FormField/Default
Components/FormField/Focus
Components/FormField/Error
Components/FormField/Disabled

Components/Checkbox/Unchecked
Components/Checkbox/Checked
Components/Checkbox/Focus
Components/Checkbox/Disabled

Components/Radio/Unselected
Components/Radio/Selected
Components/Radio/Focus
Components/Radio/Disabled

Components/Toggle/Off
Components/Toggle/On
Components/Toggle/Hover
Components/Toggle/Disabled

## Output Format

Please generate:
1. **Figma file** with all tokens and components
2. **Export as:** Figma link or file download
3. **Include:** All states and variants
4. **Organization:** Proper page structure

## Accessibility Requirements

✓ Color contrast: WCAG AA (4.5:1 minimum)
✓ Focus rings: 2-3px teal-500, visible on all interactive elements
✓ Touch targets: Minimum 44px × 44px
✓ No color-only indicators (use icon + text + color)
✓ All interactive elements keyboard accessible

## Design Quality Checklist

- [ ] All colors match exact hex codes
- [ ] Typography sizes match specifications
- [ ] Spacing aligns to 8px grid
- [ ] Focus rings visible and styled correctly
- [ ] All states clearly differentiated
- [ ] No hard-coded colors (use tokens)
- [ ] Consistent naming across all components
- [ ] Shadow depths correct
- [ ] Border radius matches spec
- [ ] Animations smooth (200ms default)

---END---
```

---

## 📍 PHẦN 2: TRÊN STITCH AI WEBSITE (30-45 phút)

### Bước 2.1: Tạo Project Mới

1. **Trên Stitch AI homepage**
   - Click "Create New Project" hoặc "New Design"
   - Đặt tên: "HealthLens Design System"
   - Chọn "Design System" hoặc "Web App"
   - Click "Create"

---

### Bước 2.2: Sử dụng AI Generation Feature

1. **Tìm nút "Generate with AI"** (thường ở top/sidebar)
2. **Paste prompt từ Phần 1.3 vào text box:**
   - Click input field
   - Paste toàn bộ prompt (từ START đến END)
   - **HOẶC** upload file text nếu Stitch AI hỗ trợ

3. **Cấu hình settings:**
   - Framework: React / Figma component
   - Color mode: Light (default)
   - Typography: Inter
   - Pixel base: 8px grid
   - Generate variants: Yes
   - Include states: Yes

4. **Click "Generate"**
   - Chờ Stitch AI xử lý (có thể 5-15 phút)
   - Có thể có progress bar

---

### Bước 2.3: Review & Adjust Generated Design

**Khi Stitch AI hoàn thành:**

1. **Review kết quả:**
   - ✓ Tất cả colors có không?
   - ✓ Typography scale đầy đủ?
   - ✓ Components có states?
   - ✓ Focus rings visible?
   - ✓ Spacing align to grid?

2. **Nếu có sai lệch:**
   - Click "Edit" / "Regenerate"
   - Sửa prompt (chỉ ra vấn đề cụ thể)
   - Generate lại

3. **Nếu OK:**
   - Lưu project: "Save" hoặc "Export"

---

### Bước 2.4: Export từ Stitch AI

1. **Tìm nút "Export"**
   - Figma format (.fig)
   - SVG (if needed)
   - Code format (React/CSS - optional)

2. **Chọn "Export to Figma"** (recommended)
   - Nó sẽ sinh link Figma hoặc download file
   - Hoặc: Direct sync to your Figma account

3. **Tải về máy** nếu cần (file .fig)

---

## 📍 PHẦN 3: IMPORT VÀO FIGMA (30-45 phút)

### Bước 3.1: Mở/Tạo Figma File

1. **Vào https://figma.com**
2. **Nếu chưa có Figma file:**
   - Click "New file"
   - Đặt tên: "HealthLens - Design System"
3. **Nếu đã có (bạn setup từ trước):**
   - Open file có sẵn

---

### Bước 3.2: Import Stitch AI File

**Cách 1: Direct export từ Stitch AI to Figma**
- Stitch AI có nút "Open in Figma"
- Click → Tự động open trong Figma
- File auto-import, xong!

**Cách 2: Upload file .fig**
1. Stitch AI → Export → Download .fig file
2. Figma → File menu → "Import file"
3. Select file .fig từ máy
4. Figma import (có thể mất 1-2 phút)

**Cách 3: Copy/Paste từ Stitch preview**
1. Stitch AI → Preview mode
2. Select all (Ctrl+A)
3. Copy (Ctrl+C)
4. Figma file → Paste (Ctrl+V)
5. Sắp xếp lại nếu cần

---

### Bước 3.3: Organize Figma Pages

**Sau import, sắp xếp lại pages theo structure:**

```
Trong Figma, pages nên có thứ tự:

Pages:
├── 0. Cover Page (có sẵn từ setup trước)
├── 1. TOKENS - Colors (from Stitch AI)
├── 2. TOKENS - Typography (from Stitch AI)
├── 3. TOKENS - Spacing & Grid (from Stitch AI)
├── 4. TOKENS - Shadows & Radius (from Stitch AI)
├── 5. TOKENS - Animation (create manually if needed)
├── 6. COMPONENTS - Foundation (from Stitch AI)
├── 7. COMPONENTS - Composite (TODO - Phase 2)
├── 8. COMPONENTS - Layout (TODO - Phase 2)
├── 9. COMPONENTS - Data Display (TODO - Phase 2)
├── 10. COMPONENTS - Health (TODO - Phase 3)
├── 11. PATTERNS & States (TODO - Phase 2)
├── 12. SCREENS - Authentication (TODO - Phase 4)
├── 13. SCREENS - Dashboard (TODO - Phase 4)
├── 14. SCREENS - Profiles (TODO - Phase 4)
├── 15. SCREENS - Family (TODO - Phase 4)
├── 16. SCREENS - Admin (TODO - Phase 4)
└── 17. SCREENS - Settings (TODO - Phase 4)
```

**Cách sắp xếp:**
1. Right-click page name
2. Drag to reorder
3. Or use page menu

---

### Bước 3.4: Verify Color & Text Styles

**Kiểm tra styles đã được tạo:**

1. **Từ sidebar:** Assets tab (hoặc right panel)
2. **Colors section:**
   - ✓ Có Colors/Primary/Teal-50 đến Teal-900?
   - ✓ Có Colors/Status/* (Success, Warning, Critical, Info)?
   - ✓ Có Colors/Neutral/* (Slate)?
   - ✓ Có Colors/Functional/* (Focus, Error, Success)?

3. **Typography section:**
   - ✓ Có Typography/Caption đến Display?
   - ✓ Tất cả size/weight đúng?

4. **Nếu thiếu:**
   - Tạo thủ công trong Figma
   - Hoặc regenerate từ Stitch AI

---

### Bước 3.5: Verify Components

**Kiểm tra components:**

1. **Assets tab → Components section**
2. **Tìm:**
   - ✓ Components/Button/* (Primary, Secondary, Danger, Ghost)?
   - ✓ Components/Input/* (Text, Email, Password, Number)?
   - ✓ Components/Label/*?
   - ✓ Components/FormField/*?
   - ✓ Components/Checkbox/*?
   - ✓ Components/Radio/*?
   - ✓ Components/Toggle/*?

3. **Mỗi component có states:**
   - Default, Hover, Focus, Active, Disabled

4. **Nếu thiếu states:**
   - Stitch AI có thể chưa tạo hết
   - Tạo thủ công hoặc regenerate

---

## 📍 PHẦN 4: QA & CHỈNH SỬA (15-30 phút)

### Bước 4.1: Visual QA

1. **Mở page "1. TOKENS - Colors"**
   - ✓ 10 teal tones có?
   - ✓ 4 status color groups (Success, Warning, Critical, Info)?
   - ✓ 12 slate neutrals?
   - ✓ Tất cả hex codes đúng?
   - ✓ Color contrast OK (WCAG AA)?

**Nếu sai:**
- Click swatch
- Adjust color picker
- Fix hex code
- Update style

2. **Mở page "2. TOKENS - Typography"**
   - ✓ 8 sizes từ 12px đến 40px?
   - ✓ Font weights: 400, 500, 600, 700?
   - ✓ Line heights đúng?
   - ✓ Letter spacing (-0.5px, -1px) cho headings?

**Nếu sai:**
- Click text
- Adjust trong character panel
- Update style

3. **Mở page "6. COMPONENTS - Foundation"**
   - ✓ Button có 5 states × 4 types = 20 buttons?
   - ✓ Input có 5 states?
   - ✓ Checkbox, Radio, Toggle có states?
   - ✓ Focus rings visible (2-3px teal)?
   - ✓ Colors match tokens?
   - ✓ Spacing align to 8px grid?

**Nếu sai:**
- Edit component
- Fix properties
- Re-publish component

---

### Bước 4.2: Functional QA

**Test tính năng:**

1. **Figma Links:**
   - Create shared link for viewing
   - Share mode: Can edit / View only
   - Copy link

2. **Check component usability:**
   - Drag component to canvas
   - Can override properties?
   - Can swap variant?
   - Working properly?

3. **Check styles:**
   - Apply color style to shape
   - Apply text style to text
   - Are they live-linked?

---

### Bước 4.3: Fix Issues (nếu có)

**Common issues & fixes:**

| Issue | Fix |
|-------|-----|
| Color hex không đúng | Edit color swatch → Update hex → Update style |
| Typography size sai | Edit text → Change size/weight → Update style |
| Component missing state | Duplicate component → Modify → Rename |
| Focus ring không visible | Add stroke to component → Teal-500, 3px |
| Spacing not 8px aligned | Select shape → Adjust x,y,w,h to multiples of 8 |
| Colors not linked to style | Select object → Right-click → Assign style |

---

### Bước 4.4: Documentation

**Thêm documentation trên page:**

1. **Page "0. Cover Page"**
   - Update "Last Updated" date
   - Add link to all token pages

2. **Mỗi token page:**
   - Add title: "🎨 TOKENS - Colors" (dùng emoji)
   - Add usage guide ở cuối page
   - Example: "Use Colors/Primary/Teal-600 for all primary buttons"

3. **Components page:**
   - Add notes cho mỗi component
   - Ví dụ: "Button has 5 states: Default, Hover, Focus, Active, Disabled"

---

## 📍 PHẦN 5: FINAL EXPORT & DELIVERY (10 phút)

### Bước 5.1: Prepare Figma File

1. **Rename pages theo chuẩn:**
   ```
   ✓ 0. 📋 Cover Page
   ✓ 1. 🎨 TOKENS - Colors
   ✓ 2. ✍️ TOKENS - Typography
   ✓ 3. 📏 TOKENS - Spacing & Grid
   ✓ 4. ☀️ TOKENS - Shadows & Radius
   ✓ 5. ⏱️ TOKENS - Animation (if created)
   ✓ 6. 🧩 COMPONENTS - Foundation
   ```

2. **Organize frames properly:**
   - Group components by type
   - Clear naming: "Button / Primary / Default"
   - No orphaned frames

3. **Add version info:**
   - Add text on cover: "Version 1.0 - Phase 1 Complete"
   - Add date: "March 2026"

---

### Bước 5.2: Create Shared Links

1. **Get Figma link:**
   - File menu → "Share"
   - Select "Anyone with link"
   - Choose: "Can edit" or "Can view"
   - Copy link

2. **Share link:**
   - Send to team
   - For developers: "Can view"
   - For designers: "Can edit"

---

### Bước 5.3: Export Assets (Optional)

**Nếu cần export colors/typography cho dev:**

1. **Export color palette:**
   - Window → Tokens Studio (if installed)
   - Or: Manual copy hex codes

2. **Export typography:**
   - Right-click text style
   - Copy CSS (if possible)
   - Or: Manual documentation

3. **Export SVG/PNG:**
   - Select component
   - Right-click → Export
   - Choose format
   - Save

---

## ✅ CHECKLIST: PHASE 1 COMPLETE

**Trước khi quyết định "done", kiểm tra:**

### Colors ✓
- [ ] 40+ color tokens created
- [ ] All hex codes correct (exact match)
- [ ] WCAG AA contrast verified
- [ ] 30+ color styles in Figma
- [ ] Naming pattern consistent (Colors/Primary/Teal-600)

### Typography ✓
- [ ] 8 type sizes defined (12px to 40px)
- [ ] All weights used (400, 500, 600, 700)
- [ ] Line heights correct (1.2 to 1.5)
- [ ] 8 text styles in Figma
- [ ] Font: Inter throughout
- [ ] Body text minimum 16px (WCAG AA)

### Spacing ✓
- [ ] 9 spacing tokens (4px to 64px)
- [ ] All multiples of 8px
- [ ] Common patterns documented
- [ ] Responsive breakpoints noted

### Shadows & Radius ✓
- [ ] 5 shadow levels with exact CSS
- [ ] 6 radius options defined
- [ ] Component-specific radius assigned
- [ ] Applied to components

### Foundation Components ✓
- [ ] 7 components created:
  - [ ] Button (Primary, Secondary, Danger, Ghost)
  - [ ] Input (Text, Email, Password, Number)
  - [ ] Label (Default, Required, Disabled)
  - [ ] FormField (Default, Focus, Error, Disabled)
  - [ ] Checkbox (5 states)
  - [ ] Radio (5 states)
  - [ ] Toggle (4 states)

- [ ] Each component has 3-5 states
- [ ] All states clearly labeled
- [ ] Focus rings visible (2-3px teal-500)
- [ ] Colors use token styles (not hard-coded)
- [ ] Spacing aligns to 8px grid
- [ ] All interactive elements keyboard accessible

### Figma Organization ✓
- [ ] Pages organized with emoji headers
- [ ] Proper naming convention
- [ ] Components in Assets tab
- [ ] Color styles in Assets tab
- [ ] Typography styles in Assets tab
- [ ] Documentation on each page
- [ ] Cover page updated with version

### Accessibility ✓
- [ ] All text colors contrast ≥ 4.5:1
- [ ] All interactive elements have focus ring
- [ ] No color-only indicators
- [ ] Touch targets ≥ 44px × 44px
- [ ] All components keyboard navigable

### Documentation ✓
- [ ] Usage guidelines for each token
- [ ] Component state documentation
- [ ] Color usage rules documented
- [ ] Responsive behavior noted
- [ ] Accessibility notes added
- [ ] Quick reference available

---

## 🎯 BƯỚC TIẾP THEO (Phase 2-4)

**Khi Phase 1 xong:**

### Phase 2 (Tuần 2):
- Composite components (Card, Alert, Badge, Modal, etc.)
- Layout components (Navigation, Tabs, Pagination)
- Data Display (Table, Avatar, Skeleton, Empty State)
- Patterns & States

### Phase 3 (Tuần 3):
- Custom Health Components
- HealthStatusBadge, HealthMetricCard, ProfileCard, etc.

### Phase 4 (Tuần 4):
- Screen designs (20+ screens)
- Authentication, Dashboard, Profiles, Family, Admin, Settings

---

## 🚨 TROUBLESHOOTING

### Stitch AI không generate components

**Solution:**
1. Check prompt syntax
2. Simplify prompt (chia nhỏ)
3. Try "Generate with AI" lại
4. Contact Stitch AI support

### Imported file có issues

**Solution:**
1. Check Figma file format
2. Try copy/paste instead of import
3. Manually create components if needed

### Colors not matching hex codes

**Solution:**
1. Click color swatch
2. Copy hex from prompt
3. Paste into color picker
4. Press Enter
5. Update style

### Focus rings not visible

**Solution:**
1. Add stroke to component
2. Stroke color: teal-500 (#14B8A6)
3. Stroke width: 3px
4. Stroke position: Outside
5. Offset from element: 2px (if possible)

### Styling not applying

**Solution:**
1. Ensure component is main component (not copy)
2. Select element → Right-click → "Detach instance"
3. Edit main component
4. Click "Reset all changes"
5. Publish component

---

## 📞 SUPPORT

**Nếu gặp vấn đề:**

1. **Stitch AI:**
   - Help center: https://www.stitchdesign.ai/help
   - Discord community
   - Email support

2. **Figma:**
   - Help: https://help.figma.com/
   - Community: https://forum.figma.com/

3. **HealthLens team:**
   - Check DESIGN-SYSTEM-PLAN.md
   - Review STITCH-AI-SPECIFICATIONS.md
   - Contact lead designer

---

## 📊 TIMELINE

```
Phase 1 Total Time: ~2-3 hours

0:00-0:20 | Phần 1: Chuẩn bị (Setup Stitch AI)
0:20-1:15 | Phần 2: Generate trên Stitch AI (Generate + Review)
1:15-2:00 | Phần 3: Import vào Figma (Import + Organize)
2:00-2:30 | Phần 4: QA & Chỉnh sửa (Fix + Verify)
2:30-2:45 | Phần 5: Export & Delivery (Final touches)

Total: 2 giờ 45 phút
```

---

**Document Version:** 1.0  
**Language:** Tiếng Việt  
**Created:** March 2026  
**Status:** Ready for Phase 1 Execution
