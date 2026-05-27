# Core Feature Data Requirements For Reference Data

Accessed/reviewed: 2026-05-22, Asia/Ho_Chi_Minh

## Why The Current Dataset Is Not Enough

The current `initial-reference-ranges.csv` is useful as a parser/import smoke dataset, but it is too small for the actual HealthLens core flow. It has 15 range rows for 12 metric names. The production path needs reference data to support OCR review, metric normalization, classification, explanation retrieval, LLM prompts, recommendations, PDF export, and admin audit.

When OCR extracts a metric without a reference range printed in the document, `HealthRecordService.enrichMetric(...)` calls `ReferenceDataService.classifyMetric(...)`. If the metric name or alias cannot match an active `ReferenceMetric`, the metric becomes `status=no_data`, `referenceRangeSource=none`, and loses useful context for Explain/RAG/LLM.

## How Core Features Use Reference Data

### OCR And Review

- `OcrService` extracts metric `name`, `value`, `unit`, optional document `flag`, and optional document `referenceRange`.
- If the document contains a valid range, the system classifies from the document range and marks `referenceRangeSource=document`.
- If the document does not contain a usable range, the system depends on reference DB matching by metric name or alias.
- The review UI renders status, source badge, range text, metric cards, and low-confidence review signals from enriched `MetricDto`.

Implication: reference data must include aliases that look like real Vietnamese lab sheets, not only canonical English names.

### Classification And Alerts

- `ReferenceDataService` resolves a metric by canonical `name` or `reference_metric_aliases.alias_normalized`.
- It selects active ranges by profile gender and age, then computes `normal`, `attention`, `abnormal`, or `no_data`.
- `attentionMin` and `attentionMax` drive abnormal/critical-like behavior, but the current import file only supplies min/max and `confirmImport(...)` sets attention bounds equal to min/max.

Implication: a quality dataset needs both normal intervals and wider attention/critical thresholds. The current import schema cannot express that nuance.

### RAG And Explain

- `MetricExplanationRetrievalService` queries the active approved RAG corpus by `metricKey` or aliases.
- If Qdrant has no matching approved chunk, it falls back to `ReferenceDataService.buildMetricKnowledgeSnippet(...)`.
- The reference-data fallback has hardcoded relation hints for only a subset of metrics.
- `LlmService.generateExplanationResult(...)` passes metric name, value, status, exact reference range, and retrieved knowledge into the prompt. It rejects outputs that invent reference ranges.

Implication: range data and RAG corpus must use the same metric keys and alias vocabulary. Otherwise Explain silently falls back to generic text.

### Recommendations

- Recommendations are generated only for risky metrics: `attention`, `warning`, or `abnormal`.
- If a metric cannot be classified, it usually will not become an actionable recommendation input.

Implication: reference coverage directly controls whether recommendations appear.

## Minimum Dataset Quality Bar

The import dataset should be promoted from "smoke/demo" to "core-feature seed" only when it meets these gates:

1. Coverage: covers the common panels expected from Vietnamese uploaded lab sheets.
2. Alias quality: includes Vietnamese, English, abbreviation, accented/unaccented, and OCR-noisy aliases.
3. Unit quality: records canonical unit and known alternate units, or explicitly blocks unsupported unit variants.
4. Range quality: distinguishes lab reference interval from disease/decision threshold.
5. Context quality: has age/gender rows where clinically meaningful.
6. Attention quality: stores wider attention/abnormal bounds, or the model/schema is changed to import them.
7. Provenance quality: every value has source URL, accessed date, conversion note, and reviewer note.
8. RAG alignment: every reference metric has a matching `metric-explanations.vi.json` chunk or a documented fallback.
9. Test quality: has fixture OCR metric names that prove alias matching and classification for real-looking rows.
10. Safety: contains no PHI and clearly says lab printed ranges should win over system fallback.

## Recommended Coverage For The Next Dataset

Priority A: needed for the current OCR/review/explain demo path.

- Glucose, HbA1c
- Cholesterol, Triglycerides, HDL, LDL
- Hemoglobin/HGB, WBC, RBC, Hematocrit/HCT, Platelet/PLT
- MCV, MCH, MCHC, RDW
- Neutrophils, Lymphocytes, Monocytes, Eosinophils, Basophils, and percent/absolute variants if the app supports units
- AST/GOT/SGOT, ALT/GPT/SGPT, ALP, GGT, Bilirubin total/direct, Albumin, Total Protein
- Creatinine, Urea/BUN, eGFR if numeric classification is supported
- Sodium/Na+, Potassium/K+, Chloride/Cl-, Calcium
- CRP

Priority B: useful but should be added only if source and units are clear.

- TSH, FT4, FT3
- Uric Acid
- Ferritin, Iron
- Vitamin D
- Urinalysis numeric fields such as pH and specific gravity; qualitative fields need a different data model.

## Current Schema Gaps

The existing CSV import schema accepts:

```csv
metricName,displayNameVi,unit,minValue,maxValue,gender,minAge,maxAge
```

This is not enough for high-quality clinical reference data because it cannot import:

- aliases
- source URL/source title/publisher
- accessed date
- normal-vs-decision-threshold type
- unit conversion metadata
- attentionMin/attentionMax
- method/specimen/analyzer notes
- review status per source/value
- qualitative values such as negative/trace/positive

For the next iteration, either extend Story 7.6 or create Story 7.7 for "reference data quality model expansion" before importing a much larger dataset.

## Source Candidates For Expansion

Use primary or reputable institutional sources first. Candidate sources reviewed for the next pass:

- MedlinePlus CBC and encyclopedia pages for CBC, WBC, hemoglobin, CMP/BMP, AST, ALT, creatinine, cholesterol, urinalysis.
- Mayo Clinic liver function tests for typical ALT, AST, ALP, albumin, bilirubin, and GGT ranges.
- ARUP Consult/Test Directory for liver panel composition and lab-specific intervals where source terms are clear.
- NIDDK/CDC/MedlinePlus for HbA1c and thyroid/kidney context.
- Vietnam-local peer-reviewed/public sources should be preferred when a human reviewer confirms applicability, population, units, and publication quality.

## Product Decision

The current file should stay labeled `MVP/demo smoke seed`, not `trusted core-feature reference set`.

Story 7.6 adds `core-feature-reference-dataset-v1.csv` as the first core-feature fallback dataset candidate. It is still imported through the admin draft/approval workflow and includes alias, attention-bound, source, range type, reviewer-note, conversion-note, and method/specimen metadata per row. Qualitative urinalysis values remain deferred because they require a categorical data model rather than numeric min/max ranges.

Recommended next move:

1. Create/upgrade a story for a larger "Core Feature Reference Dataset v1".
2. Expand the backend import schema to include aliases, provenance, and attention bounds.
3. Add or update RAG corpus chunks for every Priority A metric.
4. Add tests that feed representative OCR metric names into `ReferenceDataService.classifyMetric(...)` and `MetricExplanationRetrievalService.retrieve(...)`.
5. Only then import/publish the larger dataset through the admin approval workflow.
