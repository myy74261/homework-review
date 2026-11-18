import { chromium } from "@playwright/test";

const baseUrl = process.env.SMOKE_BASE_URL;
const username = process.env.SMOKE_USERNAME;
const password = process.env.SMOKE_PASSWORD;

if (!baseUrl || !username || !password) {
  throw new Error("SMOKE_BASE_URL、SMOKE_USERNAME 和 SMOKE_PASSWORD 必须提供");
}

const browser = await chromium.launch({ channel: "msedge", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto(`${baseUrl}/login`);
  await page.getByLabel("账号").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await page.waitForURL(/\/app\/(admin|teacher|student)$/);
  const url = page.url();
  if (url.endsWith("/app/admin")) {
    await page.getByRole("heading", { name: "教师注册审核" }).waitFor();
    await page.getByRole("tab", { name: "待审核" }).waitFor();
  } else if (url.endsWith("/app/teacher")) {
    await page.getByRole("heading", { name: "教学工作台" }).waitFor();
    const dashboard = await page.evaluate(async () => {
      const response = await fetch("/api/app/teacher/dashboard");
      return response.json();
    });
    const submissionId = dashboard.submissions?.[0]?.id;
    if (submissionId) {
      await page.goto(`${baseUrl}/app/reviews/${submissionId}`);
      await page.getByRole("heading", { name: "预审建议" }).waitFor();
    }
  } else {
    await page.getByRole("heading", { name: "我的作业" }).waitFor();
  }
  if (!url.endsWith("/app/admin")) {
    await page.goto(`${baseUrl}/app/account`);
    await page.getByRole("heading", { name: "个人中心" }).waitFor();
    await page.getByRole("heading", { name: "AI 接口设置" }).waitFor({ state: "detached" });
  }
  console.log(`Production React smoke test passed: ${new URL(url).pathname}`);
} finally {
  await browser.close();
}
