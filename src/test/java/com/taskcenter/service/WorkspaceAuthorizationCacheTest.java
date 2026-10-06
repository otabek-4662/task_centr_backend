package com.taskcenter.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceAuthorizationCacheTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    private CaffeineCacheManager cacheManager;
    private WorkspaceAuthorizationService authorizationService;

    private final String workspaceId = "ws-test-1";
    private final String ownerId = "user-owner-1";
    private final String memberUserId = "user-member-1";

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager("workspaces", "workspaceRoles");
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(100).expireAfterWrite(5, TimeUnit.MINUTES));

        authorizationService = new WorkspaceAuthorizationService(workspaceRepository, memberRepository, cacheManager);

        workspace = Workspace.builder()
                .id(workspaceId)
                .title("Cache Test Workspace")
                .ownerId(ownerId)
                .build();
    }

    @Test
    @DisplayName("requireWorkspace birinchi marta bazadan o'qiydi, ikkinchi marta Caffeine keshdan qaytaradi")
    void requireWorkspace_usesCache() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        Workspace first = authorizationService.requireWorkspace(workspaceId);
        Workspace second = authorizationService.requireWorkspace(workspaceId);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.getId()).isEqualTo(workspaceId);
        assertThat(second.getId()).isEqualTo(workspaceId);

        // Bazaga faqat bir marta so'rov ketishi shart
        verify(workspaceRepository, times(1)).findById(workspaceId);
    }

    @Test
    @DisplayName("getRole birinchi marta bazadan o'qiydi, ikkinchi marta Caffeine keshdan qaytaradi")
    void getRole_usesCache() {
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId))
                .thenReturn(Optional.of(WorkspaceMember.builder()
                        .workspaceId(workspaceId)
                        .userId(memberUserId)
                        .role(WorkspaceRole.ADMIN)
                        .build()));

        Optional<WorkspaceRole> first = authorizationService.getRole(workspace, memberUserId);
        Optional<WorkspaceRole> second = authorizationService.getRole(workspace, memberUserId);

        assertThat(first).contains(WorkspaceRole.ADMIN);
        assertThat(second).contains(WorkspaceRole.ADMIN);

        // memberRepository ga faqat bir marta so'rov ketishi shart
        verify(memberRepository, times(1)).findByWorkspaceIdAndUserId(workspaceId, memberUserId);
    }

    @Test
    @DisplayName("evictRole chaqirilganda kesh tozalanadi va keyingi so'rov yana bazadan olinadi")
    void evictRole_purgesCache() {
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId))
                .thenReturn(Optional.of(WorkspaceMember.builder()
                        .workspaceId(workspaceId)
                        .userId(memberUserId)
                        .role(WorkspaceRole.MEMBER)
                        .build()));

        authorizationService.getRole(workspace, memberUserId);
        verify(memberRepository, times(1)).findByWorkspaceIdAndUserId(workspaceId, memberUserId);

        // Keshni tozalash
        authorizationService.evictRole(workspaceId, memberUserId);

        // Keyingi chaqiruv yana DB ga borishi kerak
        authorizationService.getRole(workspace, memberUserId);
        verify(memberRepository, times(2)).findByWorkspaceIdAndUserId(workspaceId, memberUserId);
    }

    @Test
    @DisplayName("evictWorkspace chaqirilganda workspace va barcha huquqlar keshi tozalanadi")
    void evictWorkspace_purgesWorkspaceAndRolesCache() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        authorizationService.requireWorkspace(workspaceId);
        verify(workspaceRepository, times(1)).findById(workspaceId);

        authorizationService.evictWorkspace(workspaceId);

        authorizationService.requireWorkspace(workspaceId);
        verify(workspaceRepository, times(2)).findById(workspaceId);
    }

    @Test
    @DisplayName("Workspace egasi (OWNER) uchun memberRepository so'rovi umuman chaqirilmaydi")
    void owner_roleResolvedDirectly() {
        Optional<WorkspaceRole> role = authorizationService.getRole(workspace, ownerId);

        assertThat(role).contains(WorkspaceRole.OWNER);
        verifyNoInteractions(memberRepository);
    }
}
