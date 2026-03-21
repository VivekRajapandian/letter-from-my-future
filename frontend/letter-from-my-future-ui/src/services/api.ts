import { getStoredAccessToken } from "@/services/auth-storage";

export const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "/api";

export function apiFetch(path: string, init?: RequestInit) {
  const token = getStoredAccessToken();

  return fetch(`${API_BASE}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
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
