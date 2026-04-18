package com.healthlens.api.service;

import com.healthlens.api.entity.User;
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
