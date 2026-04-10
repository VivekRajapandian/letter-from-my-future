package com.letterfuture.execution.engine.workflow.execution;

import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.dto.execution.ExecutionSnapshotResponse;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GoalExecutionQueryService {

    private final GoalRepository goalRepository;
    private final PhaseRepository phaseRepository;
    private final TaskRepository taskRepository;
    private final ExecutionSnapshotAssembler executionSnapshotAssembler;

    public ExecutionSnapshotResponse getExecutionSnapshot(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Goal not found: " + goalId
                ));

        List<Phase> phases = phaseRepository.findByGoalIdOrderByOrderIndexAsc(goalId);

        Phase activePhase = resolveActivePhase(phases);

        List<Task> tasks = activePhase == null
                ? List.of()
                : taskRepository.findByPhaseIdOrderByOrderIndexAsc(activePhase.getId());

        return executionSnapshotAssembler.toResponse(goal, phases, activePhase, tasks);
    }

    private Phase resolveActivePhase(List<Phase> phases) {
        if (phases == null || phases.isEmpty()) {
            return null;
        }

        return phases.stream()
                .filter(this::isActivePhase)
                .min(Comparator.comparing(Phase::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .orElseGet(() -> phases.stream()
                        .filter(this::isIncompletePhase)
                        .min(Comparator.comparing(Phase::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                        .orElse(null));
    }

    private boolean isActivePhase(Phase phase) {
        return equalsIgnoreCase(phase.getStatus().toString(), "ACTIVE");
    }

    private boolean isIncompletePhase(Phase phase) {
        return !equalsIgnoreCase(phase.getStatus().toString(), "COMPLETED");
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}