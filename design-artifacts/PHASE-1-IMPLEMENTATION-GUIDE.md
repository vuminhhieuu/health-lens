# Phase 1: Foundation - Step-by-Step Implementation Guide

**Duration:** Week 1 (5-7 working days)  
**Platform:** Figma (Free tier sufficient)  
**Output:** Complete tokens + Foundation components ready for Phase 2

---

## 📍 Phase 1 Overview

**Goal:** Set up all design tokens and foundation components so developers have clear reference

**Tasks:**
1. ✅ Set up Figma file structure
2. ✅ Design Color tokens + create color styles
3. ✅ Design Typography tokens + create text styles  
4. ✅ Design Spacing tokens with grid visualization
5. ✅ Design Shadow & Radius tokens
6. ✅ Create Foundation Components (Button, Input, Label)
7. ✅ All interactive states (default, hover, focus, active, disabled)

**Deliverables:**
- Figma file with organized pages
- All color styles exported
- All typography styles exported
- Component library with variants
- Documentation on each token page

---

## 🎯 Task 1: Figma Setup (30 minutes)

### Step 1.1: Create File Structure

**In Figma, create pages in this exact order:**

```
PAGE STRUCTURE:
├── 0. 📋 Cover Page
├── 1. 🎨 TOKENS - Colors
├── 2. ✍️ TOKENS - Typography  
├── 3. 📏 TOKENS - Spacing & Grid
├── 4. ☀️ TOKENS - Shadows & Radius
├── 5. ⏱️ TOKENS - Animation
├── 6. 🧩 COMPONENTS - Foundation
├── 7. 🔗 COMPONENTS - Composite
├── 8. 🏗️ COMPONENTS - Layout
├── 9. 📊 COMPONENTS - Data Display
├── 10. ❤️ COMPONENTS - Health (Custom)
├── 11. 🎭 PATTERNS & States
├── 12. 🔐 SCREENS - Authentication
├── 13. 📱 SCREENS - Dashboard
├── 14. 👤 SCREENS - Profiles
├── 15. 👥 SCREENS - Sharing
├── 16. 🛠️ SCREENS - Admin
├── 17. ⚙️ SCREENS - Settings
└── 18. ♿ Accessibility & QA
```

**How to create pages:**
1. Right-click on page list (left panel)
2. Select "New page"
3. Rename with number and emoji (keeps organized)

### Step 1.2: Set Up 0. Cover Page

**Create a title page:**

1. Create frame: 1920 × 1080 (desktop size)
2. Add text: "HealthLens Design System"
   - Font: Inter
   - Size: 48px
   - Weight: Bold (700)
   - Color: teal-900 (#0D3D3A)

3. Add subtitle: "Web Dashboard - Phase 1: Foundation"
   - Font: Inter
   - Size: 20px
   - Weight: Regular (400)
   - Color: slate-600 (#475569)

4. Add version info at bottom:
   - "Version 1.0 | Updated March 2026"
   - Size: 14px
   - Color: slate-400 (#94A3B8)

5. Add quick links (just text, no interaction):
   ```
   Contents:
   → Page 1: Color Tokens & Palette
   → Page 2: Typography Scale
   → Page 3: Spacing & Grid System
   → Page 4: Shadows & Radius
   → Page 5: Animation Tokens
   → Page 6-11: Component Library
   → Page 12-18: Screens & Patterns
   ```

---

## 🎨 Task 2: Color Tokens (3-4 hours)

### Step 2.1: Create Color Palette Visualization

**Open page: "1. 🎨 TOKENS - Colors"**

**Create main frame:** 1920 × 2400 (tall page for all colors)

### Section 2.2: Primary Brand Colors (Teal)

**Title:** "Primary Brand - Teal"
- Font: Inter, 28px, Bold (700), color: slate-900

**Create 10 color swatches in a row:**

```
Layout: Horizontal row, gap 8px between each

Swatch 1: teal-50
├── Square: 120×120px
├── Fill color: #F0FDFA
├── Border: 1px slate-200
├── Label below: "teal-50\n#F0FDFA"
└── Font: 12px, slate-600

Swatch 2: teal-100
├── Square: 120×120px
├── Fill color: #CCFBF1
└── Label: "teal-100\n#CCFBF1"

[Continue for teal-200 through teal-900]

Last swatch: teal-900
├── Fill: #0D3D3A
├── Text color: #FFFFFF (white for readability)
└── Label: "teal-900\n#0D3D3A"
```

**Important:** For dark colors (600+), use WHITE text for labels. For light colors (50-400), use DARK text.

### Section 2.3: Health Status Colors

**Title:** "Health Status Colors"

**Create 4 color groups (Success, Warning, Critical, Info):**

```
Group 1: Success (Green) - row of 5 swatches
├── green-50: #F0FDF4
├── green-400: #4ADE80
├── green-500: #10B981  ← PRIMARY SUCCESS
├── green-600: #059669
└── green-700: #047857
Label: "✅ NORMAL/HEALTHY"

Group 2: Warning (Yellow) - row of 5 swatches
├── yellow-50: #FFFBEB
├── yellow-400: #FACC15
├── yellow-500: #F59E0B  ← PRIMARY WARNING
├── yellow-600: #D97706
└── yellow-700: #B45309
Label: "⚠️  MONITOR/CAUTION"

Group 3: Critical (Red) - row of 5 swatches
├── red-50: #FEF2F2
├── red-400: #F87171
├── red-500: #EF4444  ← PRIMARY CRITICAL
├── red-600: #DC2626
└── red-700: #B91C1C
Label: "🚨 ABNORMAL/ACTION"

Group 4: Info (Blue) - row of 5 swatches
├── blue-50: #EFF6FF
├── blue-400: #60A5FA
├── blue-500: #3B82F6  ← PRIMARY INFO
├── blue-600: #2563EB
└── blue-700: #1D4ED8
Label: "ℹ️  INFORMATION"
```

### Section 2.4: Neutral Colors (Slate)

**Title:** "Neutral Palette - Slate (for text, backgrounds, borders)"

**Create complete slate scale (12 swatches):**

```
slate-50  → slate-950 (all 12 steps)

Layout: Grid format or long horizontal row

Key ones to highlight with annotations:
├── slate-50:   #F8FAFC  → "Page background"
├── slate-100:  #F1F5F9  → "Card background"  
├── slate-200:  #E2E8F0  → "Subtle border"
├── slate-300:  #CBD5E1  → "Default border"
├── slate-700:  #334155  → "Body text"
├── slate-900:  #0F172A  → "Heading text"
└── slate-950:  #020617  → "Max contrast (reserved)"
```

### Section 2.5: Functional Colors

**Title:** "Functional & Accessibility Colors"

```
Focus Ring:
├── Color: teal-500 (#14B8A6)
├── Width: 3px
├── Radius: 4px (offset)
└── Example: Show button with focus ring

Error/Validation:
├── Color: red-600 (#DC2626)
└── Example: Input field with red border

Success Feedback:
├── Color: green-600 (#059669)
└── Example: Checkmark in green

Disabled State:
├── Color: slate-400 (#94A3B8)
├── Opacity: 50%
└── Example: Disabled button (light appearance)
```

### Step 2.6: Create Color Styles in Figma

**NOW create Figma color styles:**

1. Select teal-50 swatch
2. Right-click → "Create style"
3. Name it: `Colors/Primary/Teal-50`
4. Repeat for ALL colors using this pattern:
   ```
   Colors/Primary/Teal-50
   Colors/Primary/Teal-100
   ...
   Colors/Primary/Teal-900
   
   Colors/Status/Success-500
   Colors/Status/Warning-500
   Colors/Status/Critical-500
   Colors/Status/Info-500
   
   Colors/Neutral/Slate-50
   Colors/Neutral/Slate-100
   ... etc
   
   Colors/Functional/FocusRing
   Colors/Functional/Error
   Colors/Functional/Disabled
   ```

**Result:** You now have 40+ reusable color styles!

### Step 2.7: Add Color Usage Documentation

**At bottom of page, add a reference table:**

```
Create a text frame with color usage rules:

"COLOR USAGE RULES:

PRIMARY ACTIONS (Buttons, Links):
  Background: teal-600 (#0D9488)
  Text: White (#FFFFFF)
  Border radius: 12px
  Hover: teal-700 (#0F766E)
  Active: teal-800 (#134E4A)
  Disabled: slate-400 (#94A3B8) + opacity 50%

SECONDARY BUTTONS:
  Background: slate-200 (#E2E8F0)
  Text: slate-900 (#0F172A)
  Hover: slate-300 (#CBD5E1)

HEALTH STATUS DISPLAY (Always use 3-part redundancy):
  ✅ Normal:     icon + green-500 + text 'Normal'
  ⚠️  Warning:    icon + yellow-500 + text 'Monitor'
  🚨 Critical:   icon + red-500 + text 'Action Needed'

TEXT ON BACKGROUNDS:
  Heading on slate-50:  slate-900 (#0F172A) ✓ 16:1 contrast
  Body on slate-50:     slate-700 (#334155) ✓ 11.5:1 contrast
  Helper on slate-50:   slate-500 (#64748B) ✓ 4.5:1 contrast (minimum)

BORDERS:
  Subtle (inactive):    slate-200 (#E2E8F0)
  Default (active):     slate-300 (#CBD5E1)
  Strong (emphasis):    slate-400 (#94A3B8)
  Error:                red-600 (#DC2626)

FOCUS STATE:
  Always: 3px solid teal-500 (#14B8A6)
  Offset: 2px from element
"
```

**Save this page.** ✅ Task 2 complete!

---

## ✍️ Task 3: Typography Tokens (2-3 hours)

### Step 3.1: Create Typography Scale Page

**Open page: "2. ✍️ TOKENS - Typography"**

**Create main frame:** 1920 × 2400

### Step 3.2: Show Type Sizes

**Create a visual scale showing all 8 sizes:**

```
LAYOUT: Vertical stack, gap 32px between each

1. CAPTION (12px)
   ├── Sample text: "Updated 30 minutes ago"
   ├── Font: Inter
   ├── Size: 12px
   ├── Weight: 400
   ├── Line height: 16px (1.33)
   ├── Annotation: "12px / 16px (1.33) | For timestamps, helpers"
   └── Color: slate-600

2. BODY SMALL (14px)
   ├── Sample: "Click here to view more details about your health record"
   ├── Size: 14px
   ├── Weight: 400
   ├── Line height: 20px (1.43)
   ├── Annotation: "14px / 20px (1.43) | Secondary descriptions"
   └── Color: slate-600

3. BODY (16px) ⭐ [WCAG AA MINIMUM]
   ├── Sample: "Your health record shows normal results. All metrics are within the recommended range."
   ├── Size: 16px
   ├── Weight: 400
   ├── Line height: 24px (1.5)
   ├── Annotation: "16px / 24px (1.5) | Default body text - MINIMUM FOR ACCESSIBILITY"
   ├── Color: slate-700
   └── [Box highlight in light teal]

4. HEADING 4 (18px)
   ├── Sample: "Your Health Summary"
   ├── Size: 18px
   ├── Weight: 600
   ├── Line height: 28px (1.56)
   ├── Annotation: "18px / 28px (1.56) | Card titles, section headings"
   └── Color: slate-900

5. HEADING 3 (24px)
   ├── Sample: "Health Records"
   ├── Size: 24px
   ├── Weight: 600
   ├── Line height: 32px (1.33)
   ├── Letter-spacing: -0.5px
   ├── Annotation: "24px / 32px (1.33) | Page section headings"
   └── Color: slate-900

6. HEADING 2 (28px)
   ├── Sample: "My Dashboard"
   ├── Size: 28px
   ├── Weight: 700
   ├── Line height: 36px (1.29)
   ├── Letter-spacing: -0.5px
   ├── Annotation: "28px / 36px (1.29) | Major page sections"
   └── Color: slate-900

7. HEADING 1 (30px)
   ├── Sample: "HealthLens Dashboard"
   ├── Size: 30px
   ├── Weight: 700
   ├── Line height: 40px (1.33)
   ├── Letter-spacing: -1px
   ├── Annotation: "30px / 40px (1.33) | Main page title"
   └── Color: slate-900

8. DISPLAY (40px)
   ├── Sample: "Welcome to HealthLens"
   ├── Size: 40px
   ├── Weight: 700
   ├── Line height: 48px (1.2)
   ├── Letter-spacing: -1.5px
   ├── Annotation: "40px / 48px (1.2) | Hero/landing sections (Phase 2+)"
   └── Color: teal-900
```

### Step 3.3: Font Weight Reference

**Create a separate section:**

```
FONT WEIGHTS (Inter)

Light (300)
├── Sample: "This text is light and delicate"
├── Use: Very large display text only
└── Color: slate-500

Regular (400) ⭐ [DEFAULT]
├── Sample: "This is the default body text weight"
├── Use: All body text, paragraphs
└── Color: slate-700

Medium (500)
├── Sample: "Form labels and secondary emphasis"
├── Use: Form labels, small headings
└── Color: slate-700

Semibold (600)
├── Sample: "Card titles and section headings"
├── Use: H4-H3 headings, emphasized labels
└── Color: slate-900

Bold (700)
├── Sample: "Page titles and strong emphasis"
├── Use: H1-H2 headings, CTAs
└── Color: slate-900

Extrabold (800)
├── Sample: "Maximum emphasis (use sparingly)"
├── Use: Reserved for special emphasis only
└── Color: slate-900
```

### Step 3.4: Line Height Reference

```
OPTIMAL LINE HEIGHTS

Tight (1.2)
├── For headings with short lines
├── Example: Heading in 28px
├── Sample: "Display Text\nWith Tight\nSpacing"

Normal (1.5) ⭐ [RECOMMENDED FOR BODY]
├── For paragraphs and body text
├── Optimal readability
├── Sample: "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

Relaxed (1.75)
├── For dense information
├── Helps scanning
├── Sample: "Dense medical information\ncould benefit\nfrom relaxed spacing"
```

### Step 3.5: Create Typography Styles

**In Figma, create text styles:**

1. Select body text (16px, weight 400)
2. Right-click → "Create style"
3. Name: `Typography/Body`
4. Repeat for all 8 sizes:
   ```
   Typography/Caption
   Typography/Body-Small
   Typography/Body
   Typography/Heading-4
   Typography/Heading-3
   Typography/Heading-2
   Typography/Heading-1
   Typography/Display
   ```

**Also create weight variations:**
```
Typography/Heading-3-Bold (weight 700)
Typography/Heading-4-Semibold (weight 600)
```

### Step 3.6: Add Font Stack Documentation

```
FONT FAMILY (Copy-paste for developers):

Web: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
Fallback: Roboto

Monospace (for code/data):
"JetBrains Mono", "Courier New", monospace

Why Inter?
✓ Excellent Vietnamese support
✓ Highly legible at small sizes (WCAG AA requirement)
✓ Clean medical aesthetic
✓ Good for elderly users (clear letterforms)
```

**Save this page.** ✅ Task 3 complete!

---

## 📏 Task 4: Spacing Tokens (1.5-2 hours)

### Step 4.1: Create Spacing Visualization

**Open page: "3. 📏 TOKENS - Spacing & Grid"**

**Create main frame:** 1920 × 1800

### Step 4.2: Create Spacing Scale

```
SPACING SCALE (8px base grid)

Title: "Spacing Tokens"

Show each spacing as visual blocks:

xs (4px):
├── Small rectangle: 4px × 80px
├── Label: "xs: 4px"
├── Use: "Icon padding, tight lists"
└── Color: teal-100

sm (8px):
├── Rectangle: 8px × 80px
├── Label: "sm: 8px"
├── Use: "Gap between items, default spacing"
└── Color: teal-100

md (12px):
├── Rectangle: 12px × 80px
├── Label: "md: 12px"
├── Use: "Standard padding, moderate gaps"
└── Color: teal-100

lg (16px):
├── Rectangle: 16px × 80px
├── Label: "lg: 16px"
├── Use: "Default button/card padding"
└── Color: teal-200

xl (20px):
├── Rectangle: 20px × 80px
├── Label: "xl: 20px"
├── Use: "Large padding, spacious layouts"
└── Color: teal-200

2xl (24px):
├── Rectangle: 24px × 80px
├── Label: "2xl: 24px"
├── Use: "Section spacing, clear separation"
└── Color: teal-300

3xl (32px):
├── Rectangle: 32px × 80px
├── Label: "3xl: 32px"
├── Use: "Major section spacing"
└── Color: teal-300

4xl (40px):
├── Rectangle: 40px × 80px
├── Label: "4xl: 40px"
├── Use: "Large layout spacing"
└── Color: teal-400

5xl (48px):
├── Rectangle: 48px × 80px
├── Label: "5xl: 48px"
├── Use: "Extra large spacing"
└── Color: teal-400
```

**Display all as vertical stack** so spacing is visually clear!

### Step 4.3: Show 8px Grid System

```
GRID SYSTEM VISUALIZATION:

Title: "8px Base Grid"

Create a grid overlay showing 8px increments:
├── Use light slate-200 lines
├── Width/Height: multiple of 8
└── Example: 320×240px grid with 8px squares marked
```

### Step 4.4: Common Spacing Patterns

```
PATTERN: Button/Input Height
├── Small:   32px (padding: 4px 12px)
├── Medium:  40px (padding: 8px 16px)  ⭐ [DEFAULT]
└── Large:   48px (padding: 12px 20px)

PATTERN: Card Padding
├── Compact:  12px
├── Default:  16px  ⭐
└── Spacious: 20px (recommended for elderly)

PATTERN: Gap Between Cards
├── Tight:    8px
├── Default:  12px  ⭐
└── Spacious: 16px

PATTERN: Page/Section Margins
├── Mobile:  16px (lg)
├── Tablet:  24px (2xl)
├── Desktop: 32px (3xl)  ⭐

PATTERN: Section Spacing (vertical gap)
├── Small:  16px (lg)
├── Medium: 24px (2xl)  ⭐
└── Large:  32-40px (3xl-4xl)
```

### Step 4.5: Responsive Spacing Guide

```
RESPONSIVE BREAKPOINTS:

Desktop (1280px and above):
├── Page margins:    32px (3xl)
├── Section gap:     40px (4xl)
├── Component gap:   12px (md)
└── Card padding:    16px (lg)

Tablet (768px - 1279px):
├── Page margins:    24px (2xl)
├── Section gap:     32px (3xl)
├── Component gap:   12px (md)
└── Card padding:    16px (lg)

Mobile (375px) - Reference:
├── Page margins:    16px (lg)
├── Section gap:     24px (2xl)
├── Component gap:   8px (sm)
└── Card padding:    12px (md)
```

**Save this page.** ✅ Task 4 complete!

---

## ☀️ Task 5: Shadow & Radius Tokens (1-1.5 hours)

### Step 5.1: Create Shadow Page

**Open page: "4. ☀️ TOKENS - Shadows & Radius"**

**Create main frame:** 1920 × 1600

### Step 5.2: Show All Shadow Depths

```
SHADOW LEVELS:

Shadow 0 (None):
├── Card with white background
├── No shadow
├── Label: "shadow-0: None (flat)"
└── Use: "Flat elements, disabled state"

Shadow 1 (Subtle):
├── Card with shadow
├── box-shadow: 0 1px 2px 0 rgba(0,0,0,0.05)
├── Label: "shadow-1: Subtle"
└── Use: "Hover states, light elevation"

Shadow 2 (Light):
├── Card with shadow
├── box-shadow: 0 1px 3px 0 rgba(0,0,0,0.1), 0 1px 2px
├── Label: "shadow-2: Light"  ⭐ [DEFAULT]
└── Use: "Default cards, inputs, buttons"

Shadow 3 (Medium):
├── Card with shadow
├── box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px
├── Label: "shadow-3: Medium"
└── Use: "Elevated cards, dropdowns"

Shadow 4 (Prominent):
├── Card with shadow
├── box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px
├── Label: "shadow-4: Prominent"
└── Use: "Modals, overlays"

Shadow 5 (Maximum):
├── Card with shadow
├── box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px
├── Label: "shadow-5: Maximum"
└── Use: "Full-page overlays"

[Show these side-by-side so depth progression is clear]
```

### Step 5.3: Show Border Radius Scale

```
BORDER RADIUS:

none (0px):
├── Square: 60×60px
├── Fill: teal-600
├── Label: "none: 0px"
└── Use: "Icon shapes (reserved)"

sm (4px):
├── Square: 60×60px
├── Fill: teal-600
├── Rounded corners: 4px
├── Label: "sm: 4px"
└── Use: "Precise controls"

md (8px):
├── Square: 60×60px
├── Rounded corners: 8px
├── Label: "md: 8px"  ⭐
└── Use: "Input fields, form controls"

lg (12px):
├── Square: 60×60px
├── Rounded corners: 12px
├── Label: "lg: 12px"  ⭐⭐ [MOST COMMON]
└── Use: "Buttons, cards, modals"

xl (16px):
├── Square: 60×60px
├── Rounded corners: 16px
├── Label: "xl: 16px"
└── Use: "Very rounded, friendly appearance"

full (9999px):
├── Circle: 60×60px
├── Label: "full: 9999px"
└── Use: "Circles, pill buttons"

[Show all 6 in a grid]
```

### Step 5.4: Add Shadow + Radius Combinations

```
COMPONENT EXAMPLES:

Button (Primary):
├── Background: teal-600
├── Radius: lg (12px)
├── Shadow: shadow-2
└── "Friendly, approachable feeling"

Input Field:
├── Background: white
├── Border: 1px slate-300
├── Radius: md (8px)
├── Shadow: none (stays flat)
└── "Professional, precise"

Card:
├── Background: white  
├── Radius: lg (12px)
├── Shadow: shadow-2
├── Padding: lg (16px)
└── "Warm, inviting"

Modal/Overlay:
├── Background: white
├── Radius: lg (12px)
├── Shadow: shadow-4
└── "Clear prominence, layered"

Disabled Button:
├── Background: slate-400
├── Radius: lg (12px)
├── Shadow: none (flat)
└── "Visual feedback: inactive"
```

**Save this page.** ✅ Task 5 complete!

---

## 🧩 Task 6: Foundation Components (4-5 hours)

### Step 6.1: Create Components Page

**Open page: "6. 🧩 COMPONENTS - Foundation"**

**Create main frame:** 1920 × 3000 (tall for many components)

### Step 6.2: Create Button Component

**Title:** "Button Component"

**Create the first button variant:**

1. Draw rectangle: 120px × 40px
2. Fill: teal-600 (#0D9488)
3. Radius: lg (12px)
4. Shadow: shadow-2
5. Add text inside:
   - Text: "Button"
   - Font: Inter 16px, weight 600, color white
   - Center aligned

6. **Right-click → Create component**
7. Name: `Components/Button/Primary`

**Now create 4 variants (by duplicating):**

```
Variant 1: PRIMARY (done above)
├── Background: teal-600
├── Text: white
├── Shadow: shadow-2
├── State: Default

Variant 2: PRIMARY - HOVER
├── Duplicate the primary button
├── Background: teal-700 (darker)
├── Shadow: shadow-3
├── Annotation: "Hover state"

Variant 3: PRIMARY - FOCUS
├── Duplicate primary button
├── Add focus ring: 2-3px solid teal-500 (outside box)
├── Shadow: shadow-3
└── Annotation: "Keyboard focus"

Variant 4: PRIMARY - ACTIVE
├── Background: teal-800 (even darker)
├── Shadow: shadow-1 (inset pressed effect)
└── Annotation: "Clicked/pressed state"

Variant 5: PRIMARY - DISABLED
├── Background: slate-400 (#94A3B8)
├── Text opacity: 50%
├── Shadow: none
└── Annotation: "Inactive, cursor not-allowed"
```

**Group these variants together:**
1. Select all 5 buttons
2. Right-click → "Create component set"
3. Name: `Button / Primary`
4. Add property: "State" with values: Default, Hover, Focus, Active, Disabled

### Step 6.3: Create Secondary & Tertiary Button Variants

**Create Secondary button set (same states):**
```
PRIMARY BUTTON SET (Done above)
├── Background: teal-600
├── Text: white

SECONDARY BUTTON SET:
├── Background: slate-200 (#E2E8F0)
├── Text: slate-900 (#0F172A)
├── Border: 1px slate-300
├── Hover: Background slate-300
├── Focus: Add focus ring
└── Active: Background slate-400

DANGER BUTTON SET:
├── Background: red-600 (#DC2626)
├── Text: white
├── Hover: Background red-700
├── Focus: Add focus ring
└── Active: Background red-800

GHOST BUTTON SET (no background):
├── Background: transparent
├── Text: teal-600
├── Border: 1px teal-600
├── Hover: Background teal-50
├── Focus: Add focus ring + border teal-700
└── Active: Background teal-100
```

### Step 6.4: Create Input Component

**Title:** "Input / Text Field"

```
Draw rectangle: 320px × 40px
├── Fill: white (#FFFFFF)
├── Border: 1px slate-300 (#CBD5E1)
├── Radius: md (8px)
├── Shadow: none

Add text inside:
├── Placeholder text: "Enter text..."
├── Font: Inter 16px, weight 400
├── Color: slate-400 (placeholder gray)

Right-click → Create component
Name: `Components/Input/Default`

Create 4 states:
├── Input/Default (done)
├── Input/Hover
│   └── Border: slate-400
├── Input/Focus
│   ├── Border: 2px teal-500
│   └── Focus ring: 2-3px solid teal-500 (outside)
├── Input/Filled
│   ├── Shows typed text: "Health Record"
│   ├── Font color: slate-900
│   ├── Border: slate-300
│   └── Clear button (X) on right
└── Input/Error
    ├── Border: 2px red-600
    ├── Border color: red-600
    └── Error text below: "This field is required"
         Font: 12px, color: red-600
```

### Step 6.5: Create Label Component

**Create a label to go with inputs:**

```
Text: "Email Address"
├── Font: Inter 14px, weight 600
├── Color: slate-900
├── Used above input field with gap md (12px)

Create component: `Components/Label/Default`

States:
├── Label/Default (shown)
├── Label/Required
│   └── Add red asterisk: "*"
│       Font color: red-600
└── Label/Disabled
    └── Color: slate-400
```

### Step 6.6: Create FormField (Label + Input combo)

**Combine label + input + error message:**

```
Frame: 320px × 90px
├── Contains:
│   ├── Label (top)
│   ├── Input field (middle, gap md)
│   └── Helper/Error text (bottom, gap sm)
│
├── States:
│   ├── FormField/Default
│   ├── FormField/Focus (input focused, border teal)
│   ├── FormField/Error (border red, error text red)
│   └── FormField/Disabled (all text slate-400)
│
└── Create component: `Components/FormField/Default`
```

### Step 6.7: Create Checkbox Component

```
Draw square: 20px × 20px
├── Fill: white
├── Border: 2px slate-300
├── Radius: sm (4px)

Add checkmark icon inside (use Lucide plugin):
├── Icon: "check"
├── Color: white
├── Size: 14px

States:
├── Checkbox/Unchecked
│   └── No fill, border slate-300
├── Checkbox/Checked
│   ├── Fill: teal-600
│   ├── Border: teal-600
│   └── Show white checkmark
├── Checkbox/Hover
│   └── Border: teal-500, shadow-1
├── Checkbox/Focus
│   └── Add focus ring (teal-500)
└── Checkbox/Disabled
    ├── Fill: slate-100
    └── Border: slate-300, opacity 50%
```

### Step 6.8: Create Toggle Switch

```
Draw rounded rectangle: 48px × 28px
├── Fill: slate-300
├── Radius: full (9999px)

Add circle inside: 24px × 24px
├── Fill: white
├── Position: left side

States:
├── Toggle/Off (shown above)
├── Toggle/On
│   ├── Fill: teal-600
│   └── Circle: positioned right side
├── Toggle/Hover (slight opacity change)
└── Toggle/Disabled
    ├── Fill: slate-200
    ├── Opacity: 50%
    └── Cursor: not-allowed
```

### Step 6.9: Create Select/Dropdown

```
Component like Input, but with:
├── Draw rectangle: 320px × 40px
├── Border: 1px slate-300
├── Radius: md (8px)
├── Fill: white

Add content:
├── Text: "Select option..."
├── Dropdown arrow (Lucide "ChevronDown" icon)
│   ├── Right-aligned
│   ├── Color: slate-600
│   └── Padding: lg (16px) from right

States:
├── Select/Closed (shown)
├── Select/Open
│   ├── Border: 2px teal-500
│   └── Show dropdown menu (frame below)
│       ├── Options listed
│       ├── Each: 320px × 40px
│       ├── Hover: background slate-50
│       └── Selected: background teal-50, text teal-600
├── Select/Focus
│   └── Border: 2px teal-500, focus ring
└── Select/Disabled
    └── Opacity 50%, cursor not-allowed
```

**Save this page.** ✅ Task 6 complete!

---

## 🎯 Task 7: Component States & Finish (2-3 hours)

### Step 7.1: Create Complete State Documentation

**On same page or new page, show:**

```
COMPONENT STATES MATRIX:

Create a table showing all states:

         Default | Hover  | Focus  | Active | Disabled
Button   [show]  [show]  [show]  [show]  [show]
Input    [show]  [show]  [show]  [show]  [show]
Checkbox [show]  [show]  [show]  [show]  [show]
Select   [show]  [show]  [show]  [show]  [show]
Label    [show]  -       -       -       [show]

With annotations for each showing the specific changes.
```

### Step 7.2: Create Accessibility Documentation

**Create page or section showing:**

```
ACCESSIBILITY REQUIREMENTS:

Color Contrast Verification:
├── teal-600 text on white: 8.2:1 ✅ WCAG AAA
├── slate-700 text on slate-50: 11.5:1 ✅ WCAG AAA
├── slate-600 text on slate-50: 4.8:1 ✅ WCAG AA

Focus Ring Specification:
├── Width: 2-3px solid
├── Color: teal-500 (#14B8A6)
├── Offset: 2px from element edge
├── Radius: 4px offset
├── Always visible on keyboard navigation

Touch Target Size:
├── Minimum: 44px × 44px
├── Recommended: 48px × 48px
├── All buttons comply with minimum

Interactive Element Labels:
├── All inputs have <label> with htmlFor
├── All icons have aria-label
├── All buttons have clear text labels
└── No color-only status indicators (icon + text + color)
```

### Step 7.3: Create Quick Reference Card

**On Token pages, add footer with quick links:**

```
QUICK REFERENCE:

Colors: See page 1 🎨
Typography: See page 2 ✍️
Spacing: See page 3 📏
Shadows & Radius: See page 4 ☀️
Components: See page 6 🧩

Questions? See TOKENS-QUICK-REFERENCE.md
```

### Step 7.4: Final QA Checklist

**Before finishing Phase 1, verify:**

- [ ] All color styles created and named correctly
- [ ] All typography styles created and named correctly
- [ ] All components have default + 4 states (hover, focus, active, disabled)
- [ ] Focus rings visible on all interactive elements (3px teal-500)
- [ ] Color contrast meets WCAG AA (4.5:1 minimum)
- [ ] All components have 8px grid alignment
- [ ] Documentation on each token page is clear
- [ ] Naming convention consistent across all components
- [ ] Mobile view tested (responsive preview)

**Save Figma file.** ✅✅✅ Phase 1 Complete!

---

## 📊 Phase 1 Deliverables Checklist

By end of this phase, you should have:

✅ **Figma File Structure**
  - [ ] 18 organized pages
  - [ ] Cover page with navigation
  - [ ] Clear naming conventions

✅ **Color System**
  - [ ] 40+ color swatches
  - [ ] 30+ Figma color styles
  - [ ] WCAG AA verified
  - [ ] Usage documentation

✅ **Typography System**
  - [ ] 8 type sizes defined
  - [ ] 6 font weights defined
  - [ ] 8 Figma text styles created
  - [ ] Readability verified

✅ **Spacing System**
  - [ ] 8px grid system visualized
  - [ ] 9 spacing tokens defined
  - [ ] Responsive guidelines documented
  - [ ] Common patterns shown

✅ **Shadow & Radius**
  - [ ] 5 shadow depths documented
  - [ ] 6 radius options documented
  - [ ] Component combinations shown

✅ **Foundation Components**
  - [ ] Button (5 variants: Primary, Secondary, Danger, Ghost, sizes)
  - [ ] Input (5 states: default, hover, focus, filled, error)
  - [ ] Label (3 states: default, required, disabled)
  - [ ] FormField (4 states: default, focus, error, disabled)
  - [ ] Checkbox (5 states: all interactive states)
  - [ ] Toggle Switch (4 states: off, on, hover, disabled)
  - [ ] Select/Dropdown (5 states: closed, open, focus, error, disabled)

✅ **Documentation**
  - [ ] Usage guidelines for each component
  - [ ] Accessibility checklist
  - [ ] Color usage rules
  - [ ] Responsive guidelines
  - [ ] Quick reference card

---

## 🚀 Next: Phase 2

Once Phase 1 is complete, move to **Phase 2: Components** (Week 2):
- Composite components (Card, Alert, Badge, Modal)
- Layout components (Navigation, Tabs, Pagination)
- Data display (Table, Avatar, Skeleton, Empty State)
- Animation specifications

**Estimated total Phase 1 time:** 5-7 working days  
**Estimated total Design System:** 4-5 weeks

---

**Document Version:** 1.0  
**Created:** March 2026  
**Use with:** DESIGN-SYSTEM-PLAN.md + TOKENS-QUICK-REFERENCE.md
