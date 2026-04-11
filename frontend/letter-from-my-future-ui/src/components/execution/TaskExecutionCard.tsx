"use client";

import { useMemo, useState } from "react";
import TaskInputForm from "@/components/execution/TaskInputForm";
import { submitTask } from "@/services/executionApi";
import { ExecutionTask } from "@/types/execution";

type TaskExecutionCardProps = {
  task: ExecutionTask;
  userId: string;
  onRefresh: () => Promise<void>;
};

export default function TaskExecutionCard({
  task,
  userId,
  onRefresh,
}: TaskExecutionCardProps) {
  const instruction = task.instruction || {};

  const initialValues = useMemo(() => {
    const byInputId: Record<string, unknown> = {};

    if (task.latestSubmission?.values && task.inputSchema?.length) {
      for (const input of task.inputSchema) {
        if (task.latestSubmission.values[input.key] !== undefined) {
          byInputId[input.inputDefinitionId] = task.latestSubmission.values[input.key];
        }
      }
    }

    return byInputId;
  }, [task]);

  const [values, setValues] = useState<Record<string, unknown>>(initialValues);
  const [note, setNote] = useState(task.latestSubmission?.note ?? "");
  const [submittingAction, setSubmittingAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (
    action: "SAVE_PROGRESS" | "COMPLETE" | "SKIP" | "REOPEN"
  ) => {
    try {
      setSubmittingAction(action);
      setError(null);

      const payloadValues = task.inputSchema
        .map((input) => ({
          inputDefinitionId: input.inputDefinitionId,
          value: values[input.inputDefinitionId],
        }))
        .filter((entry) => entry.value !== undefined && entry.value !== "");

      await submitTask(task.taskId, {
        userId,
        action,
        note: note.trim() ? note.trim() : undefined,
        values: payloadValues,
      });

      await onRefresh();
    } catch (err) {
      console.error(err);
      setError(err instanceof Error ? err.message : "Failed to submit task.");
    } finally {
      setSubmittingAction(null);
    }
  };

  return (
    <article className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-xl shadow-black/20">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-white/70">
              Task {task.orderIndex}
            </span>

            {task.scheduledDay ? (
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-white/70">
                Day {task.scheduledDay}
              </span>
            ) : null}

            <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-white/85">
              {task.status}
            </span>
          </div>

          <div>
            <h3 className="text-xl font-semibold tracking-tight text-white">
              {task.title}
            </h3>
          </div>
        </div>

        <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-right">
          <div className="text-[11px] uppercase tracking-[0.14em] text-white/45">
            Input slots
          </div>
          <div className="mt-1 text-sm font-semibold text-white">
            {task.inputSchema?.length ?? 0}
          </div>
        </div>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-2">
        <InfoBlock
          label="What"
          value={instruction.what || "No detailed instruction added yet."}
        />
        <InfoBlock
          label="How"
          value={instruction.how || "No execution method specified yet."}
        />
        <InfoBlock
          label="Why"
          value={instruction.why || "No rationale added yet."}
        />
        <InfoBlock
          label="Success criteria"
          value={instruction.successCriteria || "No success criteria defined yet."}
        />
      </div>

      {task.inputSchema?.length ? (
        <div className="mt-6">
          <TaskInputForm
            inputs={task.inputSchema}
            values={values}
            onChange={setValues}
          />
        </div>
      ) : null}

      <div className="mt-6">
        <label className="block">
          <div className="text-sm font-medium text-white">Notes</div>
          <textarea
            className="mt-3 min-h-[100px] w-full resize-y rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white outline-none transition placeholder:text-white/30 focus:border-white/25 focus:bg-white/10"
            placeholder="Add context, friction, observations, or anything Future You should know."
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </label>
      </div>

      {task.latestSubmission ? (
        <div className="mt-6 rounded-2xl border border-emerald-400/15 bg-emerald-500/10 p-4">
          <div className="text-[11px] uppercase tracking-[0.14em] text-emerald-100/80">
            Latest submission
          </div>

          <div className="mt-2 text-sm font-medium text-white">
            {task.latestSubmission.action}
          </div>

          <div className="mt-1 text-xs text-white/60">
            {formatDateTime(task.latestSubmission.submittedAt)}
          </div>

          {task.latestSubmission.note ? (
            <p className="mt-3 text-sm leading-6 text-white/75">
              {task.latestSubmission.note}
            </p>
          ) : null}
        </div>
      ) : null}

      {error ? (
        <div className="mt-6 rounded-2xl border border-red-400/20 bg-red-500/10 p-4 text-sm text-red-100">
          {error}
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap gap-3">
        <ActionButton
          label="Save progress"
          onClick={() => void handleSubmit("SAVE_PROGRESS")}
          loading={submittingAction === "SAVE_PROGRESS"}
        />

        <ActionButton
          label="Complete"
          onClick={() => void handleSubmit("COMPLETE")}
          loading={submittingAction === "COMPLETE"}
          primary
        />

        <ActionButton
          label="Skip"
          onClick={() => void handleSubmit("SKIP")}
          loading={submittingAction === "SKIP"}
        />

        <ActionButton
          label="Reopen"
          onClick={() => void handleSubmit("REOPEN")}
          loading={submittingAction === "REOPEN"}
        />
      </div>
    </article>
  );
}

function InfoBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-white/45">{label}</div>
      <p className="mt-2 text-sm leading-6 text-white/75">{value}</p>
    </div>
  );
}

function ActionButton({
  label,
  onClick,
  loading,
  primary = false,
}: {
  label: string;
  onClick: () => void;
  loading: boolean;
  primary?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={
        primary
          ? "rounded-xl bg-white px-4 py-2 text-sm font-medium text-black transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
          : "rounded-xl border border-white/15 bg-white/5 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50"
      }
    >
      {loading ? "Saving..." : label}
    </button>
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-CA", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}