"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getStoredAccessToken } from "@/services/auth-storage";

export default function Home() {
  const router = useRouter();

  useEffect(() => {
    if (getStoredAccessToken()) {
      router.replace("/app");
    }
  }, [router]);

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[linear-gradient(180deg,#f8f4ec_0%,#f5f1e8_45%,#efebe2_100%)] px-6 py-16">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.75),_transparent_45%),linear-gradient(to_right,rgba(0,0,0,0.03)_1px,transparent_1px),linear-gradient(to_bottom,rgba(0,0,0,0.03)_1px,transparent_1px)] [background-size:auto,32px_32px,32px_32px]" />

      <section className="relative z-10 w-full max-w-2xl rounded-[2rem] border border-black/5 bg-white/75 px-8 py-14 text-center shadow-[0_24px_80px_rgba(34,30,24,0.08)] backdrop-blur-sm sm:px-14">
        <p className="text-sm uppercase tracking-[0.35em] text-[#8a7f70]">
          Letter from My Future
        </p>

        <h1 className="mt-6 text-4xl font-semibold tracking-tight text-[#1f1a14] sm:text-5xl">
          A calm execution system for becoming the person you described.
        </h1>

        <p className="mx-auto mt-5 max-w-lg text-base leading-7 text-[#6f6558] sm:text-lg">
          Start clean if you&apos;re new. Step back in if you&apos;re already
          building.
        </p>

        <div className="mt-12 flex flex-col items-center gap-4">
          <Link
            href="/register"
            className="w-full max-w-xs rounded-full bg-[#1f1a14] px-6 py-4 text-base font-medium text-[#f8f4ec] transition hover:bg-[#2c251d]"
          >
            Create account
          </Link>

          <Link
            href="/login"
            className="w-full max-w-xs rounded-full border border-[#d9d0c4] bg-[#fbf8f1] px-6 py-4 text-base font-medium text-[#2e281f] transition hover:border-[#c9bdaf] hover:bg-white"
          >
            Sign in
          </Link>
        </div>

        <p className="mt-12 text-sm leading-6 text-[#8a7f70]">
          A quiet system for turning a future self-description into steady daily
          action.
        </p>
      </section>
    </main>
  );
}
