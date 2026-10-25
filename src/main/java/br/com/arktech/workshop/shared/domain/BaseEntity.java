package br.com.arktech.workshop.shared.domain;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

import org.jspecify.annotations.Nullable;

@Getter
@MappedSuperclass
public abstract class BaseEntity extends TimeStamping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BaseEntity entity) || !isSameTypeAs(entity)) {
            return false;
        }

        return getId() != null && getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return BaseEntity.class.hashCode();
    }

    private boolean isSameTypeAs(BaseEntity entity) {
        return getClass().isAssignableFrom(entity.getClass()) || entity.getClass().isAssignableFrom(getClass());
    }
}
