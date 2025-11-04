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

    // --- Direct children (entities) ---
    @Query("""
        select (count(cc) > 0)
        from CategoryV1Closure cc
        where cc.id.ancestorId = :ancestorId
          and cc.level = 1
    """)
    boolean existsDirectChild(Long ancestorId);

    @Query("""
        select cc.descendant
        from CategoryV1Closure cc
        where cc.id.ancestorId = :ancestorId
          and cc.level = 1
        order by cc.descendant.name
    """)
    List<CategoryV1> findDirectChildren(Long ancestorId);


    // --- Paths/Depth ---
    @Query("""
        select coalesce(max(cc.level), 0)
        from CategoryV1Closure cc
        where cc.id.descendantId = :id
    """)
    int findDepthOf(Long id);

    @Query("""
        select count(cc) > 0
        from CategoryV1Closure cc
        where cc.id.ancestorId = :parentId
          and cc.id.descendantId = :childId
          and cc.level = 1
    """)
    boolean existsDirectLink(Long parentId, Long childId);


    // --- DELETE all links related to a node ---
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from CategoryV1Closure cc
        where cc.id.ancestorId = :id
           or cc.id.descendantId = :id
    """)
    int deleteAllLinksOf(Long id);


    // ---------- NEW: Descendant/Ancestor IDs with explicit naming ----------

    /** 조상 ID 기준: 자기 자신 포함한 모든 자손 ID 반환 (level >= 0) */
    @Query("""
        select cc.id.descendantId
        from CategoryV1Closure cc
        where cc.id.ancestorId = :ancestorId
        order by cc.level, cc.id.descendantId
    """)
    List<Long> findDescendantIdsIncludingSelf(Long ancestorId);

    /** 조상 ID 기준: 자기 자신 제외한 모든 자손 ID 반환 (level >= 1) */
    @Query("""
        select cc.id.descendantId
        from CategoryV1Closure cc
        where cc.id.ancestorId = :ancestorId
          and cc.level >= 1
        order by cc.level, cc.id.descendantId
    """)
    List<Long> findDescendantIdsExcludingSelf(Long ancestorId);

    /** 자손 ID 기준: 자기 자신 포함한 모든 조상 ID 반환 (level >= 0) */
    @Query("""
        select cc.id.ancestorId
        from CategoryV1Closure cc
        where cc.id.descendantId = :descendantId
        order by cc.level desc, cc.id.ancestorId
    """)
    List<Long> findAncestorIdsIncludingSelf(Long descendantId);


    // -------- OLD: 의미 모호/혼동 야기 가능 → 유지하되 사용 지양 --------
    @Deprecated
    @Query("""
        select cc
        from CategoryV1Closure cc
        where cc.id.descendantId = :descendantId
    """)
    List<CategoryV1Closure> findByDescendant_Id(Long descendantId);
}

