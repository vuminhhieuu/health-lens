---
stepsCompleted: [step-01-init, step-02-discovery, step-02b-vision, step-02c-executive-summary, step-03-success, step-04-journeys, step-05-domain, step-06-innovation, step-07-project-type, step-08-scoping, step-09-functional, step-10-nonfunctional, step-11-polish]
inputDocuments: []
workflowType: 'prd'
project_name: health-lens
user_name: ie303
date: '2026-03-17'
classification:
  projectType: web_app + mobile_app
  domain: healthcare
  complexity: high
  projectContext: greenfield
  compliance: Nghị định 13/2023/NĐ-CP
---

# Product Requirements Document —   

**Author:** ie303 | **Date:** 2026-03-17 | **Platform:** Web (Next.js) + Mobile (React Native/Expo) + Backend (Java Spring Boot)

---

## Executive Summary

HealthLens là ứng dụng **personal health intelligence** dành cho người Việt Nam — biến kết quả khám bệnh thô (PDF, ảnh giấy) thành hiểu biết có thể hành động được. Vấn đề cốt lõi: người dùng nhận kết quả khám nhưng không hiểu các chỉ số có nghĩa gì và không biết phải làm gì tiếp theo.

**Người dùng mục tiêu:** Người trung niên/cao tuổi có bệnh nền mãn tính và người thân muốn theo dõi, hỗ trợ từ xa.

**Bốn năng lực cốt lõi:**

1. **Hiểu** — OCR trích xuất chỉ số, LLM giải thích bằng tiếng Việt đơn giản, so sánh ngưỡng chuẩn
2. **Hành động** — Gợi ý lối sống, dinh dưỡng, thời điểm cần gặp bác sĩ dựa trên chỉ số cá nhân
3. **Theo dõi** — Lịch sử chỉ số theo thời gian, nhận diện xu hướng
4. **Chia sẻ** — Mạng lưới sức khỏe gia đình: người thân theo dõi cross-profile từ web dashboard

**Insight cốt lõi:** Khoảnh khắc người dùng lần đầu *thực sự hiểu* con số trên tờ kết quả khám tạo ra sự gắn kết — từ đó họ chủ động quản lý sức khỏe thay vì bị động chờ đợt khám tiếp theo.

**Điểm khác biệt:** Không có sản phẩm nào tại VN kết hợp OCR + LLM để giải thích kết quả khám bằng tiếng Việt thông thường. Các app hiện tại (BookingCare, Medici, app bệnh viện) tập trung vào đặt lịch/lưu trữ — không giải thích chỉ số, không family sharing.

---

## Success Criteria

### User Success

- Người bệnh đọc giải thích chỉ số sau khi upload và hiểu ngay — không cần tra cứu bên ngoài
- Người thân gia đình mở web dashboard thấy tóm tắt sức khỏe đủ thông tin để yên tâm hoặc lo lắng đúng chỗ
- ≥70% người dùng hoàn thành toàn bộ flow upload → xem giải thích trong lần đầu sử dụng

### Business Success

- 300 weekly active users trong vòng 3 tháng sau launch
- **"Active"**: upload ≥1 kết quả khám HOẶC đăng nhập xem lịch sử trong 7 ngày
- **Primary metric:** Weekly retention rate
- **Secondary:** Số lần upload/user/tháng, tỉ lệ kích hoạt family sharing

### Measurable Outcomes

| Outcome | Metric | Target | Thời hạn |
|---|---|---|---|
| Adoption | WAU | 300 users | T+3 tháng |
| Retention | Weekly return rate | ≥40% | T+3 tháng |
| Core flow | Upload completion rate | ≥70% | T+1 tháng |
| Family feature | % users tạo ≥2 profile | ≥30% | T+3 tháng |
| OCR quality | Extraction accuracy | ≥85% | Launch |

---

## Product Scope

### P1 — Core (Launch)

- Upload PDF/ảnh từ thư viện (Web + Mobile) → OCR → hiển thị chỉ số kèm giải thích LLM
- **Ghi chú:** Chụp camera trên mobile (FR11), chế độ offline (FR27) và tự đồng bộ (FR28) chuyển sang Phase 2 (Epic 9)
- Cảnh báo chỉ số bất thường, AI khuyến nghị cá nhân hóa (Mức A+B)
- Lịch sử khám theo timeline, quản lý hồ sơ gia đình (multi-profile)
- Web dashboard read-only cho người thân theo dõi
- Consent flow + right-to-delete (bắt buộc NĐ 13/2023)
- Admin panel quản lý reference data y tế

### P2 — Growth (T+1–2 tháng)

- Push notifications nhắc tái khám và uống thuốc
- Thống kê xu hướng chỉ số (trend charts)
- Xuất PDF tóm tắt sức khỏe
- Real-time WebSocket dashboard update (upgrade từ polling)

### P3 — Vision (T+6 tháng+)

- Chia sẻ trực tiếp cho bác sĩ (identity verification + privacy workflow phức tạp)
- Crawl tự động reference data từ WHO/Bộ Y tế VN
- Khuyến nghị Mức C (thuốc/liều) — chỉ sau khi có tư vấn pháp lý y tế

---

## User Journeys

### J1 — Bà Lan: Lần Đầu Hiểu Kết Quả Khám *(Primary — Happy Path)*

**Persona:** 58 tuổi, Hà Nội. Mỡ máu cao 3 năm, khám hàng tháng, không hiểu các chỉ số LDL/HDL/Triglyceride trên tờ kết quả in A4.

1. Mở app mobile, chọn "Thêm kết quả khám mới"
2. Chụp ảnh tờ giấy bằng camera — app xử lý trong ~8 giây
3. Màn hình hiện danh sách chỉ số trích xuất: Cholesterol, LDL, HDL, Triglyceride, Glucose
4. Tap vào "LDL Cholesterol: 4.2 mmol/L" → *"LDL là cholesterol 'xấu'. Chỉ số của bạn ở mức Cần chú ý (ngưỡng an toàn: dưới 3.4). Nên duy trì chế độ ăn ít dầu mỡ và báo bác sĩ trong lần khám tới."*
5. Đọc khuyến nghị: hạn chế thịt đỏ, tăng rau xanh — lưu kết quả, đặt nhắc tái khám tháng sau *(P2 feature)*

**Outcome:** Lần đầu tiên sau 3 năm, bà Lan hiểu con số trên tờ giấy khám. Biết cần hỏi bác sĩ điều gì. Cảm giác chủ động.

*Capabilities: Camera OCR, chỉ số extraction, LLM explanation, ngưỡng chuẩn comparison, AI recommendation, timeline storage*

---

### J2 — Anh Minh: Theo Dõi Sức Khỏe Bố Từ Xa *(Secondary — Family Web)*

**Persona:** 32 tuổi, TP.HCM. Bố — ông Nam, 61 tuổi, tiểu đường type 2 — sống một mình ở Hà Nội. Đã dùng HealthLens 2 tháng, upload đều đặn.

1. Thứ Hai sáng, mở web dashboard trước khi họp
2. Thấy: *"Bố — Ông Nam: Kết quả khám mới nhất 3 ngày trước. HbA1c: 7.8% — Cần chú ý"*
3. Xem biểu đồ HbA1c 6 tháng — trend tăng nhẹ liên tục *(P2 feature — trend charts)*
4. AI summary: *"HbA1c tăng 0.3% so với tháng trước. Gợi ý: nhắc ông kiểm tra chế độ ăn và liên hệ bác sĩ nếu tháng tới vẫn tăng."*
5. Nhắn tin cho bố: *"Bố ăn ít cơm trắng hơn nhé"* — lo lắng đúng chỗ, có căn cứ

**Outcome:** Không cần gọi điện hỏi "bố có ổn không" — biết chính xác tình trạng sức khỏe bố.

*Capabilities: Multi-profile, web dashboard, trend charts, family sharing, AI summary*

---

### J3 — Chị Thu: OCR Thất Bại Và Phục Hồi *(Primary — Edge Case)*

**Persona:** 45 tuổi, Đà Nẵng. Kết quả khám từ phòng khám tư nhỏ — viết tay trên giấy photocopy mờ.

1. Upload ảnh → OCR xử lý 12 giây → chỉ nhận diện 3/8 chỉ số
2. App hiển thị: *"Một số chỉ số chưa đọc được. Bạn có thể nhập thủ công hoặc chụp lại ảnh rõ hơn."*
3. Chụp lại từng phần → thêm 2 chỉ số; 3 còn lại nhập tay
4. App lưu hồ sơ đầy đủ, ghi chú: *"3 chỉ số nhập thủ công"*

**Outcome:** Dù OCR không hoàn hảo, dữ liệu đầy đủ. App không crash, không để người dùng bơ vơ.

*Capabilities: OCR error handling, manual input fallback, data source tagging, UX guidance*

---

### J4 — Admin: Cập Nhật Reference Data Y Tế *(Admin)*

**Persona:** Admin hệ thống — đảm bảo ngưỡng chuẩn chỉ số luôn cập nhật.

1. Đăng nhập admin panel (MFA)
2. Reference Data Management → tìm "HbA1c" → xem giá trị hiện tại
3. Cập nhật ngưỡng theo guideline mới, lưu với ghi chú nguồn
4. Hệ thống apply cho kết quả mới; kết quả cũ giữ nguyên annotation gốc
5. Audit log ghi: ai thay đổi, khi nào, từ giá trị nào sang giá trị nào

*Capabilities: Admin panel, reference data CRUD, approve workflow, audit logging, versioned data*

---

### Journey–Capability Summary

| Journey | FR liên quan |
|---|---|
| J1 — Bà Lan | FR9–FR17 (upload/OCR), FR18–FR23 (explanation), FR24–FR25 (history) |
| J2 — Anh Minh | FR5–FR6 (multi-profile), FR29–FR32 (family sharing) |
| J3 — Chị Thu | FR14–FR16 (OCR fallback, manual input, error UX) |
| J4 — Admin | FR33–FR38 (admin panel, reference data, audit) |

---

## Domain Requirements

### Compliance — Nghị định 13/2023/NĐ-CP

Dữ liệu sức khỏe là **dữ liệu cá nhân nhạy cảm** — các yêu cầu bắt buộc:

- Xin đồng ý rõ ràng và riêng biệt trước khi thu thập dữ liệu y tế (consent flow — FR8)
- Right-to-delete: thực thi trong vòng 72 giờ kể từ khi nhận yêu cầu (FR7, NFR-S5)
- Không chia sẻ dữ liệu y tế với bên thứ ba khi chưa có đồng ý tường minh
- Host trên cloud quốc tế: tuân thủ điều khoản chuyển dữ liệu xuyên biên giới (Điều 25)

### Giới Hạn Khuyến Nghị Y Tế

| Mức | Nội dung | Priority |
|---|---|---|
| **A** | Giải thích chỉ số — *"LDL là gì, tại sao quan trọng"* | P1 |
| **B** | Gợi ý lối sống chung + disclaimer bắt buộc: *"Chỉ mang tính tham khảo, không thay thế bác sĩ"* | P1 |
| **C** | Khuyến nghị thuốc/liều — không implement cho đến khi có tư vấn pháp lý | P3 |

### Reference Data Management

Ngưỡng chuẩn chỉ số y tế — **hybrid approach**:

- Admin import file chuẩn (CSV/JSON từ WHO, Bộ Y tế VN) — kiểm soát được, có version control
- Admin review + approve trước khi apply lên production
- Audit log: ai thay đổi gì, khi nào, nguồn tham chiếu nào
- Context-aware thresholds: phân biệt theo tuổi, giới tính, bệnh nền
- Crawl tự động: P3 Vision — chỉ sau khi có cơ chế validate tự động đủ mạnh

### Risk Summary

| Rủi ro | Mức độ | Biện pháp |
|---|---|---|
| Khuyến nghị y tế sai gây hại | Cao | Giới hạn Mức A+B, disclaimer bắt buộc |
| OCR trích xuất sai chỉ số | Trung bình | Manual fallback, tag nguồn, user confirm |
| Reference data lỗi thời | Trung bình | Version control, admin approve workflow |
| Rò rỉ dữ liệu y tế | Cao | Encryption at rest + in transit, audit log, RBAC |
| Vi phạm NĐ 13/2023 | Cao | Consent flow, right-to-delete 72h |
| LLM hallucinate thông tin y tế | Cao | Giới hạn Mức A+B; cache theo hash để kiểm soát output |
| Family sharing vi phạm privacy | Trung bình | Người được share phải approve trước khi xem |

---

## Innovation Analysis

### Khoảng Trống Thị Trường

| Đối thủ | Định vị | Điểm thiếu |
|---|---|---|
| BookingCare | Đặt lịch khám, lưu hồ sơ | Không giải thích chỉ số, không AI |
| Medici | Telemedicine, tư vấn bác sĩ | Trả phí, không phân tích kết quả |
| App bệnh viện (VinMec, FV) | Hồ sơ bệnh nhân nội bộ | Không cross-hospital, không family sharing |
| Google/Apple Health | Tổng quát | Không LLM tiếng Việt, không OCR giấy VN |

**Khoảng trống:** Không sản phẩm nào giải quyết *"tôi có kết quả khám trong tay nhưng không hiểu và không biết phải làm gì"* — đặc biệt với người dùng không tech-savvy.

### Ba Innovation Areas

**1. LLM-Powered Health Interpretation cho VN** — Dùng LLM không để chat mà để translate medical jargon → ngôn ngữ phổ thông có context cá nhân (tuổi, giới tính, bệnh nền).

**2. Family Health Network Model** — Một tài khoản quản lý nhiều hồ sơ, người thân theo dõi cross-profile. Phù hợp văn hóa VN: con cái chăm sóc cha mẹ từ xa.

**3. Contextual Reference Data** — Context-aware thresholds (tuổi, giới tính, bệnh nền) thay vì ngưỡng one-size-fits-all.

### Validation Pre-Launch

- OCR: test trên 200+ mẫu kết quả khám từ Bạch Mai, Chợ Rẫy, phòng khám tư nhỏ — target ≥85%
- LLM explanation: user testing với 20 người phổ thông — đo tỉ lệ "hiểu ngay không cần tra Google"
- Family feature: track % users tạo ≥2 profile trong 30 ngày đầu

---

## Platform Requirements

### Architecture Overview

- **API-first:** Spring Boot REST API, cả web và mobile consume cùng endpoint
- **File processing pipeline:** Upload → Object Storage (S3-compatible) → OCR (async) → Parse → DB → Notify
- **Real-time (P1 MVP):** Polling 30s cho web dashboard; WebSocket upgrade ở P2
- **Offline (mobile):** Expo SQLite cache — read-only offline, sync queue khi có mạng

### Web App

| Requirement | Chi tiết |
|---|---|
| Framework | Next.js App Router — SSR cho landing page, CSR cho dashboard |
| Browser | Chrome, Firefox, Safari, Edge — 2 phiên bản gần nhất |
| Responsive | Desktop-first cho dashboard; mobile-responsive cho landing page |
| Accessibility | WCAG 2.1 AA cho toàn bộ giao diện kết quả y tế |

### Mobile App

| Requirement | Chi tiết |
|---|---|
| Platform | React Native (Expo) — iOS + Android |
| Camera | Expo Camera — chụp ảnh giấy khám trực tiếp |
| File access | Expo Document Picker (PDF) + Expo Image Picker (ảnh thư viện) |
| Offline | SQLite cache; UI hiển thị trạng thái offline + last sync time; sync queue |
| Permissions | Camera, photo library, notifications, local storage |
| Store compliance | App Store + Google Play — health app guidelines, privacy disclosure |

### Real-time Flow

```
Mobile upload → Spring Boot API → OCR (async) → DB saved
    → Polling/WebSocket → Web Dashboard update
    → Push Notification → Mobile (thành viên gia đình khác)
```

### Offline Strategy

| Trạng thái | Có thể làm |
|---|---|
| Offline | Xem lịch sử đã cache (read-only) |
| Offline | Không thể: upload mới, AI interpretation, sync profile mới |
| Back online | Tự động sync; conflict resolution: last-write-wins (profile), append-only (kết quả khám) |

---

## Project Scoping

### MVP Philosophy

**Approach:** Experience MVP — toàn bộ core loop phải hoạt động mượt mà, không stripped-down.

> *"Một người dùng upload kết quả khám lần đầu và thực sự hiểu chỉ số của mình — không có gì quan trọng hơn ở Phase 1."*

Khoảnh khắc "aha" xảy ra khi flow upload → OCR → giải thích chạy trơn tru. Thiếu bất kỳ bước nào, người dùng sẽ không quay lại.

### Phase 1 — MVP (Launch)

**Journeys hỗ trợ:** J1, J3, J4 (J2 ở mức read-only cơ bản)

**Must-have:**
- Upload ảnh/PDF từ thư viện (web/mobile) → OCR → hiển thị chỉ số; camera native (FR11) và offline/sync (FR27–28) thuộc Phase 2
- Manual fallback khi OCR thất bại, tag nguồn dữ liệu
- LLM explanation Mức A+B + cảnh báo bất thường
- Lịch sử khám theo timeline
- Auth (đăng ký/đăng nhập/đặt lại mật khẩu)
- Multi-profile (≥3 profiles/tài khoản)
- Web dashboard read-only
- Admin panel reference data CRUD
- Consent flow + right-to-delete

**Deferrable đến P2:**
- Real-time WebSocket → polling 30s trong MVP
- Push notifications → launch sau 2 tuần đầu

### Phased Roadmap

| Phase | Focus | Timeline |
|---|---|---|
| Phase 1 — MVP | Core upload → OCR → explain loop; multi-profile; web read-only | Launch |
| Phase 2 — Growth | Real-time dashboard; push notifications; trend charts; PDF export | T+1–2 tháng |
| Phase 3 — Vision | Doctor sharing; auto-crawl reference data; Mức C recommendation | T+6 tháng+ |

### Scoping Risks

| Rủi ro | Biện pháp |
|---|---|
| OCR quality thấp với giấy VN | Manual fallback bắt buộc từ MVP; test 200+ mẫu trước launch |
| LLM cost vượt budget | Cache theo hash; giới hạn calls/user/tháng nếu cần |
| WebSocket complexity delay launch | Polling 30s là fallback acceptable |
| Thiếu OCR training data VN | Thu thập mẫu sớm trước khi code integration |

---

## Functional Requirements

### Quản Lý Tài Khoản & Hồ Sơ

- FR1: Người dùng có thể đăng ký tài khoản bằng email và mật khẩu
- FR2: Người dùng có thể đăng nhập và đăng xuất
- FR3: Người dùng có thể đặt lại mật khẩu qua email
- FR4: Người dùng có thể cập nhật thông tin cá nhân (tên, ngày sinh, giới tính)
- FR5: Người dùng có thể tạo và quản lý nhiều hồ sơ sức khỏe trong một tài khoản
- FR6: Người dùng có thể đặt tên và ghi chú cho từng hồ sơ
- FR7: Người dùng có thể yêu cầu xóa toàn bộ dữ liệu tài khoản (right-to-delete, NĐ 13/2023)
- FR8: Người dùng phải xác nhận đồng ý thu thập dữ liệu y tế nhạy cảm trước khi sử dụng

### Upload & Xử Lý Kết Quả Khám

- FR9: Người dùng có thể upload file PDF kết quả khám
- FR10: Người dùng có thể upload ảnh kết quả khám từ thư viện
- FR11: Người dùng (mobile) có thể chụp ảnh tờ kết quả bằng camera
- FR12: Người dùng có thể xem chỉ số y tế được tự động trích xuất từ ảnh/PDF qua OCR
- FR13: Người dùng có thể xem danh sách chỉ số trích xuất trước khi xác nhận lưu
- FR14: Người dùng có thể chỉnh sửa hoặc nhập thủ công chỉ số khi OCR thất bại
- FR15: Người dùng có thể xem nguồn gốc từng chỉ số (OCR-extracted vs. nhập tay)
- FR16: Người dùng nhận phản hồi rõ ràng khi OCR thất bại, kèm hướng dẫn tiếp theo
- FR17: Người dùng có thể gán kết quả khám cho một hồ sơ cụ thể

### Hiển Thị & Giải Thích Kết Quả

- FR18: Người dùng có thể xem từng chỉ số kèm giá trị và ngưỡng tham chiếu chuẩn
- FR19: Người dùng có thể xem trạng thái phân loại từng chỉ số (bình thường / cần chú ý / bất thường)
- FR20: Người dùng có thể xem giải thích tiếng Việt đơn giản cho từng chỉ số — ý nghĩa và tầm quan trọng (Mức A)
- FR21: Người dùng nhận gợi ý lối sống và dinh dưỡng dựa trên chỉ số cá nhân (Mức B) kèm disclaimer bắt buộc
- FR22: Người dùng nhận cảnh báo nổi bật khi chỉ số nằm ngoài ngưỡng bất thường
- FR23: Người dùng nhận ngưỡng tham chiếu context-aware theo tuổi và giới tính của hồ sơ

### Lịch Sử & Theo Dõi

- FR24: Người dùng có thể xem lịch sử kết quả khám của một hồ sơ theo timeline
- FR25: Người dùng có thể xem chi tiết bất kỳ kết quả khám trong lịch sử
- FR26: Người dùng có thể xóa một kết quả khám
- FR27: Người dùng (mobile) có thể xem lịch sử đã đồng bộ khi offline
- FR28: Người dùng có thể thấy dữ liệu được đồng bộ tự động khi kết nối mạng được khôi phục

### Chia Sẻ Gia Đình

- FR29: Người dùng có thể mời thành viên gia đình theo dõi một hồ sơ qua email
- FR30: Người được mời có thể xem lịch sử và kết quả khám của hồ sơ được chia sẻ qua web
- FR31: Chủ hồ sơ có thể thu hồi quyền truy cập của thành viên gia đình bất kỳ lúc nào
- FR32: Người dùng web nhận cập nhật khi hồ sơ được chia sẻ có kết quả mới

### Quản Trị (Admin)

- FR33: Admin có thể đăng nhập admin panel với MFA
- FR34: Admin có thể xem, thêm, sửa, xóa chỉ số y tế và ngưỡng tham chiếu
- FR35: Admin có thể import reference data từ file CSV/JSON
- FR36: Admin có thể approve thay đổi reference data trước khi có hiệu lực
- FR37: Admin có thể xem audit log toàn bộ thay đổi reference data (ai, khi nào, thay đổi gì, nguồn)
- FR38: Admin có thể xem dashboard thống kê bao gồm: tổng số user đăng ký (all-time và theo tháng), Weekly Active Users (WAU), tổng số lượt upload (all-time, theo ngày, theo tuần), tỷ lệ upload thành công vs. thất bại

---

## Non-Functional Requirements

### Performance

- NFR-P1: Flow upload → OCR → hiển thị chỉ số hoàn thành trong **≤10 giây** (ảnh chất lượng tốt, mạng bình thường)
- NFR-P2: Tải trang/màn hình (không bao gồm OCR/LLM) trong **≤2 giây** ở 4G/WiFi thông thường
- NFR-P3: LLM explanation hiển thị trong **≤5 giây** (bao gồm cache lookup)
- NFR-P4: Web dashboard cập nhật kết quả mới trong **≤30 giây** (polling) kể từ khi upload thành công

### Security

- NFR-S1: Dữ liệu y tế mã hóa **at rest** (AES-256) và **in transit** (TLS 1.2+)
- NFR-S2: Mọi truy cập dữ liệu y tế ghi vào **audit log** (ai, khi nào, hành động, IP)
- NFR-S3: Phiên đăng nhập hết hạn sau **30 phút** không hoạt động; refresh token rotation bắt buộc
- NFR-S4: Admin bắt buộc **MFA** — không ngoại lệ
- NFR-S5: Right-to-delete thực thi hoàn toàn trong **≤72 giờ**
- NFR-S6: File ảnh/PDF lưu tách biệt với dữ liệu chỉ số; người dùng có thể xóa file gốc sau OCR

### Scalability

- NFR-SC1: Hỗ trợ **500 concurrent users** không suy giảm hiệu năng (giai đoạn growth)
- NFR-SC2: OCR pipeline xử lý đồng thời **≥50 jobs** không timeout hoặc mất request
- NFR-SC3: Database và storage mở rộng horizontal hỗ trợ **10x user growth** không cần refactor kiến trúc

### Accessibility

- NFR-A1: Giao diện hiển thị kết quả y tế tuân thủ **WCAG 2.1 cấp AA**
- NFR-A2: Font size tối thiểu **16px** cho body text trên mobile
- NFR-A3: Phân loại trạng thái chỉ số không phụ thuộc hoàn toàn vào màu sắc — phải có icon hoặc text label bổ sung

### Integration & Reliability

- NFR-I1: OCR external API timeout **≤15 giây**; vượt quá → thông báo lỗi + hướng dẫn nhập thủ công
- NFR-I2: LLM API retry **≤3 lần** nếu lỗi tạm thời; cache theo hash(chỉ số + reference range)
- NFR-I3: Uptime **≥99%** trong giờ cao điểm (6:00–22:00 ICT)
- NFR-I4: Mobile offline mode hoạt động read-only; UI hiển thị trạng thái offline và thời gian đồng bộ cuối
