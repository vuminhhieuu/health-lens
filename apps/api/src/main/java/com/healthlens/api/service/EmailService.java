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

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@healthlens.vn}")
    private String fromAddress;

    @Value("${app.frontend.verification-url:http://localhost:3000/verify-email}")
    private String verificationBaseUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public void sendVerificationEmail(User user, String token) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Skip sending verification email for {}", user.getEmail());
            return;
        }

        String verificationLink = verificationBaseUrl + "?token=" + token;
        String htmlContent = """
                <html>
                  <body style=\"font-family: Arial, sans-serif; color: #111827;\">
                    <h2>Chao mung ban den voi HealthLens</h2>
                    <p>Cam on ban da dang ky tai khoan.</p>
                    <p>Vui long bam vao lien ket ben duoi de xac thuc email (hieu luc 24 gio):</p>
                    <p><a href=\"%s\">Xac thuc email</a></p>
                    <p>Neu ban khong thuc hien dang ky, hay bo qua email nay.</p>
                  </body>
                </html>
                """.formatted(verificationLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("[HealthLens] Xac thuc email dang ky");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Cannot send verification email for user {}", user.getEmail(), e);
            throw new IllegalStateException("Gui email xac thuc that bai", e);
        }
    }
}
