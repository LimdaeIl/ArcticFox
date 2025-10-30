package com.cheese.ArcticFox.v1.category.domain.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class CategoryV1ClosureId implements Serializable {

    private Long ancestorId;
    private Long descendantId;

    private CategoryV1ClosureId(Long ancestorId, Long descendantId) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
    }

    public static CategoryV1ClosureId create(Long ancestorId, Long descendantId) {
        return  new CategoryV1ClosureId(ancestorId, descendantId);
    }
}
