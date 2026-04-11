"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

type AppShellProps = {
  children: React.ReactNode;
};

export default function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();

  const isHome =
    pathname === "/app" ||
    pathname === "/";

  return (
    <div className="min-h-screen bg-[#07111b] text-white">
      <header className="sticky top-0 z-40 border-b border-white/10 bg-[#07111b]/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-3">
            <Link
              href="/app"
              className="text-sm font-semibold tracking-[0.24em] text-white/70 transition hover:text-white"
            >
              LETTER FROM MY FUTURE
            </Link>
          </div>

          <div className="flex items-center gap-3">
            {!isHome && (
              <Link
                href="/app"
                className="rounded-full border border-white/12 bg-white/5 px-4 py-2 text-sm font-medium text-white/85 transition hover:bg-white/10 hover:text-white"
              >
                Home
              </Link>
            )}

            <button
              className="rounded-full border border-black/10 bg-white px-5 py-2 text-sm font-medium text-black transition hover:bg-white/90"
              type="button"
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      <main>{children}</main>
    </div>
  );
}