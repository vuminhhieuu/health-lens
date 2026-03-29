# Viết Pull Request Documentation

Nhiệm vụ của bạn là sinh ra tiêu đề và mô tả Pull Request chuyên nghiệp (trong pull request description các heading là tiếng Anh, còn nội dung chính là tiếng Việt).

## Dữ liệu đầu vào

1. **Context:** Issue ID, Story ID, Branch Name.
2. **User Story:** Mục đích của task, Acceptance Criteria.
3. **Git Diff:** Tổng hợp các file đã thay đổi.

## Quy tắc định dạng

### PR Title
- Giống format commit message: `<type>(<issue-id>): <subject>`

### PR Description
Sử dụng cấu trúc Markdown sau:

Fixes {ISSUE-ID-UPPERCASE}

### Overview
[Tóm tắt mục đích chính của PR này dựa trên User Story]

### ⚠️ Breaking Changes
[Nếu phát hiện Breaking Changes, hãy liệt kê và mô tả ảnh hưởng tại đây. Nếu không có, hãy xóa phần này]
- [Loại thay đổi]: [Ảnh hưởng tới các component/module khác]

### Design Reference
[Nếu User Story có link Google Stitch (HTML prototype, Screenshot), hãy liệt kê tại đây]
- **Design:** [Tên screen]
- **HTML Prototype:** [Link]
- **Screenshot:** [Link]

### Tasks / Subtasks
[Liệt kê các Task và Subtask đã thực hiện từ User Story]
- [x] Task 1: [Tiêu đề]
  - [x] [Subtask 1.1]
- [ ] Task 2: [Tiêu đề]

### Changes
[Liệt kê danh sách các thay đổi chính dựa trên Git Diff và User Story]
- File/Component A: [Mô tả thay đổi]
- File/Component B: [Mô tả thay đổi]

### Proof of Work
#### Visuals (Screenshots or Videos)
[Chèn screenshot hoặc video minh họa tại đây]

#### Testing
- [Mô tả cách thức bạn đã test (hoặc đề xuất người review test)]

## Ràng buộc
- Đảm bảo tính chuyên nghiệp và minh bạch.
- Sử dụng ngôn ngữ thống nhất với dự án (mặc định là tiếng Anh cho nội dung technical nếu không có yêu cầu khác).
- BẮT BUỘC có "Fixes {ISSUE-ID}" ngay dòng đầu tiên của mô tả.

## Kết quả đầu ra
Trả về nội dung PR đầy đủ (Title & Description) trong một khối Markdown duy nhất.
