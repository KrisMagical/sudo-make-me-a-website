package com.magiccode.backend.service;

import com.magiccode.backend.model.Comment;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Data
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailNotificationService implements NotificationService {
    private final JavaMailSender mailSender;
    @Value("${app.frontend.url}")
    private String frontendUrl;
    @Value("${app.mail.recipient}")
    private String recipient;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendCommentNotification(Comment comment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo("recipient");
            helper.setSubject("New Comment On Post: " + comment.getPost().getTitle());
            String formattedDate = comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String postLink=String.format("%s/posts/%s", frontendUrl, comment.getPost().getSlug());
            String content = String.format(
                    "Name: %s%nEmail: %s%nDate: %s%nContent: %s%nPost Link: %s",
                    comment.getName(),
                    comment.getEmail(),
                    formattedDate,
                    comment.getContent(),
                    postLink
            );
            helper.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
        }
    }
}
