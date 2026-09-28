package com.taskcenter.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendInvitationEmail(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            System.err.println("Email konfiguratsiyasi (MAIL_USERNAME) yo'qligi sababli " + toEmail + " manziliga xat yuborilmadi.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Task Center - Yangi jamoaga taklif");

            Context context = new Context();
            context.setVariable("inviterName", senderName);
            context.setVariable("workspaceTitle", workspaceTitle);
            context.setVariable("workspaceRole", workspaceRole);
            context.setVariable("invitationLink", "https://task-center.uz/invite/" + invitationToken);

            String htmlContent = templateEngine.process("email/invitation", context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Taklif xatini yuborishda xatolik yuz berdi: " + e.getMessage());
        }
    }
}
