# Florida Man: Storm Cleanup

A portrait Android match-three puzzle, wrapped in a small native Android app and built around the generated Gulf Coast art. Swap neighboring storm debris to make matches, clear the job's requested junk, and watch gravity refill the board with increasingly bizarre Florida salvage.

## How to play

- Swap two neighboring pieces. The swap only sticks if it makes a match of three or more.
- Matched pieces clear; the tiles above fall down and new debris drops in from the top.
- Match four in a row to earn a row or column blast. Match five for a rainbow gator; a T/L match earns a hurricane bomb.
- Use the power-up by tapping it or swapping it into a match.
- Clear the target debris before the move counter runs out. Leftover moves earn more stars.
- If a board runs out of legal swaps, it reshuffles automatically without spending a move.

## Build

The game runs offline as local HTML/CSS/JavaScript inside a native Android WebView. Build with `gradle :app:assembleRelease`. GitHub Actions runs the gameplay checks and produces a signed APK on pushes to `main`.

The package ID `com.brannenservices.floridamanunsupervised` and existing development signing key are retained, so version 1.0.2 can update the prior test APK. The test key is for development installs, not Google Play publishing.
