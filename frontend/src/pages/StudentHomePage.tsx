import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, BookOpen, CheckCircle2, Clock3, KeyRound, ListTodo } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import type { StudentAssignmentOverview } from "@/lib/types";
import { formatDate } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Field, Input } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

function AssignmentTaskCard({ item }: { item: StudentAssignmentOverview }) {
  return (
    <Card className="group flex flex-col p-5">
      <div className="flex items-center justify-between gap-3">
        <Badge
          tone={
            item.statusText === "已反馈"
              ? "default"
              : item.statusText === "待提交"
                ? "warning"
                : "neutral"
          }
        >
          {item.statusText}
        </Badge>
        <span className="text-xs text-muted">{item.assignment.courseName}</span>
      </div>
      <h3 className="mt-3 font-display text-xl font-semibold leading-7">{item.assignment.title}</h3>
      <p className="mt-2 line-clamp-3 flex-1 text-sm leading-6 text-muted">
        {item.assignment.instructions}
      </p>
      <div className="mt-4 grid gap-2 border-t border-line pt-3 text-xs text-muted">
        <span className="flex items-center gap-2">
          <Clock3 className="size-3.5" />
          截止 {formatDate(item.assignment.dueAt)}
        </span>
        <span className="flex items-center gap-2">
          {item.latestSubmission ? (
            <CheckCircle2 className="size-3.5 text-pine-600" />
          ) : (
            <BookOpen className="size-3.5" />
          )}
          {item.latestSubmission ? `第 ${item.latestSubmission.attemptNumber} 次提交` : "尚未提交"}
        </span>
      </div>
      <Button asChild className="mt-4 w-full" variant="secondary">
        <Link to={`/student/assignments/${item.assignment.id}`}>
          {item.latestSubmission ? "查看提交与反馈" : "查看要求并提交"}
          <ArrowRight className="size-4 transition group-hover:translate-x-0.5" />
        </Link>
      </Button>
    </Card>
  );
}

export function StudentHomePage() {
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const [joinCode, setJoinCode] = useState("");
  const [selectedClassroomId, setSelectedClassroomId] = useState<number | "all">("all");
  const dashboard = useQuery({
    queryKey: ["student-dashboard"],
    queryFn: api.studentDashboard,
  });

  const join = useMutation({
    mutationFn: () => api.joinClassroom(joinCode),
    onSuccess: async (classroom) => {
      setJoinCode("");
      setSelectedClassroomId(classroom.id);
      notify(`已加入课堂：${classroom.name}`, "success");
      await queryClient.invalidateQueries({ queryKey: ["student-dashboard"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (dashboard.isLoading) return <Loading />;
  if (dashboard.isError) {
    return <ErrorState message={dashboard.error.message} retry={() => dashboard.refetch()} />;
  }

  const data = dashboard.data!;
  const pending = data.assignments.filter((item) => !item.latestSubmission).length;
  const feedback = data.assignments.filter((item) => item.statusText === "已反馈").length;
  const filteredAssignments =
    selectedClassroomId === "all"
      ? data.assignments
      : data.assignments.filter((item) => item.assignment.classroomId === selectedClassroomId);
  const pendingAssignments = filteredAssignments.filter((item) => !item.latestSubmission);
  const submittedAssignments = filteredAssignments.filter((item) => item.latestSubmission);
  const selectedClassroom =
    selectedClassroomId === "all"
      ? "全部课堂"
      : data.classrooms.find((classroom) => classroom.id === selectedClassroomId)?.name;

  return (
    <div className="space-y-5">
      <section className="rounded-[28px] border border-line bg-white/85 p-6 shadow-card lg:p-8">
        <p className="page-eyebrow">你好，{data.user.displayName}</p>
        <div className="mt-2 flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
          <div>
            <h1 className="max-w-2xl font-display text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
              作业与反馈
            </h1>
            <p className="mt-4 max-w-xl text-sm leading-7 text-muted">
              按课堂查看任务，优先完成待提交作业。
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge>{data.assignments.length} 项作业</Badge>
            <Badge tone="warning">{pending} 项待办</Badge>
            <Badge>{feedback} 项已反馈</Badge>
          </div>
        </div>
      </section>

      <div className="grid gap-4 lg:grid-cols-[250px_minmax(0,1fr)] lg:items-start">
        <aside className="rounded-2xl border border-line bg-white/90 p-3 shadow-card lg:sticky lg:top-20">
          <div className="flex items-center justify-between px-2 py-1">
            <h2 className="font-display text-lg font-semibold">我加入的课堂</h2>
            <Badge tone="neutral">{data.classrooms.length}</Badge>
          </div>

          <div
            role="tablist"
            aria-label="我的课堂"
            aria-orientation="vertical"
            className="mt-2 flex gap-1 overflow-x-auto pb-1 lg:flex-col lg:overflow-visible"
          >
            <button
              type="button"
              role="tab"
              aria-selected={selectedClassroomId === "all"}
              onClick={() => setSelectedClassroomId("all")}
              className={`flex min-h-10 shrink-0 items-center gap-2 rounded-xl px-3 text-left text-sm transition lg:w-full ${
                selectedClassroomId === "all"
                  ? "bg-pine-600 font-medium text-white"
                  : "text-muted hover:bg-pine-50 hover:text-pine-700"
              }`}
            >
              <ListTodo className="size-4 shrink-0" />
              <span className="min-w-0 flex-1 truncate">全部课堂</span>
              <span className="text-[10px] opacity-70">{data.assignments.length}</span>
            </button>
            {data.classrooms.map((classroom) => {
              const count = data.assignments.filter(
                (item) => item.assignment.classroomId === classroom.id,
              ).length;
              const selected = selectedClassroomId === classroom.id;
              return (
                <button
                  key={classroom.id}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  onClick={() => setSelectedClassroomId(classroom.id)}
                  className={`flex min-h-10 shrink-0 items-center gap-2 rounded-xl px-3 text-left text-sm transition lg:w-full ${
                    selected
                      ? "bg-pine-600 font-medium text-white"
                      : "text-muted hover:bg-pine-50 hover:text-pine-700"
                  }`}
                >
                  <BookOpen className="size-4 shrink-0" />
                  <span className="min-w-0 flex-1 truncate">{classroom.name}</span>
                  <span className="text-[10px] opacity-70">{count}</span>
                </button>
              );
            })}
          </div>

          <div className="mt-3 border-t border-line px-2 pt-3">
            <div className="flex items-center gap-2">
              <KeyRound className="size-4 text-pine-600" />
              <h3 className="text-sm font-semibold">加入课堂</h3>
            </div>
            <form
              className="mt-3 space-y-2"
              onSubmit={(event) => {
                event.preventDefault();
                join.mutate();
              }}
            >
              <Field label="课堂码">
                <Input
                  value={joinCode}
                  onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                  maxLength={12}
                  placeholder="例如 A2B3C4D5"
                  className="h-9 text-xs"
                  required
                />
              </Field>
              <Button size="sm" className="w-full" disabled={join.isPending || !joinCode.trim()}>
                {join.isPending ? "加入中…" : "加入课堂"}
              </Button>
            </form>
          </div>
        </aside>

        <section className="min-w-0 space-y-5">
          <div>
            <div className="mb-3 flex items-end justify-between gap-3">
              <div>
                <p className="page-eyebrow">{selectedClassroom}</p>
                <h2 className="section-title mt-1">待办任务</h2>
              </div>
              <Badge tone={pendingAssignments.length ? "warning" : "neutral"}>
                {pendingAssignments.length} 项
              </Badge>
            </div>
            {pendingAssignments.length ? (
              <div className="grid gap-4 xl:grid-cols-2">
                {pendingAssignments.map((item) => (
                  <AssignmentTaskCard key={item.assignment.id} item={item} />
                ))}
              </div>
            ) : (
              <Card className="p-8 text-center">
                <CheckCircle2 className="mx-auto size-6 text-pine-600" />
                <h3 className="mt-3 font-display text-xl font-semibold">当前没有待办任务</h3>
                <p className="mt-2 text-sm text-muted">这个课堂的作业已经全部提交。</p>
              </Card>
            )}
          </div>

          {submittedAssignments.length ? (
            <div>
              <div className="mb-3 flex items-end justify-between">
                <h2 className="section-title">已提交与反馈</h2>
                <span className="text-xs text-muted">{submittedAssignments.length} 项</span>
              </div>
              <div className="grid gap-4 xl:grid-cols-2">
                {submittedAssignments.map((item) => (
                  <AssignmentTaskCard key={item.assignment.id} item={item} />
                ))}
              </div>
            </div>
          ) : null}

          {data.assignments.length === 0 ? (
            <Card className="p-10 text-center">
              <h2 className="font-display text-2xl font-semibold">还没有可查看的作业</h2>
              <p className="mt-2 text-sm text-muted">请使用左侧课堂码加入课堂。</p>
            </Card>
          ) : null}
        </section>
      </div>
    </div>
  );
}
