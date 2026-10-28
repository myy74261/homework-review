import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowRight,
  BarChart3,
  Bot,
  BookOpen,
  CalendarClock,
  ClipboardCheck,
  Copy,
  LayoutDashboard,
  MessageSquareWarning,
  Plus,
  Trash2,
  Undo2,
  Upload,
} from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import type { Classroom, ClassroomAnalytics, TeacherDashboard } from "@/lib/types";
import { formatDate, formatLabels } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { ErrorState, Loading } from "@/components/ui/loading";
import { Field, Input, Textarea } from "@/components/ui/field";

const classroomStatusLabels: Record<Classroom["status"], string> = {
  UNSCHEDULED: "时间未设置",
  UPCOMING: "未开始",
  ACTIVE: "进行中",
  ENDED: "已结课",
  DISSOLVED: "已解散",
};

function localDateTime(days = 0) {
  const value = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
  value.setSeconds(0, 0);
  const offset = value.getTimezoneOffset() * 60_000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 16);
}

function toDateTimeInput(value: string) {
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function ReviewCompletionCard({ data }: { data: TeacherDashboard }) {
  const totalSubmissions = data.submissions.length;
  const completion = totalSubmissions
    ? Math.round((data.reviewedCount / totalSubmissions) * 100)
    : 0;

  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="page-eyebrow">审核进度</p>
          <h2 className="mt-1 font-display text-xl font-semibold">审核完成度</h2>
        </div>
        <BarChart3 className="size-5 text-pine-600" />
      </div>
      <div className="mt-5 flex items-center gap-5">
        <div
          className="grid size-24 shrink-0 place-items-center rounded-full"
          style={{
            background: `conic-gradient(#2f6d57 ${completion * 3.6}deg, #e7ece9 0deg)`,
          }}
        >
          <div className="grid size-[68px] place-items-center rounded-full bg-white">
            <strong className="font-display text-xl text-pine-700">{completion}%</strong>
          </div>
        </div>
        <div className="space-y-1.5 text-sm">
          <p>
            <strong className="text-xl">{data.reviewedCount}</strong>
            <span className="ml-2 text-muted">已完成反馈</span>
          </p>
          <p>
            <strong className="text-xl">{totalSubmissions - data.reviewedCount}</strong>
            <span className="ml-2 text-muted">待完成审核</span>
          </p>
        </div>
      </div>
    </Card>
  );
}

function ClassroomAnalyticsPanel({ analytics }: { analytics?: ClassroomAnalytics }) {
  if (!analytics) return null;
  const submittedPercent = analytics.enrolledStudentCount
    ? Math.round((analytics.submittedStudentCount / analytics.enrolledStudentCount) * 100)
    : 0;
  const unsubmittedStudents = Math.max(
    0,
    analytics.enrolledStudentCount - analytics.submittedStudentCount,
  );
  const maxIssue = Math.max(1, ...analytics.commonIssues.map((issue) => issue.count));

  return (
    <div className="mt-4 space-y-4 border-t border-pine-500/10 pt-4">
      <div>
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-xs font-semibold">学生提交占比</h3>
          <span className="text-xs font-medium text-pine-700">{submittedPercent}%</span>
        </div>
        <div className="mt-2 flex h-2 overflow-hidden rounded-full bg-stone-100">
          <div className="h-full bg-pine-500" style={{ width: `${submittedPercent}%` }} />
          <div className="bg-clay-200 h-full" style={{ width: `${100 - submittedPercent}%` }} />
        </div>
        <p className="mt-2 text-[11px] text-muted">
          已提交学生 {analytics.submittedStudentCount} 人 · 未提交学生 {unsubmittedStudents} 人 · 共{" "}
          {analytics.enrolledStudentCount} 人
        </p>
      </div>

      <div>
        <h3 className="text-xs font-semibold">问题维度分析</h3>
        {analytics.commonIssues.length ? (
          <div className="mt-2 space-y-2">
            {analytics.commonIssues.slice(0, 4).map((issue) => (
              <div key={issue.rubricName}>
                <div className="mb-1 flex justify-between gap-3 text-[11px] text-muted">
                  <span>{issue.rubricName}</span>
                  <strong>{issue.count}</strong>
                </div>
                <div className="h-1.5 overflow-hidden rounded-full bg-white">
                  <div
                    className="h-full rounded-full bg-pine-500"
                    style={{ width: `${(issue.count / maxIssue) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="mt-2 text-[11px] text-muted">暂无已确认的共性问题。</p>
        )}
      </div>
    </div>
  );
}

function parseRoster(text: string) {
  return text
    .replace(/^\uFEFF/, "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [studentNumber = "", studentName = ""] = line
        .split(/[,\t;]/)
        .map((value) => value.trim());
      if (index === 0 && studentNumber.includes("学号") && studentName.includes("姓名")) {
        return null;
      }
      return { studentNumber, studentName };
    })
    .filter(
      (entry): entry is { studentNumber: string; studentName: string } =>
        entry != null && Boolean(entry.studentNumber) && Boolean(entry.studentName),
    );
}

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
  const [courseStartAt, setCourseStartAt] = useState(localDateTime(0));
  const [courseEndAt, setCourseEndAt] = useState(localDateTime(120));
  const [scheduleTarget, setScheduleTarget] = useState<Classroom | null>(null);
  const [scheduleStartAt, setScheduleStartAt] = useState("");
  const [scheduleEndAt, setScheduleEndAt] = useState("");
  const [dissolveTarget, setDissolveTarget] = useState<Classroom | null>(null);
  const [rosterTarget, setRosterTarget] = useState<Classroom | null>(null);
  const [rosterText, setRosterText] = useState("");
  const [rosterFileName, setRosterFileName] = useState("");
  const [dueTarget, setDueTarget] = useState<{
    id: number;
    title: string;
  } | null>(null);
  const [dueAtInput, setDueAtInput] = useState(localDateTime(7));
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
    mutationFn: () =>
      api.createClassroom({
        name: classroomName,
        courseName,
        startAt: new Date(courseStartAt).toISOString(),
        endAt: new Date(courseEndAt).toISOString(),
      }),
    onSuccess: async () => {
      notify("课堂已创建，可以分享课堂码。", "success");
      setClassroomOpen(false);
      setClassroomName("");
      setCourseName("");
      setCourseStartAt(localDateTime(0));
      setCourseEndAt(localDateTime(120));
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-classrooms"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const updateSchedule = useMutation({
    mutationFn: () =>
      api.updateClassroomSchedule(scheduleTarget!.id, {
        startAt: new Date(scheduleStartAt).toISOString(),
        endAt: new Date(scheduleEndAt).toISOString(),
      }),
    onSuccess: async () => {
      notify("课程时间已更新。", "success");
      setScheduleTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-classrooms"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const dissolveClassroom = useMutation({
    mutationFn: (classroomId: number) => api.dissolveClassroom(classroomId),
    onSuccess: async () => {
      notify("课堂已解散，历史数据仍保留在数据库中。", "success");
      setDissolveTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-classrooms"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const updateDueDate = useMutation({
    mutationFn: () => api.updateAssignmentDueDate(dueTarget!.id, dueAtInput),
    onSuccess: async () => {
      notify("作业截止时间已更新，无需撤回作业。", "success");
      setDueTarget(null);
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["settled-submissions"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  const importRoster = useMutation({
    mutationFn: () => api.importClassroomRoster(rosterTarget!.id, parseRoster(rosterText)),
    onSuccess: async (result) => {
      notify(
        `名单已导入，共 ${result.rosterCount} 人，已关联 ${result.linkedCount} 个学生账号。`,
        "success",
      );
      setRosterTarget(null);
      setRosterText("");
      setRosterFileName("");
      await queryClient.invalidateQueries({ queryKey: ["teacher-dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["teacher-classrooms"] });
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
  const rosterEntries = parseRoster(rosterText);

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-line bg-white/85 px-5 py-5 shadow-card sm:px-6">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <p className="page-eyebrow">教学概览</p>
            <h1 className="mt-1 font-display text-3xl font-semibold tracking-tight sm:text-4xl">
              教学工作台
            </h1>
            <p className="mt-2 text-sm text-muted">审核作业、查看进度并整理课堂反馈。</p>
          </div>
          <Button asChild>
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
              <section className="grid grid-cols-2 gap-2 rounded-2xl border border-line bg-white/85 p-2 shadow-card sm:grid-cols-4">
                {[
                  {
                    label: "已发布作业",
                    value: publishedCount,
                  },
                  {
                    label: "学生提交",
                    value: data.submissions.length,
                  },
                  {
                    label: "待完成审核",
                    value: pendingCount,
                  },
                  {
                    label: "已完成反馈",
                    value: data.reviewedCount,
                  },
                ].map((metric) => (
                  <div key={metric.label} className="rounded-xl bg-stone-50/80 px-4 py-3">
                    <span className="text-xs text-muted">{metric.label}</span>
                    <strong className="mt-1 block font-display text-2xl">{metric.value}</strong>
                  </div>
                ))}
              </section>

              <ReviewCompletionCard data={data} />

              <section className="rounded-2xl border border-line bg-white/85 p-5 shadow-card">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="section-title">我的课堂</h2>
                  </div>
                  <Button variant="secondary" size="sm" onClick={() => setClassroomOpen(true)}>
                    <Plus className="size-4" />
                    新建课堂
                  </Button>
                </div>
                <div
                  className={`mt-4 grid gap-3 ${
                    data.classrooms.length > 1 ? "lg:grid-cols-2" : ""
                  }`}
                >
                  {data.classrooms.map((classroom) => (
                    <div
                      key={classroom.id}
                      data-testid={`classroom-card-${classroom.id}`}
                      className="rounded-xl border border-line bg-pine-50/55 p-4"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <strong className="text-sm">{classroom.name}</strong>
                            <Badge
                              tone={
                                classroom.status === "ACTIVE"
                                  ? "default"
                                  : classroom.status === "ENDED"
                                    ? "neutral"
                                    : "warning"
                              }
                            >
                              {classroomStatusLabels[classroom.status]}
                            </Badge>
                          </div>
                          <p className="mt-1 text-xs text-muted">
                            {classroom.courseName} · {classroom.memberCount} 名学生
                          </p>
                          <p className="mt-1 flex items-center gap-1.5 text-xs text-muted">
                            <CalendarClock className="size-3.5" />
                            {classroom.startAt && classroom.endAt
                              ? `${formatDate(classroom.startAt)} 至 ${formatDate(classroom.endAt)}`
                              : "课程时间未设置"}
                          </p>
                          <p className="mt-1 text-xs text-muted">
                            名单 {classroom.rosterCount} 人 · 已关联 {classroom.linkedRosterCount}{" "}
                            个账号 · 待注册 {classroom.rosterCount - classroom.linkedRosterCount} 人
                            {classroom.unlistedMemberCount
                              ? ` · 名单外成员 ${classroom.unlistedMemberCount} 人`
                              : ""}
                          </p>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            void navigator.clipboard.writeText(classroom.joinCode);
                            notify(`课堂码 ${classroom.joinCode} 已复制。`, "success");
                          }}
                          className="flex shrink-0 items-center gap-2 rounded-lg border border-dashed border-pine-500/40 bg-white px-2.5 py-2 font-mono text-xs text-pine-700"
                        >
                          {classroom.joinCode}
                          <Copy className="size-3.5" />
                        </button>
                      </div>
                      <ClassroomAnalyticsPanel
                        analytics={data.classroomAnalytics.find(
                          (item) => item.classroomId === classroom.id,
                        )}
                      />
                      <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-pine-500/10 pt-3">
                        <div className="flex flex-wrap gap-1">
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setScheduleTarget(classroom);
                              setScheduleStartAt(
                                classroom.startAt
                                  ? toDateTimeInput(classroom.startAt)
                                  : localDateTime(0),
                              );
                              setScheduleEndAt(
                                classroom.endAt
                                  ? toDateTimeInput(classroom.endAt)
                                  : localDateTime(120),
                              );
                            }}
                          >
                            <CalendarClock className="size-3.5" />
                            {classroom.status === "UNSCHEDULED" ? "设置课程时间" : "调整课程时间"}
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setRosterTarget(classroom);
                              setRosterText("");
                              setRosterFileName("");
                            }}
                          >
                            <Upload className="size-3.5" />
                            导入名单
                          </Button>
                        </div>
                        {classroom.status === "ENDED" ? (
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            onClick={() => setDissolveTarget(classroom)}
                          >
                            <Trash2 className="size-3.5" />
                            解散课堂
                          </Button>
                        ) : null}
                      </div>
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
                            <>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => {
                                  setDueTarget({
                                    id: assignment.id,
                                    title: assignment.title,
                                  });
                                  setDueAtInput(
                                    new Date(assignment.dueAt).getTime() > Date.now()
                                      ? toDateTimeInput(assignment.dueAt)
                                      : localDateTime(7),
                                  );
                                }}
                              >
                                <CalendarClock className="size-3.5" />
                                调整截止时间
                              </Button>
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
                            </>
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
                      <p className="page-eyebrow">AI 预审</p>
                      <CardTitle className="mt-1">批量预审进度</CardTitle>
                    </div>
                    <Bot className="size-5 text-pine-600" />
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {!data.aiConfigured ? (
                      <div className="rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-700">
                        <p>尚未接入模型。配置 AI 接口后，作业截止时会自动开始预审。</p>
                        <Button asChild variant="secondary" size="sm" className="mt-3">
                          <Link to="/teacher/ai-settings">配置 AI 接口</Link>
                        </Button>
                      </div>
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
                          {status === "等待配置" ? (
                            <Link to="/teacher/ai-settings" className="shrink-0">
                              <Badge tone="warning">等待配置 →</Badge>
                            </Link>
                          ) : (
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
                          )}
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
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="课程起始时间">
              <Input
                type="datetime-local"
                value={courseStartAt}
                onChange={(event) => setCourseStartAt(event.target.value)}
                required
              />
            </Field>
            <Field label="课程结束时间">
              <Input
                type="datetime-local"
                value={courseEndAt}
                onChange={(event) => setCourseEndAt(event.target.value)}
                required
              />
            </Field>
          </div>
          <Button
            size="lg"
            disabled={
              createClassroom.isPending ||
              !classroomName.trim() ||
              !courseName.trim() ||
              !courseStartAt ||
              !courseEndAt ||
              courseStartAt >= courseEndAt
            }
            onClick={() => createClassroom.mutate()}
          >
            {createClassroom.isPending ? "创建中…" : "创建课堂"}
          </Button>
        </div>
      </Dialog>

      <Dialog
        open={scheduleTarget != null}
        onOpenChange={(open) => {
          if (!open) setScheduleTarget(null);
        }}
        title="调整课程时间"
        description={
          scheduleTarget ? `设置《${scheduleTarget.name}》的课程起始和结束时间。` : undefined
        }
      >
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="新的起始时间">
              <Input
                type="datetime-local"
                value={scheduleStartAt}
                onChange={(event) => setScheduleStartAt(event.target.value)}
                required
              />
            </Field>
            <Field label="新的结束时间">
              <Input
                type="datetime-local"
                value={scheduleEndAt}
                onChange={(event) => setScheduleEndAt(event.target.value)}
                required
              />
            </Field>
          </div>
          <p className="rounded-xl bg-stone-50 p-3 text-xs leading-6 text-muted">
            调整结束时间不会删除作业、提交或审核记录。课程结束前不能解散课堂。
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setScheduleTarget(null)}>
              取消
            </Button>
            <Button
              type="button"
              disabled={
                updateSchedule.isPending ||
                !scheduleStartAt ||
                !scheduleEndAt ||
                scheduleStartAt >= scheduleEndAt
              }
              onClick={() => updateSchedule.mutate()}
            >
              {updateSchedule.isPending ? "保存中…" : "保存课程时间"}
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={rosterTarget != null}
        onOpenChange={(open) => {
          if (!open) setRosterTarget(null);
        }}
        title="导入学生名单"
        description={
          rosterTarget
            ? `为《${rosterTarget.name}》导入学号和姓名。已有账号会立即加入，未注册账号会在注册后自动关联。`
            : undefined
        }
      >
        <div className="space-y-4">
          <Field label="上传名单文件" hint="支持 CSV/TXT，格式为“学号,姓名”，首行可写表头。">
            <input
              type="file"
              accept=".csv,.txt,text/csv,text/plain"
              onChange={async (event) => {
                const file = event.target.files?.[0];
                if (!file) return;
                setRosterText(await file.text());
                setRosterFileName(file.name);
              }}
              className="block w-full rounded-xl border border-dashed border-line bg-stone-50 p-3 text-xs text-muted file:mr-3 file:rounded-lg file:border-0 file:bg-pine-600 file:px-3 file:py-2 file:text-white"
            />
          </Field>
          <Field label="或直接粘贴名单" hint="每行一名学生，例如：20230118,王同学。">
            <Textarea
              rows={8}
              value={rosterText}
              onChange={(event) => {
                setRosterText(event.target.value);
                setRosterFileName("");
              }}
              placeholder={"学号,姓名\n20230118,王同学\n20230119,李同学"}
            />
          </Field>
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-stone-50 p-3 text-xs text-muted">
            <span>
              {rosterFileName ? `${rosterFileName} · ` : ""}
              已识别 <strong className="text-ink">{rosterEntries.length}</strong> 名学生
            </span>
            {rosterTarget ? (
              <span>
                当前名单 {rosterTarget.rosterCount} 人，已关联 {rosterTarget.linkedRosterCount}{" "}
                个账号，待注册 {rosterTarget.rosterCount - rosterTarget.linkedRosterCount} 人
                {rosterTarget.unlistedMemberCount
                  ? `，名单外成员 ${rosterTarget.unlistedMemberCount} 人`
                  : ""}
              </span>
            ) : null}
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setRosterTarget(null)}>
              取消
            </Button>
            <Button
              type="button"
              disabled={
                importRoster.isPending || rosterEntries.length === 0 || rosterEntries.length > 500
              }
              onClick={() => importRoster.mutate()}
            >
              {importRoster.isPending ? "导入中…" : "确认导入名单"}
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={dissolveTarget != null}
        onOpenChange={(open) => {
          if (!open) setDissolveTarget(null);
        }}
        title="解散课堂"
        description={
          dissolveTarget
            ? `《${dissolveTarget.name}》已结课。解散后将不再显示在教师端和学生端。`
            : undefined
        }
      >
        <div className="space-y-4">
          <p className="rounded-xl bg-amber-50 p-3 text-xs leading-6 text-amber-700">
            解散采用软删除，作业、提交、评分和申诉记录仍保留在数据库中，不会物理删除。
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setDissolveTarget(null)}>
              暂不解散
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={dissolveClassroom.isPending}
              onClick={() => {
                if (dissolveTarget) dissolveClassroom.mutate(dissolveTarget.id);
              }}
            >
              {dissolveClassroom.isPending ? "解散中…" : "确认解散"}
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={dueTarget != null}
        onOpenChange={(open) => {
          if (!open) setDueTarget(null);
        }}
        title="调整作业截止时间"
        description={
          dueTarget ? `只修改《${dueTarget.title}》的提交时间，作业内容和状态保持不变。` : undefined
        }
      >
        <div className="space-y-4">
          <Field label="新的截止时间">
            <Input
              type="datetime-local"
              value={dueAtInput}
              onChange={(event) => setDueAtInput(event.target.value)}
              required
            />
          </Field>
          <p className="rounded-xl bg-amber-50 p-3 text-xs leading-6 text-amber-700">
            无需撤回作业。如果原截止时间已过且 AI 预审已经开始，已有建议和运行记录会保留。
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setDueTarget(null)}>
              取消
            </Button>
            <Button
              type="button"
              disabled={updateDueDate.isPending || !dueAtInput}
              onClick={() => updateDueDate.mutate()}
            >
              {updateDueDate.isPending ? "保存中…" : "保存截止时间"}
            </Button>
          </div>
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
