package com.letterfuture.execution.engine.workflow.domain;

import com.letterfuture.execution.engine.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goals",
        indexes = @Index(name="idx_goal_user", columnList="user_id"))
@Getter
@Setter
public class Goal {

    @Id
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    private String title;

    private LocalDate targetDate;

    private LocalDateTime createdAt;
}

