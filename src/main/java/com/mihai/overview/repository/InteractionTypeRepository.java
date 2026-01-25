package com.mihai.overview.repository;

import com.mihai.overview.entity.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InteractionTypeRepository extends JpaRepository<InteractionType, Long> {
    Optional<InteractionType> findByCode(String code);
}
