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
    padding (30, 16)

style fm_choice_button_text is button_text:
    size 34
    color "#f6f1e7"
    hover_color "#ffd166"
    text_align 0.5
    xalign 0.5

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
