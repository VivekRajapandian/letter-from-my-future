"use client";

import AuthForm from "@/components/AuthForm";

export default function RegisterPage() {
  return (
    <AuthForm
      mode="register"
      title="Create your account."
      description="Start with a clear identity, then let the system turn that into steady execution."
      submitLabel="Create account"
      alternateLabel="Sign in"
      alternateHref="/login"
    />
  );
}
