# Florida Man: Storm Cleanup — Match-three prototype

## Core loop

Swap two neighboring storm-debris tiles. Only swaps that create a horizontal or vertical run of at least three stick. Matched tiles disappear, pieces above fall into the gaps, and new debris drops from the top. Cascades resolve before the next move. Invalid swaps snap back and cost nothing.

Each cleanup job sets two debris quotas and a move limit. Clear both quotas to finish the job; leftover moves award more stars. The board refills continuously, so the goal is clearing the requested mess rather than leaving a permanently empty board. If the board has no legal swaps, it reshuffles without charging a move.

## Power-ups

- Match four horizontally or vertically: earn a row or column blast.
- Match five: earn a rainbow gator that clears every piece of its debris type.
- Make a T or L intersection: earn a hurricane bomb.
- Tap a power-up or swap it into a match to fire it. It consumes one move and can create cascades.

## Jobs

1. Fix the Porch: clear 10 soggy planks and 10 busted lawn chairs in 24 moves.
2. Clear the Bait Shop: clear 12 palm-frond piles and 12 coolers in 26 moves.
3. Build Gator Command: clear 14 satellite bait coolers and 14 storm-surfing sandals in 28 moves.

Tile art/names escalate by job: porch junk becomes improbable construction equipment, chair debris becomes gator furniture, leaf piles become hurricane gadgets, coolers become command hardware, and flip-flops become storm gear.

## Platform

Portrait, offline, touch-first. The five-column, six-row board uses adjacent swaps, tap-to-select, gravity refill, move-limited jobs, cascades, and earned power-ups. Progress saves locally on-device.
