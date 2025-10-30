package com.cheese.ArcticFox.v1.category.domain.repository;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryV1Repository extends JpaRepository<CategoryV1, Long> {

    boolean existsByName(String name);
}
