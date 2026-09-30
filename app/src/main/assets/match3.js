(function (root) {
  'use strict';
  const ROWS = 6;
  const COLS = 5;
  const SIZE = ROWS * COLS;
  const CHAINS = {
    wood: { colors: ['#d8a05e', '#e7a15e', '#f2ac62'], names: ['Soggy plank', 'Instant porch kit', 'Trailer-palace plan'], icons: ['🪵', '🛠️', '🏠'] },
    chair: { colors: ['#d6a16d', '#cb8c54', '#b7784a'], names: ['Busted lawn chair', 'Gator recliner', 'Mayor’s throne'], icons: ['🪑', '🐊', '👑'] },
    leaf: { colors: ['#aad277', '#91c86a', '#72b75d'], names: ['Loose palm frond', 'Hurricane leaf cannon', 'Storm steering wheel'], icons: ['🌿', '🌪️', '🛞'] },
    cooler: { colors: ['#8fd3df', '#74cbd0', '#66bac7'], names: ['Empty cooler', 'Satellite bait cooler', 'Gator command center'], icons: ['🧊', '📡', '🐊'] },
    flip: { colors: ['#f09bb0', '#e77f9d', '#ca74c5'], names: ['Lone flip-flop', 'Storm-surfing sandals', 'Cloud-surfing flip-flops'], icons: ['🩴', '🏄', '☁️'] }
  };
  const JOBS = [
    { location: 'Panama City Beach', region: 'GULF COAST', story: 'The storm shoved half the porch into the street. Help Florida Man clear a path before the neighborhood gator claims the whole house.', title: 'Fix the Porch', copy: 'Clear the storm junk blocking the porch. The gator already claimed the recliner.', needs: [{ chain: 'wood', amount: 10 }, { chain: 'chair', amount: 10 }], moves: 24, chains: ['wood', 'chair', 'leaf', 'cooler'] },
    { location: 'Apalachicola', region: 'FORGOTTEN COAST', story: 'A strange radio signal is coming from the bait shop. Clear the driftwood and the satellite cooler before the next high tide.', title: 'Clear the Bait Shop', copy: 'Dig the bait shop out before the next high tide. Mind the satellite cooler.', needs: [{ chain: 'leaf', amount: 12 }, { chain: 'cooler', amount: 12 }], moves: 26, chains: ['leaf', 'cooler', 'wood', 'flip'] },
    { location: 'Tallahassee', region: 'CAPITAL CITY', story: 'The bait shop radio keeps picking up an emergency broadcast from Tallahassee. The storm office is buried under a very suspicious amount of junk.', title: 'Find the Signal', copy: 'Clear the storm-office lot and follow the signal south.', needs: [{ chain: 'cooler', amount: 14 }, { chain: 'flip', amount: 14 }], moves: 28, chains: ['cooler', 'flip', 'leaf', 'chair'] },
    { location: 'Gainesville', region: 'NORTH CENTRAL FLORIDA', story: 'The signal points to a roadside gator museum outside Gainesville. Its exhibits appear to have escaped during the storm.', title: 'Round Up the Exhibits', copy: 'Clear the runaway museum junk before it reaches the highway.', needs: [{ chain: 'chair', amount: 16 }, { chain: 'leaf', amount: 16 }], moves: 30, chains: ['chair', 'leaf', 'wood', 'cooler'] },
    { location: 'Jacksonville', region: 'FIRST COAST', story: 'A trucker saw the signal bouncing toward Jacksonville. Florida Man finds a marina full of gear, including one cooler that is definitely transmitting.', title: 'Clear the Marina', copy: 'Salvage the marina before the cooler phones home.', needs: [{ chain: 'flip', amount: 18 }, { chain: 'chair', amount: 18 }], moves: 32, chains: ['flip', 'chair', 'cooler', 'wood'] },
    { location: 'Orlando', region: 'CENTRAL FLORIDA', story: 'The signal leads to an abandoned roadside attraction near Orlando. The gator insists it is a theme park and has already hired itself as manager.', title: 'Unjam the Gator Park', copy: 'Clear the entrance and keep the gator away from the ticket booth.', needs: [{ chain: 'cooler', amount: 20 }, { chain: 'wood', amount: 20 }], moves: 34, chains: ['cooler', 'wood', 'flip', 'leaf'] },
    { location: 'Tampa Bay', region: 'SUNCOAST', story: 'At last, the signal source: a satellite cooler in Tampa Bay. Florida Man builds Gator Command before the next squall rolls in.', title: 'Build Gator Command', copy: 'Turn the salvaged junk into an extremely unnecessary command center.', needs: [{ chain: 'cooler', amount: 22 }, { chain: 'flip', amount: 22 }], moves: 36, chains: ['cooler', 'flip', 'leaf', 'chair'] },
    { location: 'Everglades', region: 'THE GLADES', story: 'Gator Command detects one last storm cell over the Everglades. The locals are calm. The wildlife is not.', title: 'Secure the Glades', copy: 'Clear a path through the swamp before the storm changes its mind.', needs: [{ chain: 'leaf', amount: 24 }, { chain: 'chair', amount: 24 }], moves: 38, chains: ['leaf', 'chair', 'wood', 'cooler'] },
    { location: 'Key West', region: 'THE KEYS', story: 'The final storm cell is headed for Key West. The whole crew races south to finish the cleanup and give the gator a well-earned day off.', title: 'Save the Keys', copy: 'One last push. Get the island ready before the squall hits.', needs: [{ chain: 'wood', amount: 26 }, { chain: 'flip', amount: 26 }], moves: 40, chains: ['wood', 'flip', 'cooler', 'leaf'] }
  ];
  const POWER = { row: { icon: '🌪️', label: 'Storm row-blast' }, column: { icon: '⚡', label: 'Lightning column-blast' }, bomb: { icon: '💥', label: 'Hurricane bomb' }, rainbow: { icon: '🌈', label: 'Rainbow gator' } };

  function idx(row, col) { return row * COLS + col; }
  function coords(i) { return { row: Math.floor(i / COLS), col: i % COLS }; }
  function adjacent(a, b) { const p = coords(a), q = coords(b); return Math.abs(p.row - q.row) + Math.abs(p.col - q.col) === 1; }
  function sameKind(a, b) { return !!a && !!b && a.chain === b.chain; }
  function tile(chain, power = null) { return { chain, power }; }
  function candidateChain(chains, rng) { return chains[Math.floor(rng() * chains.length)]; }

  function scanMatches(board) {
    const lines = [], squares = [];
    for (let r = 0; r < ROWS; r++) {
      let c = 0;
      while (c < COLS) {
        const start = c, first = board[idx(r, c)];
        if (!first) { c++; continue; }
        c++;
        while (c < COLS && sameKind(first, board[idx(r, c)])) c++;
        if (c - start >= 3) lines.push({ direction: 'row', cells: Array.from({ length: c - start }, (_, k) => idx(r, start + k)) });
      }
    }
    for (let c = 0; c < COLS; c++) {
      let r = 0;
      while (r < ROWS) {
        const start = r, first = board[idx(r, c)];
        if (!first) { r++; continue; }
        r++;
        while (r < ROWS && sameKind(first, board[idx(r, c)])) r++;
        if (r - start >= 3) lines.push({ direction: 'column', cells: Array.from({ length: r - start }, (_, k) => idx(start + k, c)) });
      }
    }
    const indices = new Set();
    lines.forEach(line => line.cells.forEach(i => indices.add(i)));
    // A solid 2x2 block is a valid four-piece match, even without a straight run.
    // Larger solid blocks are covered by the union of their overlapping 2x2 squares.
    for (let r = 0; r < ROWS - 1; r++) for (let c = 0; c < COLS - 1; c++) {
      const cells = [idx(r, c), idx(r, c + 1), idx(r + 1, c), idx(r + 1, c + 1)];
      const first = board[cells[0]];
      if (first && cells.slice(1).every(i => sameKind(first, board[i]))) {
        squares.push({ direction: 'square', cells });
        cells.forEach(i => indices.add(i));
      }
    }
    // T/L intersections are already included by the union of their matching
    // row and column runs. A loose connected triplet is not a valid match.
    return { lines, squares, indices };
  }
  function findPowerPlan(lines, preferred = -1) {
    const long = lines.filter(line => line.cells.length >= 5).sort((a, b) => b.cells.length - a.cells.length)[0];
    if (long) return { kind: 'rainbow', chain: null, at: long.cells.includes(preferred) ? preferred : long.cells[Math.floor(long.cells.length / 2)] };
    const four = lines.filter(line => line.cells.length >= 4).sort((a, b) => b.cells.length - a.cells.length)[0];
    if (four) return { kind: four.direction === 'row' ? 'row' : 'column', chain: null, at: four.cells.includes(preferred) ? preferred : four.cells[Math.floor(four.cells.length / 2)] };
    // A T/L intersection that covers at least five pieces earns a local hurricane bomb.
    const intersection = lines.find((a, i) => lines.some((b, j) => j > i && a.cells.some(x => b.cells.includes(x)) && new Set([...a.cells, ...b.cells]).size >= 5));
    if (intersection) {
      const other = lines.find(line => line !== intersection && line.cells.some(x => intersection.cells.includes(x)));
      const group = [...new Set([...intersection.cells, ...other.cells])];
      return { kind: 'bomb', chain: null, at: group.includes(preferred) ? preferred : group[Math.floor(group.length / 2)] };
    }
    return null;
  }
  function rawPlayable(board) {
    for (let i = 0; i < SIZE; i++) {
      for (const j of [i + 1, i + COLS]) {
        if (j >= SIZE || (j === i + 1 && Math.floor(i / COLS) !== Math.floor(j / COLS))) continue;
        if (board[i]?.power || board[j]?.power) return true;
        [board[i], board[j]] = [board[j], board[i]];
        const found = scanMatches(board).indices.size > 0;
        [board[i], board[j]] = [board[j], board[i]];
        if (found) return true;
      }
    }
    return false;
  }
  function makeBoard(rng, activeChains) {
    let board;
    for (let attempt = 0; attempt < 80; attempt++) {
      board = Array(SIZE).fill(null);
      for (let i = 0; i < SIZE; i++) {
        let choices = activeChains.filter(chain => {
          board[i] = tile(chain);
          const wouldMatch = scanMatches(board).indices.size > 0;
          board[i] = null;
          return !wouldMatch;
        });
        if (!choices.length) choices = activeChains;
        board[i] = tile(candidateChain(choices, rng));
      }
      if (!scanMatches(board).indices.size && rawPlayable(board)) return board;
    }
    // A shuffle keeps the board usable if an unusual random stream repeats a dead layout.
    board = Array.from({ length: SIZE }, (_, i) => tile(activeChains[i % activeChains.length]));
    for (let i = SIZE - 1; i > 0; i--) { const j = Math.floor(rng() * (i + 1)); [board[i], board[j]] = [board[j], board[i]]; }
    return board;
  }
  function newState(rng = Math.random) {
    const job = JOBS[0];
    return { board: makeBoard(rng, job.chains), job: 0, moves: job.moves, cleared: {}, score: 0, coins: 0, won: false, failed: false };
  }
  function jobStatus(state) {
    if (state.job >= JOBS.length) return [];
    return JOBS[state.job].needs.map(need => ({ ...need, have: Math.min(state.cleared[need.chain] || 0, need.amount) }));
  }
  function jobComplete(state) { return state.job >= JOBS.length || jobStatus(state).every(n => n.have >= n.amount); }
  function activateTargets(board, center, type, chain) {
    const { row, col } = coords(center), result = new Set([center]);
    if (type === 'row' || type === 'bomb') for (let c = 0; c < COLS; c++) result.add(idx(row, c));
    if (type === 'column' || type === 'bomb') for (let r = 0; r < ROWS; r++) result.add(idx(r, col));
    if (type === 'bomb') for (let r = Math.max(0, row - 1); r <= Math.min(ROWS - 1, row + 1); r++) for (let c = Math.max(0, col - 1); c <= Math.min(COLS - 1, col + 1); c++) result.add(idx(r, c));
    if (type === 'rainbow') board.forEach((item, i) => { if (item && item.chain === chain) result.add(i); });
    return result;
  }
  function settle(state, initial, rng, preferred = -1, allowPowerCreation = true, events = []) {
    let pending = new Set(initial);
    let first = true, waves = 0;
    while (pending.size && waves++ < 30) {
      const beforeBoard = state.board.slice();
      const removal = new Set(pending), activated = new Set();
      // Powers caught in a match chain together.
      let changed = true;
      while (changed) {
        changed = false;
        for (const i of [...removal]) {
          const item = state.board[i];
          if (item?.power && !activated.has(i)) {
            activated.add(i);
            activateTargets(state.board, i, item.power, item.chain).forEach(j => {
              if (!removal.has(j)) { removal.add(j); changed = true; }
            });
          }
        }
      }
      let plan = null;
      if (allowPowerCreation) {
        const matches = scanMatches(state.board);
        if (matches.indices.size) plan = findPowerPlan(matches.lines, first ? preferred : -1);
      }
      if (plan && !removal.has(plan.at)) {
        // If this wave came from a power, the matched cells are being settled too.
        removal.add(plan.at);
      }
      let keep = null;
      if (plan) {
        const existing = state.board[plan.at];
        const chain = plan.kind === 'rainbow' ? (existing?.chain || state.board[[...removal][0]]?.chain || 'wood') : (existing?.chain || 'wood');
        keep = { index: plan.at, item: tile(chain, plan.kind) };
        removal.delete(plan.at);
      }
      const clearedIndices = [...removal];
      for (const i of removal) {
        const item = state.board[i];
        if (item) {
          state.cleared[item.chain] = (state.cleared[item.chain] || 0) + 1;
          state.score += item.power ? 3 : 1;
          state.coins += item.power ? 2 : 1;
          state.board[i] = null;
        }
      }
      if (keep) state.board[keep.index] = keep.item;
      // Collapse each column downward and refill the vacated cells from above.
      for (let c = 0; c < COLS; c++) {
        const survivors = [];
        for (let r = ROWS - 1; r >= 0; r--) { const item = state.board[idx(r, c)]; if (item) survivors.push(item); }
        let cursor = 0;
        for (let r = ROWS - 1; r >= 0; r--) state.board[idx(r, c)] = cursor < survivors.length ? survivors[cursor++] : tile(candidateChain(JOBS[state.job]?.chains || Object.keys(CHAINS), rng));
      }
      events.push({ before: beforeBoard, clear: clearedIndices, after: state.board.slice(), created: keep ? { index: keep.index, item: keep.item } : null, wave: events.length });
      const next = scanMatches(state.board);
      if (!next.indices.size) break;
      pending = next.indices;
      preferred = -1;
      first = false;
    }
    if (state.job < JOBS.length && jobComplete(state)) state.won = true;
    else if (state.moves <= 0) state.failed = true;
    return waves;
  }
  function shuffleDeadBoard(state, rng) {
    const chains = JOBS[state.job]?.chains || Object.keys(CHAINS);
    const tiles = state.board.filter(Boolean);
    for (let attempt = 0; attempt < 100; attempt++) {
      for (let i = tiles.length - 1; i > 0; i--) { const j = Math.floor(rng() * (i + 1)); [tiles[i], tiles[j]] = [tiles[j], tiles[i]]; }
      state.board = tiles.slice();
      if (!scanMatches(state.board).indices.size && rawPlayable(state.board)) return;
    }
    state.board = makeBoard(rng, chains);
  }
  function swap(state, a, b, rng = Math.random) {
    if (state.won || state.failed || state.moves <= 0 || !adjacent(a, b) || !state.board[a] || !state.board[b]) return { ok: false, reason: 'Swap neighboring pieces only.' };
    [state.board[a], state.board[b]] = [state.board[b], state.board[a]];
    const powerIndex = state.board[a]?.power ? a : state.board[b]?.power ? b : -1;
    const matches = scanMatches(state.board);
    if (!matches.indices.size && powerIndex < 0) {
      [state.board[a], state.board[b]] = [state.board[b], state.board[a]];
      return { ok: false, reason: 'That swap did not make a match.' };
    }
    state.moves--;
    const midBoard = state.board.slice(), events = [];
    if (powerIndex >= 0) {
      const item = state.board[powerIndex];
      const targets = activateTargets(state.board, powerIndex, item.power, item.chain);
      matches.indices.forEach(i => targets.add(i));
      settle(state, targets, rng, -1, false, events);
    } else settle(state, matches.indices, rng, b, true, events);
    if (!state.won && !state.failed && !rawPlayable(state.board)) shuffleDeadBoard(state, rng);
    return { ok: true, matched: matches.indices.size, power: powerIndex >= 0 ? 'activated' : null, animation: { midBoard, events, finalBoard: state.board.slice() } };
  }
  function activate(state, index, rng = Math.random) {
    const item = state.board[index];
    if (state.won || state.failed || state.moves <= 0 || !item?.power) return { ok: false, reason: 'Select an earned power-up.' };
    state.moves--;
    const midBoard = state.board.slice(), events = [];
    settle(state, activateTargets(state.board, index, item.power, item.chain), rng, -1, false, events);
    if (!state.won && !state.failed && !rawPlayable(state.board)) shuffleDeadBoard(state, rng);
    return { ok: true, power: item.power, animation: { midBoard, events, finalBoard: state.board.slice() } };
  }
  function nextJob(state, rng = Math.random) {
    if (!state.won || state.job >= JOBS.length) return false;
    state.job++;
    state.won = false;
    state.cleared = {};
    if (state.job >= JOBS.length) return true;
    state.moves = JOBS[state.job].moves;
    state.failed = false;
    state.board = makeBoard(rng, JOBS[state.job].chains);
    return true;
  }
  function restartJob(state, rng = Math.random) {
    if (state.job >= JOBS.length) return newState(rng);
    const job = JOBS[state.job];
    state.board = makeBoard(rng, job.chains);
    state.moves = job.moves;
    state.cleared = {};
    state.won = false;
    state.failed = false;
    return state;
  }

  const api = { ROWS, COLS, SIZE, CHAINS, JOBS, POWER, idx, coords, adjacent, scanMatches, findPowerPlan, makeBoard, newState, jobStatus, jobComplete, swap, activate, nextJob, restartJob, rawPlayable };
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.FloridaMatch3 = api;
})(typeof window === 'undefined' ? globalThis : window);
