"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  getNextTask,
  completeTask,
  skipTask,
  pauseGoal,
  resumeGoal,
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
};

export default function GoalPage() {
  const params = useParams();
  const router = useRouter();
  const goalId = params?.goalId as string;

  const [task, setTask] = useState<Task | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [finished, setFinished] = useState(false);
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
        } catch (error) {
          console.error(error);
          setFinished(true);
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

  const handleComplete = async () => {
    if (!task || !userId) {
      return;
    }

    try {
      setActionLoading(true);
      await completeTask(task.taskId, userId);
      const next = await getNextTask(goalId, userId);
      setTask(next);
    } catch (error) {
      console.error(error);
      setFinished(true);
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
      const next = await getNextTask(goalId, userId);
      setTask(next);
    } catch (error) {
      console.error(error);
      setFinished(true);
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

  if (finished || !task) {
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

  return (
    <main className="min-h-screen bg-[#FAFAFA] relative overflow-hidden px-6 py-16 flex flex-col items-center">
      <div className="absolute inset-0 pointer-events-none opacity-5 bg-[radial-gradient(circle_at_1px_1px,_black_1px,_transparent_0)] [background-size:24px_24px]" />

      <div className="relative z-10 w-full flex flex-col items-center">
        <GoalHeader
          goalTitle={task.goalTitle}
          phaseName={
            task.phaseIndex && task.phaseCount
              ? `Phase ${task.phaseIndex} of ${task.phaseCount} - ${task.phaseName}`
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
        />
      </div>
    </main>
  );
}
