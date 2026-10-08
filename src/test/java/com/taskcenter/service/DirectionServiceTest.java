package com.taskcenter.service;

import com.taskcenter.dto.DirectionDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.model.Direction;
import com.taskcenter.model.User;
import com.taskcenter.repository.DirectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DirectionServiceTest {

    @Mock
    private DirectionRepository directionRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    private DirectionService directionService;
    private User testUser;

    @BeforeEach
    void setUp() {
        directionService = new DirectionService(directionRepository, authorizationService);
        testUser = User.builder().id("user-1").name("Test User").build();
    }

    @Test
    void testGetDirections() {
        Direction d1 = Direction.builder().id("d-1").workspaceId("ws-1").name("Frontend").build();
        Direction d2 = Direction.builder().id("d-2").workspaceId("ws-1").name("Backend").build();

        when(directionRepository.findByWorkspaceId("ws-1")).thenReturn(List.of(d1, d2));

        List<DirectionDto> result = directionService.getDirections("ws-1", testUser);

        assertEquals(2, result.size());
        assertEquals("Frontend", result.get(0).getName());
        assertEquals("Backend", result.get(1).getName());
        verify(authorizationService).checkAccess("ws-1", testUser);
    }

    @Test
    void testCreateDirection() {
        DirectionDto req = DirectionDto.builder().name("QA Engineer").color("#10B981").build();
        Direction saved = Direction.builder().id("d-3").workspaceId("ws-1").name("QA Engineer").color("#10B981").build();

        when(directionRepository.existsByWorkspaceIdAndNameIgnoreCase("ws-1", "QA Engineer")).thenReturn(false);
        when(directionRepository.saveAndFlush(any(Direction.class))).thenReturn(saved);

        DirectionDto result = directionService.createDirection("ws-1", req, testUser);

        assertNotNull(result);
        assertEquals("d-3", result.getId());
        assertEquals("QA Engineer", result.getName());
        assertEquals("#10B981", result.getColor());
        verify(authorizationService).checkCanEdit("ws-1", testUser);
    }

    @Test
    void testCreateDirection_duplicateName_throwsBadRequestException() {
        DirectionDto req = DirectionDto.builder().name("Frontend").build();
        when(directionRepository.existsByWorkspaceIdAndNameIgnoreCase("ws-1", "Frontend")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> directionService.createDirection("ws-1", req, testUser));
    }

    @Test
    void testCreateDirection_parallelRaceCondition_throwsBadRequestException() {
        DirectionDto req = DirectionDto.builder().name("Frontend").build();
        when(directionRepository.existsByWorkspaceIdAndNameIgnoreCase("ws-1", "Frontend")).thenReturn(false);
        when(directionRepository.saveAndFlush(any(Direction.class))).thenThrow(new DataIntegrityViolationException("Duplicate key unique constraint"));

        assertThrows(BadRequestException.class, () -> directionService.createDirection("ws-1", req, testUser));
    }

    @Test
    void testUpdateDirection() {
        Direction existing = Direction.builder().id("d-1").workspaceId("ws-1").name("Frontend").color("#3B82F6").build();
        Direction updated = Direction.builder().id("d-1").workspaceId("ws-1").name("Frontend React").color("#00FF00").build();

        when(directionRepository.findByIdAndWorkspaceId("d-1", "ws-1")).thenReturn(Optional.of(existing));
        when(directionRepository.findByWorkspaceIdAndNameIgnoreCase("ws-1", "Frontend React")).thenReturn(Optional.empty());
        when(directionRepository.saveAndFlush(any(Direction.class))).thenReturn(updated);

        DirectionDto req = DirectionDto.builder().name("Frontend React").color("#00FF00").build();
        DirectionDto result = directionService.updateDirection("ws-1", "d-1", req, testUser);

        assertEquals("Frontend React", result.getName());
        assertEquals("#00FF00", result.getColor());
    }

    @Test
    void testUpdateDirection_sameNamePreserved_updatesSuccessfully() {
        Direction existing = Direction.builder().id("d-1").workspaceId("ws-1").name("Frontend").color("#3B82F6").build();
        when(directionRepository.findByIdAndWorkspaceId("d-1", "ws-1")).thenReturn(Optional.of(existing));
        when(directionRepository.findByWorkspaceIdAndNameIgnoreCase("ws-1", "Frontend")).thenReturn(Optional.of(existing));
        when(directionRepository.saveAndFlush(any(Direction.class))).thenAnswer(inv -> inv.getArgument(0));

        DirectionDto req = DirectionDto.builder().name("Frontend").color("#112233").build();
        DirectionDto result = directionService.updateDirection("ws-1", "d-1", req, testUser);

        assertEquals("Frontend", result.getName());
        assertEquals("#112233", result.getColor());
    }

    @Test
    void testDeleteDirection_unlinksFromTasksAndDeletes() {
        Direction existing = Direction.builder().id("d-1").workspaceId("ws-1").name("Frontend").build();
        when(directionRepository.findByIdAndWorkspaceId("d-1", "ws-1")).thenReturn(Optional.of(existing));

        directionService.deleteDirection("ws-1", "d-1", testUser);

        verify(authorizationService).checkAdmin("ws-1", testUser);
        verify(directionRepository).unlinkDirectionFromAllTasks("d-1");
        verify(directionRepository).delete(existing);
    }
}
