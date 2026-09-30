(function (root) {
  'use strict';
  if (typeof document === 'undefined') return;
  const G = root.FloridaMatch3;
  const $ = id => document.getElementById(id);
  const boardEl = $('board'), hintEl = $('hint'), toastEl = $('toast');
  const SAVE_KEY = 'storm-cleanup-match3-v3';
  const SOUND_KEY = 'storm-cleanup-sound-v1';
  const tileTokens = new WeakMap();
  let nextToken = 1;
  let selected = null, gesture = null, suppressClick = false, toastTimer, completionTimer, busy = false;
  let soundEnabled = localStorage.getItem(SOUND_KEY) !== 'off', audioContext = null;

  function loadState() {
    try {
      const saved = JSON.parse(localStorage.getItem(SAVE_KEY));
      if (saved && Array.isArray(saved.board) && saved.board.length === G.SIZE && Number.isInteger(saved.job) && saved.job >= 0 && saved.job <= G.JOBS.length) {
        // A completed three-job save from the previous campaign continues at new stop four.
        if (saved.job === 3 && saved.campaignVersion !== 2) {
          saved.board = G.makeBoard(Math.random, G.JOBS[3].chains); saved.moves = G.JOBS[3].moves;
          saved.cleared = {}; saved.won = false; saved.failed = false;
        }
        saved.campaignVersion = 2;
        saved.board = saved.board.map(item => item && G.CHAINS[item.chain] && (!item.power || G.POWER[item.power]) ? { chain: item.chain, power: item.power || null } : null);
        saved.cleared ||= {}; saved.score ||= 0; saved.coins ||= 0;
        if (saved.job < G.JOBS.length) { saved.moves = Math.max(0, saved.moves || 0); saved.failed = saved.moves === 0 && !saved.won; }
        return saved;
      }
    } catch (_) { /* start a new board */ }
    return G.newState();
  }
  let state = loadState();
  function token(item) {
    if (!tileTokens.has(item)) tileTokens.set(item, String(nextToken++));
    return tileTokens.get(item);
  }
  function save() { state.campaignVersion = 2; localStorage.setItem(SAVE_KEY, JSON.stringify(state)); }
  function say(message) {
    toastEl.textContent = message; toastEl.classList.add('show');
    clearTimeout(toastTimer); toastTimer = setTimeout(() => toastEl.classList.remove('show'), 1800);
  }
  function updateSoundButtons() {
    [ $('sound-toggle'), $('map-sound-toggle') ].forEach(button => {
      button.textContent = soundEnabled ? '♫ ON' : '♫ OFF';
      button.setAttribute('aria-label', soundEnabled ? 'Turn sound off' : 'Turn sound on');
      button.classList.toggle('sound-off', !soundEnabled);
    });
  }
  function playTone(frequency, duration, offset = 0, type = 'sine', volume = .055, endFrequency = frequency) {
    if (!soundEnabled) return;
    try {
      const Audio = root.AudioContext || root.webkitAudioContext;
      if (!Audio) return;
      audioContext ||= new Audio();
      if (audioContext.state === 'suspended') audioContext.resume().catch(() => {});
      const start = audioContext.currentTime + offset, oscillator = audioContext.createOscillator(), gain = audioContext.createGain();
      oscillator.type = type; oscillator.frequency.setValueAtTime(frequency, start);
      if (endFrequency !== frequency) oscillator.frequency.exponentialRampToValueAtTime(Math.max(1, endFrequency), start + duration);
      gain.gain.setValueAtTime(.0001, start); gain.gain.exponentialRampToValueAtTime(volume, start + .012);
      gain.gain.exponentialRampToValueAtTime(.0001, start + duration);
      oscillator.connect(gain); gain.connect(audioContext.destination); oscillator.start(start); oscillator.stop(start + duration + .01);
    } catch (_) { /* Audio is an optional enhancement; gameplay stays available. */ }
  }
  function playSound(effect, amount = 1, wave = 0) {
    if (!soundEnabled) return;
    if (effect === 'tap') playTone(560, .055, 0, 'sine', .035, 720);
    else if (effect === 'swap') playTone(290, .075, 0, 'triangle', .04, 430);
    else if (effect === 'invalid') playTone(190, .12, 0, 'square', .025, 95);
    else if (effect === 'clear') {
      const lift = Math.min(180, amount * 9 + wave * 45);
      [0, 1, 2].forEach((n) => playTone(500 + lift + n * 125, .12, n * .045, 'sine', .045));
    } else if (effect === 'power') {
      playTone(220, .34, 0, 'sawtooth', .035, 760); playTone(440, .3, .04, 'triangle', .04, 880);
    } else if (effect === 'win') {
      [523, 659, 784, 1047].forEach((note, i) => playTone(note, .25, i * .13, 'triangle', .05));
    }
  }
  function toggleSound() {
    soundEnabled = !soundEnabled; localStorage.setItem(SOUND_KEY, soundEnabled ? 'on' : 'off'); updateSoundButtons();
    if (soundEnabled) playSound('tap');
  }
  function delay(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }
  function stageIndex() { return Math.min(Math.floor(state.job / 3), 2); }
  function itemName(item) { return item.power ? G.POWER[item.power].label : G.CHAINS[item.chain].names[stageIndex()]; }
  function itemIcon(item) { return item.power ? G.POWER[item.power].icon : G.CHAINS[item.chain].icons[stageIndex()]; }
  function spritePosition(chain) { return `${Object.keys(G.CHAINS).indexOf(chain) * 25}% ${stageIndex() * 50}%`; }
  function tallyPower() { return state.board.filter(x => x?.power).length; }
  const routePoints = [[122, 155], [159, 195], [230, 157], [205, 222], [270, 160], [234, 290], [190, 315], [215, 390], [151, 458]];
  function positionRouteNodes() {
    const frame = document.querySelector('.florida-map').getBoundingClientRect();
    if (!frame.width || !frame.height) return;
    const scale = Math.min(frame.width / 360, frame.height / 500);
    const offsetX = (frame.width - 360 * scale) / 2, offsetY = (frame.height - 500 * scale) / 2;
    routePoints.forEach(([x, y], i) => {
      const node = $(`node-${i}`);
      node.style.left = `${(offsetX + x * scale) / frame.width * 100}%`;
      node.style.top = `${(offsetY + y * scale) / frame.height * 100}%`;
    });
  }

  function renderMap() {
    const done = state.job >= G.JOBS.length || (state.won && state.job === G.JOBS.length - 1);
    const next = state.won && !done ? Math.min(state.job + 1, G.JOBS.length - 1) : Math.min(state.job, G.JOBS.length - 1);
    const job = G.JOBS[next];
    $('map-coins').textContent = String(state.coins);
    $('map-story').textContent = done ? 'The coast is clear, the radio is quiet, and the gator has somehow been promoted.' : job.story;
    $('route-progress').textContent = done ? 'ROUTE COMPLETE' : `STOP ${next + 1} OF ${G.JOBS.length}`;
    $('map-location').textContent = done ? 'GULF COAST · CLEANUP COMPLETE' : `${job.location} · ${job.region}`;
    $('map-stars').textContent = done ? '★★★' : state.won && next === state.job ? '★★★' : '☆☆☆';
    $('map-job-title').textContent = done ? 'FLORIDA THANKS YOU · THE END (FOR NOW)' : `${state.won ? 'NEXT STOP' : `JOB ${next + 1}`} · ${job.title.toUpperCase()}`;
    $('map-play').innerHTML = done ? 'RIDE THE ROUTE AGAIN <span>↻</span>' : state.won ? `DRIVE TO ${job.location.toUpperCase()} <span>→</span>` : `HEAD TO ${job.title.toUpperCase()} <span>→</span>`;
    G.JOBS.forEach((level, i) => {
      const node = $(`node-${i}`), available = i === state.job && !state.won || state.won && i === state.job + 1;
      const completed = i < state.job || (state.won && i === state.job);
      node.className = `map-node route-node route-node-${i + 1} ${completed ? 'complete' : available ? (i === state.job ? 'current' : 'unlocked') : 'locked'}`;
      node.disabled = !available;
      node.querySelector('.node-medal').textContent = completed ? '✓' : String(i + 1);
      node.setAttribute('aria-label', `${level.location}, level ${i + 1}${completed ? ', complete' : available ? ', available' : ', locked'}`);
      node.setAttribute('aria-current', i === next ? 'step' : 'false');
    });
    positionRouteNodes();
  }
  function showMap() {
    clearTimeout(completionTimer); completionTimer = null;
    $('job-complete').classList.add('hidden'); $('job-complete').classList.remove('show');
    $('game-screen').classList.add('hidden'); $('map-screen').classList.remove('hidden'); renderMap();
  }
  function presentCompletion() {
    if (!state.won || state.job >= G.JOBS.length) return;
    playSound('win');
    const job = G.JOBS[state.job], stars = state.moves >= Math.ceil(job.moves * .5) ? 3 : state.moves >= 3 ? 2 : 1;
    $('completion-kicker').textContent = `STOP ${state.job + 1} CLEARED`;
    $('completion-job').textContent = job.title;
    $('completion-stars').textContent = '★'.repeat(stars) + '☆'.repeat(3 - stars);
    $('completion-stars').setAttribute('aria-label', `${stars} star${stars === 1 ? '' : 's'}`);
    $('completion-story').textContent = state.job + 1 < G.JOBS.length
      ? `Nice work. The next cleanup is in ${G.JOBS[state.job + 1].location}.`
      : 'The whole Gulf Coast route is clear. Florida Man rides again.';
    const popup = $('job-complete');
    popup.classList.remove('hidden', 'show');
    requestAnimationFrame(() => popup.classList.add('show'));
    clearTimeout(completionTimer);
    completionTimer = setTimeout(showMap, 3200);
  }
  function showGame() {
    $('map-screen').classList.add('hidden'); $('game-screen').classList.remove('hidden');
    if (localStorage.getItem('storm-cleanup-match3-tutorial-v3') !== 'seen') $('tutorial').classList.remove('hidden');
  }

  function renderBoard(viewBoard = state.board, animateFrom = null) {
    const oldRects = new Map();
    if (animateFrom) {
      boardEl.querySelectorAll('.cell[data-token]').forEach(cell => {
        const rect = cell.getBoundingClientRect();
        oldRects.set(cell.dataset.token, { left: rect.left, top: rect.top });
      });
    }
    const fragment = document.createDocumentFragment();
    viewBoard.forEach((item, index) => {
      const cell = document.createElement('button');
      cell.type = 'button';
      cell.className = 'cell' + (item ? ' filled' : '') + (selected === index ? ' selected' : '') + (item?.power ? ' powered' : '');
      cell.dataset.index = String(index); cell.setAttribute('role', 'gridcell');
      if (item) {
        cell.dataset.token = token(item);
        cell.style.setProperty('--tile-color', G.CHAINS[item.chain].colors[stageIndex()]);
        if (item.power) {
          const icon = document.createElement('span'); icon.className = 'power-icon'; icon.textContent = itemIcon(item);
          cell.appendChild(icon);
        } else {
          const art = document.createElement('span'); art.className = 'illustration'; art.style.backgroundPosition = spritePosition(item.chain);
          cell.appendChild(art);
        }
        const name = document.createElement('span'); name.className = 'sr-only'; name.textContent = item.power ? G.POWER[item.power].label : G.CHAINS[item.chain].names[stageIndex()];
        cell.appendChild(name); cell.setAttribute('aria-label', itemName(item) + (item.power ? ', power-up, tap to activate' : ''));
      } else {
        cell.classList.add('empty'); cell.setAttribute('aria-label', 'Empty board space');
      }
      cell.addEventListener('pointerdown', event => beginGesture(event, index));
      cell.addEventListener('click', () => { if (suppressClick) { suppressClick = false; return; } tapCell(index); });
      fragment.appendChild(cell);
    });
    boardEl.replaceChildren(fragment);
    if (animateFrom) {
      const animations = [];
      boardEl.querySelectorAll('.cell.filled').forEach(cell => {
        const rect = cell.getBoundingClientRect(), old = oldRects.get(cell.dataset.token);
        if (old) {
          const dx = old.left - rect.left, dy = old.top - rect.top;
          if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
            animations.push(cell.animate(
              [{ transform: `translate(${dx}px, ${dy}px) scale(.96)` }, { transform: 'translate(0,0) scale(1)' }],
              { duration: 190, easing: 'cubic-bezier(.2,.75,.25,1)' }
            ).finished.catch(() => {}));
          }
        } else {
          animations.push(cell.animate(
            [{ transform: `translateY(-${Math.max(24, rect.height * .9)}px) scale(.82)`, opacity: .15 }, { transform: 'translateY(0) scale(1)', opacity: 1 }],
            { duration: 255, easing: 'cubic-bezier(.18,.75,.24,1)' }
          ).finished.catch(() => {}));
        }
      });
      return Promise.all(animations);
    }
    return Promise.resolve();
  }

  function renderHud() {
    const done = state.job >= G.JOBS.length, job = done ? G.JOBS[G.JOBS.length - 1] : G.JOBS[state.job];
    $('job-kicker').textContent = done ? 'ALL JOBS COMPLETE' : `CURRENT JOB · ${state.job + 1} OF ${G.JOBS.length}`;
    $('job-title').textContent = done ? 'Coast is looking good!' : job.title;
    $('job-copy').textContent = done ? 'You saved the block, the bait shop, and the neighborhood gator.' : `${job.location} · ${job.copy}`;
    $('level-badge').textContent = done ? 'FLORIDA ROUTE COMPLETE' : `LEVEL ${state.job + 1} · ${job.region}`;
    $('story-banner').textContent = done ? 'FLORIDA MAN · LEGEND OF THE GULF' : `FLORIDA MAN · ${job.location.toUpperCase()}`;
    const earned = state.won ? (state.moves >= Math.ceil(job.moves * .5) ? 3 : state.moves >= 3 ? 2 : 1) : Math.min(state.job, 3);
    $('stars').textContent = '★'.repeat(earned) + '☆'.repeat(3 - earned);
    $('requirements').replaceChildren();
    if (done) {
      const badge = document.createElement('span'); badge.className = 'requirement ready'; badge.textContent = '✓ Cleanup complete'; $('requirements').appendChild(badge);
    } else {
      G.jobStatus(state).forEach(need => {
        const badge = document.createElement('span'); badge.className = 'requirement' + (need.have >= need.amount ? ' ready' : '');
        const label = G.CHAINS[need.chain].names[stageIndex()];
        badge.setAttribute('aria-label', `${label}: ${need.have} of ${need.amount}`);
        const art = document.createElement('span'); art.className = 'req-art'; art.style.backgroundPosition = spritePosition(need.chain);
        const count = document.createElement('span'); count.className = 'goal-count'; count.textContent = `${need.have}/${need.amount}`;
        badge.append(art, count);
        $('requirements').appendChild(badge);
      });
    }
    $('coins').textContent = String(state.coins);
    $('score').textContent = String(state.score);
    $('moves').textContent = String(state.moves);
    $('board-count').textContent = `${G.SIZE} tiles`;
    $('restart-button').disabled = busy || state.job >= G.JOBS.length;
    const action = $('job-button');
    action.hidden = !(state.failed || done);
    $('action-buttons').classList.toggle('single-action', action.hidden);
    action.disabled = busy || !(state.won || state.failed || state.job >= G.JOBS.length);
    action.textContent = state.job >= G.JOBS.length ? 'PLAY AGAIN' : state.failed ? 'RETRY JOB' : state.won ? 'JOB COMPLETE · NEXT' : 'JOB IN PROGRESS';
    if (state.won) hintEl.textContent = 'Job cleared! Leftover moves boost your stars.';
    else if (state.failed) hintEl.textContent = 'Out of moves. Restart the job and take another run.';
    else if (selected !== null && state.board[selected]) hintEl.textContent = state.board[selected].power ? 'Tap the power-up again or swap it to fire.' : 'Selected. Swap it with a neighbor to make a match.';
    else hintEl.textContent = 'Match 3+ in a line · 2×2 squares count · 4 in a line earns a blast';
  }
  function render() { renderBoard(state.board); renderHud(); }

  async function animateTurn(result, oldBoard) {
    busy = true; selected = null; renderHud();
    const sequence = result.animation;
    if (result.power) playSound('power');
    await renderBoard(sequence.midBoard, oldBoard);
    for (const wave of sequence.events) {
      if (wave.clear.length) playSound('clear', wave.clear.length, wave.wave);
      const clearAnims = [];
      for (const i of wave.clear) {
        const cell = boardEl.querySelector(`.cell[data-index="${i}"]`);
        if (!cell) continue;
        cell.classList.add('clearing');
        const burst = document.createElement('span'); burst.className = 'clear-burst'; burst.textContent = '✦'; cell.appendChild(burst);
        clearAnims.push(cell.animate(
          [{ transform: 'scale(1) rotate(0)', opacity: 1, filter: 'brightness(1) saturate(1)' }, { transform: 'scale(.12) rotate(24deg)', opacity: 0, filter: 'brightness(2.2) saturate(1.8)' }],
          { duration: 235, easing: 'cubic-bezier(.32,0,.8,.3)', fill: 'forwards' }
        ).finished.catch(() => {}));
        burst.animate(
          [{ transform: 'translate(-50%,-50%) scale(.2) rotate(0)', opacity: 0 }, { transform: 'translate(-50%,-50%) scale(1.5) rotate(45deg)', opacity: 1, offset: .45 }, { transform: 'translate(-50%,-50%) scale(1.9) rotate(90deg)', opacity: 0 }],
          { duration: 260, easing: 'ease-out' }
        );
      }
      await Promise.all(clearAnims);
      await renderBoard(wave.after, wave.before);
      if (wave.wave < sequence.events.length - 1) await delay(65);
    }
    await renderBoard(state.board, sequence.events.at(-1)?.after || sequence.midBoard);
    busy = false; renderHud(); save();
    if (state.won) presentCompletion();
  }

  function tapCell(index) {
    if (busy) return;
    const item = state.board[index];
    if (!item) { selected = null; render(); return; }
    if (item.power) {
      const oldBoard = state.board.slice(), result = G.activate(state, index);
      if (result.ok) {
        animateTurn(result, oldBoard).then(() => { if (!state.won) say(`${G.POWER[result.power].label} fired!`); });
      } else say(result.reason);
      return;
    }
    if (selected === null || selected === index) { selected = selected === index ? null : index; if (selected !== null) playSound('tap'); render(); return; }
    if (G.adjacent(selected, index)) trySwap(selected, index);
    else { selected = index; render(); say('Swap neighboring pieces only.'); }
  }
  function shake(from, to) {
    for (const i of [from, to]) {
      const cell = boardEl.querySelector(`.cell[data-index="${i}"]`);
      cell?.animate([
        { transform: 'translateX(0)' }, { transform: 'translateX(-5px)' }, { transform: 'translateX(5px)' }, { transform: 'translateX(-3px)' }, { transform: 'translateX(0)' }
      ], { duration: 210, easing: 'ease-out' });
    }
  }
  function trySwap(from, to) {
    if (busy) return;
    const oldBoard = state.board.slice(), before = tallyPower();
    const result = G.swap(state, from, to);
    if (!result.ok) { playSound('invalid'); selected = null; render(); shake(from, to); say(result.reason); return; }
    playSound('swap');
    const powerEarned = tallyPower() > before;
    animateTurn(result, oldBoard).then(() => {
      if (state.won) return;
      if (powerEarned) { playSound('power'); say('Power-up earned! Tap it or swap it to fire.'); }
      else if (result.matched >= 5) say('Big match! Watch that cascade.');
      else say(result.power ? 'Power-up fired!' : 'Nice match! More junk is dropping in.');
    });
  }
  function beginGesture(event, index) {
    if (busy || (event.button !== undefined && event.button !== 0)) return;
    gesture = { index, x: event.clientX, y: event.clientY };
    if (event.cancelable) event.preventDefault();
  }
  document.addEventListener('pointerup', event => {
    if (!gesture || busy) return;
    const start = gesture, dx = event.clientX - start.x, dy = event.clientY - start.y;
    const element = document.elementFromPoint(event.clientX, event.clientY)?.closest('.cell');
    let target = element ? Number(element.dataset.index) : start.index;
    if (target === start.index && Math.max(Math.abs(dx), Math.abs(dy)) > 18) {
      const { row, col } = G.coords(start.index);
      if (Math.abs(dx) > Math.abs(dy)) target = start.index + (dx > 0 ? 1 : -1);
      else target = start.index + (dy > 0 ? G.COLS : -G.COLS);
      if (target < 0 || target >= G.SIZE || (Math.abs(dx) > Math.abs(dy) && Math.floor(target / G.COLS) !== row) || (Math.abs(dx) <= Math.abs(dy) && Math.abs(Math.floor(target / G.COLS) - row) !== 1) || (Math.abs(dx) > Math.abs(dy) && Math.abs(target % G.COLS - col) !== 1)) target = start.index;
    }
    gesture = null; suppressClick = true; setTimeout(() => { suppressClick = false; }, 0);
    if (target !== start.index && target >= 0 && target < G.SIZE && G.adjacent(start.index, target)) trySwap(start.index, target);
    else tapCell(start.index);
  });
  document.addEventListener('pointercancel', () => { gesture = null; });
  $('restart-button').addEventListener('click', () => { if (busy) return; playSound('tap'); G.restartJob(state); selected = null; save(); render(); say('Job reset. Fresh board, same Florida.'); });
  $('job-button').addEventListener('click', () => {
    if (busy) return;
    if (state.failed) { G.restartJob(state); selected = null; save(); render(); say('Take two.'); return; }
    if (state.job >= G.JOBS.length) { state = G.newState(); selected = null; save(); render(); $('tutorial').classList.remove('hidden'); return; }
    if (state.won) { showMap(); }
  });
  $('start-button').addEventListener('click', () => { playSound('tap'); $('tutorial').classList.add('hidden'); localStorage.setItem('storm-cleanup-match3-tutorial-v3', 'seen'); });
  $('completion-continue').addEventListener('click', () => { playSound('tap'); showMap(); });
  $('map-button').addEventListener('click', () => { if (!busy) { playSound('tap'); showMap(); } });
  $('sound-toggle').addEventListener('click', toggleSound);
  $('map-sound-toggle').addEventListener('click', toggleSound);
  $('map-play').addEventListener('click', () => {
    if (busy) return;
    playSound('tap');
    if (state.won && state.job === G.JOBS.length - 1) { G.nextJob(state); save(); showMap(); return; }
    if (state.job >= G.JOBS.length) { state = G.newState(); selected = null; }
    else if (state.won) G.nextJob(state);
    selected = null; save(); render(); showGame();
  });
  G.JOBS.forEach((job, i) => $(`node-${i}`).addEventListener('click', () => {
    if ($(`node-${i}`).disabled) return;
    if (state.won && i === state.job + 1) $('map-play').click();
    else { playSound('tap'); showGame(); }
  }));
  $('tutorial').classList.add('hidden');
  updateSoundButtons();
  render();
  renderMap();
  root.addEventListener('resize', positionRouteNodes);
})(typeof window === 'undefined' ? globalThis : window);
