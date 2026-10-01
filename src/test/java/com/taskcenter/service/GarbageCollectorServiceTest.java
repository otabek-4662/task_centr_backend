package com.taskcenter.service;

import com.taskcenter.service.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GarbageCollectorServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private GarbageCollectorService garbageCollectorService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void test1_successfulTransaction_callsAfterCommit() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(LocalDateTime.class)))
                .thenReturn(List.of("path/to/file1.jpg", "path/to/file2.png"));
        
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(1);

        garbageCollectorService.cleanUpOldDeletedRecords();

        // 1. Inside the method, DB delete is called
        verify(jdbcTemplate, times(3)).update(anyString(), any(LocalDateTime.class));
        
        // 2. Physical delete is NOT called yet (because we haven't triggered afterCommit)
        verify(fileStorageService, never()).deleteFile(anyString());

        // 3. Trigger afterCommit
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCommit();
        }

        // 4. Now physical delete should be called
        verify(fileStorageService).deleteFile("path/to/file1.jpg");
        verify(fileStorageService).deleteFile("path/to/file2.png");
    }

    @Test
    void test2_transactionRollback_neverCallsDeleteFile() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(LocalDateTime.class)))
                .thenReturn(List.of("path/to/file1.jpg"));
        
        // Simulate DB error during delete
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated rollback"));

        assertThatThrownBy(() -> garbageCollectorService.cleanUpOldDeletedRecords())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated rollback");

        // The transaction rolled back, so Spring would not call afterCommit.
        // Therefore, deleteFile is never called.
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    void test3_multipleFiles_collectedAndDeletedOnSuccess() {
        // Equivalent to test 1
        test1_successfulTransaction_callsAfterCommit();
    }

    @Test
    void test4_physicalDeleteFailsAfterCommit_doesNotRollback() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(LocalDateTime.class)))
                .thenReturn(List.of("path/to/file1.jpg"));
        
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(1);
        
        // Simulate file deletion failure
        doThrow(new RuntimeException("S3 error")).when(fileStorageService).deleteFile(anyString());

        // Call method
        garbageCollectorService.cleanUpOldDeletedRecords();
        
        // DB transaction completes successfully (no exception thrown)
        verify(jdbcTemplate, times(3)).update(anyString(), any(LocalDateTime.class));

        // Trigger afterCommit
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCommit(); // Should catch the exception internally and log it, without failing
        }

        verify(fileStorageService).deleteFile("path/to/file1.jpg");
    }

    @Test
    void test5_noAttachments_noCallbackRegistered() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(1);

        garbageCollectorService.cleanUpOldDeletedRecords();

        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).isEmpty();
        
        verify(fileStorageService, never()).deleteFile(anyString());
    }
}
