import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, Download, FileUp } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "@/lib/api";
import {
  aiPolicyLabels,
  aiUsageLabels,
  appealStatusLabels,
  formatDate,
  formatFileSize,
  formatLabels,
} from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, Input, Select, Textarea } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

const MAX_FILES = 8;
const MAX_FILE_BYTES = 25 * 1024 * 1024;
const ALLOWED_EXTENSIONS = new Set([
  "pdf",
  "doc",
  "docx",
  "ppt",
  "pptx",
  "xls",
  "xlsx",
  "csv",
  "rtf",
  "odt",
  "ods",
  "odp",
  "txt",
  "md",
  "java",
  "py",
  "js",
  "ts",
  "c",
  "cpp",
  "h",
  "cs",
  "go",
  "rs",
  "sql",
  "json",
  "xml",
  "yml",
  "yaml",
  "ipynb",
  "html",
  "css",
  "zip",
  "rar",
  "7z",
  "png",
  "jpg",
  "jpeg",
  "webp",
]);

export function StudentAssignmentPage() {
  const { assignmentId } = useParams();
  const id = Number(assignmentId);
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const formRef = useRef<HTMLFormElement>(null);
  const assignment = useQuery({
    queryKey: ["student-assignment", id],
    queryFn: () => api.studentAssignment(id),
    enabled: Number.isFinite(id),
  });

  const [content, setContent] = useState("");
  const [aiUsage, setAiUsage] = useState("NONE");
  const [aiTools, setAiTools] = useState("");
  const [aiScope, setAiScope] = useState("");
  const [sourceDeclaration, setSourceDeclaration] = useState("");
  const [collaborationDeclaration, setCollaborationDeclaration] = useState("");
  const [integrityConfirmed, setIntegrityConfirmed] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [appealReason, setAppealReason] = useState("");
  const [requestedResolution, setRequestedResolution] = useState("");
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (!assignment.data || initialized) return;
    const latest = assignment.data.latestSubmission;
    if (latest) {
      setContent(latest.content);
      setAiUsage(latest.aiUsage);
      setAiTools(latest.aiTools ?? "");
      setAiScope(latest.aiScope ?? "");
      setSourceDeclaration(latest.sourceDeclaration ?? "");
      setCollaborationDeclaration(latest.collaborationDeclaration ?? "");
      setIntegrityConfirmed(latest.integrityConfirmed);
    }
    setInitialized(true);
  }, [assignment.data, initialized]);

  const submit = useMutation({
    mutationFn: () => {
      const form = new FormData();
      form.set("content", content);
      form.set("aiUsage", aiUsage);
      form.set("aiTools", aiTools);
      form.set("aiScope", aiScope);
      form.set("sourceDeclaration", sourceDeclaration);
      form.set("collaborationDeclaration", collaborationDeclaration);
      if (integrityConfirmed) form.set("integrityConfirmed", "true");
      files.forEach((file) => form.append("files", file));
      return api.submitAssignment(id, form);
    },
    onSuccess: async (result) => {
      notify(result.message, "success");
      setFiles([]);
      formRef.current?.reset();
      await queryClient.invalidateQueries({ queryKey: ["student-assignment", id] });
      await queryClient.invalidateQueries({ queryKey: ["student-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const appeal = useMutation({
    mutationFn: () =>
      api.submitAppeal(assignment.data!.latestSubmission!.id, {
        reason: appealReason,
        requestedResolution,
      }),
    onSuccess: async () => {
      notify("申诉已提交，等待教师复核。", "success");
      setAppealReason("");
      setRequestedResolution("");
      await queryClient.invalidateQueries({ queryKey: ["student-assignment", id] });
      await queryClient.invalidateQueries({ queryKey: ["student-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  function validateFiles() {
    if (files.length > MAX_FILES) {
      notify(`一次最多上传 ${MAX_FILES} 个附件。`, "error");
      return false;
    }
    const unsupported = files.find((file) => {
      const extension = file.name.includes(".") ? file.name.split(".").pop()!.toLowerCase() : "";
      return !ALLOWED_EXTENSIONS.has(extension);
    });
    if (unsupported) {
      notify(`不支持附件类型：${unsupported.name}`, "error");
      return false;
    }
    const oversized = files.find((file) => file.size > MAX_FILE_BYTES);
    if (oversized) {
      notify(`附件 ${oversized.name} 超过 25 MB，请压缩后重试。`, "error");
      return false;
    }
    return true;
  }

  if (assignment.isLoading) return <Loading label="正在打开作业…" />;
  if (assignment.isError) {
    return <ErrorState message={assignment.error.message} retry={() => assignment.refetch()} />;
  }

  const data = assignment.data!;
  const latest = data.latestSubmission;
  const reviewed = latest?.reviewStatus === "REVIEWED";
  const a = data.assignment;

  return (
    <div className="space-y-4">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link to="/student">
          <ArrowLeft className="size-4" />
          返回我的作业
        </Link>
      </Button>

      <section className="rounded-[26px] border border-line bg-white/85 p-6 shadow-card">
        <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
          <div>
            <p className="page-eyebrow">{a.courseName}</p>
            <h1 className="mt-2 max-w-4xl font-display text-3xl font-semibold sm:text-4xl">
              {a.title}
            </h1>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-muted">{a.instructions}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge>{formatLabels[a.submissionFormat]}</Badge>
            <Badge tone="neutral">{aiPolicyLabels[a.aiPolicy]}</Badge>
            <Badge tone="warning">截止 {formatDate(a.dueAt)}</Badge>
          </div>
        </div>
      </section>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.05fr)_minmax(380px,0.95fr)]">
        <div className="space-y-4">
          {latest ? (
            <Card>
              <CardHeader>
                <div>
                  <p className="page-eyebrow">我的提交内容</p>
                  <CardTitle className="mt-1">第 {latest.attemptNumber} 次提交全文</CardTitle>
                </div>
                <Badge tone={reviewed ? "default" : "warning"}>
                  {reviewed ? "已反馈" : "待审核"}
                </Badge>
              </CardHeader>
              <CardContent>
                <div className="max-h-[48vh] overflow-auto rounded-xl bg-stone-50 p-4">
                  <p className="prose-copy">{latest.content}</p>
                </div>
                <div className="mt-4 grid gap-2 sm:grid-cols-2">
                  {data.attachments.map((attachment) => (
                    <a
                      key={attachment.id}
                      href={attachment.url}
                      target="_blank"
                      rel="noopener"
                      className="flex items-center justify-between rounded-xl border border-line p-3 text-sm hover:border-pine-500/40"
                    >
                      <span className="min-w-0 truncate">{attachment.originalName}</span>
                      <span className="ml-3 flex shrink-0 items-center gap-1 text-xs text-muted">
                        <Download className="size-3.5" />
                        {formatFileSize(attachment.fileSize)}
                      </span>
                    </a>
                  ))}
                </div>
                <div className="mt-4 rounded-xl bg-pine-50 p-3 text-xs leading-6 text-pine-700">
                  <strong>提交于：</strong>
                  {formatDate(latest.submittedAt)}
                  <br />
                  <strong>AI 使用：</strong>
                  {aiUsageLabels[latest.aiUsage] ?? latest.aiUsage}
                  <br />
                  <strong>来源声明：</strong>
                  {latest.sourceDeclaration ?? "未填写"}
                </div>
              </CardContent>
            </Card>
          ) : null}

          {reviewed && data.review ? (
            <Card className="border-pine-500/20">
              <CardHeader>
                <div>
                  <p className="page-eyebrow">教师最终反馈</p>
                  <CardTitle className="mt-1">{data.review.title}</CardTitle>
                </div>
                <CheckCircle2 className="size-5 text-pine-600" />
              </CardHeader>
              <CardContent>
                <p className="prose-copy">{data.review.content}</p>
                {data.scores.length ? (
                  <div className="mt-5 rounded-xl border border-pine-500/15 bg-pine-50 p-4">
                    <div className="flex items-center justify-between">
                      <strong className="text-sm">最终评分</strong>
                      <span className="font-display text-2xl text-pine-700">
                        {data.finalScoreTotal ?? "—"} / 100
                      </span>
                    </div>
                    <div className="mt-3 space-y-1 border-t border-pine-500/10 pt-3 text-sm">
                      {data.scores.map((score) => (
                        <div key={score.id} className="flex justify-between gap-3 py-1">
                          <span>{score.rubricName}</span>
                          <strong>
                            {score.finalPoints ?? "未评分"} / {score.maxPoints}
                          </strong>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : null}
                {data.confirmedIntegrityFlags.map((flag) => (
                  <div
                    key={flag.id}
                    className="mt-3 rounded-xl bg-clay-50 p-3 text-xs leading-6 text-clay-600"
                  >
                    <strong>{flag.category}</strong>
                    <br />
                    证据：{flag.evidence}
                    <br />
                    说明：{flag.reason}
                  </div>
                ))}
              </CardContent>
            </Card>
          ) : null}

          {reviewed ? (
            <Card>
              <CardHeader>
                <div>
                  <p className="page-eyebrow">反馈复核</p>
                  <CardTitle className="mt-1">我的申诉</CardTitle>
                </div>
                <Badge tone={a.appealAllowed ? "default" : "neutral"}>
                  {a.appealAllowed ? "已开放申诉" : "未开放申诉"}
                </Badge>
              </CardHeader>
              <CardContent className="space-y-3">
                {data.appeals.map((item) => (
                  <div key={item.id} className="rounded-xl border border-line p-3">
                    <div className="flex justify-between gap-3">
                      <strong className="text-sm">{appealStatusLabels[item.status]}</strong>
                      <span className="text-xs text-muted">{formatDate(item.createdAt)}</span>
                    </div>
                    <p className="mt-2 text-sm leading-6 text-stone-700">{item.reason}</p>
                    {item.teacherResponse ? (
                      <p className="mt-2 rounded-lg bg-pine-50 p-2 text-xs leading-5 text-pine-700">
                        教师复核：{item.teacherResponse}
                      </p>
                    ) : null}
                  </div>
                ))}
                {a.appealAllowed && !data.activeAppeal ? (
                  <form
                    className="space-y-3"
                    onSubmit={(event) => {
                      event.preventDefault();
                      appeal.mutate();
                    }}
                  >
                    <Field label="申诉理由">
                      <Textarea
                        rows={3}
                        minLength={10}
                        value={appealReason}
                        onChange={(event) => setAppealReason(event.target.value)}
                        placeholder="具体说明对评分、反馈或诚信结论的异议"
                        required
                      />
                    </Field>
                    <Field label="希望教师如何处理">
                      <Textarea
                        rows={2}
                        value={requestedResolution}
                        onChange={(event) => setRequestedResolution(event.target.value)}
                      />
                    </Field>
                    <Button disabled={appeal.isPending}>
                      {appeal.isPending ? "提交中…" : "提交申诉"}
                    </Button>
                  </form>
                ) : data.activeAppeal ? (
                  <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-700">
                    当前申诉正在处理中，教师回复后会显示在这里。
                  </p>
                ) : null}
              </CardContent>
            </Card>
          ) : null}
        </div>

        <div className="space-y-4">
          <Card className="h-fit">
            <CardHeader>
              <div>
                <p className="page-eyebrow">评分量规</p>
                <CardTitle className="mt-1">评价依据</CardTitle>
              </div>
              <span className="text-xs text-muted">100 分</span>
            </CardHeader>
            <CardContent className="space-y-2">
              {data.rubrics.map((rubric) => (
                <div key={rubric.id} className="rounded-xl bg-stone-50 p-3">
                  <div className="flex items-center justify-between gap-3">
                    <strong className="text-sm">{rubric.name}</strong>
                    <span className="text-xs text-pine-700">{rubric.points} 分</span>
                  </div>
                  <p className="mt-1 text-xs leading-5 text-muted">{rubric.description}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div>
                <p className="page-eyebrow">{latest ? "再次提交" : "在线提交"}</p>
                <CardTitle className="mt-1">{latest ? "提交修订版本" : "提交你的作业"}</CardTitle>
              </div>
              <FileUp className="size-5 text-pine-600" />
            </CardHeader>
            <CardContent>
              <form
                ref={formRef}
                className="space-y-4"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!validateFiles()) return;
                  submit.mutate();
                }}
              >
                <Field label="作业内容" hint="文字至少 20 字，或上传附件。">
                  <Textarea
                    rows={9}
                    value={content}
                    onChange={(event) => setContent(event.target.value)}
                    placeholder="粘贴或输入完整作业内容……"
                  />
                </Field>
                <Field label="文档、代码或截图" hint="最多 8 个附件，单个不超过 25 MB。">
                  <input
                    type="file"
                    name="files"
                    multiple
                    accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.csv,.rtf,.odt,.ods,.odp,.txt,.md,.java,.py,.js,.ts,.c,.cpp,.h,.cs,.go,.rs,.sql,.json,.xml,.yml,.yaml,.ipynb,.html,.css,.zip,.rar,.7z,.png,.jpg,.jpeg,.webp"
                    onChange={(event) => setFiles(Array.from(event.target.files ?? []))}
                    className="block w-full rounded-xl border border-dashed border-line bg-stone-50 p-3 text-xs text-muted file:mr-3 file:rounded-lg file:border-0 file:bg-pine-600 file:px-3 file:py-2 file:text-white"
                  />
                </Field>
                {files.length ? (
                  <ul className="space-y-1 rounded-xl bg-pine-50 p-3 text-xs text-pine-700">
                    {files.map((file) => (
                      <li key={`${file.name}-${file.size}`} className="flex justify-between gap-3">
                        <span className="truncate">{file.name}</span>
                        <span className="shrink-0">{formatFileSize(file.size)}</span>
                      </li>
                    ))}
                  </ul>
                ) : null}
                <div className="space-y-3 rounded-xl bg-stone-50 p-4">
                  <h3 className="text-sm font-semibold">学术诚信与 AI 使用声明</h3>
                  <Field label="AI 使用情况">
                    <Select value={aiUsage} onChange={(event) => setAiUsage(event.target.value)}>
                      <option value="NONE">未使用</option>
                      <option value="ASSISTED">辅助修改或思路启发</option>
                      <option value="PARTIAL_GENERATION">部分内容由 AI 生成</option>
                      <option value="MAIN_GENERATION">主要内容由 AI 生成</option>
                    </Select>
                  </Field>
                  {aiUsage !== "NONE" ? (
                    <>
                      <Field label="使用的 AI 工具">
                        <Input
                          value={aiTools}
                          onChange={(event) => setAiTools(event.target.value)}
                        />
                      </Field>
                      <Field label="AI 使用范围">
                        <Textarea
                          rows={2}
                          value={aiScope}
                          onChange={(event) => setAiScope(event.target.value)}
                        />
                      </Field>
                    </>
                  ) : null}
                  <Field label="文献与数据来源声明">
                    <Textarea
                      rows={3}
                      value={sourceDeclaration}
                      onChange={(event) => setSourceDeclaration(event.target.value)}
                      required={a.sourceDeclarationRequired}
                      placeholder="列出主要文献、数据来源和获取方式"
                    />
                  </Field>
                  <Field label="协作或外部帮助">
                    <Textarea
                      rows={2}
                      value={collaborationDeclaration}
                      onChange={(event) => setCollaborationDeclaration(event.target.value)}
                      placeholder="如无，请填写“无”"
                    />
                  </Field>
                  <label className="flex items-start gap-2 text-xs leading-5 text-stone-600">
                    <input
                      type="checkbox"
                      checked={integrityConfirmed}
                      onChange={(event) => setIntegrityConfirmed(event.target.checked)}
                      required={a.aiDeclarationRequired || a.sourceDeclarationRequired}
                      className="mt-0.5 size-4 accent-pine-600"
                    />
                    我确认以上声明真实，并了解教师可要求补充证据。
                  </label>
                </div>
                <Button type="submit" size="lg" className="w-full" disabled={submit.isPending}>
                  {submit.isPending ? "正在提交…" : latest ? "提交修订版本" : "提交作业"}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
