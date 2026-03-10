package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.User;
import com.vbs.capsAllocation.model.Role;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Transactional
    void deleteByUsername(String username);
    List<User> findByUsernameIn(List<String> usernames);
    List<User> findByRoleIn(List<Role> roles);
}
