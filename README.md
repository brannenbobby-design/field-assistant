# Florida Man: Unsupervised

Original Ren'Py adventure game set on Florida's Gulf Coast.

## v0.3.8 Android test scope

### Monday
1. Opening choices and phone messages
2. Town map
3. Dale's Place — start **Boat With No Name**
4. Gator Mart — obtain fuel
5. Beach — find ignition key
6. Sand Trap — obtain battery
7. Dale's Place — finish the boat and earn $150
8. Sand Trap evening — unlock **The Cooler Incident**
9. Wayne's House — find Kelsey's cooler
10. Gator Mart — return cooler
11. Wayne's House — sleep into Tuesday

### Tuesday
1. Deal with Frank the landlord or follow Dale's new lead
2. **Laundry Room Rescue**
3. **A Simple Delivery** through Storage Unit 14 and the Sand Trap
4. Choices affect stats, relationships, reputation, rent balance, and payout
5. End Tuesday at Wayne's House to reach Wednesday

### Wednesday
1. Discover Dale's inert surplus turbine core in Wayne's yard
2. Start **Absolutely Not**
3. Confirm the turbine is an inert display core
4. Choose to sell it to Peanut, return it to the surplus yard, or let Dale keep it
5. Each route changes money, rent, reputation, inventory, and relationships
6. Finish Wednesday with a route-specific ending

## v0.3.8 presentation upgrade
- Eight illustrated Gulf Coast location backgrounds
- Wayne, Dale, corrected Kelsey, Frank, and Peanut character sprites
- Dedicated bottom dialogue box so narration no longer collides with HUD text
- Large touch-friendly choice buttons
- HUD/status information restricted to the free-roam map
- Full smartphone-style phone UI with hardware bezel, status bar, app header, message cards, gesture bar, and four app tabs
- Phone tabs for Messages, Jobs, Inventory, and Stats
- Fixed the story-phone freeze by returning correctly from a called phone screen while still allowing map-overlay phone use
- Scene-specific character placement instead of characters floating over every screen
- Character clarity pass prevents Wayne, Dale, and Peanut from appearing as three near-identical full-body figures at once
- Peanut completely redesigned as a large, bald, heavily muscled white guy in a black tank, work shorts, and boots so his silhouette is unmistakably different from Dale
- Sand Trap scenes now use sequential POV-style staging: Dale and Peanut enter one at a time as they become relevant
- Dialogue window is hard-anchored to the bottom of the Android layout with a visible continuation cue
- Frank now appears in the rent/laundry scenes; Peanut appears in the battery and swordfish-delivery scenes
- Named-character dialogue now uses Ren'Py Character speakers with distinct name colors
- New cinematic title screen built from real in-game art and sprites
- Dedicated pause menu with save, load, settings, and main-menu navigation
- Six visible save slots with screenshots and timestamps
- Text speed, auto-forward, sound, music, and transition settings
- Animated character entrances and 0.25-second scene dissolves
- Menu access added directly to the free-roam HUD
- Animated quest-start/completion notifications replace prototype quest text
- Live ACTIVE JOB panel on the town map with context-sensitive objectives
- Full Wednesday chapter with three meaningful turbine outcomes
- Stable development signing key for repeatable Android test updates

## Core systems
- Money and rent balance
- Time-of-day progression
- Stats: Charm, Grit, Street Smarts, Dumb Luck
- Inventory
- Active/completed quests
- Dale/Kelsey relationship values
- Reputation
- Dynamic phone messages
- Ren'Py save/load support

## Previous fixes retained
- Prevented **Boat With No Name** from being completed repeatedly for duplicate $150 rewards.
- Made both Tuesday quests reachable from normal gameplay.
- Added guarded quest/inventory updates.
- Fixed the Android startup overlay bug.

## Android signing
The checked-in keystore is intentionally a **development/test key only** so successive test APKs can update each other. It must never be reused as a production Google Play signing key.

## Running
Open this folder as a Ren'Py project using Ren'Py 8.5.3 or install the Android artifact produced by GitHub Actions.

Current milestone: validate the v0.3.8 Peanut redesign and existing character-staging fixes on Android, then add true expression/pose variants and a dedicated audio pass.
