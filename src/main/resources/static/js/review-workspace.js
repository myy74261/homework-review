/**
 * 教师审核工作台的前端交互。
 *
 * 负责反馈草稿、服务端自动保存、版本历史、逐行对比和响应式导航。
 */
(() => {
  'use strict';

  const SERVER_SAVE_DELAY = 900;
  const LOCAL_SAVE_DELAY = 300;
  const TRIGGER_LABELS = {
    INITIAL: '初始审核版本',
    MANUAL: '教师手动保存',
    RESTORE: '恢复后快照',
  };

  const body = document.body;
  const submissionId = body.dataset.submissionId || '1';
  const reviewApi = `/api/submissions/${submissionId}/review`;
  const draftKey = `review-desk:draft:${submissionId}`;

  const editor = document.getElementById('feedbackEditor');
  const title = document.getElementById('feedbackTitle');
  const characterCount = document.getElementById('characterCount');
  const paragraphCount = document.getElementById('paragraphCount');
  const saveState = document.getElementById('saveState');
  const assistantCount = document.getElementById('assistantCount');
  const mobileBadge = document.getElementById('mobileBadge');
  const workspace = document.getElementById('workspace');
  const contextToggle = document.getElementById('contextToggle');
  const scrim = document.getElementById('scrim');
  const tabs = Array.from(document.querySelectorAll('.mobile-tab'));
  const toast = document.getElementById('toast');

  const versionSave = document.getElementById('versionSave');
  const versionOpen = document.getElementById('versionOpen');
  const versionCountElement = document.getElementById('versionCount');
  const versionDrawer = document.getElementById('versionDrawer');
  const versionScrim = document.getElementById('versionScrim');
  const versionClose = document.getElementById('versionClose');
  const versionListWrap = document.getElementById('versionListWrap');
  const versionList = document.getElementById('versionList');
  const versionEmpty = document.getElementById('versionEmpty');
  const versionDetail = document.getElementById('versionDetail');
  const versionBack = document.getElementById('versionBack');
  const detailTitle = document.getElementById('detailTitle');
  const detailMeta = document.getElementById('detailMeta');
  const diffView = document.getElementById('diffView');
  const restoreButton = document.getElementById('restoreButton');

  let revision = Number(body.dataset.revision || 0);
  let versionCount = Number(body.dataset.versionCount || 0);
  let localTimer = null;
  let serverTimer = null;
  let saveInFlight = null;
  let pendingSave = false;
  let unsynced = false;
  let toastTimer = null;
  let selectedVersionId = null;
  let restoreArmed = false;

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

  function setSaveState(message, tone = '') {
    saveState.textContent = message;
    saveState.classList.toggle('is-warning', tone === 'warning');
    saveState.classList.toggle('is-error', tone === 'error');
  }

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('is-visible');
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove('is-visible'), 2200);
  }

  function persistLocalDraft() {
    try {
      localStorage.setItem(draftKey, JSON.stringify({
        title: title.value,
        content: editor.value,
        revision,
        pending: unsynced,
        savedAt: Date.now(),
      }));
    } catch (error) {
      // 浏览器禁用本机存储时，服务端保存仍可正常工作。
    }
  }

  function scheduleLocalDraft() {
    window.clearTimeout(localTimer);
    localTimer = window.setTimeout(persistLocalDraft, LOCAL_SAVE_DELAY);
  }

  /**
   * 只有上次服务端保存失败的本机草稿才覆盖页面，防止旧内容覆盖新反馈。
   */
  function restoreLocalDraft() {
    try {
      const raw = localStorage.getItem(draftKey);
      if (!raw) {
        return;
      }
      const draft = JSON.parse(raw);
      if (draft.pending === true && typeof draft.content === 'string') {
        editor.value = draft.content;
        if (typeof draft.title === 'string' && draft.title.trim()) {
          title.value = draft.title;
        }
        if (Number.isFinite(Number(draft.revision))) {
          revision = Number(draft.revision);
        }
        unsynced = true;
        setSaveState('有未同步内容', 'warning');
      }
    } catch (error) {
      setSaveState('本机草稿不可用', 'warning');
    }
  }

  function scheduleServerSave(delay = SERVER_SAVE_DELAY) {
    window.clearTimeout(serverTimer);
    serverTimer = window.setTimeout(() => {
      void saveReview();
    }, delay);
  }

  /**
   * 同一时间只允许一个保存请求在途，编辑期间产生的改动会排队再保存。
   */
  function saveReview() {
    if (saveInFlight) {
      pendingSave = true;
      return saveInFlight;
    }

    setSaveState('保存中…');
    saveInFlight = (async () => {
      try {
        const saved = await putReviewWithRetry();
        revision = saved.revision;
        unsynced = false;
        persistLocalDraft();
        setSaveState('已保存到服务器');
        return true;
      } catch (error) {
        unsynced = true;
        persistLocalDraft();
        setSaveState('保存失败，已存本机', 'error');
        return false;
      } finally {
        saveInFlight = null;
        if (pendingSave) {
          pendingSave = false;
          scheduleServerSave(180);
        }
      }
    })();
    return saveInFlight;
  }

  /**
   * 遇到版本冲突时先读取服务端最新版本号，再重试一次当前页面的文字。
   */
  async function putReviewWithRetry() {
    for (let attempt = 0; attempt < 2; attempt++) {
      const response = await fetch(reviewApi, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: title.value,
          content: editor.value,
          baseRevision: revision,
        }),
      });

      if (response.ok) {
        return response.json();
      }
      if (response.status === 409 && attempt === 0) {
        const conflict = await response.json();
        if (typeof conflict.currentRevision === 'number') {
          revision = conflict.currentRevision;
          continue;
        }
      }
      throw new Error(`保存失败：${response.status}`);
    }
    throw new Error('保存失败：重试后仍未成功');
  }

  function formatTime(isoTime) {
    try {
      return new Intl.DateTimeFormat('zh-CN', {
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(isoTime));
    } catch (error) {
      return isoTime;
    }
  }

  function triggerLabel(triggerType) {
    return TRIGGER_LABELS[triggerType] || '审核版本快照';
  }

  function updateVersionBadge() {
    versionCountElement.textContent = String(versionCount);
  }

  function setVersionMode(mode) {
    versionDrawer.dataset.mode = mode;
    versionListWrap.hidden = mode !== 'list';
    versionDetail.hidden = mode !== 'detail';
  }

  function openVersionDrawer() {
    versionDrawer.inert = false;
    versionDrawer.classList.add('is-open');
    versionScrim.classList.add('is-open');
    versionDrawer.setAttribute('aria-hidden', 'false');
    setVersionMode('list');
    void loadVersions();
    versionClose.focus();
  }

  function closeVersionDrawer() {
    versionDrawer.classList.remove('is-open');
    versionScrim.classList.remove('is-open');
    versionDrawer.setAttribute('aria-hidden', 'true');
    versionDrawer.inert = true;
    versionOpen.focus();
  }

  async function loadVersions() {
    versionList.innerHTML = '';
    versionEmpty.hidden = true;
    const loading = document.createElement('li');
    loading.className = 'drawer-empty';
    loading.textContent = '正在加载审核记录…';
    versionList.appendChild(loading);

    try {
      const versions = await fetch(`${reviewApi}/versions`).then(readJson);
      versionCount = versions.length;
      updateVersionBadge();
      renderVersionList(versions);
    } catch (error) {
      versionList.innerHTML = '';
      versionEmpty.hidden = false;
      versionEmpty.textContent = '审核记录加载失败，请稍后再试。';
    }
  }

  function renderVersionList(versions) {
    versionList.innerHTML = '';
    versionEmpty.hidden = versions.length > 0;
    versionEmpty.textContent = '还没有审核记录，点击“存为审核版本”创建第一条。';
    if (versions.length === 0) {
      return;
    }

    versions.forEach((version) => {
      const item = document.createElement('li');
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'version-item';

      const badge = document.createElement('span');
      badge.className = 'version-number';
      badge.textContent = `V${version.versionNumber}`;

      const main = document.createElement('span');
      main.className = 'version-main';
      const label = document.createElement('strong');
      label.textContent = triggerLabel(version.triggerType);
      const meta = document.createElement('small');
      meta.textContent =
        `${formatTime(version.createdAt)} · ${version.characterCount.toLocaleString('zh-CN')} 字`;
      main.append(label, meta);

      const chevron = document.createElement('span');
      chevron.className = 'version-chevron';
      chevron.textContent = '›';
      chevron.setAttribute('aria-hidden', 'true');

      button.append(badge, main, chevron);
      button.addEventListener('click', () => {
        void openVersionDetail(version.id);
      });
      item.appendChild(button);
      versionList.appendChild(item);
    });
  }

  async function openVersionDetail(versionId) {
    selectedVersionId = versionId;
    restoreArmed = false;
    restoreButton.disabled = false;
    restoreButton.textContent = '恢复此反馈版本';
    setVersionMode('detail');
    detailTitle.textContent = '正在加载…';
    detailMeta.textContent = '';
    diffView.textContent = '正在生成对比…';

    try {
      const [detail, diff] = await Promise.all([
        fetch(`${reviewApi}/versions/${versionId}`).then(readJson),
        fetch(`${reviewApi}/versions/${versionId}/diff`).then(readJson),
      ]);
      detailTitle.textContent = `反馈版本 ${detail.versionNumber}`;
      detailMeta.textContent =
        `${triggerLabel(detail.triggerType)} · ${formatTime(detail.createdAt)} · ` +
        `${detail.characterCount.toLocaleString('zh-CN')} 字 · +${diff.added} −${diff.removed}`;
      renderDiff(diff);
    } catch (error) {
      detailTitle.textContent = '审核记录加载失败';
      detailMeta.textContent = '';
      diffView.textContent = '请稍后再试。';
    }
  }

  function renderDiff(diff) {
    diffView.innerHTML = '';
    if (diff.added === 0 && diff.removed === 0) {
      const same = document.createElement('p');
      same.className = 'drawer-empty';
      same.textContent = '当前反馈与这个版本内容一致。';
      diffView.appendChild(same);
      return;
    }

    diff.lines.forEach((line) => {
      const row = document.createElement('div');
      row.className = `diff-line is-${line.type.toLowerCase()}`;
      const marker = document.createElement('span');
      marker.className = 'diff-marker';
      marker.textContent = line.type === 'ADD' ? '+' : line.type === 'REMOVE' ? '−' : '';
      const text = document.createElement('span');
      text.textContent = line.text || ' ';
      row.append(marker, text);
      diffView.appendChild(row);
    });
  }

  async function readJson(response) {
    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`);
    }
    return response.json();
  }

  async function saveVersion() {
    versionSave.disabled = true;
    try {
      if (unsynced) {
        const saved = await saveReview();
        if (!saved) {
          throw new Error('审核意见尚未保存');
        }
      }
      const version = await fetch(`${reviewApi}/versions`, {
        method: 'POST',
      }).then(readJson);
      versionCount += 1;
      updateVersionBadge();
      showToast(`已保存为反馈版本 ${version.versionNumber}`);
      if (versionDrawer.classList.contains('is-open')) {
        await loadVersions();
      }
    } catch (error) {
      showToast('保存审核版本失败，请稍后再试');
    } finally {
      versionSave.disabled = false;
    }
  }

  async function restoreSelectedVersion() {
    if (!selectedVersionId) {
      return;
    }
    if (!restoreArmed) {
      restoreArmed = true;
      restoreButton.textContent = '确认恢复？';
      return;
    }

    restoreButton.disabled = true;
    restoreButton.textContent = '正在恢复…';
    try {
      await fetch(`${reviewApi}/versions/${selectedVersionId}/restore`, {
        method: 'POST',
      }).then(readJson);
      window.location.reload();
    } catch (error) {
      restoreButton.disabled = false;
      restoreButton.textContent = '恢复此反馈版本';
      restoreArmed = false;
      showToast('恢复失败，请稍后再试');
    }
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
      editor.dispatchEvent(new Event('input', { bubbles: true }));
      showToast('审核建议已写入反馈');
    }

    card.classList.add('is-resolved');
    card.querySelector('.card-buttons')?.remove();
    const note = document.createElement('p');
    note.className = 'card-resolved';
    note.textContent = action === 'adopt'
      ? '已写入反馈，保存后将进入审核记录。'
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

  editor.addEventListener('input', () => {
    unsynced = true;
    updateCounts();
    scheduleLocalDraft();
    scheduleServerSave();
  });

  title.addEventListener('input', () => {
    resizeTitle();
    unsynced = true;
    scheduleLocalDraft();
    scheduleServerSave();
  });

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => setMobileView(tab.dataset.target));
  });

  contextToggle.addEventListener('click', () => {
    setContextOpen(!workspace.classList.contains('is-context-open'));
  });
  scrim.addEventListener('click', () => setContextOpen(false));

  versionOpen.addEventListener('click', openVersionDrawer);
  versionClose.addEventListener('click', closeVersionDrawer);
  versionScrim.addEventListener('click', closeVersionDrawer);
  versionBack.addEventListener('click', () => setVersionMode('list'));
  versionSave.addEventListener('click', () => {
    void saveVersion();
  });
  restoreButton.addEventListener('click', () => {
    void restoreSelectedVersion();
  });

  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') {
      return;
    }
    if (versionDrawer.classList.contains('is-open')) {
      closeVersionDrawer();
      return;
    }
    setContextOpen(false);
  });

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden' && unsynced) {
      persistLocalDraft();
    }
  });

  document.querySelectorAll('[data-card-action]').forEach((button) => {
    button.addEventListener('click', () => resolveCard(button));
  });

  restoreLocalDraft();
  resizeTitle();
  updateCounts();
  updateVersionBadge();
})();
