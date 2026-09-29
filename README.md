# Florida Man: Unsupervised

Original Ren'Py adventure game set on Florida's Gulf Coast.

## v0.3.3 Android test scope

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
5. End Tuesday at Wayne's House to reach the Wednesday teaser

## v0.3.3 presentation upgrade
- Eight illustrated Gulf Coast location backgrounds
- Wayne, Dale, corrected Kelsey, Frank, and Peanut character sprites
- Dedicated bottom dialogue box so narration no longer collides with HUD text
- Large touch-friendly choice buttons
- HUD/status information restricted to the free-roam map
- Scrollable phone screen with messages, quests, inventory, and stats
- Scene-specific character placement instead of characters floating over every screen
- Frank now appears in the rent/laundry scenes; Peanut appears in the battery and swordfish-delivery scenes
- Named-character dialogue now uses Ren'Py Character speakers with distinct name colors
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

Current milestone: validate the fully illustrated Monday → Tuesday → Wednesday-teaser path on Android with the five-character cast, then expand Wednesday and add expression/pose variants.
