"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { createGoal,getOngoingGoals  } from "@/services/api";
import { getCurrentUser } from "@/services/auth";
import {
  validateGoal,
  type GoalValidationResult,
} from "@/lib/goal-validation";

type GoalCardResponse = {
  goalId: string;
  title: string;
  status: string;
  progressPercent: number;
  completedTasks: number;
  totalTasks: number;
  nextTaskTitle: string | null;
  phaseName: string | null;
  phaseIndex: number;
  phaseCount: number;
  targetDate: string | null;
  updatedAt: string;
};

type OngoingGoalsResponse = {
  userId: string;
  hasOngoingGoals: boolean;
  goals: GoalCardResponse[];
};

export default function AppPage() {
  const [goal, setGoal] = useState("");
  const [loading, setLoading] = useState(false);
  const [authLoading, setAuthLoading] = useState(true);
  const [goalsLoading, setGoalsLoading] = useState(true);
  const [goalsError, setGoalsError] = useState<string | null>(null);
  const [ongoingGoals, setOngoingGoals] = useState<GoalCardResponse[]>([]);
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
    const loadUserAndGoals = async () => {
      try {
        const user = await getCurrentUser();
        setUsername(user.username);
        setUserId(user.userId);

        try {
          setGoalsLoading(true);
          setGoalsError(null);

          const res = await getOngoingGoals(user.userId);
          if (!res.ok) {
            throw new Error("Failed to load goals");
          }

          const data: OngoingGoalsResponse = await res.json();
          setOngoingGoals(data.goals ?? []);
        } catch (error) {
          console.error(error);
          setGoalsError("Could not load your ongoing goals.");
        } finally {
          setGoalsLoading(false);
        }
      } catch {
        router.replace("/login");
      } finally {
        setAuthLoading(false);
      }
    };

    void loadUserAndGoals();
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

  function openGoal(goalId: string) {
    router.push(`/goal/${goalId}`);
  }

  if (authLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5f1e8]">
        <p className="text-sm text-[#8a7f70]">Loading your workspace...</p>
      </main>
    );
  }

  return (
    <main className="flex min-h-screen justify-center bg-[#f5f1e8] px-6 py-10">
      <div className="w-full max-w-4xl rounded-[2rem] border border-black/5 bg-white px-8 py-10 shadow-[0_24px_80px_rgba(34,30,24,0.08)]">
        <div className="text-left">
          <p className="text-sm uppercase tracking-[0.3em] text-[#8a7f70]">
            Letter from My Future
          </p>
          <h1 className="mt-3 text-3xl font-semibold text-[#1f1a14]">
            {ongoingGoals.length > 0
              ? `Continue your path${username ? `, ${username}` : ""}`
              : `What future are we building${username ? `, ${username}` : ""}?`}
          </h1>
          <p className="mt-4 text-[#6f6558]">
            {ongoingGoals.length > 0
              ? "Pick up an active goal or start a new direction."
              : "Describe the direction clearly. The system will turn it into the next concrete step."}
          </p>
        </div>

        {goalsLoading ? (
          <div className="mt-8 rounded-[1.5rem] border border-[#e8e0d3] bg-[#fcfaf6] p-5 text-left">
            <p className="text-sm text-[#8a7f70]">Loading ongoing goals...</p>
          </div>
        ) : goalsError ? (
          <div className="mt-8 rounded-[1.5rem] border border-[#f0d7cb] bg-[#fbf1eb] p-5 text-left">
            <p className="text-sm text-[#8c4d35]">{goalsError}</p>
          </div>
        ) : ongoingGoals.length > 0 ? (
          <div className="mt-8">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-[#1f1a14]">
                Ongoing Goals
              </h2>
              <span className="text-sm text-[#8a7f70]">
                {ongoingGoals.length} active
              </span>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              {ongoingGoals.map((goalItem) => (
                <button
                  key={goalItem.goalId}
                  onClick={() => openGoal(goalItem.goalId)}
                  className="rounded-[1.5rem] border border-[#e8e0d3] bg-[#fcfaf6] p-5 text-left transition hover:border-[#d6c8b5]"
                >
                  <div className="flex items-center justify-between">
                    <span className="rounded-full bg-[#efe7da] px-3 py-1 text-xs font-medium text-[#6f6558]">
                      {goalItem.status}
                    </span>
                    <span className="text-xs text-[#8a7f70]">
                      {goalItem.progressPercent}%
                    </span>
                  </div>

                  <h3 className="mt-4 text-base font-semibold text-[#1f1a14]">
                    {goalItem.title}
                  </h3>

                  <div className="mt-4 h-2 w-full overflow-hidden rounded-full bg-[#ece4d8]">
                    <div
                      className="h-full rounded-full bg-[#1f1a14]"
                      style={{ width: `${goalItem.progressPercent}%` }}
                    />
                  </div>

                  <p className="mt-3 text-sm text-[#6f6558]">
                    {goalItem.completedTasks}/{goalItem.totalTasks} tasks complete
                  </p>

                  {goalItem.phaseName && (
                    <p className="mt-2 text-sm text-[#7a6f62]">
                      Phase: {goalItem.phaseName}
                      {goalItem.phaseIndex > 0 && goalItem.phaseCount > 0
                        ? ` (${goalItem.phaseIndex}/${goalItem.phaseCount})`
                        : ""}
                    </p>
                  )}

                  {goalItem.nextTaskTitle && (
                    <p className="mt-2 text-sm text-[#7a6f62]">
                      Next: {goalItem.nextTaskTitle}
                    </p>
                  )}
                </button>
              ))}
            </div>

            <div className="mt-8 border-t border-[#eee5d9] pt-6">
              <p className="text-sm uppercase tracking-[0.25em] text-[#8a7f70]">
                Start Something New
              </p>
            </div>
          </div>
        ) : (
          <div className="mt-8 rounded-[1.5rem] border border-[#e8e0d3] bg-[#fcfaf6] p-5 text-left">
            <p className="text-sm uppercase tracking-[0.25em] text-[#8a7f70]">
              No Ongoing Goals
            </p>
            <h2 className="mt-2 text-xl font-semibold text-[#1f1a14]">
              Start your first path
            </h2>
            <p className="mt-2 text-sm text-[#6f6558]">
              There is nothing in progress yet. Create a goal and the system
              will turn it into an execution plan.
            </p>
          </div>
        )}

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