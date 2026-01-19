package com.mihai.overview.repository;

import com.mihai.overview.entity.InteractionKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionKpiRepository extends JpaRepository<InteractionKpi, Long> {
    List<InteractionKpi> findByReviewType_Id(Long reviewTypeId);
}
