package com.cheese.ArcticFox.v1.product.presentation;

import com.cheese.ArcticFox.v1.product.application.dto.request.CreateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.request.ProductSearchCondition;
import com.cheese.ArcticFox.v1.product.application.dto.request.UpdateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.response.CreateProductResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.GetProductResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.ProductStockResponse;
import com.cheese.ArcticFox.v1.product.application.service.ProductV1Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@RestController
public class ProductController {

    private final ProductV1Service productV1Service;

    @PostMapping
    public ResponseEntity<CreateProductResponse> create(
            @RequestBody CreateProductRequest request
    ) {

        CreateProductResponse response = productV1Service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<GetProductResponse>> get(
            ProductSearchCondition condition,
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Direction.DESC),
                    @SortDefault(sort = "id", direction = Direction.DESC)
            }) Pageable pageable
    ) {

        Page<GetProductResponse> responses = productV1Service.get(condition, pageable);

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{productId}/decrease/{count}")
    public ResponseEntity<ProductStockResponse> decrease(
            @PathVariable Long productId,
            @PathVariable Integer count
    ) {
        ProductStockResponse response = productV1Service.decrease(productId, count);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/increase/{count}")
    public ResponseEntity<ProductStockResponse> increase(
            @PathVariable Long productId,
            @PathVariable Integer count
    ) {
        ProductStockResponse response = productV1Service.increase(productId, count);
        return ResponseEntity.ok(response);

    }

    @PatchMapping("/{productId}")
    public ResponseEntity<GetProductResponse> update(
            @PathVariable Long productId,
            @RequestBody UpdateProductRequest request
    ) {
        GetProductResponse response = productV1Service.update(productId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        productV1Service.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
