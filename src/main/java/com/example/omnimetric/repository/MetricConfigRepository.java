package com.example.omnimetric.repository;

import com.example.omnimetric.model.entity.MetricConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricConfigRepository extends JpaRepository<MetricConfig, Long> {
    List<MetricConfig> findByEnabledTrue();
}
