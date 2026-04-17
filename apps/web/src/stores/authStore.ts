import { create } from "zustand";
import { persist } from "zustand/middleware";

interface UserInfo {
  id: string;
  email: string;
  role: string;
}

export interface SessionConsent {
  consentGiven: boolean;
  consentVersion: string | null;
}

interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  consentGiven: boolean;
  consentVersion: string | null;
  /** Server active policy version; not persisted — refetched when session starts. */
  activeConsentVersion: string | null;
  setAuth: (user: UserInfo, token: string, consent?: SessionConsent) => void;
  setConsent: (version: string) => void;
  setConsentState: (consentGiven: boolean, consentVersion: string | null) => void;
  setActiveConsentVersion: (version: string | null) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      consentGiven: false,
      consentVersion: null,
      activeConsentVersion: null,

      setAuth: (user, token, consent) =>
        set({
          user,
          accessToken: token,
          isAuthenticated: true,
          activeConsentVersion: null,
          ...(consent !== undefined
            ? {
                consentGiven: consent.consentGiven,
                consentVersion: consent.consentVersion,
              }
            : { consentGiven: false, consentVersion: null }),
        }),

      setConsent: (version) =>
        set({
          consentGiven: true,
          consentVersion: version,
        }),

      setConsentState: (consentGiven, consentVersion) =>
        set({
          consentGiven,
          consentVersion,
        }),

      setActiveConsentVersion: (version) => set({ activeConsentVersion: version }),

      clearAuth: () =>
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          consentGiven: false,
          consentVersion: null,
          activeConsentVersion: null,
        }),
    }),
    {
      name: "auth-storage",
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
        consentGiven: state.consentGiven,
        consentVersion: state.consentVersion,
      }),
    }
  )
);
