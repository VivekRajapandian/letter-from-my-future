import { getStoredAccessToken } from "@/services/auth-storage";

export const API_BASE ="http://localhost:8080";

export function apiFetch(path: string, init?: RequestInit) {
  const token = getStoredAccessToken();

  return fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "Accept": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  });
}

export async function createGoal(userId: string, goalDescription: string) {
  const res = await apiFetch(`/goals?userId=${userId}`, {
    method: "POST",
    body: JSON.stringify({
      goalDescription,
    }),
  });

  if (!res.ok) {
    throw new Error("Failed to create goal");
  }

  return res.json();
}

export async function getNextTask(goalId: string, userId: string) {
  const res = await apiFetch(`/goals/${goalId}/next-task?userId=${userId}`);

  if (!res.ok) {
    throw new Error("Failed to fetch next task");
  }

  return res.json();
}

export async function completeTask(taskId: string, userId: string) {
  const res = await apiFetch(`/tasks/${taskId}/${userId}/complete`, {
    method: "POST",
  });

  if (!res.ok) {
    throw new Error("Failed to complete task");
  }
}

export async function skipTask(taskId: string, userId: string) {
  const res = await apiFetch(`/tasks/${taskId}/${userId}/skip`, {
    method: "POST",
  });

  if (!res.ok) {
    throw new Error("Failed to skip task");
  }
}

export async function pauseGoal(goalId: string, userId: string) {
  const res = await apiFetch(`/goals/${goalId}/${userId}/pause`, {
    method: "POST",
  });

  if (!res.ok) {
    throw new Error("Failed to pause goal");
  }
}

export async function resumeGoal(goalId: string, userId: string) {
  const res = await apiFetch(`/goals/${goalId}/resume?userId=${userId}`, {
    method: "POST",
  });

  if (!res.ok) {
    throw new Error("Failed to resume goal");
  }
}

export type GoalCardResponse = {
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

export type OngoingGoalsResponse = {
  userId: string;
  hasOngoingGoals: boolean;
  goals: GoalCardResponse[];
};

export async function getOngoingGoals(
  userName: string
): Promise<Response> {

  const response = await apiFetch(`/users/${userName}/goals/ongoing`, {
    method: "GET"
  });

  return response;
}
