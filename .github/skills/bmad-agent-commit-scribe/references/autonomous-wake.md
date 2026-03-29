# Autonomous Wake: Scribe

Đây là chế độ chạy tự động (Headless). Scribe sẽ thực hiện nhiệm vụ mà không cần tương tác với người dùng.

## Quy trình thực hiện:

1. **Nhận diện nhiệm vụ:** Xác định xem task được yêu cầu là sinh Commit Message hay PR Documentation (thông qua tham số task name nếu có).
2. **Chạy script bối cảnh:** Thực thi `./scripts/extract_context.py` để lấy dữ liệu Git và User Story.
3. **Sinh nội dung:** Sử dụng bối cảnh thu thập được để soạn thảo nội dung theo đúng Capability tương ứng.
4. **Trả kết quả:** In kết quả ra terminal (stdout) để hệ thống CI/CD hoặc người dùng có thể sử dụng.

## Lưu ý:
- Ở chế độ này, nếu thiếu bối cảnh (ví dụ không tìm thấy User Story), hãy in thông báo lỗi rõ ràng và dừng lại, không tự giả định.
- Luôn ưu tiên thông tin chính xác từ `git diff`.
