package com.letterfuture.execution.engine.workflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
                @Index(name = "idx_refresh_tokens_revoked_at", columnList = "revoked_at"),
                @Index(name = "idx_refresh_tokens_user_revoked", columnList = "user_id, revoked_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_tokens_token_hash", columnNames = "token_hash")
        }
)
@Getter
@Setter
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;
}
