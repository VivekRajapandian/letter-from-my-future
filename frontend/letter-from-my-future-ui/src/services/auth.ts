import { API_BASE, apiFetch } from "@/services/api";
import {
  clearStoredAccessToken,
  getStoredAccessToken,
  setStoredAccessToken,
} from "@/services/auth-storage";

type AuthResponse = {
  userId: string;
  username: string;
  email: string;
  accessToken: string;
};

type RegisterPayload = {
  email: string;
  username: string;
  password: string;
};

type LoginPayload = {
  usernameOrEmail: string;
  password: string;
};

export type AuthUser = {
  userId: string;
  username: string;
  email: string;
  role: string;
};

async function persistSession(response: Response) {
  const data = (await response.json()) as AuthResponse;
  setStoredAccessToken(data.accessToken);
  return data;
}

export async function registerUser(payload: RegisterPayload) {
  const response = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to register");
  }

  return persistSession(response);
}

export async function loginUser(payload: LoginPayload) {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("Failed to login");
  }

  return persistSession(response);
}

export async function refreshAccessToken() {
  const response = await fetch(`${API_BASE}/auth/refresh`, {
    method: "POST",
    credentials: "include",
  });

  if (!response.ok) {
    clearStoredAccessToken();
    throw new Error("Failed to refresh token");
  }

  const data = (await response.json()) as { accessToken: string };
  setStoredAccessToken(data.accessToken);
  return data.accessToken;
}

export async function getCurrentUser(): Promise<AuthUser> {
  const token = getStoredAccessToken();

  if (!token) {
    throw new Error("No active session");
  }

  let response = await apiFetch("/auth/me");

  if (response.status === 401) {
    await refreshAccessToken();
    response = await apiFetch("/auth/me");
  }

  if (!response.ok) {
    throw new Error("Failed to load current user");
  }

  return (await response.json()) as AuthUser;
}

export async function logoutUser() {
  const token = getStoredAccessToken();

  try {
    await fetch(`${API_BASE}/auth/logout`, {
      method: "POST",
      credentials: "include",
      headers: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : undefined,
    });
  } finally {
    clearStoredAccessToken();
  }
}
