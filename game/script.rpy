init python:
    FM_CHAINS = {
        "wood": ["wood:0", "wood:1", "wood:2", "wood:3", "wood:4"],
        "chair": ["chair:0", "chair:1", "chair:2", "chair:3", "chair:4"],
        "leaf": ["leaf:0", "leaf:1", "leaf:2", "leaf:3", "leaf:4"],
        "cooler": ["cooler:0", "cooler:1", "cooler:2", "cooler:3", "cooler:4"],
        "flop": ["flop:0", "flop:1", "flop:2", "flop:3", "flop:4"],
    }
    FM_META = {
        "wood:0": ("🪵", "Waterlogged plank"),
        "wood:1": ("🪚", "Dry plank stack"),
        "wood:2": ("📐", "Porch lumber"),
        "wood:3": ("🏠", "Instant porch kit"),
        "wood:4": ("🏰", "Trailer-palace plans"),
        "chair:0": ("🪑", "Busted lawn chair"),
        "chair:1": ("🪑", "Repaired chair"),
        "chair:2": ("🛋️", "Gator recliner"),
        "chair:3": ("👑", "Gator lifeguard throne"),
        "chair:4": ("🐊", "Mayor's chair"),
        "leaf:0": ("🌴", "Loose palm frond"),
        "leaf:1": ("🍃", "Yard pile"),
        "leaf:2": ("🧹", "Turbo broom"),
        "leaf:3": ("🌪️", "Hurricane leaf cannon"),
        "leaf:4": ("🚀", "Storm steering wheel"),
        "cooler:0": ("🧊", "Empty cooler"),
        "cooler:1": ("🎣", "Bait cooler"),
        "cooler:2": ("🛞", "Rolling bait cooler"),
        "cooler:3": ("📡", "Satellite bait cooler"),
        "cooler:4": ("🐊", "Gator command center"),
        "flop:0": ("🩴", "Lone flip-flop"),
        "flop:1": ("🩴", "Matching pair"),
        "flop:2": ("🛡️", "Gator-proof sandals"),
        "flop:3": ("🚀", "Storm-surfing sandals"),
        "flop:4": ("☁️", "Cloud-surfing flip-flops"),
    }
    FM_JOBS = [
        {
            "name": "FIX THE PORCH",
            "story": "The storm took the steps. Dale says the gator is now in charge of inspections.",
            "needs": [("wood:2", 1), ("chair:1", 1)],
            "reward": 120,
            "repair": "The steps are back. The gator has accepted the repaired chair as a permit fee.",
        },
        {
            "name": "CLEAR THE BAIT SHOP",
            "story": "The bait shop is buried in palm debris. The owner's cleanup plan involves a suspiciously powerful broom.",
            "needs": [("leaf:3", 1), ("cooler:1", 1)],
            "reward": 220,
            "repair": "The shop is open again. The hurricane leaf cannon has been placed behind the counter. Probably safe.",
        },
        {
            "name": "BUILD GATOR COMMAND",
            "story": "The neighborhood gator has a clipboard now. Apparently it wants a mobile command center and storm-surfing sandals.",
            "needs": [("cooler:4", 1), ("flop:3", 1)],
            "reward": 500,
            "repair": "The gator's command center is operational. The sandals are already on the roof.",
        },
    ]
    FM_SPAWN_ORDER = [
        ["wood:0", "wood:0", "chair:0", "wood:0", "chair:0", "leaf:0", "cooler:0", "flop:0"],
        ["leaf:0", "leaf:0", "cooler:0", "leaf:0", "cooler:0", "leaf:0", "flop:0"],
        ["cooler:0", "cooler:0", "flop:0", "cooler:0", "flop:0", "leaf:0"],
    ]

    def fm_reset_game():
        store.board = [None] * 20
        starter = ["wood:0", "wood:0", "wood:0", "chair:0", "chair:0", "leaf:0", "cooler:0", "flop:0"]
        for i, item in enumerate(starter):
            store.board[i] = item
        store.job_index = 0
        store.coins = 0
        store.moves = 0
        store.spawn_count = 0
        store.selected_tile = -1
        store.toast = "Tap two matching pieces to merge them."
        store.game_finished = False

    def fm_next_item(item):
        family, level_text = item.split(":")
        chain = FM_CHAINS[family]
        level = int(level_text)
        if level + 1 >= len(chain):
            return None
        return chain[level + 1]

    def fm_drag_drop(drags, drop):
        if not drags:
            return
        try:
            source = int(drags[0].drag_name)
        except (TypeError, ValueError):
            return
        if drop is None:
            if store.board[source]:
                store.selected_tile = source
                store.toast = "Selected " + FM_META[store.board[source]][1] + ". Tap CLEAR SELECTED to discard it."
            renpy.restart_interaction()
            return
        try:
            target = int(drop.drag_name)
        except (TypeError, ValueError):
            return
        if source == target or not store.board[source]:
            return
        source_item = store.board[source]
        target_item = store.board[target]
        if target_item is None:
            store.board[target] = source_item
            store.board[source] = None
            store.selected_tile = -1
            store.toast = "Moved " + FM_META[source_item][1] + "."
        elif target_item == source_item:
            upgraded = fm_next_item(source_item)
            if upgraded:
                store.board[target] = upgraded
                store.board[source] = None
                store.selected_tile = -1
                store.moves += 1
                store.toast = "Merged into " + FM_META[upgraded][1] + "!"
            else:
                store.toast = "That one's already as bizarre as it gets."
        else:
            store.toast = "Those don't match. Try two identical pieces."
        renpy.restart_interaction()

    def fm_spawn():
        try:
            slot = store.board.index(None)
        except ValueError:
            store.toast = "Board's full. Merge or clear a piece first."
            return
        order = FM_SPAWN_ORDER[min(store.job_index, len(FM_SPAWN_ORDER) - 1)]
        item = order[store.spawn_count % len(order)]
        store.spawn_count += 1
        store.board[slot] = item
        store.toast = "Found: " + FM_META[item][1] + "."

    def fm_clear_selected():
        if store.selected_tile < 0 or not store.board[store.selected_tile]:
            store.toast = "Tap a piece first, then clear it."
            return
        name = FM_META[store.board[store.selected_tile]][1]
        store.board[store.selected_tile] = None
        store.selected_tile = -1
        store.toast = "Cleared " + name + "."

    def fm_job_ready():
        job = FM_JOBS[store.job_index]
        remaining = list(store.board)
        for item, count in job["needs"]:
            for _ in range(count):
                if item not in remaining:
                    return False
                remaining.remove(item)
        return True

    def fm_turn_in():
        if not fm_job_ready():
            store.toast = "Still missing a requested item. Check the job card."
            return
        job = FM_JOBS[store.job_index]
        for item, count in job["needs"]:
            for _ in range(count):
                store.board.remove(item)
                store.board.append(None)
        store.coins += job["reward"]
        store.toast = job["repair"]
        store.job_index += 1
        if store.job_index >= len(FM_JOBS):
            store.game_finished = True
        renpy.restart_interaction()

default board = [None] * 20
default job_index = 0
default coins = 0
default moves = 0
default spawn_count = 0
default selected_tile = -1
default toast = "Tap two matching pieces to merge them."
default game_finished = False

label start:
    $ fm_reset_game()
    call screen merge_game
    return
