# HealthLens Design System Plan

**Status:** 📋 Planning Phase  
**Target Platform:** Web Dashboard (Next.js + Shadcn/ui)  
**Scope:** Complete Phase 1 MVP Design System  
**Updated:** March 2026

---

## 🎯 Strategic Vision

**Design Philosophy:** "Calm Healthcare" - Warm, spacious, friendly interface that builds trust for vulnerable users (58+ years old managing chronic conditions)

**Core Design Principles:**
- Clarity over complexity - Medical data must be immediately understandable
- Accessibility first - WCAG 2.1 AA compliance is non-negotiable
- Generous whitespace - Reduce cognitive load for elderly users
- Progressive disclosure - Hide complexity until needed
- Warm color palette - Teal (trust) + Green (health) + thoughtful reds (caution, not alarm)

---

## 📁 Figma File Structure

### Recommended Figma Organization

```
HealthLens Design System (Main File)
├── 0. Cover Page
│   └── Design System Overview & Version History
│
├── 1. TOKENS
│   ├── Colors
│   ├── Typography
│   ├── Spacing & Sizing
│   ├── Shadows & Elevation
│   ├── Border Radius
│   └── Animation Tokens
│
├── 2. COMPONENTS - FOUNDATION
│   ├── Button (Primary, Secondary, Tertiary, Danger)
│   ├── Input (Text, Email, Password, Number)
│   ├── Select & Dropdown
│   ├── Checkbox & Radio
│   ├── Toggle Switch
│   └── Textarea
│
├── 3. COMPONENTS - COMPOSITE
│   ├── Form Field (Label + Input + Error State)
│   ├── Card (Base, Interactive, Elevated)
│   ├── Alert (Info, Warning, Error, Success)
│   ├── Badge (Status, Category)
│   ├── Chip (Removable tag)
│   └── Dialog / Modal
│
├── 4. COMPONENTS - LAYOUT
│   ├── Sidebar Navigation
│   ├── Top Navigation Bar
│   ├── Breadcrumb
│   ├── Tabs
│   ├── Pagination
│   └── Stepper/Progress
│
├── 5. COMPONENTS - DATA DISPLAY
│   ├── Table
│   ├── List Item
│   ├── Avatar & AvatarGroup
│   ├── Skeleton Loader
│   ├── Empty State
│   └── Error State
│
├── 6. COMPONENTS - CUSTOM (HEALTH)
│   ├── HealthStatusBadge
│   ├── HealthMetricCard
│   ├── HealthResultSummary
│   ├── HealthMetricsGrid
│   ├── ProfileCard
│   ├── FamilyMemberCard
│   ├── ReferenceRangeIndicator
│   └── HealthStatusTimeline
│
├── 7. PATTERNS & STATES
│   ├── Loading States
│   ├── Error Handling Patterns
│   ├── Empty States
│   ├── Success Confirmations
│   ├── Form Patterns
│   ├── Validation Patterns
│   └── State Combinations (Dark, Disabled, Hover, Focus)
│
├── 8. RESPONSIVE BREAKPOINTS
│   ├── Desktop (1280px)
│   ├── Tablet (768px)
│   └── Mobile (375px - reference only for awareness)
│
├── 9. ACCESSIBILITY GUIDE
│   ├── Color Contrast Checker
│   ├── Focus States Documentation
│   ├── ARIA Annotation Guide
│   └── Touch Target Sizing
│
└── 10. SCREENS (Web Dashboard)
    ├── Authentication
    │   ├── Login
    │   ├── Register
    │   └── Password Reset
    ├── User Dashboard
    │   ├── Home/Overview
    │   ├── Health Records List
    │   └── Health Record Detail
    ├── Profile Management
    │   ├── My Profiles
    │   ├── Create Profile
    │   └── Edit Profile
    ├── Family Sharing
    │   ├── Shared Profiles
    │   └── Share Management
    ├── Upload & Process
    │   ├── Upload Page
    │   └── Processing Status
    ├── Admin Dashboard
    │   ├── Analytics Overview
    │   ├── Reference Data List
    │   ├── Reference Data Editor
    │   ├── Audit Logs
    │   └── User Management
    └── Settings
        ├── Account Settings
        ├── Privacy & Data
        └── Notifications
```

---

## 🎨 Design Tokens Complete Specification

### 1. COLOR TOKENS

#### Primary Brand Colors
```
Teal (Primary/Trust)
├── teal-50:   #F0FDFA (Lightest background)
├── teal-100:  #CCFBF1
├── teal-200:  #99F6E4
├── teal-300:  #5EEAD4
├── teal-400:  #2DD4BF
├── teal-500:  #14B8A6 (Primary button background)
├── teal-600:  #0D9488 (PRIMARY - use for main brand)
├── teal-700:  #0F766E
├── teal-800:  #134E4A
└── teal-900:  #0D3D3A (Darkest - for text)
```

#### Semantic Health Status Colors
```
Success (Healthy/Normal)
├── green-50:  #F0FDF4
├── green-500: #10B981 (Badge/Icon)
└── green-600: #059669 (Hover state)

Warning (Slightly Abnormal/Monitor)
├── yellow-50: #FFFBEB
├── yellow-500: #F59E0B (Badge/Icon)
└── yellow-600: #D97706 (Hover state)

Critical/Alert (Abnormal/Action needed)
├── red-50:    #FEF2F2
├── red-500:   #EF4444 (Badge/Icon)
└── red-600:   #DC2626 (Hover state)

Info (Neutral information)
├── blue-50:   #EFF6FF
├── blue-500:  #3B82F6 (Badge/Icon)
└── blue-600:  #2563EB (Hover state)
```

#### Neutral Colors (Text, Backgrounds, Borders)
```
Slate Grayscale (Accessibility-optimized)
├── slate-50:   #F8FAFC (Background)
├── slate-100:  #F1F5F9 (Card background)
├── slate-200:  #E2E8F0 (Border - subtle)
├── slate-300:  #CBD5E1 (Border - default)
├── slate-400:  #94A3B8 (Border - strong)
├── slate-500:  #64748B (Text - secondary)
├── slate-600:  #475569 (Text - secondary emphasis)
├── slate-700:  #334155 (Text - body)
├── slate-800:  #1E293B (Text - heading)
├── slate-900:  #0F172A (Text - highest contrast)
└── slate-950:  #020617 (Reserved for extreme contrast)
```

#### Functional Colors
```
Disabled/Inactive
└── slate-400: #94A3B8

Focus Ring (Accessibility)
└── teal-500: #14B8A6 (3px width for WCAG AA)

Success Feedback
└── green-600: #059669

Error/Validation
└── red-600: #DC2626
```

#### Color Usage Rules
- **Primary Actions:** teal-600 (#0D9488)
- **Secondary Actions:** slate-200 border with slate-700 text
- **Danger Actions:** red-600 (#DC2626)
- **Disabled State:** slate-400 (#94A3B8) with opacity 50%
- **Text on Health Status:** Always use 3-part redundancy (color + icon + text) for accessibility
- **Links:** teal-600 (#0D9488) - underlined by default, remove underline on hover (web convention)
- **Focus Ring:** 2-3px solid teal-500 (#14B8A6)

---

### 2. TYPOGRAPHY TOKENS

#### Font Families
```
Primary Font Stack: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
Fallback: Roboto (for systems without Inter)
Monospace: JetBrains Mono, "Courier New", monospace (for data/codes)

Why Inter?
- Excellent Vietnamese character support
- Highly legible at small sizes (critical for 16px+ requirement)
- Good readability for elderly users
- Clean, medical aesthetic matching "Calm Healthcare"
```

#### Type Scale (8px Base)
```
Caption (Micro text - use sparingly)
├── font-size: 12px
├── line-height: 16px (1.33)
├── letter-spacing: 0px
├── font-weight: 400
└── Use for: Timestamps, helper text, small labels

Body (Paragraph/Secondary)
├── font-size: 14px
├── line-height: 20px (1.43)
├── letter-spacing: 0px
├── font-weight: 400
└── Use for: Secondary text, descriptions, form helper text

Body Default (Main paragraph - ACCESSIBILITY MIN 16px on mobile)
├── font-size: 16px (WCAG AA minimum for body)
├── line-height: 24px (1.5)
├── letter-spacing: 0px
├── font-weight: 400
└── Use for: Primary body text, descriptions, form labels, navigation

Heading 4 (Section subheading)
├── font-size: 18px
├── line-height: 28px (1.56)
├── letter-spacing: -0.5px
├── font-weight: 600
└── Use for: Card titles, section headings

Heading 3 (Subsection heading)
├── font-size: 24px
├── line-height: 32px (1.33)
├── letter-spacing: -0.5px
├── font-weight: 600
└── Use for: Page subsection headings, major section titles

Heading 2 (Page section)
├── font-size: 28px
├── line-height: 36px (1.29)
├── letter-spacing: -0.5px
├── font-weight: 700
└── Use for: Major page section heading

Heading 1 (Page title)
├── font-size: 30px
├── line-height: 40px (1.33)
├── letter-spacing: -1px
├── font-weight: 700
└── Use for: Main page title, hero section

Display Large (Hero/Marketing)
├── font-size: 40px
├── line-height: 48px (1.2)
├── letter-spacing: -1.5px
├── font-weight: 700
└── Use for: App title, main hero text (not in MVP)
```

#### Font Weight Usage
```
Light (300)      - Reserved for very large display text
Regular (400)    - Body text, paragraphs, descriptions
Medium (500)     - Form labels, secondary headings
Semibold (600)   - Card titles, section headings (H4-H3)
Bold (700)       - Page titles (H2-H1), emphasis
Extrabold (800)  - Reserved for special emphasis (minimal use)
```

#### Line Height Ratios (for consistency)
```
Tight:   1.2  (Headings - reduce visual spacing)
Normal:  1.5  (Body text - optimal readability)
Relaxed: 1.75 (Dense information - aids scanning)
```

#### Letter Spacing Rules
```
Positive letter-spacing:
├── For headings with tight line height: -0.5px to -1.5px (tighten)
├── For all caps or emphasis: +0.5px to +1px (loosen)

Negative letter-spacing:
├── Large display text (40px+): -1.5px
├── Medium headings (28-30px): -0.5px to -1px
└── Small text (12-14px): 0px (no adjustment)
```

---

### 3. SPACING TOKENS (8px Base Grid)

#### Spacing Scale
```
xs:  4px   (Micro spacing: icon padding, tight lists)
sm:  8px   (Default element padding, small gaps)
md:  12px  (Standard padding, moderate gaps)
lg:  16px  (Common padding, readable gaps)
xl:  20px  (Large padding, significant gaps)
2xl: 24px  (Extra large padding, spacious layouts)
3xl: 32px  (Section spacing, clear separation)
4xl: 40px  (Large section spacing)
5xl: 48px  (Major layout spacing)
6xl: 56px  (Hero/page top spacing)
7xl: 64px  (Maximum consistent spacing)
```

#### Common Spacing Combinations
```
Button/Input (Height)
├── Small:  32px height (padding: sm md)
├── Medium: 40px height (padding: md lg) [DEFAULT]
└── Large:  48px height (padding: lg xl)

Card Padding
├── Compact:   md (12px)
├── Default:   lg (16px) [RECOMMENDED]
└── Spacious:  xl (20px) [for elderly users - accessibility]

Gap Patterns
├── Items in row:     sm (8px) [tight list]
├── Elements spacing: md (12px) [comfortable]
├── Section spacing:  2xl (24px) [clear separation]
└── Page sections:    3xl (32px) or 4xl (40px) [breathing room]
```

#### Responsive Spacing
```
Desktop (1280px):
├── Page padding: 3xl (32px)
├── Card gap: md (12px)
└── Section gap: 4xl (40px)

Tablet (768px):
├── Page padding: 2xl (24px)
├── Card gap: sm (8px)
└── Section gap: 3xl (32px)

Mobile (375px) - Reference only:
├── Page padding: lg (16px)
├── Card gap: sm (8px)
└── Section gap: 2xl (24px)
```

---

### 4. BORDER RADIUS TOKENS

#### Radius Scale (Rounded corners for approachability)
```
None:   0px    (Reserved for icons, strict geometric shapes)
sm:     4px    (Tight rounding on small elements)
md:     8px    (Standard rounding, default)
lg:     12px   (Rounded appearance, friendly)
xl:     16px   (Very rounded, emphasizes friendly nature)
full:   9999px (Fully rounded - pills, circles)
```

#### Usage Guidelines
```
Component-specific Radius:
├── Button:           lg (12px) [friendly, warm]
├── Input/Form field: md (8px) [standard, professional]
├── Card:             lg (12px) [warm, inviting]
├── Avatar:           full (9999px) [standard profile circles]
├── Badge:            sm or md (4-8px) [tag appearance]
├── Modal:            lg (12px) [consistent with cards]
├── Toast/Notification: lg (12px) [warm, friendly]
└── Checkbox/Radio:   sm (4px) [precise, controls]
```

---

### 5. SHADOW & ELEVATION TOKENS

#### Shadow Depth (Accessibility: visible but not overwhelming)
```
Shadow 0 (None)
└── No shadow (flat components)

Shadow 1 (Subtle - Subtle elevation, hover state)
├── box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05)
└── Use: Interactive elements on hover, light cards

Shadow 2 (Light elevation - Slightly raised)
├── box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)
└── Use: Default cards, inputs, buttons (resting state)

Shadow 3 (Medium elevation - Clear separation)
├── box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1)
└── Use: Elevated cards, dropdown menus, tooltips

Shadow 4 (Prominent elevation - Strong emphasis)
├── box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)
└── Use: Modals, overlays, prominent alerts

Shadow 5 (Maximum elevation - Most prominent)
├── box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)
└── Use: Full-page overlays, top-level modals
```

#### Elevation Usage Pattern
```
Resting State:    shadow-2 (default cards, inputs)
Hover State:      shadow-3 (interactive feedback)
Active/Focus:     shadow-3 + focus-ring (keyboard accessibility)
Overlay:          shadow-4 or shadow-5 (prominence)
Disabled State:   none (flat appearance = disabled)
```

---

### 6. ANIMATION TOKENS

#### Duration Scale (Respect prefers-reduced-motion)
```
Fast:    150ms (Micro interactions - hover, state changes)
Normal:  200ms (Standard transitions - page changes, modals)
Slow:    300ms (Complex animations - multi-step sequences)
```

#### Easing Functions
```
ease-out:      cubic-bezier(0.16, 1, 0.3, 1)
               Use: Initial interactions, appearing elements

ease-in-out:   cubic-bezier(0.42, 0, 0.58, 1)
               Use: Continuous transitions, modal movements

ease-in:       cubic-bezier(0.42, 0, 1, 1)
               Use: Disappearing elements, focus shifts
```

#### Animation Presets
```
Fade In:
├── Opacity: 0 → 1
├── Duration: Normal (200ms)
├── Easing: ease-out
└── Use: Page load, card appearance

Slide Up:
├── Transform: translateY(+20px) → translateY(0)
├── Opacity: 0 → 1
├── Duration: Normal (200ms)
├── Easing: ease-out
└── Use: Modal entry, result cards

Slide Down:
├── Transform: translateY(-20px) → translateY(0)
├── Duration: Normal (200ms)
├── Easing: ease-out
└── Use: Dropdown appearance, menu drop

Scale Pulse:
├── Transform: scale(1) → scale(1.05) → scale(1)
├── Duration: Fast (150ms)
├── Easing: ease-out, ease-in
└── Use: Button press feedback, notification

Loading Spinner:
├── Animation: spin (360 rotation)
├── Duration: 1s (continuous)
├── Easing: linear
└── Use: Data loading, OCR processing
```

#### Motion Accessibility
```
@media (prefers-reduced-motion: reduce)
├── Disable all animations
├── Keep transitions instant (0ms)
├── Preserve visual state changes (no opacity tricks)
└── Example CSS:
    * { animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important; }
```

---

## 🧩 Component Architecture

### Foundation Components (Basic building blocks)

| Component | Variants | Key Props | Accessibility Notes |
|-----------|----------|-----------|---------------------|
| **Button** | Primary, Secondary, Tertiary, Danger, Ghost | size (sm/md/lg), disabled, loading | Focus ring, aria-pressed, keyboard activation |
| **Input** | Text, Email, Password, Number | placeholder, error, disabled, readonly | Label linked via htmlFor, aria-invalid |
| **Select** | Dropdown, Multi-select | options, value, disabled | ARIA listbox, keyboard navigation |
| **Checkbox** | Default, Checked, Indeterminate | checked, disabled, error | aria-checked, focus ring |
| **Radio** | Default, Checked | checked, name, value, disabled | aria-checked, fieldset grouping |
| **Textarea** | Resizable | rows, placeholder, disabled | Linked label, resize constraints |
| **Toggle** | On/Off | checked, disabled, aria-label | Clear focus state, keyboard toggle |

### Composite Components (Assembled from foundation)

| Component | Elements | Use Case | Notes |
|-----------|----------|----------|-------|
| **Form Field** | Label + Input + Error/Help | All form inputs | Automatic spacing, error styling |
| **Card** | Container, Header, Content, Footer | Content containers | Elevation shadow-2, padding lg |
| **Alert** | Icon + Text + Action | Important information | Info/Warning/Error/Success variants |
| **Badge** | Icon + Text + Optional close | Labels, pills | Removable variant for filters |
| **Chip** | Avatar + Text + Remove button | Tags, selectable items | Keyboard dismissal support |
| **Dialog/Modal** | Overlay + Header + Content + Footer | Important actions | Focus trap, esc to close |
| **Breadcrumb** | List of links | Navigation history | ARIA current-page |
| **Tabs** | Tab list + Tab panels | Content switching | Keyboard arrow navigation |

### Layout Components (Structure & Navigation)

| Component | Structure | Breakpoints | Notes |
|-----------|-----------|------------|-------|
| **Sidebar Nav** | Vertical menu, collapsible | Desktop: 250px, Tablet: collapsed | Active state indicator |
| **Top Nav** | Horizontal header, logo, actions | All sizes, responsive menu | Sticky on scroll |
| **Pagination** | Prev/Next + page numbers | Shows per desktop design | Accessible page buttons |
| **Stepper** | Step indicators + content | Full width responsive | Current/completed states |

### Data Display Components

| Component | Display Pattern | Accessibility | Performance |
|-----------|-----------------|-----------------|-------------|
| **Table** | Grid + header + rows | ARIA table, keyboard nav | Sortable columns |
| **List Item** | Flex container + icon/avatar + text | List semantics | Reusable row component |
| **Avatar** | Image + fallback initial | aria-label, alt text | Auto-generated fallback |
| **Skeleton** | Placeholder shimmer | aria-hidden, aria-busy | Matches content shape |
| **Empty State** | Icon + heading + description + CTA | Informative, not error | Clear next actions |

### Custom Health Components (Domain-specific)

| Component | Purpose | Accessibility | Data Structure |
|-----------|---------|-----------------|----------------|
| **HealthStatusBadge** | Display health metric status | Triple redundancy: color + icon + text | status: 'normal'/'warning'/'critical', label string |
| **HealthMetricCard** | Show single metric with explanation | Expandable region, focus management | metric: {name, value, unit, status, range}, explanation string |
| **HealthResultSummary** | Quick overview of test results | Landmark region, heading hierarchy | results: [{metric, status}], timestamp, testType |
| **HealthMetricsGrid** | Grid layout for multiple metrics | List semantics, keyboard nav | metrics: HealthMetricCard[] |
| **ProfileCard** | User/profile preview | Image alt text, name focus | profile: {name, avatar, lastUpdate, status} |
| **FamilyMemberCard** | Family member in dashboard | Relationship context clear | member: {name, avatar, lastResult, status} |
| **ReferenceRangeIndicator** | Visual comparison to normal range | Clear min/max labels | value, min, max, units, status |
| **HealthStatusTimeline** | Historical health tracking | Timeline semantics, date order | events: [{date, status, metrics}] |

---

## 📋 Implementation Roadmap

### Phase 1: Foundation (Week 1)
**Goal:** Core tokens and basic components ready for screens

1. **Set up Figma file structure**
   - [ ] Create main Design System file
   - [ ] Create Tokens pages
   - [ ] Set up naming conventions

2. **Design & Document Color Tokens**
   - [ ] Create color palette page
   - [ ] Generate all color variants
   - [ ] Test WCAG AA contrast on text combinations
   - [ ] Document semantic color meanings
   - [ ] Create color usage guide

3. **Design & Document Typography**
   - [ ] Set up font files (Inter)
   - [ ] Create type scale page
   - [ ] Generate all size/weight combinations
   - [ ] Test readability at each size
   - [ ] Document typography rules

4. **Design & Document Spacing**
   - [ ] Create spacing scale visualization
   - [ ] Document grid system (8px base)
   - [ ] Create responsive breakpoint guides
   - [ ] Document margin/padding patterns

5. **Basic Foundation Components**
   - [ ] Button (4 variants)
   - [ ] Input field (text, email, password)
   - [ ] Label & form field combination
   - [ ] All states: default, hover, focus, active, disabled

### Phase 2: Components (Week 2)
**Goal:** All foundation and composite components ready

6. **Add Form Components**
   - [ ] Select/Dropdown
   - [ ] Checkbox (all states)
   - [ ] Radio button (all states)
   - [ ] Toggle switch
   - [ ] Textarea
   - [ ] Error state variations

7. **Add Composite Components**
   - [ ] Card (resting + elevated states)
   - [ ] Alert (4 types: info, warning, error, success)
   - [ ] Badge & Chip (removable)
   - [ ] Dialog/Modal
   - [ ] Tooltip
   - [ ] Toast/Notification

8. **Add Layout Components**
   - [ ] Breadcrumb
   - [ ] Tabs
   - [ ] Pagination
   - [ ] Sidebar Navigation
   - [ ] Top Navigation

9. **Add Data Display**
   - [ ] Avatar (image + initials fallback)
   - [ ] Avatar Group
   - [ ] List Item
   - [ ] Table (basic structure)
   - [ ] Empty State
   - [ ] Error State
   - [ ] Skeleton/Loading

10. **Add Shadow & Animation**
    - [ ] Apply shadow tokens to components
    - [ ] Document animation tokens
    - [ ] Create loading spinner component
    - [ ] Add micro-interactions (hover, focus feedback)

### Phase 3: Custom Health Components (Week 3)
**Goal:** Domain-specific components ready for health dashboard

11. **Create Health Status Components**
    - [ ] HealthStatusBadge (with color + icon + text)
    - [ ] HealthStatusTimeline
    - [ ] ReferenceRangeIndicator (visual comparison)

12. **Create Health Data Components**
    - [ ] HealthMetricCard (expandable with explanation)
    - [ ] HealthMetricsGrid (responsive grid)
    - [ ] HealthResultSummary (quick overview)

13. **Create Entity Components**
    - [ ] ProfileCard
    - [ ] FamilyMemberCard

14. **Create Patterns & States**
    - [ ] Loading patterns
    - [ ] Error patterns
    - [ ] Empty state (no results)
    - [ ] Success confirmations
    - [ ] Form validation patterns

### Phase 4: Screen Designs (Week 4)
**Goal:** All MVP screens designed and tested

15. **Authentication Screens**
    - [ ] Login page
    - [ ] Register page
    - [ ] Password reset flow
    - [ ] Email verification

16. **Core User Workflows**
    - [ ] Dashboard home
    - [ ] Health records list
    - [ ] Health record detail view
    - [ ] Upload health record
    - [ ] Processing/OCR status screen

17. **Profile Management**
    - [ ] My Profiles list
    - [ ] Create Profile
    - [ ] Edit Profile
    - [ ] Profile settings

18. **Family Sharing**
    - [ ] Shared Profiles view
    - [ ] Share Management
    - [ ] Family Dashboard

19. **Admin Screens**
    - [ ] Admin dashboard
    - [ ] Reference data list
    - [ ] Reference data editor
    - [ ] Audit logs
    - [ ] Analytics overview

20. **Settings & Secondary Screens**
    - [ ] Account settings
    - [ ] Privacy & data settings
    - [ ] Notifications settings

### Phase 5: QA & Refinement (Week 5)
**Goal:** Design System complete and ready for handoff

21. **Design QA**
    - [ ] Verify all components match tokens
    - [ ] Check accessibility (WCAG AA)
    - [ ] Test responsive layouts on all breakpoints
    - [ ] Verify keyboard navigation paths
    - [ ] Test focus ring visibility

22. **Component Documentation**
    - [ ] Document usage guidelines for each component
    - [ ] Create examples of right/wrong usage
    - [ ] Document state combinations
    - [ ] Create component variations reference

23. **Handoff Preparation**
    - [ ] Export all component specs
    - [ ] Create developer notes (annotations)
    - [ ] Document design-to-code specifications
    - [ ] Create CSS-in-JS variable mappings

---

## 🎯 Best Practices & Guidelines

### Naming Conventions

**Color Token Naming:**
```
{semantic-meaning}-{lightness-level}
Examples:
  primary-600 (brand primary)
  success-500 (health check mark)
  warning-400 (caution indicator)
  slate-700 (body text)
  teal-50 (light background)
```

**Component Naming:**
```
{ComponentName}
Base:        Button, Input, Card, etc.
Variants:    Button[Primary], Button[Danger]
States:      Button[Hover], Button[Focus], Button[Disabled]
Size:        Button[lg], Button[sm]
Custom:      HealthStatusBadge, ReferenceRangeIndicator
```

**File & Page Naming in Figma:**
```
Pages: [Priority]-[Category]-[Name]
  1. TOKENS-Colors
  2. COMPONENTS-Foundation-Buttons
  3. COMPONENTS-Composite-Cards
  4. SCREENS-Authentication-Login
  5. PATTERNS-ErrorHandling

Frames within pages:
  [Component Name] / [Variant] / [State]
  Example: Button / Primary / Hover
```

### Consistency Checklist

- [ ] All colors use defined tokens (no hex values)
- [ ] All text uses defined typography scale
- [ ] All spacing uses 8px grid
- [ ] All corner radius matches radius tokens
- [ ] All shadows use elevation tokens
- [ ] All interactive elements have focus ring (teal-500)
- [ ] All states documented: default, hover, focus, active, disabled
- [ ] Color contrast meets WCAG AA (4.5:1 for text)
- [ ] Touch targets minimum 44px × 44px
- [ ] Form labels clearly associated with inputs
- [ ] Error messages helpful and specific
- [ ] Loading states visible and informative
- [ ] Empty states direct users to next action

### Accessibility QA Checklist

- [ ] Color is not the only differentiator (text + icon + label)
- [ ] Focus ring clearly visible on all interactive elements
- [ ] Keyboard navigation order logical (left→right, top→bottom)
- [ ] Screen reader can access all meaningful content
- [ ] No flashing/strobing (< 3 times per second)
- [ ] Animated content respects prefers-reduced-motion
- [ ] Links underlined (default web convention)
- [ ] Form inputs have associated labels
- [ ] Error messages linked to form fields
- [ ] Help text associated with inputs (aria-describedby)
- [ ] Modal traps focus and has clear close button
- [ ] Sufficient white space for cognitive load (elderly users)

---

## 📊 Success Metrics

By end of Phase 5, Design System should have:

- ✅ **10+ token categories** complete and documented
- ✅ **45+ components** designed and specified
- ✅ **100% WCAG AA compliance** on color contrast
- ✅ **Responsive designs** for 3 breakpoints (Desktop, Tablet, Mobile reference)
- ✅ **20+ screens** designed for MVP phase
- ✅ **Zero ambiguity** - developers can build from design without questions
- ✅ **Reusable patterns** documented and exemplified
- ✅ **Consistent aesthetics** across all components and screens

---

## 📚 Reference Documents

**Related Documents in Project:**
- `/docs/project/ux-design-specification.md` - Complete UX strategy
- `/docs/project/architecture.md` - Tech stack and implementation approach
- `/docs/project/prd.md` - Product requirements and user journeys
- `/docs/project/epics.md` - Implementation breakdown

**External Standards:**
- WCAG 2.1 AA - Accessibility guidelines
- Figma Design System Best Practices - Component organization
- Material Design 3 - Elevation and motion principles
- Shadcn/ui - Web component patterns

---

## ✅ Next Steps

1. **Create Figma Project**: Set up free Figma workspace and create main Design System file
2. **Follow Roadmap**: Execute phases 1-5 in sequence, checking off tasks
3. **Share Feedback**: Use Figma comments for design team collaboration
4. **Iterate**: Design is never truly "done" - collect feedback and refine

**Estimated Timeline:** 4-5 weeks for full Design System + Screens
**Team Size:** 1-2 designers recommended (can be concurrent)

---

**Design System Version:** 1.0 (Initial)  
**Last Updated:** March 2026  
**Next Review:** After Phase 1 completion (Week 1)
