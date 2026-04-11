package com.letterfuture.execution.engine.workflow.domain;

import com.letterfuture.execution.engine.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "target_duration_days")
    private Integer targetDurationDays;

    private LocalDate targetDate;

    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "planning_mode")
    private String planningMode;

    @Column(name = "planning_state")
    private String planningState;

    @Column(name = "total_phases_planned")
    private Integer totalPhasesPlanned;

    @Column(name = "current_phase_number")
    private Integer currentPhaseNumber;

    @Column(name = "goal_input_text")
    private String goalInputText;

    @Column(name = "future_you_tone")
    private String futureYouTone;

}

