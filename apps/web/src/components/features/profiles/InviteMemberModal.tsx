"use client";

import React, { FormEvent, useState } from "react";
import { UserPlus, X, Mail, Shield, Loader2, ArrowRight } from "lucide-react";

export const INVITE_ACCESS_OPTIONS = [
  { value: "view", label: "Chỉ xem" },
  { value: "edit", label: "Chỉnh sửa" },
] as const;

interface InviteMemberModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (payload: { email: string; accessLevel: "view" | "edit" }) => void;
  isLoading?: boolean;
  title?: string;
  description?: string;
}

export function InviteMemberModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
  title = "Chia sẻ hồ sơ",
  description = "Nhập email người nhận.",
}: InviteMemberModalProps) {
  const [email, setEmail] = useState("");
  const [accessLevel, setAccessLevel] = useState<"view" | "edit">("view");
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleClose = () => {
    setEmail("");
    setAccessLevel("view");
    setError(null);
    onClose();
  };

  const handleFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedEmail = email.trim().toLowerCase();
    if (!trimmedEmail) {
      setError("Vui lòng nhập địa chỉ email.");
      return;
    }
    setError(null);
    onSubmit({ email: trimmedEmail, accessLevel });
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-[32px] bg-white shadow-2xl shadow-black/20 animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="relative p-8 pb-4">
          <button
            type="button"
            onClick={handleClose}
            className="absolute top-6 right-6 rounded-full p-2 text-[#6d7a77] transition-colors hover:bg-[#e9f6f3]"
            aria-label="Đóng"
          >
            <X size={20} />
          </button>

          <div className="mb-2 flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#e9f6f3] text-[#00685f]">
              <UserPlus size={24} />
            </div>
            <div>
              <h2 className="text-2xl font-black text-[#121e1c]">{title}</h2>
              <p className="text-sm font-medium text-[#6d7a77]">{description}</p>
            </div>
          </div>
        </div>

        <form onSubmit={handleFormSubmit} className="flex flex-1 flex-col">
          <div className="space-y-5 overflow-y-auto px-8 pb-6">
            <div>
              <label className="mb-2 block text-sm font-bold text-[#121e1c]" htmlFor="invite-email">
                Địa chỉ Email <span className="text-red-600">*</span>
              </label>
              <div className="relative">
                <Mail className="absolute top-1/2 left-4 h-[18px] w-[18px] -translate-y-1/2 text-[#9ba9a6]" />
                <input
                  id="invite-email"
                  type="email"
                  autoComplete="email"
                  placeholder="ví dụ: email@vidu.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="h-12 w-full rounded-2xl border border-[#c5dfd9] pr-4 pl-12 text-sm text-[#3d4947] outline-none focus:border-[#008378]"
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-sm font-bold text-[#121e1c]" htmlFor="invite-access">
                Quyền truy cập <span className="text-red-600">*</span>
              </label>
              <div className="relative">
                <Shield className="pointer-events-none absolute top-1/2 left-4 h-[18px] w-[18px] -translate-y-1/2 text-[#00685f]" />
                <select
                  id="invite-access"
                  value={accessLevel}
                  onChange={(e) => setAccessLevel(e.target.value as "view" | "edit")}
                  className="h-12 w-full cursor-pointer appearance-none rounded-2xl border border-[#b7e8e0] bg-[#f0faf8] pr-10 pl-12 text-sm font-medium text-[#3d4947] outline-none focus:border-[#008378]"
                >
                  {INVITE_ACCESS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
                <span className="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-[#00685f]">▾</span>
              </div>
            </div>

            {error ? <p className="text-sm font-medium text-[#ba1a1a]">{error}</p> : null}
          </div>

          <div className="mt-auto flex items-center justify-end border-t border-[#e8eeec] px-8 py-5">
            <button
              type="submit"
              disabled={isLoading}
              className="flex items-center gap-2 rounded-2xl bg-[#008378] px-6 py-3 text-sm font-bold text-white shadow-md transition hover:brightness-110 disabled:opacity-70"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang gửi...
                </>
              ) : (
                <>
                  Gửi lời mời
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
