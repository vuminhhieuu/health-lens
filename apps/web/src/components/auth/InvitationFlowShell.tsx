"use client";

import type { ReactNode } from "react";

import { ErrorState, LoadingState } from "@/components/ui";

const invitationCardClassName =
  "min-h-48 rounded-3xl border border-white/70 bg-white shadow-[0_20px_60px_rgba(0,79,73,0.08)]";

type InvitationFlowShellProps = {
  children: ReactNode;
  maxWidthClassName?: string;
};

export function InvitationFlowShell({
  children,
  maxWidthClassName = "max-w-lg",
}: InvitationFlowShellProps) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-[#effcf9] px-6 py-10">
      <div className={`w-full ${maxWidthClassName}`}>{children}</div>
    </main>
  );
}

export function InvitationLoadingState({
  title,
  description,
}: {
  title: string;
  description?: string;
}) {
  return (
    <InvitationFlowShell>
      <LoadingState title={title} description={description} className={invitationCardClassName} />
    </InvitationFlowShell>
  );
}

export function InvitationErrorState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <InvitationFlowShell maxWidthClassName="max-w-xl">
      <ErrorState
        title={title}
        description={description}
        action={action}
        className={invitationCardClassName}
      />
    </InvitationFlowShell>
  );
}
