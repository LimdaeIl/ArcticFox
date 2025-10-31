package com.cheese.ArcticFox.v1.category.application.dto.response;

import java.util.List;

public record GetCategoryResponse(
        List<CategoryNodeResponse> roots,
        List<CategoryNodeResponse> directChildren,
        List<CategoryNodeResponse> descendants,
        List<CategoryNodeResponse> breadcrumb
) {

    public static GetCategoryResponse of(
            List<CategoryNodeResponse> roots,
            List<CategoryNodeResponse> directChildren,
            List<CategoryNodeResponse> descendants,
            List<CategoryNodeResponse> breadcrumb) {
        return new GetCategoryResponse(roots, directChildren, descendants, breadcrumb);
    }

    public record CategoryNodeResponse(
            Long id,
            String name
    ) {

        public static CategoryNodeResponse of(Long id, String name) {
            return new CategoryNodeResponse(id, name);
        }
    }
}
