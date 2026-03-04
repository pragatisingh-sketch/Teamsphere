package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.Project;
import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.model.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProjectRepository extends JpaRepository<UserProject, Long> {
    List<UserProject> findByUser(User user);
    List<UserProject> findByProject(Project project);
    Optional<UserProject> findByUserAndProject(User user, Project project);
}
