const assert = require('node:assert/strict');
const game = require('../app/src/main/assets/game.js');

const board = [
  { chain: 'wood', level: 0 }, { chain: 'wood', level: 0 },
  { chain: 'chair', level: 0 }, null
];
assert.equal(game.merge(board, 0, 3).ok, false, 'empty target rejects a move');
assert.deepEqual(board[0], { chain: 'wood', level: 0 }, 'rejected move leaves source in place');
assert.equal(game.merge(board, 0, 2).ok, false, 'different family cannot merge');
assert.equal(game.merge(board, 0, 1).ok, true, 'identical pieces merge');
assert.equal(board[0], null, 'source slot clears after valid merge');
assert.deepEqual(board[1], { chain: 'wood', level: 1 }, 'target upgrades one tier');
assert.equal(game.merge([{ chain: 'wood', level: 4 }, { chain: 'wood', level: 4 }], 0, 1).ok, false, 'terminal tier cannot merge');

const state = game.newState();
assert.equal(game.canDeliver(state), false, 'starting board does not falsely complete the job');
state.board[12] = { chain: 'wood', level: 1 };
state.board[13] = { chain: 'chair', level: 1 };
assert.equal(game.canDeliver(state), true, 'job checks exact requested tiers');
assert.equal(game.deliver(state), true, 'ready job delivers');
assert.equal(state.job, 1, 'delivery advances one job');
assert.equal(state.coins, 25, 'delivery awards coins');
assert.equal(state.board[12], null, 'delivery consumes requested item');
assert.equal(state.board[13], null, 'delivery consumes every requirement');
console.log('Gameplay checks passed: matching merge, invalid drop rejection, tier cap, exact delivery, rewards, and job progression.');
