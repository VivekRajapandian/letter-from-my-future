package com.letterfuture.execution.engine.workflow.engine;

import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.TaskEventType;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.TaskEvent;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskEventRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final TaskRepository taskRepo;
    private final PhaseRepository phaseRepo;
    private final GoalRepository goalRepo;
    private final TaskEventRepository eventRepo;
    private static final Logger log =
            LoggerFactory.getLogger(WorkflowEngine.class);


    @Transactional
    public void completeTask(UUID taskId, UUID userId){

        Task task = taskRepo.findByIdAndUser(taskId, userId)
                .orElseThrow();

        if(task.getStatus() == TaskStatus.COMPLETED){
            return; // idempotent
        }

        validateTransition(task, TaskStatus.COMPLETED);

        task.setStatus(TaskStatus.COMPLETED);
        taskRepo.save(task);

        logEvent(taskId, TaskEventType.COMPLETED);

        unlockNextTask(task);

        checkPhaseCompletion(task.getPhaseId());

        log.info("Task {} completed", taskId);
    }

    @Transactional
    public void pauseGoal(UUID goalId, UUID userId){

        Goal goal = goalRepo.findByIdAndUserId(goalId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Goal not found"));


        goal.setStatus(GoalStatus.PAUSED);

        goalRepo.save(goal);
    }

    @Transactional
    public void skipTask(UUID taskId, UUID userId){

        Task task = taskRepo.findByIdAndUser(taskId, userId)
                .orElseThrow();

        validateTransition(task, TaskStatus.SKIPPED);

        task.setStatus(TaskStatus.SKIPPED);
        taskRepo.save(task);

        logEvent(taskId, TaskEventType.SKIPPED);

        unlockNextTask(task);

        checkPhaseCompletion(task.getPhaseId());
    }

    @Transactional
    public void reopenTask(UUID taskId, UUID userId){

        Task task = taskRepo.findByIdAndUser(taskId, userId)
                .orElseThrow();

        if(task.getStatus() == TaskStatus.LOCKED){
            throw new IllegalStateException("Cannot reopen a locked task");
        }

        task.setStatus(TaskStatus.AVAILABLE);
        taskRepo.save(task);

        logEvent(taskId, TaskEventType.REOPENED);
    }

    private void validateTransition(Task task, TaskStatus target){

        TaskStatus current = task.getStatus();

        if(current == TaskStatus.LOCKED){
            throw new IllegalStateException("Locked tasks cannot transition");
        }

        if(current == target){
            return;
        }

        if(current == TaskStatus.COMPLETED && target == TaskStatus.SKIPPED){
            throw new IllegalStateException("Completed task cannot be skipped");
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

        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED ||
                t.getStatus() == TaskStatus.SKIPPED).count();

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
    public NextTaskResponse getNextTask(UUID goalId, UUID userId){

        List<Object[]> result =
                taskRepo.findNextTaskWithContext(goalId, userId);

        if(result.isEmpty()){
            throw new RuntimeException("No available tasks");
        }

        Object[] row = result.get(0);

        Task task = (Task) row[0];
        Phase phase = (Phase) row[1];
        Goal goal = (Goal) row[2];

        int totalTasks = taskRepo.countTotalTasks(goalId);
        int completedTasks = taskRepo.countCompletedTasks(goalId);

        int taskCountInPhase =
                taskRepo.countTasksInPhase(phase.getId());

        int phaseCount =
                phaseRepo.countByGoalId(goalId);

        return new NextTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                goal.getTitle(),
                phase.getTitle(),
                phase.getOrderIndex(),
                phaseCount,
                task.getOrderIndex(),
                taskCountInPhase,
                completedTasks
        );
    }

    @Transactional(readOnly = true)
    public long getTotalTasks(UUID goalId, UUID userId){
        return taskRepo.countTotalTasks(goalId, userId);
    }

    @Transactional(readOnly = true)
    public long getCompletedTasks(UUID goalId, UUID userId){
        return taskRepo.countCompletedTasks(goalId, userId);
    }

    @Transactional
    public void resumeTask(UUID taskId, UUID userId){

        Task task = taskRepo.findByIdAndUser(taskId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));

        if(task.getStatus() == TaskStatus.LOCKED){
            throw new IllegalStateException("Locked task cannot be resumed");
        }

        if(task.getStatus() == TaskStatus.AVAILABLE){
            return; // idempotent
        }

        task.setStatus(TaskStatus.AVAILABLE);
        taskRepo.save(task);

        logEvent(taskId, TaskEventType.REOPENED);
    }

}

