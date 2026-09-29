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
                        action FileSave(slot) if mode == "save" else FileLoad(slot)

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
