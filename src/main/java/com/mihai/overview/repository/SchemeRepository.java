package com.mihai.overview.repository;

import com.mihai.overview.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchemeRepository extends JpaRepository<Scheme, Long> {
    List<Scheme> findByInteractionType_Code(String code);
}
