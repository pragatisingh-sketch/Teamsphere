package com.vbs.capsAllocation.repository;

import com.vbs.capsAllocation.model.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationTypeRepository extends JpaRepository<RelationType, Long> {
}