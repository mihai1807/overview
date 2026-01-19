package com.mihai.overview.repository;

import com.mihai.overview.entity.KpiScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KpiSchemeRepository extends JpaRepository<KpiScheme, Long> {
    List<KpiScheme> findByReviewType_Id(Long reviewTypeId);
}
