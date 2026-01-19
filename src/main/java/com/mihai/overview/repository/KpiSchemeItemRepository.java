package com.mihai.overview.repository;

import com.mihai.overview.entity.KpiSchemeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KpiSchemeItemRepository extends JpaRepository<KpiSchemeItem, Long> {
    List<KpiSchemeItem> findByScheme_Id(Long schemeId);
}
