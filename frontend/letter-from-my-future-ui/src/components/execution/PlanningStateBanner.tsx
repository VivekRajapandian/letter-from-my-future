"use client";

import { ExecutionPlanning } from "@/types/execution";

type PlanningStateBannerProps = {
  planning: ExecutionPlanning;
  onRefresh?: () => Promise<void>;
  revealMode?: boolean;
};

export default function PlanningStateBanner({
  planning,
  onRefresh,
  revealMode = false,
}: PlanningStateBannerProps) {
  const state = (planning.state || "").toUpperCase();
  const config = getBannerConfig(state, revealMode);

  return (
    <section
      className={`rounded-3xl border p-5 md:p-6 backdrop-blur-sm transition-all ${config.containerClass}`}
    >
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="space-y-3">
          <div
            className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold tracking-wide ${config.badgeClass}`}
          >
            {stateLabel(state)}
          </div>

          <div className="space-y-2">
            <h2 className="text-xl font-semibold text-white">{config.title}</h2>

            <p className="max-w-3xl text-sm leading-6 text-white/75">
              {planning.futureYouMessage || config.description}
            </p>

            {planning.transitionReason ? (
              <p className="max-w-3xl text-sm leading-6 text-white/65">
                {planning.transitionReason}
              </p>
            ) : null}

            {planning.generatedFromSignals ? (
              <p className="text-xs uppercase tracking-[0.18em] text-white/45">
                {planning.generatedFromSignals}
              </p>
            ) : null}

            {planning.generatedAt ? (
              <p className="text-xs text-white/40">
                Generated {formatDateTime(planning.generatedAt)}
              </p>
            ) : null}
          </div>
        </div>

        {onRefresh ? (
          <button
            type="button"
            onClick={() => void onRefresh()}
            className="rounded-2xl border border-white/15 bg-white/5 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
          >
            Refresh state
          </button>
        ) : null}
      </div>
    </section>
  );
}

function stateLabel(state: string): string {
  switch (state) {
    case "READY":
      return "Ready";
    case "WAITING_FOR_USER_INPUT":
      return "Waiting";
    case "GENERATING_NEXT_PHASE":
      return "Generating";
    case "NEXT_PHASE_READY":
      return "Next phase ready";
    case "COMPLETED":
      return "Completed";
    default:
      return state || "Unknown";
  }
}

function getBannerConfig(state: string, revealMode: boolean) {
  switch (state) {
    case "READY":
      return {
        title: revealMode
          ? "Your next phase is ready."
          : "You have a clear execution window.",
        description: revealMode
          ? "Future You adapted the plan from what you actually completed."
          : "Stay focused on the current phase. Completion matters more than perfection.",
        containerClass: "border-emerald-400/20 bg-emerald-500/10 shadow-[0_0_80px_rgba(16,185,129,0.08)]",
        badgeClass:
          "border border-emerald-300/20 bg-emerald-400/15 text-emerald-200",
      };
    case "WAITING_FOR_USER_INPUT":
      return {
        title: "The system is waiting for more execution from you.",
        description:
          "Your next best phase should be based on real signals, not assumptions.",
        containerClass: "border-amber-400/20 bg-amber-500/10",
        badgeClass:
          "border border-amber-300/20 bg-amber-400/15 text-amber-100",
      };
    case "GENERATING_NEXT_PHASE":
      return {
        title: "Future You is reviewing your completed phase.",
        description:
          "Building the next best phase from your real progress.",
        containerClass: "border-sky-400/20 bg-sky-500/10 shadow-[0_0_90px_rgba(56,189,248,0.08)]",
        badgeClass:
          "border border-sky-300/20 bg-sky-400/15 text-sky-100",
      };
    case "NEXT_PHASE_READY":
      return {
        title: "Your next phase was adapted from your execution signals.",
        description:
          "The system noticed your latest progress and prepared the next step.",
        containerClass: "border-cyan-400/20 bg-cyan-500/10 shadow-[0_0_90px_rgba(34,211,238,0.08)]",
        badgeClass:
          "border border-cyan-300/20 bg-cyan-400/15 text-cyan-100",
      };
    case "COMPLETED":
      return {
        title: "Your Future You system is complete.",
        description:
          "The structure now belongs to you. Review what worked and decide what to build next.",
        containerClass: "border-violet-400/20 bg-violet-500/10",
        badgeClass:
          "border border-violet-300/20 bg-violet-400/15 text-violet-100",
      };
    default:
      return {
        title: "Execution state available.",
        description: "The workspace loaded successfully.",
        containerClass: "border-white/10 bg-white/5",
        badgeClass: "border border-white/10 bg-white/10 text-white/75",
      };
  }
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}