package com.taskcenter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final TemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.frontend.url:https://task-center-frontend.onrender.com}")
    private String frontendUrl;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void sendInvitationEmail(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.warn("BREVO_API_KEY yo'qligi sababli {} manziliga xat yuborilmadi.", toEmail);
            return;
        }
        
        String senderEmail = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "otabeksotimov9@gmail.com";
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) 
                ? frontendUrl.replaceAll("/+$", "") 
                : "https://task-center-frontend.onrender.com";

        try {
            String recipientUsername = toEmail;
            if (toEmail != null && toEmail.contains("@")) {
                recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
            }

            Context context = new Context();
            context.setVariable("recipientUsername", recipientUsername);
            context.setVariable("inviterName", senderName);
            context.setVariable("workspaceTitle", workspaceTitle);
            context.setVariable("workspaceRole", workspaceRole);
            context.setVariable("invitationLink", baseUrl + "/invite/" + invitationToken);

            String htmlContent = templateEngine.process("email/invitation", context);

            // Brevo API Request Body
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, String> sender = new HashMap<>();
            sender.put("name", "Task Center");
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);
            
            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail);
            requestBody.put("to", List.of(to));
            
            requestBody.put("subject", "Task Center - Yangi jamoaga taklif");
            requestBody.put("htmlContent", htmlContent);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);
            log.info("Taklif xati muvaffaqiyatli jo'natildi -> {}", toEmail);
        } catch (Exception e) {
            log.error("Taklif xatini yuborishda HTTP xatolik yuz berdi: {}", e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.warn("BREVO_API_KEY yo'qligi sababli {} manziliga parol tiklash xati yuborilmadi.", toEmail);
            return;
        }

        String senderEmail = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "otabeksotimov9@gmail.com";
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl.replaceAll("/+$", "")
                : "https://task-center-frontend.onrender.com";

        try {
            String recipientUsername = toEmail;
            if (toEmail != null && toEmail.contains("@")) {
                recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
            }

            Context context = new Context();
            context.setVariable("recipientUsername", recipientUsername);
            context.setVariable("resetLink", baseUrl + "/reset-password?token=" + resetToken);

            String htmlContent = templateEngine.process("email/password_reset", context);

            Map<String, Object> requestBody = new HashMap<>();

            Map<String, String> sender = new HashMap<>();
            sender.put("name", "Task Center");
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);

            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail);
            requestBody.put("to", List.of(to));

            requestBody.put("subject", "Task Center — Parolni tiklash");
            requestBody.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);
            log.info("Parol tiklash xati muvaffaqiyatli jo'natildi -> {}", toEmail);
        } catch (Exception e) {
            log.error("Parol tiklash xatini yuborishda HTTP xatolik yuz berdi: {}", e.getMessage());
        }
    }
}

