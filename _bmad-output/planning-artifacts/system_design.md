# HealthLens: Exhaustive System Design Documentation

Tài liệu này cung cấp cái nhìn chi tiết và toàn diện về kiến trúc hệ thống HealthLens, được chia thành các góc nhìn (views) kỹ thuật cụ thể.

## 1. Cụm Thành phần Kỹ thuật (C4 Container Diagram)

Mô tả các khối kỹ thuật lớn, ranh giới và cách chúng tương tác qua giao thức mạng.

```mermaid
graph TB
    subgraph Users ["User Layer"]
        Patient["Bệnh nhân (Mobile/Web)"]
        Family["Thành viên gia đình (Web)"]
        Admin["Quản trị viên (Web Admin)"]
    end

    subgraph Entry ["Exposure & Entry Layer"]
        Ingress["Nginx Ingress / K8s"]
    end

    subgraph App ["Application Layer"]
        Web["Next.js Web App (Dashboard & Admin)"]
        Mobile["Expo Mobile App (App & Upload)"]
        Shared["packages/shared (Types, Schemas)"]
    end

    subgraph Core ["Service Layer"]
        Spring["Spring Boot API (Monolith)"]
    end

    subgraph AI ["AI Processing Layer"]
        Ollama["Ollama (Qwen 3.5 & Embeddings)"]
        Paddle["PaddleOCR (Self-hosted OCR)"]
    end

    subgraph Data ["Data & Persistence Layer"]
        Postgres[(PostgreSQL 16 - JSONB)]
        Redis[(Redis 7 - Queue/Cache)]
        Qdrant[(Qdrant - Vector DB)]
        Storage["MinIO / AWS S3"]
    end

    %% Flow
    Patient & Family & Admin --> App
    Web & Mobile -- "HTTPS / JWT" --> Ingress
    Ingress --> Spring
    
    Spring -- "Pre-signed URL" --> Storage
    Spring -- "Redis Streams" --> Paddle
    Spring -- "Semantic RAG" --> Ollama
    Ollama -- "Query" --> Qdrant
    
    Spring --> Postgres
    Spring --> Redis
```

---

## 2. Kiến trúc Thành phần Backend (Backend Component Architecture)

Chi tiết cấu trúc bên trong ứng dụng Spring Boot theo mô hình **Layered Architecture**.

```mermaid
graph TD
    subgraph Controller ["Controller Layer (REST)"]
        C1["AuthController"]
        C2["ProfileController"]
        C3["HealthRecordController"]
        C4["AdminController"]
        V["Validation (JSR-380 / Bean)"]
    end

    subgraph Service ["Service Layer (Business Logic)"]
        S1["AuthService"]
        S2["ProfileService"]
        S3["HealthRecordService"]
        S4["OcrService (Async)"]
        S5["LlmService (RAG)"]
        S6["StorageService (S3)"]
    end

    subgraph Repository ["Repository Layer (JPA)"]
        R1["UserRepository"]
        R2["ProfileRepository"]
        R3["HealthRecordRepository"]
        R4["RefDataRepository"]
    end

    subgraph Entities ["Domain & State"]
        E1["JPA Entities (JSONB Support)"]
        D1["DTOs (DTO Pattern)"]
        M1["Mappers (Entity <--> DTO)"]
    end

    subgraph CrossCutting ["Cross-cutting Concerns"]
        Sec["Spring Security (JWT)"]
        Audit["Audit Logging (AOP)"]
        Ex["Global Ex Handling (RFC 7807)"]
    end

    %% Connections
    Controller --> Service
    Service --> Repository
    Repository --> E1
    Service --> D1
    
    CrossCutting -.-> Controller
    CrossCutting -.-> Service
```

---

## 3. Luồng Dữ liệu AI Chuyên sâu (AI Data Pipeline Sequence)

Mô tả các bước biến đổi dữ liệu từ file ảnh thô thành thông tin giải thích chuyên sâu.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Web/Mobile App
    participant API as Spring Boot Service
    participant S3 as MinIO / S3
    participant Redis as Redis Streams (Queue)
    participant OCR as PaddleOCR Worker
    participant LLM as Ollama (Qwen 3.5)
    participant Vector as Qdrant (Vector DB)

    Client->>API: 1. Request Upload (Pre-signed URL)
    API-->>Client: 2. URL Response
    Client->>S3: 3. PUT File directly
    Client->>API: 4. Notify Upload Done
    
    Note over API, OCR: Trích xuất OCR (Giai đoạn 1)
    API->>Redis: 5. Enqueue OCR Job
    Redis->>OCR: 6. Consume Job
    OCR->>S3: 7. Fetch Image
    OCR-->>API: 8. Return Raw Text / Layout
    
    Note over API, LLM: Giải thích & RAG (Giai đoạn 2)
    API->>LLM: 9. Prompt: Parse & Interpret Raw Text
    LLM->>Vector: 10. Semantic Search for Reference Range
    Vector-->>LLM: 11. Return Medical Guidelines
    LLM-->>API: 12. Return Structured Health Info
    
    API->>Client: 13. Push Result (via Polling/Socket)
    Client-->>API: 14. User Review & Save
```

---

## 4. Kiến trúc Frontend & State Management

Phân rã cách ứng dụng Web (Next.js) và Mobile (Expo) quản lý dữ liệu và chia sẻ logic.

```mermaid
graph TD
    subgraph Shared ["Shared Logic (packages/shared)"]
        Types["Types / Interfaces"]
        Zod["Zod Validation Schemas"]
        Const["Constants / Error Codes"]
    end

    subgraph Store ["State Management"]
        RQ["TanStack Query (Server State)"]
        Zustand["Zustand (Local UI State)"]
    end

    subgraph UI ["UI Components"]
        Shadcn["Shadcn/ui (Web)"]
        CustomRN["Custom RN UI (Mobile)"]
    end

    subgraph Logic ["Feature Hooks"]
        useAuth["useAuth"]
        useHealth["useHealthRecords"]
        useUpload["useUpload"]
    end

    %% Data Flow
    Shared --> Store
    Shared --> Logic
    Store --> Logic
    Logic --> UI
    
    RQ -- "Axios / Fetch" --> API["Backend API"]
```

---

## 5. Hạ tầng Triển khai & Mạng (Deployment & Networking)

Mô hình vận hành của hệ thống trong môi trường container hóa.

```mermaid
graph TD
    Internet((Internet)) --> Ingress["K8s Ingress / Nginx Proxy"]
    
    subgraph K8s ["Kubernetes Cluster / Docker Network"]
        direction TB
        
        SvcWeb["Web App Service"]
        SvcAPI["API Service"]
        SvcAI["AI Services Pods"]
        
        subgraph Pods ["Application Pods"]
            WebNode["Node.js (Next.js)"]
            APIBoot["Java JVM (Spring Boot)"]
        end

        subgraph Infra ["Infrastructure Pods"]
            PG[(PostgreSQL)]
            RD[(Redis)]
            QD[(Qdrant)]
        end
        
        subgraph AIService ["AI Engine Containers"]
            OL["Ollama (LLM)"]
            PO["PaddleOCR"]
        end
    end

    Ingress --> SvcWeb & SvcAPI
    SvcWeb --> WebNode
    SvcAPI --> APIBoot
    
    APIBoot --> PG & RD & QD
    APIBoot --> OL & PO
```

---

## 6. Mô hình Dữ liệu & Quyền Sở hữu (Data Model & Ownership)

Sơ đồ ERD rút gọn tập trung vào cấu trúc đa hồ sơ và tính bảo mật.

```mermaid
erDiagram
    USER ||--o{ ACCOUNT : "has"
    USER ||--o{ PROFILE : "owns (multi-profile)"
    USER ||--o{ USER_CONSENT : "grants"
    
    PROFILE ||--o{ HEALTH_RECORD : "contains"
    PROFILE ||--o{ PROFILE_SHARE : "shared with"
    
    HEALTH_RECORD {
        UUID id PK
        UUID profile_id FK
        DATE exam_date
        JSONB metrics "Health metrics array"
        JSONB raw_ocr "Original OCR text"
        VARCHAR source "ocr / manual"
        TIMESTAMP created_at
    }

    PROFILE_SHARE {
        UUID id PK
        UUID profile_id FK
        EMAIL shared_with
        VARCHAR role "viewer"
        VARCHAR status "pending/accepted"
    }

    REFERENCE_DATA {
        UUID id PK
        VARCHAR metric_type "HbA1c / LDL"
        JSONB thresholds "Age/Gender rules"
        TIMESTAMP updated_at
    }

    AUDIT_LOG {
        UUID id PK
        UUID user_id FK
        VARCHAR action "view / edit"
        VARCHAR resource_type
        JSONB diff
        TIMESTAMP timestamp
    }
```

---

## 7. Các Quyết định Kiến trúc Chính (Key Architecture Decisions)

*   **Platform Focus**: Web-First MVP cho Phase 1; Mobile camera/offline deferred sang Phase 2.
*   **AI Philosophy**: Ưu tiên **Self-hosted (Local AI)** qua Ollama và PaddleOCR để tối ưu chi phí ($0 runtime) và bảo vệ dữ liệu y tế nhạy cảm cho người dùng Việt Nam.
*   **Security Model**: TOTP MFA bắt buộc cho Admin Panel; RBAC kết hợp Resource Ownership Checking cho mọi request hồ sơ.
*   **Scale Plan**: Thiết kế Stateless API để sẵn sàng mở rộng ngang (Horizontal Scaling) qua Kubernetes.
