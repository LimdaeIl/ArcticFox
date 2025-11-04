package com.cheese.ArcticFox.v1.product.domain.repository;

import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ProductV1Repository extends CrudRepository<ProductV1, Long> {

    // (기존) 단일 이름/ID 기반
    @Query(
            value = """
            SELECT DISTINCT p
            FROM ProductV1 p
            LEFT JOIN p.productCategoriesV1 pc
            LEFT JOIN pc.category c
            WHERE (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:minPrice   IS NULL OR p.price >= :minPrice)
              AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
              AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)
              AND (:createdTo   IS NULL OR p.createdAt <= :createdTo)
              AND (
                     (:categoryId IS NULL AND :categoryName IS NULL)
                     OR (:categoryId IS NOT NULL AND c.id = :categoryId)
                     OR (:categoryId IS NULL AND :categoryName IS NOT NULL AND c.name = :categoryName)
                  )
              AND (
                     (:inStock IS NULL)
                     OR (:inStock = TRUE AND p.stock > 0)
                     OR (:inStock = FALSE AND p.stock = 0)
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM ProductV1 p
            LEFT JOIN p.productCategoriesV1 pc
            LEFT JOIN pc.category c
            WHERE (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:minPrice   IS NULL OR p.price >= :minPrice)
              AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
              AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)
              AND (:createdTo   IS NULL OR p.createdAt <= :createdTo)
              AND (
                     (:categoryId IS NULL AND :categoryName IS NULL)
                     OR (:categoryId IS NOT NULL AND c.id = :categoryId)
                     OR (:categoryId IS NULL AND :categoryName IS NOT NULL AND c.name = :categoryName)
                  )
              AND (
                     (:inStock IS NULL)
                     OR (:inStock = TRUE AND p.stock > 0)
                     OR (:inStock = FALSE AND p.stock = 0)
                  )
        """
    )
    Page<ProductV1> findByCondition(
            String productName,
            String categoryName,
            Long categoryId,
            Integer minPrice,
            Integer maxPrice,
            Boolean inStock,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Pageable pageable
    );

    // (신규) 자손 포함 categoryIds 기반
    @Query(
            value = """
            SELECT DISTINCT p
            FROM ProductV1 p
            JOIN p.productCategoriesV1 pc
            JOIN pc.category c
            WHERE (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:minPrice   IS NULL OR p.price >= :minPrice)
              AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
              AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)
              AND (:createdTo   IS NULL OR p.createdAt <= :createdTo)
              AND ( :categoryIds IS NULL OR c.id IN :categoryIds )
              AND (
                     (:inStock IS NULL)
                     OR (:inStock = TRUE AND p.stock > 0)
                     OR (:inStock = FALSE AND p.stock = 0)
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p)
            FROM ProductV1 p
            JOIN p.productCategoriesV1 pc
            JOIN pc.category c
            WHERE (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:minPrice   IS NULL OR p.price >= :minPrice)
              AND (:maxPrice   IS NULL OR p.price <= :maxPrice)
              AND (:createdFrom IS NULL OR p.createdAt >= :createdFrom)
              AND (:createdTo   IS NULL OR p.createdAt <= :createdTo)
              AND ( :categoryIds IS NULL OR c.id IN :categoryIds )
              AND (
                     (:inStock IS NULL)
                     OR (:inStock = TRUE AND p.stock > 0)
                     OR (:inStock = FALSE AND p.stock = 0)
                  )
        """
    )
    Page<ProductV1> findByConditionV2(
            String productName,
            Integer minPrice,
            Integer maxPrice,
            Boolean inStock,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            List<Long> categoryIds,
            Pageable pageable
    );
}
