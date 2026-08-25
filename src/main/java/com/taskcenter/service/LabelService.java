package com.taskcenter.service;

import com.taskcenter.dto.LabelDto;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Label;
import com.taskcenter.model.User;
import com.taskcenter.repository.LabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabelService {

    private final LabelRepository labelRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public LabelService(LabelRepository labelRepository,
                        WorkspaceAuthorizationService authorizationService) {
        this.labelRepository = labelRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<LabelDto> getLabels(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        return labelRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(LabelDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LabelDto createLabel(String workspaceId, LabelDto req, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        Label label = Label.builder()
                .workspaceId(workspaceId)
                .name(req.getName())
                .color(req.getColor())
                .build();

        Label saved = labelRepository.save(label);
        return LabelDto.fromEntity(saved);
    }

    @Transactional
    public void deleteLabel(String workspaceId, String id, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label topilmadi: " + id));

        if (!workspaceId.equals(label.getWorkspaceId())) {
            throw new ResourceNotFoundException("Label ushbu workspace ga tegishli emas");
        }

        labelRepository.deleteById(id);
    }
}
