package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceKeyPrefixTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private WorkspaceCreateRequest req(String title) {
        WorkspaceCreateRequest r = new WorkspaceCreateRequest();
        r.setTitle(title);
        return r;
    }

    private User owner() {
        return User.builder().id("owner-1").name("owner").email("owner@test.local").password("x").build();
    }

    private String savedPrefix() {
        ArgumentCaptor<Workspace> cap = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(cap.capture());
        return cap.getValue().getKeyPrefix();
    }

    @Test
    void freePrefix_usedAsIs() {
        when(workspaceRepository.existsByKeyPrefix("WR")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        workspaceService.createWorkspace(req("Website Redesign"), owner());

        assertThat(savedPrefix()).isEqualTo("WR");
    }

    @Test
    void takenPrefix_getsNumericSuffix() {
        when(workspaceRepository.existsByKeyPrefix("WR")).thenReturn(true);
        when(workspaceRepository.existsByKeyPrefix("WR2")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceDto dto = workspaceService.createWorkspace(req("Website Redesign"), owner());

        assertThat(savedPrefix()).isEqualTo("WR2");
        assertThat(dto).isNotNull();
    }

    @Test
    void takenPrefix_skipsToFirstFreeSuffix() {
        when(workspaceRepository.existsByKeyPrefix("WR")).thenReturn(true);
        when(workspaceRepository.existsByKeyPrefix("WR2")).thenReturn(true);
        when(workspaceRepository.existsByKeyPrefix("WR3")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        workspaceService.createWorkspace(req("Website Redesign"), owner());

        assertThat(savedPrefix()).isEqualTo("WR3");
    }

    @Test
    void singleWordTitle_firstFourChars() {
        when(workspaceRepository.existsByKeyPrefix("ALPH")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        workspaceService.createWorkspace(req("Alpha"), owner());

        assertThat(savedPrefix()).isEqualTo("ALPH");
    }
}
