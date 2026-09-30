const assert = require('node:assert/strict');
const G = require('../app/src/main/assets/match3.js');

function rngFrom(seed) {
  let x = seed >>> 0;
  return () => { x = (1664525 * x + 1013904223) >>> 0; return x / 4294967296; };
}

// Fresh boards start full, playable, and free from automatic matches.
const state = G.newState(rngFrom(9321));
assert.equal(state.board.length, G.SIZE);
assert.ok(state.board.every(Boolean));
assert.equal(G.scanMatches(state.board).indices.size, 0, 'fresh board has no free matches');
assert.ok(G.rawPlayable(state.board), 'fresh board has at least one legal move');

// Invalid swaps restore the board and do not spend a move.
let invalidFound = false;
for (let i = 0; i < G.SIZE && !invalidFound; i++) {
  for (const j of [i + 1, i + G.COLS]) {
    if (j >= G.SIZE || (j === i + 1 && Math.floor(i / G.COLS) !== Math.floor(j / G.COLS))) continue;
    const before = JSON.stringify(state.board), moves = state.moves;
    const result = G.swap(state, i, j, rngFrom(6));
    if (!result.ok && result.reason === 'That swap did not make a match.') {
      invalidFound = true;
      assert.equal(JSON.stringify(state.board), before, 'invalid swap snaps back');
      assert.equal(state.moves, moves, 'invalid swap costs no move');
      break;
    }
  }
}
assert.ok(invalidFound, 'generated board exposes at least one invalid swap to reject');

// Find and play a legal swap: it clears pieces, refills the board by gravity, and uses one move.
let played = false;
for (let i = 0; i < G.SIZE && !played; i++) {
  for (const j of [i + 1, i + G.COLS]) {
    if (j >= G.SIZE || (j === i + 1 && Math.floor(i / G.COLS) !== Math.floor(j / G.COLS))) continue;
    const trial = G.newState(rngFrom(9321));
    const result = G.swap(trial, i, j, rngFrom(25));
    if (result.ok) {
      played = true;
      assert.equal(trial.moves, G.JOBS[0].moves - 1);
      assert.ok(result.animation.midBoard.length === G.SIZE, 'move animation records the swapped board');
      assert.ok(result.animation.events.length > 0, 'clear and fall animations receive a resolved wave');
      assert.ok(result.animation.events[0].clear.length >= 3, 'clear event identifies the matched tiles');
      assert.equal(result.animation.events[0].after.length, G.SIZE, 'fall event includes the refilled board');
      assert.ok(trial.board.every(Boolean), 'gravity refills every cleared cell');
      assert.ok(Object.values(trial.cleared).reduce((a, b) => a + b, 0) >= 3, 'match adds debris to cleanup progress');
      assert.equal(G.scanMatches(trial.board).indices.size, 0, 'cascades settle before the next move');
      assert.ok(G.rawPlayable(trial.board), 'settled board remains playable');
    }
  }
}
assert.ok(played, 'a valid swap is available');

// Four- and five-piece matches award line and rainbow powers.
assert.equal(G.findPowerPlan([{ direction: 'row', cells: [0, 1, 2, 3] }]).kind, 'row');
assert.equal(G.findPowerPlan([{ direction: 'column', cells: [0, 5, 10, 15, 20] }]).kind, 'rainbow');
const powerEarned = G.newState(rngFrom(1));
assert.equal(G.swap(powerEarned, 11, 12, rngFrom(101)).ok, true);
assert.ok(powerEarned.board.some(item => item?.power), 'a 4+ match places a usable power-up on the board');
assert.equal(G.findPowerPlan([
  { direction: 'row', cells: [6, 7, 8] },
  { direction: 'column', cells: [2, 7, 12] }
]).kind, 'bomb', 'T intersections earn a hurricane bomb');

// Exact cleanup quotas complete a job and carry into the next one.
state.cleared.wood = 10; state.cleared.chair = 10;
assert.equal(G.jobComplete(state), true);
state.won = true;
assert.equal(G.nextJob(state, rngFrom(12)), true);
assert.equal(state.job, 1);
assert.equal(state.moves, G.JOBS[1].moves);
assert.equal(state.board.length, G.SIZE);
assert.deepEqual(state.cleared, {});
assert.ok(G.rawPlayable(state.board));

// Tapping an earned line power uses one move and resolves it without breaking the board.
const powerState = G.newState(rngFrom(77));
powerState.board[0] = { chain: 'wood', power: 'row' };
const powerMoves = powerState.moves;
assert.equal(G.activate(powerState, 0, rngFrom(45)).ok, true);
assert.equal(powerState.moves, powerMoves - 1);
assert.ok(powerState.board.every(Boolean));
console.log('Match-three checks passed: playable start, rejected swaps, gravity/refill, power tiers, cleanup goals, and job progression.');
