package com.aibuilder.service;

import com.aibuilder.dto.CreateProjectRequest;
import com.aibuilder.dto.ProjectResponse;
import com.aibuilder.entity.Project;
import com.aibuilder.entity.User;
import com.aibuilder.repository.ApiDefinitionRepository;
import com.aibuilder.repository.GeneratedFileRepository;
import com.aibuilder.repository.ProjectRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final GeneratedFileRepository generatedFileRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ApiDefinitionRepository apiDefinitionRepository,
            GeneratedFileRepository generatedFileRepository
    ) {
        this.projectRepository = projectRepository;
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.generatedFileRepository = generatedFileRepository;
    }

    public List<ProjectResponse> getProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapProject)
                .toList();
    }

    public List<ProjectResponse> getProjectsForUser(User user) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapProject)
                .toList();
    }

    public ProjectResponse createProject(CreateProjectRequest request, User user) {
        Project project = Project.builder()
                .name(request.name().trim())
                .description(request.description())
                .user(user)
                .build();

        return mapProject(projectRepository.save(project));
    }

    public void deleteProject(Long projectId, User user) {
        Project project = getOwnedProject(projectId, user);
        projectRepository.delete(project);
    }

    public Project getRequiredProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    public Project getOwnedProject(Long projectId, User user) {
        Project project = getRequiredProject(projectId);
        if (!project.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not have access to this project.");
        }
        return project;
    }

    public long countProjects() {
        return projectRepository.count();
    }

    public long countProjectsForUser(User user) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size();
    }

    private ProjectResponse mapProject(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getUser().getId(),
                project.getUser().getName(),
                project.getCreatedAt(),
                apiDefinitionRepository.countByProjectId(project.getId()),
                generatedFileRepository.countByApiDefinitionProjectId(project.getId())
        );
    }
}
