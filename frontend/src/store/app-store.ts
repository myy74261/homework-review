import { create } from "zustand";
import type { Session } from "@/lib/types";

interface Notice {
  id: number;
  message: string;
  tone: "success" | "error" | "info";
}

interface AppState {
  session: Session | null;
  notices: Notice[];
  noticesEnabled: boolean;
  reduceMotion: boolean;
  setSession: (session: Session | null) => void;
  setNoticesEnabled: (enabled: boolean) => void;
  setReduceMotion: (enabled: boolean) => void;
  notify: (message: string, tone?: Notice["tone"]) => void;
  dismiss: (id: number) => void;
}

let noticeId = 0;

function storedBoolean(key: string, fallback: boolean) {
  return window.localStorage.getItem(key) == null
    ? fallback
    : window.localStorage.getItem(key) === "true";
}

export const useAppStore = create<AppState>((set) => ({
  session: null,
  notices: [],
  noticesEnabled: storedBoolean("review-desk-notices", true),
  reduceMotion: storedBoolean("review-desk-reduce-motion", false),
  setSession: (session) => set({ session }),
  setNoticesEnabled: (enabled) => {
    window.localStorage.setItem("review-desk-notices", String(enabled));
    set({ noticesEnabled: enabled });
  },
  setReduceMotion: (enabled) => {
    window.localStorage.setItem("review-desk-reduce-motion", String(enabled));
    set({ reduceMotion: enabled });
  },
  notify: (message, tone = "info") => {
    if (!useAppStore.getState().noticesEnabled) return;
    const id = ++noticeId;
    set((state) => ({
      notices: [...state.notices, { id, message, tone }],
    }));
    window.setTimeout(() => {
      set((state) => ({
        notices: state.notices.filter((notice) => notice.id !== id),
      }));
    }, 4200);
  },
  dismiss: (id) =>
    set((state) => ({
      notices: state.notices.filter((notice) => notice.id !== id),
    })),
}));
