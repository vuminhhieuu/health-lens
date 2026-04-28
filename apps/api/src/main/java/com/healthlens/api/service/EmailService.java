package com.healthlens.api.service;

import com.healthlens.api.entity.User;
import com.healthlens.api.entity.DataDeletionRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:no-reply@healthlens.vn}")
    private String fromAddress;

    @Value("${app.frontend.verification-url:http://localhost:3000/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.frontend.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, ObjectProvider<TemplateEngine> templateEngineProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.templateEngine = templateEngineProvider.getIfAvailable();
    }

    public void sendVerificationEmail(User user, String token) {
        log.info("[EmailService] Attempting to send verification email to: {}", user.getEmail());
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send verification email to {}", user.getEmail());
            return;
        }

        String verificationLink = verificationBaseUrl + "?token=" + token;
        String htmlContent = renderVerificationTemplate(verificationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Xác thực email đăng ký");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Verification email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send verification email to {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email xac thuc that bai", e);
        }
    }

    public void sendPasswordResetEmail(User user, String token) {
        log.info("[EmailService] Attempting to send password reset email to: {}", user.getEmail());
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send password reset email to {}", user.getEmail());
            return;
        }

        String resetLink = resetPasswordBaseUrl + "?token=" + token;
        String htmlContent = """
                <html>
                  <body style=\"font-family: Arial, sans-serif; color: #111827;\">
                    <h2> HealthLens - Dat lai mat khau</h2>
                    <p>Chao %s,</p>
                    <p>Chung toi da nhan duoc yeu cau dat lai mat khau cho tai khoan cua ban.</p>
                    <p>Vui long bam vao lien ket ben duoi de thuc hien (hieu luc trong 1 gio):</p>
                    <p><a href=\"%s\" style=\"background-color: #00685f; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Dat lai mat khau</a></p>
                    <p>Neu ban khong yeu cau dat lai mat khau, hay bo qua email nay. Mat khau cua ban se khong thay doi cho den khi ban truy cap vao lien ket tren va tao mat khau moi.</p>
                  </body>
                </html>
                """.formatted(user.getFullName(), resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Dat lai mat khau");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Password reset email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send password reset email to {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email dat lai mat khau that bai", e);
        }
    }

    public void sendDeletionConfirmationEmail(User user, DataDeletionRequest deletionRequest, String cancellationLink) {
        log.info("[EmailService] Attempting to send deletion confirmation email to: {}", user.getEmail());
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send deletion confirmation email to {}", user.getEmail());
            return;
        }

        String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #111827;">
                    <h2>HealthLens - Xác nhận yêu cầu xóa tài khoản</h2>
                    <p>Chào %s,</p>
                    <p>Chúng tôi đã nhận được yêu cầu xóa tài khoản của bạn theo Nghị định 13/2023/NĐ-CP. Tài khoản và toàn bộ dữ liệu sẽ bị xóa vĩnh viễn sau 72 giờ.</p>
                    <p><strong>Thời gian xóa dự kiến:</strong> %s</p>
                    <p><strong>Dữ liệu sẽ bị xóa vĩnh viễn:</strong></p>
                    <ul>
                      <li>Thông tin cá nhân (họ tên, ngày sinh, giới tính, email)</li>
                      <li>Tất cả hồ sơ sức khỏe và kết quả xét nghiệm</li>
                      <li>Tất cả tệp PDF, ảnh kết quả khám đã tải lên</li>
                      <li>Lịch sử đồng ý xử lý dữ liệu (consent logs)</li>
                      <li>Phiên đăng nhập, token làm mới và token đặt lại mật khẩu</li>
                    </ul>
                    <p>Trong vòng 72 giờ tới bạn có thể hủy yêu cầu này bằng cách bấm vào liên kết bên dưới:</p>
                    <p><a href="%s" style="background-color: #00685f; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Hủy yêu cầu xóa tài khoản</a></p>
                    <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bấm vào liên kết hủy phía trên ngay lập tức và liên hệ đội hỗ trợ.</p>
                    <p>Xin cảm ơn,<br>Đội HealthLens</p>
                  </body>
                </html>
                """.formatted(user.getFullName(), deletionRequest.getScheduledDeletionAt(), cancellationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Xác nhận yêu cầu xóa tài khoản");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Deletion confirmation email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send deletion confirmation email to {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email xac nhan xoa tai khoan that bai", e);
        }
    }

    public void sendCancellationConfirmationEmail(User user) {
        log.info("[EmailService] Attempting to send cancellation confirmation email to: {}", user.getEmail());
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send cancellation confirmation email to {}", user.getEmail());
            return;
        }

        String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #111827;">
                    <h2>HealthLens - Hủy yêu cầu xóa tài khoản</h2>
                    <p>Chào %s,</p>
                    <p>Yêu cầu xóa tài khoản của bạn đã được hủy thành công. Tài khoản của bạn hiện tại đã được khôi phục và hoạt động bình thường.</p>
                    <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với đội hỗ trợ của chúng tôi.</p>
                    <p>Xin cảm ơn,<br>Đội HealthLens</p>
                  </body>
                </html>
                """.formatted(user.getFullName());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Yêu cầu xóa tài khoản đã được hủy");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Cancellation confirmation email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send cancellation confirmation email to {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email xac nhan huy xoa tai khoan that bai", e);
        }
    }

    public void sendDeletionCompletionEmail(User user) {
        log.info("[EmailService] Attempting to send deletion completion email to: {}", user.getEmail());
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send deletion completion email to {}", user.getEmail());
            return;
        }

        String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #111827;">
                    <h2>HealthLens - Xác nhận xóa tài khoản hoàn tất</h2>
                    <p>Chào,</p>
                    <p>Yêu cầu xóa tài khoản của bạn đã được hoàn tất. Tài khoản của bạn và toàn bộ dữ liệu cá nhân liên quan đã bị xóa vĩnh viễn khỏi hệ thống của chúng tôi.</p>
                    <p>Dữ liệu có thể mất từ 7-30 ngày để bị xóa hoàn toàn từ các bản sao lưu.</p>
                    <p>Nếu bạn có bất kỳ câu hỏi nào, vui lòng liên hệ với đội hỗ trợ của chúng tôi.</p>
                    <p>Xin cảm ơn,<br>Đội HealthLens</p>
                  </body>
                </html>
                """;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Xóa tài khoản hoàn tất");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Deletion completion email sent successfully to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send deletion completion email to {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email xac nhan xoa tai khoan hoan tat that bai", e);
        }
    }

    private String renderVerificationTemplate(String verificationLink) {
        if (templateEngine == null) {
            return """
                    <html>
                      <body style="font-family: Arial, sans-serif; color: #111827;">
                        <h2>HealthLens</h2>
                        <p>Cảm ơn bạn đã đăng ký. Vui lòng xác thực email:</p>
                        <p><a href="%s">Xác thực email</a></p>
                      </body>
                    </html>
                    """.formatted(verificationLink);
        }

        Context context = new Context();
        context.setVariable("verificationLink", verificationLink);
        return templateEngine.process("email/verification", context);
    }
}
