# Viết Commit Message

Nhiệm vụ của bạn là sinh ra một thông điệp commit chuẩn Conventional Commits, dựa trên bối cảnh từ Git Diff và User Story được cung cấp.

## Dữ liệu đầu vào

1. **Bối cảnh trích xuất từ `extract_context.py`:** Loại thay đổi, Issue ID, Story ID.
2. **User Story:** Các yêu cầu, tiêu chí chấp nhận (Acceptance Criteria), và ràng buộc kỹ thuật.
3. **Git Diff:** Các thay đổi thực tế trong phiên làm việc hiện tại.

## Quy tắc định dạng

- **Format:** `<type>(<issue-id>): <subject>`
- **Type:** Lấy từ User Story hoặc suy luận từ bối cảnh:
    - `feat`: Thêm tính năng mới.
    - `fix`: Sửa lỗi.
    - `refactor`: Tái cấu trúc code (không đổi chức năng).
    - `docs`: Thay đổi tài liệu.
    - `test`: Thêm/sửa unit test.
    - `chore`: Các tác vụ nhỏ không liên quan tới source code.
- **Scope:** BẮT BUỘC dùng `{issue-id}` trích xuất được.
- **Subject:** Ngắn gọn, mô tả đúng thay đổi cốt lõi. Bắt đầu bằng động từ ở thì hiện tại (ví dụ: `add`, `update`, `refactor`).

### Ví dụ
`feat(lin-12): add user registration endpoint`

#### Ví dụ Breaking Change
```
feat(lin-12): refactor auth provider interface

BREAKING CHANGE: The AuthProvider interface now requires an additional 'tenantId' parameter in all methods.
```

## Ràng buộc

- **Breaking Changes:** Nếu bối cảnh trích xuất có `Breaking Changes: DETECTED` (ví dụ xóa file core, đổi schema DB), bạn BẮT BUỘC phải thêm dòng `BREAKING CHANGE:` ở cuối commit message kèm mô tả ngắn gọn về ảnh hưởng.

- KHÔNG ĐƯỢC phép bịa đặt các thay đổi không có trong `git diff`.
- Ưu tiên các từ vựng kỹ thuật được sử dụng trong User Story.
- Nội dung phải súc tích, chuyên nghiệp.

## Kết quả đầu ra
Chỉ trả về thông điệp commit cuối cùng, không kèm theo giải thích hay nội dung khác.
