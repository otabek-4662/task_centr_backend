package com.taskcenter.service;

import com.taskcenter.dto.InviteRequestDto;
import com.taskcenter.dto.WorkspaceInvitationDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.*;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TelegramNotificationService telegramNotificationService;

    public WorkspaceInvitationService(WorkspaceInvitationRepository invitationRepository,
                                      WorkspaceMemberRepository memberRepository,
                                      UserRepository userRepository,
                                      WorkspaceAuthorizationService authorizationService,
                                      NotificationService notificationService,
                                      EmailService emailService,
                                      TelegramNotificationService telegramNotificationService) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.telegramNotificationService = telegramNotificationService;
    }

    @Transactional
    public WorkspaceInvitationDto inviteUser(String workspaceId, InviteRequestDto request, User currentUser) {
        Workspace workspace = authorizationService.checkAdmin(workspaceId, currentUser);

        // Rate limiting / Spam protection: Har bir loyihada maksimum 50 ta PENDING taklif bo'lishi mumkin
        long pendingCount = invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
                .count();
        if (pendingCount >= 50) {
            throw new BadRequestException("Ushbu loyihada kutilayotgan takliflar soni 50 tadan oshib ketdi. Eski takliflarni bekor qiling.");
        }

        String query = request.getUsernameOrEmail().trim();
        java.util.Optional<User> receiverOpt = userRepository.findByNameOrEmail(query);

        String receiverId = null;
        String receiverEmail = null;

        if (receiverOpt.isPresent()) {
            User receiver = receiverOpt.get();
            if (workspace.getOwnerId().equals(receiver.getId()) || memberRepository.existsByWorkspaceIdAndUserId(workspaceId, receiver.getId())) {
                throw new ConflictException("Ushbu foydalanuvchi allaqachon workspace a'zosi");
            }
            receiverId = receiver.getId();
            invitationRepository.findByWorkspaceIdAndReceiverIdAndStatus(workspaceId, receiver.getId(), InvitationStatus.PENDING)
                    .ifPresent(i -> {
                        throw new ConflictException("Foydalanuvchiga allaqachon taklif yuborilgan");
                    });
        } else {
            if (query.contains("@")) {
                receiverEmail = query;
                final String email = query;
                invitationRepository.findByWorkspaceIdAndReceiverIdAndStatus(workspaceId, null, InvitationStatus.PENDING)
                        .stream().filter(inv -> email.equalsIgnoreCase(inv.getReceiverEmail())).findFirst()
                        .ifPresent(i -> {
                            throw new ConflictException("Ushbu emailga allaqachon taklif yuborilgan");
                        });
            } else {
                throw new ResourceNotFoundException("Foydalanuvchi topilmadi: " + query);
            }
        }

        WorkspaceRole roleToAssign = request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        if (roleToAssign == WorkspaceRole.OWNER) {
            throw new BadRequestException("A'zoga OWNER rolini berib bo'lmaydi");
        }

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .senderId(currentUser.getId())
                .receiverId(receiverId)
                .receiverEmail(receiverEmail)
                .role(roleToAssign)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7)) // 7 kunlik taklif
                .build();

        WorkspaceInvitation saved = invitationRepository.save(invitation);

        if (receiverId != null) {
            notificationService.notifyUser(receiverId, "Yangi taklif", currentUser.getName() + " sizni " + workspace.getTitle() + " loyihasiga taklif qildi.");
            receiverOpt.ifPresent(receiver -> telegramNotificationService.sendWorkspaceInviteNotification(workspace, receiver, currentUser));
        }
        
        String emailToNotify = receiverEmail != null ? receiverEmail : (receiverOpt.isPresent() ? receiverOpt.get().getEmail() : null);
        if (emailToNotify != null) {
            emailService.sendInvitationEmail(emailToNotify, workspace.getTitle(), currentUser.getName(), roleToAssign.name(), saved.getId());
        }

        return WorkspaceInvitationDto.fromEntity(saved);
    }

    @Transactional
    public void acceptInvitation(String id, User currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taklif topilmadi"));

        if (invitation.getStatus() != InvitationStatus.PENDING || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Taklif yaroqsiz yoki muddati o'tgan");
        }

        boolean isAuthorized = false;
        if (invitation.getReceiverId() != null) {
            isAuthorized = invitation.getReceiverId().equals(currentUser.getId());
        } else if (invitation.getReceiverEmail() != null) {
            isAuthorized = invitation.getReceiverEmail().equalsIgnoreCase(currentUser.getEmail());
        }

        if (!isAuthorized) {
            throw new ForbiddenException("Bu taklif sizga tegishli emas");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(currentUser.getId())
                .role(invitation.getRole())
                .build();
        memberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
    }

    @Transactional
    public void rejectInvitation(String id, User currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taklif topilmadi"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Taklif allaqachon yakunlangan");
        }

        boolean isAuthorized = false;
        if (invitation.getReceiverId() != null) {
            isAuthorized = invitation.getReceiverId().equals(currentUser.getId());
        } else if (invitation.getReceiverEmail() != null) {
            isAuthorized = invitation.getReceiverEmail().equalsIgnoreCase(currentUser.getEmail());
        }

        if (!isAuthorized) {
            throw new ForbiddenException("Bu taklif sizga tegishli emas");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);
    }

    @Transactional
    public void cancelInvitation(String workspaceId, String id, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taklif topilmadi"));

        if (!invitation.getWorkspaceId().equals(workspaceId)) {
            throw new BadRequestException("Ushbu taklif berilgan workspace ga tegishli emas");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDto> getMyPendingInvitations(User currentUser) {
        return invitationRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(currentUser.getId(), InvitationStatus.PENDING)
                .stream().map(WorkspaceInvitationDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDto> getWorkspaceInvitations(String workspaceId, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);
        return invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream().map(WorkspaceInvitationDto::fromEntity).collect(Collectors.toList());
    }
}
