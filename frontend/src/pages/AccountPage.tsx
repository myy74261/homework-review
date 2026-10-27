import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Bot, CheckCircle2, KeyRound, PlugZap, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input, Select } from "@/components/ui/field";

export function AccountPage() {
  const session = useAppStore((state) => state.session)!;
  const notify = useAppStore((state) => state.notify);
  const queryClient = useQueryClient();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");
  const [aiProvider, setAiProvider] = useState("");
  const [aiModel, setAiModel] = useState("");
  const [aiApiKey, setAiApiKey] = useState("");
  const [aiInitialized, setAiInitialized] = useState(false);
  const aiSettings = useQuery({
    queryKey: ["ai-settings"],
    queryFn: api.getAiSettings,
    enabled: session.user.role === "TEACHER",
  });

  useEffect(() => {
    if (!aiSettings.data || aiInitialized) return;
    setAiProvider(
      aiSettings.data.provider === "ENVIRONMENT"
        ? (aiSettings.data.providers[0]?.code ?? "DEEPSEEK")
        : aiSettings.data.provider,
    );
    setAiModel(aiSettings.data.model || "");
    setAiInitialized(true);
  }, [aiInitialized, aiSettings.data]);

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

  const saveAiSettings = useMutation({
    mutationFn: () =>
      api.saveAiSettings({
        provider: aiProvider,
        model: aiModel,
        apiKey: aiApiKey,
      }),
    onSuccess: async (settings) => {
      setAiApiKey("");
      notify("AI 接口配置已加密保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["ai-settings"] });
      await queryClient.invalidateQueries({ queryKey: ["review-workspace"] });
      if (settings.configured) setAiInitialized(false);
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const testAiSettings = useMutation({
    mutationFn: () =>
      api.testAiSettings({
        provider: aiProvider,
        model: aiModel,
        apiKey: aiApiKey,
      }),
    onSuccess: (result) => notify(result.message, result.success ? "success" : "error"),
    onError: (error: Error) => notify(error.message, "error"),
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

      {session.user.role === "TEACHER" ? (
        <Card className="overflow-hidden">
          <CardHeader className="bg-gradient-to-r from-pine-50 to-white">
            <div>
              <p className="page-eyebrow">模型连接</p>
              <CardTitle className="mt-1">AI 接口设置</CardTitle>
              <p className="mt-1 text-xs leading-5 text-muted">
                仅教师可用。API Key 加密保存，页面只显示末四位。
              </p>
            </div>
            <Bot className="size-6 text-pine-600" />
          </CardHeader>
          <CardContent className="space-y-4 pt-5">
            {aiSettings.isError ? (
              <p className="rounded-xl bg-clay-50 p-3 text-sm text-clay-600">
                {aiSettings.error.message}
              </p>
            ) : null}
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <Badge tone={aiSettings.data?.configured ? "default" : "warning"}>
                {aiSettings.data?.configured ? "已接入" : "未接入"}
              </Badge>
              {aiSettings.data?.configured ? (
                <span className="flex items-center gap-1 text-pine-700">
                  <CheckCircle2 className="size-3.5" />
                  {aiSettings.data.providerName} · {aiSettings.data.model} ·{" "}
                  {aiSettings.data.maskedKey}
                </span>
              ) : (
                <span className="text-muted">填写接口信息后即可使用 AI 预审。</span>
              )}
              {aiSettings.data?.source === "SERVER" ? (
                <span className="text-muted">当前使用服务器环境变量配置</span>
              ) : null}
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              <Field label="模型服务">
                <Select
                  value={aiProvider}
                  onChange={(event) => {
                    const code = event.target.value;
                    setAiProvider(code);
                    const provider = aiSettings.data?.providers.find((item) => item.code === code);
                    if (provider) setAiModel(provider.defaultModel);
                  }}
                >
                  {(aiSettings.data?.providers ?? []).map((provider) => (
                    <option key={provider.code} value={provider.code}>
                      {provider.name}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="模型名称">
                <Input
                  value={aiModel}
                  onChange={(event) => setAiModel(event.target.value)}
                  placeholder="例如 deepseek-chat"
                />
              </Field>
              <Field
                label="API Key"
                className="md:col-span-2"
                hint={
                  aiSettings.data?.configured
                    ? "留空表示继续使用当前密钥；填写新值会覆盖旧密钥。"
                    : "首次接入必须填写。密钥不会返回浏览器明文。"
                }
              >
                <Input
                  type="password"
                  autoComplete="new-password"
                  value={aiApiKey}
                  onChange={(event) => setAiApiKey(event.target.value)}
                  placeholder={aiSettings.data?.configured ? "留空保持不变" : "输入 API Key"}
                />
              </Field>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button
                variant="secondary"
                disabled={
                  testAiSettings.isPending ||
                  !aiProvider ||
                  !aiModel ||
                  (!aiApiKey && !aiSettings.data?.configured)
                }
                onClick={() => testAiSettings.mutate()}
              >
                <PlugZap className="size-4" />
                {testAiSettings.isPending ? "测试中…" : "测试连接"}
              </Button>
              <Button
                disabled={
                  saveAiSettings.isPending ||
                  !aiProvider ||
                  !aiModel ||
                  (!aiApiKey && !aiSettings.data?.configured)
                }
                onClick={() => saveAiSettings.mutate()}
              >
                {saveAiSettings.isPending ? "保存中…" : "加密保存配置"}
              </Button>
            </div>
            <p className="rounded-xl bg-stone-50 p-3 text-xs leading-5 text-muted">
              服务地址由系统固定。测试连接会发送一条简短的“回复 OK”请求。
            </p>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
