# Quyết Định: Ownership Cho Generated OpenAPI Contracts

**Ngày:** 2026-05-28  
**Trạng thái:** Accepted  
**Phạm vi:** Epic R2 - API Contract Foundation With OpenAPI  
**Story nguồn:** `_bmad-output/implementation-artifacts/refactor-stories/r2-1-decide-openapi-generated-contract-ownership.md`

## Bối Cảnh Hiện Tại

Backend đã có SpringDoc:

- Dependency `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8` trong `apps/api/build.gradle.kts`.
- OpenAPI metadata ở `apps/api/src/main/java/com/healthlens/api/config/OpenApiConfig.java`.
- Runtime spec expose tại `/v3/api-docs`; Swagger UI tại `/swagger-ui.html`.

Frontend hiện vẫn dùng API client thủ công:

- `apps/web/src/lib/api/apiClient.ts` là Axios instance chung cho auth cookies, bearer token, refresh retry, CSRF bootstrap, và revoke redirect.
- Route constants và frontend-facing contracts đang nằm trong `packages/shared`, nhưng một số contract active/planned còn có rủi ro drift theo `review/constants-scripts-deep-dive.md`.

Mobile đang deferred, nhưng kiến trúc đã dự kiến Expo app sẽ dùng TypeScript và cần cùng API contract với web khi Phase 2 bắt đầu.

## Quyết Định

Generated OpenAPI contracts thuộc ownership của `packages/shared`, không thuộc riêng `apps/web` hoặc backend build output.

### Output Location

Generated artifacts sẽ nằm tại:

```text
packages/shared/generated/openapi/
```

Quy ước dự kiến:

```text
packages/shared/generated/openapi/
├── index.ts             # Public generated barrel export
├── models/              # DTO/type definitions sinh từ OpenAPI schemas
└── schemas/             # Schema/type helpers nếu generator tạo type-only output
```

`packages/shared/generated/openapi` là generated-only zone. Không viết tay business logic, adapter auth, hoặc app-specific behavior trong folder này.

R2.2 phải cấu hình generator theo hướng **type-only hoặc model-only** trước. Không sinh runtime HTTP client trong `packages/shared` ở bước đầu, vì runtime transport thuộc từng app. Nếu sau này cần sinh runtime client/wrappers, story đó phải thêm decision riêng về dependency/runtime compatibility cho web và Expo mobile trước khi output đó được export.

R2.2 cũng phải cập nhật `packages/shared/tsconfig.json` để include generated source, ví dụ `generated/**/*.ts`, để `pnpm --filter @healthlens/shared build` bắt lỗi TypeScript trong generated contracts.

### Generation Command

Command chính sẽ được thêm ở story R2.2:

```bash
pnpm openapi:generate
```

Command check stale dành cho CI sẽ được thêm ở story R2.6:

```bash
pnpm openapi:check
```

R2.2 sẽ hiện thực command bằng script dưới `scripts/openapi/` và config OpenAPI generator. Source mặc định cho developer local là API đang chạy:

- API local đang chạy: `http://localhost:8080/v3/api-docs`.

Source mặc định cho CI/stale check là một OpenAPI JSON file được capture deterministically trong repo hoặc tạo bởi một bước CI đã được kiểm soát. R2.2/R2.6 phải chọn và document chính xác file/source đó trước khi bật `pnpm openapi:check`; CI không được phụ thuộc mơ hồ vào `localhost:8080` nếu workflow không khởi động API và prerequisites tương ứng.

Command phải fail rõ khi API local không sẵn sàng, spec không tải được, hoặc generator tạo diff không deterministic.

### Commit Policy

Generated OpenAPI files sẽ được commit vào repository.

Lý do:

- `apps/web` và mobile tương lai cần import type ổn định mà không phải chạy generator trước mỗi lần build.
- Review có thể thấy contract diff trực tiếp khi backend đổi DTO/route.
- CI có thể kiểm tra stale output bằng cách regenerate và so sánh working tree.

Policy review:

- PR đổi backend public API phải bao gồm generated diff tương ứng hoặc ghi rõ vì sao không ảnh hưởng OpenAPI output.
- Không review logic thủ công trong generated files như application code; review tập trung vào public contract diff, breaking change, và consumer impact.
- Không sửa tay trong `packages/shared/generated/openapi`; mọi thay đổi phải đi qua generation command.
- Nếu generator version/config đổi, PR phải tách rõ config/tooling diff với contract diff khi khả thi.

### CI Stale Check

CI sẽ chạy `pnpm openapi:check` sau khi R2.6 hoàn tất. Check này phải:

- Regenerate output từ source OpenAPI đã định nghĩa.
- Fail nếu `packages/shared/generated/openapi` có diff chưa commit.
- In hướng dẫn chạy `pnpm openapi:generate` khi stale.
- Dùng source OpenAPI CI đã được chọn ở phần Generation Command, không tự suy đoán giữa local API và captured JSON.
- Có exclusions explicit cho metadata/timestamps nếu generator không deterministic hoàn toàn.

## Consumer Packages Và Boundary

### `packages/shared`

`packages/shared` là package sở hữu contract dùng chung:

- Export generated DTO/types qua namespace ổn định từ `@healthlens/shared/generated/openapi`.
- Không re-export generated OpenAPI types từ root `@healthlens/shared` trong bước đầu, để tránh làm root barrel phình ra và để consumer import boundary rõ ràng.
- Giữ shared schemas/constants thủ công cho những domain chưa được OpenAPI hóa hoặc cần validation UI riêng.
- Không import từ `apps/web`, `apps/mobile`, hoặc backend source.
- Không chứa adapter phụ thuộc UI framework hoặc auth store.

`packages/shared` là boundary giữa generated contract và các app TypeScript.

R2.2 phải đảm bảo package path hoạt động với repo TS path aliases và workspace package resolution. Nếu cần, thêm export file tại `packages/shared/generated/openapi/index.ts` và update package/tsconfig đủ để imports kiểu sau compile được:

```ts
import type { SomeDto } from "@healthlens/shared/generated/openapi";
```

### `apps/web`

`apps/web` là consumer đầu tiên:

- Tiếp tục dùng `apps/web/src/lib/api/apiClient.ts` làm transport/auth layer.
- Import generated types từ `@healthlens/shared/generated/openapi` khi migrate từng feature.
- Không đặt generated OpenAPI output riêng trong `apps/web`.
- Không buộc rewrite toàn bộ API client trong một story; migration sẽ diễn ra theo compatibility/adoption stories.

Nếu generator sinh runtime API wrappers ở một story tương lai, web chỉ dùng chúng khi có adapter rõ để giữ auth refresh, CSRF, và revoke handling hiện tại. Cho R2.2, output mặc định phải tránh runtime HTTP dependency.

### Mobile Tương Lai

Mobile tương lai sẽ consume `@healthlens/shared` giống web:

- Import DTO/types từ `@healthlens/shared/generated/openapi`.
- Tự sở hữu transport/runtime adapter phù hợp với Expo/React Native.
- Không phụ thuộc vào `apps/web/src/lib/api/apiClient.ts`.

Điều này giữ contract dùng chung ở `packages/shared`, nhưng runtime behavior thuộc từng app.

## Alternatives Không Chọn

### Generated Output Trong `apps/web`

Không chọn vì mobile tương lai cũng cần contract. Đặt output trong web sẽ tạo dependency ngược hoặc buộc copy sang mobile.

### Generated Output Trong `apps/api/build`

Không chọn vì build output không phải source package cho TypeScript consumers. Web/mobile sẽ cần bước copy hoặc publish riêng, làm tăng drift và khó review.

### Generated Output Trong Root `generated/`

Không chọn vì package ownership mơ hồ. Consumers TypeScript đang dùng workspace package `@healthlens/shared`, nên root folder sẽ cần thêm alias/build plumbing không cần thiết.

### Chỉ Generate Types, Không Commit Generated Files

Không chọn ở giai đoạn này vì CI/build của web và mobile sẽ phụ thuộc vào generator/API runtime sẵn có. Commit generated files tạo diff review được và giảm setup friction.

### Custom Regex/Compiler Script Từ Java Constants

Không chọn làm hướng chính vì chỉ cover constants/routes rời rạc, không cover DTO schema, request/response body, status, và operation metadata đầy đủ như OpenAPI.

### Full OpenAPI Client Migration Ngay

Không chọn vì `apps/web/src/lib/api/apiClient.ts` đang chứa auth/CSRF/refresh behavior quan trọng. Epic R2 chỉ đặt nền contract sync và migrate dần theo feature, không rewrite toàn bộ client trong một PR.

## Hệ Quả

- R2.2 phải tạo generation script/config trỏ output vào `packages/shared/generated/openapi`.
- R2.2 phải update `packages/shared` export/tsconfig boundary để generated files được type-check và import được qua `@healthlens/shared/generated/openapi`.
- R2.3-R2.5 có thể dọn active/planned shared constants mà không trộn với generated adoption.
- R2.6 phải hiện thực stale check dựa trên command, CI OpenAPI source, và output location đã chốt ở quyết định này.
- Backend route/DTO thay đổi sau khi generation tồn tại phải được xem là contract change có consumer impact.
