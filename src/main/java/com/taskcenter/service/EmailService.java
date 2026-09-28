package com.taskcenter.service;

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

    private final TemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void sendInvitationEmail(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            System.err.println("BREVO_API_KEY yo'qligi sababli " + toEmail + " manziliga xat yuborilmadi.");
            return;
        }
        
        String senderEmail = (fromEmail != null && !fromEmail.isEmpty()) ? fromEmail : "otabeksotimov9@gmail.com";

        try {
            Context context = new Context();
            context.setVariable("inviterName", senderName);
            context.setVariable("workspaceTitle", workspaceTitle);
            context.setVariable("workspaceRole", workspaceRole);
            context.setVariable("invitationLink", "https://task-center.uz/invite/" + invitationToken);

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
            System.out.println("Taklif xati muvaffaqiyatli jo'natildi -> " + toEmail);
        } catch (Exception e) {
            System.err.println("Taklif xatini yuborishda HTTP xatolik yuz berdi: " + e.getMessage());
        }
    }
}

