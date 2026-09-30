(function (root) {
  'use strict';
  if (typeof document === 'undefined') return;
  const G = root.FloridaMatch3;
  const $ = id => document.getElementById(id);
  const boardEl = $('board'), hintEl = $('hint'), toastEl = $('toast');
  const SAVE_KEY = 'storm-cleanup-match3-v3';
  let selected = null, gesture = null, suppressClick = false, toastTimer;
  let previousTiles = new WeakSet();

  function loadState() {
    try {
      const saved = JSON.parse(localStorage.getItem(SAVE_KEY));
      if (saved && Array.isArray(saved.board) && saved.board.length === G.SIZE && Number.isInteger(saved.job) && saved.job >= 0 && saved.job <= G.JOBS.length) {
        saved.board = saved.board.map(item => item && G.CHAINS[item.chain] && (!item.power || G.POWER[item.power]) ? { chain: item.chain, power: item.power || null } : null);
        saved.cleared ||= {};
        saved.score ||= 0; saved.coins ||= 0;
        if (saved.job < G.JOBS.length) { saved.moves = Math.max(0, saved.moves || 0); saved.failed = saved.moves === 0 && !saved.won; }
        return saved;
      }
    } catch (_) { /* start a new board */ }
    return G.newState();
  }
  let state = loadState();
  function save() { localStorage.setItem(SAVE_KEY, JSON.stringify(state)); }
  function say(message) {
    toastEl.textContent = message; toastEl.classList.add('show');
    clearTimeout(toastTimer); toastTimer = setTimeout(() => toastEl.classList.remove('show'), 1800);
  }
  function stageIndex() { return Math.min(state.job, G.JOBS.length - 1); }
  function itemName(item) { return item.power ? G.POWER[item.power].label : G.CHAINS[item.chain].names[stageIndex()]; }
  function itemIcon(item) { return item.power ? G.POWER[item.power].icon : G.CHAINS[item.chain].icons[stageIndex()]; }
  function tallyPower() { return state.board.filter(x => x?.power).length; }
  function renderJob() {
    const done = state.job >= G.JOBS.length, job = done ? G.JOBS[G.JOBS.length - 1] : G.JOBS[state.job];
    $('job-kicker').textContent = done ? 'ALL JOBS COMPLETE' : `CURRENT JOB · ${state.job + 1} OF ${G.JOBS.length}`;
    $('job-title').textContent = done ? 'Coast is looking good!' : job.title;
    $('job-copy').textContent = done ? 'You saved the block, the bait shop, and the neighborhood gator.' : job.copy;
    const earned = state.won ? (state.moves >= Math.ceil(job.moves * .5) ? 3 : state.moves >= 3 ? 2 : 1) : Math.min(state.job, 3);
    $('stars').textContent = '★'.repeat(earned) + '☆'.repeat(3 - earned);
    $('requirements').replaceChildren();
    if (done) {
      const badge = document.createElement('span'); badge.className = 'requirement ready'; badge.textContent = '✓ Cleanup complete'; $('requirements').appendChild(badge); return;
    }
    G.jobStatus(state).forEach(need => {
      const badge = document.createElement('span'); badge.className = 'requirement' + (need.have >= need.amount ? ' ready' : '');
      const item = { chain: need.chain, power: null }, icon = G.CHAINS[need.chain].icons[stageIndex()];
      badge.textContent = `${icon} ${G.CHAINS[need.chain].names[stageIndex()]} ${need.have}/${need.amount}`;
      $('requirements').appendChild(badge);
    });
  }
  function render() {
    boardEl.replaceChildren();
    state.board.forEach((item, index) => {
      const cell = document.createElement('button');
      cell.type = 'button'; cell.className = 'cell' + (item ? ' filled' : '') + (item && !previousTiles.has(item) ? ' dropping' : '') + (selected === index ? ' selected' : '') + (item?.power ? ' powered' : '');
      cell.dataset.index = String(index); cell.setAttribute('role', 'gridcell');
      if (item) {
        cell.style.setProperty('--tile-color', G.CHAINS[item.chain].colors[stageIndex()]);
        const icon = document.createElement('span'); icon.className = 'emoji'; icon.textContent = itemIcon(item);
        const name = document.createElement('span'); name.className = 'name'; name.textContent = item.power ? G.POWER[item.power].label : G.CHAINS[item.chain].names[stageIndex()];
        cell.append(icon, name); cell.setAttribute('aria-label', itemName(item) + (item.power ? ', power-up, tap to activate' : ''));
      } else {
        cell.classList.add('empty'); cell.setAttribute('aria-label', 'Empty board space');
      }
      cell.addEventListener('pointerdown', event => beginGesture(event, index));
      cell.addEventListener('click', () => { if (suppressClick) { suppressClick = false; return; } tapCell(index); });
      boardEl.appendChild(cell);
    });
    renderJob();
    $('coins').textContent = String(state.coins);
    $('score').textContent = String(state.score);
    $('moves').textContent = String(state.moves);
    $('board-count').textContent = `${G.SIZE} tiles`;
    $('restart-button').disabled = state.job >= G.JOBS.length;
    const action = $('job-button');
    action.disabled = !(state.won || state.failed || state.job >= G.JOBS.length);
    action.textContent = state.job >= G.JOBS.length ? 'PLAY AGAIN' : state.failed ? 'RETRY JOB' : state.won ? 'JOB COMPLETE · NEXT' : 'JOB IN PROGRESS';
    previousTiles = new WeakSet(state.board.filter(Boolean));
    if (state.won) hintEl.textContent = 'Job cleared! Leftover moves boost your stars.';
    else if (state.failed) hintEl.textContent = 'Out of moves. Restart the job and take another run.';
    else if (selected !== null && state.board[selected]) hintEl.textContent = state.board[selected].power ? 'Power-up selected. Tap again or swap it to fire.' : 'Selected. Swap it with a neighbor to make a match.';
    else hintEl.textContent = 'Match 4 for a line blast. Match 5 for a rainbow gator.';
  }
  function tapCell(index) {
    const item = state.board[index];
    if (!item) { selected = null; render(); return; }
    if (item.power) {
      const result = G.activate(state, index);
      if (result.ok) { selected = null; save(); render(); say(`${G.POWER[result.power].label} fired!`); }
      else say(result.reason);
      return;
    }
    if (selected === null || selected === index) { selected = selected === index ? null : index; render(); return; }
    if (G.adjacent(selected, index)) trySwap(selected, index);
    else { selected = index; render(); say('Swap neighboring pieces only.'); }
  }
  function trySwap(from, to) {
    const before = tallyPower();
    const result = G.swap(state, from, to);
    if (!result.ok) { say(result.reason); selected = null; render(); return; }
    selected = null; save(); render();
    const after = tallyPower();
    if (after > before) say('Power-up earned! Tap it to fire, or swap it into another match.');
    else if (result.matched >= 5) say('Big match! Watch the cascade.');
    else say(result.power ? 'Power-up fired!' : 'Nice match! New junk is dropping in.');
  }
  function beginGesture(event, index) {
    if (event.button !== undefined && event.button !== 0) return;
    gesture = { index, x: event.clientX, y: event.clientY };
    if (event.cancelable) event.preventDefault();
  }
  document.addEventListener('pointerup', event => {
    if (!gesture) return;
    const start = gesture, dx = event.clientX - start.x, dy = event.clientY - start.y;
    const element = document.elementFromPoint(event.clientX, event.clientY)?.closest('.cell');
    let target = element ? Number(element.dataset.index) : start.index;
    if (target === start.index && Math.max(Math.abs(dx), Math.abs(dy)) > 18) {
      const { row, col } = G.coords(start.index);
      if (Math.abs(dx) > Math.abs(dy)) target = start.index + (dx > 0 ? 1 : -1);
      else target = start.index + (dy > 0 ? G.COLS : -G.COLS);
      if (target < 0 || target >= G.SIZE || (Math.floor(target / G.COLS) !== row && Math.abs(dx) > Math.abs(dy)) || (Math.abs(dx) > Math.abs(dy) && Math.floor(target / G.COLS) !== row) || (Math.abs(dx) <= Math.abs(dy) && Math.abs(Math.floor(target / G.COLS) - row) !== 1)) target = start.index;
      if (Math.abs(dx) > Math.abs(dy) && Math.abs(target % G.COLS - col) !== 1) target = start.index;
    }
    gesture = null; suppressClick = true; setTimeout(() => { suppressClick = false; }, 0);
    if (target !== start.index && target >= 0 && target < G.SIZE && G.adjacent(start.index, target)) trySwap(start.index, target);
    else tapCell(start.index);
  });
  document.addEventListener('pointercancel', () => { gesture = null; });
  $('restart-button').addEventListener('click', () => { G.restartJob(state); selected = null; save(); render(); say('Job reset. Fresh board, same Florida.'); });
  $('job-button').addEventListener('click', () => {
    if (state.failed) { G.restartJob(state); selected = null; save(); render(); say('Take two.'); return; }
    if (state.job >= G.JOBS.length) { state = G.newState(); selected = null; save(); render(); $('tutorial').classList.remove('hidden'); return; }
    if (state.won) { G.nextJob(state); selected = null; save(); render(); say(state.job >= G.JOBS.length ? 'Cleanup complete. You are a county legend.' : 'Next job. The debris got weirder.'); }
  });
  $('start-button').addEventListener('click', () => { $('tutorial').classList.add('hidden'); localStorage.setItem('storm-cleanup-match3-tutorial-v3', 'seen'); });
  if (localStorage.getItem('storm-cleanup-match3-tutorial-v3') === 'seen') $('tutorial').classList.add('hidden');
  render();
})(typeof window === 'undefined' ? globalThis : window);
