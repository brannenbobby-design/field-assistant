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

default kelsey_relationship = 0
default dale_relationship = 0
default cooler_found = False
default evening_intro_seen = False

default reputation = 0
default day_number = 1
default rent_due = 475
default landlord_warning = 0
default tuesday_started = False
default delivery_stage = 0
default delivery_strapped = False
default delivery_damage = False

init python:
    periods = ["Morning", "Afternoon", "Evening", "Night"]

    def advance_time():
        global tod
        i = periods.index(tod)
        if i < len(periods) - 1:
            tod = periods[i + 1]

# Compatibility no-op screens for saves made before v0.3.2.
screen florida_backdrop():
    null width 1 height 1

screen hud():
    null width 1 height 1

screen phone_screen():
    modal True
    add Solid("#061015f2")

    frame:
        style "fm_panel"
        xalign 0.5
        yalign 0.5
        xsize 940
        ysize 900

        vbox:
            spacing 14

            text "WAYNE'S PHONE" style "fm_title"
            text "[day] — [tod]   |   $[money:.2f]   |   Gas [gas]%" size 27 color "#9fd8df"

            viewport:
                ymaximum 670
                mousewheel True
                draggable True

                vbox:
                    spacing 14
                    xsize 840

                    text "MESSAGES" size 29 color "#ffd166"

                    if rent_due > 0:
                        text "LANDLORD: Rent due: $[rent_due]." size 27

                    if "The Cooler Incident" in active_quests:
                        text "KELSEY: Cooler. Today. Seriously." size 27
                    elif "The Cooler Incident" in completed_quests:
                        text "KELSEY: I got the cooler. We're still discussing the power-tools comment." size 27

                    if "A Simple Delivery" in active_quests:
                        if delivery_stage == 1:
                            text "DALE: Storage lot. Unit 14. Bring straps." size 27
                        elif delivery_stage == 2:
                            text "DALE: Take the fish to the Sand Trap. Don't ask." size 27
                    elif day_number >= 2 and "A Simple Delivery" not in completed_quests:
                        text "DALE: Boat buyer has another job. Might pay better." size 27
                    elif "Boat With No Name" in active_quests:
                        text "DALE: Boat still needs fuel, battery, key." size 27
                    else:
                        text "DALE: You awake?" size 27

                    null height 8
                    text "ACTIVE QUESTS" size 29 color "#ffd166"

                    if active_quests:
                        for q in active_quests:
                            text "• [q]" size 27
                    else:
                        text "Nothing active. Somehow." size 27

                    null height 8
                    text "INVENTORY" size 29 color "#ffd166"
                    text ", ".join(inventory) size 27

                    null height 8
                    text "STATS" size 29 color "#ffd166"
                    text "Charm [charm]   Grit [grit]" size 27
                    text "Street Smarts [street_smarts]   Dumb Luck [dumb_luck]" size 27
                    text "Dale [dale_relationship]   Kelsey [kelsey_relationship]   Reputation [reputation]" size 27

            textbutton "CLOSE" action Hide("phone_screen") xalign 0.5

screen town_map_screen():
    modal True

    add "images/bg_beach.webp" xysize (1920, 1080)
    add Solid("#04101499")

    frame:
        xfill True
        ysize 104
        background Solid("#071216e8")
        padding (32, 18)

        hbox:
            xfill True

            vbox:
                xsize 580
                text "FLORIDA MAN: UNSUPERVISED" size 25 color "#9fd8df"
                text "[day] — [tod]" size 34 color "#ffd166"

            vbox:
                xsize 650
                text "$[money:.2f]    GAS [gas]%" size 30
                if day_number >= 2:
                    text "RENT DUE $[rent_due]" size 25 color "#f6c67b"

            textbutton "PHONE":
                action Show("phone_screen")
                xalign 1.0
                xsize 260
                yminimum 64

    frame:
        style "fm_panel"
        xalign 0.07
        yalign 0.58
        xsize 720

        vbox:
            spacing 11

            text "WHERE TO?" style "fm_title"

            textbutton "Wayne's House" action Jump("wayne_house") xfill True
            textbutton "Dale's Place" action Jump("dale_house") xfill True
            textbutton "Gator Mart" action Jump("gator_mart") xfill True
            textbutton "Beach" action Jump("beach") xfill True

            if "Laundry Room Rescue" in active_quests:
                textbutton "Apartment Laundry Room — JOB" action Jump("laundry_room_rescue") xfill True

            if "A Simple Delivery" in active_quests and delivery_stage == 1:
                textbutton "Storage Lot — UNIT 14" action Jump("storage_lot") xfill True

            if tod in ["Afternoon", "Evening", "Night"]:
                if "A Simple Delivery" in active_quests and delivery_stage == 2:
                    textbutton "The Sand Trap — MAKE DELIVERY" action Jump("sand_trap") xfill True
                else:
                    textbutton "The Sand Trap" action Jump("sand_trap") xfill True
            else:
                text "The Sand Trap — too early for respectable bad decisions." size 24 color "#b8b8b8"

    text "Tap a destination. Story scenes keep the HUD out of the way." size 22 color "#d7ddd9" xalign 0.97 yalign 0.96

label start:
    scene bg wayne_house
    show wayne at fm_center

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

    if "An Opportunity" not in active_quests:
        $ active_quests.append("An Opportunity")

    "QUEST STARTED: An Opportunity"
    jump town_map

label town_map:
    hide wayne
    hide dale
    hide kelsey
    call screen town_map_screen
    jump town_map

label wayne_house:
    scene bg wayne_house
    show wayne at fm_left

    "Home. The AC is trying its best, which is more than can be said for Wayne."

    menu:
        "Check phone":
            call screen phone_screen

        "Search for Kelsey's cooler" if "The Cooler Incident" in active_quests and not cooler_found:
            jump cooler_search

        "Take a nap (advance time)" if tod != "Night":
            $ advance_time()
            "Against all odds, this counts as planning."

        "Go to sleep for the night" if tod == "Night" and not tuesday_started:
            jump tuesday_morning

        "Call it a night" if tod == "Night" and tuesday_started:
            jump wednesday_teaser

        "Back to map":
            pass

    jump town_map

label dale_house:
    scene bg dale_house
    show wayne at fm_left
    show dale at fm_right

    if "Boat With No Name" in completed_quests:
        if day_number >= 2:
            if "A Simple Delivery" in completed_quests:
                d "You did good with the fish."
                w "That sentence should not make sense."
            elif "A Simple Delivery" in active_quests:
                if delivery_stage == 1:
                    d "Unit 14 at the storage lot. Bring ratchet straps."
                elif delivery_stage == 2:
                    d "Why are you here? Take the fish to the Sand Trap."
            else:
                d "Boat buyer's brother has another job."
                menu:
                    "Hear him out":
                        jump dale_tuesday
                    "Not yet":
                        d "Suit yourself. Opportunity has a short attention span."
        else:
            d "Airboat money spend okay?"
            w "Define okay."

        jump town_map

    if not quest_boat:
        $ advance_time()
        "An airboat is sitting halfway across Dale's lawn. It is not on a trailer."
        w "I'm going to regret asking this."
        d "Good news. I got an opportunity."
        d "Six hundred bucks. You get a hundred-fifty if we get her running."
        w "Whose boat is it?"
        d "That's kind of a philosophical question."

        $ quest_boat = True

        if "An Opportunity" in active_quests:
            $ active_quests.remove("An Opportunity")

        if "An Opportunity" not in completed_quests:
            $ completed_quests.append("An Opportunity")

        if "Boat With No Name" not in active_quests:
            $ active_quests.append("Boat With No Name")

        "QUEST STARTED: Boat With No Name"
        "Needs: fuel, battery, ignition key."
    else:
        "The airboat remains exactly where an airboat should not be."

        if battery_acquired and fuel_acquired and key_acquired:
            jump finish_boat

    jump town_map

label gator_mart:
    scene bg gator_mart
    show wayne at fm_left

    "The Gator Mart smells like coffee, bait, and an electrical fire nobody has investigated."

    if cooler_found and "The Cooler Incident" in active_quests:
        menu:
            "Text Kelsey and return her cooler":
                jump return_cooler
            "Keep it for now":
                pass

    if quest_boat and "Boat With No Name" in active_quests and not fuel_acquired:
        menu:
            "Buy fuel can and gas — $12":
                if money >= 12:
                    $ money -= 12
                    $ fuel_acquired = True

                    if "Fuel Can" not in inventory:
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
    scene bg beach
    show wayne at fm_left

    "White sand, Gulf water, and at least one person who brought a Bluetooth speaker nobody asked for."

    if quest_boat and "Boat With No Name" in active_quests and not key_acquired:
        "Something metallic is half buried near an abandoned cooler."

        menu:
            "Pick it up":
                $ key_acquired = True

                if "Unlabeled Boat Key" not in inventory:
                    $ inventory.append("Unlabeled Boat Key")

                $ dumb_luck += 1
                "An unlabeled ignition key. Surely this is fine."

            "Leave it alone":
                $ street_smarts += 1

    jump town_map

label sand_trap:
    scene bg sand_trap
    show wayne at fm_left

    if "A Simple Delivery" in active_quests and delivery_stage == 2:
        jump delivery_dropoff

    if tod == "Evening" and not evening_intro_seen and "Boat With No Name" in completed_quests:
        jump sand_trap_evening

    "The Sand Trap: cold beer, questionable karaoke, and several people who owe Dale money."

    if quest_boat and "Boat With No Name" in active_quests and not battery_acquired:
        show dale at fm_right
        d "Peanut says there's a spare marine battery behind the shed."

        menu:
            "Buy a used battery from Peanut — $10":
                if money >= 10:
                    $ money -= 10
                    $ battery_acquired = True

                    if "Marine Battery" not in inventory:
                        $ inventory.append("Marine Battery")

                    "Peanut accepts ten dollars and absolutely no questions."
                else:
                    "Peanut looks at your wallet and laughs."

            "Offer to do Peanut a favor instead":
                $ grit += 1
                $ battery_acquired = True

                if "Marine Battery" not in inventory:
                    $ inventory.append("Marine Battery")

                "PEANUT: Fine. But now you owe me."

            "Walk away":
                pass

    jump town_map

label finish_boat:
    scene bg dale_house
    show wayne at fm_left
    show dale at fm_right

    "Wayne installs the battery, pours in the fuel, and tries the mystery key."
    "The airboat coughs twice, launches a cloud of smoke, and starts."
    d "See? Basically legal."

    $ money += 150
    $ quest_boat = False

    if "Boat With No Name" in active_quests:
        $ active_quests.remove("Boat With No Name")

    if "Boat With No Name" not in completed_quests:
        $ completed_quests.append("Boat With No Name")

    $ dale_relationship += 1

    "QUEST COMPLETE: Boat With No Name"
    "Wayne earned $150."
    $ tod = "Evening"
    "Monday evening is now open."
    jump town_map

label sand_trap_evening:
    scene bg sand_trap
    show wayne at fm_left
    show dale at fm_right

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

    if "The Cooler Incident" not in active_quests and "The Cooler Incident" not in completed_quests:
        $ active_quests.append("The Cooler Incident")

    "NEW QUEST: The Cooler Incident"
    $ tod = "Night"
    "By the time Wayne leaves the Sand Trap, it's officially night."
    jump town_map

label cooler_search:
    scene bg wayne_house
    show wayne at fm_left

    "Behind Wayne's house sits a pile of things that were going to be dealt with tomorrow."

    if not cooler_found:
        $ cooler_found = True

        if "Kelsey's Cooler" not in inventory:
            $ inventory.append("Kelsey's Cooler")

        "Under a beach chair and one traffic cone: Kelsey's cooler."
        w "That was easier than expected."
        "The cooler is locked."

    jump town_map

label return_cooler:
    scene bg gator_mart
    show wayne at fm_left
    show kelsey at fm_right

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

    if "Kelsey's Cooler" in inventory:
        $ inventory.remove("Kelsey's Cooler")

    if "The Cooler Incident" in active_quests:
        $ active_quests.remove("The Cooler Incident")

    if "The Cooler Incident" not in completed_quests:
        $ completed_quests.append("The Cooler Incident")

    "QUEST COMPLETE: The Cooler Incident"
    jump town_map

label tuesday_morning:
    scene bg wayne_house
    show wayne at fm_center

    $ day = "Tuesday"
    $ day_number = 2
    $ tod = "Morning"
    $ tuesday_started = True

    centered "7:12 AM — TUESDAY"
    "Wayne's phone vibrates across the floor."
    "LANDLORD: Morning. Rent. Today."
    $ landlord_warning += 1

    if "The Cooler Incident" in active_quests:
        "KELSEY: Cooler. Today. Seriously."

    "DALE: Boat buyer has another job. Might pay better."

    menu:
        "Deal with the landlord first.":
            $ street_smarts += 1
            jump landlord_scene

        "Ask Dale about the new job.":
            $ dumb_luck += 1
            jump dale_tuesday

        "Pretend the phone is dead.":
            $ grit += 1
            "It continues vibrating. Technology has betrayed you."
            jump town_map

label landlord_scene:
    scene bg wayne_house
    show wayne at fm_left

    "Frank the landlord arrives on a golf cart with a clipboard."
    "FRANK: You owe [rent_due]."

    menu:
        "Pay $100 toward rent":
            if money >= 100:
                $ money -= 100
                $ rent_due = max(0, rent_due - 100)
                $ reputation += 1
                "FRANK: It's a start. Friday."
            else:
                "Wayne checks his wallet. Ambitious."

        "Ask for until Friday":
            $ charm += 1
            "FRANK: Friday. Then we have a different conversation."

        "Offer to work some of it off":
            $ grit += 1
            "FRANK: Laundry room door won't close. Fix it and I'll knock a hundred off."

            if "Laundry Room Rescue" not in active_quests and "Laundry Room Rescue" not in completed_quests:
                $ active_quests.append("Laundry Room Rescue")

            "NEW QUEST: Laundry Room Rescue"

    jump town_map

label dale_tuesday:
    scene bg gator_mart
    show wayne at fm_left
    show dale at fm_right

    "Dale is waiting beside Gator Mart with a breakfast burrito and no visible plan."
    d "Remember the guy who bought the airboat?"
    w "Unfortunately."
    d "His brother needs something moved."
    w "What?"
    d "He was real specific about not telling me over the phone."

    menu:
        "Ask what it pays.":
            $ street_smarts += 1
            d "Hundred twenty-five for you."

        "Ask whether it bites.":
            $ dumb_luck += 1
            d "Not anymore, probably."

        "Tell Dale this already sounds stupid.":
            $ dale_relationship += 1
            d "That's how you know it's ours."

    if "A Simple Delivery" not in active_quests and "A Simple Delivery" not in completed_quests:
        $ active_quests.append("A Simple Delivery")

    $ delivery_stage = 1

    if tod == "Morning":
        $ advance_time()

    "NEW QUEST: A Simple Delivery"
    "Meet Dale at Storage Unit 14."
    jump town_map

label laundry_room_rescue:
    scene bg laundry
    show wayne at fm_left

    "The apartment laundry room smells like dryer sheets, humidity, and somebody else's bad decisions."
    "The exterior door is swollen and scraping the frame."

    menu:
        "Adjust the hinges and latch carefully":
            $ street_smarts += 1
            $ reputation += 1
            $ rent_due = max(0, rent_due - 100)
            "Wayne resets the hinges, adjusts the latch, and gets an even reveal."
            "FRANK: Good. Hundred bucks off."

        "Plane the sticking edge and seal the exposed wood":
            $ grit += 1
            $ street_smarts += 1
            $ reputation += 1
            $ rent_due = max(0, rent_due - 100)
            "The door swings clean and the exposed edge gets sealed."
            "FRANK: That's better than it was before. Hundred off."

        "Hit it until it closes":
            $ grit += 1
            $ dumb_luck += 1
            $ reputation -= 1
            $ rent_due = max(0, rent_due - 50)
            "It closes. It may never open again."
            "FRANK: Fifty bucks off. Don't touch anything else."

    if "Laundry Room Rescue" in active_quests:
        $ active_quests.remove("Laundry Room Rescue")

    if "Laundry Room Rescue" not in completed_quests:
        $ completed_quests.append("Laundry Room Rescue")

    $ advance_time()
    "QUEST COMPLETE: Laundry Room Rescue"
    jump town_map

label storage_lot:
    scene bg storage_lot
    show wayne at fm_left
    show dale at fm_right

    "Storage Unit 14 rolls open with the sound of a dumpster losing an argument."
    "Inside is a nine-foot fiberglass swordfish from a closed seafood restaurant."
    w "No."
    d "Counterpoint: one hundred twenty-five dollars."

    menu:
        "Use proper ratchet straps and padding":
            $ street_smarts += 1
            $ delivery_strapped = True
            "Wayne pads the fins, crosses the straps, and checks the load twice."

        "Use every bungee cord Dale owns":
            $ dumb_luck += 1
            $ delivery_strapped = False
            "The fish is technically attached to the truck."
            w "This is not the same thing as secured."

        "Make Dale do the sketchy part":
            $ charm += 1
            $ dale_relationship += 1
            $ delivery_strapped = True
            d "Fine. But if this fish scratches me, I'm billing you."

    if "Fiberglass Swordfish" not in inventory:
        $ inventory.append("Fiberglass Swordfish")

    $ delivery_stage = 2
    $ advance_time()

    "The destination is the Sand Trap."
    jump town_map

label delivery_dropoff:
    scene bg sand_trap
    show wayne at fm_left
    show dale at fm_right

    "Peanut is standing behind the Sand Trap when Wayne and Dale arrive."
    "PEANUT: Put it over the outdoor bar."
    w "Of course that's where it goes."

    if not delivery_strapped:
        "A bungee cord snaps loose as they unload the swordfish."

        menu:
            "Catch the fish before it hits the truck":
                $ grit += 1
                $ delivery_damage = False
                "Wayne catches the tail. His shoulder files a complaint."

            "Get out of the way":
                $ dumb_luck += 1
                $ delivery_damage = True
                "The swordfish hits the gravel and loses a little paint off one fin."

    menu:
        "Help Peanut position it correctly":
            $ grit += 1
            $ reputation += 1
            "Ten minutes later the swordfish is level enough to look intentional."

        "Tell Dale 'your opportunity, your fish'":
            $ charm += 1
            $ dale_relationship -= 1
            "Dale mutters something unprintable and takes the heavy end."

    if delivery_damage:
        $ money += 100
        "PEANUT: Hundred. I'm keeping twenty-five for the paint."
        "Wayne earned $100."
    else:
        $ money += 125
        "PEANUT: Hundred twenty-five. Pleasure doing questionable business."
        "Wayne earned $125."

    if "Fiberglass Swordfish" in inventory:
        $ inventory.remove("Fiberglass Swordfish")

    if "A Simple Delivery" in active_quests:
        $ active_quests.remove("A Simple Delivery")

    if "A Simple Delivery" not in completed_quests:
        $ completed_quests.append("A Simple Delivery")

    $ delivery_stage = 3
    $ dale_relationship += 1
    $ advance_time()

    "QUEST COMPLETE: A Simple Delivery"
    "Tuesday is getting late."
    jump town_map

label wednesday_teaser:
    scene bg wednesday
    show wayne at fm_left

    $ day = "Wednesday"
    $ day_number = 3
    $ tod = "Morning"

    centered "8:03 AM — WEDNESDAY"
    "Three missed calls from Dale."
    "One voicemail from Frank."
    "A photo from Kelsey with no explanation."
    "And something large is parked in Wayne's yard."

    w "Absolutely not."

    centered "END OF v0.3.2 TEST BUILD"
    return
