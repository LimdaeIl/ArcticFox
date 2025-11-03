package com.cheese.ArcticFox.v1.product.application.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateProductRequest(

        String name,

        String description,

        @Min(0)
        Integer price,

        String category
) {

}
