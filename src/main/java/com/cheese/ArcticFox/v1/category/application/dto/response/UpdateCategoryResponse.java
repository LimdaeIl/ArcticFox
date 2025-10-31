package com.cheese.ArcticFox.v1.category.application.dto.response;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;

public record UpdateCategoryResponse(
        Long categoryId,
        String name
) {

    public static UpdateCategoryResponse from(CategoryV1 categoryV1) {
        return new  UpdateCategoryResponse(
                categoryV1.getId(),
                categoryV1.getName()
        );
    }
}
