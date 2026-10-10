package com.taskcenter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.EmailJob;
import com.taskcenter.model.EmailJobStatus;
import com.taskcenter.repository.EmailJobRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.taskcenter.util.InvitationTokenUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    public static boolean isValidRecipient(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private final TemplateEngine templateEngine;
    private final EmailJobRepository emailJobRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String fromEmail;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:${BREVO_SENDER_EMAIL:}}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name:${BREVO_SENDER_NAME:Task Center}}")
    private String brevoSenderName;

    @Value("${app.frontend.url:https://task-center-frontend.onrender.com}")
    private String frontendUrl;

    public EmailService(TemplateEngine templateEngine,
                        EmailJobRepository emailJobRepository,
                        ObjectMapper objectMapper) {
        this.templateEngine = templateEngine;
        this.emailJobRepository = emailJobRepository;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String resolveSenderEmail() {
        if (brevoSenderEmail != null && !brevoSenderEmail.isBlank()) {
            return brevoSenderEmail.trim();
        }
        if (fromEmail != null && !fromEmail.isBlank()) {
            String trimmed = fromEmail.trim();
            if (trimmed.toLowerCase().endsWith("@gmail.com")) {
                return "no-reply@taskcenter.uz";
            }
            return trimmed;
        }
        return null;
    }

    public String resolveSenderName() {
        if (brevoSenderName != null && !brevoSenderName.isBlank()) {
            return brevoSenderName.trim();
        }
        return "Task Center";
    }

    public record EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus status, String errorMessage) {}

    public EmailDeliveryResult sendInvitationEmailDirect(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        if (!isValidRecipient(toEmail)) {
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.FAILED, "Yaroqsiz email manzili");
        }

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.info("BREVO_API_KEY sozlanmaganligi sababli taklifnoma emaili o'tkazib yuborildi (SKIPPED)");
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.SKIPPED, "Brevo API kaliti sozlanmagan");
        }

        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.FAILED, "Jo'natuvchi email manzili sozlanmagan");
        }

        try {
            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                    ? frontendUrl.replaceAll("/+$", "")
                    : "https://task-center-frontend.onrender.com";

            Map<String, Object> data = new HashMap<>();
            String recipientUsername = toEmail;
            if (toEmail.contains("@")) {
                recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
            }
            data.put("recipientUsername", recipientUsername);
            data.put("inviterName", senderName);
            data.put("workspaceTitle", workspaceTitle);
            data.put("workspaceRole", workspaceRole);
            data.put("invitationLink", baseUrl + "/invite/" + invitationToken);

            Context context = new Context();
            data.forEach(context::setVariable);
            String htmlContent = templateEngine.process("email/invitation", context);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> sender = new HashMap<>();
            sender.put("name", resolveSenderName());
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);

            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail.trim());
            requestBody.put("to", List.of(to));

            requestBody.put("subject", "Task Center - " + workspaceTitle + " jamoasiga taklif");
            requestBody.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);

            log.info("Taklifnoma emaili to'g'ridan-to'g'ri muvaffaqiyatli jo'natildi -> To: {}", InvitationTokenUtil.maskEmail(toEmail));
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.SENT, null);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            String errorMsg = extractBrevoErrorMessage(e.getStatusCode().value(), responseBody, senderEmail);
            log.error("Brevo API HTTP xatolik qaytardi: {}", errorMsg);
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.FAILED, errorMsg);
        } catch (Exception e) {
            log.error("Taklifnoma emailini jo'natishda xatolik: {}", e.getMessage());
            return new EmailDeliveryResult(com.taskcenter.model.EmailDeliveryStatus.FAILED, e.getMessage());
        }
    }

    public com.taskcenter.dto.TestEmailResponse sendTestEmail(String toEmail) {
        if (!isValidRecipient(toEmail)) {
            return new com.taskcenter.dto.TestEmailResponse(false, "FAILED", "Yaroqsiz email manzili: " + toEmail);
        }

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            return new com.taskcenter.dto.TestEmailResponse(false, "SKIPPED", "BREVO_API_KEY sozlanmagan. Iltimos, env da BREVO_API_KEY ni o'rnating.");
        }

        String senderEmail = resolveSenderEmail();
        if (senderEmail == null) {
            return new com.taskcenter.dto.TestEmailResponse(false, "FAILED", "Jo'natuvchi email manzili sozlanmagan (BREVO_SENDER_EMAIL / spring.mail.username).");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> sender = new HashMap<>();
            sender.put("name", resolveSenderName());
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);

            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail.trim());
            requestBody.put("to", List.of(to));

            requestBody.put("subject", "Task Center - Brevo Test Email");
            requestBody.put("htmlContent", "<h2>Test xabari</h2><p>Task Center tizimidan Brevo orqali test xati muvaffaqiyatli yetkazildi!</p>");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);

            log.info("Test email muvaffaqiyatli jo'natildi -> To: {}", InvitationTokenUtil.maskEmail(toEmail));
            return new com.taskcenter.dto.TestEmailResponse(true, "SENT", "Test email " + toEmail + " manziliga muvaffaqiyatli yuborildi");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            String errorMsg = extractBrevoErrorMessage(e.getStatusCode().value(), responseBody, senderEmail);
            log.error("Test email yuborishda Brevo HTTP xatolik: {}", errorMsg);
            return new com.taskcenter.dto.TestEmailResponse(false, "FAILED", errorMsg);
        } catch (Exception e) {
            log.error("Test email yuborishda xatolik: {}", e.getMessage());
            return new com.taskcenter.dto.TestEmailResponse(false, "FAILED", "Xatolik: " + e.getMessage());
        }
    }

    private String extractBrevoErrorMessage(int statusCode, String responseBody, String senderEmail) {
        String detail = "";
        try {
            if (responseBody != null && !responseBody.isBlank()) {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("message")) {
                    detail = node.get("message").asText();
                } else {
                    detail = responseBody;
                }
            }
        } catch (Exception ignored) {
            detail = responseBody != null ? responseBody : "";
        }

        String lower = detail.toLowerCase();
        if (lower.contains("sender") || lower.contains("not verified") || lower.contains("unauthorized sender") || lower.contains("domain")) {
            return "Brevo jo'natuvchi xatosi: '" + senderEmail + "' manzili yoki domeni Brevo hisobida tasdiqlanmagan (Sender not verified). HTTP " + statusCode + ": " + detail;
        }
        return "Brevo xatolik: HTTP " + statusCode + (detail.isBlank() ? "" : " — " + detail);
    }

    public void sendInvitationEmail(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl.replaceAll("/+$", "")
                : "https://task-center-frontend.onrender.com";

        Map<String, Object> data = new HashMap<>();
        String recipientUsername = toEmail;
        if (toEmail != null && toEmail.contains("@")) {
            recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
        }
        data.put("recipientUsername", recipientUsername);
        data.put("inviterName", senderName);
        data.put("workspaceTitle", workspaceTitle);
        data.put("workspaceRole", workspaceRole);
        data.put("invitationLink", baseUrl + "/invite/" + invitationToken);

        enqueueJob(toEmail, "Task Center - Yangi jamoaga taklif", "email/invitation", data);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl.replaceAll("/+$", "")
                : "https://task-center-frontend.onrender.com";

        Map<String, Object> data = new HashMap<>();
        String recipientUsername = toEmail;
        if (toEmail != null && toEmail.contains("@")) {
            recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
        }
        data.put("recipientUsername", recipientUsername);
        data.put("resetLink", baseUrl + "/reset-password?token=" + resetToken);

        enqueueJob(toEmail, "Task Center — Parolni tiklash", "email/password_reset", data);
    }

    @Transactional
    public EmailJob enqueueJob(String toEmail, String subject, String templateName, Map<String, Object> data) {
        if (!isValidRecipient(toEmail)) {
            throw new IllegalArgumentException("Yaroqsiz qabul qiluvchi email manzili");
        }
        String sanitizedToEmail = toEmail.trim();

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(data != null ? data : Map.of());
        } catch (Exception e) {
            log.error("Email parametrlarini JSON ga aylantirishda xatolik: {}", e.getMessage());
            jsonPayload = "{}";
        }

        EmailJob job = EmailJob.builder()
                .toEmail(sanitizedToEmail)
                .subject(subject)
                .templateName(templateName)
                .templateData(jsonPayload)
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        EmailJob saved = emailJobRepository.save(job);
        log.info("Email job navbatga qo'shildi -> ID: {}", saved.getId());
        return saved;
    }

    @Scheduled(fixedDelayString = "${app.email.queue.poll-interval-ms:5000}")
    @SchedulerLock(name = "EmailService_processPendingJobs", lockAtLeastFor = "2s", lockAtMostFor = "1m")
    public void processPendingJobs() {
        List<EmailJob> readyJobs = emailJobRepository.findReadyJobs(
                EmailJobStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, 20)
        );

        if (readyJobs.isEmpty()) {
            return;
        }

        log.debug("Navbatdagi {} ta email jo'natishga tayyor", readyJobs.size());
        for (EmailJob job : readyJobs) {
            processSingleJob(job);
        }
    }

    @Transactional
    public void processSingleJob(EmailJob job) {
        if (job == null) {
            return;
        }

        if (!isValidRecipient(job.getToEmail())) {
            log.warn("Email job bekor qilindi: yaroqsiz qabul qiluvchi email manzili (Job ID: {})", job.getId());
            job.setStatus(EmailJobStatus.FAILED);
            job.setLastError("Yaroqsiz qabul qiluvchi email manzili");
            emailJobRepository.save(job);
            return;
        }

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY yo'qligi sababli xat yuborilmadi. Job SENT deb belgilandi -> Job ID: {}", job.getId());
            job.setStatus(EmailJobStatus.SENT);
            job.setSentAt(LocalDateTime.now());
            job.setLastError("SKIPPED: Brevo API key not configured");
            emailJobRepository.save(job);
            return;
        }

        try {
            job.setStatus(EmailJobStatus.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            emailJobRepository.save(job);

            if (fromEmail == null || fromEmail.isBlank()) {
                throw new IllegalStateException("Email jo'natuvchi manzili sozlanmagan (spring.mail.username / MAIL_USERNAME mavjud emas)");
            }
            String senderEmail = resolveSenderEmail() != null ? resolveSenderEmail() : fromEmail.trim();
            String senderName = resolveSenderName();

            Map<String, Object> data = objectMapper.readValue(job.getTemplateData(), new TypeReference<>() {});
            Context context = new Context();
            if (data != null) {
                data.forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(job.getTemplateName(), context);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);

            Map<String, String> to = new HashMap<>();
            to.put("email", job.getToEmail().trim());
            requestBody.put("to", List.of(to));

            requestBody.put("subject", job.getSubject());
            requestBody.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);

            job.setStatus(EmailJobStatus.SENT);
            job.setSentAt(LocalDateTime.now());
            job.setLastError(null);
            emailJobRepository.save(job);
            log.info("Email muvaffaqiyatli jo'natildi -> Job ID: {}, To: {}", job.getId(), InvitationTokenUtil.maskEmail(job.getToEmail()));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Brevo API HTTP xatolik qaytardi (Job ID: {}): HTTP {} - {}", job.getId(), e.getStatusCode(), responseBody);
            job.setLastError("HTTP " + e.getStatusCode() + ": " + responseBody);
            handleJobFailure(job);
        } catch (Exception e) {
            log.error("Email yuborishda xatolik yuz berdi (Job ID: {}): {}", job.getId(), e.getMessage());
            job.setLastError(e.getMessage());
            handleJobFailure(job);
        }
    }

    private void handleJobFailure(EmailJob job) {
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(EmailJobStatus.FAILED);
        } else {
            long backoffSeconds = (long) Math.min(30 * Math.pow(2, job.getAttempts() - 1), 3600);
            job.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            job.setStatus(EmailJobStatus.PENDING);
        }
        emailJobRepository.save(job);
    }
}
