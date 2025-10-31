package com.cheese.ArcticFox.v1.category.domain.repository;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryV1Repository extends JpaRepository<CategoryV1, Long> {

    boolean existsByName(String name);

    Optional<CategoryV1> findByName(String name);


    @Query(value = """
            SELECT c
            FROM CategoryV1 AS c
            WHERE not exists (
                  SELECT cc
                  FROM CategoryV1Closure AS cc
                  WHERE cc.id.descendantId = c.id
                  AND cc.level = 1
            )
            ORDER BY c.name
            """)
    List<CategoryV1> findRoots();
}
