package com.cheese.ArcticFox.v1.category.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank(message = "카테고리: 카테고리 이름은 필수입니다.")
        String newName
) {

}
