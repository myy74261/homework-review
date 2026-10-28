import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, KeyRound, ShieldCheck, UserRound } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input } from "@/components/ui/field";

export function AccountPage() {
  const session = useAppStore((state) => state.session)!;
  const setSession = useAppStore((state) => state.setSession);
  const notify = useAppStore((state) => state.notify);
  const queryClient = useQueryClient();
  const isTeacher = session.user.role === "TEACHER";

  const [displayName, setDisplayName] = useState(session.user.displayName);
  const [email, setEmail] = useState(session.user.email ?? "");
  const [department, setDepartment] = useState(session.user.department ?? "");
  const [professionalTitle, setProfessionalTitle] = useState(session.user.professionalTitle ?? "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");

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
    <div className="mx-auto max-w-5xl space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to={session.homePath.replace("/app", "")}>
          <ArrowLeft className="size-4" />
          返回工作台
        </Link>
      </Button>

      <section>
        <p className="page-eyebrow">账号与资料</p>
        <h1 className="mt-2 font-display text-4xl font-semibold tracking-tight">个人中心</h1>
        <p className="mt-3 text-sm leading-7 text-muted">
          更新个人资料或登录密码。登录账号和角色不能在这里修改。
        </p>
      </section>

      <div className="grid gap-4 md:grid-cols-[300px_minmax(0,1fr)]">
        <Card className="h-fit">
          <CardHeader>
            <div>
              <p className="page-eyebrow">当前账号</p>
              <CardTitle className="mt-1">{session.user.displayName}</CardTitle>
            </div>
            <Badge>{isTeacher ? "教师" : "学生"}</Badge>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex justify-between gap-4 border-b border-line pb-3">
              <span className="text-muted">登录账号</span>
              <strong>{session.user.username}</strong>
            </div>
            {session.user.studentNumber ? (
              <div className="flex justify-between gap-4 border-b border-line pb-3">
                <span className="text-muted">学号</span>
                <strong>{session.user.studentNumber}</strong>
              </div>
            ) : null}
            <div className="rounded-xl bg-pine-50 p-3 text-xs leading-6 text-pine-700">
              <ShieldCheck className="mb-1 size-4" />
              密码使用 BCrypt 哈希保存，网站不会记录或返回密码明文。
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <p className="page-eyebrow">个人信息</p>
              <CardTitle className="mt-1">编辑个人资料</CardTitle>
            </div>
            <UserRound className="size-5 text-pine-600" />
          </CardHeader>
          <CardContent>
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
      </div>

      <Card>
        <CardHeader>
          <div>
            <p className="page-eyebrow">账号安全</p>
            <CardTitle className="mt-1">修改密码</CardTitle>
          </div>
          <KeyRound className="size-5 text-pine-600" />
        </CardHeader>
        <CardContent>
          <form className="grid gap-4 md:grid-cols-3" onSubmit={submitPassword}>
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
              <p className="rounded-xl bg-clay-50 px-3 py-2 text-sm text-clay-600 md:col-span-3">
                {formError}
              </p>
            ) : null}
            <div className="flex justify-end md:col-span-3">
              <Button
                type="submit"
                disabled={
                  changePassword.isPending ||
                  !currentPassword ||
                  newPassword.length < 8 ||
                  !confirmPassword
                }
              >
                {changePassword.isPending ? "正在更新…" : "更新密码"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
