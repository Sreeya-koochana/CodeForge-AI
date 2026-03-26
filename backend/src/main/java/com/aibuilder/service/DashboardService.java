package com.aibuilder.service;

import com.aibuilder.entity.User;
import com.aibuilder.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final UserService userService;
    private final ProjectService projectService;
    private final ApiGenerationService apiGenerationService;

    public DashboardService(UserService userService, ProjectService projectService, ApiGenerationService apiGenerationService) {
        this.userService = userService;
        this.projectService = projectService;
        this.apiGenerationService = apiGenerationService;
    }

    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(
                userService.countUsers(),
                projectService.countProjects(),
                apiGenerationService.countApis(),
                apiGenerationService.countFiles(),
                projectService.getProjects().stream().limit(5).toList()
        );
    }

    public DashboardSummaryResponse getSummary(User user) {
        return new DashboardSummaryResponse(
                1,
                projectService.countProjectsForUser(user),
                apiGenerationService.countApisForUser(user),
                apiGenerationService.countFilesForUser(user),
                projectService.getProjectsForUser(user).stream().limit(5).toList()
        );
    }
}
