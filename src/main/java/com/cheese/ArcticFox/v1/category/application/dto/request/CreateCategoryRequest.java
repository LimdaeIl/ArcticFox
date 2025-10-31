package com.cheese.ArcticFox.v1.category.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(

        @NotBlank(message = "카테고리명: 카테고리명은 필수 입니다.")
        String name,

        Long parentId
) {

}
