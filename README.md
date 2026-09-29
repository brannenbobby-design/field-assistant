# Florida Man: Storm Cleanup

A portrait Android merge-and-restoration game built with Ren'Py 8.5.3. The player salvages storm debris, merges matching pieces into increasingly bizarre gear, and fulfills jobs to rebuild a Gulf Coast neighborhood.

## First playable scope

- Three jobs: Fix the Porch, Clear the Bait Shop, and Build Gator Command.
- Five merge chains with five tiers apiece, from ordinary storm junk to Florida-grade nonsense.
- Drag pieces onto matching pieces to merge; drag to an empty cell to move.
- Tap for salvage, clear unwanted pieces, then deliver requested items to complete each job.
- Coins, merge count, save/load, pause, settings, and replay.
- Portrait layout with generated 3D Gulf Coast scene art.

## Android build

Pushes to `main` run Ren'Py lint and build a signed test APK. The package ID and development keystore are retained so the test build can install as an update over the prior test app. The development key is not for Play Store production signing.
