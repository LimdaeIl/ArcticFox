package com.cheese.ArcticFox.v1.product.application.service;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1ClosureRepository;
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

    private final CategoryV1ClosureRepository categoryV1ClosureRepository;

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
        // 1) 카테고리 선택을 자손 포함 ID 리스트로 변환
        List<Long> categoryIds = resolveCategoryIds(condition.categoryId(), condition.categoryName());

        // 2) 레포 V2 호출 (자손 포함)
        Page<ProductV1> page = productV1Repository.findByConditionV2(
                condition.productName(),
                condition.minPrice(),
                condition.maxPrice(),
                condition.inStock(),
                condition.createdFrom(),
                condition.createdTo(),
                categoryIds, // null이면 전체
                pageable
        );

        return page.map(GetProductResponse::from);
    }

    private List<Long> resolveCategoryIds(Long categoryId, String categoryNameOrPath) {
        // (A) categoryId 우선
        if (categoryId != null) {
            return categoryV1ClosureRepository.findDescendantIdsIncludingSelf(categoryId);
        }

        // (B) 이름/경로
        if (categoryNameOrPath == null || categoryNameOrPath.isBlank()) return null;

        String normalized = normalize(categoryNameOrPath);
        Long targetId;

        if (normalized.contains("/")) {
            // 경로: "의류/상의/셔츠"
            String[] segs = normalized.split("/");
            Long cur = null;
            for (String name : segs) {
                CategoryV1 node = categoryV1Repository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + name));
                cur = node.getId();
                // (선택) 직계 검증: 이전 cur와 existsDirectLink 체크 가능
            }
            targetId = cur;
        } else {
            // 단일 이름
            CategoryV1 node = categoryV1Repository.findByName(normalized)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + normalized));
            targetId = node.getId();
        }

        return categoryV1ClosureRepository.findDescendantIdsIncludingSelf(targetId);
    }

    private static String normalize(String raw) {
        String p = raw.trim();
        p = p.replaceAll("^/+|/+$", "");
        p = p.replaceAll("/+", "/");
        return p;
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
