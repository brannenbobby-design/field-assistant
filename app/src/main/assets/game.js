(function (root) {
  'use strict';
  const SIZE = 16;
  const CHAINS = {
    wood: { label: 'Porch lumber', icons: ['🪵', '🪚', '🪵', '🛠️', '🏠'], names: ['Soggy plank', 'Dry plank stack', 'Porch lumber', 'Instant porch kit', 'Trailer-palace plans'] },
    chair: { label: 'Lawn chair', icons: ['🪑', '🪑', '🐊', '👑', '🛋️'], names: ['Busted lawn chair', 'Repaired chair', 'Gator recliner', 'Gator lifeguard throne', 'Mayor’s chair'] },
    leaf: { label: 'Palm fronds', icons: ['🌿', '🍂', '🧹', '🌪️', '🛞'], names: ['Loose palm frond', 'Yard pile', 'Turbo broom', 'Hurricane leaf cannon', 'Storm steering wheel'] },
    cooler: { label: 'Cooler', icons: ['🧊', '🧰', '🛒', '📡', '🐊'], names: ['Empty cooler', 'Bait cooler', 'Rolling bait cooler', 'Satellite bait cooler', 'Gator command center'] },
    flip: { label: 'Flip-flops', icons: ['🩴', '🩴', '🥾', '🏄', '☁️'], names: ['Lone flip-flop', 'Matching pair', 'Gator-proof sandals', 'Storm-surfing sandals', 'Cloud-surfing flip-flops'] }
  };
  const JOBS = [
    { title: 'Fix the Porch', copy: 'The front steps are in the next county. Put the porch back together.', needs: [{ chain: 'wood', level: 1, amount: 1 }, { chain: 'chair', level: 1, amount: 1 }] },
    { title: 'Clear the Bait Shop', copy: 'The shop is buried in palm junk. The cooler is somehow still open.', needs: [{ chain: 'leaf', level: 2, amount: 1 }, { chain: 'cooler', level: 1, amount: 1 }] },
    { title: 'Build Gator Command', copy: 'One last job: outfit the neighborhood gator for hurricane season.', needs: [{ chain: 'cooler', level: 4, amount: 1 }, { chain: 'flip', level: 2, amount: 1 }] }
  ];

  function newState() {
    return {
      board: [
        { chain: 'wood', level: 0 }, { chain: 'chair', level: 0 }, { chain: 'leaf', level: 0 }, { chain: 'wood', level: 0 },
        { chain: 'cooler', level: 0 }, { chain: 'chair', level: 0 }, { chain: 'flip', level: 0 }, { chain: 'wood', level: 0 },
        { chain: 'leaf', level: 0 }, { chain: 'chair', level: 0 }, { chain: 'cooler', level: 0 }, { chain: 'flip', level: 0 },
        null, null, null, null
      ],
      job: 0, coins: 0, merges: 0
    };
  }
  function isSame(a, b) { return !!a && !!b && a.chain === b.chain && a.level === b.level; }
  function merge(board, from, into) {
    if (!board[from] || from === into || !isSame(board[from], board[into])) return { ok: false, reason: 'Only identical pieces merge.' };
    if (board[from].level >= 4) return { ok: false, reason: 'That item is already as bizarre as it gets.' };
    board[into] = { chain: board[from].chain, level: board[from].level + 1 };
    board[from] = null;
    return { ok: true, item: board[into] };
  }
  function counts(board) {
    const result = {};
    board.forEach(item => {
      if (!item) return;
      const key = item.chain + ':' + item.level;
      result[key] = (result[key] || 0) + 1;
    });
    return result;
  }
  function deliveryStatus(state) {
    const have = counts(state.board);
    const needs = JOBS[state.job].needs;
    return needs.map(n => ({ ...n, have: have[n.chain + ':' + n.level] || 0 }));
  }
  function canDeliver(state) { return deliveryStatus(state).every(n => n.have >= n.amount); }
  function deliver(state) {
    if (state.job >= JOBS.length || !canDeliver(state)) return false;
    for (const need of JOBS[state.job].needs) {
      let remaining = need.amount;
      for (let i = 0; i < state.board.length && remaining > 0; i++) {
        const item = state.board[i];
        if (item && item.chain === need.chain && item.level === need.level) {
          state.board[i] = null;
          remaining--;
        }
      }
    }
    state.job++;
    state.coins += 25;
    return true;
  }
  function nextSalvageChain(state) {
    if (state.job >= JOBS.length) return 'wood';
    const need = deliveryStatus(state).filter(n => n.have < n.amount);
    return need.length ? need[Math.floor(Math.random() * need.length)].chain : JOBS[state.job].needs[0].chain;
  }

  const core = { SIZE, CHAINS, JOBS, newState, isSame, merge, counts, deliveryStatus, canDeliver, deliver, nextSalvageChain };
  if (typeof module !== 'undefined' && module.exports) module.exports = core;
  root.FloridaMerge = core;

  if (typeof document === 'undefined') return;
  const $ = id => document.getElementById(id);
  const boardEl = $('board');
  const hintEl = $('hint');
  const toastEl = $('toast');
  const SAVE_KEY = 'storm-cleanup-save-v2';
  let state = restore();
  let selected = null;
  let dragFrom = null;
  let toastTimer;

  function restore() {
    try {
      const saved = JSON.parse(localStorage.getItem(SAVE_KEY));
      if (saved && Array.isArray(saved.board) && saved.board.length === SIZE && Number.isInteger(saved.job)) {
        saved.board = saved.board.map(item => item && CHAINS[item.chain] && item.level >= 0 && item.level <= 4 ? item : null);
        saved.job = Math.max(0, Math.min(saved.job, JOBS.length));
        return saved;
      }
    } catch (_) { /* use a fresh board */ }
    return newState();
  }
  function save() { localStorage.setItem(SAVE_KEY, JSON.stringify(state)); }
  function say(message) {
    toastEl.textContent = message;
    toastEl.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toastEl.classList.remove('show'), 1700);
  }
  function getItemName(item) { return CHAINS[item.chain].names[item.level]; }
  function render() {
    boardEl.replaceChildren();
    state.board.forEach((item, index) => {
      const cell = document.createElement('div');
      cell.className = 'cell' + (item ? ' filled' : '') + (selected === index ? ' selected' : '');
      cell.dataset.index = String(index);
      cell.setAttribute('role', 'gridcell');
      if (item) {
        const icon = document.createElement('span'); icon.className = 'emoji'; icon.textContent = CHAINS[item.chain].icons[item.level];
        const level = document.createElement('span'); level.className = 'level'; level.textContent = 'T' + (item.level + 1);
        const name = document.createElement('span'); name.className = 'name'; name.textContent = getItemName(item);
        cell.append(icon, level, name);
        cell.setAttribute('aria-label', getItemName(item));
        cell.draggable = false;
        cell.addEventListener('pointerdown', event => beginDrag(event, index));
        cell.addEventListener('pointercancel', cancelDrag);
      } else {
        cell.setAttribute('aria-label', 'Empty slot. Pieces cannot be moved here.');
      }
      cell.addEventListener('click', () => tapCell(index));
      boardEl.appendChild(cell);
    });
    renderJob();
    $('coins').textContent = String(state.coins);
    const used = state.board.filter(Boolean).length;
    $('board-count').textContent = used + ' / ' + SIZE;
    $('scrap-button').disabled = selected === null || !state.board[selected];
    $('salvage-button').disabled = !state.board.includes(null) || state.job >= JOBS.length;
    const ready = state.job < JOBS.length && canDeliver(state);
    $('deliver-button').disabled = !ready;
    $('deliver-button').textContent = state.job >= JOBS.length ? 'NEIGHBORHOOD RESTORED!' : ready ? 'DELIVER JOB · +25 🪙' : 'DELIVER JOB';
    if (selected !== null && state.board[selected]) hintEl.textContent = 'Selected: ' + getItemName(state.board[selected]) + ' · choose an identical twin.';
    else hintEl.textContent = state.job >= JOBS.length ? 'All jobs done. Florida is mostly under control.' : 'Drag one piece onto its twin to merge.';
  }
  function renderJob() {
    const done = state.job >= JOBS.length;
    const job = done ? JOBS[JOBS.length - 1] : JOBS[state.job];
    $('job-kicker').textContent = done ? 'ALL JOBS COMPLETE' : 'CURRENT JOB · ' + (state.job + 1) + ' OF ' + JOBS.length;
    $('job-title').textContent = done ? 'Coast is looking good!' : job.title;
    $('job-copy').textContent = done ? 'You saved the block, the bait shop, and the neighborhood gator.' : job.copy;
    $('stars').textContent = '★'.repeat(Math.min(state.job, 3)) + '☆'.repeat(Math.max(0, 3 - state.job));
    $('requirements').replaceChildren();
    if (done) {
      const badge = document.createElement('span'); badge.className = 'requirement ready'; badge.textContent = '✓ Cleanup complete'; $('requirements').appendChild(badge); return;
    }
    deliveryStatus(state).forEach(need => {
      const badge = document.createElement('span');
      badge.className = 'requirement' + (need.have >= need.amount ? ' ready' : '');
      const item = { chain: need.chain, level: need.level };
      badge.textContent = CHAINS[need.chain].icons[need.level] + ' ' + getItemName(item) + '  ' + Math.min(need.have, need.amount) + '/' + need.amount;
      $('requirements').appendChild(badge);
    });
  }
  function setSelection(index) {
    selected = state.board[index] ? index : null;
    render();
  }
  function tapCell(index) {
    if (dragFrom !== null) return;
    const target = state.board[index];
    if (selected === null || selected === index) { setSelection(selected === index ? null : index); return; }
    const source = state.board[selected];
    if (isSame(source, target)) tryMerge(selected, index);
    else say(target ? 'Those are different. Match the exact same piece and tier.' : 'Empty slots stay empty. Only matching pieces merge.');
  }
  function tryMerge(from, into) {
    const outcome = merge(state.board, from, into);
    if (!outcome.ok) { say(outcome.reason); return false; }
    selected = null; state.merges++; state.coins += 1;
    save(); render();
    say('Merged into ' + getItemName(outcome.item) + '!');
    return true;
  }
  function beginDrag(event, index) {
    if (event.button !== undefined && event.button !== 0) return;
    dragFrom = index;
    event.currentTarget.setPointerCapture?.(event.pointerId);
    event.currentTarget.classList.add('dragging');
    event.preventDefault();
  }
  document.addEventListener('pointerup', event => {
    if (dragFrom === null) return;
    const from = dragFrom;
    const target = document.elementFromPoint(event.clientX, event.clientY)?.closest('.cell');
    const into = target ? Number(target.dataset.index) : -1;
    dragFrom = null;
    document.querySelectorAll('.dragging').forEach(el => el.classList.remove('dragging'));
    if (into >= 0 && from !== into) tryMerge(from, into);
    else if (into === from) tapCell(from);
    else say('Drop onto an identical piece to merge.');
    event.preventDefault();
  });
  function cancelDrag() { dragFrom = null; document.querySelectorAll('.dragging').forEach(el => el.classList.remove('dragging')); }

  $('salvage-button').addEventListener('click', () => {
    const slot = state.board.indexOf(null);
    if (slot < 0) { say('The pile is full. Merge or scrap something first.'); return; }
    const chain = nextSalvageChain(state);
    state.board[slot] = { chain, level: 0 };
    save(); render();
    say('Found: ' + getItemName(state.board[slot]));
  });
  $('scrap-button').addEventListener('click', () => {
    if (selected === null || !state.board[selected]) return;
    const item = state.board[selected];
    state.board[selected] = null; selected = null; state.coins += 2;
    save(); render(); say('Scrapped ' + getItemName(item) + ' for 2 coins.');
  });
  $('deliver-button').addEventListener('click', () => {
    if (!deliver(state)) { say('Build every item shown on the job card first.'); return; }
    selected = null; save(); render();
    say(state.job >= JOBS.length ? 'Cleanup complete! You are the hero of this county.' : 'Job complete! The neighborhood looks a little less Florida.');
  });
  $('start-button').addEventListener('click', () => $('tutorial').classList.add('hidden'));
  if (localStorage.getItem('storm-cleanup-tutorial-v2') === 'seen') $('tutorial').classList.add('hidden');
  $('start-button').addEventListener('click', () => localStorage.setItem('storm-cleanup-tutorial-v2', 'seen'));
  render();
})(typeof window === 'undefined' ? globalThis : window);
