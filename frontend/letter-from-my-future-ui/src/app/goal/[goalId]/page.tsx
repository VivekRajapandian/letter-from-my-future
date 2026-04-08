"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  getNextTask,
  getGoalSummary,
  completeTask,
  skipTask,
  pauseGoal,
  resumeGoal,
  submitTaskResponses,
} from "@/services/api";
import { getCurrentUser } from "@/services/auth";
import GoalHeader from "@/components/GoalHeader";
import TaskCard from "@/components/TaskCard";

type Task = {
  taskId: string;
  title: string;
  description: string;
  goalTitle?: string;
  phaseName?: string;
  phaseIndex?: number;
  phaseCount?: number;
  taskIndex?: number;
  taskCount?: number;
  completedCount?: number;
  questions?: Array<{
    questionId: string;
    questionIndex: number;
    question: string;
    questionType: string;
    hint?: string;
  }>;
  responses?: Array<{
    questionId: string;
    response: string;
  }>;
};

type GoalSummaryResponse = {
  goalId: string;
  title: string;
  status: string;
  targetDate: string | null;
  completedTasks: number;
  totalTasks: number;
  progressPercent: number;
  nextTask: Task | null;
  updatedAt: string;
};

export default function GoalPage() {
  const params = useParams();
  const router = useRouter();
  const goalId = params?.goalId as string;

  const [task, setTask] = useState<Task | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [finished, setFinished] = useState(false);
  const [waitingForNextPhase, setWaitingForNextPhase] = useState(false);
  const [paused, setPaused] = useState(false);
  const [userId, setUserId] = useState("");

  useEffect(() => {
    if (!goalId) {
      return;
    }

    const fetchTask = async () => {
      try {
        const user = await getCurrentUser();
        setUserId(user.userId);

        try {
          const data = await getNextTask(goalId, user.userId);
          setTask(data);
          setFinished(false);
          setWaitingForNextPhase(false);
        } catch (error) {
          console.error(error);
          // Check if goal is actually completed or just waiting for next phase
          try {
            const summaryRes = await getGoalSummary(goalId, user.userId);
            if (summaryRes.ok) {
              const summary = await summaryRes.json();
              if (summary.status === "COMPLETED") {
                setFinished(true);
                setWaitingForNextPhase(false);
              } else {
                setTask(null);
                setFinished(false);
                setWaitingForNextPhase(true);
              }
            } else {
              setFinished(true);
              setWaitingForNextPhase(false);
            }
          } catch (summaryError) {
            console.error("Failed to get goal summary:", summaryError);
            setFinished(true);
            setWaitingForNextPhase(false);
          }
        }
      } catch (error) {
        console.error(error);
        router.replace("/login");
      } finally {
        setLoading(false);
      }
    };

    void fetchTask();
  }, [goalId, router]);

  const loadNextTaskWithRetry = async () => {
    const maxRetries = 3;
    const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

    for (let attempt = 0; attempt < maxRetries; attempt += 1) {
      try {
        const next = await getNextTask(goalId, userId);
        setTask(next);
        setFinished(false);
        setWaitingForNextPhase(false);
        return;
      } catch (error) {
        console.error("Next task load attempt", attempt + 1, "failed", error);
        if (attempt < maxRetries - 1) {
          await delay(500);
        }
      }
    }

    // After retries, check goal status
    try {
      const summaryRes = await getGoalSummary(goalId, userId);
      if (summaryRes.ok) {
        const summary = await summaryRes.json();
        if (summary.status === "COMPLETED") {
          setTask(null);
          setFinished(true);
          setWaitingForNextPhase(false);
        } else {
          setTask(null);
          setFinished(false);
          setWaitingForNextPhase(true);
        }
      } else {
        setTask(null);
        setFinished(true);
        setWaitingForNextPhase(false);
      }
    } catch (summaryError) {
      console.error("Failed to get goal summary after retries:", summaryError);
      setTask(null);
      setFinished(true);
      setWaitingForNextPhase(false);
    }
  };

  const handleComplete = async () => {
    if (!task || !userId) {
      return;
    }

    try {
      setActionLoading(true);
      await completeTask(task.taskId, userId);
      await loadNextTaskWithRetry();
    } catch (error) {
      console.error(error);
      // On error, check if goal is completed or just waiting
      try {
        const summaryRes = await getGoalSummary(goalId, userId);
        if (summaryRes.ok) {
          const summary = await summaryRes.json();
          if (summary.status === "COMPLETED") {
            setFinished(true);
            setWaitingForNextPhase(false);
          } else {
            setTask(null);
            setFinished(false);
            setWaitingForNextPhase(true);
          }
        } else {
          setFinished(true);
          setWaitingForNextPhase(false);
        }
      } catch (summaryError) {
        console.error("Failed to get goal summary on complete error:", summaryError);
        setFinished(true);
        setWaitingForNextPhase(false);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleSkip = async () => {
    if (!task || !userId) {
      return;
    }

    try {
      setActionLoading(true);
      await skipTask(task.taskId, userId);
      await loadNextTaskWithRetry();
    } catch (error) {
      console.error(error);
      // On error, check if goal is completed or just waiting
      try {
        const summaryRes = await getGoalSummary(goalId, userId);
        if (summaryRes.ok) {
          const summary = await summaryRes.json();
          if (summary.status === "COMPLETED") {
            setFinished(true);
            setWaitingForNextPhase(false);
          } else {
            setTask(null);
            setFinished(false);
            setWaitingForNextPhase(true);
          }
        } else {
          setFinished(true);
          setWaitingForNextPhase(false);
        }
      } catch (summaryError) {
        console.error("Failed to get goal summary on skip error:", summaryError);
        setFinished(true);
        setWaitingForNextPhase(false);
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handlePause = async () => {
    if (!userId) {
      return;
    }

    try {
      await pauseGoal(goalId, userId);
      setPaused(true);
    } catch (error) {
      console.error(error);
    }
  };

  const handleResume = async () => {
    if (!userId) {
      return;
    }

    try {
      await resumeGoal(goalId, userId);
      setPaused(false);
    } catch (error) {
      console.error(error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#FAFAFA]">
        <p className="text-gray-400">Loading...</p>
      </div>
    );
  }

  if (finished || (!task && !waitingForNextPhase)) {
    return (
      <main className="min-h-screen bg-[#FAFAFA] flex items-center justify-center px-6">
        <div className="max-w-xl text-center">
          <div className="h-px bg-gray-200 mb-10" />

          <h2 className="text-3xl font-semibold tracking-tight text-[#111]">
            You completed this goal.
          </h2>

          <p className="text-gray-500 mt-6 leading-relaxed">
            The version of you that set this intention followed through.
          </p>

          <div className="mt-10 text-sm text-gray-400 space-y-2">
            <p>All planned tasks completed.</p>
            <p>Progress sustained.</p>
          </div>

          <div className="mt-12">
            <button
              onClick={() => router.push("/app")}
              className="px-6 py-3 bg-black text-white rounded-xl transition hover:opacity-90"
            >
              Start a new goal
            </button>
          </div>
        </div>
      </main>
    );
  }

  if (waitingForNextPhase) {
    return (
      <main className="min-h-screen bg-[#FAFAFA] flex items-center justify-center px-6">
        <div className="max-w-xl text-center">
          <div className="h-px bg-gray-200 mb-10" />

          <h2 className="text-3xl font-semibold tracking-tight text-[#111]">
            Preparing the next phase...
          </h2>

          <p className="text-gray-500 mt-6 leading-relaxed">
            Your goal is still active, and the next batch of tasks is being prepared.
          </p>

          <div className="mt-10 text-sm text-gray-400 space-y-2">
            <p>Current status: ACTIVE</p>
            <p>If it takes a moment, you can refresh the page to check again.</p>
          </div>

          <div className="mt-12 flex justify-center gap-4">
            <button
              onClick={loadNextTaskWithRetry}
              className="px-6 py-3 bg-black text-white rounded-xl transition hover:opacity-90"
            >
              Refresh
            </button>
            <button
              onClick={() => router.push("/app")}
              className="px-6 py-3 border border-black text-black rounded-xl transition hover:bg-black/5"
            >
              Back to goals
            </button>
          </div>
        </div>
      </main>
    );
  }

  if (!task) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#FAFAFA]">
        <p className="text-gray-400">Loading task...</p>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-[#FAFAFA] relative overflow-hidden px-6 py-16 flex flex-col items-center">
      <div className="absolute inset-0 pointer-events-none opacity-5 bg-[radial-gradient(circle_at_1px_1px,_black_1px,_transparent_0)] [background-size:24px_24px]" />

      <div className="relative z-10 w-full flex flex-col items-center">
        <GoalHeader
          goalTitle={task.goalTitle}
          phaseName={
            task.phaseIndex
              ? `Phase ${task.phaseIndex} - ${task.phaseName}`
              : task.phaseName
          }
          taskIndex={task.taskIndex}
          taskCount={task.taskCount}
          completedCount={task.completedCount}
          paused={paused}
          onPause={handlePause}
          onResume={handleResume}
        />

        <TaskCard
          taskId={task.taskId}
          title={task.title}
          description={task.description}
          paused={paused}
          loading={actionLoading}
          onComplete={handleComplete}
          onSkip={handleSkip}
          questions={task.questions}
          existingResponses={task.responses}
        />
      </div>
    </main>
  );
}
