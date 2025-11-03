package com.cheese.ArcticFox.v1.product.application.dto.response;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductCategoryV1;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record GetProductResponse(
        Long id,
        String name,
        String description,
        Integer price,
        Integer stock,
        LocalDateTime createdAt,
        List<CategorySummary> categories
) {

    public static GetProductResponse from(ProductV1 productV1) {
        List<CategorySummary> cats =
                productV1.getProductCategoriesV1().stream()
                        .map(ProductCategoryV1::getCategory)
                        .filter(Objects::nonNull)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        CategoryV1::getId,
                                        categoryV1 -> new CategorySummary(
                                                categoryV1.getId(),
                                                categoryV1.getName()
                                        ),
                                        (a, b) -> a, LinkedHashMap::new
                                ),
                                m -> new ArrayList<>(m.values())
                        ));

        return new GetProductResponse(
                productV1.getId(),
                productV1.getName(),
                productV1.getDescription(),
                productV1.getPrice(),
                productV1.getStock(),
                productV1.getCreatedAt(),
                cats
        );
    }

    public record CategorySummary(
            Long id,
            String name
    ) {

    }
}
