# HealthLens Design Tokens - Quick Reference

Dùng file này khi design trên Figma để nhanh chóng tìm hex codes và giá trị tokens.

## 🎨 Color Quick Reference

### Primary Brand (Teal)
```
teal-600: #0D9488  ← PRIMARY USE (buttons, brand)
teal-500: #14B8A6  ← Hover state, focus ring
teal-700: #0F766E  ← Darker backgrounds
```

### Health Status Colors
```
✅ Success (Normal):  #10B981 (green-500)
⚠️  Warning (Monitor): #F59E0B (yellow-500)
🚨 Critical (Alert):  #EF4444 (red-500)
ℹ️  Info (Neutral):   #3B82F6 (blue-500)
```

### Text Colors (Accessibility)
```
Heading (darkest):     #0F172A (slate-900)
Body text (dark):      #334155 (slate-700)
Secondary text:        #475569 (slate-600)
Helper/disabled:       #94A3B8 (slate-400)
```

### Backgrounds
```
Page background:      #F8FAFC (slate-50)
Card background:      #F1F5F9 (slate-100)
Border (subtle):      #E2E8F0 (slate-200)
Border (default):     #CBD5E1 (slate-300)
```

### Focus & Accessibility
```
Focus ring: #14B8A6 (teal-500) - 2-3px solid
Error/validation: #DC2626 (red-600)
Disabled: #94A3B8 (slate-400) with 50% opacity
```

---

## ✍️ Typography Quick Reference

### Type Sizes (Web, all weights: 400 Regular default)
```
Caption:     12px / 16px (1.33)
Body Small:  14px / 20px (1.43)
Body:        16px / 24px (1.5)   ← WCAG AA minimum for body
H4 heading:  18px / 28px (1.56), weight: 600
H3 heading:  24px / 32px (1.33), weight: 600
H2 heading:  28px / 36px (1.29), weight: 700
H1 heading:  30px / 40px (1.33), weight: 700
```

### Font Stack
```
Primary: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
Mono:    "JetBrains Mono", "Courier New", monospace
```

### Font Weights
```
400 = Regular (body text)
500 = Medium (labels)
600 = Semibold (card titles, subheadings)
700 = Bold (page titles, emphasis)
```

---

## 📏 Spacing Quick Reference (8px base grid)

```
xs:  4px
sm:  8px   ← Gap between list items
md:  12px  ← Standard padding
lg:  16px  ← Default button/input padding
xl:  20px  ← Large padding
2xl: 24px  ← Section spacing
3xl: 32px  ← Major section spacing
4xl: 40px  ← Large layout spacing
```

### Common Uses
```
Button height:      40px (padding: lg)
Input height:       40px (padding: md lg)
Card padding:       lg (16px)
Gap between cards:  md (12px)
Page margins:       2xl-3xl (24-32px)
Section spacing:    3xl-4xl (32-40px)
```

---

## 🔲 Border Radius Quick Reference

```
none: 0px
sm:   4px   ← Tight, precise
md:   8px   ← Standard (inputs)
lg:   12px  ← Friendly (buttons, cards)
xl:   16px  ← Very rounded
full: 9999px ← Circles, pills
```

### Component Defaults
```
Button:      lg (12px)
Input:       md (8px)
Card:        lg (12px)
Modal:       lg (12px)
Avatar:      full (circles)
Badge:       md (8px)
```

---

## ☀️ Shadow Quick Reference

```
shadow-1: 0 1px 2px 0 rgba(0,0,0,0.05)              ← subtle hover
shadow-2: 0 1px 3px 0 rgba(0,0,0,0.1), 0 1px 2px    ← default cards
shadow-3: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px ← elevated
shadow-4: 0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px ← modal
shadow-5: 0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px ← overlay
```

### Usage
```
Resting cards:   shadow-2
Hover state:     shadow-3
Modals/overlays: shadow-4 or shadow-5
Disabled:        no shadow (flat)
```

---

## ⏱️ Animation Quick Reference

```
Duration:
  Fast:   150ms (hover, quick feedback)
  Normal: 200ms (standard transitions)
  Slow:   300ms (complex animations)

Easing:
  ease-out:    cubic-bezier(0.16, 1, 0.3, 1)       ← appearing
  ease-in-out: cubic-bezier(0.42, 0, 0.58, 1)     ← moving
  ease-in:     cubic-bezier(0.42, 0, 1, 1)        ← disappearing
```

### Common Animations
```
Fade in:      opacity 0→1, duration normal, ease-out
Slide up:     transform translateY(20px)→0, duration normal
Scale pulse:  transform scale(1)→scale(1.05)→1, duration fast
Loading spin: rotate 360°, duration 1s, linear
```

---

## 🎯 Accessibility Checklist (Quick)

✅ **Color Contrast:**
- Text on background: 4.5:1 ratio minimum
- Test: teal-600 #0D9488 on slate-50 #F8FAFC ✓ PASS
- Test: slate-700 #334155 on slate-50 #F8FAFC ✓ PASS

✅ **Focus States:**
- Ring: 2-3px solid teal-500
- Visible on all interactive elements
- Keyboard accessible

✅ **Touch Targets:**
- Minimum 44px × 44px
- Recommended 48px × 48px

✅ **Text Size:**
- Body text minimum 16px
- Heading hierarchy clear

✅ **Motion:**
- Respect prefers-reduced-motion
- No flashing/strobing

---

## 🧩 Component Size Defaults

```
Button sizes:
  Small:  32px height
  Medium: 40px height (DEFAULT)
  Large:  48px height

Input/Select:
  Height: 40px

Card content area:
  Padding: 16px (lg)

Modal/Dialog:
  Width: 480px (desktop)
  Padding: 24px (2xl)

Avatar:
  Small: 32px
  Medium: 40px (DEFAULT)
  Large: 48px
```

---

## 📐 Responsive Breakpoints

```
Desktop:  1280px and above
Tablet:   768px - 1279px
Mobile:   Below 768px (reference only for this phase)

Margins by breakpoint:
  Desktop: 32px (3xl)
  Tablet:  24px (2xl)
  Mobile:  16px (lg)

Column counts:
  Desktop: 3 columns max
  Tablet:  2 columns
  Mobile:  1 column (reference)
```

---

## 🚀 Quick Start Commands

### In Figma:
1. Create color styles with tokens (right-click color → Create style)
2. Name: `Colors/Primary/Teal-600` (follow pattern)
3. Create text styles for typography
4. Use naming: `Typography/Body` or `Typography/Heading-1`
5. Use component sets for variants

### Naming Pattern in Figma:
- `Colors/[Semantic]/[Token]`
- `Typography/[Level]`
- `Spacing/[Size]`
- `Components/[Category]/[Name]`
- `Screens/[Feature]/[Screen Name]`

---

**Version:** 1.0  
**Last Updated:** March 2026  
**Use with:** DESIGN-SYSTEM-PLAN.md
