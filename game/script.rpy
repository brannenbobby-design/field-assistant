define w = Character("Wayne")
define d = Character("Dale")
define k = Character("Kelsey")

default money = 23.17
default gas = 25
default day = "Monday"
default tod = "Morning"
default charm = 0
default grit = 0
default street_smarts = 0
default dumb_luck = 0
default inventory = ["Phone", "Wallet", "Mystery Key"]
default active_quests = []
default completed_quests = []
default quest_opportunity = False
default quest_boat = False
default battery_acquired = False
default fuel_acquired = False
default key_acquired = False

init python:
    periods = ["Morning", "Afternoon", "Evening", "Night"]
    def advance_time():
        global tod
        i = periods.index(tod)
        if i < len(periods)-1:
            tod = periods[i+1]

screen hud():
    frame:
        xalign 0.02
        yalign 0.02
        padding (14, 10)
        vbox:
            text "[day] — [tod]"
            text "$[money:.2f]   Gas: [gas]%"
    textbutton "PHONE" action Show("phone_screen") xalign 0.98 yalign 0.02
    textbutton "MAP" action Jump("town_map") xalign 0.98 yalign 0.09

screen phone_screen():
    modal True
    frame:
        xalign .5
        yalign .5
        xsize 720
        ysize 760
        padding (28, 24)
        vbox:
            spacing 18
            text "WAYNE'S PHONE" size 42
            text "Messages" size 28
            text "LANDLORD: Rent?"
            text "KELSEY: I need my cooler back. And don't tell me you lost it."
            text "DALE: You awake? Got an opportunity."
            null height 12
            text "Active Quests" size 28
            if active_quests:
                for q in active_quests:
                    text "• [q]"
            else:
                text "Nothing active. Somehow."
            null height 12
            text "Inventory" size 28
            text ", ".join(inventory)
            textbutton "Close" action Hide("phone_screen")

screen town_map_screen():
    modal True
    frame:
        xalign .5
        yalign .5
        xsize 900
        padding (30, 25)
        vbox:
            spacing 14
            text "GULF COAST — WHERE TO?" size 38
            textbutton "Wayne's House" action Jump("wayne_house")
            textbutton "Dale's Place" action Jump("dale_house")
            textbutton "Gator Mart" action Jump("gator_mart")
            textbutton "Beach" action Jump("beach")
            if tod in ["Afternoon", "Evening", "Night"]:
                textbutton "The Sand Trap" action Jump("sand_trap")
            else:
                text "The Sand Trap — too early for respectable bad decisions."

label start:
    scene black
    centered "6:47 AM — MONDAY"
    "A phone alarm screams for the third time."
    w "Why am I on the couch?"
    menu:
        "I remember exactly why.":
            $ street_smarts += 1
        "I have absolutely no idea.":
            $ dumb_luck += 1
        "Better question: whose couch is this?":
            $ charm += 1
    "Your phone buzzes."
    "LANDLORD: Rent?"
    "KELSEY: I need my cooler back. And don't tell me you lost it."
    "DALE: You awake?"
    "DALE: Got an opportunity."
    menu:
        "What kind of opportunity?":
            pass
        "Is it legal?":
            $ street_smarts += 1
        "How much?":
            pass
        "No.":
            d "Cool. I'll pick you up in 20."
    $ quest_opportunity = True
    $ active_quests.append("An Opportunity")
    "QUEST STARTED: An Opportunity"
    show screen hud
    jump town_map

label town_map:
    call screen town_map_screen
    jump town_map

label wayne_house:
    "Home. The AC is trying its best, which is more than can be said for Wayne."
    menu:
        "Check phone":
            show screen phone_screen
            $ renpy.pause(hard=True)
        "Take a nap (advance time)":
            $ advance_time()
            "Against all odds, this counts as planning."
        "Back to map":
            pass
    jump town_map

label dale_house:
    if not quest_boat:
        $ advance_time()
        "An airboat is sitting halfway across Dale's lawn. It is not on a trailer."
        w "I'm going to regret asking this."
        d "Good news. I got an opportunity."
        d "Six hundred bucks. You get a hundred-fifty if we get her running."
        w "Whose boat is it?"
        d "That's kind of a philosophical question."
        $ quest_boat = True
        $ active_quests.remove("An Opportunity")
        $ completed_quests.append("An Opportunity")
        $ active_quests.append("Boat With No Name")
        "QUEST STARTED: Boat With No Name"
        "Needs: fuel, battery, ignition key."
    else:
        "The airboat remains exactly where an airboat should not be."
        if battery_acquired and fuel_acquired and key_acquired:
            jump finish_boat
    jump town_map

label gator_mart:
    "The Gator Mart smells like coffee, bait, and an electrical fire nobody has investigated."
    if quest_boat and not fuel_acquired:
        menu:
            "Buy fuel can and gas — $12":
                if money >= 12:
                    $ money -= 12
                    $ fuel_acquired = True
                    $ inventory.append("Fuel Can")
                    "Fuel acquired."
                else:
                    "Your wallet disagrees."
            "Ask the clerk if Dale has a tab":
                $ charm += 1
                "CLERK: He did. Past tense."
            "Leave":
                pass
    jump town_map

label beach:
    "White sand, Gulf water, and at least one person who brought a Bluetooth speaker nobody asked for."
    if quest_boat and not key_acquired:
        "Something metallic is half buried near an abandoned cooler."
        menu:
            "Pick it up":
                $ key_acquired = True
                $ inventory.append("Unlabeled Boat Key")
                $ dumb_luck += 1
                "An unlabeled ignition key. Surely this is fine."
            "Leave it alone":
                $ street_smarts += 1
    jump town_map

label sand_trap:
    if tod == "Evening" and not evening_intro_seen and "Boat With No Name" in completed_quests:
        jump sand_trap_evening
    "The Sand Trap: cold beer, questionable karaoke, and several people who owe Dale money."
    if quest_boat and not battery_acquired:
        d "Peanut says there's a spare marine battery behind the shed."
        menu:
            "Buy a used battery from Peanut — $10":
                if money >= 10:
                    $ money -= 10
                    $ battery_acquired = True
                    $ inventory.append("Marine Battery")
                    "Peanut accepts ten dollars and absolutely no questions."
                else:
                    "Peanut looks at your wallet and laughs."
            "Offer to do Peanut a favor instead":
                $ grit += 1
                $ battery_acquired = True
                $ inventory.append("Marine Battery")
                "PEANUT: Fine. But now you owe me."
            "Walk away":
                pass
    jump town_map

label finish_boat:
    "Wayne installs the battery, pours in the fuel, and tries the mystery key."
    "The airboat coughs twice, launches a cloud of smoke, and starts."
    d "See? Basically legal."
    $ money += 150
    $ active_quests.remove("Boat With No Name")
    $ completed_quests.append("Boat With No Name")
    "QUEST COMPLETE: Boat With No Name"
    "Wayne earned $150."
    $ tod = "Evening"
    "Monday evening is now open."
    jump town_map


default kelsey_relationship = 0
default dale_relationship = 0
default cooler_found = False
default evening_intro_seen = False

label sand_trap_evening:
    $ evening_intro_seen = True
    "Neon buzzes over the Sand Trap's front door."
    "Inside, Dale is arguing with a dartboard like it owes him money."
    d "There he is. The only man I know who can turn twenty-three dollars into an airboat."
    $ dale_relationship += 1
    "Wayne's phone buzzes."
    k "You have until tomorrow to bring my cooler back."
    menu:
        "Tell Kelsey you'll find it.":
            $ kelsey_relationship += 1
            w "I'll find it."
        "Ask what's so important about the cooler.":
            $ street_smarts += 1
            k "What's inside it is none of your business."
            w "Well now it's definitely my business."
        "Ignore the message.":
            $ grit += 1
    if "The Cooler Incident" not in active_quests:
        $ active_quests.append("The Cooler Incident")
    "NEW QUEST: The Cooler Incident"
    jump town_map

label cooler_search:
    "Behind Wayne's house sits a pile of things that were going to be dealt with tomorrow."
    if not cooler_found:
        $ cooler_found = True
        $ inventory.append("Kelsey's Cooler")
        "Under a beach chair and one traffic cone: Kelsey's cooler."
        w "That was easier than expected."
        "The cooler is locked."
    jump town_map

label return_cooler:
    "Kelsey meets Wayne outside Gator Mart."
    k "Please tell me you didn't open it."
    menu:
        "Of course not.":
            $ charm += 1
            $ kelsey_relationship += 2
        "It was locked.":
            $ street_smarts += 1
            $ kelsey_relationship += 1
        "I considered power tools.":
            $ dumb_luck += 1
            k "That's exactly why it was locked."
    $ inventory.remove("Kelsey's Cooler")
    $ active_quests.remove("The Cooler Incident")
    $ completed_quests.append("The Cooler Incident")
    "QUEST COMPLETE: The Cooler Incident"
    jump town_map
