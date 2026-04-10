import { ExecutionSnapshot } from "@/types/execution";

const API_BASE = "/api";

export type TaskSubmitRequest = {
  userId: string;
  action: "SAVE_PROGRESS" | "COMPLETE" | "SKIP" | "REOPEN";
  note?: string;
  values: Array<{
    inputDefinitionId: string;
    value: unknown;
  }>;
};

export type TaskSubmitResponse = {
  taskId: string;
  submissionId: string;
  executionState: string;
  planningTriggered: boolean;
};

export async function getExecutionSnapshot(
  goalId: string,
  userId: string
): Promise<ExecutionSnapshot> {
  const response = await fetch(
    `${API_BASE}/v2/goals/${goalId}/execution?userId=${encodeURIComponent(userId)}`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
      headers: {
        Accept: "application/json",
      },
    }
  );

  if (!response.ok) {
    const body = await safeReadText(response);
    throw new Error(
      `Failed to fetch execution snapshot. status=${response.status} body=${body}`
    );
  }

  return response.json();
}

export async function submitTask(
  taskId: string,
  payload: TaskSubmitRequest
): Promise<TaskSubmitResponse> {
  const response = await fetch(`${API_BASE}/v2/tasks/${taskId}/submit`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await safeReadText(response);
    throw new Error(
      `Failed to submit task. status=${response.status} body=${body}`
    );
  }

  return response.json();
}

async function safeReadText(response: Response): Promise<string> {
  try {
    return await response.text();
  } catch {
    return "";
  }
}