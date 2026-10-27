import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowRight,
  Bot,
  BookOpen,
  CheckCircle2,
  ClipboardCheck,
  Copy,
  LayoutDashboard,
  MessageSquareWarning,
  Plus,
  Undo2,
} from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { formatDate, formatLabels } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { ErrorState, Loading } from "@/components/ui/loading";
import { Field, Input } from "@/components/ui/field";

export function TeacherDashboardPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const [classroomOpen, setClassroomOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<
    "overview" | "assignments" | "submissions" | "ai" | "appeals"
  >("overview");
  const [classroomName, setClassroomName] = useState("");
  const [courseName, setCourseName] = useState("");
  const [appealDrafts, setAppealDrafts] = useState<Record<number, string>>({});
  const [appealStatus, setAppealStatus] = useState<Record<number, string>>({});
  const [withdrawTarget, setWithdrawTarget] = useState<{
    id: number;
    title: string;
    submissionCount: number;
  } | null>(null);

  const dashboard = useQuery({
    queryKey: ["teacher-dashboard"],
    queryFn: api.teacherDashboard,
  });

  const createClassroom = useMutation({
    mutationFn: () => api.createClassroom({ name: classroomName, courseName }),
    onSuccess: async () => {
      notify("课堂已创建，可以分享课堂码。", "success");
      setClassroomOpen(false);
      setClassroomName("");
      setCourseName("");
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const respondAppeal = useMutation({
    mutationFn: ({ id }: { id: number }) =>
      api.respondAppeal(id, {
        status: (appealStatus[id] ?? "UNDER_REVIEW") as "UNDER_REVIEW" | "ACCEPTED" | "REJECTED",
        teacherResponse: appealDrafts[id] ?? "",
      }),
    onSuccess: async () => {
      notify("申诉复核结果已保存。", "success");
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const withdrawAssignment = useMutation({
    mutationFn: (assignmentId: number) => api.withdrawAssignment(assignmentId),
    onSuccess: async (result) => {
      setWithdrawTarget(null);
      notify("作业已撤回，学生端暂时隐藏。请修改后重新发布。", "success");
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      navigate(result.redirectPath.replace("/app", ""));
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (dashboard.isLoading) return <Loading />;
  if (dashboard.isError) {
    return <ErrorState message={dashboard.error.message} retry={() => dashboard.refetch()} />;
  }

  const data = dashboard.data!;
  const pendingCount = data.submissions.filter((item) => item.reviewStatus !== "REVIEWED").length;
  const publishedCount = data.assignments.filter(
    ({ assignment }) => assignment.status === "PUBLISHED",
  ).length;

  return (
    <div className="space-y-5">
      <section className="relative isolate overflow-hidden rounded-[28px] border border-white/80 bg-white/80 p-6 shadow-card sm:p-8">
        <div className="absolute -bottom-14 right-6 -z-10 select-none font-display text-[150px] text-pine-600/[0.045]">
          明鉴
        </div>
        <div className="flex flex-col justify-between gap-6 lg:flex-row lg:items-end">
          <div>
            <p className="page-eyebrow">教学概览</p>
            <h1 className="mt-2 max-w-3xl font-display text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
              作业审核与课堂反馈
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-muted">
              查看学生提交，完成作业审核，并整理课堂反馈。
            </p>
          </div>
          <Button asChild size="lg">
            <Link to="/teacher/assignments/new">
              <Plus className="size-4" />
              发布作业
            </Link>
          </Button>
        </div>
      </section>

      <div className="grid gap-4 lg:grid-cols-[190px_minmax(0,1fr)] lg:items-start">
        <nav
          role="tablist"
          aria-label="教师主页任务栏"
          className="flex gap-1 overflow-x-auto rounded-2xl border border-line bg-white/90 p-1.5 shadow-card backdrop-blur-xl lg:sticky lg:top-20 lg:flex-col lg:overflow-visible"
        >
          {[
            { id: "overview", label: "概览", icon: LayoutDashboard, count: null },
            {
              id: "assignments",
              label: "作业管理",
              icon: BookOpen,
              count: data.assignments.length,
            },
            {
              id: "submissions",
              label: "提交审核",
              icon: ClipboardCheck,
              count: pendingCount,
            },
            { id: "ai", label: "AI 预审", icon: Bot, count: null },
            {
              id: "appeals",
              label: "申诉",
              icon: MessageSquareWarning,
              count: data.appeals.length,
            },
          ].map((tab) => {
            const Icon = tab.icon;
            const selected = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`flex min-h-10 shrink-0 items-center gap-2 rounded-xl px-3 text-left text-sm transition lg:w-full ${
                  selected
                    ? "bg-pine-600 font-medium text-white shadow-sm"
                    : "text-muted hover:bg-pine-50 hover:text-pine-700"
                }`}
              >
                <Icon className="size-4" />
                {tab.label}
                {tab.count != null ? (
                  <span
                    className={`rounded-full px-1.5 py-0.5 text-[10px] ${
                      selected ? "bg-white/15" : "bg-stone-100 text-stone-600"
                    }`}
                  >
                    {tab.count}
                  </span>
                ) : null}
              </button>
            );
          })}
        </nav>

        <div className="min-w-0 space-y-5">
          {activeTab === "overview" ? (
            <>
              <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  {
                    label: "已发布作业",
                    value: publishedCount,
                    detail: "课堂任务",
                    icon: BookOpen,
                  },
                  {
                    label: "学生提交",
                    value: data.submissions.length,
                    detail: "提交记录",
                    icon: ClipboardCheck,
                  },
                  {
                    label: "待完成审核",
                    value: pendingCount,
                    detail: "需要处理",
                    icon: MessageSquareWarning,
                  },
                  {
                    label: "已完成反馈",
                    value: data.reviewedCount,
                    detail: "反馈已发布",
                    icon: CheckCircle2,
                  },
                ].map((metric) => {
                  const Icon = metric.icon;
                  return (
                    <Card key={metric.label} className="relative overflow-hidden p-5">
                      <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-pine-500/70 to-emerald-100" />
                      <div className="flex items-start justify-between">
                        <span className="text-sm text-muted">{metric.label}</span>
                        <Icon className="size-4 text-pine-600" />
                      </div>
                      <strong className="metric-number mt-4 block">{metric.value}</strong>
                      <small className="mt-1 block text-xs text-stone-500">{metric.detail}</small>
                    </Card>
                  );
                })}
              </section>

              <section className="rounded-2xl border border-line bg-white/85 p-5 shadow-card">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="page-eyebrow">我的课堂</p>
                    <h2 className="section-title mt-1">课堂与加入码</h2>
                  </div>
                  <Button variant="secondary" size="sm" onClick={() => setClassroomOpen(true)}>
                    <Plus className="size-4" />
                    新建课堂
                  </Button>
                </div>
                <div className="mt-4 flex gap-3 overflow-x-auto pb-1">
                  {data.classrooms.map((classroom) => (
                    <div
                      key={classroom.id}
                      className="flex min-w-[280px] flex-1 items-center justify-between gap-4 rounded-xl border border-line bg-pine-50/55 px-4 py-3"
                    >
                      <div>
                        <strong className="text-sm">{classroom.name}</strong>
                        <p className="mt-1 text-xs text-muted">
                          {classroom.courseName} · {classroom.memberCount} 名学生
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          void navigator.clipboard.writeText(classroom.joinCode);
                          notify(`课堂码 ${classroom.joinCode} 已复制。`, "success");
                        }}
                        className="flex items-center gap-2 rounded-lg border border-dashed border-pine-500/40 bg-white px-2.5 py-2 font-mono text-xs text-pine-700"
                      >
                        {classroom.joinCode}
                        <Copy className="size-3.5" />
                      </button>
                    </div>
                  ))}
                  {data.classrooms.length === 0 ? (
                    <p className="rounded-xl border border-dashed border-line px-4 py-5 text-sm text-muted">
                      还没有课堂，请先创建一个课堂。
                    </p>
                  ) : null}
                </div>
              </section>
            </>
          ) : null}

          {activeTab !== "overview" ? (
            <div className="grid gap-4">
              <div className="space-y-4">
                <section hidden={activeTab !== "assignments"}>
                  <div className="mb-3 flex items-center justify-between">
                    <div>
                      <p className="page-eyebrow">作业</p>
                      <h2 className="section-title mt-1">已发布作业</h2>
                    </div>
                    <Badge>{data.assignments.length} 项</Badge>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    {data.assignments.map(({ assignment, submissionCount }) => (
                      <Card
                        key={assignment.id}
                        data-testid={`assignment-card-${assignment.id}`}
                        className="p-5"
                      >
                        <div className="flex items-center justify-between gap-3">
                          <div className="flex flex-wrap gap-2">
                            <Badge>{formatLabels[assignment.submissionFormat]}</Badge>
                            <Badge tone={assignment.status === "PUBLISHED" ? "default" : "warning"}>
                              {assignment.status === "PUBLISHED" ? "已发布" : "已撤回"}
                            </Badge>
                          </div>
                          <span className="text-xs text-muted">{assignment.courseName}</span>
                        </div>
                        <h3 className="mt-3 font-display text-xl font-semibold leading-7">
                          {assignment.title}
                        </h3>
                        <p className="mt-2 line-clamp-2 text-sm leading-6 text-muted">
                          {assignment.instructions}
                        </p>
                        <div className="mt-4 flex items-center justify-between border-t border-line pt-3 text-xs text-muted">
                          <span>截止 {formatDate(assignment.dueAt)}</span>
                          <span>{submissionCount} 份提交</span>
                        </div>
                        <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                          <Button asChild variant="ghost" size="sm">
                            <Link to={`/teacher/assignments/${assignment.id}/lesson-plan`}>
                              生成备课建议
                              <ArrowRight className="size-3.5" />
                            </Link>
                          </Button>
                          {assignment.status === "WITHDRAWN" ? (
                            <Button asChild variant="secondary" size="sm">
                              <Link to={`/teacher/assignments/${assignment.id}/edit`}>
                                继续编辑
                              </Link>
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                setWithdrawTarget({
                                  id: assignment.id,
                                  title: assignment.title,
                                  submissionCount,
                                })
                              }
                            >
                              <Undo2 className="size-3.5" />
                              撤回并修改
                            </Button>
                          )}
                        </div>
                      </Card>
                    ))}
                  </div>
                </section>

                <Card hidden={activeTab !== "submissions"}>
                  <CardHeader>
                    <div>
                      <p className="page-eyebrow">待审核</p>
                      <CardTitle className="mt-1">学生提交</CardTitle>
                    </div>
                    <Badge tone="warning">{pendingCount} 待审核</Badge>
                  </CardHeader>
                  <CardContent className="p-0">
                    <div className="divide-y divide-line">
                      {data.submissions.map((submission) => (
                        <div
                          key={submission.id}
                          className="flex flex-col justify-between gap-3 px-5 py-4 sm:flex-row sm:items-center"
                        >
                          <div className="flex min-w-0 items-center gap-3">
                            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-pine-50 font-display text-pine-700">
                              {submission.studentName.slice(0, 1)}
                            </span>
                            <div className="min-w-0">
                              <strong className="block truncate text-sm">
                                {submission.studentName} · {submission.studentNumber}
                              </strong>
                              <p className="truncate text-xs text-muted">
                                {submission.assignmentTitle}
                              </p>
                              <small className="text-[11px] text-stone-400">
                                第 {submission.attemptNumber} 次 ·{" "}
                                {formatDate(submission.submittedAt)}
                              </small>
                            </div>
                          </div>
                          <div className="flex shrink-0 items-center gap-2">
                            <Badge
                              tone={submission.reviewStatus === "REVIEWED" ? "default" : "warning"}
                            >
                              {submission.statusText}
                            </Badge>
                            <Button asChild variant="secondary" size="sm">
                              <Link to={`/reviews/${submission.id}`}>
                                {submission.reviewStatus === "REVIEWED" ? "查看审核" : "开始审核"}
                              </Link>
                            </Button>
                          </div>
                        </div>
                      ))}
                      {data.submissions.length === 0 ? (
                        <p className="px-5 py-10 text-center text-sm text-muted">
                          学生提交后会出现在这里。
                        </p>
                      ) : null}
                    </div>
                  </CardContent>
                </Card>
              </div>

              <aside className="space-y-4" hidden={activeTab !== "ai" && activeTab !== "appeals"}>
                <Card hidden={activeTab !== "ai"}>
                  <CardHeader>
                    <div>
                      <p className="page-eyebrow">共性问题</p>
                      <CardTitle className="mt-1">常见问题</CardTitle>
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    {data.commonIssues.map((issue) => (
                      <div
                        key={issue.rubricName}
                        className="flex items-center justify-between rounded-xl bg-clay-50 px-3 py-2.5 text-sm text-clay-600"
                      >
                        <span>{issue.rubricName}</span>
                        <strong>{issue.count} 处</strong>
                      </div>
                    ))}
                    {data.commonIssues.length === 0 ? (
                      <p className="rounded-xl border border-dashed border-line p-4 text-center text-sm leading-6 text-muted">
                        在审核页确认问题后，这里会自动汇总。
                      </p>
                    ) : null}
                    <div className="mt-3 rounded-xl bg-stone-50 p-3 text-xs leading-6 text-muted">
                      平台只整理教师确认的问题，不自动判定学术不端。
                    </div>
                  </CardContent>
                </Card>

                <Card hidden={activeTab !== "ai"}>
                  <CardHeader>
                    <div>
                      <p className="page-eyebrow">AI 预审</p>
                      <CardTitle className="mt-1">批量预审进度</CardTitle>
                    </div>
                    <Bot className="size-5 text-pine-600" />
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {!data.aiConfigured ? (
                      <p className="rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-700">
                        尚未接入模型。配置 AI 接口后，作业截止时会自动开始预审。
                      </p>
                    ) : (
                      <p className="rounded-xl bg-pine-50 p-3 text-xs leading-5 text-pine-700">
                        作业截止后自动开始。AI 只生成待确认建议，不会发布成绩。
                      </p>
                    )}
                    {data.assignments.map(({ assignment }) => {
                      const progress = data.aiBatchProgress.find(
                        (item) => item.assignmentId === assignment.id,
                      );
                      const status =
                        !progress || progress.total === 0
                          ? data.aiConfigured
                            ? "等待截止"
                            : "等待配置"
                          : progress.running > 0
                            ? `处理中 ${progress.completed}/${progress.total}`
                            : progress.pending > 0
                              ? `排队 ${progress.pending}`
                              : progress.failed > 0
                                ? `失败 ${progress.failed}`
                                : progress.completed === progress.total
                                  ? "预审完成"
                                  : "已跳过";
                      return (
                        <div
                          key={assignment.id}
                          className="flex items-center justify-between gap-3 rounded-xl border border-line p-3"
                        >
                          <span className="min-w-0 truncate text-xs text-stone-700">
                            {assignment.title}
                          </span>
                          <Badge
                            tone={
                              status === "预审完成"
                                ? "default"
                                : status.startsWith("失败")
                                  ? "danger"
                                  : "warning"
                            }
                            className="shrink-0"
                          >
                            {status}
                          </Badge>
                        </div>
                      );
                    })}
                  </CardContent>
                </Card>

                <Card hidden={activeTab !== "appeals"}>
                  <CardHeader>
                    <div>
                      <p className="page-eyebrow">反馈复核</p>
                      <CardTitle className="mt-1">申诉</CardTitle>
                    </div>
                    <Badge tone={data.appeals.length ? "warning" : "neutral"}>
                      {data.appeals.length} 条
                    </Badge>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {data.appeals.map(({ appeal, studentName, assignmentTitle }) => (
                      <div key={appeal.id} className="rounded-xl border border-line p-3">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <strong className="text-sm">{studentName}</strong>
                            <p className="mt-0.5 text-xs text-muted">{assignmentTitle}</p>
                          </div>
                          <Badge tone="warning">{appeal.status}</Badge>
                        </div>
                        <p className="mt-3 text-sm leading-6 text-stone-700">{appeal.reason}</p>
                        {appeal.teacherResponse ? (
                          <p className="mt-2 rounded-lg bg-pine-50 p-2 text-xs leading-5 text-pine-700">
                            当前回复：{appeal.teacherResponse}
                          </p>
                        ) : null}
                        <div className="mt-3 grid gap-2">
                          <select
                            value={appealStatus[appeal.id] ?? "UNDER_REVIEW"}
                            onChange={(event) =>
                              setAppealStatus((value) => ({
                                ...value,
                                [appeal.id]: event.target.value,
                              }))
                            }
                            className="h-9 rounded-lg border border-line bg-white px-2 text-xs"
                          >
                            <option value="UNDER_REVIEW">继续复核</option>
                            <option value="ACCEPTED">采纳申诉</option>
                            <option value="REJECTED">维持原结果</option>
                          </select>
                          <textarea
                            rows={2}
                            value={appealDrafts[appeal.id] ?? ""}
                            onChange={(event) =>
                              setAppealDrafts((value) => ({
                                ...value,
                                [appeal.id]: event.target.value,
                              }))
                            }
                            placeholder="填写复核依据"
                            className="rounded-lg border border-line px-2 py-2 text-xs leading-5 outline-none focus:border-pine-500"
                          />
                          <Button
                            size="sm"
                            disabled={respondAppeal.isPending}
                            onClick={() => respondAppeal.mutate({ id: appeal.id })}
                          >
                            保存复核结果
                          </Button>
                        </div>
                      </div>
                    ))}
                    {data.appeals.length === 0 ? (
                      <p className="py-6 text-center text-sm text-muted">当前没有待处理申诉。</p>
                    ) : null}
                  </CardContent>
                </Card>
              </aside>
            </div>
          ) : null}
        </div>
      </div>

      <Dialog
        open={classroomOpen}
        onOpenChange={setClassroomOpen}
        title="新建课堂"
        description="课堂码会自动生成，学生加入后才能看到作业。"
      >
        <div className="grid gap-4">
          <Field label="课堂名称">
            <Input
              value={classroomName}
              onChange={(event) => setClassroomName(event.target.value)}
              placeholder="例如：传播学方法课 · 2026 秋"
            />
          </Field>
          <Field label="课程名称">
            <Input
              value={courseName}
              onChange={(event) => setCourseName(event.target.value)}
              placeholder="例如：传播学方法课"
            />
          </Field>
          <Button
            size="lg"
            disabled={createClassroom.isPending || !classroomName.trim() || !courseName.trim()}
            onClick={() => createClassroom.mutate()}
          >
            {createClassroom.isPending ? "创建中…" : "创建课堂"}
          </Button>
        </div>
      </Dialog>

      <Dialog
        open={withdrawTarget != null}
        onOpenChange={(open) => {
          if (!open) setWithdrawTarget(null);
        }}
        title="撤回作业并修改"
        description={
          withdrawTarget
            ? `《${withdrawTarget.title}》撤回后将从学生端暂时隐藏，教师可修改内容后重新发布。`
            : undefined
        }
      >
        <div className="space-y-4">
          {withdrawTarget?.submissionCount ? (
            <p className="rounded-xl bg-amber-50 p-3 text-xs leading-6 text-amber-700">
              该作业已有 {withdrawTarget.submissionCount} 份提交。撤回不会删除提交和审核记录；
              修改截止时间或评分标准前，请确认不会影响已完成的工作。
            </p>
          ) : (
            <p className="rounded-xl bg-pine-50 p-3 text-xs leading-6 text-pine-700">
              当前还没有学生提交，可以安全撤回并修改。
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setWithdrawTarget(null)}>
              取消
            </Button>
            <Button
              type="button"
              disabled={withdrawAssignment.isPending}
              onClick={() => {
                if (withdrawTarget) withdrawAssignment.mutate(withdrawTarget.id);
              }}
            >
              {withdrawAssignment.isPending ? "撤回中…" : "确认撤回"}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
