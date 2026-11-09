package com.baton.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 생성 일시(createdAt)를 관리하는 기본 엔티티 (A 소유).
 * BaseTimeEntity는 createdAt만 가지며, updatedAt이 필요한 엔티티는 자체적으로 관리한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

