"use client";

import React from "react";
import { User, ChevronRight, Calendar } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";

export type HealthStatus = "normal" | "warning" | "critical";

interface ProfileCardProps {
  name: string;
  relationship?: string;
  avatarUrl?: string;
  latestStatus?: HealthStatus;
  lastUpdated?: string | Date;
  isSelected?: boolean;
  onPress?: () => void;
}

const statusConfig = {
  normal: {
    label: "Bình thường",
    color: "bg-[#ecfdf5] text-[#065f46]",
    dot: "bg-[#10b981]",
  },
  warning: {
    label: "Cần chú ý",
    color: "bg-[#fffbeb] text-[#92400e]",
    dot: "bg-[#f59e0b]",
  },
  critical: {
    label: "Bất thường",
    color: "bg-[#fef2f2] text-[#991b1b]",
    dot: "bg-[#ef4444]",
  },
};

export function ProfileCard({
  name,
  relationship,
  avatarUrl,
  latestStatus,
  lastUpdated,
  isSelected,
  onPress,
}: ProfileCardProps) {
  const status = latestStatus ? statusConfig[latestStatus] : null;

  const formattedDate = lastUpdated
    ? formatDistanceToNow(new Date(lastUpdated), { addSuffix: true, locale: vi })
    : "Chưa có dữ liệu";

  return (
    <div
      onClick={onPress}
      className={`
        group relative flex flex-col p-6 rounded-3xl transition-all duration-300 cursor-pointer
        border-2 
        ${isSelected 
          ? "bg-white border-[#00685f] shadow-lg shadow-[#00685f]/10 translate-y-[-4px]" 
          : "bg-white/60 border-transparent hover:bg-white hover:border-[#bcc9c6]/40 shadow-sm hover:shadow-md hover:translate-y-[-2px]"}
      `}
    >
      <div className="flex items-start justify-between mb-6">
        <div className="flex items-center gap-4">
          <div className="relative">
            <div className="w-14 h-14 rounded-2xl overflow-hidden bg-[#e9f6f3] border-2 border-[#89f5e7]/30 flex items-center justify-center">
              {avatarUrl ? (
                <img src={avatarUrl} alt={name} className="w-full h-full object-cover" />
              ) : (
                <User className="w-7 h-7 text-[#00685f]" />
              )}
            </div>
            {isSelected && (
              <div className="absolute -top-1 -right-1 w-4 h-4 bg-[#00685f] rounded-full border-2 border-white flex items-center justify-center">
                <div className="w-1.5 h-1.5 bg-white rounded-full"></div>
              </div>
            )}
          </div>
          <div>
            <h3 className="font-bold text-[#121e1c] text-lg group-hover:text-[#00685f] transition-colors line-clamp-1">
              {name}
            </h3>
            {relationship && (
              <p className="text-xs font-bold uppercase tracking-wider text-[#6d7a77] mt-0.5">
                {relationship}
              </p>
            )}
          </div>
        </div>
        <ChevronRight className="w-5 h-5 text-[#bcc9c6] group-hover:text-[#00685f] group-hover:translate-x-1 transition-all" />
      </div>

      <div className="mt-auto pt-4 border-t border-[#bcc9c6]/20 flex flex-col gap-3">
        {status ? (
          <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold w-fit ${status.color}`}>
            <span className={`w-2 h-2 rounded-full ${status.dot} animate-pulse`}></span>
            {status.label}
          </div>
        ) : (
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold w-fit bg-[#f1f5f9] text-[#64748b]">
            <span className="w-2 h-2 rounded-full bg-[#cbd5e1]"></span>
            Chưa có cập nhật
          </div>
        )
        }

        <div className="flex items-center gap-2 text-[#6d7a77] text-xs font-medium">
          <Calendar className="w-3.5 h-3.5" />
          <span>Cập nhật: {formattedDate}</span>
        </div>
      </div>
    </div>
  );
}
