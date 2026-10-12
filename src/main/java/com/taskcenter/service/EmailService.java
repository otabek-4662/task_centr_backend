package com.taskcenter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.EmailJob;
import com.taskcenter.model.EmailJobStatus;
import com.taskcenter.repository.EmailJobRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
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
    private final WorkspaceInvitationRepository invitationRepository;
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

    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public EmailService(TemplateEngine templateEngine,
                        EmailJobRepository emailJobRepository,
                        ObjectMapper objectMapper) {
        this(templateEngine, emailJobRepository, objectMapper, null, null);
    }

    public EmailService(TemplateEngine templateEngine,
                        EmailJobRepository emailJobRepository,
                        ObjectMapper objectMapper,
                        org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this(templateEngine, emailJobRepository, objectMapper, transactionManager, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmailService(TemplateEngine templateEngine,
                        EmailJobRepository emailJobRepository,
                        ObjectMapper objectMapper,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.transaction.PlatformTransactionManager transactionManager,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) WorkspaceInvitationRepository invitationRepository) {
        this.templateEngine = templateEngine;
        this.emailJobRepository = emailJobRepository;
        this.invitationRepository = invitationRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionManager != null
                ? new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                : null;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
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

    public void sendInvitationEmail(String invitationId, String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl.replaceAll("/+$", "")
                : "https://task-center-frontend.onrender.com";

        Map<String, Object> data = new HashMap<>();
        String recipientUsername = toEmail;
        if (toEmail != null && toEmail.contains("@")) {
            recipientUsername = toEmail.substring(0, toEmail.indexOf("@"));
        }
        if (invitationId != null) {
            data.put("invitationId", invitationId);
        }
        data.put("recipientUsername", recipientUsername);
        data.put("inviterName", senderName);
        data.put("workspaceTitle", workspaceTitle);
        data.put("workspaceRole", workspaceRole);
        data.put("invitationLink", baseUrl + "/invite/" + invitationToken);

        enqueueJob(toEmail, "Task Center - Yangi jamoaga taklif", "email/invitation", data);
    }

    public void sendInvitationEmail(String toEmail, String workspaceTitle, String senderName, String workspaceRole, String invitationToken) {
        sendInvitationEmail(null, toEmail, workspaceTitle, senderName, workspaceRole, invitationToken);
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

    @Scheduled(fixedDelayString = "${app.email.queue.poll-interval-ms:5000}", initialDelay = 15000)
    @SchedulerLock(name = "EmailService_processPendingJobs", lockAtLeastFor = "2s", lockAtMostFor = "1m")
    public void processPendingJobs() {
        try {
            int recovered = emailJobRepository.recoverStuckProcessingJobs(
                    EmailJobStatus.PROCESSING,
                    EmailJobStatus.PENDING,
                    EmailJobStatus.FAILED,
                    LocalDateTime.now().minusMinutes(10),
                    LocalDateTime.now()
            );
            if (recovered > 0) {
                log.warn("{} ta PROCESSING da qolib ketgan (stuck) email job qayta tiklandi (PENDING yoki FAILED)", recovered);
            }
        } catch (Exception e) {
            log.error("Stuck email joblarni qayta tiklashda xatolik: {}", e.getMessage());
        }

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

    public void processSingleJob(EmailJob job) {
        if (job == null) {
            return;
        }

        String invitationId = extractInvitationId(job);

        if (!isValidRecipient(job.getToEmail())) {
            log.warn("Email job bekor qilindi: yaroqsiz qabul qiluvchi email manzili (Job ID: {})", job.getId());
            job.setStatus(EmailJobStatus.FAILED);
            job.setLastError("Yaroqsiz qabul qiluvchi email manzili");
            saveJobInTransaction(job);
            updateInvitationDeliveryStatus(invitationId, com.taskcenter.model.EmailDeliveryStatus.FAILED, "Yaroqsiz qabul qiluvchi email manzili");
            return;
        }

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY yo'qligi sababli xat yuborilmadi. Job SENT deb belgilandi -> Job ID: {}", job.getId());
            job.setStatus(EmailJobStatus.SENT);
            job.setSentAt(LocalDateTime.now());
            job.setLastError("SKIPPED: Brevo API key not configured");
            saveJobInTransaction(job);
            updateInvitationDeliveryStatus(invitationId, com.taskcenter.model.EmailDeliveryStatus.SKIPPED, "SKIPPED: Brevo API key not configured");
            return;
        }

        // 1. Qisqa tranzaksiyada jobni atomik ravishda PROCESSING ga o'tkazish
        boolean claimed = claimJobForProcessing(job);
        if (!claimed) {
            log.debug("Email job {} boshqa worker tomonidan band qilingan, o'tkazib yuborildi", job.getId());
            return;
        }

        // 2. Tashqi Brevo HTTP API chaqiruvini tranzaksiyadan tashqarida bajarish
        try {
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

            // 3. Muvaffaqiyatli yakunni qisqa tranzaksiyada saqlash
            job.setStatus(EmailJobStatus.SENT);
            job.setSentAt(LocalDateTime.now());
            job.setLastError(null);
            saveJobInTransaction(job);
            updateInvitationDeliveryStatus(invitationId, com.taskcenter.model.EmailDeliveryStatus.SENT, null);
            log.info("Email muvaffaqiyatli jo'natildi -> Job ID: {}, To: {}", job.getId(), InvitationTokenUtil.maskEmail(job.getToEmail()));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Brevo API HTTP xatolik qaytardi (Job ID: {}): HTTP {} - {}", job.getId(), e.getStatusCode(), responseBody);
            String err = "HTTP " + e.getStatusCode() + ": " + responseBody;
            job.setLastError(err);
            handleJobFailureAndSave(job, invitationId, err);
        } catch (Exception e) {
            log.error("Email yuborishda xatolik yuz berdi (Job ID: {}): {}", job.getId(), e.getMessage());
            job.setLastError(e.getMessage());
            handleJobFailureAndSave(job, invitationId, e.getMessage());
        }
    }

    private boolean claimJobForProcessing(EmailJob job) {
        if (job.getId() == null) {
            job.setStatus(EmailJobStatus.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            saveJobInTransaction(job);
            return true;
        }

        if (transactionTemplate != null) {
            Boolean result = transactionTemplate.execute(status -> {
                int updated = emailJobRepository.claimJobAtomic(
                        job.getId(), EmailJobStatus.PENDING, EmailJobStatus.PROCESSING, LocalDateTime.now());
                if (updated > 0) {
                    job.setStatus(EmailJobStatus.PROCESSING);
                    job.setAttempts(job.getAttempts() + 1);
                    return true;
                }
                return false;
            });
            return Boolean.TRUE.equals(result);
        } else {
            job.setStatus(EmailJobStatus.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            emailJobRepository.save(job);
            return true;
        }
    }

    private void handleJobFailureAndSave(EmailJob job, String invitationId, String errorMessage) {
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(EmailJobStatus.FAILED);
            updateInvitationDeliveryStatus(invitationId, com.taskcenter.model.EmailDeliveryStatus.FAILED, errorMessage);
        } else {
            long backoffSeconds = (long) Math.min(30 * Math.pow(2, job.getAttempts() - 1), 3600);
            job.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            job.setStatus(EmailJobStatus.PENDING);
        }
        saveJobInTransaction(job);
    }

    private String extractInvitationId(EmailJob job) {
        if (job == null || job.getTemplateData() == null || !job.getTemplateData().contains("invitationId")) {
            return null;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(job.getTemplateData(), new TypeReference<>() {});
            return (data != null && data.get("invitationId") != null) ? String.valueOf(data.get("invitationId")) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateInvitationDeliveryStatus(String invitationId, com.taskcenter.model.EmailDeliveryStatus status, String error) {
        if (invitationId != null && invitationRepository != null) {
            try {
                if (transactionTemplate != null) {
                    transactionTemplate.executeWithoutResult(s ->
                            invitationRepository.updateEmailDeliveryStatus(invitationId, status, error));
                } else {
                    invitationRepository.updateEmailDeliveryStatus(invitationId, status, error);
                }
            } catch (Exception e) {
                log.error("Taklifnoma email_delivery_status yangilashda xatolik (Invite ID: {}): {}", invitationId, e.getMessage());
            }
        }
    }

    private void saveJobInTransaction(EmailJob job) {
        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(status -> emailJobRepository.save(job));
        } else {
            emailJobRepository.save(job);
        }
    }
}
