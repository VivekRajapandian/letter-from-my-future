package com.letterfuture.execution.engine.workflow.signals;

import com.letterfuture.execution.engine.workflow.domain.Task;
import com.letterfuture.execution.engine.workflow.domain.TaskInputDefinition;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmission;
import com.letterfuture.execution.engine.workflow.domain.TaskSubmissionValue;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TaskSignalSummaryService {

    public TaskSignalSummary summarize(
            Task task,
            TaskSubmission latestSubmission,
            List<TaskInputDefinition> inputDefinitions,
            List<TaskSubmissionValue> submissionValues
    ) {
        Map<UUID, TaskInputDefinition> definitionById = inputDefinitions == null
                ? Map.of()
                : inputDefinitions.stream().collect(Collectors.toMap(
                TaskInputDefinition::getId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
        ));

        Map<String, String> capturedInputs = new LinkedHashMap<>();

        if (submissionValues != null) {
            for (TaskSubmissionValue value : submissionValues) {
                TaskInputDefinition definition = definitionById.get(value.getInputDefinitionId());
                String key = definition != null && definition.getKey() != null && !definition.getKey().isBlank()
                        ? definition.getKey()
                        : String.valueOf(value.getInputDefinitionId());

                capturedInputs.put(key, formatValue(value));
            }
        }

        return new TaskSignalSummary(
                task.getId(),
                task.getTitle(),
                task.getStatus() == null ? "UNKNOWN" : task.getStatus().name(),
                latestSubmission == null ? null : latestSubmission.getAction(),
                latestSubmission == null ? null : latestSubmission.getNote(),
                capturedInputs
        );
    }

    private String formatValue(TaskSubmissionValue value) {
        if (value.getValueText() != null && !value.getValueText().isBlank()) {
            return value.getValueText();
        }

        if (value.getValueNumber() != null) {
            BigDecimal normalized = value.getValueNumber().stripTrailingZeros();
            return normalized.toPlainString();
        }

        if (value.getValueBoolean() != null) {
            return String.valueOf(value.getValueBoolean());
        }

        if (value.getValueDate() != null) {
            return value.getValueDate().toString();
        }

        if (value.getValueJson() != null && !value.getValueJson().isBlank()) {
            return value.getValueJson();
        }

        return "(empty)";
    }
}