# Florida Man: Storm Cleanup

A portrait Android merge puzzle built as a small native Android shell around a local HTML5 game. The player taps to salvage storm debris, combines identical pieces into increasingly bizarre upgrades, and delivers exact items to rebuild a Gulf Coast neighborhood.

## How to play

- Tap **TAP FOR SALVAGE** to add storm debris to the board.
- Drag an item onto an identical item at the same tier to merge them.
- Mismatched items and empty slots reject drops; there are no free moves.
- Scrap an unwanted item to free a slot, make the job card's requested pieces, and deliver the job.
- Three jobs lead from repairing a porch to building a gator command center.

## Build

The Android app uses a WebView for local, offline HTML/CSS/JavaScript gameplay. Android Studio/Gradle builds the APK with `gradle :app:assembleRelease`. The GitHub Actions workflow runs gameplay checks and produces a signed test APK on pushes to `main`.

Package ID `com.brannenservices.floridamanunsupervised` and the existing development signing key are retained so version 1.0.1 can update the already-installed 1.0.0 test app. The test key is for development installs, not Google Play publishing.
