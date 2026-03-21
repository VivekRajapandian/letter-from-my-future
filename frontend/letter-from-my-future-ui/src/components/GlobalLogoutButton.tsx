"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { logoutUser } from "@/services/auth";
import { getStoredAccessToken } from "@/services/auth-storage";

export default function GlobalLogoutButton() {
  const pathname = usePathname();
  const router = useRouter();
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    setIsVisible(Boolean(getStoredAccessToken()));
  }, [pathname]);

  async function handleLogout() {
    try {
      await logoutUser();
    } finally {
      setIsVisible(false);
      router.replace("/");
    }
  }

  if (!isVisible) {
    return null;
  }

  return (
    <button
      type="button"
      onClick={handleLogout}
      className="fixed right-6 top-6 z-50 rounded-full border border-[#d9d0c4] bg-white/90 px-4 py-2 text-sm text-[#5d5347] shadow-[0_12px_30px_rgba(34,30,24,0.08)] backdrop-blur-sm transition hover:bg-[#f8f4ec]"
    >
      Log out
    </button>
  );
}
