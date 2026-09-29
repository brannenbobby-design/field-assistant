define w = Character("Wayne", color="#72d4ff")
define d = Character("Dale", color="#f2b15f")
define k = Character("Kelsey", color="#ff9fc8")
define f = Character("Frank", color="#b9d4c5")
define p = Character("Peanut", color="#e1a467")

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

default wednesday_started = False
default turbine_stage = 0
default turbine_route = ""
default turbine_tag_read = False
default turbine_called_dale = False
default turbine_called_kelsey = False

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

                    if "Absolutely Not" in active_quests:
                        if turbine_stage == 0:
                            text "DALE: Before you get mad, I can explain the turbine." size 27
                            text "FRANK: Whatever is in your yard needs to be gone by five." size 27
                        elif turbine_stage == 1:
                            text "KELSEY: I found somebody willing to pay for that ridiculous thing." size 27
                    elif "A Simple Delivery" in active_quests:
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

            hbox:
                xalign 1.0
                spacing 14

                textbutton "PHONE":
                    action Show("phone_screen")
                    xsize 220
                    yminimum 64

                textbutton "MENU":
                    action ShowMenu("pause_menu")
                    xsize 220
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

            if "Absolutely Not" in active_quests:
                textbutton "Wayne's Yard — TURBINE" action Jump("wednesday_yard") xfill True

            if tod in ["Afternoon", "Evening", "Night"]:
                if "A Simple Delivery" in active_quests and delivery_stage == 2:
                    textbutton "The Sand Trap — MAKE DELIVERY" action Jump("sand_trap") xfill True
                else:
                    textbutton "The Sand Trap" action Jump("sand_trap") xfill True
            else:
                text "The Sand Trap — too early for respectable bad decisions." size 24 color "#b8b8b8"

    frame:
        style "fm_panel"
        xalign 0.95
        yalign 0.57
        xsize 500

        vbox:
            spacing 10
            text "ACTIVE JOB" size 30 color "#ffd166"

            if active_quests:
                text "[active_quests[0]]" size 28 color "#f6f1e7"

                if "Absolutely Not" in active_quests:
                    if turbine_stage == 0:
                        text "Figure out what Dale had delivered." size 23 color "#c9d5d9"
                    elif turbine_stage == 1:
                        text "Decide what happens to the turbine core." size 23 color "#c9d5d9"
                elif "Boat With No Name" in active_quests:
                    text "Find fuel, battery, and an ignition key." size 23 color "#c9d5d9"
                elif "A Simple Delivery" in active_quests:
                    text "Finish Dale's delivery." size 23 color "#c9d5d9"
            else:
                text "No active jobs." size 24 color "#c9d5d9"

    text "Tap a destination. Story scenes keep the HUD out of the way." size 22 color "#d7ddd9" xalign 0.97 yalign 0.96

label start:
    scene bg wayne_house with fm_scene
    show wayne at fm_center

    centered "6:47 AM — MONDAY"
    pause 0.2
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

    show screen fm_toast("QUEST STARTED: AN OPPORTUNITY")
    jump town_map

label town_map:
    hide wayne
    hide dale
    hide kelsey
    hide frank
    hide peanut
    call screen town_map_screen
    jump town_map

label wayne_house:
    scene bg wayne_house with fm_scene
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
    scene bg dale_house with fm_scene
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

        show screen fm_toast("QUEST STARTED: BOAT WITH NO NAME")
        "Needs: fuel, battery, ignition key."
    else:
        "The airboat remains exactly where an airboat should not be."

        if battery_acquired and fuel_acquired and key_acquired:
            jump finish_boat

    jump town_map

label gator_mart:
    scene bg gator_mart with fm_scene
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
    scene bg beach with fm_scene
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
    scene bg sand_trap with fm_scene
    show wayne at fm_left

    if "A Simple Delivery" in active_quests and delivery_stage == 2:
        jump delivery_dropoff

    if tod == "Evening" and not evening_intro_seen and "Boat With No Name" in completed_quests:
        jump sand_trap_evening

    "The Sand Trap: cold beer, questionable karaoke, and several people who owe Dale money."

    if quest_boat and "Boat With No Name" in active_quests and not battery_acquired:
        show dale at fm_center with dissolve
        show peanut at fm_right with dissolve
        d "Peanut says there's a spare marine battery behind the shed."
        p "Was spare. Now it's ten bucks."

        menu:
            "Buy a used battery from Peanut — $10":
                if money >= 10:
                    $ money -= 10
                    $ battery_acquired = True

                    if "Marine Battery" not in inventory:
                        $ inventory.append("Marine Battery")

                    p "Pleasure doing questionable business."
                else:
                    p "Come back when your wallet quits rattling like a loose fan blade."

            "Offer to do Peanut a favor instead":
                $ grit += 1
                $ battery_acquired = True

                if "Marine Battery" not in inventory:
                    $ inventory.append("Marine Battery")

                p "Fine. But now you owe me."

            "Walk away":
                pass

    jump town_map

label finish_boat:
    scene bg dale_house with fm_scene
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

    show screen fm_toast("QUEST COMPLETE: BOAT WITH NO NAME")
    "Wayne earned $150."
    $ tod = "Evening"
    "Monday evening is now open."
    jump town_map

label sand_trap_evening:
    scene bg sand_trap with fm_scene
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

    show screen fm_toast("NEW QUEST: THE COOLER INCIDENT")
    $ tod = "Night"
    "By the time Wayne leaves the Sand Trap, it's officially night."
    jump town_map

label cooler_search:
    scene bg wayne_house with fm_scene
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
    scene bg gator_mart with fm_scene
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

    show screen fm_toast("QUEST COMPLETE: THE COOLER INCIDENT")
    jump town_map

label tuesday_morning:
    scene bg wayne_house with fm_scene
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
    scene bg wayne_house with fm_scene
    show wayne at fm_left
    show frank at fm_right with dissolve

    "Frank the landlord arrives on a golf cart with a clipboard."
    f "You owe [rent_due]."

    menu:
        "Pay $100 toward rent":
            if money >= 100:
                $ money -= 100
                $ rent_due = max(0, rent_due - 100)
                $ reputation += 1
                f "It's a start. Friday."
            else:
                "Wayne checks his wallet. Ambitious."

        "Ask for until Friday":
            $ charm += 1
            f "Friday. Then we have a different conversation."

        "Offer to work some of it off":
            $ grit += 1
            f "Laundry room door won't close. Fix it and I'll knock a hundred off."

            if "Laundry Room Rescue" not in active_quests and "Laundry Room Rescue" not in completed_quests:
                $ active_quests.append("Laundry Room Rescue")

            show screen fm_toast("NEW QUEST: LAUNDRY ROOM RESCUE")

    jump town_map

label dale_tuesday:
    scene bg gator_mart with fm_scene
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

    show screen fm_toast("NEW QUEST: A SIMPLE DELIVERY")
    "Meet Dale at Storage Unit 14."
    jump town_map

label laundry_room_rescue:
    scene bg laundry with fm_scene
    show wayne at fm_left
    show frank at fm_right with dissolve

    "The apartment laundry room smells like dryer sheets, humidity, and somebody else's bad decisions."
    "The exterior door is swollen and scraping the frame."

    menu:
        "Adjust the hinges and latch carefully":
            $ street_smarts += 1
            $ reputation += 1
            $ rent_due = max(0, rent_due - 100)
            "Wayne resets the hinges, adjusts the latch, and gets an even reveal."
            f "Good. Hundred bucks off."

        "Plane the sticking edge and seal the exposed wood":
            $ grit += 1
            $ street_smarts += 1
            $ reputation += 1
            $ rent_due = max(0, rent_due - 100)
            "The door swings clean and the exposed edge gets sealed."
            f "That's better than it was before. Hundred off."

        "Hit it until it closes":
            $ grit += 1
            $ dumb_luck += 1
            $ reputation -= 1
            $ rent_due = max(0, rent_due - 50)
            "It closes. It may never open again."
            f "Fifty bucks off. Don't touch anything else."

    if "Laundry Room Rescue" in active_quests:
        $ active_quests.remove("Laundry Room Rescue")

    if "Laundry Room Rescue" not in completed_quests:
        $ completed_quests.append("Laundry Room Rescue")

    $ advance_time()
    show screen fm_toast("QUEST COMPLETE: LAUNDRY ROOM RESCUE")
    jump town_map

label storage_lot:
    scene bg storage_lot with fm_scene
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
    scene bg sand_trap with fm_scene
    show wayne at fm_left
    show dale at fm_center
    show peanut at fm_right with dissolve

    "Peanut is standing behind the Sand Trap when Wayne and Dale arrive."
    p "Put it over the outdoor bar."
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
        p "Hundred. I'm keeping twenty-five for the paint."
        "Wayne earned $100."
    else:
        $ money += 125
        p "Hundred twenty-five. Pleasure doing questionable business."
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

    show screen fm_toast("QUEST COMPLETE: A SIMPLE DELIVERY")
    "Tuesday is getting late."
    jump town_map

label wednesday_teaser:
    scene bg wednesday with fm_scene
    show wayne at fm_left

    $ day = "Wednesday"
    $ day_number = 3
    $ tod = "Morning"
    $ wednesday_started = True

    centered "8:03 AM — WEDNESDAY"
    "Three missed calls from Dale."
    "One voicemail from Frank."
    "A photo from Kelsey with no explanation."
    "And something large is parked in Wayne's yard."

    w "Absolutely not."

    "Wayne pulls back one corner of the tarp."
    "Under it is a decommissioned turbine core on a homemade steel cradle."

    "His phone buzzes immediately."
    d "Before you get mad, I can explain."
    f "Whatever is in your yard needs to be gone by five."
    k "Please tell me Dale did not buy a jet engine."

    if "Absolutely Not" not in active_quests:
        $ active_quests.append("Absolutely Not")

    show screen fm_toast("QUEST STARTED: ABSOLUTELY NOT")
    $ turbine_stage = 0

    menu:
        "Inspect the turbine and shipping tag.":
            $ street_smarts += 1
            jump wednesday_yard

        "Call Dale before touching anything.":
            $ turbine_called_dale = True
            $ dale_relationship += 1
            d "Technically I bought a turbine-shaped opportunity."
            jump wednesday_yard

        "Send Kelsey a photo and ask what she knows.":
            $ turbine_called_kelsey = True
            $ kelsey_relationship += 1
            k "Give me ten minutes. And don't let Dale start it."
            jump wednesday_yard

label wednesday_yard:
    scene bg wednesday with fm_scene
    show wayne at fm_left

    if turbine_stage == 0:
        "The metal data plate is sun-faded but readable."
        "INERT DISPLAY CORE. NO COMBUSTOR. NOT AIRWORTHY."
        $ turbine_tag_read = True
        $ turbine_stage = 1
        $ street_smarts += 1

        w "Good. So it can't launch the trailer into low orbit."

        "Dale pulls up like he has been waiting for that exact sentence."
        show dale at fm_right with dissolve
        d "See? Safe."
        w "That is not what that word means."

        "Kelsey texts a screenshot from a local listing."
        k "Peanut will pay three-fifty for it as bar decor."
        f "Four fifty-five. It is gone at five."

        jump turbine_decision

    elif turbine_stage == 1:
        jump turbine_decision
    else:
        jump wednesday_wrap

label turbine_decision:
    scene bg wednesday with fm_scene
    show wayne at fm_left
    show dale at fm_right

    "Three options. Somehow none of them are normal."

    menu:
        "Sell it to Peanut for $350.":
            $ turbine_route = "sell"
            $ charm += 1
            jump turbine_sale

        "Return it to the surplus yard and end this nonsense.":
            $ turbine_route = "return"
            $ street_smarts += 1
            jump turbine_return

        "Let Dale keep it for whatever terrible idea comes next.":
            $ turbine_route = "keep"
            $ dumb_luck += 1
            $ dale_relationship += 2
            jump turbine_keep

label turbine_sale:
    scene bg sand_trap with fm_scene
    show wayne at fm_left
    show peanut at fm_right

    p "That thing is going over the outdoor bar."
    w "Of course it is."

    $ money += 350
    $ reputation += 1
    $ turbine_stage = 3

    "Peanut counts out $350."
    show screen fm_toast("SOLD TURBINE CORE: +$350")

    menu:
        "Pay Frank $150 toward rent." if money >= 150 and rent_due > 0:
            $ money -= 150
            $ rent_due = max(0, rent_due - 150)
            $ reputation += 1
            f "Now we're having a much better Wednesday."

        "Keep the cash for now.":
            w "Future Wayne can negotiate with Frank."

    jump complete_turbine_quest

label turbine_return:
    scene bg storage_lot with fm_scene
    show wayne at fm_left
    show dale at fm_right

    "The surplus yard agrees to take the turbine back."
    "The catch: forty dollars for their forklift and paperwork."

    if money >= 40:
        $ money -= 40
        w "Forty bucks to erase one of Dale's ideas is probably market rate."
    else:
        d "I'll cover it."
        $ dale_relationship -= 1
        w "You absolutely will."

    $ rent_due = max(0, rent_due - 50)
    $ reputation += 2
    $ turbine_stage = 3

    f "It's gone. Fifty off what you owe me for not making this my problem."

    jump complete_turbine_quest

label turbine_keep:
    scene bg dale_house with fm_scene
    show wayne at fm_left
    show dale at fm_right

    "Two ratchet straps, one borrowed trailer, and several poor decisions later, the turbine is in Dale's yard."
    d "Tell me that doesn't look fast."
    w "It is currently sitting still."
    d "For now."

    $ money += 75
    $ reputation -= 1
    $ turbine_stage = 3

    if "Turbine Claim Ticket" not in inventory:
        $ inventory.append("Turbine Claim Ticket")

    "Dale pays Wayne $75 for helping move his newest problem."
    "Something about the way he says 'phase two' suggests this will matter later."

    jump complete_turbine_quest

label complete_turbine_quest:
    if "Absolutely Not" in active_quests:
        $ active_quests.remove("Absolutely Not")

    if "Absolutely Not" not in completed_quests:
        $ completed_quests.append("Absolutely Not")

    show screen fm_toast("QUEST COMPLETE: ABSOLUTELY NOT")
    $ tod = "Evening"

    jump wednesday_wrap

label wednesday_wrap:
    scene bg wednesday with fm_scene
    show wayne at fm_left

    "By sunset, Wayne's yard is almost normal again."

    if turbine_route == "sell":
        k "You made money off Dale's mistake. That's disturbingly efficient."
        w "I hate that this counts as a business model."
    elif turbine_route == "return":
        f "No turbines. No boats. No livestock."
        w "Those rules feel weirdly specific."
    elif turbine_route == "keep":
        d "Phase two tomorrow."
        w "There better not be a phase two."

    centered "END OF v0.3.5 TEST BUILD"
    return
