# HealthLens Design System - Master Execution Guide
# Hướng dẫn Hoàn thành Toàn bộ Design System (Phase 2 → 4)

**Phiên bản:** 1.0  
**Ngôn ngữ:** Tiếng Việt + English  
**Tác giả:** OpenCode AI Assistant  
**Ngày:** March 2026

---

## 📌 TỔNG QUAN

Bạn đã hoàn thành **Phase 1: Foundation** (Tokens + 7 Foundation Components).

Bây giờ bạn có ba prompt sẵn sàng để tiếp tục:
- **Phase 2:** Composite + Layout + Data Display Components (17 components, 24+ variants)
- **Phase 3:** Custom Health-Specific Components (8 components)
- **Phase 4:** Web + Mobile Screens (20 screens: 11 web + 9 mobile)

**Tổng thời gian:** ~12-17 giờ (chia nhỏ thành 3 phase có thể thực hiện từng ngày)

---

## 🎯 EXECUTION ROADMAP

### Phase 2 Execution (3-4 giờ)

**Step 1: Copy Prompt (5 phút)**
```
File: PHASE-2-STITCH-PROMPT-VI.md
Location: /home/vmhieu/Workspace/UIT/health-lens/design-artifacts/
```
- Mở file
- Copy toàn bộ prompt (từ START đến END)

**Step 2: Stitch AI Generation (10-20 phút)**
1. Vào https://www.stitchdesign.ai/
2. Click "Create New Project" → "Generate with AI"
3. Paste prompt đã copy
4. Configure:
   - Framework: Figma component
   - Generate variants: Yes
   - All states: Yes
5. Click "Generate" → Chờ

**Step 3: Import to Figma (30 phút)**
1. Stitch AI → "Export to Figma" hoặc "Open in Figma"
2. Components auto-import vào file "HealthLens - Design System"
3. Organize vào Pages 7, 8, 9

**Step 4: QA & Verification (45 phút)**
Checklist:
- [ ] All 17 components present
- [ ] Each has 3-5 states (Default, Hover, Focus, Active, Disabled)
- [ ] Colors use Phase 1 styles (not hard-coded)
- [ ] Typography correct
- [ ] Spacing 8px aligned
- [ ] Focus rings visible
- [ ] Component naming consistent

**Step 5: Documentation & Save (15 phút)**
- Add usage notes to each page
- Create summary document (optional)
- Save Figma file
- Share link with team (if needed)

**Total Phase 2: ~3-4 giờ**

---

### Phase 3 Execution (2-3 giờ)

**Same workflow as Phase 2:**

**File:** PHASE-3-STITCH-PROMPT-VI.md

**Components to Generate:**
1. Health Status Badge (4 types: Normal, Monitor, Concern, Critical)
2. Health Metric Card (4 metric types)
3. Health Result Summary (3 test types)
4. Profile Card (3 user types)
5. Family Member List Item
6. Reference Range Indicator (3 range types)
7. Health Status Timeline (4 event types)
8. Quick Action Card (6 action types)

**Organization:**
- Page 10: COMPONENTS - Health (all 8 components)

**Key Design Considerations:**
- 3-part redundancy: Color + Icon + Text (for health status)
- Large text: Min 14px labels, min 16px values (elderly users)
- Color-blind friendly design
- Clear status indicators

**Total Phase 3: ~2-3 giờ (after Phase 2 complete)**

---

### Phase 4 Execution (4-6 giờ)

**Largest phase - Multiple screens**

**File:** PHASE-4-STITCH-PROMPT-VI.md

**Screens to Design:**

**Web Dashboard (11 screens):**
1. Login Page
2. Register Page
3. Password Reset Page
4. Home/Dashboard Page
5. My Profile Page
6. Family Sharing Page
7. Health Results List Page
8. Health Result Detail Page
9. Upload Results Page
10. Settings Page (General/Notifications/Security/Privacy)
11. Breadcrumb navigation (if separate screen)

**Mobile App (9 screens):**
1. Mobile Login
2. Mobile Register
3. Mobile Home
4. Mobile Profile
5. Mobile Family
6. Mobile Results List
7. Mobile Result Detail
8. Mobile Upload
9. Mobile Navigation/Menu

**Organization:**
- Pages 12-18: SCREENS - Web (11 screens)
- Pages 19-20: SCREENS - Mobile (9 screens)

**Responsive Design:**
- Desktop: 1280px+ with sidebar (250px)
- Tablet: 768-1279px with collapsed sidebar (60px icons)
- Mobile: 360px width (iPhone 12 size)

**Key Points:**
- Use ALL Phase 1, 2, 3 components
- No new components - only composition
- Realistic content (Tiếng Việt throughout)
- All form states shown
- Focus states on interactive elements
- Responsive variants

**Total Phase 4: ~4-6 giờ (after Phase 3 complete)**

---

## 📋 DETAILED CHECKLIST

### Before Starting Phase 2
- [ ] Phase 1 completely done (tokens + foundation components in Figma)
- [ ] Access to Stitch AI (account created, logged in)
- [ ] Phase-2-STITCH-PROMPT-VI.md file available
- [ ] Figma file "HealthLens - Design System" open and ready
- [ ] Time available: ~3-4 hours uninterrupted

### During Phase 2
- [ ] Copy prompt from file
- [ ] Paste into Stitch AI
- [ ] Configure settings correctly
- [ ] Start generation
- [ ] Monitor progress
- [ ] Download/export when complete
- [ ] Import to Figma
- [ ] Organize into Pages 7, 8, 9
- [ ] Run QA checklist (see below)

### QA Checklist for Phase 2
**Composite Components:**
- [ ] Card: 4 states (Resting, Hover, Focus, Selected)
- [ ] Alert: 4 types × 2 states (Default, Hover/Closed)
- [ ] Badge: 4 semantic types + removable variants
- [ ] Chip: 4 states (Unselected, Selected, Hover, Removable)
- [ ] Modal: 3 types × 2 states (Open, Loading/Closed)
- [ ] Tooltip: 4 positions (Top, Bottom, Left, Right)
- [ ] Toast: 3 types × 3 states (Default, Hover, Dismissing)

**Layout Components:**
- [ ] Sidebar Nav: Expanded (250px) + Collapsed (60px)
- [ ] Top Nav: Full layout + responsive variants
- [ ] Breadcrumb: Default + with icons
- [ ] Tabs: Default + multiple active states
- [ ] Pagination: First page, middle page, last page, with ellipsis

**Data Display Components:**
- [ ] Table: Basic layout + striped variant
- [ ] Avatar: 4 sizes (S, M, L, XL)
- [ ] Skeleton: Line, Circle, Card, Table Row
- [ ] Empty State: 4 types (No records, No results, First use, Error)
- [ ] Error State: Form + Page + Network errors

**Quality Checks:**
- [ ] All colors use Phase 1 styles (no hex codes)
- [ ] All typography uses Phase 1 text styles
- [ ] Spacing aligns to 8px grid
- [ ] Border radius correct (sm/md/lg/xl/full)
- [ ] Shadows use Phase 1 tokens
- [ ] Focus rings present (2-3px teal-500)
- [ ] Component naming consistent
- [ ] No missing variants
- [ ] States clearly different

### After Phase 2 Before Phase 3
- [ ] All components in Figma verified
- [ ] Figma file saved
- [ ] Phase 2 documentation complete (if needed)
- [ ] Take screenshot of all 17 components
- [ ] Ready to start Phase 3

---

## 📂 FILE LOCATIONS

```
/home/vmhieu/Workspace/UIT/health-lens/design-artifacts/

PHASE PROMPTS:
├── PHASE-1-IMPLEMENTATION-GUIDE.md (completed)
├── STITCH-AI-SPECIFICATIONS.md (Phase 1)
├── PHASE-2-STITCH-PROMPT-VI.md (ready to use ✅)
├── PHASE-2-IMPLEMENTATION-VI.md (manual guide, if needed)
├── PHASE-3-STITCH-PROMPT-VI.md (ready to use ✅)
├── PHASE-4-STITCH-PROMPT-VI.md (ready to use ✅)

REFERENCE DOCS:
├── DESIGN-SYSTEM-PLAN.md (5-phase strategy)
├── TOKENS-QUICK-REFERENCE.md (hex codes, sizes)
├── SCREENS-FULL-LIST-VI.md (detailed screen breakdown)

THIS FILE:
└── MASTER-EXECUTION-GUIDE.md (you are here)
```

---

## 🔄 WORKFLOW SUMMARY

Each phase follows same pattern:

```
┌─────────────────────────────────────┐
│ 1. COPY PROMPT                      │
│    File → Select all → Copy         │
├─────────────────────────────────────┤
│ 2. STITCH AI                        │
│    Paste → Configure → Generate     │
│    (Wait 10-30 minutes)             │
├─────────────────────────────────────┤
│ 3. IMPORT TO FIGMA                  │
│    Export → Figma → Select pages    │
│    Organize into correct pages      │
├─────────────────────────────────────┤
│ 4. QA & VERIFY                      │
│    Check colors, typography, states │
│    Run through checklist            │
├─────────────────────────────────────┤
│ 5. SAVE & DOCUMENT                  │
│    Save file, add notes, share      │
└─────────────────────────────────────┘
```

---

## ⏱️ TIME ESTIMATES

```
Phase 2: Composite + Layout + Data (17 components)
├── Copy prompt & prep:        0:15 hours
├── Stitch AI generation:      0:30 hours (plus wait time)
├── Import & organize:         0:45 hours
├── QA & verification:         1:00 hour
├── Documentation & save:      0:30 hours
└── Total: 3-4 hours

Phase 3: Custom Health Components (8 components)
├── Copy prompt & prep:        0:15 hours
├── Stitch AI generation:      0:30 hours (plus wait time)
├── Import & organize:         0:30 hours
├── QA & verification:         0:45 hours
└── Total: 2-3 hours (after Phase 2)

Phase 4: Web + Mobile Screens (20 screens)
├── Copy prompt & prep:        0:15 hours
├── Stitch AI generation:      1:00 hour (larger prompt)
├── Import & organize:         1:00 hour
├── QA & verification:         1:30 hours
├── Documentation & save:      0:45 hours
└── Total: 4-6 hours (after Phase 3)

GRAND TOTAL: 12-17 hours over 3 days
```

---

## 🎨 KEY DESIGN PRINCIPLES (REMINDER)

As you go through Phase 2, 3, 4, remember:

**"Calm Healthcare" Philosophy**
- Warm, approachable, not alarming
- Color + Icon + Text redundancy for critical info
- Large text for elderly (min 16px body, min 14px labels)
- Clear, simple language (Tiếng Việt)
- High contrast for accessibility

**Design System Rules**
- 8px grid: All spacing is multiple of 8 (4, 8, 12, 16, 20, 24, 32, 40, 48px)
- 3-color palette: Teal (primary), Slate (neutral), Functional (red/yellow/green)
- Reuse tokens: NO hard-coded hex values
- Component hierarchy: Use Phase 1/2/3 components in Phase 4 screens
- Accessibility: Focus rings on all interactive elements

**Color-Blind Friendly**
- Don't rely on color alone
- Use icons + text labels
- High contrast ratios (WCAG AA minimum 4.5:1)
- Consider deuteranopia (red-green) color blindness

---

## ❓ FAQ & TROUBLESHOOTING

**Q: Can I skip a phase?**
A: Not recommended. Each phase builds on the previous:
- Phase 1 (tokens) → Phase 2 (components) → Phase 3 (health components) → Phase 4 (screens)

**Q: What if Stitch AI generation fails?**
A: 
1. Try again (sometimes API times out)
2. Simplify prompt (remove some components)
3. Use manual implementation guide (PHASE-X-IMPLEMENTATION-VI.md)

**Q: Can I edit components after import?**
A: Yes! Figma allows editing, but keep component system consistent.
- Update main component → all instances update automatically
- Keep naming convention consistent

**Q: How do I handle multiple screens for responsive?**
A: Show 3 versions per screen:
1. Desktop (1280px width)
2. Tablet (768px width - if different layout)
3. Mobile (360px width)
Stack them vertically on same page or create separate pages.

**Q: Should I use Phase 1 color styles or create new ones for Phase 4?**
A: ALWAYS reuse Phase 1 styles:
- Open Phase 1 color style
- Drag to component
- Don't create new colors
- Maintains design system consistency

---

## 🚀 NEXT STEPS

### Immediate (Now)
1. **Read this guide** (you're doing it!)
2. **Prepare for Phase 2:**
   - Check Stitch AI account is active
   - Open PHASE-2-STITCH-PROMPT-VI.md
   - Have Figma file ready
3. **Start Phase 2** when ready (copy prompt → Stitch AI)

### Phase 2 (0-4 hours)
- Generate 17 components via Stitch AI
- Import to Figma pages 7, 8, 9
- Run QA checklist
- Save file

### Phase 3 (4-7 hours)
- Wait for Phase 2 to be completely done
- Generate 8 health components via Stitch AI
- Import to Figma page 10
- Run QA checklist

### Phase 4 (7-17 hours)
- Wait for Phase 3 to be completely done
- Generate 20 screens (11 web + 9 mobile) via Stitch AI
- Import to Figma pages 12-20
- Run extensive QA
- Final documentation and sharing

### After All Phases (17+ hours)
- Complete design system in Figma
- Export components library
- Create developer handoff documentation
- Share with engineering team
- Begin implementation

---

## 📞 SUPPORT & RESOURCES

**If you need help:**
1. Check relevant IMPLEMENTATION guide (not just STITCH-PROMPT)
2. Review DESIGN-SYSTEM-PLAN.md for strategy
3. Check TOKENS-QUICK-REFERENCE.md for specific values
4. Read this MASTER-EXECUTION-GUIDE.md again

**Files Available:**
- All prompts: `/design-artifacts/PHASE-*-STITCH-PROMPT-VI.md`
- All guides: `/design-artifacts/PHASE-*-IMPLEMENTATION-*.md`
- Reference: `/design-artifacts/TOKENS-QUICK-REFERENCE.md`
- Plan: `/design-artifacts/DESIGN-SYSTEM-PLAN.md`

---

## ✅ FINAL CHECKLIST

Before you start:
- [ ] Read entire MASTER-EXECUTION-GUIDE.md
- [ ] Understand 3-phase workflow
- [ ] Have PHASE-2-STITCH-PROMPT-VI.md ready
- [ ] Stitch AI account active and logged in
- [ ] Figma file "HealthLens - Design System" ready
- [ ] Time allocated: ~12-17 hours total
- [ ] Understand "Calm Healthcare" philosophy
- [ ] Know 8px grid rules

---

## 📊 STATUS BOARD

```
PHASE 1: Foundation      ✅ COMPLETE
├── 50+ color tokens    ✅
├── 8 typography styles ✅
├── Spacing + shadows   ✅
└── 7 foundation comps  ✅

PHASE 2: Composite + Layout + Data   🔄 READY TO START
├── 17 components       📋 (ready in PHASE-2-STITCH-PROMPT-VI.md)
└── 24+ variants        📋

PHASE 3: Custom Health Comps        ⏳ READY (after Phase 2)
├── 8 health components 📋 (ready in PHASE-3-STITCH-PROMPT-VI.md)
└── 20+ variants        📋

PHASE 4: Web + Mobile Screens       ⏳ READY (after Phase 3)
├── 11 web screens      📋 (ready in PHASE-4-STITCH-PROMPT-VI.md)
├── 9 mobile screens    📋
└── Responsive variants 📋

TOTAL: 50+ components + 20 screens = Complete Design System
```

---

**This guide is your roadmap. Follow it step by step, and you'll have a complete design system in 12-17 hours!**

**Good luck! 🚀**

---

Document Version: 1.0  
Created: March 2026  
Status: Ready for Phase 2 Execution  
Language: Tiếng Việt + English
