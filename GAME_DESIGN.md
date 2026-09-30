# Florida Man: Storm Cleanup — Match-three prototype

## Core loop

Swap two neighboring storm-debris tiles. Only swaps that create a horizontal or vertical run of at least three stick. Matched tiles pop away, survivors slide down, and fresh debris drops in from above. Cascades resolve in animated waves. Invalid swaps snap back and cost nothing.

Each cleanup job sets two debris quotas and a move limit. Clear both quotas to finish; leftover moves award more stars. The board refills continuously, so the goal is clearing the requested mess rather than leaving a permanently empty board. If there are no legal swaps, the board reshuffles for free.

## Power-ups

- Match four horizontally or vertically: earn a row or column blast.
- Match five: earn a rainbow gator that clears every piece of its debris type.
- Make a T/L intersection: earn a hurricane bomb.
- Tap a power-up or swap it into a match to fire it. It consumes one move and can start a cascade.

## Visuals

The play screen uses a wide, uncropped Gulf Coast scene above a compact level and coin HUD, leaving more room for the board. Bright sky, coral, aqua, and gold accents carry through the job card, board frame, and controls. The board uses an original transparent sprite sheet with 15 custom colorful 3D items: ordinary storm junk, strange upgrades, and full Florida nonsense. Swaps slide, matches pop with a sparkle burst, power-ups pulse, survivors animate into their new cells, and replacement pieces fall from above.

## Jobs

1. Fix the Porch: clear 10 soggy planks and 10 busted lawn chairs in 24 moves.
2. Clear the Bait Shop: clear 12 palm-frond piles and 12 coolers in 26 moves.
3. Build Gator Command: clear 14 satellite bait coolers and 14 storm-surfing sandals in 28 moves.

Tile art/names escalate by job: porch junk becomes improbable construction equipment, chair debris becomes gator furniture, leaf piles become hurricane gadgets, coolers become command hardware, and flip-flops become storm gear.

## Platform

Portrait, offline, touch-first. The five-column, six-row board uses adjacent swaps, tap-to-select, animated gravity refill, cascades, earned power-ups, and local save data.
