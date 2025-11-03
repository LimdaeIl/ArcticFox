package com.cheese.ArcticFox.v1.product.application.dto.response;

import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record ProductStockResponse(
        Long id,
        String name,
        Integer price,
        Integer stock
) {

    public static ProductStockResponse from(ProductV1 productById) {
        return ProductStockResponse
                .builder()
                .id(productById.getId())
                .name(productById.getName())
                .price(productById.getPrice())
                .stock(productById.getStock())
                .build();
    }
}
