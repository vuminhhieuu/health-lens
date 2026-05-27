# Core Feature Reference Dataset v1 Provenance

Accessed/reviewed: 2026-05-23, Asia/Ho_Chi_Minh

## Purpose

`core-feature-reference-dataset-v1.csv` is a curated fallback/reference-data seed for HealthLens core flows:

- OCR metric normalization by canonical metric name and active aliases.
- System fallback classification when the uploaded document does not provide a usable reference range.
- Explain/RAG/LLM context alignment through matching `metricKey` and aliases.
- Admin import workflow validation through draft change sets, approval/publish, and audit logs.

This dataset does not replace the reference interval printed on a lab report and does not replace clinician interpretation.
If OCR extracts a document-provided reference range, HealthLens must continue to prefer that document range and show `referenceRangeSource=document`.

## Source Policy

Each row carries source metadata in CSV columns:

- `sourceUrl`
- `sourceTitle`
- `sourcePublisher`
- `accessedDate`
- `rangeType`
- `reviewerNote`
- `conversionNote`
- `methodSpecimenNote`

Accepted `rangeType` values are:

- `reference_interval`
- `clinical_decision_threshold`
- `lab_specific_interval`

Rows using decision thresholds, such as HbA1c and lipid categories, are intentionally marked as `clinical_decision_threshold` rather than normal laboratory intervals.
Rows using public laboratory handbooks are marked as `lab_specific_interval`; a reviewer must confirm local applicability before production use.

## Scope

Priority A coverage includes glucose/HbA1c, lipid profile, CBC and differential, liver panel, kidney markers, electrolytes, calcium, total protein, albumin, bilirubin, and CRP.

Priority B/deferred coverage is not included in v1:

- TSH, FT4, FT3.
- Uric acid.
- Ferritin, iron, vitamin D.
- Urinalysis qualitative values such as negative, trace, positive.

Qualitative urinalysis values are deferred because the current numeric `ReferenceRange` model is not appropriate for categorical values.

## Safety Guardrails

- The CSV contains only public reference data and no PHI.
- The dataset must be imported through admin preview/confirm and then published or approved through the existing workflow.
- Do not seed directly into production tables with SQL for this story.
- If a source or value is found to be wrong, rollback by deactivating or editing the metric/range through admin UI or by submitting a correcting change set.
- Unit conversions are not applied silently at runtime. `conversionNote` documents any row that needs human review for local lab unit conventions.
