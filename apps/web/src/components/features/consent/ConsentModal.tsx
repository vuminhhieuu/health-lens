"use client";

import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { ApiPaths, CONSENT_VERSION } from '@healthlens/shared/constants';
import { useAuthStore } from '@/stores/authStore';
import { apiClient } from '@/lib/api/apiClient';
import { syncActiveConsentVersion } from '@/lib/consent/syncActiveConsentVersion';
import { notify } from '@/lib/notify';
import { ArrowRight, ShieldCheck, TriangleAlert } from "lucide-react";
import { usePathname } from 'next/navigation';

export const ConsentModal: React.FC = () => {
    const pathname = usePathname();
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    const consentGiven = useAuthStore((s) => s.consentGiven);
    const activeConsentVersion = useAuthStore((s) => s.activeConsentVersion);
    const setConsent = useAuthStore((s) => s.setConsent);
    const clearAuth = useAuthStore((s) => s.clearAuth);
    const [submitting, setSubmitting] = useState(false);
    const [ackTerms, setAckTerms] = useState(false);
    const [ackData, setAckData] = useState(false);
    const [ackDisclaimer, setAckDisclaimer] = useState(false);

    useEffect(() => {
        if (!isAuthenticated) return;
        void syncActiveConsentVersion();
    }, [isAuthenticated]);

    const policyVersion = activeConsentVersion ?? CONSENT_VERSION;

    // consentGiven is derived server-side against the active policy version (refresh / GET me/consent).
    const hiddenPaths = ["/login", "/register", "/forgot-password", "/reset-password", "/verify-email", "/admin"];
    const shouldHideOnAuthRoutes = hiddenPaths.some((path) => pathname?.startsWith(path));

    if (!isAuthenticated || consentGiven || shouldHideOnAuthRoutes) {
        return null;
    }

    const allChecked = ackTerms && ackData && ackDisclaimer;

    const handleAccept = async () => {
        setSubmitting(true);
        try {
            await apiClient.post(ApiPaths.CONSENT.ME, { version: policyVersion, accepted: true });
            setConsent(policyVersion);
            notify.success('Đồng thuận đã được ghi nhận thành công');
        } catch (error) {
            console.error('Failed to submit consent', error);

            if (axios.isAxiosError(error)) {
                const status = error.response?.status;
                if (status === 401) {
                    notify.error('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại');
                    clearAuth();
                    window.location.href = '/login';
                } else if (status === 403) {
                    notify.error('Bạn không có quyền thực hiện hành động này');
                } else if (status === 404) {
                    notify.error('Không tìm thấy tài khoản người dùng');
                } else {
                    notify.error('Không thể xử lý yêu cầu, vui lòng thử lại');
                }
            } else {
                notify.error('Không thể xử lý yêu cầu, vui lòng thử lại');
            }
        } finally {
            setSubmitting(false);
        }
    };

    const handleReject = async () => {
        setSubmitting(true);
        try {
            await apiClient.post(ApiPaths.CONSENT.ME, { version: policyVersion, accepted: false });
            notify.info('Bạn đã từ chối đồng thuận. Vui lòng đăng nhập lại để tiếp tục.');
        } catch (error) {
            console.error('Failed to register rejection', error);
            notify.error('Không thể ghi nhận từ chối đồng thuận');
        } finally {
            clearAuth(); // Log out the user immediately if they reject
            setTimeout(() => {
                window.location.href = '/login';
            }, 1000);
        }
    };

    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-2 sm:p-4">
        <div className="w-full max-w-[700px] max-h-[calc(100vh-1rem)] sm:max-h-[calc(100vh-2rem)] overflow-hidden rounded-xl bg-white shadow-[0_24px_80px_rgba(0,0,0,0.25)] flex flex-col">
          <div className="p-6 sm:p-8 text-center text-white bg-linear-to-r from-[#00685f] to-[#008378]">
            <h2 className="text-2xl font-bold tracking-tight">Điều khoản & đồng thuận dữ liệu y tế</h2>
            <p className="mt-2 text-white/85">
              Vui lòng đọc và xác nhận trước khi sử dụng tính năng tải lên/kết quả
            </p>
            <p className="mt-1 text-sm text-white/80">Theo NĐ 13/2023/NĐ-CP</p>
          </div>

          <div className="bg-white p-6 sm:p-8 overflow-y-auto hl-custom-scrollbar">
            <div className="space-y-8 pr-2 text-[15px] leading-relaxed text-slate-700">
              <section>
                <h3 className="text-lg font-semibold text-[#00685f] mb-3">1. Mục đích thu thập</h3>
                <p className="text-slate-600">
                  HealthLens thu thập dữ liệu sức khỏe bạn cung cấp (kết quả xét nghiệm, chỉ số sức khỏe) để hỗ trợ
                  giải thích, theo dõi và cá nhân hóa trải nghiệm sử dụng.
                </p>
              </section>

              <section>
                <h3 className="text-lg font-semibold text-[#00685f] mb-3">2. Thu thập & xử lý dữ liệu (NĐ 13/2023/NĐ-CP)</h3>
                <div className="space-y-4 text-slate-600">
                  <div className="rounded-lg bg-[#e9f6f3] p-4">
                    <p className="font-semibold text-[#3f6560] mb-1">Loại dữ liệu</p>
                    <p>Dữ liệu y tế do bạn tải lên/cung cấp và một số thông tin kỹ thuật phục vụ bảo mật.</p>
                  </div>
                  <div>
                    <p className="font-semibold text-[#3f6560] mb-1">Mục đích xử lý</p>
                    <p>Hiển thị kết quả, giải thích thông tin y khoa, và cải thiện chất lượng dịch vụ.</p>
                  </div>
                  <div>
                    <p className="font-semibold text-[#3f6560] mb-1">Quyền của bạn</p>
                    <p>Bạn có quyền truy cập, chỉnh sửa, yêu cầu xóa hoặc phản đối việc xử lý dữ liệu theo quy định pháp luật.</p>
                  </div>
                </div>
              </section>

              <section>
                <h3 className="text-lg font-semibold text-[#00685f] mb-3">3. Bảo mật dữ liệu</h3>
                <div className="flex items-start gap-3 rounded-lg bg-[#deebe8] p-4">
                  <ShieldCheck className="mt-0.5 h-5 w-5 text-[#00685f]" aria-hidden="true" />
                  <p className="text-sm text-slate-600">
                    Dữ liệu được mã hóa (AES-256) và chỉ sử dụng cho mục đích cung cấp dịch vụ. HealthLens cam kết không chia sẻ với bên thứ ba nếu không có sự đồng ý của bạn.
                  </p>
                </div>
              </section>

              <section>
                <h3 className="text-lg font-semibold text-[#00685f] mb-3">4. Giới hạn trách nhiệm</h3>
                <div className="flex items-start gap-3 rounded-r-lg border-l-4 border-[#924628] bg-[#ffdbce] p-4 text-[#3d4947]">
                  <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-[#924628]" aria-hidden="true" />
                  <p className="font-medium">
                    Khuyến nghị y tế từ HealthLens chỉ mang tính tham khảo, không thay thế tư vấn từ bác sĩ chuyên môn.
                  </p>
                </div>
              </section>
            </div>

            <div className="mt-6 border-t border-slate-200 pt-6 space-y-4">
              <label className="flex items-center gap-3 cursor-pointer min-h-[44px] select-none">
                <input
                  type="checkbox"
                  className="h-5 w-5 rounded border-slate-300 text-[#00685f] focus:ring-[#00685f]"
                  checked={ackTerms}
                  onChange={(e) => setAckTerms(e.target.checked)}
                  disabled={submitting}
                />
                <span className="text-slate-700">Tôi đã đọc và hiểu Điều khoản sử dụng</span>
              </label>

              <label className="flex items-center gap-3 cursor-pointer min-h-[44px] select-none">
                <input
                  type="checkbox"
                  className="h-5 w-5 rounded border-slate-300 text-[#00685f] focus:ring-[#00685f]"
                  checked={ackData}
                  onChange={(e) => setAckData(e.target.checked)}
                  disabled={submitting}
                />
                <span className="text-slate-700">
                  Tôi đồng ý cho HealthLens thu thập và xử lý dữ liệu sức khỏe theo NĐ 13/2023/NĐ-CP
                </span>
              </label>

              <label className="flex items-center gap-3 cursor-pointer min-h-[44px] select-none">
                <input
                  type="checkbox"
                  className="h-5 w-5 rounded border-slate-300 text-[#00685f] focus:ring-[#00685f]"
                  checked={ackDisclaimer}
                  onChange={(e) => setAckDisclaimer(e.target.checked)}
                  disabled={submitting}
                />
                <span className="text-slate-700">Tôi hiểu rằng khuyến nghị y tế chỉ mang tính tham khảo</span>
              </label>
            </div>
          </div>

          <div className="px-6 pb-6 pt-4 sm:px-8 sm:pb-8 border-t border-slate-200 bg-white">
            <div className="flex flex-col-reverse md:flex-row gap-4">
              <button
                onClick={handleReject}
                disabled={submitting}
                className="flex-1 rounded-xl border-2 border-slate-300 bg-transparent px-6 py-3 font-semibold text-slate-600 hover:bg-slate-50 transition disabled:opacity-50"
              >
                Từ chối
              </button>
              <button
                onClick={handleAccept}
                disabled={submitting || !allChecked}
                className="flex-1 rounded-xl bg-linear-to-r from-[#00685f] to-[#008378] px-6 py-3 font-semibold text-white shadow-md hover:brightness-110 active:scale-[0.99] transition disabled:opacity-50 disabled:active:scale-100 inline-flex items-center justify-center gap-2"
              >
                {submitting ? "Đang xác thực..." : "Đồng ý và Tiếp tục"}
                <ArrowRight className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
};
