package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.IssueDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<IssueDetails, Long> {
    List<IssueDetails> findByReporterId(Long reporterId);
}
