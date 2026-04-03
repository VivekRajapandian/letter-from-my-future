"use client";

import AuthForm from "@/components/AuthForm";

export default function LoginPage() {
  return (
    <AuthForm
      mode="login"
      title="Welcome back."
      description="Sign in and continue the version of yourself you already started building."
      submitLabel="Sign in"
      alternateLabel="Create account"
      alternateHref="/register"
      defaultUsername="demo"
      defaultPassword="demo"
    />
  );
}
