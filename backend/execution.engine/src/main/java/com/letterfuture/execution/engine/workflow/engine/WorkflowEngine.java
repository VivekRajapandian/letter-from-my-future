package com.letterfuture.execution.engine.workflow.engine;

import com.letterfuture.execution.engine.enums.GoalStatus;
import com.letterfuture.execution.engine.enums.TaskEventType;
import com.letterfuture.execution.engine.workflow.domain.Goal;
import com.letterfuture.execution.engine.workflow.domain.Phase;
import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.enums.TaskStatus;
import com.letterfuture.execution.engine.workflow.domain.TaskEvent;
import com.letterfuture.execution.engine.workflow.domain.TaskQuestion;
import com.letterfuture.execution.engine.workflow.domain.TaskResponse;
import com.letterfuture.execution.engine.workflow.dto.NextTaskResponse;
import com.letterfuture.execution.engine.workflow.dto.TaskQuestionDTO;
import com.letterfuture.execution.engine.workflow.dto.TaskResponseDTO;
import com.letterfuture.execution.engine.workflow.engine.NextPhaseGenerationService;
import com.letterfuture.execution.engine.workflow.repository.GoalRepository;
import com.letterfuture.execution.engine.workflow.repository.PhaseRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskEventRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskQuestionRepository;
import com.letterfuture.execution.engine.workflow.repository.TaskResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final TaskRepository taskRepo;
    private final PhaseRepository phaseRepo;
    private final GoalRepository goalRepo;
    private final TaskEventRepository eventRepo;
    private final TaskQuestionRepository taskQuestionRepo;
    private final TaskResponseRepository taskResponseRepo;
    private final NextPhaseGenerationService nextPhaseGenerationService;
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

        checkPhaseCompletion(task);

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

        checkPhaseCompletion(task);
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

    private void checkPhaseCompletion(Task completedTask) {
        Task lastTask = taskRepo.findFirstByPhaseIdOrderByOrderIndexDesc(completedTask.getPhaseId())
                .orElseThrow(() -> new RuntimeException("Phase has no tasks"));

        if (!lastTask.getId().equals(completedTask.getId())) {
            return;
        }

        List<Task> tasks = taskRepo.findAllByPhaseId(completedTask.getPhaseId());
        long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED ||
                t.getStatus() == TaskStatus.SKIPPED).count();

        onPhaseReachedCompletion(completedTask.getPhaseId(), completedCount, tasks.size());
    }

    private void onPhaseReachedCompletion(UUID phaseId, long completedCount, int totalCount) {
        Phase phase = phaseRepo.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Phase not found"));

        log.info("Phase {} ('{}') reached completion threshold ({}/{}). Triggering next phase unlock.",
                phaseId, phase.getTitle(), completedCount, totalCount);

        logPhaseEventForAllTasks(phaseId, TaskEventType.PHASE_COMPLETED);
        proceedToNextPhase(phaseId);
    }

    private void proceedToNextPhase(UUID phaseId) {
        Phase currentPhase = phaseRepo.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Phase not found"));

        UUID goalId = currentPhase.getGoalId();
        var nextPhaseOpt = phaseRepo.findByGoalIdAndOrderIndex(goalId, currentPhase.getOrderIndex() + 1);

        if (nextPhaseOpt.isPresent()) {
            Phase nextPhase = nextPhaseOpt.get();
            log.info("Unlocking existing next phase {} ('{}') for goal {}.",
                    nextPhase.getId(), nextPhase.getTitle(), goalId);
            unlockPhaseFirstTask(nextPhase.getId(), TaskEventType.NEXT_PHASE_TRIGGERED);
        } else {
            log.info("No pre-planned next phase found for goal {}. Scheduling LLM-based next phase generation.",
                    goalId);
            try {
                String nextPhaseJson = nextPhaseGenerationService.generateNextPhaseForCompletedPhase(currentPhase);
                UUID nextPhaseId = nextPhaseGenerationService.processNextPhaseResponse(goalId, nextPhaseJson);
                if (nextPhaseId != null) {
                    logPhaseEventForGoal(goalId, TaskEventType.NEXT_PHASE_GENERATED);
                } else {
                    log.info("Goal {} determined to be complete by LLM - no next phase generated.", goalId);
                }
            } catch (Exception ex) {
                log.error("Next phase generation failed for goal {}: {}",
                        goalId, ex.getMessage(), ex);
                // Do not mark goal as completed - let it remain active for retry
                // The UI will show appropriate waiting state when no tasks are available
            }
        }
    }

    private void markGoalAsCompleted(UUID goalId) {
        Goal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if (goal.getStatus() != GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.COMPLETED);
            goalRepo.save(goal);
            log.info("Goal {} marked as COMPLETED.", goalId);
        }
    }

    private void unlockPhaseFirstTask(UUID phaseId, TaskEventType triggerEvent) {
        taskRepo.findFirstByPhaseIdAndStatusOrderByOrderIndex(phaseId, TaskStatus.LOCKED)
                .ifPresent(first -> {
                    first.setStatus(TaskStatus.AVAILABLE);
                    taskRepo.save(first);
                    logEvent(first.getId(), triggerEvent);
                    log.debug("Unlocked first task {} in phase {} via event {}.",
                            first.getId(), phaseId, triggerEvent);
                });
    }

    private void logPhaseEventForAllTasks(UUID phaseId, TaskEventType eventType) {
        List<Task> tasks = taskRepo.findAllByPhaseId(phaseId);
        for (Task task : tasks) {
            logEvent(task.getId(), eventType);
        }
    }

    private void logPhaseEventForGoal(UUID goalId, TaskEventType eventType) {
        List<Phase> phases = phaseRepo.findByGoalIdOrderByOrderIndex(goalId);
        for (Phase phase : phases) {
            List<Task> tasks = taskRepo.findAllByPhaseId(phase.getId());
            for (Task task : tasks) {
                logEvent(task.getId(), eventType);
            }
        }
    }



    private void logEvent(UUID taskId, TaskEventType type) {

        TaskEvent event = new TaskEvent();
        event.setId(UUID.randomUUID());
        event.setTaskId(taskId);
        event.setEventType(type);
        event.setCreatedAt(LocalDateTime.now());

        eventRepo.save(event);
    }

    @Transactional(readOnly = true, noRollbackFor = RuntimeException.class)
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

        // Load questions and responses for this task
        List<TaskQuestion> questions = taskQuestionRepo.findByTaskIdOrderByQuestionIndex(task.getId());
        List<TaskQuestionDTO> questionDTOs = questions.stream()
                .map(q -> new TaskQuestionDTO(q.getId(), q.getQuestionIndex(), q.getQuestion(), q.getQuestionType(), q.getHint()))
                .collect(Collectors.toList());

        List<TaskResponse> responses = taskResponseRepo.findByTaskId(task.getId());
        List<TaskResponseDTO> responseDTOs = responses.stream()
                .map(r -> new TaskResponseDTO(r.getQuestionId(), r.getResponse()))
                .collect(Collectors.toList());

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
                completedTasks,
                questionDTOs,
                responseDTOs
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

