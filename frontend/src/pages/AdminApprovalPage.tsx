import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, ShieldCheck, UserRoundCheck, UserRoundX } from "lucide-react";
import { useState } from "react";
import { api } from "@/lib/api";
import type { TeacherApplication } from "@/lib/types";
import { formatDate } from "@/lib/utils";
import { useAppStore } from "@/store/app-store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/field";
import { ErrorState, Loading } from "@/components/ui/loading";

type ApprovalFilter = "PENDING" | "ALL" | "APPROVED" | "REJECTED";

const statusLabels: Record<TeacherApplication["approvalStatus"], string> = {
  PENDING: "待审核",
  APPROVED: "已通过",
  REJECTED: "已拒绝",
};

export function AdminApprovalPage() {
  const queryClient = useQueryClient();
  const notify = useAppStore((state) => state.notify);
  const [filter, setFilter] = useState<ApprovalFilter>("PENDING");
  const [notes, setNotes] = useState<Record<number, string>>({});
  const applications = useQuery({
    queryKey: ["teacher-applications", filter],
    queryFn: () => api.teacherApplications(filter),
  });

  const review = useMutation({
    mutationFn: ({ teacherId, status }: { teacherId: number; status: "APPROVED" | "REJECTED" }) =>
      api.reviewTeacherApplication(teacherId, {
        status,
        note: notes[teacherId]?.trim() ?? "",
      }),
    onSuccess: async (teacher) => {
      notify(
        `${teacher.displayName} 的教师申请已${teacher.approvalStatus === "APPROVED" ? "通过" : "拒绝"}。`,
        "success",
      );
      await queryClient.invalidateQueries({ queryKey: ["teacher-applications"] });
    },
    onError: (error: Error) => notify(error.message, "error"),
  });

  if (applications.isLoading) return <Loading label="正在读取教师申请…" />;
  if (applications.isError) {
    return <ErrorState message={applications.error.message} retry={() => applications.refetch()} />;
  }

  const data = applications.data!;
  return (
    <div className="space-y-5">
      <section className="rounded-[28px] border border-line bg-white/85 p-6 shadow-card lg:p-8">
        <p className="page-eyebrow">平台治理</p>
        <div className="mt-2 flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <h1 className="font-display text-4xl font-semibold tracking-tight">教师注册审核</h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-muted">
              教师账号通过审核后才能创建课堂。学生账号注册后可直接使用，已有教师账号保持有效。
            </p>
          </div>
          <Badge tone={data.pendingCount ? "warning" : "default"}>
            {data.pendingCount} 项待处理
          </Badge>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-3">
        {[
          { label: "待审核", value: data.pendingCount, icon: ShieldCheck },
          { label: "已通过", value: data.approvedCount, icon: UserRoundCheck },
          { label: "已拒绝", value: data.rejectedCount, icon: UserRoundX },
        ].map((metric) => {
          const Icon = metric.icon;
          return (
            <Card key={metric.label} className="flex items-center justify-between p-5">
              <div>
                <span className="text-sm text-muted">{metric.label}</span>
                <strong className="mt-1 block font-display text-3xl">{metric.value}</strong>
              </div>
              <Icon className="size-6 text-pine-600" />
            </Card>
          );
        })}
      </section>

      <div
        role="tablist"
        aria-label="教师申请状态"
        className="flex gap-1 overflow-x-auto rounded-xl border border-line bg-white/85 p-1.5"
      >
        {(
          [
            ["PENDING", "待审核"],
            ["ALL", "全部"],
            ["APPROVED", "已通过"],
            ["REJECTED", "已拒绝"],
          ] as const
        ).map(([value, label]) => (
          <button
            key={value}
            type="button"
            role="tab"
            aria-selected={filter === value}
            onClick={() => setFilter(value)}
            className={`min-h-10 shrink-0 rounded-lg px-4 text-sm ${
              filter === value ? "bg-pine-600 font-medium text-white" : "text-muted"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        {data.teachers.map((teacher) => (
          <Card key={teacher.id} className="p-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="font-display text-2xl font-semibold">{teacher.displayName}</h2>
                <p className="mt-1 text-sm text-muted">
                  {teacher.username}
                  {teacher.department ? ` · ${teacher.department}` : ""}
                  {teacher.professionalTitle ? ` · ${teacher.professionalTitle}` : ""}
                </p>
              </div>
              <Badge
                tone={
                  teacher.approvalStatus === "APPROVED"
                    ? "default"
                    : teacher.approvalStatus === "REJECTED"
                      ? "danger"
                      : "warning"
                }
              >
                {statusLabels[teacher.approvalStatus]}
              </Badge>
            </div>
            <p className="mt-3 text-sm text-muted">
              申请时间：{formatDate(teacher.createdAt)}
              {teacher.email ? ` · ${teacher.email}` : ""}
            </p>
            {teacher.reviewNote ? (
              <p className="mt-3 rounded-xl bg-stone-50 p-3 text-sm leading-6 text-muted">
                审核说明：{teacher.reviewNote}
              </p>
            ) : null}
            {teacher.approvalStatus === "PENDING" ? (
              <div className="mt-4 space-y-3 border-t border-line pt-4">
                <Textarea
                  rows={2}
                  value={notes[teacher.id] ?? ""}
                  onChange={(event) =>
                    setNotes((current) => ({
                      ...current,
                      [teacher.id]: event.target.value,
                    }))
                  }
                  placeholder="审核说明；拒绝时必须填写原因"
                />
                <div className="flex gap-2">
                  <Button
                    disabled={review.isPending}
                    onClick={() => review.mutate({ teacherId: teacher.id, status: "APPROVED" })}
                  >
                    <Check className="size-4" />
                    通过申请
                  </Button>
                  <Button
                    variant="destructive"
                    disabled={review.isPending || !notes[teacher.id]?.trim()}
                    onClick={() => review.mutate({ teacherId: teacher.id, status: "REJECTED" })}
                  >
                    <UserRoundX className="size-4" />
                    拒绝申请
                  </Button>
                </div>
              </div>
            ) : null}
          </Card>
        ))}
      </div>

      {data.teachers.length === 0 ? (
        <Card className="p-10 text-center">
          <UserRoundCheck className="mx-auto size-7 text-pine-600" />
          <CardTitle className="mt-3">当前没有此状态的教师申请</CardTitle>
        </Card>
      ) : null}
    </div>
  );
}
