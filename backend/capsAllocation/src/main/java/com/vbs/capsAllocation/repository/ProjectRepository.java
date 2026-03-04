package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Project;
import com.vbs.capsAllocation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectCode(String projectCode);
    List<Project> findByCreatedBy(User createdBy);
}
