/**
 * P0 教师审核工作台的页面交互。
 *
 * 负责文字统计、审核提示采纳和响应式导航。自动保存与反馈版本将在 P1 接入。
 */
(() => {
  'use strict';

  const body = document.body;
  const editor = document.getElementById('feedbackEditor');
  const title = document.getElementById('feedbackTitle');
  const characterCount = document.getElementById('characterCount');
  const paragraphCount = document.getElementById('paragraphCount');
  const assistantCount = document.getElementById('assistantCount');
  const mobileBadge = document.getElementById('mobileBadge');
  const workspace = document.getElementById('workspace');
  const contextToggle = document.getElementById('contextToggle');
  const scrim = document.getElementById('scrim');
  const tabs = Array.from(document.querySelectorAll('.mobile-tab'));
  const toast = document.getElementById('toast');
  let toastTimer = null;

  function resizeTitle() {
    title.style.height = 'auto';
    title.style.height = `${title.scrollHeight}px`;
  }

  function updateCounts() {
    const characters = editor.value.replace(/\s/g, '').length;
    const paragraphs = editor.value
      .split(/\n\s*\n/)
      .filter((paragraph) => paragraph.trim()).length;
    characterCount.textContent = characters.toLocaleString('zh-CN');
    paragraphCount.textContent = String(paragraphs);
  }

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('is-visible');
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove('is-visible'), 2200);
  }

  function setMobileView(view) {
    body.dataset.view = view;
    tabs.forEach((tab) => {
      const selected = tab.dataset.target === view;
      tab.classList.toggle('is-active', selected);
      tab.setAttribute('aria-selected', String(selected));
    });
    if (view === 'assistant' && mobileBadge) {
      mobileBadge.hidden = true;
    }
  }

  function setContextOpen(open) {
    workspace.classList.toggle('is-context-open', open);
    contextToggle.setAttribute('aria-expanded', String(open));
  }

  function resolveCard(button) {
    const card = button.closest('[data-assistant-card]');
    if (!card || card.classList.contains('is-resolved')) {
      return;
    }

    const action = button.dataset.cardAction;
    const suggestion = button.dataset.suggestion?.trim();
    if (action === 'adopt' && suggestion) {
      const separation = editor.value.trim() ? '\n\n' : '';
      editor.value = `${editor.value}${separation}${suggestion}`;
      updateCounts();
      showToast('审核建议已写入反馈');
    }

    card.classList.add('is-resolved');
    card.querySelector('.card-buttons')?.remove();
    const note = document.createElement('p');
    note.className = 'card-resolved';
    note.textContent = action === 'adopt'
      ? '已写入反馈，P1 接入保存后会自动进入审核记录。'
      : '已记录教师核验结果。';
    card.appendChild(note);

    const remaining = document.querySelectorAll('[data-assistant-card]:not(.is-resolved)').length;
    assistantCount.textContent = String(remaining);
    assistantCount.classList.toggle('is-empty', remaining === 0);
    assistantCount.setAttribute('aria-label', `${remaining} 条审核提示`);
    if (mobileBadge) {
      mobileBadge.textContent = String(remaining);
      mobileBadge.hidden = remaining === 0 || body.dataset.view === 'assistant';
    }
  }

  title.addEventListener('input', () => {
    resizeTitle();
    showToast('当前为 P0 演示，内容将在 P1 自动保存');
  });

  editor.addEventListener('input', () => {
    updateCounts();
    showToast('当前为 P0 演示，内容将在 P1 自动保存');
  });

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => setMobileView(tab.dataset.target));
  });

  contextToggle.addEventListener('click', () => {
    setContextOpen(!workspace.classList.contains('is-context-open'));
  });

  scrim.addEventListener('click', () => setContextOpen(false));

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      setContextOpen(false);
    }
  });

  document.querySelectorAll('[data-card-action]').forEach((button) => {
    button.addEventListener('click', () => resolveCard(button));
  });

  resizeTitle();
  updateCounts();
})();

