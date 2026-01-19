package com.mihai.overview.repository;

import com.mihai.overview.entity.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewTypeRepository extends JpaRepository<ReviewType, Long> {
    Optional<ReviewType> findByCode(String code);
}
