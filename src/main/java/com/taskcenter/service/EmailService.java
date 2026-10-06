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

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

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
            String senderEmail = fromEmail.trim();

            Map<String, Object> data = objectMapper.readValue(job.getTemplateData(), new TypeReference<>() {});
            Context context = new Context();
            if (data != null) {
                data.forEach(context::setVariable);
            }

            String htmlContent = templateEngine.process(job.getTemplateName(), context);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> sender = new HashMap<>();
            sender.put("name", "Task Center");
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
            log.info("Email muvaffaqiyatli jo'natildi -> Job ID: {}", job.getId());
        } catch (Exception e) {
            log.error("Email yuborishda xatolik yuz berdi (Job ID: {}): {}", job.getId(), e.getMessage());
            job.setLastError(e.getMessage());
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
}
