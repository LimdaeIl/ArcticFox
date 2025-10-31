package com.cheese.ArcticFox.v1.category.application.dto.response;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1Closure;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record CreateCategoryResponse(
        String name,
        Long ancestor,
        Long descendant,
        int level
) {

    public static CreateCategoryResponse from(CategoryV1 category, CategoryV1Closure closure) {
        return CreateCategoryResponse.builder()
                .name(category.getName())
                .ancestor(closure.getAncestor().getId())
                .descendant(closure.getDescendant().getId())
                .level(closure.getLevel())
                .build();
    }
}
