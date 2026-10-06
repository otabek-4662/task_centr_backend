package com.taskcenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.EmailJob;
import com.taskcenter.model.EmailJobStatus;
import com.taskcenter.repository.EmailJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailJobQueueTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailJobRepository emailJobRepository;

    private ObjectMapper objectMapper;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailService = new EmailService(templateEngine, emailJobRepository, objectMapper);
    }

    @Test
    void testEnqueueInvitationEmail_SavesJobWithPendingStatus() {
        when(emailJobRepository.save(any(EmailJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailService.sendInvitationEmail(
                "invitee@example.com",
                "Frontend Workspace",
                "Inviter User",
                "MEMBER",
                "invite-token-abc"
        );

        ArgumentCaptor<EmailJob> captor = ArgumentCaptor.forClass(EmailJob.class);
        verify(emailJobRepository).save(captor.capture());

        EmailJob savedJob = captor.getValue();
        assertEquals("invitee@example.com", savedJob.getToEmail());
        assertEquals("email/invitation", savedJob.getTemplateName());
        assertEquals(EmailJobStatus.PENDING, savedJob.getStatus());
        assertEquals(0, savedJob.getAttempts());
        assertNotNull(savedJob.getTemplateData());
    }

    @Test
    void testProcessPendingJobs_WhenBrevoKeyMissing_MarksSentSkipped() {
        EmailJob job = EmailJob.builder()
                .id("job-1")
                .toEmail("user@example.com")
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{\"recipientUsername\":\"user\"}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(emailJobRepository.findReadyJobs(eq(EmailJobStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(job));
        when(emailJobRepository.save(any(EmailJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailService.processPendingJobs();

        assertEquals(EmailJobStatus.SENT, job.getStatus());
        assertNotNull(job.getSentAt());
        assertTrue(job.getLastError().contains("SKIPPED"));
        verify(emailJobRepository).save(job);
    }

    @Test
    void testProcessSingleJob_FailureRetryCalculatesBackoff() {
        EmailJob job = EmailJob.builder()
                .id("job-2")
                .toEmail("user@example.com")
                .subject("Test Subject")
                .templateName("invalid/template")
                .templateData("invalid-json{")
                .status(EmailJobStatus.PENDING)
                .attempts(1)
                .maxAttempts(3)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        // Calling processSingleJob will fail parsing JSON or template, triggering retry backoff
        // Set brevoApiKey and fromEmail via reflection so it attempts processing
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "fake-api-key");
        ReflectionTestUtils.setField(emailService, "fromEmail", "configured-sender@example.com");

        emailService.processSingleJob(job);

        assertEquals(2, job.getAttempts());
        assertEquals(EmailJobStatus.PENDING, job.getStatus());
        assertNotNull(job.getLastError());
        assertTrue(job.getNextAttemptAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void testProcessSingleJob_MaxAttemptsExceeded_SetsFailed() {
        EmailJob job = EmailJob.builder()
                .id("job-3")
                .toEmail("user@example.com")
                .subject("Test Subject")
                .templateName("invalid/template")
                .templateData("invalid-json{")
                .status(EmailJobStatus.PENDING)
                .attempts(2)
                .maxAttempts(3)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "fake-api-key");
        ReflectionTestUtils.setField(emailService, "fromEmail", "configured-sender@example.com");

        emailService.processSingleJob(job);

        assertEquals(3, job.getAttempts());
        assertEquals(EmailJobStatus.FAILED, job.getStatus());
        assertNotNull(job.getLastError());
    }

    @Test
    void testProcessSingleJob_WithExplicitSender_UsesConfiguredSenderInBrevoPayload() {
        EmailJob job = EmailJob.builder()
                .id("job-valid-1")
                .toEmail("recipient@example.com")
                .subject("Welcome to Task Center")
                .templateName("email/invitation")
                .templateData("{\"recipientUsername\":\"recipient\"}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "real-brevo-key-123");
        ReflectionTestUtils.setField(emailService, "fromEmail", "notifications@company.com");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        when(templateEngine.process(eq("email/invitation"), any(Context.class)))
                .thenReturn("<html>Invite Body</html>");

        when(mockRestTemplate.postForEntity(eq("https://api.brevo.com/v3/smtp/email"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"messageId\":\"msg-001\"}", HttpStatus.OK));

        emailService.processSingleJob(job);

        assertEquals(EmailJobStatus.SENT, job.getStatus());
        assertEquals(1, job.getAttempts());
        assertNotNull(job.getSentAt());
        assertNull(job.getLastError());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(mockRestTemplate).postForEntity(eq("https://api.brevo.com/v3/smtp/email"), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> entity = captor.getValue();
        assertNotNull(entity.getBody());
        Map<String, Object> body = entity.getBody();

        @SuppressWarnings("unchecked")
        Map<String, String> sender = (Map<String, String>) body.get("sender");
        assertNotNull(sender);
        assertEquals("notifications@company.com", sender.get("email"));
        assertEquals("Task Center", sender.get("name"));
        assertNotEquals("otabeksotimov9@gmail.com", sender.get("email"));

        assertEquals("real-brevo-key-123", entity.getHeaders().getFirst("api-key"));
    }

    @Test
    void testProcessSingleJob_WhenSenderMissing_FailsWithoutFallback() {
        EmailJob job = EmailJob.builder()
                .id("job-no-sender")
                .toEmail("user@example.com")
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{\"recipientUsername\":\"user\"}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "brevo-key-xyz");
        ReflectionTestUtils.setField(emailService, "fromEmail", null);

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        emailService.processSingleJob(job);

        assertEquals(1, job.getAttempts());
        assertEquals(EmailJobStatus.PENDING, job.getStatus());
        assertNotNull(job.getLastError());
        assertTrue(job.getLastError().contains("sozlanmagan") || job.getLastError().contains("spring.mail.username"));
        assertFalse(job.getLastError().contains("otabeksotimov9@gmail.com"));

        verify(mockRestTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void testProcessSingleJob_WhenSenderIsBlank_FailsWithoutFallback() {
        EmailJob job = EmailJob.builder()
                .id("job-blank-sender")
                .toEmail("user@example.com")
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{\"recipientUsername\":\"user\"}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "brevo-key-xyz");
        ReflectionTestUtils.setField(emailService, "fromEmail", "   ");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        emailService.processSingleJob(job);

        assertEquals(1, job.getAttempts());
        assertNotNull(job.getLastError());
        assertTrue(job.getLastError().contains("sozlanmagan") || job.getLastError().contains("spring.mail.username"));

        verify(mockRestTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void testEnqueueJob_WhenRecipientIsNull_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.enqueueJob(null, "Test Subject", "email/invitation", Map.of())
        );
        verify(emailJobRepository, never()).save(any(EmailJob.class));
    }

    @Test
    void testEnqueueJob_WhenRecipientIsEmpty_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.enqueueJob("", "Test Subject", "email/invitation", Map.of())
        );
        verify(emailJobRepository, never()).save(any(EmailJob.class));
    }

    @Test
    void testEnqueueJob_WhenRecipientIsWhitespace_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.enqueueJob("   ", "Test Subject", "email/invitation", Map.of())
        );
        verify(emailJobRepository, never()).save(any(EmailJob.class));
    }

    @Test
    void testEnqueueJob_WhenRecipientIsMalformedMissingDomain_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.enqueueJob("invalid@", "Test Subject", "email/invitation", Map.of())
        );
        verify(emailJobRepository, never()).save(any(EmailJob.class));
    }

    @Test
    void testEnqueueJob_WhenRecipientIsMalformedMissingLocalPart_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                emailService.enqueueJob("@example.com", "Test Subject", "email/invitation", Map.of())
        );
        verify(emailJobRepository, never()).save(any(EmailJob.class));
    }

    @Test
    void testEnqueueJob_WhenRecipientIsValid_SuccessfullyEnqueuedAndTrimmed() {
        when(emailJobRepository.save(any(EmailJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailJob job = emailService.enqueueJob("  user@example.com  ", "Test Subject", "email/invitation", Map.of());

        assertNotNull(job);
        assertEquals("user@example.com", job.getToEmail());
        assertEquals(EmailJobStatus.PENDING, job.getStatus());
        assertEquals(0, job.getAttempts());
        verify(emailJobRepository, times(1)).save(any(EmailJob.class));
    }

    @Test
    void testProcessSingleJob_WhenRecipientIsMalformed_MarksFailedImmediatelyWithoutCallingBrevoOrRetrying() {
        EmailJob job = EmailJob.builder()
                .id("job-invalid-1")
                .toEmail("invalid@")
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "real-brevo-key-123");
        ReflectionTestUtils.setField(emailService, "fromEmail", "configured-sender@example.com");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        emailService.processSingleJob(job);

        assertEquals(EmailJobStatus.FAILED, job.getStatus());
        assertEquals(0, job.getAttempts());
        assertNotNull(job.getLastError());
        assertTrue(job.getLastError().contains("Yaroqsiz"));
        verify(mockRestTemplate, never()).postForEntity(anyString(), any(), any());
        verify(emailJobRepository).save(job);
    }

    @Test
    void testProcessSingleJob_WhenRecipientIsNull_MarksFailedImmediatelyWithoutCallingBrevoOrRetrying() {
        EmailJob job = EmailJob.builder()
                .id("job-null-email")
                .toEmail(null)
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "real-brevo-key-123");
        ReflectionTestUtils.setField(emailService, "fromEmail", "configured-sender@example.com");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        emailService.processSingleJob(job);

        assertEquals(EmailJobStatus.FAILED, job.getStatus());
        assertEquals(0, job.getAttempts());
        assertNotNull(job.getLastError());
        verify(mockRestTemplate, never()).postForEntity(anyString(), any(), any());
        verify(emailJobRepository).save(job);
    }

    @Test
    void testProcessSingleJob_WhenRecipientIsBlank_MarksFailedImmediatelyWithoutCallingBrevoOrRetrying() {
        EmailJob job = EmailJob.builder()
                .id("job-blank-email")
                .toEmail("   ")
                .subject("Test Subject")
                .templateName("email/invitation")
                .templateData("{}")
                .status(EmailJobStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(emailService, "brevoApiKey", "real-brevo-key-123");
        ReflectionTestUtils.setField(emailService, "fromEmail", "configured-sender@example.com");

        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(emailService, "restTemplate", mockRestTemplate);

        emailService.processSingleJob(job);

        assertEquals(EmailJobStatus.FAILED, job.getStatus());
        assertEquals(0, job.getAttempts());
        assertNotNull(job.getLastError());
        verify(mockRestTemplate, never()).postForEntity(anyString(), any(), any());
        verify(emailJobRepository).save(job);
    }
}
