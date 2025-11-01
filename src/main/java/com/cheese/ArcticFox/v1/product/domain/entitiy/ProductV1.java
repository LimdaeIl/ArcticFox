package com.cheese.ArcticFox.v1.product.domain.entitiy;

import com.cheese.ArcticFox.v1.category.domain.entity.CategoryV1;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "v1_products")
@Entity
public class ProductV1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategoryV1> productCategoriesV1 = new ArrayList<>();

    private ProductV1(String name, String description, Integer price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = LocalDateTime.now();
    }

    public static ProductV1 create(String name, String description, Integer price,
            Integer stock) {
        return new ProductV1(name, description, price, stock);
    }

    public void addCategory(CategoryV1 category) {
        boolean exists = productCategoriesV1.stream()
                .anyMatch(pc -> pc.getCategory().getId().equals(category.getId()));
        if (exists) {
            return;
        }
        ProductCategoryV1 link = ProductCategoryV1.of(this, category);
        productCategoriesV1.add(link);
    }

    public void removeCategory(CategoryV1 category) {
        productCategoriesV1.removeIf(pc -> {
            boolean exists = pc.getCategory().getId().equals(category.getId());
            if (exists) {
                pc.unlink();
            }
            return exists;
        });
    }


    public void decrease(Integer count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        if (count > this.stock) {
            throw new IllegalArgumentException("count must be less than stock");
        }

        this.stock -= count;
    }

    public void increase(Integer count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        this.stock += count;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is blank");
        }
        this.name = name;
    }

    public void updateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is blank");
        }
        this.description = description;
    }

    public void updatePrice(Integer price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        this.price = price;
    }
}
