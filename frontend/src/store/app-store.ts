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
  setSession: (session: Session | null) => void;
  notify: (message: string, tone?: Notice["tone"]) => void;
  dismiss: (id: number) => void;
}

let noticeId = 0;

export const useAppStore = create<AppState>((set) => ({
  session: null,
  notices: [],
  setSession: (session) => set({ session }),
  notify: (message, tone = "info") => {
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
