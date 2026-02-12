package com.mihai.overview.repository;

import com.mihai.overview.entity.CriticalConditionPoolItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriticalConditionPoolItemRepository extends JpaRepository<CriticalConditionPoolItem, Long> {
    List<CriticalConditionPoolItem> findByInteractionType_Id(Long interactionTypeId);
    List<CriticalConditionPoolItem> findAllByInteractionTypeIdAndArchivedFalseOrderByNameAsc(Long interactionTypeId);
}
