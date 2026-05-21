# Validation Report: Story 7.9

Status: validated  
Date: 2026-05-20  
Target Story: `7-9-confirm-and-document-event-driven-architecture.md`

## Validation Outcome

**Result:** Ready with conditions

Story `7-9` đã đạt mức khá tốt cho một story định hướng kiến trúc, nhưng chưa phải loại implementation story "pick lên là code ngay". Nó phù hợp để một agent hoặc technical lead thực hiện như một **decision/documentation story**, miễn là giữ đúng scope và không vô tình trôi sang implementation của `7-10` hoặc `7-11`.

## What Is Strong

1. Story có mục tiêu rõ: chốt rule kiến trúc event-driven trước khi làm refactor backend sâu hơn.
2. Story đã chỉ rõ non-goals ở mức đủ tốt: không move package, không introduce stream abstraction, không migrate email trong story này.
3. Acceptance criteria đã bao phủ các quyết định kiến trúc quan trọng:
   - Redis Streams có phải chuẩn chung không
   - email có phải chuyển hết sang event-driven không
   - boundary `events.*`, `ocr.*`, `ai.*` sẽ ra sao
4. Story đã có references và likely files đúng với evidence đã review.
5. Story được đặt đúng thứ tự trước `7-1`, nên hợp logic sprint mới.

## Findings

1. Story chưa chỉ ra **artifact đầu ra chính thức** phải là gì. Hiện mới nói "documented" nhưng chưa khóa rõ sẽ cập nhật file nào là source of truth.
2. Story chưa nói rõ **ai có authority chốt decision** nếu có nhiều phương án cạnh tranh như `Redis Streams only for OCR/email` hay `all durable async tasks`.
3. Acceptance criteria nói về "documented rule" nhưng chưa có tiêu chí đủ cứng cho phần **decision table**: nên yêu cầu ít nhất các cột `task category`, `current state`, `chosen model`, `reason`, `follow-up story`.
4. Story chưa nêu rõ **output phải feed ngược vào đâu** ngoài “Epic 7 guidance”. Nên tối thiểu phải cập nhật:
   - `source-code-architecture-review.md`
   - `epic-7-issues-and-proposed-stories.md`
   - và một doc kiến trúc hoặc ADR ngắn trong `docs/`
5. Story là decision story nhưng đang để `ready-for-dev`, điều này chấp nhận được, tuy nhiên dev agent cầm vào phải hiểu đây là **documentation/architecture execution**, không phải code feature.

## Recommended Conditions Before Execution

1. Khi thực hiện story này, coi deliverable bắt buộc là:
   - một decision section rõ ràng trong [source-code-architecture-review.md](source-code-architecture-review.md)
   - một bản backlog/order update trong [epic-7-issues-and-proposed-stories.md](epic-7-issues-and-proposed-stories.md)
   - và một doc kiến trúc ngắn trong `docs/` hoặc planning artifacts
2. Decision table nên bắt buộc cover ít nhất 5 category:
   - OCR jobs
   - user-facing emails
   - follow-up reminders
   - audit events
   - future notifications/analytics events
3. Mỗi category nên chốt:
   - synchronous
   - after-commit direct
   - DB-claimed scheduled job
   - Redis Stream event
4. Nếu story gặp quyết định vượt authority của code review hiện tại, output phải ghi rõ:
   - proposed default
   - unresolved question
   - downstream story blocked or not blocked

## Final Gate

**Decision:** Pass with conditions

`7-9` đủ tốt để đi tiếp và đúng là story nên làm đầu tiên trong Epic 7 đã mở rộng. Khuyến nghị là khi bước vào execution, agent thực hiện story này nên xem đây là một **ADR-style alignment story** và phải kết thúc bằng decision record rõ ràng, không chỉ là prose discussion.
