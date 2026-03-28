# SMART Requirements Validation Report

**Document:** HealthLens PRD - Functional Requirements  
**Date:** 2026-03-17  
**Total FRs Analyzed:** 38

---

## Summary Table

| FR# | S | M | A | R | T | Avg | Flag |
|-----|---|---|---|---|---|-----|------|
| FR1 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR2 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR3 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR4 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR5 | 4 | 4 | 5 | 5 | 5 | 4.6 | |
| FR6 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR7 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR8 | 4 | 4 | 5 | 5 | 5 | 4.6 | |
| FR9 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR10 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR11 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR12 | 4 | 4 | 4 | 5 | 5 | 4.4 | |
| FR13 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR14 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR15 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR16 | 4 | 3 | 5 | 5 | 5 | 4.4 | |
| FR17 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR18 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR19 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR20 | 4 | 3 | 4 | 5 | 5 | 4.2 | |
| FR21 | 4 | 3 | 4 | 5 | 5 | 4.2 | |
| FR22 | 4 | 4 | 5 | 5 | 5 | 4.6 | |
| FR23 | 5 | 4 | 4 | 5 | 5 | 4.6 | |
| FR24 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR25 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR26 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR27 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR28 | 4 | 3 | 4 | 5 | 5 | 4.2 | |
| FR29 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR30 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR31 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR32 | 4 | 3 | 4 | 5 | 5 | 4.2 | |
| FR33 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR34 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR35 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR36 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR37 | 5 | 5 | 5 | 5 | 5 | 5.0 | |
| FR38 | 3 | 2 | 5 | 4 | 4 | 3.6 | X |

---

## Flagged Requirements (Score <3 in any criterion)

### FR38: Admin có thể xem thống kê sử dụng cơ bản (số user, số lượt upload)

| Criterion | Score | Rationale |
|-----------|-------|-----------|
| **Specific** | 3 | "Cơ bản" is vague - what exactly constitutes "basic" statistics? Only mentions 2 metrics but doesn't specify timeframes, breakdowns, or presentation format |
| **Measurable** | 2 | No clear acceptance criteria - how do we know when this is "done"? Missing: time ranges, aggregation levels, real-time vs. historical |
| **Attainable** | 5 | Straightforward to implement |
| **Relevant** | 4 | Useful for business but connection to user journeys is weak |
| **Traceable** | 4 | Loosely tied to J4 (Admin journey) but not explicitly mentioned |

**Improvement Suggestions:**
```
CURRENT: Admin có thể xem thống kê sử dụng cơ bản (số user, số lượt upload)

IMPROVED: Admin có thể xem dashboard thống kê bao gồm:
- Tổng số user đăng ký (all-time và theo tháng)
- Số user active trong 7 ngày qua (WAU)
- Tổng số lượt upload (all-time, theo ngày, theo tuần)
- Số upload thành công vs. thất bại (OCR)
- Biểu đồ trend 30 ngày cho các metric trên
```

---

## Statistics

| Metric | Value |
|--------|-------|
| Total FRs | 38 |
| FRs with all scores ≥3 | 38 (100%) |
| FRs with all scores ≥4 | 33 (86.8%) |
| FRs flagged (any score <3) | 1 (2.6%) |
| Overall average score | 4.76 |

### Score Distribution by Criterion

| Criterion | Average | Min | Max |
|-----------|---------|-----|-----|
| Specific | 4.68 | 3 | 5 |
| Measurable | 4.55 | 2 | 5 |
| Attainable | 4.87 | 4 | 5 |
| Relevant | 4.97 | 4 | 5 |
| Traceable | 4.97 | 4 | 5 |

---

## Severity Assessment

**PASS** - Only 2.6% of requirements flagged (<10% threshold)

The PRD demonstrates strong requirements quality. Only 1 out of 38 functional requirements has any score below 3.

---

## Recommendations

### Immediate Action (FR38)
Revise FR38 to specify exact metrics, time ranges, and visualization requirements.

### General Improvements (scores 3-4)

**FR16, FR20, FR21, FR28, FR32** scored 3 on Measurability:
- These requirements describe outcomes but lack precise acceptance criteria
- Consider adding success metrics or testable conditions

**Example improvements:**

| FR | Current Issue | Suggested Enhancement |
|----|---------------|----------------------|
| FR16 | "Phản hồi rõ ràng" is subjective | Add: "Error message displays within 2s, includes specific reason and at least 2 recovery options" |
| FR20 | "Giải thích đơn giản" is subjective | Add: "Explanation uses vocabulary at Grade 8 reading level, ≤100 words per metric" |
| FR21 | "Gợi ý lối sống" scope unclear | Add: "Provides 2-4 actionable suggestions per abnormal metric" |
| FR28 | "Tự động" timing undefined | Add: "Sync initiates within 5 seconds of network restoration" |
| FR32 | "Cập nhật" mechanism undefined | Reference NFR-P4 (≤30s polling) explicitly |

---

## Quality Assessment by Category

| Category | FRs | Avg Score | Flagged |
|----------|-----|-----------|---------|
| Account & Profile (FR1-8) | 8 | 4.83 | 0 |
| Upload & Processing (FR9-17) | 9 | 4.82 | 0 |
| Display & Explanation (FR18-23) | 6 | 4.73 | 0 |
| History & Tracking (FR24-28) | 5 | 4.76 | 0 |
| Family Sharing (FR29-32) | 4 | 4.80 | 0 |
| Admin (FR33-38) | 6 | 4.77 | 1 |

---

## Conclusion

The HealthLens PRD demonstrates **excellent requirements quality** with a 97.4% pass rate and 4.76 overall average score. The requirements are well-traced to user journeys (J1-J4) and align strongly with business objectives.

**Key Strengths:**
- Clear traceability to user journeys and compliance requirements (NĐ 13/2023)
- Strong specificity in core upload/OCR flow (FR9-FR17)
- Excellent relevance scores across all categories

**Single Issue:**
- FR38 needs expansion to define specific admin metrics and visualization

**Overall Verdict:** Requirements are implementation-ready with minor FR38 revision recommended.
