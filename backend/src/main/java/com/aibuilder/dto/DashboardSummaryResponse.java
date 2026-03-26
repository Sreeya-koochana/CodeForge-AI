package com.aibuilder.dto;

import java.util.List;

public record DashboardSummaryResponse(
        long userCount,
        long projectCount,
        long apiCount,
        long fileCount,
        List<ProjectResponse> recentProjects
) {
}
