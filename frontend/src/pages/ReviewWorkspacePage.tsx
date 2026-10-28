import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Bot,
  Check,
  FileText,
  Flag,
  History,
  Save,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "@/lib/api";
import {
  aiUsageLabels,
  formatDate,
  formatFileSize,
  formatLabels,
  integrityStatusLabels,
} from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, Select, Textarea } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

export function ReviewWorkspacePage() {
  const { submissionId } = useParams();
  const id = Number(submissionId);
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const workspace = useQuery({
    queryKey: ["review-workspace", id],
    queryFn: () => api.reviewWorkspace(id),
    enabled: Number.isFinite(id),
  });

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [revision, setRevision] = useState(0);
  const [initialized, setInitialized] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [saveLabel, setSaveLabel] = useState("已保存");
  const [scoreValues, setScoreValues] = useState<Record<number, string>>({});
  const [flagOpen, setFlagOpen] = useState(false);
  const [flagForm, setFlagForm] = useState({
    category: "引用来源",
    severity: "MEDIUM",
    evidence: "",
    reason: "",
  });
  const saveTimer = useRef<number | null>(null);
  const saveNowRef = useRef<() => void>(() => undefined);

  useEffect(() => {
    if (!workspace.data || initialized) return;
    setTitle(workspace.data.review.title);
    setContent(workspace.data.review.content);
    setRevision(workspace.data.review.revision);
    setScoreValues(
      Object.fromEntries(
        workspace.data.scores.map((score) => [
          score.id,
          score.finalPoints == null ? "" : String(score.finalPoints),
        ]),
      ),
    );
    setInitialized(true);
  }, [initialized, workspace.data]);

  const saveReview = useMutation({
    mutationFn: () => api.saveReview(id, { title, content, baseRevision: revision }),
    onSuccess: (saved) => {
      setRevision(saved.revision);
      setDirty(false);
      setSaveLabel(`已保存 · ${formatDate(saved.updatedAt)}`);
    },
    onError: async (error: Error) => {
      setSaveLabel("保存失败");
      notify(error.message, "error");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
  });
  saveNowRef.current = () => saveReview.mutate();

  useEffect(() => {
    if (!initialized || !dirty) return;
    setSaveLabel("等待保存…");
    if (saveTimer.current) window.clearTimeout(saveTimer.current);
    saveTimer.current = window.setTimeout(() => saveNowRef.current(), 900);
    return () => {
      if (saveTimer.current) window.clearTimeout(saveTimer.current);
    };
  }, [content, dirty, initialized, title]);

  const complete = useMutation({
    mutationFn: async () => {
      if (dirty) {
        await api.saveReview(id, { title, content, baseRevision: revision });
      }
      await api.completeReview(id);
    },
    onSuccess: async () => {
      notify("反馈已发布，学生端现在可以看到结果。", "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const saveScores = useMutation({
    mutationFn: () =>
      api.saveScores(
        id,
        Object.fromEntries(
          Object.entries(scoreValues)
            .filter(([, value]) => value !== "")
            .map(([scoreId, value]) => [Number(scoreId), Number(value)]),
        ),
      ),
    onSuccess: async () => {
      notify("最终评分已保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const createFlag = useMutation({
    mutationFn: () => api.createIntegrityFlag(id, flagForm),
    onSuccess: async () => {
      notify("可疑线索已记录为待核实。", "success");
      setFlagForm({ category: "引用来源", severity: "MEDIUM", evidence: "", reason: "" });
      setFlagOpen(false);
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const resolveFlag = useMutation({
    mutationFn: ({ flagId, status }: { flagId: number; status: "CONFIRMED" | "CLEARED" }) =>
      api.resolveIntegrityFlag(flagId, status),
    onSuccess: async () => {
      notify("诚信核验状态已更新。", "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const createVersion = useMutation({
    mutationFn: () => api.saveVersion(id),
    onSuccess: async (version) => {
      notify(`已保存反馈版本 V${version.versionNumber}。`, "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const generateAiReview = useMutation({
    mutationFn: () => api.generateAiReview(id),
    onSuccess: async (run) => {
      notify(`预审完成，生成 ${run.suggestions.length} 条建议。`, "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const adoptAiSuggestion = useMutation({
    mutationFn: (suggestionId: number) => api.adoptAiSuggestion(suggestionId),
    onSuccess: async (suggestion) => {
      const separator = content.trim() ? "\n\n" : "";
      setContent(`${content}${separator}【${suggestion.rubricName}】${suggestion.suggestion}`);
      setDirty(true);
      notify("已加入教师意见，请核对后保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const dismissAiSuggestion = useMutation({
    mutationFn: (suggestionId: number) => api.dismissAiSuggestion(suggestionId),
    onSuccess: async () => {
      notify("已忽略该建议，记录已保留。", "info");
      await queryClient.invalidateQueries({ queryKey: ["review-workspace", id] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const computedTotal = useMemo(() => {
    const values = Object.values(scoreValues).filter((value) => value !== "");
    if (values.length !== workspace.data?.scores.length) return null;
    return values.reduce((sum, value) => sum + Number(value), 0);
  }, [scoreValues, workspace.data?.scores.length]);

  if (workspace.isLoading) return <Loading label="正在打开审核工作台…" />;
  if (workspace.isError) {
    return <ErrorState message={workspace.error.message} retry={() => workspace.refetch()} />;
  }

  const data = workspace.data!;
  const reviewed = data.submission.reviewStatus === "REVIEWED";

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild variant="ghost" size="sm" className="-ml-2">
          <Link to="/teacher">
            <ArrowLeft className="size-4" />
            返回课堂
          </Link>
        </Button>
        <div className="flex items-center gap-2">
          <Badge tone={reviewed ? "default" : "warning"}>
            {reviewed ? "反馈已发布" : "审核中"}
          </Badge>
          <Button
            variant="secondary"
            size="sm"
            disabled={createVersion.isPending}
            onClick={() => createVersion.mutate()}
          >
            <History className="size-4" />
            保存版本 · {data.review.versionCount}
          </Button>
          <Button
            size="sm"
            disabled={reviewed || complete.isPending}
            onClick={() => complete.mutate()}
          >
            <Check className="size-4" />
            {reviewed ? "已发布" : complete.isPending ? "发布中…" : "发布反馈"}
          </Button>
        </div>
      </div>

      <section className="rounded-[26px] border border-line bg-white/80 p-5 shadow-card">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="page-eyebrow">当前审核</p>
            <h1 className="mt-2 font-display text-3xl font-semibold">{data.assignment.title}</h1>
            <p className="mt-2 text-sm text-muted">
              {data.submission.studentName} · {data.submission.studentNumber} · 第{" "}
              {data.submission.attemptNumber} 次提交
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge>{formatLabels[data.assignment.submissionFormat]}</Badge>
            <Badge tone="neutral">{data.submission.wordCount} 字</Badge>
            <Badge tone="neutral">截止 {formatDate(data.assignment.dueAt)}</Badge>
          </div>
        </div>
      </section>

      <div className="grid gap-4 xl:grid-cols-[minmax(320px,0.76fr)_minmax(440px,1.15fr)_minmax(330px,0.82fr)]">
        <Card className="h-fit">
          <CardHeader>
            <div>
              <p className="page-eyebrow">学生提交</p>
              <CardTitle className="mt-1">提交内容</CardTitle>
            </div>
            <FileText className="size-5 text-pine-600" />
          </CardHeader>
          <CardContent>
            <div className="max-h-[44vh] overflow-auto rounded-xl bg-stone-50 p-4">
              <p className="prose-copy">{data.submission.content}</p>
            </div>
            {data.attachments.length ? (
              <div className="mt-4 space-y-2">
                {data.attachments.map((attachment) => (
                  <a
                    key={attachment.id}
                    href={attachment.url}
                    target="_blank"
                    rel="noopener"
                    className="flex items-center justify-between gap-3 rounded-xl border border-line p-3 text-sm hover:border-pine-500/40"
                  >
                    <span className="truncate">{attachment.originalName}</span>
                    <span className="shrink-0 text-xs text-muted">
                      {formatFileSize(attachment.fileSize)}
                    </span>
                  </a>
                ))}
              </div>
            ) : null}
            <div className="mt-4 rounded-xl bg-pine-50 p-3 text-xs leading-6 text-pine-700">
              <strong>AI 使用：</strong>
              {aiUsageLabels[data.submission.aiUsage] ?? data.submission.aiUsage}
              <br />
              <strong>来源声明：</strong>
              {data.submission.sourceDeclaration ?? "未提交"}
              <br />
              <strong>协作说明：</strong>
              {data.submission.collaborationDeclaration ?? "无"}
            </div>
          </CardContent>
        </Card>

        <Card className="flex min-h-[620px] flex-col">
          <CardHeader>
            <div>
              <p className="page-eyebrow">教师反馈</p>
              <CardTitle className="mt-1">审核意见</CardTitle>
            </div>
            <span className="text-xs text-muted">{saveLabel}</span>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col gap-3">
            <Input
              aria-label="审核意见标题"
              value={title}
              onChange={(event) => {
                setTitle(event.target.value);
                setDirty(true);
              }}
              className="border-0 px-0 font-display text-xl font-semibold shadow-none focus:ring-0"
            />
            <Textarea
              aria-label="教师审核意见正文"
              value={content}
              onChange={(event) => {
                setContent(event.target.value);
                setDirty(true);
              }}
              className="min-h-[480px] flex-1 border-0 px-0 font-display text-base leading-8 shadow-none focus:ring-0"
            />
            <div className="flex items-center justify-between border-t border-line pt-3 text-xs text-muted">
              <span>{content.replace(/\s/g, "").length} 字</span>
              <Button
                variant="secondary"
                size="sm"
                disabled={!dirty || saveReview.isPending}
                onClick={() => saveReview.mutate()}
              >
                <Save className="size-3.5" />
                {saveReview.isPending ? "保存中…" : "立即保存"}
              </Button>
            </div>
          </CardContent>
        </Card>

        <div className="space-y-4">
          <Card className="border-pine-500/25 bg-gradient-to-br from-white to-pine-50/50">
            <CardHeader>
              <div>
                <p className="page-eyebrow">AI 预审</p>
                <CardTitle className="mt-1">预审建议</CardTitle>
              </div>
              <Bot className="size-5 text-pine-600" />
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-700">
                建议仅供参考。是否采纳、评分和发布反馈均由教师决定。
              </p>
              {!data.aiConfigured ? (
                <div className="rounded-xl border border-dashed border-line p-3 text-xs leading-6 text-muted">
                  当前未配置 AI 接口。
                  <Link to="/teacher/ai-settings" className="ml-1 font-medium text-pine-700">
                    前往 AI 设置
                  </Link>
                </div>
              ) : (
                <Button
                  size="sm"
                  className="w-full"
                  disabled={generateAiReview.isPending || reviewed}
                  onClick={() => generateAiReview.mutate()}
                >
                  <Bot className="size-3.5" />
                  {generateAiReview.isPending
                    ? "正在预审…"
                    : data.aiReviews.length
                      ? "重新预审"
                      : "开始预审"}
                </Button>
              )}

              {data.aiReviews.slice(0, 3).map((run, runIndex) => (
                <section key={run.id} className="rounded-xl border border-line bg-white p-3">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <strong className="text-xs">
                        {runIndex === 0 ? "最新预审" : "历史预审"}
                      </strong>
                      <p className="mt-1 text-[11px] text-muted">
                        {run.model} · {formatDate(run.createdAt)}
                      </p>
                    </div>
                    <Badge tone="warning">待核实</Badge>
                  </div>
                  <p className="mt-2 text-xs leading-5 text-stone-600">{run.summary}</p>
                  <div className="mt-3 space-y-2">
                    {run.suggestions.map((suggestion) => (
                      <article
                        key={suggestion.id}
                        className="rounded-lg border border-line bg-stone-50 p-2.5"
                      >
                        <div className="flex items-center justify-between gap-2">
                          <strong className="text-xs">{suggestion.rubricName}</strong>
                          <span className="text-[10px] text-muted">
                            {suggestion.status === "PENDING"
                              ? "待教师决定"
                              : suggestion.status === "ADOPTED"
                                ? "已采纳"
                                : suggestion.status === "DISMISSED"
                                  ? "已忽略"
                                  : "已被新预审替代"}
                          </span>
                        </div>
                        <blockquote className="mt-2 border-l-2 border-clay-500/40 pl-2 text-[11px] leading-5 text-stone-600">
                          “{suggestion.evidenceQuote}”
                        </blockquote>
                        <p className="mt-2 text-xs leading-5 text-stone-700">
                          {suggestion.suggestion}
                        </p>
                        {suggestion.suggestedPoints != null ? (
                          <span className="mt-2 inline-block rounded-md bg-white px-2 py-1 text-[10px] text-pine-700">
                            参考分：{suggestion.suggestedPoints}
                          </span>
                        ) : null}
                        {suggestion.status === "PENDING" && !reviewed ? (
                          <div className="mt-2 flex gap-2">
                            <Button
                              size="sm"
                              disabled={adoptAiSuggestion.isPending}
                              onClick={() => adoptAiSuggestion.mutate(suggestion.id)}
                            >
                              采用建议
                            </Button>
                            <Button
                              size="sm"
                              variant="secondary"
                              disabled={dismissAiSuggestion.isPending}
                              onClick={() => dismissAiSuggestion.mutate(suggestion.id)}
                            >
                              忽略
                            </Button>
                          </div>
                        ) : null}
                      </article>
                    ))}
                  </div>
                </section>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">评分</p>
                <CardTitle className="mt-1">评分建议</CardTitle>
              </div>
              <Sparkles className="size-5 text-pine-600" />
            </CardHeader>
            <CardContent className="space-y-3">
              <ul className="space-y-1 rounded-xl bg-stone-50 p-3 text-xs leading-5 text-muted">
                {data.reviewChecklist.map((item) => (
                  <li key={item} className="flex gap-2">
                    <span className="mt-2 size-1 shrink-0 rounded-full bg-pine-500" />
                    {item}
                  </li>
                ))}
              </ul>
              {data.scores.map((score) => (
                <div key={score.id} className="rounded-xl border border-line p-3">
                  <div className="flex items-center justify-between gap-3">
                    <strong className="text-sm">{score.rubricName}</strong>
                    <span className="text-xs text-pine-700">
                      建议 {score.suggestedPoints} / {score.maxPoints}
                    </span>
                  </div>
                  <p className="mt-2 text-xs leading-5 text-muted">{score.suggestion}</p>
                  <div className="mt-3 flex items-center justify-between gap-3">
                    <span className="text-xs text-muted">教师最终分</span>
                    <Input
                      type="number"
                      min={0}
                      max={score.maxPoints}
                      value={scoreValues[score.id] ?? ""}
                      onChange={(event) =>
                        setScoreValues((values) => ({
                          ...values,
                          [score.id]: event.target.value,
                        }))
                      }
                      className="h-8 w-24 text-right"
                    />
                  </div>
                </div>
              ))}
              <div className="flex items-center justify-between border-t border-line pt-3 text-xs">
                <span className="text-muted">
                  {computedTotal == null ? "填写全部维度后生成总分" : `当前总分 ${computedTotal}`}
                </span>
                <Button
                  size="sm"
                  disabled={saveScores.isPending}
                  onClick={() => saveScores.mutate()}
                >
                  保存评分
                </Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">学术诚信</p>
                <CardTitle className="mt-1">诚信核验</CardTitle>
              </div>
              <ShieldCheck className="size-5 text-clay-500" />
            </CardHeader>
            <CardContent className="space-y-3">
              {data.integrityFlags.map((flag) => (
                <div key={flag.id} className="rounded-xl border border-line p-3">
                  <div className="flex items-center justify-between gap-2">
                    <strong className="text-sm">{flag.category}</strong>
                    <Badge tone={flag.status === "CONFIRMED" ? "danger" : "warning"}>
                      {integrityStatusLabels[flag.status]}
                    </Badge>
                  </div>
                  <p className="mt-2 text-xs leading-5 text-stone-600">证据：{flag.evidence}</p>
                  <p className="mt-1 text-xs leading-5 text-muted">{flag.reason}</p>
                  {flag.status === "SUSPECTED" ? (
                    <div className="mt-2 flex gap-2">
                      <Button
                        size="sm"
                        onClick={() => resolveFlag.mutate({ flagId: flag.id, status: "CONFIRMED" })}
                      >
                        确认
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => resolveFlag.mutate({ flagId: flag.id, status: "CLEARED" })}
                      >
                        排除
                      </Button>
                    </div>
                  ) : null}
                </div>
              ))}
              {!flagOpen ? (
                <Button variant="secondary" size="sm" onClick={() => setFlagOpen(true)}>
                  <Flag className="size-3.5" />
                  记录可疑线索
                </Button>
              ) : (
                <div className="grid gap-2 rounded-xl border border-dashed border-line p-3">
                  <Select
                    value={flagForm.category}
                    onChange={(event) =>
                      setFlagForm((form) => ({ ...form, category: event.target.value }))
                    }
                  >
                    <option>引用来源</option>
                    <option>署名或协作</option>
                    <option>AI 使用声明</option>
                    <option>内容重复</option>
                  </Select>
                  <Textarea
                    rows={2}
                    value={flagForm.evidence}
                    onChange={(event) =>
                      setFlagForm((form) => ({ ...form, evidence: event.target.value }))
                    }
                    placeholder="可疑原文或具体位置"
                  />
                  <Textarea
                    rows={2}
                    value={flagForm.reason}
                    onChange={(event) =>
                      setFlagForm((form) => ({ ...form, reason: event.target.value }))
                    }
                    placeholder="核验依据"
                  />
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      disabled={createFlag.isPending}
                      onClick={() => createFlag.mutate()}
                    >
                      保存
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => setFlagOpen(false)}>
                      取消
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
