"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import GoalExecutionScreen from "@/components/execution/GoalExecutionScreen";
import PlanningStateBanner from "@/components/execution/PlanningStateBanner";
import { getExecutionSnapshot } from "@/services/executionApi";
import { ExecutionSnapshot } from "@/types/execution";
import { getCurrentUser } from "@/services/auth";

const GENERATION_POLL_INTERVAL_MS = 2500;
const GENERATION_POLL_LIMIT = 6;

export default function GoalPage() {
  const params = useParams();
  const router = useRouter();
  const goalId = params?.goalId as string;

  const [snapshot, setSnapshot] = useState<ExecutionSnapshot | null>(null);
  const [userId, setUserId] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revealMode, setRevealMode] = useState(false);

  const pollAttemptsRef = useRef(0);
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const previousPlanningStateRef = useRef<string | null>(null);

  const loadExecution = useCallback(
    async (resolvedUserId: string) => {
      const data = await getExecutionSnapshot(goalId, resolvedUserId);
      setSnapshot((prev) => {
        const previousState = prev?.planning?.state?.toUpperCase() ?? null;
        const nextState = data?.planning?.state?.toUpperCase() ?? null;

        if (
          previousState === "GENERATING_NEXT_PHASE" &&
          (nextState === "READY" || nextState === "NEXT_PHASE_READY")
        ) {
          setRevealMode(true);
        }

        previousPlanningStateRef.current = nextState;
        return data;
      });
    },
    [goalId]
  );

  const clearPollTimer = useCallback(() => {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  }, []);

  const handleRefresh = useCallback(async () => {
    if (!userId) return;

    setError(null);

    try {
      await loadExecution(userId);
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error ? err.message : "Failed to refresh execution."
      );
    }
  }, [loadExecution, userId]);

  useEffect(() => {
    if (!goalId) {
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const user = await getCurrentUser();
        const resolvedUserId = user.userId ?? user.userId;

        if (!resolvedUserId) {
          throw new Error("Unable to resolve current user ID.");
        }

        setUserId(resolvedUserId);
        await loadExecution(resolvedUserId);
      } catch (err) {
        console.error(err);
        setError(
          err instanceof Error ? err.message : "Failed to load goal execution."
        );
      } finally {
        setLoading(false);
      }
    };

    void load();

    return () => {
      clearPollTimer();
    };
  }, [goalId, loadExecution, clearPollTimer]);

  useEffect(() => {
    if (!snapshot || !userId) {
      return;
    }

    const planningState = snapshot.planning.state?.toUpperCase();

    if (planningState !== "GENERATING_NEXT_PHASE") {
      pollAttemptsRef.current = 0;
      clearPollTimer();
      return;
    }

    if (pollAttemptsRef.current >= GENERATION_POLL_LIMIT) {
      clearPollTimer();
      return;
    }

    clearPollTimer();

    pollTimerRef.current = setTimeout(async () => {
      try {
        pollAttemptsRef.current += 1;
        await loadExecution(userId);
      } catch (err) {
        console.error(err);
      }
    }, GENERATION_POLL_INTERVAL_MS);

    return () => {
      clearPollTimer();
    };
  }, [snapshot, userId, loadExecution, clearPollTimer]);

  useEffect(() => {
    if (!revealMode) {
      return;
    }

    const timer = setTimeout(() => {
      setRevealMode(false);
    }, 5000);

    return () => clearTimeout(timer);
  }, [revealMode]);

  const shouldShowBanner = useMemo(() => {
    return !!snapshot?.planning;
  }, [snapshot]);

  if (loading) {
    return (
      <main className="min-h-screen bg-[#071018] px-6 py-10 text-white">
        <div className="mx-auto max-w-5xl">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-6">
            <p className="text-sm text-white/70">Loading execution workspace…</p>
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="min-h-screen bg-[#071018] px-6 py-10 text-white">
        <div className="mx-auto max-w-3xl space-y-4 rounded-3xl border border-white/10 bg-white/5 p-6">
          <h1 className="text-2xl font-semibold">Unable to load execution</h1>
          <p className="text-white/70">{error}</p>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => void handleRefresh()}
              className="rounded-xl bg-white px-4 py-2 text-sm font-medium text-black transition hover:opacity-90"
            >
              Retry
            </button>
            <button
              type="button"
              onClick={() => router.push("/")}
              className="rounded-xl border border-white/15 px-4 py-2 text-sm font-medium text-white transition hover:bg-white/5"
            >
              Go home
            </button>
          </div>
        </div>
      </main>
    );
  }

  if (!snapshot || !userId) {
    return (
      <main className="min-h-screen bg-[#071018] px-6 py-10 text-white">
        <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-6">
          <h1 className="text-2xl font-semibold">Execution unavailable</h1>
          <p className="mt-2 text-white/70">
            We could not load this goal right now.
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#071018] px-6 py-10 text-white">
      <div className="mx-auto max-w-6xl space-y-6">
        {shouldShowBanner ? (
          <PlanningStateBanner
            planning={snapshot.planning}
            onRefresh={handleRefresh}
            revealMode={revealMode}
          />
        ) : null}

        <GoalExecutionScreen
          snapshot={snapshot}
          userId={userId}
          onRefresh={handleRefresh}
        />
      </div>
    </main>
  );
}