package com.aibuilder.repository;

import com.aibuilder.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);
}
