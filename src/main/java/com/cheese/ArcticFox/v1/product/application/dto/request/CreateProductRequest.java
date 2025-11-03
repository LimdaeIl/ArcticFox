package com.cheese.ArcticFox.v1.product.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(

        @NotNull(message = "상품: 상품명은 필수 입니다.")
        String name,

        @NotNull(message = "상품: 상품 설명은 필수 입니다.")
        String description,

        @NotNull(message = "상품: 상품 가격은 필수 입니다.")
        Integer price,

        @NotNull(message = "상품: 상품 재고는 필수 입니다.")
        Integer stock,

        @NotNull(message = "상품: 카테고리는 필수 입니다.")
        String categoryName
) {

}
