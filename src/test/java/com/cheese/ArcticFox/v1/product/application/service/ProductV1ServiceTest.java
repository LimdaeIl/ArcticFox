package com.cheese.ArcticFox.v1.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import com.cheese.ArcticFox.v1.category.domain.repository.CategoryV1Repository;
import com.cheese.ArcticFox.v1.product.application.dto.request.CreateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.request.ProductSearchCondition;
import com.cheese.ArcticFox.v1.product.application.dto.request.UpdateProductRequest;
import com.cheese.ArcticFox.v1.product.application.dto.response.CreateProductResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.GetProductResponse;
import com.cheese.ArcticFox.v1.product.application.dto.response.ProductStockResponse;
import com.cheese.ArcticFox.v1.product.domain.entitiy.ProductV1;
import com.cheese.ArcticFox.v1.product.domain.repository.ProductV1Repository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;


@DisplayName("상품 서비스")
@ExtendWith(MockitoExtension.class)
class ProductV1ServiceTest {

    @InjectMocks
    private ProductV1Service productV1Service;

    @Mock
    private ProductV1Repository productV1Repository;

    @Mock
    private CategoryV1Repository categoryV1Repository;

    // 공통 변수
    private String categoryName;
    private CategoryV1 savedCategory;

    @BeforeEach
    public void setup() {
        categoryName = "food";
        savedCategory = CategoryV1.create(categoryName);

        ReflectionTestUtils.setField(savedCategory, "id", 1L);
    }

    @DisplayName("성공: 상품 생성")
    @Test
    void create_product_success() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "치즈",
                "맛있는 치즈",
                1000,
                100,
                categoryName);

        when(categoryV1Repository.findByName(request.categoryName())).thenReturn(
                Optional.of(savedCategory));
        ProductV1 saved = ProductV1.create(request.name(), request.description(), request.price(),
                request.stock());
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(productV1Repository.save(
                org.mockito.ArgumentMatchers.any(ProductV1.class))).thenReturn(saved);

        // when
        CreateProductResponse response = productV1Service.create(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("치즈");
        assertThat(response.price()).isEqualTo(1000);
        assertThat(response.stock()).isEqualTo(100);
        verify(categoryV1Repository).findByName(categoryName);
        verify(productV1Repository).save(org.mockito.ArgumentMatchers.any(ProductV1.class));
    }

    @DisplayName("실패: 존재하지 않는 카테고리 이름으로 상품 생성")
    @Test
    void create_product_fail_category_not_found() {
        // given
        CreateProductRequest request = new CreateProductRequest(
                "치즈", "맛있는 치즈", 1000, 100, "no-such-category"
        );

        when(categoryV1Repository.findByName("no-such-category"))
                .thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productV1Service.create(request)
        );

        assertThat(ex.getMessage()).isEqualTo("not found category by name: no-such-category");
        verify(productV1Repository, org.mockito.Mockito.never()).save(
                org.mockito.ArgumentMatchers.any());

    }

    @DisplayName("성공: 재고 감소")
    @Test
    void decrease_stock_success() {
        // given
        ProductV1 p = ProductV1.create("치즈", "맛있는 치즈", 1000, 100);
        ReflectionTestUtils.setField(p, "id", 11L);

        when(productV1Repository.findById(11L)).thenReturn(Optional.of(p));

        // when
        ProductStockResponse res = productV1Service.decrease(11L, 5);

        // then
        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(11L);
        assertThat(res.stock()).isEqualTo(95);
    }

    @DisplayName("성공: 재고 증가")
    @Test
    void increase_stock_success() {
        // given
        ProductV1 p = ProductV1.create("치즈", "맛있는 치즈", 1000, 100);
        ReflectionTestUtils.setField(p, "id", 12L);

        when(productV1Repository.findById(12L)).thenReturn(Optional.of(p));

        // when
        ProductStockResponse res = productV1Service.increase(12L, 7);

        // then
        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(12L);
        assertThat(res.stock()).isEqualTo(107);
    }

    @DisplayName("실패: 재고보다 많이 차감 시 예외")
    @Test
    void decrease_stock_fail_overflow() {
        ProductV1 p = ProductV1.create("치즈", "맛있는 치즈", 1000, 3);
        ReflectionTestUtils.setField(p, "id", 13L);
        when(productV1Repository.findById(13L)).thenReturn(Optional.of(p));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productV1Service.decrease(13L, 5)
        );
        assertThat(ex.getMessage()).isEqualTo("count must be less than stock");
    }

    @DisplayName("성공: 상품 정보 수정 (이름/설명/가격 + 카테고리 교체)")
    @Test
    void update_product_success_with_category_replace() {
        // given: 기존 상품
        ProductV1 product = ProductV1.create("치즈", "맛있는 치즈", 1000, 100);
        ReflectionTestUtils.setField(product, "id", 20L);

        // 기존 카테고리 old
        CategoryV1 oldCat = CategoryV1.create("old");
        ReflectionTestUtils.setField(oldCat, "id", 100L);
        product.addCategory(oldCat);

        // 교체 대상 카테고리 target
        CategoryV1 targetCat = CategoryV1.create("food");
        ReflectionTestUtils.setField(targetCat, "id", 101L);

        when(productV1Repository.findById(20L)).thenReturn(Optional.of(product));
        when(categoryV1Repository.findByName("food")).thenReturn(Optional.of(targetCat));

        UpdateProductRequest req = new UpdateProductRequest(
                "슈퍼치즈", "더 맛있는 치즈", 1200, "food"
        );

        // when
        GetProductResponse res = productV1Service.update(20L, req);

        // then: 필드 수정 확인
        assertThat(res.name()).isEqualTo("슈퍼치즈");
        assertThat(res.description()).isEqualTo("더 맛있는 치즈");
        assertThat(res.price()).isEqualTo(1200);

        // 카테고리 교체 확인: old 제거, food 유지
        List<String> categoryNames = product.getProductCategoriesV1().stream()
                .map(pc -> pc.getCategory().getName())
                .toList();
        assertThat(categoryNames).containsExactly("food");
    }

    @DisplayName("실패: 수정 시 카테고리 이름이 존재하지 않음")
    @Test
    void update_product_fail_category_not_found() {
        ProductV1 product = ProductV1.create("치즈", "맛있는 치즈", 1000, 100);
        ReflectionTestUtils.setField(product, "id", 21L);
        when(productV1Repository.findById(21L)).thenReturn(Optional.of(product));
        when(categoryV1Repository.findByName("nope")).thenReturn(Optional.empty());

        UpdateProductRequest req = new UpdateProductRequest(
                null, null, null, "nope"
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productV1Service.update(21L, req)
        );
        assertThat(ex.getMessage()).isEqualTo("not found category by name: nope");
    }

    @DisplayName("성공: 상품 삭제")
    @Test
    void delete_product_success() {
        // given
        ProductV1 p = ProductV1.create("치즈", "맛있는 치즈", 1000, 100);
        ReflectionTestUtils.setField(p, "id", 30L);
        when(productV1Repository.findById(30L)).thenReturn(Optional.of(p));

        // when
        productV1Service.delete(30L);

        // then
        verify(productV1Repository).delete(p);
    }

    @DisplayName("실패: 삭제 대상 상품 없음")
    @Test
    void delete_product_fail_not_found() {
        when(productV1Repository.findById(404L)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> productV1Service.delete(404L)
        );
        assertThat(ex.getMessage()).isEqualTo("No product found with id: 404");
        verify(productV1Repository, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @DisplayName("성공: 조건 검색 및 응답 매핑")
    @Test
    void get_products_by_condition_success() {
        // given
        ProductV1 p1 = ProductV1.create("치즈A", "A치즈", 1500, 50);
        ReflectionTestUtils.setField(p1, "id", 40L);
        ProductV1 p2 = ProductV1.create("치즈B", "B치즈", 2000, 0);
        ReflectionTestUtils.setField(p2, "id", 41L);

        PageRequest pageable = PageRequest.of(0, 10);

        when(productV1Repository.findByCondition(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        ProductSearchCondition cond = new ProductSearchCondition(
                "치즈", null, null, null, null, null, null, null
        );

        // when
        Page<GetProductResponse> page = productV1Service.get(cond, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).id()).isEqualTo(40L);
        assertThat(page.getContent().get(0).name()).isEqualTo("치즈A");
        assertThat(page.getContent().get(1).id()).isEqualTo(41L);
        assertThat(page.getContent().get(1).name()).isEqualTo("치즈B");
    }
}
