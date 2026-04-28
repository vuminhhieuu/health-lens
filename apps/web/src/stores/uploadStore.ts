"use client";

import { create } from "zustand";

type UploadStore = {
  justUploaded: boolean;
  markJustUploaded: () => void;
  clearJustUploaded: () => void;
};

export const useUploadStore = create<UploadStore>((set) => ({
  justUploaded: false,
  markJustUploaded: () => set({ justUploaded: true }),
  clearJustUploaded: () => set({ justUploaded: false }),
}));
