/**
 * 通用页面小交互：复制课堂码。
 */
(() => {
  'use strict';

  document.querySelectorAll('[data-copy-code]').forEach((button) => {
    button.addEventListener('click', async () => {
      const code = button.dataset.copyCode;
      try {
        await navigator.clipboard.writeText(code);
      } catch (error) {
        const helper = document.createElement('textarea');
        helper.value = code;
        helper.style.position = 'fixed';
        helper.style.opacity = '0';
        document.body.appendChild(helper);
        helper.select();
        document.execCommand('copy');
        helper.remove();
      }
      const original = button.textContent;
      button.textContent = '已复制';
      button.classList.add('is-copied');
      window.setTimeout(() => {
        button.textContent = original;
        button.classList.remove('is-copied');
      }, 1400);
    });
  });
})();
