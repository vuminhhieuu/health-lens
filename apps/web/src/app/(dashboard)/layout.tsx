"use client";

import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/authStore";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";

export default function DashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const router = useRouter();
  const isLoading = useAuthBootstrap();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    router.replace("/login");
    return null;
  }

  return <>{children}</>;
}
