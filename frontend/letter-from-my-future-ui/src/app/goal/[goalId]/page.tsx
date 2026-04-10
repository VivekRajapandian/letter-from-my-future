"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import GoalExecutionScreen from "@/components/execution/GoalExecutionScreen";
import { getExecutionSnapshot } from "@/services/executionApi";
import { ExecutionSnapshot } from "@/types/execution";
import { getCurrentUser } from "@/services/auth";

export default function GoalPage() {
  const params = useParams();
  const router = useRouter();
  const goalId = params?.goalId as string;

  const [snapshot, setSnapshot] = useState<ExecutionSnapshot | null>(null);
  const [userId, setUserId] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadExecution = useCallback(async (resolvedUserId: string) => {
    const data = await getExecutionSnapshot(goalId, resolvedUserId);
    setSnapshot(data);
  }, [goalId]);

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
  }, [goalId, loadExecution]);

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

  if (loading) {
    return (
      <main className="min-h-screen bg-neutral-950 text-white px-6 py-10">
        <div className="mx-auto max-w-5xl">
          <div className="animate-pulse rounded-3xl border border-white/10 bg-white/5 p-8">
            <div className="h-8 w-56 rounded bg-white/10" />
            <div className="mt-4 h-4 w-96 max-w-full rounded bg-white/10" />
            <div className="mt-10 h-40 rounded-2xl bg-white/10" />
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="min-h-screen bg-neutral-950 text-white px-6 py-10">
        <div className="mx-auto max-w-3xl rounded-3xl border border-red-400/20 bg-red-500/10 p-8">
          <h1 className="text-2xl font-semibold">Unable to load execution</h1>
          <p className="mt-3 text-sm text-white/70">{error}</p>

          <div className="mt-6 flex gap-3">
            <button
              onClick={() => void handleRefresh()}
              className="rounded-xl bg-white px-4 py-2 text-sm font-medium text-black transition hover:opacity-90"
            >
              Retry
            </button>
            <button
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
      <main className="min-h-screen bg-neutral-950 text-white px-6 py-10">
        <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-8">
          <h1 className="text-2xl font-semibold">Execution unavailable</h1>
          <p className="mt-3 text-sm text-white/70">
            We could not load this goal right now.
          </p>
        </div>
      </main>
    );
  }

  return (
    <GoalExecutionScreen
      snapshot={snapshot}
      userId={userId}
      onRefresh={handleRefresh}
    />
  );
}