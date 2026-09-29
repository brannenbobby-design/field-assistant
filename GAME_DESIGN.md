# First Playable Design

## Core loop

1. Tap the salvage button to add a basic object to the 5×4 board.
2. Drag matching objects together to merge them; drag an object to an empty cell to reposition it.
3. Complete the current job card's requested items.
4. Deliver the items to earn coins and reveal the repaired location and next job.

## First chapter

- Fix the Porch: porch lumber + repaired chair.
- Clear the Bait Shop: hurricane leaf cannon + bait cooler.
- Build Gator Command: gator command center + storm-surfing sandals.

Each chain has five tiers. The early tiers are normal storm salvage; later tiers get increasingly absurd. The first playable version has no energy timer or online service. Salvage is unlimited; the board creates the puzzle through limited space and item management.

## Controls and presentation

Portrait Android layout. Touch drag handles merging and moving. The item board occupies the center; the job card sits above it; salvage and cleanup controls sit below. The visual direction follows bright polished 3D casual-game art with coastal colors and an expressive Florida handyman.

## Build validation

GitHub Actions runs Ren'Py lint, validates package/version/orientation and game content, then builds and uploads the Android APK. The prior stable test signing key is retained for install-over testing.
