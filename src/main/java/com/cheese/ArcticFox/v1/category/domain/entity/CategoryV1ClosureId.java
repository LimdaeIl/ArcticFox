package com.cheese.ArcticFox.v1.category.domain.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class CategoryV1ClosureId implements Serializable {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoryV1ClosureId that)) {
            return false;
        }
        return Objects.equals(ancestorId, that.ancestorId)
                && Objects.equals(descendantId, that.descendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestorId, descendantId);
    }

    private Long ancestorId;
    private Long descendantId;

    private CategoryV1ClosureId(Long ancestorId, Long descendantId) {
        this.ancestorId = ancestorId;
        this.descendantId = descendantId;
    }

    public static CategoryV1ClosureId create(Long ancestorId, Long descendantId) {
        return new CategoryV1ClosureId(ancestorId, descendantId);
    }
}
