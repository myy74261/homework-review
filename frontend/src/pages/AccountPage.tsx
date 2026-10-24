import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, KeyRound, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input } from "@/components/ui/field";

export function AccountPage() {
  const session = useAppStore((state) => state.session)!;
  const notify = useAppStore((state) => state.notify);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");

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

  function submit(event: React.FormEvent) {
    event.preventDefault();
    setFormError("");
    if (newPassword !== confirmPassword) {
      setFormError("两次输入的新密码不一致");
      return;
    }
    changePassword.mutate();
  }

  return (
    <div className="mx-auto max-w-4xl space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to={session.homePath.replace("/app", "")}>
          <ArrowLeft className="size-4" />
          返回工作台
        </Link>
      </Button>

      <section>
        <p className="page-eyebrow">账号安全</p>
        <h1 className="mt-2 font-display text-4xl font-semibold tracking-tight">个人中心</h1>
        <p className="mt-3 text-sm leading-7 text-muted">
          查看当前账号并自行更新密码。旧密码不正确时不会修改任何数据。
        </p>
      </section>

      <div className="grid gap-4 md:grid-cols-[300px_minmax(0,1fr)]">
        <Card className="h-fit">
          <CardHeader>
            <div>
              <p className="page-eyebrow">当前账号</p>
              <CardTitle className="mt-1">{session.user.displayName}</CardTitle>
            </div>
            <Badge>{session.user.role === "TEACHER" ? "教师" : "学生"}</Badge>
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
              <p className="page-eyebrow">修改密码</p>
              <CardTitle className="mt-1">设置新的登录密码</CardTitle>
            </div>
            <KeyRound className="size-5 text-pine-600" />
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={submit}>
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
              <Field label="新密码" hint="至少 8 个字符，最多 72 个字符。">
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
                <p className="rounded-xl bg-clay-50 px-3 py-2 text-sm text-clay-600">{formError}</p>
              ) : null}
              <Button
                type="submit"
                size="lg"
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
      </div>
    </div>
  );
}
