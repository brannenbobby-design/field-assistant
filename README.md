# Florida Man: Unsupervised

Original Ren'Py adventure game set on Florida's Gulf Coast.

## v0.3.1 Android test scope

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
2. **Laundry Room Rescue** is now a real reachable map job
3. **A Simple Delivery** now has a complete Storage Lot → Sand Trap quest line
4. Choices affect stats, relationships, reputation, rent balance, and payout
5. End Tuesday at Wayne's House to reach the Wednesday teaser

## Core systems
- Money and rent balance
- Time-of-day progression
- Stats: Charm, Grit, Street Smarts, Dumb Luck
- Inventory
- Active/completed quests
- Dale/Kelsey relationship values
- Reputation
- Dynamic phone messages
- Touch-friendly HUD and map
- Ren'Py save/load support

## v0.3/v0.3.1 fixes
- Prevented **Boat With No Name** from being completed repeatedly for duplicate $150 rewards.
- Made both Tuesday quests reachable from normal gameplay.
- Added guarded quest/inventory updates to prevent duplicate entries and unsafe removals.
- Made the Florida header reflect the actual current day/time instead of staying stuck on Monday morning.
- Updated Android/build version metadata to 0.3.1.
- Fixed the Android startup overlay bug: the teal fill is now a true scene background instead of a persistent full-screen screen layer that could cover dialogue and choices.

## Running
Open this folder as a Ren'Py project using Ren'Py 8.5.3 or install the Android artifact produced by GitHub Actions.

Current milestone: validate the full Monday → Tuesday → Wednesday-teaser path on Android before expanding Wednesday.
