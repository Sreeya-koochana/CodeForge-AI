package com.aibuilder.repository;

import com.aibuilder.entity.ApiDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiDefinitionRepository extends JpaRepository<ApiDefinition, Long> {

    List<ApiDefinition> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    long countByProjectId(Long projectId);

    long countByProjectUserId(Long userId);
}
