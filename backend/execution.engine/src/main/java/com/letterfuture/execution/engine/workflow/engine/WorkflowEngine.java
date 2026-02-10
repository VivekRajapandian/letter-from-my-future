package com.letterfuture.execution.engine.workflow.engine;

import com.letterfuture.execution.engine.enums.TaskEventType;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.TaskEvent;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskEventRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final TaskRepository taskRepo;
    private final PhaseRepository phaseRepo;
    private final GoalRepository goalRepo;
    private final TaskEventRepository eventRepo;

    @Transactional
    public void completeTask(UUID taskId) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow();

        validateTransition(task, TaskStatus.COMPLETED);

        task.setStatus(TaskStatus.COMPLETED);
        taskRepo.save(task);

        logEvent(taskId, TaskEventType.COMPLETED);

        unlockNextTask(task);

        checkPhaseCompletion(task.getPhaseId());
    }

    private void validateTransition(Task task, TaskStatus target) {

        if (task.getStatus() == TaskStatus.LOCKED) {
            throw new IllegalStateException("Locked task cannot transition");
        }

        if (task.getStatus() == TaskStatus.COMPLETED
                && target == TaskStatus.COMPLETED) {
            return;
        }
    }

    private void unlockNextTask(Task completed) {

        taskRepo.findFirstByPhaseIdAndStatusOrderByOrderIndex(
                completed.getPhaseId(),
                TaskStatus.LOCKED
        ).ifPresent(next -> {
            next.setStatus(TaskStatus.AVAILABLE);
            taskRepo.save(next);

            logEvent(next.getId(), TaskEventType.AUTO_UNLOCKED);
        });
    }

    private void checkPhaseCompletion(UUID phaseId) {

        List<Task> tasks = taskRepo.findAllByPhaseId(phaseId);

        long completed = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        double ratio = (double) completed / tasks.size();

        if (ratio >= 0.7) {
            unlockNextPhase(phaseId);
        }
    }

    private void unlockNextPhase(UUID phaseId) {

        Phase phase = phaseRepo.findById(phaseId).orElseThrow();

        phaseRepo.findAll().stream()
                .filter(p -> p.getGoalId().equals(phase.getGoalId()))
                .filter(p -> p.getOrderIndex() == phase.getOrderIndex() + 1)
                .findFirst()
                .ifPresent(nextPhase -> {

                    taskRepo.findFirstByPhaseIdAndStatusOrderByOrderIndex(
                            nextPhase.getId(),
                            TaskStatus.LOCKED
                    ).ifPresent(first -> {
                        first.setStatus(TaskStatus.AVAILABLE);
                        taskRepo.save(first);
                    });
                });
    }

    private void logEvent(UUID taskId, TaskEventType type) {

        TaskEvent event = new TaskEvent();
        event.setId(UUID.randomUUID());
        event.setTaskId(taskId);
        event.setEventType(type);
        event.setCreatedAt(LocalDateTime.now());

        eventRepo.save(event);
    }


    @Transactional(readOnly = true)
    public Task getNextTask(UUID goalId) {

        List<Phase> phases = phaseRepo.findAll().stream()
                .filter(p -> p.getGoalId().equals(goalId))
                .sorted(Comparator.comparingInt(Phase::getOrderIndex))
                .toList();

        for (Phase phase : phases) {

            Optional<Task> task =
                    taskRepo.findFirstByPhaseIdAndStatusOrderByOrderIndex(
                            phase.getId(),
                            TaskStatus.AVAILABLE
                    );

            if (task.isPresent())
                return task.get();
        }

        throw new RuntimeException("No available tasks");
    }


}

