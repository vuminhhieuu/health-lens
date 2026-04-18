---
status: done
generated: 2026-04-18
last_updated: 2026-04-18T22:00:00+07:00
epic: 2
epic_title: "Quản Lý Hồ Sơ Sức Khỏe Cá Nhân"
story_number: 4
story_title: "Cải thiện hệ thống Email (Brevo + Event-Driven + Verification Fix + Templates)"
user_story: |
  Là một người dùng HealthLens,
  Tôi muốn hệ thống email hoạt động ổn định, nhanh chóng và chuyên nghiệp,
  Để tôi nhận được email xác thực, thông báo một cách kịp thời và đáng tin cậy.
acceptances: |
  # Task 1: Chuyển từ Resend sang Brevo

  **Given** người dùng đăng ký tài khoản mới
  **When** hệ thống gửi email xác thực
  **Then** email được gửi qua Brevo SMTP và nhận được trong inbox

  **Given** môi trường dev/staging/prod
  **When** cấu hình email trong .env và application.yml
  **Then** sử dụng Brevo credentials tương ứng

  # Task 2: Implement Event-Driven cho Email (Async)

  **Given** người dùng submit form đăng ký
  **When** AuthService.register() được gọi
  **Then** API trả về response trong vòng < 500ms
  **And** không đợi email gửi xong

  **Given** event đã được publish vào Redis Stream
  **When** EmailConsumer nhận event
  **Then** gửi email bất đồng bộ sau 1-2 giây

  # Task 3: Fix Email Verification Endpoint

  **Given** token xác thực hợp lệ
  **When** POST /api/v1/auth/verify-email với token đúng
  **Then** trả về 200 OK và user.emailVerified = true

  **Given** token không hợp lệ hoặc đã hết hạn
  **When** POST /api/v1/auth/verify-email
  **Then** trả về 400 Error với message rõ ràng

  **Given** frontend nhận response
  **When** verification thành công
  **Then** hiển thị thông báo thành công và chuyển về login

  # Task 4: Cải thiện Email Templates

  **Given** email xác thực được gửi
  **When** người dùng mở email trên mobile
  **Then** template hiển thị đúng (max-width: 600px, responsive)

  **Given** email xác thực được gửi
  **When** người dùng xem email
  **Then** có HealthLens branding nhất quán với màu #10B981

  # Task 5: Chuẩn hóa quản lý ENV bằng Infisical

  **Given** team cần đồng bộ biến môi trường giữa nhiều thành viên
  **When** secret được cập nhật trên Infisical Cloud
  **Then** thành viên có thể pull về local và chạy dự án với cấu hình mới nhất
  
---
dependencies: ["2-1-update-account-personal-info"]
story_points: 8
priority: high
sprint: 1

# Story Context

## Background

Hệ thống email hiện tại có 4 vấn đề cần giải quyết:

1. **Email Provider**: Đang dùng Resend - cần chuyển sang Brevo để hoạt động ổn định hơn khi chưa cấu hình đầy đủ
2. **Event-Driven**: Email được gửi đồng bộ trong AuthService.register() - block API response
3. **Verification 404**: Endpoint /api/v1/auth/verify-email chưa được implement đúng
4. **Templates**: Email template hiện tại basic, cần professional hơn

## Technical Context

**Current Email Flow:**
```text
AuthController.register()
  → AuthService.register()
    → UserRepository.save()
    → EmailService.sendVerificationEmail() [SYNCHRONOUS - BLOCKS]
    → return response
```

**Target Email Flow:**
```text
AuthController.register()
  → AuthService.register()
    → UserRepository.save()
    → publish event to Redis Stream "email.events"
    → return response [IMMEDIATE]

[Background]
  → EmailConsumer listens on "email.events"
    → EmailService.sendVerificationEmail() [ASYNC]
```

## UI Design Reference (Stitch)

- Screen: `projects/578519912546445367/screens/11b790b3cb2743dfa361a26613777649`
- Title: `Xác thực email thành công - HealthLens`
- Screenshot URL: `https://lh3.googleusercontent.com/aida/ADBb0uhxP8R32SYIe2WBY80cIlvLYSu-HmW3FobEByo4hh_qtCMdKh0QgDRLeywe-LOdUsZ1KZcTAwcV_ca0lTdjANK5-7lNDqp4RObnZ02fNcMyBL9wjU-t1ZtaXxXdAO8DBCOZEY2ky_ywQAuUSAAhDaeIb3yQCiWhfuatFFoGgPn-rv3K_lk5pY1pOi8cbtkhuiYWqafjaQCULY1pMnptT0jHKxeJLHlwMKgEAfagjpKWoBGenhGtHnsWDg`
- HTML URL: `https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2ZhZTJhNmMxNjZkNDQwNTlhYWMyYWU2YjdkZDlhMzVlEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242`

## Files cần sửa

### Backend (apps/api)

| File | Action | Description |
| --- | --- | --- |
| `.env` | Sửa | Update MAIL_* sang Brevo |
| `application.yml` | Sửa | Update spring.mail config |
| `EmailService.java` | Sửa | Add Brevo support, create templates |
| `AuthService.java` | Sửa | Publish event instead of direct call |
| `AuthController.java` | Thêm | Verify email endpoint |
| `EmailEvent.java` | Thêm | Event payload DTO |
| `RedisConfig.java` | Sửa | Add Stream config |
| `EmailConsumer.java` | Thêm | Consumer for async email |

### Frontend (apps/web)

| File | Action | Description |
| --- | --- | --- |
| `verify-email/page.tsx` | Sửa | Handle verification API |

## Implementation Tasks

### Task 1: Switch to Brevo

**Steps:**
1. Đăng ký tài khoản Brevo ([brevo.com](https://www.brevo.com))
2. Lấy SMTP credentials từ Brevo dashboard
3. Cập nhật `.env`:
   ```
   MAIL_DRIVER=smtp
   MAIL_HOST=smtp-relay.brevo.com
   MAIL_PORT=587
   MAIL_USERNAME=<brevo-email>
   MAIL_PASSWORD=<brevo-smtp-key>
   MAIL_FROM=HealthLens <contact@healthlens.vn>
   ```
4. Cập nhật `application.yml` tương ứng với profile dev/staging/prod
5. Test gửi email với cùng logic hiện tại

**Lưu ý:** Spring mail tự động detect SMTP host nên không cần thay đổi code EmailService.java về cấu hình.

### Task 2: Event-Driven Email với Redis Streams

**Step 1: Thêm event publishing trong AuthService**

```java
// AuthService.java - sau khi lưu user
@Value("${app.stream.email-events:email.events}")
private String emailEventStream;

public User register(RegisterRequest request) {
    // ... validate và create user
    User savedUser = userRepository.save(user);
    
    // Publish event thay vì gọi trực tiếp
    EmailEvent event = new EmailEvent(
        "verification",
        savedUser.getId(),
        savedUser.getEmail(),
        Map.of("token", generateVerificationToken(savedUser))
    );
    redisTemplate.opsForStream().add(
        StreamRecords.newRecord()
            .in(emailEventStream)
            .ofMap(event.toMap())
    );
    
    return savedUser;
}
```

**Step 2: Tạo EmailEvent DTO**

```java
// EmailEvent.java
public record EmailEvent(
    String eventType,      // "verification", "password-reset", "welcome"
    UUID userId,
    String email,
    Map<String, Object> data
) {}
```

**Step 3: Tạo EmailConsumer**

```java
// EmailConsumer.java
@Service
public class EmailConsumer {

    @PostConstruct
    public void subscribe() {
        // Listen on emailEventStream
        // Process email based on eventType
    }
    
    @EventListener
    public void handleEmailEvent(EmailEvent event) {
        switch (event.eventType()) {
            case "verification" -> sendVerificationEmail(event);
            case "password-reset" -> sendPasswordResetEmail(event);
            // etc
        }
    }
}
```

### Task 3: Fix Verification Endpoint

**Step 1: Thêm controller method**

```java
// AuthController.java
@PostMapping("/verify-email")
public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
    authService.verifyEmail(request.token());
    return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
}
```

**Step 2: Implement service method**

```java
// AuthService.java
public void verifyEmail(String token) {
    EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
        .orElseThrow(() -> new ValidationException("Invalid token"));
    
    if (verificationToken.isExpired()) {
        throw new ValidationException("Token expired");
    }
    
    User user = verificationToken.getUser();
    user.setEmailVerified(true);
    userRepository.save(user);
    
    tokenRepository.delete(verificationToken);
}
```

### Task 4: Professional Email Templates

**Verification Email Template:**

```html
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Xác Thực Email - HealthLens</title>
  <style>
    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background: #f5f5f5; }
    .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .header { background: linear-gradient(135deg, #10B981 0%, #059669 100%); padding: 32px; text-align: center; }
    .header h1 { color: #ffffff; margin: 0; font-size: 28px; }
    .content { padding: 32px; }
    .button { display: inline-block; background: #10B981; color: #ffffff; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; margin: 24px 0; }
    .footer { background: #f9fafb; padding: 24px; text-align: center; font-size: 13px; color: #6b7280; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>HealthLens</h1>
    </div>
    <div class="content">
      <h2>Xác Thực Email</h2>
      <p>Xin chào,</p>
      <p>Cảm ơn bạn đã đăng ký HealthLens. Vui lòng xác thực email của bạn để hoàn tất đăng ký:</p>
      <div style="text-align: center;">
        <a href="{{verificationLink}}" class="button">Xác Thực Email</a>
      </div>
      <p style="font-size: 14px; color: #6b7280; margin-top: 24px;">
        Link có hiệu lực trong 24 giờ. Nếu bạn không đăng ký tài khoản, vui lòng bỏ qua email này.
      </p>
    </div>
    <div class="footer">
      <p>HealthLens - Theo dõi sức khỏe của bạn</p>
      <p>© 2026 HealthLens</p>
    </div>
  </div>
</body>
</html>
```

### Task 5: Setup Infisical cho quản lý secrets

**Steps:**

1. Tạo project `health-lens` trên Infisical Cloud với 3 môi trường: `dev`, `staging`, `production`
2. Import keys từ `.env`, `.env.staging.api`, `.env.production` lên đúng môi trường
3. Thiết lập quyền:
   - Tech lead: write
   - Team members: read-only
4. Giữ `.env.example` trong git làm schema chuẩn, không lưu secret thật
5. Team dùng Infisical CLI để pull hoặc run local:
   - `infisical run --env=dev -- <command>`
   - hoặc pull về file `.env` local theo nhu cầu
6. Khi thay đổi secret: update trên Infisical, thông báo qua Slack/PR description để mọi người pull lại

## Acceptance Criteria Testing

### Task 1: Brevo

| Test Case | Expected Result | Status |
| --- | --- | --- |
| Gửi email đăng ký mới | Email nhận trong inbox (check spam) | pending |
| Verify Brevo credentials hoạt động | Kết nối thành công | pending |

### Task 2: Event-Driven

| Test Case | Expected Result | Status |
| --- | --- | --- |
| Đo thời gian register API | Response < 500ms | pending |
| Kiểm tra email vẫn được gửi | Email async sau 1-2s | pending |
| Test khi email service down | Event vẫn publish, không throw | pending |

### Task 3: Verification Endpoint

| Test Case | Expected Result | Status |
| --- | --- | --- |
| Verify với valid token | 200 OK, emailVerified = true | pending |
| Verify với invalid token | 400 Error | pending |
| Verify với expired token | 400 Error | pending |

### Task 4: Templates

| Test Case | Expected Result | Status |
| --- | --- | --- |
| Test responsive trên mobile | Hiển thị đúng | pending |
| Check màu button | #10B981 đúng | pending |
| Check branding | Nhất quán | pending |

### Task 5: Infisical

| Test Case | Expected Result | Status |
| --- | --- | --- |
| Member pull secrets từ Infisical | Local ENV được cập nhật đúng | pending |
| Quyền member thường update secret | Bị từ chối (read-only) | pending |
| Update secret + thông báo team | Team pull được và chạy lại thành công | pending |