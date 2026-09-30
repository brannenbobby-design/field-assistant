# Florida Man: Storm Cleanup — Match-three prototype

## Core loop

Swap two neighboring storm-debris tiles. Horizontal or vertical runs of three and complete 2×2 squares stick; all four tiles in a square clear together. Merely touching three pieces in an L does not count. Matched tiles pop away, survivors slide down, and fresh debris drops in from above. Cascades resolve in animated waves. Invalid swaps snap back and cost nothing. Swapping a power into a match resolves both the power and any newly formed line or square in the same wave.

Each cleanup job sets two debris quotas and a move limit. Clear both quotas to finish; leftover moves award more stars. After the final clear animation, a star-rated job-complete window appears and returns to the Florida map automatically after a short celebration. The board refills continuously, so the goal is clearing the requested mess rather than leaving a permanently empty board. If there are no legal swaps, the board reshuffles for free.

## Power-ups

- Match four horizontally or vertically: earn a row or column blast.
- Match five: earn a rainbow gator that clears every piece of its debris type.
- Make a T/L intersection: earn a hurricane bomb.
- Tap a power-up or swap it into a match to fire it. It consumes one move and can start a cascade.

## Visuals

The play screen layers a translucent board and mission HUD over a full-screen Gulf Coast porch scene. The compact level/coin HUD leads into icon-and-count job targets; the move counter sits beside the grid, and the inactive next-job button stays out of the way during play. Bright sky, coral, aqua, and gold accents tie the interface together. The board uses an original transparent sprite sheet with 15 custom colorful 3D items: ordinary storm junk, strange upgrades, and full Florida nonsense. Swaps slide, matches pop with a sparkle burst, power-ups pulse, survivors animate into their new cells, and replacement pieces fall from above.

## Jobs

The Florida map links the cleanup jobs into a small story route. The storm has passed, but a strange signal from a satellite cooler sends Florida Man along the Gulf Coast.

1. **Panama City Beach — Fix the Porch:** clear 10 soggy planks and 10 busted lawn chairs in 24 moves. The neighborhood gator is already moving into the recliner.
2. **Apalachicola — Clear the Bait Shop:** clear 12 palm-frond piles and 12 coolers in 26 moves. The team traces the radio signal before high tide.
3. **Tampa Bay — Build Gator Command:** clear 14 satellite bait coolers and 14 storm-surfing sandals in 28 moves. Turn the salvage into an unnecessary command center before the next squall.

Tile art/names escalate by job: porch junk becomes improbable construction equipment, chair debris becomes gator furniture, leaf piles become hurricane gadgets, coolers become command hardware, and flip-flops become storm gear.

## Platform

Portrait, offline, touch-first. The five-column, six-row board uses adjacent swaps, tap-to-select, animated gravity refill, cascades, earned power-ups, and local save data.
