package com.taskcenter.service;

import com.taskcenter.repository.WorkspaceInvitationRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class InvitationCleanupService {

    private final WorkspaceInvitationRepository invitationRepository;

    public InvitationCleanupService(WorkspaceInvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    // Har kuni soat tungi 3:00 (Asia/Tashkent) da ishga tushadi
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Tashkent")
    @SchedulerLock(name = "InvitationCleanupService_cleanupExpiredInvitations", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void cleanupExpiredInvitations() {
        LocalDateTime nowUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        int expiredCount = invitationRepository.expireOldPendingInvitations(nowUtc);
        LocalDateTime cutoff30Days = nowUtc.minusDays(30);
        int purgedCount = invitationRepository.deleteOldInactiveInvitations(cutoff30Days);
        log.info("CronJob: {} ta muddati o'tgan taklifnoma EXPIRED qilindi, {} ta 30 kundan eski nofaol taklifnoma bazadan tozalandi.", expiredCount, purgedCount);
    }
}
