package com.mihai.overview.repository;

import com.mihai.overview.entity.KpiPoolItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KpiPoolItemRepository extends JpaRepository<KpiPoolItem, Long> {
    List<KpiPoolItem> findByInteractionType_Id(Long interactionTypeId);

    List<KpiPoolItem> findAllByInteractionTypeIdAndArchivedFalseOrderByNameAsc(Long interactionTypeId);
}
