"use client";

import { ExecutionSnapshot } from "@/types/execution";

type ActivePhaseCardProps = {
  activePhase: ExecutionSnapshot["activePhase"];
};

export default function ActivePhaseCard({ activePhase }: ActivePhaseCardProps) {
  if (!activePhase) {
    return (
      <section className="rounded-3xl border border-white/10 bg-white/5 p-6">
        <div className="text-sm font-semibold uppercase tracking-[0.16em] text-white/55">
          Active phase
        </div>

        <div className="mt-4">
          <h2 className="text-lg font-semibold text-white">No active phase yet</h2>
          <p className="mt-2 text-sm leading-6 text-white/65">
            This goal does not currently have an active phase available.
          </p>
        </div>
      </section>
    );
  }

  return (
    <section className="rounded-3xl border border-white/10 bg-white/5 p-6">
      <div className="flex items-center justify-between gap-4">
        <div className="text-sm font-semibold uppercase tracking-[0.16em] text-white/55">
          Active phase
        </div>

        <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium text-white/75">
          {activePhase.status}
        </span>
      </div>

      <div className="mt-4 space-y-4">
        <div>
          <h2 className="text-xl font-semibold tracking-tight text-white">
            {activePhase.title}
          </h2>

          {activePhase.outlineTitle && activePhase.outlineTitle !== activePhase.title ? (
            <p className="mt-2 text-sm text-white/60">{activePhase.outlineTitle}</p>
          ) : null}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <DetailCard label="Phase #" value={String(activePhase.orderIndex)} />
          <DetailCard
            label="Duration"
            value={
              activePhase.durationDays ? `${activePhase.durationDays} days` : "Not set"
            }
          />
        </div>

        <p className="text-sm leading-6 text-white/65">
          This is the only phase that matters right now. Keep the user’s attention here, and let
          the future evolve when it’s earned.
        </p>
      </div>
    </section>
  );
}

function DetailCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-4">
      <div className="text-[11px] uppercase tracking-[0.14em] text-white/45">{label}</div>
      <div className="mt-2 text-sm font-semibold text-white">{value}</div>
    </div>
  );
}