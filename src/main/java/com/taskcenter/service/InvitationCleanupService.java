package com.taskcenter.service;

import com.taskcenter.repository.WorkspaceInvitationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InvitationCleanupService {

    private final WorkspaceInvitationRepository invitationRepository;

    public InvitationCleanupService(WorkspaceInvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    // Har kuni soat tungi 3:00 da ishga tushadi
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredInvitations() {
        System.out.println("CronJob: Muddati o'tgan taklifnomalarni o'chirish jarayoni boshlandi...");
        
        // Hozirgi vaqtdan oldingi expiredAt ga ega bo'lgan takliflarni o'chirish
        // Spring Data JPA custom yozishimiz kerak yoki topib o'chirishimiz kerak
        // Lekin biz to'g'ridan-to'g'ri repository orqali delete qilishimiz ham mumkin.
        invitationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        
        System.out.println("CronJob: Muddati o'tgan taklifnomalar muvaffaqiyatli tozalandi.");
    }
}
