/**
 * 学生在页面内异步提交作业，避开部分内嵌浏览器对整页 POST 跳转的限制。
 */
(() => {
  'use strict';

  const form = document.getElementById('studentSubmissionForm');
  if (!form) {
    return;
  }

  const button = document.getElementById('submissionSubmitButton');
  const status = document.getElementById('submissionAjaxStatus');
  const originalLabel = button.textContent;

  function showStatus(message, tone) {
    status.textContent = message;
    status.classList.remove('is-error', 'is-success');
    status.classList.add(tone === 'error' ? 'is-error' : 'is-success');
    status.hidden = false;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) {
      return;
    }

    button.disabled = true;
    button.textContent = '正在提交…';
    status.hidden = true;

    try {
      const response = await fetch(form.dataset.ajaxAction, {
        method: 'POST',
        body: new FormData(form),
        credentials: 'same-origin',
        headers: {
          'X-Requested-With': 'XMLHttpRequest',
        },
      });
      const result = await response.json().catch(() => null);
      if (!response.ok || !result?.success) {
        throw new Error(result?.message || '提交失败，请检查填写内容后重试。');
      }

      showStatus(result.message || '作业已提交。', 'success');
      window.location.assign(result.redirectUrl);
    } catch (error) {
      const message = error instanceof TypeError
        ? '浏览器拦截了提交请求。请复制当前内容，在普通 Chrome 或 Edge 中重新提交。'
        : error.message;
      showStatus(message, 'error');
    } finally {
      button.disabled = false;
      button.textContent = originalLabel;
    }
  });
})();
