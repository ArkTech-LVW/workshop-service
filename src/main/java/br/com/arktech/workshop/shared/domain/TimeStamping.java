package br.com.arktech.workshop.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.Instant;

import lombok.Getter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

@Getter
@MappedSuperclass
public abstract class TimeStamping {

    @CreationTimestamp
    @Column(name = "date_created", nullable = false, updatable = false)
    private @Nullable Instant dateCreated;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private @Nullable Instant lastUpdated;

    @Version
    private @Nullable Long version;
}
