package com.cheese.ArcticFox.v1.product.application.dto.request;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record ProductSearchCondition(
        String productName,
        String categoryName,
        Long categoryId,
        Integer minPrice,
        Integer maxPrice,
        Boolean inStock,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo
) {
}
