package com.cheese.ArcticFox.v1.category.domain.repository;


import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1ClosureId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CategoryV1ClosureRepository extends
        JpaRepository<CategoryV1Closure, CategoryV1ClosureId> {

    @Query("""
        select (count(cc) > 0)
        from CategoryV1Closure cc
        where cc.id.ancestorId = :ancestorId
          and cc.level = 1
    """)
    boolean existsDirectChild(Long ancestorId);

    @Query("""
             SELECT cc.descendant
             FROM CategoryV1Closure AS cc
             WHERE cc.id.ancestorId = :ancestorId
             AND cc.level = 1
             ORDER BY cc.descendant.name
            """)
    List<CategoryV1> findDirectChildren(Long ancestorId);

    @Query("""
            SELECT cc
            FROM CategoryV1Closure AS cc
            WHERE cc.id.descendantId = :descendantId
            """)
    List<CategoryV1Closure> findByDescendant_Id(Long descendantId);

    @Query("""
              SELECT COUNT(cc) > 0
              FROM CategoryV1Closure cc
              WHERE cc.id.ancestorId = :parentId
                AND cc.id.descendantId = :childId
                AND cc.level = 1
            """)
    boolean existsDirectLink(Long parentId, Long childId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from CategoryV1Closure cc
        where cc.id.ancestorId = :id
           or cc.id.descendantId = :id
    """)
    int deleteAllLinksOf(Long id);
}
