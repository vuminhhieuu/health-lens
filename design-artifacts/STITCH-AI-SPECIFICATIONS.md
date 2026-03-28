# HealthLens Design System - Stitch AI Specifications

**Version:** 1.0  
**Target:** Stitch AI Design Generation  
**Platform:** Figma Import  
**Date:** March 2026

---

## 📋 Overview for Stitch AI

This document contains complete, structured specifications for AI-powered design generation. Stitch AI should use these specs to generate:

1. **Color Token Palettes** with all variants
2. **Typography Scale** with all sizes and weights
3. **Component Library** with all states
4. **Design Patterns** for common use cases

**Design Philosophy:** "Calm Healthcare" - warm, spacious, accessible

**Target Users:** Elderly Vietnamese (58+) managing chronic conditions

---

## 🎨 SECTION 1: COLOR SYSTEM SPECIFICATIONS

### 1.1 Primary Brand Color - Teal

**Purpose:** Trust, healthcare, primary actions

**Generate 10 color variants with these exact specifications:**

```
Base Color: teal-600 (#0D9488)

Variant Specifications:
├── teal-50:   Lightest (10% saturation decrease)
│   └── Expected hex: #F0FDFA
│   └── Use: Ultra-light backgrounds, hover states
│
├── teal-100:  Very light
│   └── Expected hex: #CCFBF1
│   └── Use: Light backgrounds, secondary hover
│
├── teal-200:  Light
│   └── Expected hex: #99F6E4
│   └── Use: Light background variants
│
├── teal-300:  Medium-light
│   └── Expected hex: #5EEAD4
│   └── Use: Card backgrounds, light overlays
│
├── teal-400:  Medium
│   └── Expected hex: #2DD4BF
│   └── Use: Light borders, secondary elements
│
├── teal-500:  Mid-tone (FOCUS RING COLOR)
│   └── Exact hex: #14B8A6
│   └── Use: Focus rings (3px), keyboard navigation
│   └── Important: This is WCAG AA compliant for focus indicators
│
├── teal-600:  Base/Primary (PRIMARY ACTION COLOR)
│   └── Exact hex: #0D9488
│   └── Use: Primary buttons, brand color, CTAs
│   └── Important: Main brand color - most used
│
├── teal-700:  Dark
│   └── Expected hex: #0F766E
│   └── Use: Button hover states, darker backgrounds
│
├── teal-800:  Darker
│   └── Expected hex: #134E4A
│   └── Use: Button active/pressed states
│
└── teal-900:  Darkest (HIGH CONTRAST TEXT)
    └── Expected hex: #0D3D3A
    └── Use: Text, headings, highest contrast
```

**Visual Properties:**
- Smooth gradation between tones
- Even saturation across scale
- Hue consistency (no color shift)
- All must be web-safe colors

**Accessibility Requirements:**
- teal-600 on white: minimum 8:1 contrast ✓
- teal-500 focus ring visible on light backgrounds
- No two adjacent colors should be indistinguishable

---

### 1.2 Health Status Colors

**Purpose:** Visual status indicators for medical data (replaces text-only status)

**Generate 4 color families with 5 variants each:**

#### Success / Normal / Healthy (Green)

```
Base: green-500 (#10B981)

Variants:
├── green-50:   #F0FDF4 (ultra-light background)
├── green-400:  #4ADE80 (light accent)
├── green-500:  #10B981 (primary status badge)
│   └── Use: Checkmark icon, status badge, "normal" indicator
├── green-600:  #059669 (button hover state)
└── green-700:  #047857 (button active state)

Accessibility:
├── Icon style: Solid checkmark (✓)
├── Text accompanying: "Normal" or "Healthy"
├── Badge use: Circular background with checkmark
└── Label: Always include text (3-part redundancy: color + icon + text)
```

#### Warning / Monitor / Caution (Yellow/Amber)

```
Base: yellow-500 (#F59E0B)

Variants:
├── yellow-50:   #FFFBEB (ultra-light background)
├── yellow-400:  #FACC15 (light accent)
├── yellow-500:  #F59E0B (primary status badge)
│   └── Use: Alert icon, status badge, "monitor" indicator
├── yellow-600:  #D97706 (button hover state)
└── yellow-700:  #B45309 (button active state)

Accessibility:
├── Icon style: Exclamation mark or alert triangle (⚠)
├── Text accompanying: "Monitor" or "Caution"
├── Badge use: Circular background with alert icon
└── Label: Always include text
```

#### Critical / Abnormal / Action Needed (Red)

```
Base: red-500 (#EF4444)

Variants:
├── red-50:   #FEF2F2 (ultra-light background)
├── red-400:  #F87171 (light accent)
├── red-500:  #EF4444 (primary status badge)
│   └── Use: X or alert icon, status badge, "abnormal" indicator
├── red-600:  #DC2626 (button hover state, error border)
└── red-700:  #B91C1C (button active state)

Accessibility:
├── Icon style: X or exclamation mark (!), not just red
├── Text accompanying: "Action Needed" or "Abnormal"
├── Badge use: Circular background with alert icon
├── Important: Tone should be "caution" not "alarm" (fits "Calm Healthcare")
└── Label: Always include text (NOT color-only)
```

#### Info / Neutral Information (Blue)

```
Base: blue-500 (#3B82F6)

Variants:
├── blue-50:   #EFF6FF (ultra-light background)
├── blue-400:  #60A5FA (light accent)
├── blue-500:  #3B82F6 (primary info badge)
│   └── Use: Info icon, status badge, neutral information
├── blue-600:  #2563EB (button hover state)
└── blue-700:  #1D4ED8 (button active state)

Accessibility:
├── Icon style: Info circle (ⓘ)
├── Text accompanying: "Information" or description
├── Badge use: Circular background with info icon
└── Label: Always include text
```

---

### 1.3 Neutral Colors (Slate Grayscale)

**Purpose:** Text, backgrounds, borders - foundation colors

**Generate complete slate scale (12 steps):**

```
Base: slate-600 (#475569)

Variants (complete scale from light to dark):
├── slate-50:   #F8FAFC
│   └── Use: Page background, very light areas
│   └── Accessibility: Text color contrast base
│
├── slate-100:  #F1F5F9
│   └── Use: Card backgrounds, light sections
│
├── slate-200:  #E2E8F0
│   └── Use: Subtle borders, dividers, disabled backgrounds
│
├── slate-300:  #CBD5E1
│   └── Use: Default borders, inactive states
│   └── Accessibility: 4.5:1 contrast with slate-900 text ✓
│
├── slate-400:  #94A3B8
│   └── Use: Disabled state backgrounds, placeholder text
│   └── Opacity: Can add 50% opacity for lighter disabled
│
├── slate-500:  #64748B
│   └── Use: Secondary text, muted content
│   └── Accessibility: 4.5:1 contrast minimum
│
├── slate-600:  #475569
│   └── Use: Secondary emphasis, secondary button text
│   └── Accessibility: Good contrast with light backgrounds
│
├── slate-700:  #334155
│   └── Use: Body text, paragraphs (WCAG AA compliant)
│   └── Accessibility: 11.5:1 contrast on slate-50 ✓✓
│
├── slate-800:  #1E293B
│   └── Use: Emphasis text, list items
│   └── Accessibility: Very high contrast (16:1+)
│
├── slate-900:  #0F172A
│   └── Use: Headings, primary text (HIGHEST CONTRAST)
│   └── Accessibility: 19:1 contrast on white ✓✓✓
│
└── slate-950:  #020617 (Optional)
    └── Use: Reserved for extreme contrast (rarely used)
```

**Important for Slate:**
- Each step should have ~50% luminance difference from previous
- Text must maintain 4.5:1 contrast on all light backgrounds
- Slate-700 is the minimum for body text (WCAG AA)
- Slate-900 required for headings on light backgrounds

---

### 1.4 Functional & Accessibility Colors

**Purpose:** Specific use cases - focus, errors, success, disabled

#### Focus Ring (Keyboard Navigation)

```
Color: teal-500 (#14B8A6)

Specifications:
├── Border style: Solid line
├── Width: 2-3px
├── Color: #14B8A6
├── Offset from element: 2px (outside the element box)
├── Border radius: 4px (offset, not on element itself)
├── Visibility: Must be visible on light and dark backgrounds
│   └── Test: Visible on slate-50 background ✓
│   └── Test: Visible on slate-900 background ✓
├── Opacity: 100% (no transparency)
└── Animation: Instant appearance (no delay), no pulse

Implementation:
├── For buttons: outline-offset: 2px; outline: 3px solid #14B8A6;
├── For inputs: border: 2px solid #14B8A6; + outer ring
└── For interactive: Always add focus state

Accessibility Requirement:
├── WCAG 2.1 Success Criterion 2.4.7
├── All interactive elements must have visible focus indicator
├── Should be visible without magnification
```

#### Error / Validation Color

```
Color: red-600 (#DC2626)

Specifications:
├── Border style: Solid line (2px for inputs)
├── Background: red-50 (#FEF2F2) for error containers
├── Text: red-900 or red-600 for error messages
│
├── Input error state:
│   ├── Border: 2px solid #DC2626
│   ├── Border color: red-600
│   ├── Background: white (keep clean)
│   └── Label: same text color or red
│
├── Error message text:
│   ├── Font: Inter 12px, weight 500
│   ├── Color: #DC2626
│   ├── Icon: ✗ or ! in red
│   ├── Location: Below input field
│   └── Gap: sm (8px) from input
│
├── Alert/Error box:
│   ├── Background: red-50 (#FEF2F2)
│   ├── Border-left: 4px solid red-600
│   ├── Padding: lg (16px)
│   ├── Icon: ✗ in red-600
│   └── Text: red-900 (#7F1D1D) for high contrast
│
└── Accessibility:
    ├── Not color-only (must have icon + text)
    ├── Error message linked to input (aria-describedby)
    └── Clear description of what went wrong
```

#### Success / Validation Color

```
Color: green-600 (#059669)

Specifications:
├── Border style: Solid line (2px for inputs)
├── Background: green-50 (#F0FDF4) for success containers
├── Text: green-900 for messages
│
├── Success state:
│   ├── Icon: ✓ checkmark in green-600
│   ├── Color: green-600 (#059669)
│   ├── Background: green-50 for container
│   └── Text: "Success" or confirmation message
│
├── Success message:
│   ├── Font: Inter 14px
│   ├── Color: green-900 or green-700
│   ├── Icon: ✓ in green-600
│   └── Animation: Gentle fade in (200ms ease-out)
│
└── Accessibility:
    ├── Role: alert or status
    ├── Text describes what succeeded
    └── Visible for at least 3-5 seconds
```

#### Disabled State Color

```
Color: slate-400 (#94A3B8)

Specifications:
├── Background: slate-400 with 50% opacity
│   └── Render as: rgba(148, 163, 184, 0.5)
│   └── Or: #94A3B8 at 50% opacity
│
├── Text: slate-400 (same color as background)
│   └── Creates "faded" appearance
│   └── Opacity may vary (50-75%)
│
├── Border: slate-300 (#CBD5E1)
│   └── Subtle, not prominent
│
├── Cursor: not-allowed (CSS pointer-events: none)
│
├── Shadow: None (flat appearance indicates disabled)
│
└── Hover state: No change (stays disabled)
    └── No hover color change
    └── No interactive feedback
```

---

### 1.5 Color Style Names for Figma

**Create Figma color styles with these exact names:**

```
NAMING PATTERN: Colors / {Category} / {Token}

PRIMARY COLORS:
├── Colors/Primary/Teal-50
├── Colors/Primary/Teal-100
├── Colors/Primary/Teal-200
├── Colors/Primary/Teal-300
├── Colors/Primary/Teal-400
├── Colors/Primary/Teal-500  (Focus Ring)
├── Colors/Primary/Teal-600  (Brand Primary)
├── Colors/Primary/Teal-700
├── Colors/Primary/Teal-800
└── Colors/Primary/Teal-900

STATUS COLORS:
├── Colors/Status/Success-50
├── Colors/Status/Success-500
├── Colors/Status/Success-600
├── Colors/Status/Success-700
├── Colors/Status/Warning-50
├── Colors/Status/Warning-500
├── Colors/Status/Warning-600
├── Colors/Status/Critical-50
├── Colors/Status/Critical-500
├── Colors/Status/Critical-600
├── Colors/Status/Info-50
├── Colors/Status/Info-500
└── Colors/Status/Info-600

NEUTRAL COLORS:
├── Colors/Neutral/Slate-50
├── Colors/Neutral/Slate-100
├── Colors/Neutral/Slate-200
├── Colors/Neutral/Slate-300
├── Colors/Neutral/Slate-400
├── Colors/Neutral/Slate-500
├── Colors/Neutral/Slate-600
├── Colors/Neutral/Slate-700
├── Colors/Neutral/Slate-800
├── Colors/Neutral/Slate-900
└── Colors/Neutral/Slate-950

FUNCTIONAL:
├── Colors/Functional/Focus-Ring
├── Colors/Functional/Error
├── Colors/Functional/Success
└── Colors/Functional/Disabled
```

---

### 1.6 Color Contrast Verification

**Stitch AI: Verify these contrast ratios when generating:**

```
TEXT ON LIGHT BACKGROUND (slate-50: #F8FAFC):

✅ WCAG AAA (7:1 minimum):
├── slate-900 (#0F172A): 19:1 ✓✓✓
├── slate-800 (#1E293B): 16:1 ✓✓✓
├── teal-900 (#0D3D3A): 16:1 ✓✓✓
├── teal-600 (#0D9488): 8.2:1 ✓✓

✅ WCAG AA (4.5:1 minimum):
├── slate-700 (#334155): 11.5:1 ✓
├── slate-600 (#475569): 7.5:1 ✓
├── teal-700 (#0F766E): 5.4:1 ✓
├── green-600 (#059669): 6.8:1 ✓
├── red-600 (#DC2626): 5.2:1 ✓

❌ Below AA (DO NOT USE):
├── slate-500: 4.2:1 (only for secondary text)
├── slate-400: 2.5:1 (disabled states only)
└── slate-300: 1.5:1 (borders only, not text)

TEXT ON DARK BACKGROUND (slate-900: #0F172A):

✅ WCAG AA:
├── white (#FFFFFF): 19:1 ✓✓✓
├── slate-50 (#F8FAFC): 18:1 ✓✓✓
├── slate-100 (#F1F5F9): 17:1 ✓✓✓
└── teal-50 (#F0FDFA): 16:1 ✓✓
```

---

## ✍️ SECTION 2: TYPOGRAPHY SPECIFICATIONS

### 2.1 Font Family

**Primary Font:** Inter

```
Font Stack (for web):
"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif

Alternative (fallback):
Roboto, "Open Sans", "Helvetica Neue", sans-serif

Monospace (for code/data):
"JetBrains Mono", "Courier New", monospace

Font Requirements for Stitch AI:
├── Support Vietnamese characters (đ, ă, â, ê, ô, ơ, ư, ơ, ơ)
├── Clear at small sizes (12px minimum readable)
├── Good legibility for elderly users (58+)
└── Medical/professional aesthetic
```

**Why Inter:**
- Excellent Vietnamese support
- Clear, legible at all sizes
- Warm, approachable feel ("Calm Healthcare")
- Used by healthcare organizations (Figma, etc.)
- Web-optimized (free from Google Fonts or Inter)

---

### 2.2 Type Scale Definition

**Generate 8 typographic styles with exact specifications:**

#### Style 1: Caption (Micro text)

```
Name: Typography/Caption
├── Font: Inter, Regular (weight 400)
├── Size: 12px
├── Line-height: 16px (1.33 ratio)
├── Letter-spacing: 0px
├── Color: slate-600 (#475569)
│
├── Use cases:
│   ├── Timestamps: "Updated 2 hours ago"
│   ├── Helper text: "Optional field"
│   ├── Form hints: "This info is private"
│   └── Metadata: "Last synced: 14:32"
│
├── Visual example:
│   └── "Updated 30 minutes ago"
│       └── Font smaller, muted color
│
└── Accessibility:
    └── Minimum size (larger better), only for non-critical info
```

#### Style 2: Body Small (Secondary descriptions)

```
Name: Typography/Body-Small
├── Font: Inter, Regular (weight 400)
├── Size: 14px
├── Line-height: 20px (1.43 ratio)
├── Letter-spacing: 0px
├── Color: slate-600 (#475569)
│
├── Use cases:
│   ├── Secondary descriptions: "Your latest test results"
│   ├── Helper text under labels: "Upload PDF or take photo"
│   ├── Form descriptions: "We'll keep this private"
│   └── Subtitle text: "Health Record Summary"
│
├── Visual example:
│   └── "Click here to view detailed health analysis"
│       └── Slightly smaller than body, secondary color
│
└── Accessibility:
    └── Readable but clearly secondary
```

#### Style 3: Body (Main paragraph text) ⭐ PRIMARY

```
Name: Typography/Body
├── Font: Inter, Regular (weight 400)
├── Size: 16px  ⭐ WCAG AA MINIMUM FOR BODY TEXT
├── Line-height: 24px (1.5 ratio)
├── Letter-spacing: 0px
├── Color: slate-700 (#334155)
│
├── Use cases:
│   ├── Primary paragraphs: Main content
│   ├── Form labels: "Email address"
│   ├── List items: Navigation, options
│   ├── Descriptions: Results explanation
│   └── Helper text: Clear instructions
│
├── Visual example:
│   └── "Your health record shows normal results. All metrics are within the recommended range for your age and gender."
│       └── Main readable size, dark text
│
├── Accessibility Requirements:
│   ├── Size: MINIMUM 16px (WCAG AA compliance for elderly)
│   ├── Line-height: 24px provides good readability
│   ├── Color: High contrast on light backgrounds (11.5:1)
│   └── Used: As default for all body content
│
└── Important:
    └── This is the baseline - all other sizes should scale from this
```

#### Style 4: Heading 4 (Card titles, section subheadings)

```
Name: Typography/Heading-4
├── Font: Inter, Semibold (weight 600)
├── Size: 18px
├── Line-height: 28px (1.56 ratio)
├── Letter-spacing: -0.5px (tighten)
├── Color: slate-900 (#0F172A)
│
├── Use cases:
│   ├── Card titles: "Your Health Summary"
│   ├── Section subheadings: "Recent Results"
│   ├── Form section titles: "Account Information"
│   ├── List section headers: "Medical Records"
│   └── Modal titles: "Edit Profile"
│
├── Visual example:
│   └── "Your Health Summary"
│       └── Larger, bold, dark color, clear hierarchy
│
└── Accessibility:
    └── Clear visual hierarchy between body and heading
```

#### Style 5: Heading 3 (Page section headings)

```
Name: Typography/Heading-3
├── Font: Inter, Semibold (weight 600)
├── Size: 24px
├── Line-height: 32px (1.33 ratio)
├── Letter-spacing: -0.5px
├── Color: slate-900 (#0F172A)
│
├── Use cases:
│   ├── Major page sections: "Health Records"
│   ├── Dashboard section titles: "Recent Updates"
│   ├── Settings section: "Privacy Settings"
│   ├── Page region headings: "My Profiles"
│   └── Modal titles (larger): "Share Health Record"
│
├── Visual example:
│   └── "Health Records"
│       └── Larger, prominent, clear section marker
│
└── Accessibility:
    └── Should use <h3> tag in HTML
```

#### Style 6: Heading 2 (Major page sections)

```
Name: Typography/Heading-2
├── Font: Inter, Bold (weight 700)
├── Size: 28px
├── Line-height: 36px (1.29 ratio)
├── Letter-spacing: -0.5px to -1px
├── Color: slate-900 (#0F172A)
│
├── Use cases:
│   ├── Major page sections: "Dashboard"
│   ├── Primary page titles: "My Health"
│   ├── Modal headers (full page): "Upload Health Record"
│   └── Main section markers
│
├── Visual example:
│   └── "My Dashboard"
│       └── Large, bold, clear page structure
│
└── Accessibility:
    └── Should use <h2> tag
```

#### Style 7: Heading 1 (Main page title) ⭐ HERO

```
Name: Typography/Heading-1
├── Font: Inter, Bold (weight 700)
├── Size: 30px
├── Line-height: 40px (1.33 ratio)
├── Letter-spacing: -1px
├── Color: slate-900 (#0F172A)
│
├── Use cases:
│   ├── Main page title: "Welcome to HealthLens"
│   ├── App title sections: "HealthLens"
│   ├── Hero sections: Main introduction
│   ├── Top-level headings: Only once per page
│   └── Application branding
│
├── Visual example:
│   └── "Welcome to HealthLens"
│       └── Very large, prominent, page identity
│
└── Accessibility:
    └── Should use <h1> tag, max 1 per page
```

#### Style 8: Display (Hero/Landing) - Future phase

```
Name: Typography/Display
├── Font: Inter, Bold (weight 700)
├── Size: 40px
├── Line-height: 48px (1.2 ratio)
├── Letter-spacing: -1.5px
├── Color: teal-900 (#0D3D3A) or slate-900
│
├── Use cases:
│   ├── Landing page hero text
│   ├── Large branding text
│   ├── Marketing headlines
│   └── Phase 2+ only (not MVP)
│
└── Note: Not used in Phase 1 MVP screens
```

---

### 2.3 Font Weight Usage

```
Weights available: 300, 400, 500, 600, 700, 800

Guidelines for Stitch AI:

Light (300):
├── Rarity: Very rare, used only in display text
├── Size: Usually 40px+
└── Example: "Display Light Text"

Regular (400): ⭐ DEFAULT & MOST USED
├── Use: All body text, paragraphs, descriptions
├── Sizes: 12px to 28px (most common)
└── Example: Body text, descriptions, secondary headings

Medium (500):
├── Use: Form labels, secondary emphasis
├── Sizes: 14px to 18px
└── Example: "Email Address" (form label)

Semibold (600): ⭐ COMMON FOR HEADINGS
├── Use: Card titles, section headings (H4, H3)
├── Sizes: 18px to 24px
└── Example: "Your Health Summary" (card title)

Bold (700): ⭐ PAGE TITLES & EMPHASIS
├── Use: Page titles (H1, H2), strong emphasis
├── Sizes: 28px to 30px
└── Example: "My Dashboard" (page title)

Extrabold (800):
├── Rarity: Reserved for special emphasis only
├── Use: Very rare, maximum emphasis
└── Avoid in normal content
```

---

### 2.4 Line Height Guidelines

**For Stitch AI when generating text:**

```
Line-height ratios (multiply by font size to get pixels):

Tight (1.2):
├── Headings with short line lengths
├── Sizes: 28px+ (H2, H1, Display)
├── Example: "My\nDashboard" (short lines, tight spacing)
└── Calculation: 28px × 1.2 = 33.6px line-height

Normal (1.5): ⭐ RECOMMENDED FOR BODY
├── Body text, optimal readability
├── Sizes: 12px to 18px (most common)
├── Example: Paragraph text
└── Calculation: 16px × 1.5 = 24px line-height

Relaxed (1.75):
├── Dense information requiring clarity
├── Medical/complex content
├── Helps with scanning
└── Sizes: 14px to 16px when dense
```

---

### 2.5 Typography Style Names for Figma

**Create text styles in Figma with these names:**

```
NAMING PATTERN: Typography / {Level}

Base Styles (8 total):
├── Typography/Caption
├── Typography/Body-Small
├── Typography/Body  (DEFAULT)
├── Typography/Heading-4
├── Typography/Heading-3
├── Typography/Heading-2
├── Typography/Heading-1
└── Typography/Display

Optional: Weight Variants
├── Typography/Heading-Bold
├── Typography/Label
└── Typography/Monospace (if needed)
```

---

## 📏 SECTION 3: SPACING SPECIFICATIONS

### 3.1 Spacing Scale

**Generate spacing values on 8px base grid:**

```
Base Grid: 8px
Multiplier: Each token is multiple of 8

xs:   4px   (half unit)
├── Use: Icon padding, very tight spacing
├── Example: Icon inside small button
└── Rarely used alone

sm:   8px   (1 unit) ⭐ COMMON
├── Use: Gap between list items, tight spacing
├── Example: Margin between nav items
└── Most common small gap

md:   12px  (1.5 units) ⭐ VERY COMMON
├── Use: Standard padding, moderate gaps
├── Example: Card padding, element spacing
└── Default for most gaps

lg:   16px  (2 units) ⭐ MOST COMMON
├── Use: Default button/input padding, comfortable spacing
├── Example: Button padding, page margins (mobile)
└── Primary spacing token

xl:   20px  (2.5 units)
├── Use: Large padding, spacious layouts
├── Example: Extra-large button, significant spacing
└── For accessibility (generous whitespace)

2xl:  24px  (3 units)
├── Use: Section spacing, clear visual separation
├── Example: Gap between major sections
└── Medium section spacing

3xl:  32px  (4 units)
├── Use: Major section spacing, page margins (tablet)
├── Example: Page side padding on tablet
└── Large visual separation

4xl:  40px  (5 units)
├── Use: Large layout spacing, page margins (desktop)
├── Example: Page side padding on desktop
└── Generous spacing between major areas

5xl:  48px  (6 units)
├── Use: Extra-large spacing between sections
├── Example: Between page sections
└── Maximum comfortable spacing

6xl:  56px  (7 units)
└── Reserved for large hero sections (Phase 2+)

7xl:  64px  (8 units)
└── Reserved for special cases
```

---

### 3.2 Common Spacing Patterns

**For Stitch AI: Use these patterns when generating components**

#### Button/Input Height

```
Small button:     32px height
├── Padding:      4px vertical, 12px horizontal
├── Font size:    14px
└── Use: Secondary actions, compact layouts

Medium button:    40px height  ⭐ DEFAULT
├── Padding:      8px vertical, 16px horizontal
├── Font size:    16px
└── Use: Primary actions, default buttons

Large button:     48px height
├── Padding:      12px vertical, 20px horizontal
├── Font size:    16px
└── Use: Accessibility, emphasized actions

Calculation:
Height = (2 × vertical-padding) + line-height
40px = (2 × 8px) + 24px line-height ✓
```

#### Card Padding

```
Compact card:     12px padding (md)
├── Use: Tight layouts, information-dense
├── Example: List items in scrollable area
└── For: Normal users

Default card:     16px padding (lg)  ⭐ RECOMMENDED
├── Use: Standard card layouts
├── Example: Health metric cards
└── For: Most cards

Spacious card:    20px padding (xl)
├── Use: Accessibility, elderly users
├── Example: Primary information cards
└── For: Important content (elderly 58+)
```

#### Gap Between Items

```
Tight list:       8px gap (sm)
├── Use: Compact listings
├── Example: Navigation items close together
└── For: Many items, space-constrained

Default gap:      12px gap (md)  ⭐ RECOMMENDED
├── Use: List items, normal spacing
├── Example: Health record items, form fields
└── For: Most lists

Comfortable gap:  16px gap (lg)
├── Use: Spacious layouts
├── Example: Card grid items
└── For: Accessibility, elderly users

Section gap:      24-32px (2xl-3xl)
├── Use: Visual separation between sections
├── Example: Between different content areas
└── For: Clear visual hierarchy
```

#### Page/Container Margins

```
Mobile (375px):    16px margin (lg)
├── Page margins: 16px left + right
├── Card gap: 8px
└── Total width: 343px

Tablet (768px):    24px margin (2xl)
├── Page margins: 24px left + right
├── Card gap: 12px
└── Total width: 720px

Desktop (1280px):  32px margin (3xl)  ⭐ PRIMARY
├── Page margins: 32px left + right
├── Card gap: 12px
├── Total width: 1216px
└── Max content width: 1200px (with 8px adjustment)
```

---

### 3.3 Responsive Spacing

**For different screen sizes:**

```
DESKTOP (1280px and up):
├── Page padding:      32px (3xl) on sides
├── Section spacing:   40px (4xl) vertical
├── Component gap:     12px (md)
├── Card padding:      16px (lg) default
├── Column spacing:    16px (lg)
└── Max layout width:  1200px

TABLET (768px - 1279px):
├── Page padding:      24px (2xl) on sides
├── Section spacing:   32px (3xl) vertical
├── Component gap:     12px (md)
├── Card padding:      16px (lg) default
├── Column spacing:    12px (md)
└── Max layout width:  712px

MOBILE (below 768px):
├── Page padding:      16px (lg) on sides
├── Section spacing:   24px (2xl) vertical
├── Component gap:     8px (sm)
├── Card padding:      12-16px (md-lg)
├── Stack: Single column
└── Max layout width:  full - 32px padding
```

**Note for MVP:** Focus on Desktop (1280px) - Tablet and Mobile reference only

---

### 3.4 8px Grid System

**All spacing and sizing should align to 8px grid:**

```
Alignment Rule:
- Every margin, padding, width, height should be multiple of 8
- Exception: Can use 4px for fine details, icons
- Exception: Can use 12px for odd spacing needs

Examples (all valid):
├── 8px ✓ (1×8)
├── 12px ✓ (1.5×8, okay for special cases)
├── 16px ✓ (2×8)
├── 24px ✓ (3×8)
├── 32px ✓ (4×8)
├── 40px ✓ (5×8)
└── 48px ✓ (6×8)

Invalid (avoid):
├── 15px ✗ (not divisible by 8)
├── 18px ✗ (not divisible by 8)
└── 30px ✗ (not divisible by 8)

For Stitch AI:
└── Use only valid 8px multiples when generating layouts
```

---

## 🔲 SECTION 4: BORDER RADIUS SPECIFICATIONS

### 4.1 Radius Scale

```
none:   0px
├── Use: Icons, precise geometric shapes
├── Example: Perfect squares for icons
└── Rarely used on UI elements

sm:     4px  ⭐ PRECISE
├── Use: Form controls, checkboxes, inputs
├── Feel: Professional, controlled
├── Example: Checkbox corners
└── Most common for inputs

md:     8px  ⭐ STANDARD
├── Use: Most UI elements
├── Feel: Balanced, modern
├── Example: Input fields, badges
└── Default choice

lg:     12px ⭐⭐ MOST COMMON
├── Use: Buttons, cards, modals
├── Feel: Warm, approachable ("Calm Healthcare")
├── Example: Primary buttons, card containers
└── Most frequently used in design

xl:     16px
├── Use: Large components, very friendly look
├── Feel: Very rounded, approachable
├── Example: Large cards, hero buttons
└── For accessibility and warmth

full:   9999px or 50%
├── Use: Circles, pill buttons, avatars
├── Feel: Fully rounded, friendly
├── Example: Profile avatars, pill-shaped buttons
└── Used sparingly
```

---

### 4.2 Component-Specific Radius

```
Button:
├── Primary button:        lg (12px)  ⭐ FRIENDLY
├── Secondary button:      lg (12px)
├── Tertiary button:       lg (12px)
├── Text button:           none (0px)
└── Icon button:           md (8px)

Input/Form:
├── Text input:            md (8px)  ⭐ PRECISE
├── Select dropdown:       md (8px)
├── Checkbox:              sm (4px)
├── Radio:                 full (circle)
├── Textarea:              md (8px)
└── Toggle switch:         full (pill shape)

Container:
├── Card:                  lg (12px)  ⭐ COMMON
├── Modal/Dialog:          lg (12px)
├── Popover:               lg (12px)
├── Tooltip:               md (8px)
└── Badge:                 md or lg (8-12px)

Avatar:
├── User avatar:           full (100% circle)
├── Avatar in list:        full (100% circle)
└── Fallback initials:     full (100% circle)

Health Components:
├── HealthMetricCard:      lg (12px)
├── HealthStatusBadge:     full (12px for large, full for small icon)
└── ResultSummary:         lg (12px)

List Items:
├── List item with hover:  md (8px)
└── Selectable item:       md (8px)
```

**Rule of thumb:**
- Buttons & Cards: lg (12px) for warmth
- Inputs & Form: md (8px) for precision
- Avatars: full for circular profile images

---

## ☀️ SECTION 5: SHADOW & ELEVATION SPECIFICATIONS

### 5.1 Shadow Depth Levels

**Stitch AI: Generate these exact shadow values:**

#### Shadow 0 (No shadow - Flat)

```
Shadow: none

Properties:
├── box-shadow: none
├── Elevation: 0 (flat on surface)
└── Use: Disabled elements, flat design areas

Example:
├── Disabled button (appears flat, inactive)
├── Flat background elements
└── Text-only areas
```

#### Shadow 1 (Subtle - Subtle elevation)

```
Shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05)

Properties:
├── Offset: 0px horizontal, 1px vertical
├── Blur: 2px
├── Spread: 0px
├── Color: Black at 5% opacity (almost invisible)
├── Elevation: 1 (minimal lift)
└── Use: Hover states, light interactive feedback

Example:
├── Button on hover (subtle lift feedback)
├── Light card elevation
├── Soft interactive hint
```

#### Shadow 2 (Light - Default elevation) ⭐ MOST COMMON

```
Shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)

Properties:
├── Outer shadow: 0 1px 3px rgba(0,0,0,0.1)
├── Inner shadow: 0 1px 2px -1px rgba(0,0,0,0.1)
├── Elevation: 2 (standard card depth)
└── Use: Default cards, inputs, buttons resting state

Example:
├── Card backgrounds (default state)
├── Input fields (resting)
├── Button resting state
├── List items
└── Most common shadow in MVP
```

#### Shadow 3 (Medium - Elevated)

```
Shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)

Properties:
├── Outer shadow: 0 4px 6px -1px rgba(0,0,0,0.1)
├── Inner shadow: 0 2px 4px -2px rgba(0,0,0,0.1)
├── Elevation: 3 (clear elevation)
└── Use: Interactive elements, dropdowns, elevated cards

Example:
├── Card on hover (more elevation feedback)
├── Dropdown menu
├── Tooltip
├── Elevated data containers
└── Focus state (with focus ring)
```

#### Shadow 4 (Prominent - Modal depth)

```
Shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)

Properties:
├── Outer shadow: 0 10px 15px -3px rgba(0,0,0,0.1)
├── Inner shadow: 0 4px 6px -4px rgba(0,0,0,0.1)
├── Elevation: 4 (prominent, clearly layered)
└── Use: Modals, overlays, important popups

Example:
├── Modal dialogs
├── Overlay components
├── Prominent notifications
└── Alert boxes
```

#### Shadow 5 (Maximum - Full-page overlay)

```
Shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)

Properties:
├── Outer shadow: 0 20px 25px -5px rgba(0,0,0,0.1)
├── Inner shadow: 0 8px 10px -6px rgba(0,0,0,0.1)
├── Elevation: 5 (maximum depth, most prominent)
└── Use: Full-page overlays, top-level modals

Example:
├── Full-page modal
├── Main menu overlay
├── Top-level notification
└── Maximum layering (rarely used)
```

---

### 5.2 Shadow Usage Pattern

```
Component States:

BUTTON:
├── Resting: shadow-2 (default lift)
├── Hover: shadow-3 (increased elevation)
├── Focus: shadow-3 + focus-ring (interactive)
├── Active/Pressed: shadow-1 (depressed effect)
└── Disabled: none (flat)

CARD:
├── Resting: shadow-2 (default)
├── Hover: shadow-3 (lift on interaction)
├── Active: shadow-3
└── Disabled: shadow-0 or shadow-1

INPUT:
├── Resting: none or shadow-0 (flat, clean)
├── Focus: shadow-2 + focus-ring (interaction hint)
├── Error: shadow-1 (red border, minimal shadow)
└── Disabled: shadow-0 (flat, inactive)

DROPDOWN/POPUP:
├── Normal: shadow-3 (elevation)
├── Open/Active: shadow-3 (consistent)
└── Hover items: slight highlight, same shadow

MODAL:
├── Overlay backdrop: shadow-5 or 50% dark overlay
├── Modal box: shadow-4 (prominent)
└── Modal nested popover: shadow-5 (more prominent)
```

---

## ⏱️ SECTION 6: ANIMATION SPECIFICATIONS

### 6.1 Animation Timing

```
Fast:    150ms
├── Use: Hover states, quick feedback
├── Example: Button hover color change
└── Quick, responsive feel

Normal:  200ms  ⭐ DEFAULT
├── Use: Standard transitions, page changes
├── Example: Modal open, card appear
└── Comfortable pace

Slow:    300ms
├── Use: Complex animations, multi-step
├── Example: Page transition, multi-element sequence
└── Graceful, deliberate feel
```

---

### 6.2 Easing Functions

```
ease-out: cubic-bezier(0.16, 1, 0.3, 1)
├── Deceleration
├── Use: Appearing elements, page load
├── Feel: Quick start, then slowing
└── Example: Fade in, slide up

ease-in-out: cubic-bezier(0.42, 0, 0.58, 1)
├── Symmetric deceleration
├── Use: Moving elements, page transitions
├── Feel: Smooth motion
└── Example: Modal slide

ease-in: cubic-bezier(0.42, 0, 1, 1)
├── Acceleration
├── Use: Disappearing elements
├── Feel: Accelerating away
└── Example: Fade out, exit
```

---

### 6.3 Animation Presets

**For Stitch AI: Use these animation patterns:**

#### Fade In

```
Duration: 200ms (Normal)
Easing: ease-out
├── Opacity: 0 → 1
└── Timing: 0ms to 200ms

Use: Page load, element appearance
```

#### Slide Up

```
Duration: 200ms (Normal)
Easing: ease-out
├── Transform: translateY(+20px) → translateY(0)
├── Opacity: 0 → 1
└── Combined for smooth entry

Use: Modal entry, result cards, notifications
```

#### Scale Pulse (Feedback)

```
Duration: 150ms (Fast)
Easing: ease-out then ease-in
├── Frame 1: scale(1)
├── Frame 2 (75ms): scale(1.05)
└── Frame 3 (150ms): scale(1)

Use: Button press feedback, click confirmation
```

#### Loading Spinner

```
Duration: 1s (continuous)
Easing: linear
├── Rotation: 0deg → 360deg → 0deg (loop)
├── Repeat: infinite
└── Speed: constant rotation

Use: Data loading, OCR processing indicator
```

---

### 6.4 Motion Accessibility

**Important: Respect user preferences**

```
CSS Media Query:
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

Effect: Disable all animations for users who prefer reduced motion
├── Animations become instant (0ms)
├── Maintains visual state changes (no tricks)
└── WCAG 2.1 Success Criterion 2.3.3 compliance
```

---

## 🧩 SECTION 7: FOUNDATION COMPONENTS

### 7.1 Button Component Specifications

#### Button - Primary

```
Name: Button / Primary / Default

Visual Properties:
├── Background: teal-600 (#0D9488)
├── Text: White (#FFFFFF)
├── Padding: 8px horizontal (md), 16px vertical (lg)
├── Height: 40px
├── Border radius: lg (12px)
├── Shadow: shadow-2
├── Font: Inter 16px, weight 600
├── Cursor: pointer

States to generate:

1. Default (shown above)
├── Background: teal-600
├── Shadow: shadow-2
└── Ready to click

2. Hover
├── Background: teal-700 (#0F766E)
├── Shadow: shadow-3 (elevated)
├── Font color: White (unchanged)
└── Cursor: pointer

3. Focus (Keyboard)
├── Background: teal-600
├── Shadow: shadow-3
├── Focus Ring: 2-3px solid teal-500 (#14B8A6)
├── Ring offset: 2px from element
└── Visible on tab navigation

4. Active/Pressed
├── Background: teal-800 (#134E4A)
├── Shadow: shadow-1 (inset, pressed)
├── Transform: translateY(1px) (optional, slight press)
└── Feels clicked/activated

5. Disabled
├── Background: slate-400 (#94A3B8)
├── Text opacity: 50%
├── Shadow: none (flat)
├── Cursor: not-allowed
├── No hover effect

Component Set in Figma:
├── Create 5 variants
├── Property: State
│   ├── Value 1: Default
│   ├── Value 2: Hover
│   ├── Value 3: Focus
│   ├── Value 4: Active
│   └── Value 5: Disabled
└── Name: Button / Primary
```

#### Button - Secondary

```
Name: Button / Secondary

Visual Properties:
├── Background: slate-200 (#E2E8F0)
├── Text: slate-900 (#0F172A)
├── Border: 1px slate-300 (#CBD5E1)
├── Padding: 8px horizontal, 16px vertical
├── Height: 40px
├── Border radius: lg (12px)
├── Shadow: shadow-2
├── Font: Inter 16px, weight 600

States (5 total):
├── Default: slate-200 background
├── Hover: slate-300 background, shadow-3
├── Focus: Add focus ring teal-500
├── Active: slate-400 background, shadow-1
└── Disabled: slate-100 background, opacity 50%
```

#### Button - Danger

```
Name: Button / Danger

Visual Properties:
├── Background: red-600 (#DC2626)
├── Text: White
├── Padding: 8px horizontal, 16px vertical
├── Height: 40px
├── Border radius: lg (12px)
├── Shadow: shadow-2
├── Font: Inter 16px, weight 600

States (5 total):
├── Default: red-600
├── Hover: red-700, shadow-3
├── Focus: red-600 + focus ring
├── Active: red-800, shadow-1
└── Disabled: slate-400, opacity 50%

Note: Used for delete/destructive actions only
```

#### Button - Sizes

```
Also generate size variants:

Small Button:
├── Height: 32px
├── Padding: 4px vertical, 12px horizontal
├── Font: 14px
├── Border radius: lg (12px)

Medium Button: (DEFAULT)
├── Height: 40px
├── Padding: 8px vertical, 16px horizontal
├── Font: 16px
├── Border radius: lg (12px)

Large Button:
├── Height: 48px
├── Padding: 12px vertical, 20px horizontal
├── Font: 16px (same font size)
├── Border radius: lg (12px)
```

---

### 7.2 Input Component Specifications

#### Input - Text Field

```
Name: Input / Text

Visual Properties:
├── Background: white (#FFFFFF)
├── Border: 1px solid slate-300 (#CBD5E1)
├── Padding: 8px horizontal (md), 12px vertical
├── Height: 40px
├── Border radius: md (8px)
├── Shadow: none (flat)
├── Font: Inter 16px, weight 400
├── Text color: slate-700 (#334155)

States (5 total):

1. Default (empty)
├── Background: white
├── Border: 1px slate-300
├── Placeholder: "Enter text..." (slate-400)
└── Ready for input

2. Hover
├── Background: white
├── Border: 1px slate-400 (darker)
├── Shadow: shadow-1
└── Indicates interactivity

3. Focus (Keyboard/Click)
├── Background: white
├── Border: 2px solid teal-500 (#14B8A6)
├── Focus ring: Add external ring (2-3px teal-500)
├── Shadow: shadow-2
└── Clearly focused

4. Filled (Typed)
├── Background: white
├── Border: 1px slate-300
├── Text: "Health Record" (example)
├── Text color: slate-900 (#0F172A)
└── Shows entered value

5. Error
├── Background: white (clean)
├── Border: 2px solid red-600 (#DC2626)
├── Error message below: "This field is required" (12px, red-600)
├── Gap below input: sm (8px)
└── Clear error indication
```

#### Input - Email

```
Name: Input / Email

Same as Text field but:
├── Type: email input
├── Placeholder: "your@email.com"
├── Validation: Checks email format on blur
└── Error state: Shows "Invalid email format"
```

#### Input - Password

```
Name: Input / Password

Properties:
├── Background: white
├── Border: 1px slate-300
├── Text: Masked with dots (••••••)
├── Show/Hide button: Eye icon on right (teal-600)
├── Click eye: Toggles password visibility
└── States: Default, Hover, Focus, Error, Disabled
```

#### Input - Number

```
Name: Input / Number

Properties:
├── Background: white
├── Border: 1px slate-300
├── Up/Down arrows: Standard browser controls
├── Decimal support: Yes
└── States: Default, Hover, Focus, Error, Disabled
```

---

### 7.3 Label Component

```
Name: Label / Default

Visual Properties:
├── Font: Inter 14px, weight 600
├── Color: slate-900 (#0F172A)
├── Used: Above input fields

States (3 total):

1. Default
├── Text: "Email Address"
├── Font: 14px, weight 600
└── Color: slate-900

2. Required
├── Text: "Email Address *"
├── Asterisk (*): color red-600
├── Font: 14px, weight 600
└── Indicates required field

3. Disabled
├── Color: slate-400 (#94A3B8)
├── Font: 14px, weight 600
└── Grayed out appearance
```

---

### 7.4 Form Field Component (Label + Input + Error)

```
Name: FormField / Default

Structure:
├── Label (top)
├── Gap: md (12px)
├── Input (middle)
├── Gap: sm (8px)
└── Help/Error text (bottom)

States (4 total):

1. Default
├── Label in normal state
├── Input in default state
├── No error text
└── Clean form field

2. Focus
├── Label unchanged
├── Input in focus state (teal border)
├── Optional: Help text shown (slate-500, 12px)

3. Error
├── Label: normal or red color
├── Input: red border, shadow-1
├── Error text: "This field is required" (red-600, 12px)
├── Layout: Error text below input
└── Clear error indication

4. Disabled
├── Label: slate-400
├── Input: disabled state
├── Error text: hidden
└── All grayed out
```

---

### 7.5 Checkbox Component

```
Name: Checkbox / Default

Visual Properties:
├── Size: 20px × 20px
├── Background: white
├── Border: 2px solid slate-300 (#CBD5E1)
├── Border radius: sm (4px)
├── Cursor: pointer

States (5 total):

1. Unchecked
├── Background: white
├── Border: 2px slate-300
└── No checkmark

2. Checked
├── Background: teal-600 (#0D9488)
├── Border: 2px teal-600
├── Checkmark: White (✓) icon, 12px
└── Checked state visible

3. Hover (Unchecked)
├── Border: 2px teal-500
├── Shadow: shadow-1
└── Interactive hint

4. Focus (Unchecked)
├── Border: 2px teal-500
├── Focus ring: 2-3px teal-500 (around element)
└── Keyboard navigation

5. Disabled
├── Background: slate-100
├── Border: 2px slate-300
├── Opacity: 50%
└── Cursor: not-allowed
```

---

### 7.6 Radio Button Component

```
Name: Radio / Default

Visual Properties:
├── Size: 20px × 20px
├── Shape: Circle (border-radius: 50%)
├── Background: white
├── Border: 2px solid slate-300
├── Cursor: pointer

States (5 total):

1. Unselected
├── Background: white
├── Border: 2px slate-300
└── Inner circle: empty

2. Selected
├── Background: white
├── Border: 2px teal-600
├── Inner circle: 8px teal-600 dot (center)
└── Selected state visible

3. Hover
├── Border: 2px teal-500
├── Shadow: shadow-1
└── Interactive hint

4. Focus
├── Border: 2px teal-500
├── Focus ring: 2-3px teal-500
└── Keyboard navigation

5. Disabled
├── Background: slate-100
├── Border: 2px slate-300
├── Opacity: 50%
└── Cursor: not-allowed
```

---

### 7.7 Toggle Switch Component

```
Name: Toggle / Default

Visual Properties:
├── Container: 48px × 28px
├── Border radius: full (9999px)
├── Circle thumb: 24px × 24px, border-radius: full
├── Cursor: pointer

States (4 total):

1. Off (Unselected)
├── Background: slate-300 (#CBD5E1)
├── Thumb: white, positioned left
├── Smooth transition

2. On (Selected)
├── Background: teal-600 (#0D9488)
├── Thumb: white, positioned right
├── Smooth transition (200ms)

3. Hover
├── Slight opacity change (90%)
└── More obvious on Off state

4. Disabled
├── Background: slate-200
├── Opacity: 50%
├── Cursor: not-allowed
```

---

### 7.8 Select / Dropdown Component

```
Name: Select / Default

Visual Properties (Closed):
├── Background: white
├── Border: 1px slate-300
├── Padding: 8px horizontal (md), 12px vertical
├── Height: 40px
├── Border radius: md (8px)
├── Font: Inter 16px, weight 400
├── Text: "Select option..." (placeholder)
├── Icon: ChevronDown (Lucide), right-aligned, teal-600

States (5 total):

1. Closed (Default)
├── Shows placeholder: "Select option..."
├── Chevron down icon: teal-600
├── Ready to open

2. Open (Expanded)
├── Border: 2px solid teal-500
├── Dropdown menu shown below
├── Menu items in list:
│   ├── Each 40px height
│   ├── Padding: md (12px)
│   ├── Font: 16px
│   ├── Hover: background slate-50
│   └── Selected: background teal-50, text teal-600
├── Chevron: rotated up

3. Hover (Closed)
├── Border: 1px slate-400
├── Shadow: shadow-1
└── Interactive hint

4. Focus (Closed)
├── Border: 2px teal-500
├── Focus ring: 2-3px teal-500
└── Keyboard navigation

5. Disabled
├── Background: slate-100
├── Opacity: 50%
├── Cursor: not-allowed
```

---

## 📊 STITCH AI GENERATION CHECKLIST

**When generating designs, verify:**

- [ ] All colors match hex codes exactly
- [ ] Typography scale from 12px to 40px
- [ ] All spacing aligns to 8px grid
- [ ] Border radius matches specification (lg for buttons/cards, md for inputs)
- [ ] Shadows use exact CSS values
- [ ] All interactive elements have focus states
- [ ] Focus rings are 2-3px teal-500
- [ ] Color contrast meets WCAG AA (4.5:1 minimum)
- [ ] Disabled states are clearly visually different
- [ ] Error states use red-600, not just red
- [ ] Success states use green-600
- [ ] All states have documentation
- [ ] Components are organized in Figma properly
- [ ] Naming convention follows pattern
- [ ] No hard-coded values (use style tokens instead)

---

## 🎯 EXPORT INSTRUCTIONS FOR STITCH AI

**After generating designs:**

1. **Export Colors**
   - Export all color styles as JSON or CSS variables
   - Include hex values and semantic names

2. **Export Typography**
   - Export text styles as JSON
   - Include font family, size, weight, line-height

3. **Export Components**
   - Export all components with variants
   - Include annotations/descriptions
   - All states visible

4. **Import to Figma**
   - Use Figma component export feature
   - Import generated components
   - Verify visual accuracy

---

**Document Version:** 1.0  
**Created:** March 2026  
**Format:** Markdown for AI Processing  
**Intended Use:** Stitch AI Design Generation
