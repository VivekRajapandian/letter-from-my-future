"use client";

type PlanningStateBannerProps = {
  state: string;
  reason?: string | null;
  onRefresh?: () => Promise<void>;
};

export default function PlanningStateBanner({
  state,
  reason,
  onRefresh,
}: PlanningStateBannerProps) {
  const config = getBannerConfig(state);

  return (
    <section
      className={`rounded-3xl border p-5 sm:p-6 ${config.containerClass}`}
      aria-live="polite"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-2">
          <div className={`inline-flex rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] ${config.badgeClass}`}>
            {stateLabel(state)}
          </div>

          <h2 className="text-lg font-semibold text-white">{config.title}</h2>
          <p className="max-w-3xl text-sm leading-6 text-white/75">
            {reason || config.description}
          </p>
        </div>

        {onRefresh ? (
          <button
            onClick={() => void onRefresh()}
            className="rounded-xl border border-white/15 bg-white/5 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/10"
          >
            Refresh state
          </button>
        ) : null}
      </div>
    </section>
  );
}

function stateLabel(state: string): string {
  switch ((state || "").toUpperCase()) {
    case "READY":
      return "Ready";
    case "WAITING_FOR_USER_INPUT":
      return "Waiting";
    case "GENERATING_NEXT_PHASE":
      return "Generating";
    case "COMPLETED":
      return "Completed";
    default:
      return state || "Unknown";
  }
}

function getBannerConfig(state: string) {
  switch ((state || "").toUpperCase()) {
    case "READY":
      return {
        title: "You have a clear execution window.",
        description:
          "Stay focused on the current phase. Completion matters more than perfection.",
        containerClass: "border-emerald-400/20 bg-emerald-500/10",
        badgeClass: "bg-emerald-400/15 text-emerald-200 border border-emerald-300/20",
      };

    case "WAITING_FOR_USER_INPUT":
      return {
        title: "The system is waiting for more execution from you.",
        description:
          "Your next best phase should be based on real signals, not assumptions.",
        containerClass: "border-amber-400/20 bg-amber-500/10",
        badgeClass: "bg-amber-400/15 text-amber-100 border border-amber-300/20",
      };

    case "GENERATING_NEXT_PHASE":
      return {
        title: "Your next phase is being prepared.",
        description:
          "Future You is adjusting the path based on what you’ve actually done so far.",
        containerClass: "border-sky-400/20 bg-sky-500/10",
        badgeClass: "bg-sky-400/15 text-sky-100 border border-sky-300/20",
      };

    case "COMPLETED":
      return {
        title: "This goal is complete.",
        description:
          "You’ve finished the current arc. You can review what worked and decide what’s next.",
        containerClass: "border-violet-400/20 bg-violet-500/10",
        badgeClass: "bg-violet-400/15 text-violet-100 border border-violet-300/20",
      };

    default:
      return {
        title: "Execution state available.",
        description: "The workspace loaded successfully.",
        containerClass: "border-white/10 bg-white/5",
        badgeClass: "bg-white/10 text-white/75 border border-white/10",
      };
  }
}