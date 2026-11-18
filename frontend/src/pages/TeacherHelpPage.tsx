import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  BookOpenCheck,
  Bot,
  ClipboardCheck,
  GraduationCap,
  House,
  ShieldCheck,
} from "lucide-react";
import { Link } from "react-router-dom";
import { api } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ErrorState, Loading } from "@/components/ui/loading";

const guides = [
  {
    title: "先建立课堂",
    icon: House,
    text: "设置课程起止时间，复制课堂码，或导入学生名单。启用课堂协作后，可以邀请其他教师使用自己的账号参与审核。",
  },
  {
    title: "再发布作业",
    icon: BookOpenCheck,
    text: "选择课堂和提交形式，设置截止时间、AI 使用政策、诚信声明要求与 1–10 个评分维度。所有维度总分必须为 100。",
  },
  {
    title: "检查学生提交",
    icon: ClipboardCheck,
    text: "在提交审核中打开学生原文和附件。系统会按文档、代码、截图或混合形式给出审核重点，但最终意见由教师填写。",
  },
  {
    title: "使用 AI 建议",
    icon: Bot,
    text: "AI 只生成待核实建议。教师可以采纳或忽略；采纳后的内容进入审核意见，仍需教师核对和保存。",
  },
  {
    title: "完成反馈与申诉",
    icon: ShieldCheck,
    text: "确认诚信线索、填写最终分并发布反馈。学生申诉后，课堂所有者可选择继续复核、采纳或维持原结果。",
  },
  {
    title: "整理课堂材料",
    icon: GraduationCap,
    text: "根据教师确认的共性问题生成备课建议，编辑后导出 Markdown。课堂统计按每个课堂分别计算。",
  },
];

export function TeacherHelpPage() {
  const help = useQuery({
    queryKey: ["teacher-help"],
    queryFn: api.teacherHelp,
  });

  if (help.isLoading) return <Loading label="正在加载使用帮助…" />;
  if (help.isError) {
    return <ErrorState message={help.error.message} retry={() => help.refetch()} />;
  }

  const data = help.data!;
  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <Button asChild variant="ghost" className="-ml-2 text-base">
        <Link to="/teacher">
          <ArrowLeft className="size-5" />
          返回教学工作台
        </Link>
      </Button>

      <section className="rounded-[28px] border border-line bg-white/85 p-6 shadow-card lg:p-8">
        <p className="page-eyebrow">教师帮助</p>
        <h1 className="mt-2 font-display text-4xl font-semibold tracking-tight">
          从课堂设置到反馈导出
        </h1>
        <p className="mt-3 max-w-3xl text-base leading-8 text-muted">
          本页集中说明教师端的主要操作。正式教学中请使用真实课程资料，AI
          建议和诚信线索都需要教师确认后才能进入最终反馈。
        </p>
      </section>

      <div className="grid gap-4 md:grid-cols-2">
        {guides.map((guide, index) => {
          const Icon = guide.icon;
          return (
            <Card key={guide.title} className="p-5">
              <div className="flex items-start gap-4">
                <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-pine-50 text-pine-700">
                  <Icon className="size-5" />
                </span>
                <div>
                  <p className="text-sm font-medium text-pine-700">步骤 {index + 1}</p>
                  <h2 className="mt-1 font-display text-2xl font-semibold">{guide.title}</h2>
                  <p className="mt-2 text-base leading-7 text-muted">{guide.text}</p>
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {data.showDemoCredentials ? (
        <Card>
          <CardHeader>
            <div>
              <p className="page-eyebrow">演示环境</p>
              <CardTitle className="mt-1">示例账号</CardTitle>
            </div>
            <Badge tone="warning">仅演示</Badge>
          </CardHeader>
          <CardContent className="grid gap-3 md:grid-cols-3">
            {[
              ["教师", data.teacherUsername, data.teacherPassword],
              ["学生", data.studentUsername, data.studentPassword],
              ["管理员", data.adminUsername, data.adminPassword],
            ].map(([role, username, password]) => (
              <div key={role} className="rounded-xl bg-stone-50 p-4">
                <strong className="text-sm">{role}</strong>
                <p className="mt-2 font-mono text-sm text-ink">
                  {username} / {password}
                </p>
              </div>
            ))}
          </CardContent>
          <CardContent className="border-t border-line pt-5">
            <div className="mb-3">
              <h3 className="font-display text-xl font-semibold">跨学科仿真账号</h3>
              <p className="mt-1 text-sm text-muted">
                用于检查文科、理科和工科的课堂、作业、提交与申诉数据。
              </p>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              {data.disciplineAccounts.map((account) => (
                <div key={account.username} className="rounded-xl bg-stone-50 p-4">
                  <strong className="text-sm">{account.role}</strong>
                  <p className="mt-1 text-xs text-muted">{account.description}</p>
                  <p className="mt-2 font-mono text-sm text-ink">
                    {account.username} / {account.password}
                  </p>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : null}
    </div>
  );
}
