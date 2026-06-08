package com.example.omnimetric.repository;

import com.example.omnimetric.model.entity.QueryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryTaskRepository extends JpaRepository<QueryTask, Long> {
}
