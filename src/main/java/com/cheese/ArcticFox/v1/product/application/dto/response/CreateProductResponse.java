package com.cheese.ArcticFox.v1.product.application.dto.response;

import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record CreateProductResponse(
        Long id,
        String name,
        String description,
        Integer price,
        Integer stock
) {

    public static CreateProductResponse from(ProductV1 savedProductV1) {
        return CreateProductResponse.builder()
                .id(savedProductV1.getId())
                .name(savedProductV1.getName())
                .description(savedProductV1.getDescription())
                .price(savedProductV1.getPrice())
                .stock(savedProductV1.getStock())
                .build();
    }
}
