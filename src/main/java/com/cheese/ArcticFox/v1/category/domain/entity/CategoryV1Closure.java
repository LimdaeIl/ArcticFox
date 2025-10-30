package com.cheese.ArcticFox.v1.category.domain.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "v1_category_closures")
@Entity
public class CategoryV1Closure {

    @EmbeddedId
    private CategoryV1ClosureId id;

    private int level;

    @MapsId("ancestorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancestor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_closure_ancestor"))
    private CategoryV1 ancestor;

    @MapsId("descendantId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descendant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_closure_descendant"))
    private CategoryV1 descendant;

    private CategoryV1Closure(
            CategoryV1ClosureId id,
            int level,
            CategoryV1 ancestor,
            CategoryV1 descendant) {
        this.id = id;
        this.level = level;
        this.ancestor = ancestor;
        this.descendant = descendant;
    }

    public static CategoryV1Closure create(
            CategoryV1ClosureId id,
            int level,
            CategoryV1 ancestor,
            CategoryV1 descendant) {
        return new CategoryV1Closure(id, level, ancestor, descendant);
    }
}
