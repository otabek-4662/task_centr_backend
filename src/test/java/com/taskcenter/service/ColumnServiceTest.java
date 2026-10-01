package com.taskcenter.service;

import com.taskcenter.dto.ColumnCreateRequest;
import com.taskcenter.dto.ColumnDto;
import com.taskcenter.dto.ColumnPatchRequest;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColumnServiceTest {

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private WebSocketNotifier webSocketNotifier;

    @InjectMocks
    private ColumnService columnService;

    private final User testUser = User.builder().id("user1").name("tester").role(User.Role.USER).build();

    @Test
    void createColumn_whenTitleIsNull_savesWithNullTitle() {
        when(columnRepository.findMaxOrderByWorkspaceId("ws1")).thenReturn(2);
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> {
            BoardColumn c = inv.getArgument(0);
            c.setId("col1");
            return c;
        });

        ColumnCreateRequest req = new ColumnCreateRequest();
        req.setTitle(null); // Title yo'q

        ColumnDto result = columnService.createColumn("ws1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isNull();
        assertThat(result.getTitle()).isNull();
    }

    @Test
    void createColumn_whenIsDoneIsNull_defaultsToFalse() {
        when(columnRepository.findMaxOrderByWorkspaceId("ws1")).thenReturn(2);
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> {
            BoardColumn c = inv.getArgument(0);
            c.setId("col1");
            return c;
        });

        ColumnCreateRequest req = new ColumnCreateRequest();
        req.setTitle("In Progress");
        req.setIsDone(null); // null yuborilganda

        ColumnDto result = columnService.createColumn("ws1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDone()).isFalse();
        assertThat(result.getIsDone()).isFalse();
    }

    @Test
    void createColumn_whenIsDoneIsTrue_setsTrue() {
        when(columnRepository.findMaxOrderByWorkspaceId("ws1")).thenReturn(2);
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> {
            BoardColumn c = inv.getArgument(0);
            c.setId("col1");
            return c;
        });

        ColumnCreateRequest req = new ColumnCreateRequest();
        req.setTitle("Done");
        req.setIsDone(true);

        ColumnDto result = columnService.createColumn("ws1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDone()).isTrue();
        assertThat(result.getIsDone()).isTrue();
    }

    @Test
    void updateColumn_putRequest_whenIsDoneIsNull_preservesExistingIsDone() {
        BoardColumn existing = BoardColumn.builder()
                .id("col1")
                .workspaceId("ws1")
                .title("Ko'z tegmasin")
                .order(3)
                .isDone(true) // mavjud qiymat true
                .build();

        when(columnRepository.findById("col1")).thenReturn(Optional.of(existing));
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> inv.getArgument(0));

        ColumnCreateRequest req = new ColumnCreateRequest();
        req.setTitle("Ko'z tegmasin");
        req.setOrder(3);
        req.setIsDone(null); // PUT so'rovida isDone yuborilmadi (null)

        ColumnDto result = columnService.updateColumn("ws1", "col1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        // Mavjud qiymat o'zgarmasligi kerak (true bo'lib qolishi shart, false ga qaytmasligi kerak)
        assertThat(captor.getValue().getIsDone()).isTrue();
        assertThat(result.getIsDone()).isTrue();
    }

    @Test
    void updateColumn_putRequest_whenIsDoneIsExplicit_updatesIsDone() {
        BoardColumn existing = BoardColumn.builder()
                .id("col1")
                .workspaceId("ws1")
                .title("Custom")
                .order(1)
                .isDone(true)
                .build();

        when(columnRepository.findById("col1")).thenReturn(Optional.of(existing));
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> inv.getArgument(0));

        ColumnCreateRequest req = new ColumnCreateRequest();
        req.setTitle("Custom");
        req.setIsDone(false); // Aniq false ga o'zgartirish

        ColumnDto result = columnService.updateColumn("ws1", "col1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDone()).isFalse();
        assertThat(result.getIsDone()).isFalse();
    }

    @Test
    void patchColumn_whenIsDoneIsNull_preservesExistingIsDone() {
        BoardColumn existing = BoardColumn.builder()
                .id("col1")
                .workspaceId("ws1")
                .title("Ko'z tegmasin")
                .order(3)
                .isDone(true) // mavjud qiymat true
                .build();

        when(columnRepository.findById("col1")).thenReturn(Optional.of(existing));
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> inv.getArgument(0));

        ColumnPatchRequest req = new ColumnPatchRequest();
        req.setTitle("Yangi nom");
        req.setIsDone(null); // PATCH so'rovida isDone yuborilmadi (null)

        ColumnDto result = columnService.patchColumn("ws1", "col1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        // Mavjud qiymat o'zgarmasligi kerak
        assertThat(captor.getValue().getIsDone()).isTrue();
        assertThat(result.getIsDone()).isTrue();
    }

    @Test
    void patchColumn_whenIsDoneIsExplicit_updatesIsDone() {
        BoardColumn existing = BoardColumn.builder()
                .id("col1")
                .workspaceId("ws1")
                .title("Testing")
                .order(2)
                .isDone(false)
                .build();

        when(columnRepository.findById("col1")).thenReturn(Optional.of(existing));
        when(columnRepository.save(any(BoardColumn.class))).thenAnswer(inv -> inv.getArgument(0));

        ColumnPatchRequest req = new ColumnPatchRequest();
        req.setIsDone(true); // PATCH orqali true ga o'tkazish

        ColumnDto result = columnService.patchColumn("ws1", "col1", req, testUser);

        ArgumentCaptor<BoardColumn> captor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDone()).isTrue();
        assertThat(result.getIsDone()).isTrue();
    }
}
