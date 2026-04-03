"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { getCurrentUser, loginUser, registerUser } from "@/services/auth";
import { getStoredAccessToken } from "@/services/auth-storage";

type AuthFormProps = {
  mode: "login" | "register";
  title: string;
  description: string;
  submitLabel: string;
  alternateLabel: string;
  alternateHref: string;
  defaultUsername?: string;
  defaultPassword?: string;
};

export default function AuthForm({
  mode,
  title,
  description,
  submitLabel,
  alternateLabel,
  alternateHref,
  defaultUsername = "",
  defaultPassword = "",
}: AuthFormProps) {
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState(defaultUsername);
  const [password, setPassword] = useState(defaultPassword);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!getStoredAccessToken()) {
      return;
    }

    const verifySession = async () => {
      try {
        await getCurrentUser();
        router.replace("/app");
      } catch {
        // Keep the user on the page if the stored token is stale.
      }
    };

    void verifySession();
  }, [router]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      if (mode === "register") {
        await registerUser({ email, username, password });
      } else {
        await loginUser({ usernameOrEmail: username, password });
      }

      router.replace("/app");
    } catch (submitError) {
      console.error(submitError);
      setError(
        mode === "register"
          ? "Account creation failed. Check your details and try again."
          : "Sign in failed. Check your credentials and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-[linear-gradient(180deg,#f8f4ec_0%,#f2ede3_100%)] px-6 py-16">
      <div className="w-full max-w-md rounded-[2rem] border border-black/5 bg-white px-8 py-10 shadow-[0_24px_80px_rgba(34,30,24,0.08)]">
        <Link
          href="/"
          className="text-sm uppercase tracking-[0.3em] text-[#8a7f70]"
        >
          Letter from My Future
        </Link>

        <h1 className="mt-6 text-3xl font-semibold tracking-tight text-[#1f1a14]">
          {title}
        </h1>

        <p className="mt-4 text-[#6f6558]">{description}</p>

        <form onSubmit={handleSubmit} className="mt-8 space-y-4">
          {mode === "register" && (
            <label className="block">
              <span className="mb-2 block text-sm text-[#5d5347]">Email</span>
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                className="w-full rounded-2xl border border-[#e8e0d3] bg-[#fcfaf6] px-4 py-3 text-[#1f1a14] outline-none transition focus:border-[#b8aa98]"
              />
            </label>
          )}

          <label className="block">
            <span className="mb-2 block text-sm text-[#5d5347]">
              {mode === "register" ? "Username" : "Username or email"}
            </span>
            <input
              type="text"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
              className="w-full rounded-2xl border border-[#e8e0d3] bg-[#fcfaf6] px-4 py-3 text-[#1f1a14] outline-none transition focus:border-[#b8aa98]"
            />
          </label>

          <label className="block">
            <span className="mb-2 block text-sm text-[#5d5347]">Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              className="w-full rounded-2xl border border-[#e8e0d3] bg-[#fcfaf6] px-4 py-3 text-[#1f1a14] outline-none transition focus:border-[#b8aa98]"
            />
          </label>

          {error && (
            <p className="rounded-2xl bg-[#f8ede8] px-4 py-3 text-sm text-[#8c4d35]">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-full bg-[#1f1a14] px-6 py-4 text-base font-medium text-[#f8f4ec] transition disabled:cursor-not-allowed disabled:opacity-40"
          >
            {loading ? "Working..." : submitLabel}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-[#6f6558]">
          <Link href={alternateHref} className="font-medium text-[#2e281f]">
            {alternateLabel}
          </Link>
        </p>
      </div>
    </main>
  );
}
