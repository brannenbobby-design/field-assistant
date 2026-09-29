style fm_dialogue_window is window:
    xalign 0.5
    yalign 1.0
    xsize 1920
    ysize 250
    background Solid("#071116ee")
    padding (74, 28)

style fm_dialogue_name is text:
    size 34
    color "#ffd166"
    bold True
    outlines [(2, "#000000bb", 0, 0)]

style fm_dialogue_text is text:
    size 38
    color "#f6f1e7"
    xmaximum 1760
    outlines [(2, "#000000bb", 0, 0)]

style fm_choice_button is button:
    xsize 1120
    yminimum 76
    background Solid("#132229ee")
    hover_background Solid("#2d424bee")
    selected_background Solid("#3b4f56ee")
    padding (30, 16)

style fm_choice_button_text is button_text:
    size 34
    color "#f6f1e7"
    hover_color "#ffd166"
    selected_color "#ffd166"
    text_align 0.5
    xalign 0.5

style fm_menu_panel is frame:
    background Solid("#061015e8")
    padding (40, 36)

style fm_menu_button is button:
    xsize 480
    yminimum 76
    background Solid("#132229ee")
    hover_background Solid("#35505bee")
    selected_background Solid("#35505bee")
    padding (28, 16)

style fm_menu_button_text is button_text:
    size 34
    color "#f6f1e7"
    hover_color "#ffd166"
    selected_color "#ffd166"
    text_align 0.5
    xalign 0.5

style fm_menu_title is text:
    size 78
    bold True
    color "#ff9f1c"
    outlines [(4, "#000000dd", 0, 0)]

style fm_menu_subtitle is text:
    size 38
    bold True
    italic True
    color "#f4f2ed"
    outlines [(3, "#000000dd", 0, 0)]

style fm_section_title is text:
    size 48
    bold True
    color "#ffd166"
    outlines [(3, "#000000cc", 0, 0)]

style fm_save_slot is button:
    xsize 470
    ysize 255
    background Solid("#0d1c22ee")
    hover_background Solid("#28404aee")
    padding (18, 16)

style fm_save_slot_text is text:
    size 23
    color "#f2eadb"
    xalign 0.5
    text_align 0.5

transform fm_menu_fade:
    alpha 0.0
    ease 0.35 alpha 1.0

transform fm_panel_slide:
    alpha 0.0
    xoffset -40
    easeout 0.28 alpha 1.0 xoffset 0

transform fm_choice_pop:
    alpha 0.0
    yoffset 18
    easeout 0.16 alpha 1.0 yoffset 0

screen say(who, what):
    window:
        id "window"
        style "fm_dialogue_window"

        if who:
            text who:
                id "who"
                style "fm_dialogue_name"
                xpos 0
                ypos 0

            text what:
                id "what"
                style "fm_dialogue_text"
                xpos 0
                ypos 58
        else:
            text what:
                id "what"
                style "fm_dialogue_text"
                xpos 0
                ypos 26

screen choice(items):
    modal True

    frame:
        at fm_choice_pop
        xalign 0.5
        yalign 0.54
        background Solid("#071116d8")
        padding (26, 26)

        vbox:
            spacing 14

            for i in items:
                textbutton i.caption:
                    action i.action
                    style "fm_choice_button"
                    text_style "fm_choice_button_text"

screen main_menu():
    tag menu

    add "images/bg_beach.webp" xysize (1920, 1080)
    add Solid("#02080c66")

    add "images/wayne.webp" at fm_menu_wayne
    add "images/kelsey.webp" at fm_menu_kelsey

    frame:
        at fm_panel_slide
        style "fm_menu_panel"
        xalign 0.045
        yalign 0.5
        xsize 620

        vbox:
            spacing 16

            text "FLORIDA MAN" style "fm_menu_title"
            text "UNSUPERVISED" style "fm_menu_subtitle"

            text "BAD DECISIONS. GOOD STORIES." size 24 color "#9fd8df"
            null height 18

            textbutton "NEW GAME":
                action Start()
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "LOAD GAME":
                action ShowMenu("load")
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "SETTINGS":
                action ShowMenu("preferences")
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "QUIT":
                action Quit(confirm=False)
                style "fm_menu_button"
                text_style "fm_menu_button_text"

    text "v[config.version]" size 22 color "#c9d5d9" xalign 0.985 yalign 0.975

screen pause_menu():
    tag menu

    add "images/bg_beach.webp" xysize (1920, 1080)
    add Solid("#02080ccc")

    frame:
        at fm_menu_fade
        style "fm_menu_panel"
        xalign 0.5
        yalign 0.5
        xsize 620

        vbox:
            spacing 14

            text "PAUSED" style "fm_section_title" xalign 0.5

            textbutton "RESUME":
                action Return()
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "SAVE GAME":
                action ShowMenu("save")
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "LOAD GAME":
                action ShowMenu("load")
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "SETTINGS":
                action ShowMenu("preferences")
                style "fm_menu_button"
                text_style "fm_menu_button_text"

            textbutton "MAIN MENU":
                action MainMenu(confirm=False)
                style "fm_menu_button"
                text_style "fm_menu_button_text"

screen game_menu(title, scroll=None, yinitial=0.0):
    tag menu
    use pause_menu

screen save():
    tag menu
    use fm_file_slots("SAVE GAME", "save")

screen load():
    tag menu
    use fm_file_slots("LOAD GAME", "load")

screen fm_file_slots(title, mode):
    add "images/bg_beach.webp" xysize (1920, 1080)
    add Solid("#02080ccc")

    frame:
        at fm_menu_fade
        style "fm_menu_panel"
        xalign 0.5
        yalign 0.5
        xsize 1180

        vbox:
            spacing 20

            text title style "fm_section_title" xalign 0.5

            grid 2 3:
                spacing 18

                for slot in range(1, 7):
                    button:
                        style "fm_save_slot"

                        if mode == "save":
                            action FileSave(slot)
                        else:
                            action FileLoad(slot)

                        vbox:
                            spacing 8
                            xalign 0.5

                            add FileScreenshot(slot):
                                xysize (300, 169)
                                xalign 0.5

                            text "SLOT [slot]" style "fm_save_slot_text"
                            text FileTime(slot, format="%b %d, %Y  %I:%M %p", empty="EMPTY"):
                                style "fm_save_slot_text"

            hbox:
                xalign 0.5
                spacing 18

                textbutton "BACK":
                    action Return()
                    style "fm_menu_button"
                    text_style "fm_menu_button_text"

screen preferences():
    tag menu

    add "images/bg_beach.webp" xysize (1920, 1080)
    add Solid("#02080ccc")

    frame:
        at fm_menu_fade
        style "fm_menu_panel"
        xalign 0.5
        yalign 0.5
        xsize 980

        vbox:
            spacing 18

            text "SETTINGS" style "fm_section_title" xalign 0.5

            text "TEXT SPEED" size 27 color "#ffd166"
            bar value Preference("text speed") xsize 820

            text "AUTO-FORWARD SPEED" size 27 color "#ffd166"
            bar value Preference("auto-forward time") xsize 820

            text "SOUND VOLUME" size 27 color "#ffd166"
            bar value Preference("sound volume") xsize 820

            text "MUSIC VOLUME" size 27 color "#ffd166"
            bar value Preference("music volume") xsize 820

            hbox:
                spacing 16
                xalign 0.5

                textbutton "TRANSITIONS ON":
                    action Preference("transitions", "all")
                    style "fm_menu_button"
                    text_style "fm_menu_button_text"

                textbutton "TRANSITIONS OFF":
                    action Preference("transitions", "none")
                    style "fm_menu_button"
                    text_style "fm_menu_button_text"

            textbutton "BACK":
                action Return()
                style "fm_menu_button"
                text_style "fm_menu_button_text"
                xalign 0.5


style fm_toast_frame is frame:
    background Solid("#08151df2")
    padding (34, 18)

style fm_toast_text is text:
    size 30
    bold True
    color "#ffd166"
    outlines [(2, "#000000cc", 0, 0)]

transform fm_toast_anim:
    alpha 0.0
    yoffset -30
    easeout 0.22 alpha 1.0 yoffset 0
    pause 1.75
    easein 0.28 alpha 0.0 yoffset -18

screen fm_toast(message):
    zorder 100

    frame:
        at fm_toast_anim
        style "fm_toast_frame"
        xalign 0.5
        yalign 0.10

        text message style "fm_toast_text"

    timer 2.35 action Hide("fm_toast")


style fm_phone_shell is frame:
    background Solid("#020407")
    padding (14, 14)

style fm_phone_screen is frame:
    background Solid("#101820")
    padding (0, 0)

style fm_phone_header is frame:
    background Solid("#111d25")
    padding (22, 16)

style fm_phone_card is frame:
    background Solid("#18242d")
    padding (18, 14)

style fm_phone_card_alt is frame:
    background Solid("#132029")
    padding (18, 14)

style fm_phone_tab is button:
    xsize 132
    yminimum 66
    background Solid("#101820")
    hover_background Solid("#1f3440")
    selected_background Solid("#244a5a")
    padding (10, 10)

style fm_phone_tab_text is button_text:
    size 20
    color "#9fb1ba"
    hover_color "#ffffff"
    selected_color "#72d4ff"
    text_align 0.5
    xalign 0.5

style fm_phone_close is button:
    xsize 150
    yminimum 56
    background Solid("#22313a")
    hover_background Solid("#354c58")
    padding (12, 8)

style fm_phone_close_text is button_text:
    size 20
    color "#f6f1e7"
    hover_color "#ffd166"
    text_align 0.5
    xalign 0.5

transform fm_phone_pop:
    alpha 0.0
    zoom 0.97
    easeout 0.18 alpha 1.0 zoom 1.0

screen fm_phone_message(sender, body, accent="#72d4ff"):
    frame:
        style "fm_phone_card"
        xfill True

        vbox:
            spacing 5

            text sender:
                size 20
                bold True
                color accent

            text body:
                size 24
                color "#f1f5f6"
                xmaximum 515

screen fm_phone_job(title, objective, accent="#ffd166"):
    frame:
        style "fm_phone_card_alt"
        xfill True

        vbox:
            spacing 7

            text title:
                size 23
                bold True
                color accent

            text objective:
                size 21
                color "#c8d2d7"
                xmaximum 515

screen phone_screen(called=False):
    modal True
    zorder 90
    default tab = "messages"

    add Solid("#02060acc")

    if called:
        key "game_menu" action Return()
    else:
        key "game_menu" action Hide("phone_screen")

    frame:
        at fm_phone_pop
        style "fm_phone_shell"
        xalign 0.5
        yalign 0.5
        xsize 650
        ysize 1030

        fixed:
            frame:
                style "fm_phone_screen"
                xfill True
                yfill True

            # Earpiece / camera island.
            frame:
                xalign 0.5
                ypos 18
                xsize 148
                ysize 28
                background Solid("#000000")
                padding (0, 0)

            # Status bar.
            text "[day] • [tod]":
                xpos 28
                ypos 24
                size 19
                color "#dce6ea"

            text "LTE    100%":
                xalign 0.95
                ypos 24
                size 19
                color "#dce6ea"

            # App header.
            frame:
                style "fm_phone_header"
                xpos 14
                ypos 62
                xsize 594
                ysize 92

                hbox:
                    xfill True
                    yalign 0.5

                    vbox:
                        xsize 390
                        text "WAYNE'S PHONE":
                            size 28
                            bold True
                            color "#ffffff"
                        text "$[money:.2f]  •  Gas [gas]%":
                            size 20
                            color "#8ecbd9"

                    if called:
                        textbutton "PUT AWAY":
                            action Return()
                            style "fm_phone_close"
                            text_style "fm_phone_close_text"
                    else:
                        textbutton "PUT AWAY":
                            action Hide("phone_screen")
                            style "fm_phone_close"
                            text_style "fm_phone_close_text"

            # App content area.
            frame:
                xpos 28
                ypos 174
                xsize 566
                ysize 700
                background Solid("#0d151b")
                padding (14, 14)

                if tab == "messages":
                    viewport:
                        mousewheel True
                        draggable True
                        scrollbars "vertical"

                        vbox:
                            spacing 12
                            xfill True

                            text "MESSAGES":
                                size 25
                                bold True
                                color "#ffffff"

                            if rent_due > 0:
                                use fm_phone_message("LANDLORD", "Rent due: $[rent_due].", "#f6c67b")

                            if "The Cooler Incident" in active_quests:
                                use fm_phone_message("KELSEY", "Cooler. Today. Seriously.", "#ff9fc8")
                            elif "The Cooler Incident" in completed_quests:
                                use fm_phone_message("KELSEY", "I got the cooler. We're still discussing the power-tools comment.", "#ff9fc8")

                            if "Absolutely Not" in active_quests:
                                if turbine_stage == 0:
                                    use fm_phone_message("DALE", "Before you get mad, I can explain the turbine.", "#f2b15f")
                                    use fm_phone_message("LANDLORD", "Whatever is in your yard needs to be gone by five.", "#f6c67b")
                                elif turbine_stage == 1:
                                    use fm_phone_message("KELSEY", "I found somebody willing to pay for that ridiculous thing.", "#ff9fc8")
                            elif "A Simple Delivery" in active_quests:
                                if delivery_stage == 1:
                                    use fm_phone_message("DALE", "Storage lot. Unit 14. Bring straps.", "#f2b15f")
                                elif delivery_stage == 2:
                                    use fm_phone_message("DALE", "Take the fish to the Sand Trap. Don't ask.", "#f2b15f")
                            elif day_number >= 2 and "A Simple Delivery" not in completed_quests:
                                use fm_phone_message("DALE", "Boat buyer has another job. Might pay better.", "#f2b15f")
                            elif "Boat With No Name" in active_quests:
                                use fm_phone_message("DALE", "Boat still needs fuel, battery, key.", "#f2b15f")
                            else:
                                use fm_phone_message("DALE", "You awake?", "#f2b15f")

                elif tab == "jobs":
                    viewport:
                        mousewheel True
                        draggable True
                        scrollbars "vertical"

                        vbox:
                            spacing 12
                            xfill True

                            text "JOBS":
                                size 25
                                bold True
                                color "#ffffff"

                            if "Absolutely Not" in active_quests:
                                if turbine_stage == 0:
                                    use fm_phone_job("Absolutely Not", "Figure out what Dale had delivered.")
                                else:
                                    use fm_phone_job("Absolutely Not", "Decide what happens to the turbine core.")
                            elif "Boat With No Name" in active_quests:
                                use fm_phone_job("Boat With No Name", "Find fuel, battery, and an ignition key.")
                            elif "The Cooler Incident" in active_quests:
                                use fm_phone_job("The Cooler Incident", "Find and return Kelsey's locked cooler.")
                            elif "Laundry Room Rescue" in active_quests:
                                use fm_phone_job("Laundry Room Rescue", "Fix the apartment laundry-room door.")
                            elif "A Simple Delivery" in active_quests:
                                if delivery_stage == 1:
                                    use fm_phone_job("A Simple Delivery", "Meet Dale at Storage Unit 14.")
                                else:
                                    use fm_phone_job("A Simple Delivery", "Deliver the fiberglass swordfish to the Sand Trap.")
                            elif "An Opportunity" in active_quests:
                                use fm_phone_job("An Opportunity", "Meet Dale and find out what this 'opportunity' is.")
                            else:
                                text "No active jobs. Suspiciously peaceful.":
                                    size 23
                                    color "#aab7bd"

                            null height 10
                            text "COMPLETED: [len(completed_quests)]":
                                size 21
                                color "#7fa6b5"

                            for q in completed_quests:
                                text "✓ [q]":
                                    size 21
                                    color "#96c79d"

                elif tab == "bag":
                    viewport:
                        mousewheel True
                        draggable True
                        scrollbars "vertical"

                        vbox:
                            spacing 12
                            xfill True

                            text "INVENTORY":
                                size 25
                                bold True
                                color "#ffffff"

                            for item in inventory:
                                frame:
                                    style "fm_phone_card_alt"
                                    xfill True
                                    text item:
                                        size 23
                                        color "#eef3f5"

                else:
                    viewport:
                        mousewheel True
                        draggable True
                        scrollbars "vertical"

                        vbox:
                            spacing 12
                            xfill True

                            text "PROFILE":
                                size 25
                                bold True
                                color "#ffffff"

                            use fm_phone_job("Money", "$[money:.2f]", "#72d4ff")
                            use fm_phone_job("Gas", "[gas]%", "#72d4ff")

                            if rent_due > 0:
                                use fm_phone_job("Rent Due", "$[rent_due]", "#f6c67b")

                            use fm_phone_job("Charm", "[charm]", "#ff9fc8")
                            use fm_phone_job("Grit", "[grit]", "#e1a467")
                            use fm_phone_job("Street Smarts", "[street_smarts]", "#72d4ff")
                            use fm_phone_job("Dumb Luck", "[dumb_luck]", "#ffd166")
                            use fm_phone_job("Dale", "[dale_relationship]", "#f2b15f")
                            use fm_phone_job("Kelsey", "[kelsey_relationship]", "#ff9fc8")
                            use fm_phone_job("Reputation", "[reputation]", "#96c79d")

            # Bottom app dock.
            frame:
                xpos 20
                ypos 892
                xsize 610
                ysize 92
                background Solid("#0a1117")
                padding (10, 10)

                hbox:
                    spacing 8
                    xalign 0.5

                    textbutton "MSGS":
                        action SetScreenVariable("tab", "messages")
                        selected (tab == "messages")
                        style "fm_phone_tab"
                        text_style "fm_phone_tab_text"

                    textbutton "JOBS":
                        action SetScreenVariable("tab", "jobs")
                        selected (tab == "jobs")
                        style "fm_phone_tab"
                        text_style "fm_phone_tab_text"

                    textbutton "BAG":
                        action SetScreenVariable("tab", "bag")
                        selected (tab == "bag")
                        style "fm_phone_tab"
                        text_style "fm_phone_tab_text"

                    textbutton "STATS":
                        action SetScreenVariable("tab", "stats")
                        selected (tab == "stats")
                        style "fm_phone_tab"
                        text_style "fm_phone_tab_text"

            # Gesture bar.
            frame:
                xalign 0.5
                ypos 995
                xsize 150
                ysize 7
                background Solid("#d8e1e5")
                padding (0, 0)
