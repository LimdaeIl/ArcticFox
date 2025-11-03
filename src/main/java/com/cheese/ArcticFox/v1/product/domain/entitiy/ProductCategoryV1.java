package com.cheese.ArcticFox.v1.product.domain.entitiy;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "v1_product_categories")
@Entity
public class ProductCategoryV1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_category_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductV1 product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryV1 category;

    private ProductCategoryV1(ProductV1 product, CategoryV1 category) {
        this.product = product;
        this.category = category;
    }

    public static ProductCategoryV1 of(ProductV1 product, CategoryV1 category) {
        return new ProductCategoryV1(product, category);
    }

    public void unlink() {
        this.category = null;
        this.product = null;
    }
}
