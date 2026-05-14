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

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DELETION_DEADLINE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy").withZone(VN_ZONE);

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

        String displayName = user.getFullName() == null || user.getFullName().isBlank()
                ? "bạn"
                : user.getFullName();
        String deadlineFormatted = formatDeletionDeadline(deletionRequest.getScheduledDeletionAt());
        int currentYear = Year.now(VN_ZONE).getValue();

        String htmlContent = renderDeletionRequestTemplate(displayName, deadlineFormatted, cancellationLink, currentYear);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Xác nhận yêu cầu xóa dữ liệu");
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

    public void sendProfileInvitationEmail(User inviter, String inviteeEmail, String invitationLink) {
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send profile invitation to {}", inviteeEmail);
            return;
        }

        String htmlContent = renderProfileInvitationTemplate(inviter.getFullName(), invitationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(inviteeEmail);
            helper.setSubject("[HealthLens] " + inviter.getFullName() + " muốn chia sẻ hồ sơ sức khỏe với bạn");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send profile invitation to {}", inviteeEmail, e);
            throw new IllegalStateException("Gui email moi chia se ho so that bai", e);
        }
    }

    public void sendHealthRecordInvitationEmail(User inviter, String inviteeEmail, String invitationLink) {
        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender is not configured! Cannot send health record invitation to {}", inviteeEmail);
            return;
        }

        String inviterName = inviter.getFullName() == null || inviter.getFullName().isBlank()
                ? "Một thành viên"
                : inviter.getFullName();
        String htmlContent = """
                <html>
                  <body style="font-family: Segoe UI, Arial, sans-serif; color: #111827; background: #f3f4f6; padding: 24px;">
                    <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden;">
                      <div style="background: #00685f; padding: 24px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 26px;">HealthLens</h1>
                      </div>
                      <div style="padding: 28px;">
                        <h2 style="margin-top: 0;">Lời mời xem một kết quả khám</h2>
                        <p>Xin chào,</p>
                        <p><strong>%s</strong> muốn chia sẻ riêng một kết quả khám với bạn trên HealthLens.</p>
                        <p>Bạn chỉ có quyền xem lần khám này, không phải toàn bộ lịch sử hồ sơ.</p>
                        <div style="text-align: center; margin: 24px 0;">
                          <a href="%s" style="display: inline-block; background: #00685f; color: #ffffff; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: 700;">
                            Xem kết quả khám
                          </a>
                        </div>
                        <p style="font-size: 14px; color: #6b7280;">Lời mời có hiệu lực trong 7 ngày.</p>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(inviterName, invitationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(inviteeEmail);
            helper.setSubject("[HealthLens] " + inviterName + " muốn chia sẻ một kết quả khám với bạn");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("[EmailService] Failed to send health record invitation to {}", inviteeEmail, e);
            throw new IllegalStateException("Gui email moi chia se ket qua kham that bai", e);
        }
    }

    private String renderProfileInvitationTemplate(String inviterName, String invitationLink) {
        if (templateEngine == null) {
            return """
                    <html>
                      <body
                        style="
                          margin: 0;
                          padding: 0;
                          background: #f3f4f6;
                          font-family: Segoe UI, Arial, sans-serif;
                          color: #111827;
                        "
                      >
                        <div
                          style="
                            max-width: 600px;
                            margin: 24px auto;
                            background: #ffffff;
                            border-radius: 12px;
                            overflow: hidden;
                          "
                        >
                          <div
                            style="
                              background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                              padding: 28px;
                              text-align: center;
                            "
                          >
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px">HealthLens</h1>
                          </div>
                          <div style="padding: 28px">
                            <h2 style="margin-top: 0">Lời mời chia sẻ hồ sơ sức khỏe</h2>
                            <p>Xin chào,</p>
                            <p><strong>%s</strong> muốn chia sẻ hồ sơ sức khỏe với bạn trên HealthLens.</p>
                            <p>Nhấn vào nút bên dưới để xem và chấp nhận lời mời:</p>
                            <div style="text-align: center; margin: 24px 0">
                              <a
                                href="%s"
                                style="
                                  display: inline-block;
                                  background: #10b981;
                                  color: #ffffff;
                                  padding: 12px 28px;
                                  border-radius: 8px;
                                  text-decoration: none;
                                  font-weight: 600;
                                "
                              >
                                Xem lời mời
                              </a>
                            </div>
                            <p style="font-size: 14px; color: #6b7280">
                              Lời mời có hiệu lực trong 7 ngày. Nếu bạn chưa có tài khoản, hệ thống sẽ hướng dẫn bạn đăng ký trước khi truy cập hồ sơ được chia sẻ.
                            </p>
                          </div>
                          <div
                            style="
                              background: #f9fafb;
                              padding: 18px;
                              text-align: center;
                              font-size: 13px;
                              color: #6b7280;
                            "
                          >
                            <p style="margin: 0">HealthLens - Theo dõi sức khỏe của bạn</p>
                          </div>
                        </div>
                      </body>
                    </html>
                    """.formatted(inviterName, invitationLink);
        }

        Context context = new Context();
        context.setVariable("inviterName", inviterName);
        context.setVariable("invitationLink", invitationLink);
        return templateEngine.process("email/profile-invitation", context);
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

    private static String formatDeletionDeadline(Instant scheduledDeletionAt) {
        if (scheduledDeletionAt == null) {
            return "";
        }
        return DELETION_DEADLINE_FORMATTER.format(scheduledDeletionAt);
    }

    private String renderDeletionRequestTemplate(
            String displayName,
            String deadlineFormatted,
            String cancellationLink,
            int currentYear) {
        if (templateEngine == null) {
            return """
                    <html><body style="font-family:Segoe UI,Arial,sans-serif;color:#121e1c;">
                    <p>Xin chào %s,</p>
                    <p>Chúng tôi đã nhận được yêu cầu xóa dữ liệu cá nhân của bạn (NĐ 13/2023/NĐ-CP).</p>
                    <p>Hoàn tất xóa dự kiến trước: <strong>%s</strong> (tối đa 72 giờ).</p>
                    <p><a href="%s">Hủy yêu cầu xóa</a></p>
                    <p style="font-size:12px;color:#6d7a77;">© %d HealthLens Meridian · privacy@healthlens.vn</p>
                    </body></html>
                    """.formatted(displayName, deadlineFormatted, cancellationLink, currentYear);
        }
        Context context = new Context();
        context.setVariable("displayName", displayName);
        context.setVariable("deadlineFormatted", deadlineFormatted);
        context.setVariable("cancellationLink", cancellationLink);
        context.setVariable("currentYear", currentYear);
        return templateEngine.process("email/deletion-request", context);
    }
}
