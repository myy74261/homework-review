import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Bot, CheckCircle2, PlugZap } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input, Select } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

export function AiSettingsPage() {
  const notify = useAppStore((state) => state.notify);
  const queryClient = useQueryClient();
  const [provider, setProvider] = useState("");
  const [model, setModel] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [initialized, setInitialized] = useState(false);
  const settings = useQuery({
    queryKey: ["ai-settings"],
    queryFn: api.getAiSettings,
  });

  useEffect(() => {
    if (!settings.data || initialized) return;
    setProvider(
      settings.data.provider === "ENVIRONMENT"
        ? (settings.data.providers[0]?.code ?? "DEEPSEEK")
        : settings.data.provider,
    );
    setModel(settings.data.model || "");
    setInitialized(true);
  }, [initialized, settings.data]);

  const saveSettings = useMutation({
    mutationFn: () =>
      api.saveAiSettings({
        provider,
        model,
        apiKey,
      }),
    onSuccess: async (saved) => {
      setApiKey("");
      notify("AI 接口配置已加密保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["ai-settings"] });
      await queryClient.invalidateQueries({ queryKey: ["review-workspace"] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      if (saved.configured) setInitialized(false);
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const testSettings = useMutation({
    mutationFn: () =>
      api.testAiSettings({
        provider,
        model,
        apiKey,
      }),
    onSuccess: (result) => notify(result.message, result.success ? "success" : "error"),
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (settings.isLoading) return <Loading />;
  if (settings.isError) {
    return <ErrorState message={settings.error.message} retry={() => settings.refetch()} />;
  }

  const data = settings.data!;
  const canUseConfiguredKey = Boolean(data.configured) && !apiKey;

  return (
    <div className="mx-auto max-w-4xl space-y-5">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to="/teacher">
          <ArrowLeft className="size-4" />
          返回工作台
        </Link>
      </Button>

      <section>
        <p className="page-eyebrow">教师设置</p>
        <h1 className="mt-2 font-display text-4xl font-semibold tracking-tight">AI 接口设置</h1>
        <p className="mt-3 max-w-2xl text-sm leading-7 text-muted">
          配置用于作业预审和备课建议的模型服务。API Key 会加密保存，页面只显示末四位。
        </p>
      </section>

      <Card className="overflow-hidden">
        <CardHeader className="bg-gradient-to-r from-pine-50 to-white">
          <div>
            <p className="page-eyebrow">模型连接</p>
            <CardTitle className="mt-1">API 服务配置</CardTitle>
          </div>
          <Bot className="size-6 text-pine-600" />
        </CardHeader>
        <CardContent className="space-y-5 pt-5">
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <Badge tone={data.configured ? "default" : "warning"}>
              {data.configured ? "已接入" : "未接入"}
            </Badge>
            {data.configured ? (
              <span className="flex items-center gap-1 text-pine-700">
                <CheckCircle2 className="size-3.5" />
                {data.providerName} · {data.model} · {data.maskedKey}
              </span>
            ) : (
              <span className="text-muted">填写接口信息后即可使用 AI 预审。</span>
            )}
            {data.configured && data.source === "SERVER" ? (
              <span className="text-muted">当前使用服务器环境变量配置</span>
            ) : null}
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <Field label="模型服务">
              <Select
                value={provider}
                onChange={(event) => {
                  const code = event.target.value;
                  setProvider(code);
                  const option = data.providers.find((item) => item.code === code);
                  if (option) setModel(option.defaultModel);
                }}
              >
                {data.providers.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="模型名称">
              <Input
                value={model}
                onChange={(event) => setModel(event.target.value)}
                placeholder="例如 deepseek-chat"
              />
            </Field>
            <Field
              label="API Key"
              className="md:col-span-2"
              hint={
                data.configured
                  ? "留空表示继续使用当前密钥；填写新值会覆盖旧密钥。"
                  : "首次接入必须填写。密钥不会返回浏览器明文。"
              }
            >
              <Input
                type="password"
                autoComplete="new-password"
                value={apiKey}
                onChange={(event) => setApiKey(event.target.value)}
                placeholder={data.configured ? "留空保持不变" : "输入 API Key"}
              />
            </Field>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              variant="secondary"
              disabled={
                testSettings.isPending || !provider || !model || (!apiKey && !data.configured)
              }
              onClick={() => testSettings.mutate()}
            >
              <PlugZap className="size-4" />
              {testSettings.isPending ? "测试中…" : "测试连接"}
            </Button>
            <Button
              disabled={
                saveSettings.isPending || !provider || !model || (!apiKey && !data.configured)
              }
              onClick={() => saveSettings.mutate()}
            >
              {saveSettings.isPending ? "保存中…" : "加密保存配置"}
            </Button>
          </div>

          <p className="rounded-xl bg-stone-50 p-3 text-xs leading-6 text-muted">
            服务地址由系统固定。测试连接会发送一条简短的“回复 OK”请求；AI 建议仍需教师确认。
          </p>
          {canUseConfiguredKey ? null : (
            <p className="text-xs text-muted">首次配置时请填写 API Key 后再测试或保存。</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
