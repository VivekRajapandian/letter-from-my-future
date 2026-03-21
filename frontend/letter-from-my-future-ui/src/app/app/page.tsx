"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createGoal } from "@/services/api";
import { getCurrentUser } from "@/services/auth";
import {
  validateGoal,
  type GoalValidationResult,
} from "@/lib/goal-validation";

export default function AppPage() {
  const [goal, setGoal] = useState("");
  const [loading, setLoading] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [username, setUsername] = useState("");
  const [userId, setUserId] = useState("");
  const [goalValidation, setGoalValidation] = useState<GoalValidationResult>({
    accepted: false,
    code: "EMPTY",
    message: "Enter a goal to continue.",
  });
  const [hasTriedSubmit, setHasTriedSubmit] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const loadUser = async () => {
      try {
        const user = await getCurrentUser();
        setUsername(user.username);
        setUserId(user.userId);
      } catch {
        router.replace("/login");
      } finally {
        setAuthLoading(false);
      }
    };

    void loadUser();
  }, [router]);

  async function handleGenerate() {
    if (!userId) {
      return;
    }

    const validation = validateGoal(goal);
    setGoalValidation(validation);
    setHasTriedSubmit(true);

    if (!validation.accepted) {
      return;
    }

    try {
      setLoading(true);
      const goalId = await createGoal(userId, validation.normalizedGoal);
      router.push(`/goal/${goalId}`);
    } catch (error) {
      console.error(error);
      alert("Failed to create goal");
    } finally {
      setLoading(false);
    }
  }

  if (authLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5f1e8]">
        <p className="text-sm text-[#8a7f70]">Loading your workspace...</p>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f5f1e8] px-6">
      <div className="w-full max-w-xl rounded-[2rem] border border-black/5 bg-white px-8 py-10 text-center shadow-[0_24px_80px_rgba(34,30,24,0.08)]">
        <div className="text-left">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-[#8a7f70]">
              Letter from My Future
            </p>
            <h1 className="mt-3 text-3xl font-semibold text-[#1f1a14]">
              What future are we building{username ? `, ${username}` : ""}?
            </h1>
          </div>
        </div>

        <p className="mt-4 text-left text-[#6f6558]">
          Describe the direction clearly. The system will turn it into the next
          concrete step.
        </p>

        <textarea
          value={goal}
          onChange={(e) => {
            const nextGoal = e.target.value;
            setGoal(nextGoal);

            if (hasTriedSubmit || nextGoal.trim()) {
              setGoalValidation(validateGoal(nextGoal));
            }
          }}
          placeholder="Describe your goal..."
          aria-invalid={hasTriedSubmit && !goalValidation.accepted}
          className={`mt-8 min-h-40 w-full rounded-[1.5rem] border bg-[#fcfaf6] p-5 text-[#1f1a14] outline-none transition focus:border-[#b8aa98] ${
            hasTriedSubmit && !goalValidation.accepted
              ? "border-[#d8b4a4] focus:border-[#c98d73]"
              : "border-[#e8e0d3]"
          }`}
          rows={5}
        />

        {hasTriedSubmit && !goalValidation.accepted && (
          <div className="mt-4 rounded-[1.25rem] bg-[#f8ede8] px-4 py-3 text-left text-sm text-[#8c4d35]">
            <p>{goalValidation.message}</p>
            {goalValidation.suggestion && (
              <p className="mt-1 text-[#9c6249]">{goalValidation.suggestion}</p>
            )}
          </div>
        )}

        <button
          onClick={handleGenerate}
          disabled={loading || !userId}
          className="mt-6 w-full rounded-full bg-[#1f1a14] px-6 py-4 text-base font-medium text-[#f8f4ec] transition disabled:cursor-not-allowed disabled:opacity-40"
        >
          {loading ? "Generating..." : "Generate Plan"}
        </button>
      </div>
    </main>
  );
}
