"use client";

import ActivePhaseCard from "@/components/execution/ActivePhaseCard";
import PlanningStateBanner from "@/components/execution/PlanningStateBanner";
import TaskExecutionCard from "@/components/execution/TaskExecutionCard";
import { ExecutionSnapshot } from "@/types/execution";

type GoalExecutionScreenProps = {
  snapshot: ExecutionSnapshot;
  userId: string;
  onRefresh: () => Promise<void>;
};

export default function GoalExecutionScreen({
  snapshot,
  userId,
  onRefresh,
}: GoalExecutionScreenProps) {
  const { goal, planning, activePhase, tasks, progress } = snapshot;

  return (
    <main className="min-h-screen bg-neutral-950 text-white px-6 py-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <header className="rounded-3xl border border-white/10 bg-[radial-gradient(circle_at_top_left,rgba(255,255,255,0.10),rgba(255,255,255,0.02)_35%,rgba(255,255,255,0.01)_70%)] p-8 shadow-2xl shadow-black/30">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="space-y-3">
              <div className="inline-flex items-center rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs uppercase tracking-[0.18em] text-white/60">
                Future You
              </div>

              <div>
                <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
                  {goal.title}
                </h1>
                {goal.summary ? (
                  <p className="mt-3 max-w-3xl text-sm leading-6 text-white/70 sm:text-base">
                    {goal.summary}
                  </p>
                ) : null}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <MetricCard label="Goal progress" value={`${progress.goalProgressPercent}%`} />
              <MetricCard label="Phase progress" value={`${progress.phaseProgressPercent}%`} />
              <MetricCard label="Tasks done" value={`${progress.completedTasks}/${progress.totalVisibleTasks}`} />
              <MetricCard
                label="Phases"
                value={`${goal.phaseCountCreated ?? 0}/${goal.phaseCountPlanned ?? goal.phaseCountCreated ?? 0}`}
              />
            </div>
          </div>
        </header>

        <PlanningStateBanner
          state={planning.state}
          reason={planning.reason}
          onRefresh={onRefresh}
        />

        <div className="grid gap-6 lg:grid-cols-[340px,minmax(0,1fr)]">
          <div className="space-y-6">
            <ActivePhaseCard activePhase={activePhase} />

            <aside className="rounded-3xl border border-white/10 bg-white/5 p-6">
              <h2 className="text-sm font-semibold uppercase tracking-[0.16em] text-white/55">
                Execution context
              </h2>

              <div className="mt-4 space-y-3 text-sm text-white/75">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-white/50">Status</span>
                  <span className="font-medium text-white">{goal.status}</span>
                </div>

                <div className="flex items-center justify-between gap-3">
                  <span className="text-white/50">Planning mode</span>
                  <span className="font-medium text-white">{goal.planningMode}</span>
                </div>

                {goal.targetDurationDays ? (
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-white/50">Target duration</span>
                    <span className="font-medium text-white">
                      {goal.targetDurationDays} days
                    </span>
                  </div>
                ) : null}

                <div className="pt-3 text-xs leading-5 text-white/50">
                  Future You is helping you focus on the next best step, not the entire future.
                </div>
              </div>
            </aside>
          </div>

          <section className="space-y-4">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-semibold tracking-tight">Current tasks</h2>
                <p className="mt-1 text-sm text-white/60">
                  Execute what matters now. The next phase can adapt later.
                </p>
              </div>

              <button
                onClick={() => void onRefresh()}
                className="rounded-xl border border-white/15 bg-white/5 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
              >
                Refresh
              </button>
            </div>

            {tasks.length === 0 ? (
              <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
                <h3 className="text-lg font-semibold">No active tasks yet</h3>
                <p className="mt-2 text-sm leading-6 text-white/65">
                  This goal is between execution states right now. Check the planning banner above
                  for more context.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {tasks.map((task) => (
                  <TaskExecutionCard
                    key={task.taskId}
                    task={task}
                    userId={userId}
                    onRefresh={onRefresh}
                  />
                ))}
              </div>
            )}
          </section>
        </div>
      </div>
    </main>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-white/45">{label}</div>
      <div className="mt-2 text-lg font-semibold text-white">{value}</div>
    </div>
  );
}