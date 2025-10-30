package com.cheese.ArcticFox.v1.category.domain.repository;


import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryV1ClosureRepository extends JpaRepository<CategoryV1Closure, Long> {

    List<CategoryV1Closure> findByDescendant_Id(Long descendantId);
}
