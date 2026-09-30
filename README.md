# Florida Man: Storm Cleanup

A portrait Android match-three puzzle built around the board: icon-only debris pieces, visual job targets, a move counter beside the grid, and a compact Gulf Coast level scene. Swap neighboring storm debris to make matches, clear job targets, and watch the board animate as pieces slide, pop, fall, and refill.

## How to play

- Swap neighboring pieces. The swap sticks only when it creates a match of three or more.
- Matched items pop away; survivors slide down and fresh junk falls in from above.
- Match four for a row or column blast, five for a rainbow gator, or make a T/L match for a hurricane bomb.
- Tap a power-up or swap it into a match to fire it.
- Clear the job's debris quota before the move counter runs out. Leftover moves earn more stars.

## Build

The game runs offline as local HTML/CSS/JavaScript inside a native Android WebView. Build with `gradle :app:assembleRelease`. GitHub Actions runs gameplay checks and produces a signed APK on pushes to `main`.

The package ID `com.brannenservices.floridamanunsupervised` and existing development signing key are retained, so version 1.0.5 can update the prior test APK. The test key is for development installs, not Google Play publishing.
