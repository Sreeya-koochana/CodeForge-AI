package com.aibuilder.repository;

import com.aibuilder.entity.GeneratedFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedFileRepository extends JpaRepository<GeneratedFile, Long> {

    List<GeneratedFile> findByApiDefinitionIdOrderByFileNameAsc(Long apiDefinitionId);

    long countByApiDefinitionProjectId(Long projectId);

    long countByApiDefinitionProjectUserId(Long userId);
}
