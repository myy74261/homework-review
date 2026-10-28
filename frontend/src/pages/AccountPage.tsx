import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  KeyRound,
  LogOut,
  Repeat2,
  ShieldCheck,
  SlidersHorizontal,
  UserRound,
  UserX,
} from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
import { cn } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Field, Input } from "@/components/ui/field";

type AccountTab = "profile" | "security" | "sessions";

export function AccountPage() {
  const session = useAppStore((state) => state.session)!;
  const setSession = useAppStore((state) => state.setSession);
  const notify = useAppStore((state) => state.notify);
  const noticesEnabled = useAppStore((state) => state.noticesEnabled);
  const reduceMotion = useAppStore((state) => state.reduceMotion);
  const setNoticesEnabled = useAppStore((state) => state.setNoticesEnabled);
  const setReduceMotion = useAppStore((state) => state.setReduceMotion);
  const queryClient = useQueryClient();
  const isTeacher = session.user.role === "TEACHER";

  const [activeTab, setActiveTab] = useState<AccountTab>("profile");
  const [displayName, setDisplayName] = useState(session.user.displayName);
  const [email, setEmail] = useState(session.user.email ?? "");
  const [department, setDepartment] = useState(session.user.department ?? "");
  const [professionalTitle, setProfessionalTitle] = useState(session.user.professionalTitle ?? "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivatePassword, setDeactivatePassword] = useState("");

  const updateProfile = useMutation({
    mutationFn: () =>
      api.updateProfile({
        displayName,
        email,
        department: isTeacher ? department : "",
        professionalTitle: isTeacher ? professionalTitle : "",
      }),
    onSuccess: async (user: User) => {
      const current = useAppStore.getState().session;
      if (current) setSession({ ...current, user });
      notify("个人信息已更新。", "success");
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["student-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const changePassword = useMutation({
    mutationFn: () =>
      api.changePassword({
        currentPassword,
        newPassword,
      }),
    onSuccess: () => {
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setFormError("");
      notify("密码已更新，下次登录时请使用新密码。", "success");
    },
    onError: (error: Error) => setFormError(error.message),
  });

  const logout = useMutation({
    mutationFn: (mode: "logout" | "switch") => api.logout().then(() => mode),
    onSuccess: (mode) => {
      window.location.assign(mode === "switch" ? "/login?switch" : "/login?logout");
    },
    onError: () => window.location.assign("/login"),
  });

  const deactivateAccount = useMutation({
    mutationFn: () =>
      api.deactivateAccount({
        currentPassword: deactivatePassword,
      }),
    onSuccess: () => window.location.assign("/login?deactivated"),
    onError: (error: Error) => notify(error.message, "error"),
  });

  function submitPassword(event: React.FormEvent) {
    event.preventDefault();
    setFormError("");
    if (newPassword !== confirmPassword) {
      setFormError("两次输入的新密码不一致");
      return;
    }
    changePassword.mutate();
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to={session.homePath.replace("/app", "")}>
          <ArrowLeft className="size-4" />
          返回工作台
        </Link>
      </Button>

      <section className="flex flex-col justify-between gap-4 rounded-2xl border border-line bg-white/85 px-5 py-5 shadow-card sm:flex-row sm:items-center">
        <div>
          <p className="page-eyebrow">账号与资料</p>
          <h1 className="mt-1 font-display text-3xl font-semibold tracking-tight">个人中心</h1>
          <p className="mt-1 text-sm text-muted">
            {session.user.displayName} · {session.user.username}
          </p>
        </div>
        <Badge>{isTeacher ? "教师账号" : "学生账号"}</Badge>
      </section>

      <div className="grid gap-4 lg:grid-cols-[190px_minmax(0,1fr)] lg:items-start">
        <div
          role="tablist"
          aria-label="个人中心设置"
          className="flex gap-1 overflow-x-auto rounded-xl border border-line bg-white/85 p-1.5 shadow-card lg:sticky lg:top-20 lg:flex-col lg:overflow-visible"
        >
          {[
            { id: "profile", label: "个人资料", icon: UserRound },
            { id: "security", label: "安全设置", icon: ShieldCheck },
            { id: "sessions", label: "账号操作", icon: LogOut },
          ].map((tab) => {
            const Icon = tab.icon;
            const selected = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setActiveTab(tab.id as AccountTab)}
                className={cn(
                  "flex min-h-10 shrink-0 items-center gap-2 rounded-lg px-4 text-sm transition lg:w-full",
                  selected
                    ? "bg-pine-600 font-medium text-white"
                    : "text-muted hover:bg-pine-50 hover:text-pine-700",
                )}
              >
                <Icon className="size-4" />
                {tab.label}
              </button>
            );
          })}
        </div>

        <div className="min-w-0 space-y-4">
          {activeTab === "profile" ? (
            <Card>
              <CardHeader>
                <div>
                  <p className="page-eyebrow">个人信息</p>
                  <CardTitle className="mt-1">编辑个人资料</CardTitle>
                </div>
                <UserRound className="size-5 text-pine-600" />
              </CardHeader>
              <CardContent>
                <div className="mb-5 grid gap-2 rounded-xl bg-stone-50 p-3 text-xs text-muted sm:grid-cols-3">
                  <span>
                    登录账号：<strong className="text-ink">{session.user.username}</strong>
                  </span>
                  <span>
                    身份：<strong className="text-ink">{isTeacher ? "教师" : "学生"}</strong>
                  </span>
                  {session.user.studentNumber ? (
                    <span>
                      学号：<strong className="text-ink">{session.user.studentNumber}</strong>
                    </span>
                  ) : null}
                </div>
                <form
                  className="grid gap-4 md:grid-cols-2"
                  onSubmit={(event) => {
                    event.preventDefault();
                    updateProfile.mutate();
                  }}
                >
                  <Field label="姓名" className="md:col-span-2">
                    <Input
                      value={displayName}
                      onChange={(event) => setDisplayName(event.target.value)}
                      maxLength={80}
                      required
                    />
                  </Field>
                  <Field label="邮箱">
                    <Input
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      maxLength={120}
                      placeholder="name@example.com"
                    />
                  </Field>
                  {isTeacher ? (
                    <>
                      <Field label="院系">
                        <Input
                          value={department}
                          onChange={(event) => setDepartment(event.target.value)}
                          maxLength={100}
                          placeholder="例如：新闻传播学院"
                        />
                      </Field>
                      <Field label="职称 / 职务" className="md:col-span-2">
                        <Input
                          value={professionalTitle}
                          onChange={(event) => setProfessionalTitle(event.target.value)}
                          maxLength={100}
                          placeholder="例如：副教授 / 课程负责人"
                        />
                      </Field>
                    </>
                  ) : null}
                  <div className="flex justify-end md:col-span-2">
                    <Button type="submit" disabled={updateProfile.isPending || !displayName.trim()}>
                      {updateProfile.isPending ? "保存中…" : "保存个人信息"}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          ) : null}

          {activeTab === "security" ? (
            <div className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
              <Card>
                <CardHeader>
                  <div>
                    <p className="page-eyebrow">账号安全</p>
                    <CardTitle className="mt-1">修改密码</CardTitle>
                  </div>
                  <KeyRound className="size-5 text-pine-600" />
                </CardHeader>
                <CardContent>
                  <form className="space-y-4" onSubmit={submitPassword}>
                    <Field label="当前密码">
                      <Input
                        aria-label="当前密码"
                        type="password"
                        autoComplete="current-password"
                        value={currentPassword}
                        onChange={(event) => setCurrentPassword(event.target.value)}
                        required
                      />
                    </Field>
                    <Field label="新密码" hint="至少 8 个字符。">
                      <Input
                        aria-label="新密码"
                        type="password"
                        autoComplete="new-password"
                        minLength={8}
                        maxLength={72}
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        required
                      />
                    </Field>
                    <Field label="确认新密码">
                      <Input
                        aria-label="确认新密码"
                        type="password"
                        autoComplete="new-password"
                        minLength={8}
                        maxLength={72}
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        required
                      />
                    </Field>
                    {formError ? (
                      <p className="rounded-xl bg-clay-50 px-3 py-2 text-sm text-clay-600">
                        {formError}
                      </p>
                    ) : null}
                    <Button
                      type="submit"
                      className="w-full"
                      disabled={
                        changePassword.isPending ||
                        !currentPassword ||
                        newPassword.length < 8 ||
                        !confirmPassword
                      }
                    >
                      {changePassword.isPending ? "正在更新…" : "更新密码"}
                    </Button>
                  </form>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <div>
                    <p className="page-eyebrow">偏好设置</p>
                    <CardTitle className="mt-1">显示与提醒</CardTitle>
                  </div>
                  <SlidersHorizontal className="size-5 text-pine-600" />
                </CardHeader>
                <CardContent className="space-y-3">
                  <label className="flex items-center justify-between gap-4 rounded-xl bg-stone-50 p-3 text-sm">
                    <span>
                      <strong className="block">减少界面动效</strong>
                      <small className="text-xs text-muted">关闭过渡和动画效果。</small>
                    </span>
                    <input
                      type="checkbox"
                      checked={reduceMotion}
                      onChange={(event) => setReduceMotion(event.target.checked)}
                      className="size-4 accent-pine-600"
                    />
                  </label>
                  <label className="flex items-center justify-between gap-4 rounded-xl bg-stone-50 p-3 text-sm">
                    <span>
                      <strong className="block">显示操作提示</strong>
                      <small className="text-xs text-muted">保存或提交后显示结果。</small>
                    </span>
                    <input
                      type="checkbox"
                      checked={noticesEnabled}
                      onChange={(event) => setNoticesEnabled(event.target.checked)}
                      className="size-4 accent-pine-600"
                    />
                  </label>
                  {isTeacher ? (
                    <Button asChild variant="secondary" className="w-full">
                      <Link to="/teacher/ai-settings">打开 AI 接口设置</Link>
                    </Button>
                  ) : null}
                </CardContent>
              </Card>
            </div>
          ) : null}

          {activeTab === "sessions" ? (
            <Card>
              <CardHeader>
                <div>
                  <p className="page-eyebrow">账号操作</p>
                  <CardTitle className="mt-1">会话与账号</CardTitle>
                </div>
                <LogOut className="size-5 text-pine-600" />
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid gap-2 sm:grid-cols-2">
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={logout.isPending}
                    onClick={() => logout.mutate("switch")}
                  >
                    <Repeat2 className="size-4" />
                    切换账号
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={logout.isPending}
                    onClick={() => logout.mutate("logout")}
                  >
                    <LogOut className="size-4" />
                    退出登录
                  </Button>
                </div>
                <p className="rounded-xl bg-stone-50 p-3 text-xs leading-6 text-muted">
                  切换账号会安全退出当前会话，浏览器不会保存账号或密码。
                </p>
                <div className="border-t border-line pt-4">
                  <p className="text-xs leading-6 text-muted">
                    注销后无法再次登录，教学和审核历史会继续保留。
                  </p>
                  <Button
                    type="button"
                    variant="destructive"
                    className="mt-3"
                    onClick={() => setDeactivateOpen(true)}
                  >
                    <UserX className="size-4" />
                    注销账号
                  </Button>
                </div>
              </CardContent>
            </Card>
          ) : null}
        </div>
      </div>

      <Dialog
        open={deactivateOpen}
        onOpenChange={(open) => {
          setDeactivateOpen(open);
          if (!open) setDeactivatePassword("");
        }}
        title="注销账号"
        description="注销后账号将无法登录，历史数据仍会保留用于教学记录和审核追溯。"
      >
        <div className="space-y-4">
          <Field label="当前密码" hint="注销前需要验证当前密码。">
            <Input
              type="password"
              autoComplete="current-password"
              value={deactivatePassword}
              onChange={(event) => setDeactivatePassword(event.target.value)}
              required
            />
          </Field>
          <p className="rounded-xl bg-amber-50 p-3 text-xs leading-6 text-amber-700">
            教师拥有未解散课堂时不能注销；学生注销后会从当前课堂成员列表移除。
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setDeactivateOpen(false)}>
              取消
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={deactivateAccount.isPending || !deactivatePassword}
              onClick={() => deactivateAccount.mutate()}
            >
              {deactivateAccount.isPending ? "注销中…" : "确认注销"}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
