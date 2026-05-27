# Initial Reference Ranges Provenance

Accessed: 2026-05-22, Asia/Ho_Chi_Minh

## Purpose

This dataset is the initial HealthLens MVP/demo reference-data import file for common adult lab metrics. It is intended to exercise OCR review, metric classification, Vietnamese explanations, and admin reference-data workflows. It is not a diagnostic authority and must not replace the reference interval printed by a lab or advice from a licensed clinician.

Import file: `docs/reference-data/initial-reference-ranges.csv`

## Import Schema

The file uses the existing Story 7.3 import schema:

```csv
metricName,displayNameVi,unit,minValue,maxValue,gender,minAge,maxAge
```

Rules:

- `metricName`, `displayNameVi`, `unit`, `minValue`, and `maxValue` are required.
- `gender` is empty, `male`, or `female`.
- `minAge` is adult-only for this seed set.
- Decimal values use `.` regardless of locale.
- The file contains no patient names, dates of birth, phone numbers, addresses, medical record numbers, uploaded documents, or any other PHI.

## Source Selection

Priority order:

1. U.S. NIH/National Library of Medicine MedlinePlus or related U.S. government sources for general public lab references.
2. Mayo Clinic Laboratories or CDC/NHANES laboratory documentation when a numeric lower bound is useful and MedlinePlus gives only a decision cutoff.
3. Existing project metric naming and units from HealthLens migrations, when mapping values into the app's import schema.

Vietnam-specific reference interval sources should replace or augment this file later when a reviewed, population-appropriate source is selected. A candidate local source is the Vietnamese Journal of Health and Development Studies article indexed at VJOL, but it still needs human review before its values are promoted into this import file.

## Metric Provenance

| Metric | CSV value | Source and rationale |
|---|---:|---|
| Glucose | 3.9-5.6 mmol/L | MedlinePlus comprehensive metabolic panel lists glucose 70-100 mg/dL / 3.9-5.6 mmol/L. Treated as fasting/routine adult screening context. |
| HbA1c | 4.0-5.6 % | MedlinePlus/CDC/NIDDK classify normal A1C as below 5.7%. Mayo Clinic Laboratories and CDC NHANES lab documentation provide 4.0-5.6% as a routine reference range. |
| Cholesterol | 0.0-5.17 mmol/L | MedlinePlus adult healthy total cholesterol is less than 200 mg/dL. Converted with cholesterol factor 0.02586 mmol/L per mg/dL. This is a cardiovascular decision threshold, not a lab interval. |
| Triglycerides | 0.0-1.69 mmol/L | MedlinePlus gives normal triglycerides below 150 mg/dL. Converted with triglyceride factor 0.01129 mmol/L per mg/dL. This is a decision threshold. |
| HDL male | 1.03-999.0 mmol/L | MedlinePlus says HDL less than 40 mg/dL is low for men. Converted 40 mg/dL with cholesterol factor 0.02586. Upper bound is open-ended in HealthLens' min/max schema. |
| HDL female | 1.29-999.0 mmol/L | Common adult female HDL low threshold is 50 mg/dL; this row should be revalidated against a chosen Vietnamese/local lab source before clinical release. Converted with cholesterol factor 0.02586. |
| LDL | 0.0-2.59 mmol/L | MedlinePlus adult healthy LDL is less than 100 mg/dL. Converted with cholesterol factor 0.02586. This is a cardiovascular decision threshold. |
| Hemoglobin male | 138-172 g/L | MedlinePlus hemoglobin encyclopedia lists adult male 13.8-17.2 g/dL / 138-172 g/L. |
| Hemoglobin female | 121-151 g/L | MedlinePlus hemoglobin encyclopedia lists adult female 12.1-15.1 g/dL / 121-151 g/L. |
| WBC | 4.5-11.0 10^9/L | MedlinePlus WBC count encyclopedia lists 4,500-11,000 per microliter / 4.5-11.0 x 10^9/L. |
| AST | 8-33 U/L | MedlinePlus AST encyclopedia lists 8-33 U/L. |
| ALT | 4-36 U/L | MedlinePlus ALT encyclopedia lists 4-36 U/L. |
| Creatinine male | 62-115 umol/L | MedlinePlus creatinine blood test lists adult male 0.7-1.3 mg/dL / 62-115 umol/L. |
| Creatinine female | 44-84 umol/L | MedlinePlus creatinine blood test lists adult female 0.5-0.95 mg/dL / 44-84 umol/L. |
| Urea | 2.14-7.14 mmol/L | MedlinePlus comprehensive metabolic panel lists BUN 6-20 mg/dL / 2.14-7.14 mmol/L. HealthLens currently names this metric `Urea`; treat this as BUN-equivalent until the domain model distinguishes urea from BUN explicitly. |

## Source URLs

- MedlinePlus comprehensive metabolic panel: https://medlineplus.gov/ency/article/003468.htm
- MedlinePlus cholesterol levels: https://medlineplus.gov/lab-tests/cholesterol-levels/
- MedlinePlus A1C: https://medlineplus.gov/a1c.html
- CDC A1C test: https://www.cdc.gov/diabetes/diabetes-testing/prediabetes-a1c-test.html
- Mayo Clinic Laboratories HbA1c overview: https://www.mayocliniclabs.com/test-catalog/Overview/610441
- CDC NHANES HbA1c laboratory method: https://wwwn.cdc.gov/nchs/data/nhanes/public/2021/labmethods/GHB-L-MET-Premier-508.pdf
- MedlinePlus hemoglobin: https://medlineplus.gov/ency/article/003645.htm
- MedlinePlus WBC count: https://medlineplus.gov/ency/article/003643.htm
- MedlinePlus AST: https://medlineplus.gov/ency/article/003472.htm
- MedlinePlus ALT: https://medlineplus.gov/ency/article/003473.htm
- MedlinePlus creatinine blood test: https://medlineplus.gov/ency/article/003475.htm
- Candidate Vietnam-local reference source for future review: https://vjol.vista.gov.vn/SK-PT/vi/article/view/77311

## Known Limitations

- Lipid rows are cardiovascular decision thresholds rather than strict lab reference intervals.
- HDL uses open-ended `maxValue` because HealthLens currently models every rule as `minValue` and `maxValue`.
- Urea is mapped from BUN-equivalent MedlinePlus units; this should be revisited if the app later distinguishes blood urea, BUN, and urea nitrogen.
- Lab reference ranges vary by analyzer, method, specimen, population, age, sex, pregnancy status, altitude, and local laboratory policy.
- This file should go through the admin import preview, draft change set, and publish/approve workflow. Do not seed it directly into production tables with SQL.
