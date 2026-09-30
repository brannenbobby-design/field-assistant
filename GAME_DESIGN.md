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

The nine-stop map runs from Panama City Beach through the Panhandle and peninsula to Key West. Each new level raises both its debris quota and move budget a little. Procedural Web Audio effects play for taps, swaps, clears, powers, and job wins; a saved sound toggle is available on both the map and board.

## Jobs

The Florida map links the cleanup jobs into a small story route. The storm has passed, but a strange signal from a satellite cooler sends Florida Man along the Gulf Coast.

1. **Panama City Beach — Fix the Porch:** clear 10 soggy planks and 10 busted lawn chairs in 24 moves.
2. **Apalachicola — Clear the Bait Shop:** clear 12 palm-frond piles and 12 coolers in 26 moves.
3. **Tallahassee — Find the Signal:** clear 14 satellite bait coolers and 14 lone flip-flops in 28 moves.
4. **Gainesville — Round Up the Exhibits:** clear 16 gator recliners and 16 hurricane leaf cannons in 30 moves.
5. **Jacksonville — Clear the Marina:** clear 18 storm-surfing sandals and 18 gator recliners in 32 moves.
6. **Orlando — Unjam the Gator Park:** clear 20 satellite bait coolers and 20 instant porch kits in 34 moves.
7. **Tampa Bay — Build Gator Command:** clear 22 gator command centers and 22 cloud-surfing flip-flops in 36 moves.
8. **Everglades — Secure the Glades:** clear 24 storm steering wheels and 24 mayor's thrones in 38 moves.
9. **Key West — Save the Keys:** clear 26 trailer-palace plans and 26 cloud-surfing flip-flops in 40 moves.

Tile art/names escalate by job: porch junk becomes improbable construction equipment, chair debris becomes gator furniture, leaf piles become hurricane gadgets, coolers become command hardware, and flip-flops become storm gear.

## Platform

Portrait, offline, touch-first. The five-column, six-row board uses adjacent swaps, tap-to-select, animated gravity refill, cascades, earned power-ups, and local save data.
