package com.aibuilder.controller;

import com.aibuilder.dto.CreateProjectRequest;
import com.aibuilder.dto.ProjectResponse;
import com.aibuilder.service.ProjectService;
import com.aibuilder.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping
    public List<ProjectResponse> getProjects(Authentication authentication) {
        return projectService.getProjectsForUser(userService.getAuthenticatedUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request, Authentication authentication) {
        return projectService.createProject(request, userService.getAuthenticatedUser(authentication));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long projectId, Authentication authentication) {
        projectService.deleteProject(projectId, userService.getAuthenticatedUser(authentication));
    }
}
