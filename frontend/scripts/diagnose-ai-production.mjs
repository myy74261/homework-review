import { chromium } from "@playwright/test";

const baseUrl = process.env.DIAGNOSE_BASE_URL;
const username = process.env.DIAGNOSE_USERNAME;
const password = process.env.DIAGNOSE_PASSWORD;

if (!baseUrl || !username || !password) {
  throw new Error("DIAGNOSE_BASE_URL、DIAGNOSE_USERNAME 和 DIAGNOSE_PASSWORD 必须提供");
}

const browser = await chromium.launch({ channel: "msedge", headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto(`${baseUrl}/login`);
  await page.getByLabel("账号").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录", exact: true }).click();
  await page.waitForURL(/\/app\/teacher$/);

  const dashboard = await page.evaluate(async () => {
    const response = await fetch("/api/app/teacher/dashboard");
    return response.json();
  });
  const submission = dashboard.submissions?.find((item) => item.reviewStatus !== "REVIEWED");
  if (!submission) {
    throw new Error("没有可复现 AI 请求的待审核提交");
  }

  await page.goto(`${baseUrl}/app/reviews/${submission.id}`);
  const button = page.getByRole("button", { name: /开始预审|重新预审/ });
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes(`/reviews/${submission.id}/ai-review`) &&
      response.request().method() === "POST",
    { timeout: 60_000 },
  );
  await button.click();
  const response = await responsePromise;
  console.log(`AI_HTTP_STATUS=${response.status()}`);
  console.log(`AI_HTTP_BODY=${await response.text()}`);
  await page.waitForTimeout(1500);
  await page.screenshot({
    path: "test-results/ai-review-diagnosis.png",
    fullPage: true,
  });
} finally {
  await browser.close();
}
