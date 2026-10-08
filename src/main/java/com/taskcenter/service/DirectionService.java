package com.taskcenter.service;

import com.taskcenter.dto.DirectionDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Direction;
import com.taskcenter.model.User;
import com.taskcenter.repository.DirectionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DirectionService {

    private final DirectionRepository directionRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public DirectionService(DirectionRepository directionRepository,
                            WorkspaceAuthorizationService authorizationService) {
        this.directionRepository = directionRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<DirectionDto> getDirections(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        return directionRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(DirectionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public DirectionDto createDirection(String workspaceId, DirectionDto req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        String trimmedName = req.getName().trim();
        if (directionRepository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmedName)) {
            throw new BadRequestException("Ushbu nomdagi yo'nalish allaqachon mavjud: " + trimmedName);
        }

        Direction direction = Direction.builder()
                .workspaceId(workspaceId)
                .name(trimmedName)
                .color(req.getColor() != null && !req.getColor().isBlank() ? req.getColor().trim() : "#3B82F6")
                .build();

        try {
            Direction saved = directionRepository.saveAndFlush(direction);
            return DirectionDto.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Ushbu nomdagi yo'nalish allaqachon mavjud: " + trimmedName);
        }
    }

    @Transactional
    public DirectionDto updateDirection(String workspaceId, String id, DirectionDto req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Direction direction = directionRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Yo'nalish (Direction) topilmadi: " + id));

        if (req.getName() != null && !req.getName().isBlank()) {
            String trimmedName = req.getName().trim();
            directionRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmedName)
                    .ifPresent(existingWithSameName -> {
                        if (!existingWithSameName.getId().equals(id)) {
                            throw new BadRequestException("Ushbu nomdagi yo'nalish allaqachon mavjud: " + trimmedName);
                        }
                    });
            direction.setName(trimmedName);
        }
        if (req.getColor() != null && !req.getColor().isBlank()) {
            direction.setColor(req.getColor().trim());
        }
        direction.setUpdatedAt(LocalDateTime.now());

        try {
            Direction saved = directionRepository.saveAndFlush(direction);
            return DirectionDto.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Ushbu nomdagi yo'nalish allaqachon mavjud: " + direction.getName());
        }
    }

    @Transactional
    public void deleteDirection(String workspaceId, String id, User currentUser) {
        // Faqat OWNER va ADMIN ga ruxsat beriladi
        authorizationService.checkAdmin(workspaceId, currentUser);

        Direction direction = directionRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Yo'nalish (Direction) topilmadi: " + id));

        // Bog'langan barcha vazifalardan uzish
        directionRepository.unlinkDirectionFromAllTasks(id);

        directionRepository.delete(direction);
    }
}
