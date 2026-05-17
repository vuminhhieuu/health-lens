"use client";

import * as ToastPrimitive from "@radix-ui/react-toast";
import { X } from "lucide-react";
import { useEffect, useState } from "react";
import {
  type ToastNotification,
  notify,
  subscribeToNotifications,
} from "@/lib/notify";

const toastStyles: Record<ToastNotification["type"], string> = {
  success: "border-emerald-200 bg-emerald-50 text-emerald-950",
  error: "border-red-200 bg-red-50 text-red-950",
  info: "border-sky-200 bg-sky-50 text-sky-950",
  loading: "border-slate-200 bg-white text-slate-950",
};

const markerStyles: Record<ToastNotification["type"], string> = {
  success: "bg-emerald-500",
  error: "bg-red-500",
  info: "bg-sky-500",
  loading: "bg-slate-500",
};

const TOAST_VIEWPORT_LABEL = "Thông báo";
const PERSISTENT_TOAST_DURATION = 24 * 60 * 60 * 1000;

function ToastItem({ toast }: { toast: ToastNotification }) {
  const isError = toast.type === "error";

  return (
    <ToastPrimitive.Root
      className={`flex min-h-14 w-full items-center gap-3 rounded-md border px-4 py-3 text-sm shadow-lg shadow-slate-950/10 ${toastStyles[toast.type]}`}
      duration={toast.durationMs ?? PERSISTENT_TOAST_DURATION}
      onOpenChange={(open) => {
        if (!open) {
          notify.dismiss(toast.id);
        }
      }}
      type={isError ? "foreground" : "background"}
    >
      <span
        aria-hidden="true"
        className={`h-2.5 w-2.5 shrink-0 rounded-full ${markerStyles[toast.type]}`}
      />
      <ToastPrimitive.Description className="min-w-0 flex-1 leading-5">
        {toast.message}
      </ToastPrimitive.Description>
      <ToastPrimitive.Close asChild>
        <button
          aria-label="Đóng thông báo"
          className="rounded p-1 text-current opacity-70 transition hover:bg-black/5 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-current"
          type="button"
        >
          <X aria-hidden="true" className="h-4 w-4" />
        </button>
      </ToastPrimitive.Close>
    </ToastPrimitive.Root>
  );
}

export function ToastProvider() {
  const [toasts, setToasts] = useState<ToastNotification[]>(() => notify.getSnapshot());

  useEffect(() => {
    return subscribeToNotifications(() => {
      setToasts([...notify.getSnapshot()]);
    });
  }, []);

  return (
    <ToastPrimitive.Provider
      label={TOAST_VIEWPORT_LABEL}
      swipeDirection="right"
    >
      {toasts.map(toast => (
        <ToastItem key={toast.id} toast={toast} />
      ))}
      <ToastPrimitive.Viewport
        className="fixed right-4 top-4 z-50 flex w-[calc(100vw-2rem)] max-w-sm list-none flex-col gap-3 p-0 outline-none sm:right-6 sm:top-6"
        label={TOAST_VIEWPORT_LABEL}
      />
    </ToastPrimitive.Provider>
  );
}
