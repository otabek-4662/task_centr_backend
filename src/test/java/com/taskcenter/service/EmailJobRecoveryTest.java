package com.taskcenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.EmailJob;
import com.taskcenter.model.EmailJobStatus;
import com.taskcenter.repository.EmailJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.TemplateEngine;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailJobRecoveryTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private EmailJobRepository emailJobRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(templateEngine, emailJobRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("processPendingJobs har siklda 10 daqiqadan ortiq PROCESSING da qolgan joblarni qayta tiklashi kerak")
    void processPendingJobs_recoversStuckProcessingJobs() {
        when(emailJobRepository.recoverStuckProcessingJobs(
                eq(EmailJobStatus.PROCESSING),
                eq(EmailJobStatus.PENDING),
                eq(EmailJobStatus.FAILED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(2);

        when(emailJobRepository.findReadyJobs(eq(EmailJobStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        emailService.processPendingJobs();

        verify(emailJobRepository, times(1)).recoverStuckProcessingJobs(
                eq(EmailJobStatus.PROCESSING),
                eq(EmailJobStatus.PENDING),
                eq(EmailJobStatus.FAILED),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }
}
