import { BookOpen, ClipboardCheck, Home, LogOut, Settings, Sparkles } from "lucide-react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Button } from "@/components/ui/button";

const teacherNav = [
  { to: "/teacher", label: "我的课堂", icon: Home },
  { to: "/teacher/assignments/new", label: "发布作业", icon: BookOpen },
];

const studentNav = [{ to: "/student", label: "我的作业", icon: ClipboardCheck }];

export function AppShell() {
  const session = useAppStore((state) => state.session)!;
  const notices = useAppStore((state) => state.notices);
  const dismiss = useAppStore((state) => state.dismiss);
  const navigate = useNavigate();
  const nav = session.user.role === "TEACHER" ? teacherNav : studentNav;

  async function logout() {
    try {
      await api.logout();
    } catch {
      // 会话已经失效时直接回到登录页。
    }
    window.location.assign("/login?logout");
  }

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <header className="sticky top-0 z-30 border-b border-line/80 bg-white/85 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-[1380px] items-center gap-5 px-4 sm:px-6">
          <button
            type="button"
            onClick={() => navigate(session.homePath)}
            className="flex items-center gap-3 text-left"
          >
            <span className="grid size-9 place-items-center rounded-xl bg-pine-600 font-display text-lg text-white">
              鉴
            </span>
            <span>
              <strong className="block font-display text-lg leading-tight">明鉴</strong>
              <small className="hidden text-[11px] tracking-[0.16em] text-muted sm:block">
                作业审核平台
              </small>
            </span>
          </button>

          <nav className="ml-2 hidden items-center gap-1 md:flex">
            {nav.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === session.homePath}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-muted transition",
                      isActive && "bg-pine-50 font-medium text-pine-700",
                      !isActive && "hover:bg-stone-50 hover:text-ink",
                    )
                  }
                >
                  <Icon className="size-4" />
                  {item.label}
                </NavLink>
              );
            })}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <Link
              to="/account"
              className="flex items-center gap-2 rounded-full bg-stone-100 px-3 py-1.5 text-xs text-muted transition hover:bg-pine-50 hover:text-pine-700"
            >
              <Settings className="size-3.5" />
              <span className="hidden sm:inline">
                {session.user.displayName} · {session.user.role === "TEACHER" ? "教师" : "学生"}
              </span>
              <span className="sm:hidden">{session.user.displayName.slice(0, 1)}</span>
            </Link>
            <Button variant="ghost" size="icon" onClick={logout} aria-label="退出登录">
              <LogOut className="size-4" />
            </Button>
          </div>
        </div>
        <nav className="flex gap-1 overflow-x-auto border-t border-line/60 px-4 py-2 md:hidden">
          {nav.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === session.homePath}
                className={({ isActive }) =>
                  cn(
                    "flex shrink-0 items-center gap-2 rounded-lg px-3 py-1.5 text-xs text-muted",
                    isActive && "bg-pine-50 text-pine-700",
                  )
                }
              >
                <Icon className="size-3.5" />
                {item.label}
              </NavLink>
            );
          })}
        </nav>
      </header>

      <main className="mx-auto max-w-[1380px] px-4 pb-16 pt-7 sm:px-6">
        <Outlet />
      </main>

      <div className="fixed bottom-5 right-5 z-50 grid w-[min(360px,calc(100%-40px))] gap-2">
        {notices.map((notice) => (
          <button
            type="button"
            key={notice.id}
            onClick={() => dismiss(notice.id)}
            className={cn(
              "flex items-start gap-3 rounded-xl border px-4 py-3 text-left text-sm shadow-lg",
              notice.tone === "success" && "border-pine-500/20 bg-pine-50 text-pine-700",
              notice.tone === "error" && "border-clay-500/20 bg-clay-50 text-clay-600",
              notice.tone === "info" && "border-line bg-white text-ink",
            )}
          >
            <Sparkles className="mt-0.5 size-4 shrink-0" />
            <span>{notice.message}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
