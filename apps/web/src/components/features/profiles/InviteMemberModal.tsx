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
  onSubmit: (payload: {
    email?: string;
    accessLevel?: "view" | "edit";
  }) => void;
  isLoading?: boolean;
  sharedMembers?: Array<{
    id: string;
    email: string;
    accessLevel: "view" | "edit" | string;
  }>;
  isSharedMembersLoading?: boolean;
  onChangeAccessLevel?: (member: { id: string; email: string; accessLevel: "view" | "edit" }) => void;
  isUpdatingAccessLevel?: boolean;
  hasPendingAccessChanges?: boolean;
  title?: string;
  description?: string;
}

export function InviteMemberModal({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
  sharedMembers = [],
  isSharedMembersLoading = false,
  onChangeAccessLevel,
  isUpdatingAccessLevel = false,
  hasPendingAccessChanges = false,
  title = "Chia sẻ hồ sơ",
  description = "Nhập email người nhận.",
}: InviteMemberModalProps) {
  const [email, setEmail] = useState("");
  const [accessLevel, setAccessLevel] = useState<"view" | "edit">("view");
  const [error, setError] = useState<string | null>(null);
  const [showInviteError, setShowInviteError] = useState(false);

  if (!isOpen) return null;

  const handleClose = () => {
    if (isLoading) {
      return;
    }
    if (hasPendingAccessChanges) {
      setError(null);
      setShowInviteError(false);
      onSubmit({});
      return;
    }
    setEmail("");
    setAccessLevel("view");
    setError(null);
    setShowInviteError(false);
    onClose();
  };

  const handleFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setShowInviteError(true);
    const trimmedEmail = email.trim().toLowerCase();
    if (!trimmedEmail && !hasPendingAccessChanges) {
      setError("Vui lòng nhập địa chỉ email.");
      return;
    }
    setError(null);
    onSubmit({
      email: trimmedEmail || undefined,
      accessLevel: trimmedEmail ? accessLevel : undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-[32px] bg-white shadow-2xl shadow-black/20 animate-in zoom-in-95 duration-200"
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
            <div className="pr-14">
              <h2 className="text-2xl font-black leading-tight text-[#121e1c]">{title}</h2>
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
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setError(null);
                    setShowInviteError(false);
                  }}
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
                  onChange={(e) => {
                    setAccessLevel(e.target.value as "view" | "edit");
                    setError(null);
                    setShowInviteError(false);
                  }}
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

            {showInviteError && error ? <p className="text-sm font-medium text-[#ba1a1a]">{error}</p> : null}

            <div className="rounded-2xl border border-[#d6ece7] bg-[#f7fcfa] p-4">
              <div className="mb-3 flex items-center justify-between">
                <p className="text-sm font-bold text-[#121e1c]">Người đã được chia sẻ</p>
                {isSharedMembersLoading ? (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold text-[#6d7a77]">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    Đang tải
                  </span>
                ) : null}
              </div>

              {sharedMembers.length === 0 ? (
                <p className="text-sm text-[#6d7a77]">Chưa có ai được cấp quyền truy cập.</p>
              ) : (
                <ul className="max-h-[170px] space-y-2 overflow-y-auto pr-1">
                  {sharedMembers.map((member) => (
                    <li
                      key={member.id}
                      className="flex items-center justify-between rounded-xl border border-[#e2efeb] bg-white px-3 py-2.5"
                    >
                      <p className="truncate pr-3 text-sm font-medium text-[#23312f]">{member.email}</p>
                      <div className="relative shrink-0">
                        <select
                          disabled={isUpdatingAccessLevel}
                          value={member.accessLevel === "edit" ? "edit" : "view"}
                          onChange={(event) => {
                            const nextAccessLevel = event.target.value as "view" | "edit";
                            setError(null);
                            setShowInviteError(false);
                            onChangeAccessLevel?.({
                              id: member.id,
                              email: member.email,
                              accessLevel: nextAccessLevel,
                            });
                          }}
                          className="h-8 cursor-pointer appearance-none rounded-full border border-[#d2ebe6] bg-[#e7f5f2] px-3 pr-7 text-xs font-bold text-[#00685f] outline-none transition hover:brightness-95 disabled:opacity-60"
                        >
                          <option value="view">Chỉ xem</option>
                          <option value="edit">Có thể chỉnh sửa</option>
                        </select>
                        <span className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-[#00685f]">
                          ▾
                        </span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
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
