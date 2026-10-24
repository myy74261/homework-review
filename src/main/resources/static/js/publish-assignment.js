/**
 * 动态评分量规编辑。
 */
(() => {
  'use strict';

  const list = document.getElementById('rubricFormList');
  const addButton = document.getElementById('addRubric');
  const total = document.getElementById('rubricTotal');
  const MAX_RUBRICS = 10;

  function rows() {
    return Array.from(list.querySelectorAll('[data-rubric-row]'));
  }

  function updateTotal() {
    const sum = rows()
      .map((row) => Number(row.querySelector('[data-points]').value || 0))
      .reduce((left, right) => left + right, 0);
    total.textContent = String(sum);
    total.classList.toggle('is-invalid', sum !== 100);
  }

  function reindex() {
    rows().forEach((row, index) => {
      row.querySelector('[data-rubric-index]').textContent = String(index + 1);
      row.querySelectorAll('[name]').forEach((input) => {
        input.name = input.name.replace(/rubrics\[\d+\]/, `rubrics[${index}]`);
        input.id = input.id.replace(/rubrics\d+/, `rubrics${index}`);
      });
    });
  }

  function addRow() {
    if (rows().length >= MAX_RUBRICS) {
      return;
    }
    const index = rows().length;
    const row = document.createElement('div');
    row.className = 'rubric-form-row';
    row.dataset.rubricRow = '';
    row.innerHTML = `
      <span class="rubric-order" data-rubric-index>${index + 1}</span>
      <label class="field"><span>维度名称</span>
        <input type="text" name="rubrics[${index}].name" id="rubrics${index}.name">
      </label>
      <label class="field points-field"><span>分值</span>
        <input type="number" min="1" max="100" data-points
               name="rubrics[${index}].points" id="rubrics${index}.points" value="0">
      </label>
      <label class="field"><span>观察说明</span>
        <input type="text" name="rubrics[${index}].description" id="rubrics${index}.description">
      </label>
      <button class="remove-rubric" type="button" aria-label="删除评分维度">×</button>
    `;
    list.appendChild(row);
    bindRow(row);
    updateTotal();
  }

  function bindRow(row) {
    row.querySelector('[data-points]').addEventListener('input', updateTotal);
    row.querySelector('.remove-rubric').addEventListener('click', () => {
      if (rows().length === 1) {
        return;
      }
      row.remove();
      reindex();
      updateTotal();
    });
  }

  rows().forEach(bindRow);
  addButton.addEventListener('click', addRow);
  updateTotal();
})();
