# Provider Switching Runbook

**Last updated:** 2026-05-18

Runbook này dùng khi thay đổi provider LLM chat, OCR, embedding hoặc vector store/RAG. Mục tiêu là đổi cấu hình có kiểm soát, có smoke test, và có đường rollback rõ ràng.

## Nguyên Tắc Chung

- Thực hiện provider switch trên staging trước production.
- Lưu lại cấu hình cũ trước khi đổi: env vars, secret names, provider dashboard project, model name, collection name.
- Không đổi đồng thời chat, OCR, embedding và vector store trong cùng một lần deploy nếu không bắt buộc.
- Mọi provider bên ngoài cần được đánh giá privacy, cost, latency và data residency trước khi bật production.
- Env-only switching chỉ an toàn khi provider tương thích với adapter hiện có. Nếu provider cần SDK/API shape khác, phải thêm code adapter và test trước.

## Compatibility Matrix

| Area | Env-only options hiện có | Adapter-required options | Env keys chính | Giới hạn vận hành |
| --- | --- | --- | --- | --- |
| LLM chat | Provider OpenAI-compatible qua `AI_CHAT_BASE_URL` | Native provider không expose OpenAI-compatible chat API | `AI_CHAT_PROVIDER`, `AI_CHAT_API_KEY`, `AI_CHAT_BASE_URL`, `AI_CHAT_MODEL`, `AI_CHAT_TIMEOUT_MS` | `AI_CHAT_PROVIDER` hiện chỉ hỗ trợ `openai-compatible`; `GROQ_*` chỉ là migration fallback. |
| Embedding | Provider tương thích OpenAI embeddings API qua `EMBEDDING_BASE_URL` | Provider có API/SDK embedding riêng | `EMBEDDING_API_KEY`, `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL`, `QDRANT_VECTOR_DIMENSION` | Đổi model/base URL có thể đổi vector dimension và bắt buộc reindex collection. |
| Vector store/RAG | Qdrant gRPC collection hiện có | Vector DB khác Qdrant hoặc Qdrant API không tương thích Spring AI config hiện tại | `QDRANT_HOST`, `QDRANT_PORT`, `QDRANT_API_KEY`, `QDRANT_COLLECTION`, `QDRANT_VECTOR_DIMENSION` | `QDRANT_HOST` phải là hostname thuần; staging/production fail-fast nếu dimension không khớp. |
| OCR | `easyocr`, `textract`, `gcv` theo registry hiện có | OCR provider mới ngoài registry | `OCR_PROVIDER_PRIMARY`, `OCR_PROVIDER_FALLBACK_ORDER`, `OCR_SERVICE_URL`, `OCR_TIMEOUT_MS`, `OCR_TEXTRACT_ENABLED`, `OCR_GCV_ENABLED`, `OCR_GCV_PROJECT_ID` | `textract` có stub mode nếu chưa enable; `gcv` cần API key/project config; fallback vẫn cần provider có capability phù hợp. |

## Preflight Checklist

- Xác nhận provider mới được phép xử lý dữ liệu y tế theo chính sách dự án.
- Xác nhận giới hạn chi phí, rate limit, region và retention của provider.
- Xác nhận secret đã có trong môi trường deploy, không commit secret vào repo.
- Xác nhận smoke test input có dữ liệu đại diện nhưng không chứa thông tin bệnh nhân thật nếu test với provider mới.
- Chuẩn bị rollback env vars và thời điểm cắt lại traffic.

## LLM Chat Switch

### Env-only switch

Áp dụng khi provider mới tương thích OpenAI Chat Completions API.

1. Giữ `AI_CHAT_PROVIDER=openai-compatible`.
2. Đổi `AI_CHAT_API_KEY`, `AI_CHAT_BASE_URL`, `AI_CHAT_MODEL`, và nếu cần `AI_CHAT_TIMEOUT_MS`.
3. Deploy staging.
4. Chạy smoke test chat/explanation có truy vấn tiếng Việt và input xét nghiệm ngắn.
5. Kiểm tra log API không có lỗi auth, timeout hoặc response parsing.

### Khi cần adapter

Cần code adapter nếu provider yêu cầu SDK riêng, endpoint không OpenAI-compatible, schema response khác, streaming-only, hoặc cơ chế auth khác. Không chỉ đổi env trong trường hợp này.

### Rollback

1. Khôi phục bộ `AI_CHAT_*` cũ.
2. Redeploy API.
3. Chạy lại smoke test explanation.
4. Kiểm tra log trong 15 phút đầu để xác nhận không còn lỗi provider mới.

## Embedding Switch

Đổi embedding là thay đổi có rủi ro cao vì vector dimension và vector space có thể thay đổi.

1. Xác nhận dimension của `EMBEDDING_MODEL` mới.
2. Set `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`, `QDRANT_VECTOR_DIMENSION`.
3. Tạo collection staging mới hoặc dùng collection rỗng.
4. Reingest corpus RAG bằng provider mới.
5. Deploy staging với strict validation bật theo profile.
6. Chạy retrieval smoke test và kiểm tra `/actuator/health` có `aiRag` UP.

Không trỏ production vào collection cũ nếu embedding model hoặc dimension đã đổi.

### Rollback

1. Khôi phục `EMBEDDING_*`, `QDRANT_VECTOR_DIMENSION`, và `QDRANT_COLLECTION` về bộ cũ.
2. Redeploy API.
3. Xác nhận startup không báo dimension mismatch.
4. Chạy retrieval smoke test với câu hỏi từng pass trước khi đổi.

## Vector Store / Qdrant Switch

1. Set `QDRANT_HOST` là hostname thuần, không có `https://`, port, path, query hoặc user-info.
2. Set `QDRANT_PORT=6334` trừ khi provider cấp gRPC port khác.
3. Set `QDRANT_API_KEY`, `QDRANT_COLLECTION`, `QDRANT_VECTOR_DIMENSION`.
4. Với collection mới, reingest corpus trước khi promote.
5. Kiểm tra `/actuator/health/readiness` cho app readiness và `/actuator/health` cho `aiRag`.

### Rollback

1. Khôi phục bộ `QDRANT_*` cũ.
2. Redeploy API.
3. Nếu startup fail do dimension mismatch, kiểm tra lại `QDRANT_COLLECTION` và `QDRANT_VECTOR_DIMENSION`.
4. Không tự động recreate production collection trong lúc incident; dùng collection backup/cũ đã biết tốt.

## OCR Switch

### Env-only switch trong registry hiện có

1. Chọn `OCR_PROVIDER_PRIMARY`: `easyocr`, `textract`, hoặc `gcv`.
2. Set `OCR_PROVIDER_FALLBACK_ORDER` theo thứ tự rollback tự động mong muốn, ví dụ `easyocr,textract`.
3. Với EasyOCR, xác nhận `OCR_SERVICE_URL` và `OCR_TIMEOUT_MS`.
4. Với Textract, set `OCR_TEXTRACT_ENABLED=true`, region/credentials AWS tương ứng.
5. Với GCV, set `OCR_GCV_ENABLED=true`, `OCR_GCV_PROJECT_ID`, API key/secret theo cấu hình deploy.
6. Deploy staging và upload ảnh xét nghiệm mẫu.
7. Kiểm tra provider source, confidence, failure diagnostics và fallback path trong log/API response.

### Khi fallback fail

1. Đổi `OCR_PROVIDER_PRIMARY` về provider cũ đã biết hoạt động.
2. Rút provider lỗi khỏi `OCR_PROVIDER_FALLBACK_ORDER`.
3. Redeploy API.
4. Với job OCR đang lỗi, retry thủ công hoặc yêu cầu người dùng upload lại nếu job không thể replay an toàn.
5. Nếu file đã upload vào object storage, giữ nguyên file và chỉ retry OCR job; không xóa hồ sơ khi chưa xác nhận trạng thái xử lý.

### Khi cần adapter

Cần code adapter nếu provider mới không nằm trong registry, cần SDK riêng, không hỗ trợ URL/base64 input hiện tại, hoặc trả confidence/text blocks khác schema hiện có.

## Smoke Test Checklist

| Area | Check | Expected result |
| --- | --- | --- |
| API readiness | `GET /actuator/health/readiness` | `UP` |
| AI/RAG health | `GET /actuator/health` | `aiRag` UP khi Qdrant reachable; DOWN/degraded rõ ràng khi unavailable |
| LLM chat | Gọi flow explanation tiếng Việt | Response hợp lệ, không timeout, không lỗi parse |
| Embedding/RAG | Truy vấn một metric có corpus liên quan | Retrieval trả nội dung liên quan, không báo dimension mismatch |
| OCR image | Upload ảnh xét nghiệm mẫu | OCR job completed, confidence hợp lý, provider source đúng |
| OCR fallback | Tạm cấu hình primary lỗi trên staging | Fallback chạy hoặc diagnostics nêu rõ provider fail |
| Rollback | Khôi phục env cũ trên staging | Service trở lại trạng thái pass trước switch |

## Manual Recovery Notes

- Nếu provider mới tạo dữ liệu chất lượng thấp, dừng ingestion mới trước khi rollback để tránh trộn vector space hoặc OCR output kém.
- Nếu embedding switch đã ghi vào collection sai, không sửa bằng cách đổi dimension tại chỗ; tạo collection sạch và reingest.
- Nếu OCR provider trả kết quả sai định dạng, giữ file gốc trong storage và retry sau khi rollback provider.
- Nếu chi phí tăng bất thường, disable provider mới bằng env rollback trước, sau đó phân tích usage dashboard.

## Related Docs

- [Environment Reference](./environment-reference.md)
- [Operations Runbook](./operations-runbook.md)
- [Deployment Guide](./deployment-guide.md)
- [Staging Deployment](./STAGING_DEPLOYMENT.md)
