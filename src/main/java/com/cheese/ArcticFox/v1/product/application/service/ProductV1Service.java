package com.cheese.ArcticFox.v1.product.application.service;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import com.cheese.ArcticFox.v1.product.application.dto.request.CreateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.request.ProductSearchCondition;
import com.cheese.ArcticFox.v1.product.application.dto.request.UpdateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.response.CreateProductResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.ProductStockResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.GetProductResponse;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductCategoryV1;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import com.cheese.ArcticFox.v1.product.domain.repository.ProductV1Repository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductV1Service {

    private final ProductV1Repository productV1Repository;

    private final CategoryV1Repository categoryV1Repository;

    private ProductV1 findProductById(Long id) {
        return productV1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No product found with id: " + id));
    }

    @Transactional
    public CreateProductResponse create(CreateProductRequest request) {

        ProductV1 careatedProductV1 = ProductV1.create(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        CategoryV1 categoryV1 = categoryV1Repository.findByName(request.categoryName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "not found category by name: " + request.categoryName()));

        careatedProductV1.addCategory(categoryV1);

        ProductV1 savedProductV1 = productV1Repository.save(careatedProductV1);

        return CreateProductResponse.from(savedProductV1);
    }

    @Transactional
    public ProductStockResponse decrease(Long productId, Integer count) {
        ProductV1 productById = findProductById(productId);

        productById.decrease(count);

        return ProductStockResponse.from(productById);
    }

    @Transactional(readOnly = true)
    public Page<GetProductResponse> get(ProductSearchCondition condition, Pageable pageable) {
        Page<ProductV1> page = productV1Repository.findByCondition(
                condition.productName(),
                condition.categoryName(),
                condition.categoryId(),
                condition.minPrice(),
                condition.maxPrice(),
                condition.inStock(),
                condition.createdFrom(),
                condition.createdTo(),
                pageable
        );
        return page.map(GetProductResponse::from);
    }

    @Transactional
    public ProductStockResponse increase(Long productId, Integer count) {
        ProductV1 productById = findProductById(productId);

        productById.increase(count);

        return ProductStockResponse.from(productById);
    }

    @Transactional
    public GetProductResponse update(Long productId, UpdateProductRequest request) {
        ProductV1 productById = findProductById(productId);

        if (request.name() != null) {
            productById.updateName(request.name());
        }
        if (request.description() != null) {
            productById.updateDescription(request.description());
        }
        if (request.price() != null) {
            productById.updatePrice(request.price());
        }

        // 카테고리 교체: 단일
        if (request.category() != null) {
            CategoryV1 target = categoryV1Repository.findByName(request.category())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "not found category by name: " + request.category()));

            // 현재 연결된 카테고리 중 target이 아니면 제거
            List<CategoryV1> toRemove = productById.getProductCategoriesV1().stream()
                    .map(ProductCategoryV1::getCategory)
                    .filter(c -> !c.getId().equals(target.getId()))
                    .toList();
            toRemove.forEach(productById::removeCategory);

            // target 미연결이면 추가
            productById.addCategory(target);
        }

        return GetProductResponse.from(productById);
    }

    @Transactional
    public void delete(Long productId) {
        ProductV1 productById = findProductById(productId);

        productV1Repository.delete(productById);
    }
}
