/**
 * P3 备课建议编辑与自动保存。
 */
(() => {
  'use strict';

  const assignmentId = document.body.dataset.assignmentId;
  const api = `/api/assignments/${assignmentId}/lesson-plan`;
  const draftKey = `review-desk:lesson-plan:${assignmentId}`;

  const title = document.getElementById('lessonTitle');
  const editor = document.getElementById('lessonEditor');
  const saveState = document.getElementById('lessonSaveState');
  const charCount = document.getElementById('lessonCharCount');
  const regenerateButton = document.getElementById('regenerateButton');
  const toast = document.getElementById('toast');

  let revision = Number(document.body.dataset.revision || 0);
  let saveTimer = null;
  let localTimer = null;
  let saveInFlight = null;
  let pendingSave = false;
  let unsynced = false;
  let toastTimer = null;

  function resizeTitle() {
    title.style.height = 'auto';
    title.style.height = `${title.scrollHeight}px`;
  }

  function updateCounts() {
    charCount.textContent = editor.value.replace(/\s/g, '').length.toLocaleString('zh-CN');
  }

  function setState(message, tone = '') {
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

  function saveLocal(pending) {
    try {
      localStorage.setItem(draftKey, JSON.stringify({
        title: title.value,
        content: editor.value,
        revision,
        pending,
      }));
    } catch (error) {
      // 本机存储失败时继续使用服务端自动保存。
    }
  }

  function restoreLocal() {
    try {
      const raw = localStorage.getItem(draftKey);
      if (!raw) {
        return;
      }
      const draft = JSON.parse(raw);
      if (draft.pending && typeof draft.content === 'string') {
        editor.value = draft.content;
        title.value = draft.title || title.value;
        revision = Number(draft.revision || revision);
        unsynced = true;
        setState('发现未同步草稿', 'warning');
      }
    } catch (error) {
      setState('本机草稿不可用', 'warning');
    }
  }

  function scheduleSave(delay = 900) {
    window.clearTimeout(saveTimer);
    saveTimer = window.setTimeout(() => {
      void save();
    }, delay);
  }

  function save() {
    if (saveInFlight) {
      pendingSave = true;
      return saveInFlight;
    }
    setState('保存中…');
    saveInFlight = (async () => {
      try {
        let response;
        for (let attempt = 0; attempt < 2; attempt++) {
          response = await fetch(api, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              title: title.value,
              content: editor.value,
              baseRevision: revision,
            }),
          });
          if (response.ok) {
            break;
          }
          if (response.status === 409 && attempt === 0) {
            const conflict = await response.json();
            revision = Number(conflict.currentRevision);
            continue;
          }
          throw new Error(`保存失败：${response.status}`);
        }
        const saved = await response.json();
        revision = saved.revision;
        unsynced = false;
        saveLocal(false);
        setState('已保存到服务器');
        return true;
      } catch (error) {
        unsynced = true;
        saveLocal(true);
        setState('保存失败，已存本机', 'error');
        return false;
      } finally {
        saveInFlight = null;
        if (pendingSave) {
          pendingSave = false;
          scheduleSave(180);
        }
      }
    })();
    return saveInFlight;
  }

  async function regenerate() {
    if (!window.confirm('重新生成会覆盖当前编辑内容，是否继续？')) {
      return;
    }
    regenerateButton.disabled = true;
    try {
      const plan = await fetch(`${api}/regenerate`, { method: 'POST' }).then((response) => {
        if (!response.ok) {
          throw new Error(`生成失败：${response.status}`);
        }
        return response.json();
      });
      title.value = plan.title;
      editor.value = plan.content;
      revision = plan.revision;
      unsynced = false;
      saveLocal(false);
      resizeTitle();
      updateCounts();
      setState('已按最新问题生成');
      showToast('备课建议已重新生成');
    } catch (error) {
      setState('重新生成失败', 'error');
    } finally {
      regenerateButton.disabled = false;
    }
  }

  function onEdit() {
    unsynced = true;
    updateCounts();
    window.clearTimeout(localTimer);
    localTimer = window.setTimeout(() => saveLocal(true), 250);
    scheduleSave();
  }

  title.addEventListener('input', () => {
    resizeTitle();
    onEdit();
  });
  editor.addEventListener('input', onEdit);
  regenerateButton.addEventListener('click', () => {
    void regenerate();
  });
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden' && unsynced) {
      saveLocal(true);
    }
  });

  restoreLocal();
  resizeTitle();
  updateCounts();
})();
