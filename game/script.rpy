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
default inventory = []
default quest_opportunity = False

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
    "QUEST STARTED: An Opportunity"
    jump town_map

label town_map:
    "Prototype map unlocked: Wayne's House, Dale's Place, Gator Mart, Beach, The Sand Trap."
    menu:
        "Go to Dale's Place":
            jump dale_house
        "Stay home":
            w "Probably the smartest thing I'll do all day."
            jump town_map

label dale_house:
    $ tod = "Afternoon"
    "An airboat is sitting halfway across Dale's lawn. It is not on a trailer."
    w "I'm going to regret asking this."
    d "Good news. I got an opportunity."
    "QUEST STARTED: Boat With No Name"
    "The airboat needs fuel, a battery, and an ignition key."
    return
