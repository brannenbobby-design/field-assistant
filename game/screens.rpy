style fm_button is button:
    background Solid("#17444feF")
    hover_background Solid("#28717a")
    padding (18, 14)

style fm_button_text is button_text:
    size 28
    bold True

style fm_card is frame:
    background Solid("#082933eF")
    padding (20, 18)

screen main_menu():
    tag menu
    add "images/storm_porch.webp" xysize (1080, 1920)
    add Solid("#03141c55")
    frame:
        xalign 0.5
        yalign 0.86
        xsize 930
        background Solid("#08242dec")
        padding (36, 34)
        vbox:
            spacing 18
            text "FLORIDA MAN" size 62 bold True color "#ffb347" xalign 0.5
            text "STORM CLEANUP" size 44 bold True color "#f8f2e9" xalign 0.5
            text "Merge the mess. Fix the coast. Try not to promote the gator." size 26 xalign 0.5 text_align 0.5
            null height 12
            textbutton "START CLEANUP" action Start() xfill True style "fm_button" text_style "fm_button_text"
            textbutton "SETTINGS" action ShowMenu("preferences") xfill True style "fm_button" text_style "fm_button_text"
    text "v[config.version]  •  FIRST PLAYABLE" size 20 color "#ffffffcc" xalign 0.5 yalign 0.97

screen merge_game():
    tag game
    modal True
    add "images/storm_porch.webp" xysize (1080, 1920)
    add Solid("#04141c45")

    frame:
        xpos 24
        ypos 22
        xsize 1032
        ysize 96
        background Solid("#08242deb")
        padding (22, 14)
        hbox:
            xfill True
            yalign 0.5
            text "STORM CLEANUP" size 32 bold True color "#ffbd59" yalign 0.5
            null width 22
            text "COINS  [coins]" size 25 bold True yalign 0.5
            null width 24
            text "MOVES  [moves]" size 25 yalign 0.5
            null width 12
            textbutton "Ⅱ" action ShowMenu("pause_menu") style "fm_button" text_style "fm_button_text" xsize 80 ysize 58

    if not game_finished:
        $ job = FM_JOBS[job_index]
        frame:
            xpos 26
            ypos 365
            xsize 1028
            ysize 158
            background Solid("#08242deb")
            padding (22, 18)
            vbox:
                spacing 8
                hbox:
                    text "JOB [job_index + 1]/3  " size 23 bold True color "#ffd166"
                    text job["name"] size 30 bold True
                text job["story"] size 22 color "#e5eff0" xmaximum 970
                hbox:
                    spacing 14
                    for item, count in job["needs"]:
                        $ need = FM_META[item]
                        text "[need[0]] [need[1]] ×[count]" size 21 color "#ffdf9a"
                    null width 10
                    textbutton "DELIVER" action Function(fm_turn_in) style "fm_button" text_style "fm_button_text" xsize 190 ysize 58

        frame:
            xpos 18
            ypos 542
            xsize 1044
            ysize 1030
            background Solid("#08242dcf")
            padding (12, 12)
            draggroup:
                for i in range(20):
                    drag:
                        drag_name "[i]"
                        draggable board[i] is not None
                        droppable True
                        dragged fm_drag_drop
                        xpos (30 + (i % 5) * 203)
                        ypos (554 + (i // 5) * 251)
                        xysize (195, 243)
                        child Frame(
                            Text((FM_META[board[i]][0] + "\n" + FM_META[board[i]][1]) if board[i] else "+",
                                size=34 if board[i] else 48,
                                color="#203a40" if board[i] else "#8faeb0",
                                bold=board[i] is not None,
                                text_align=0.5,
                                xmaximum=175,
                                xalign=0.5,
                                yalign=0.5),
                            background=Solid("#ffe79eee" if i == selected_tile else "#f4e6c9ee" if board[i] else "#173e48cc"),
                            padding=(8, 10),
                            xysize=(195, 243))

        frame:
            xpos 24
            ypos 1590
            xsize 1032
            ysize 270
            background Solid("#08242deb")
            padding (18, 16)
            vbox:
                spacing 12
                text "[toast]" size 23 color "#fff0c7" xalign 0.5 text_align 0.5 xmaximum 970
                hbox:
                    spacing 14
                    textbutton "TAP FOR SALVAGE" action Function(fm_spawn) xsize 545 ysize 92 style "fm_button" text_style "fm_button_text"
                    textbutton "CLEAR SELECTED" action Function(fm_clear_selected) xsize 420 ysize 92 style "fm_button" text_style "fm_button_text"
                text "Drag a piece onto an identical one to merge. Drag it to an empty space to move it." size 18 color "#b8d3d3" xalign 0.5 text_align 0.5
    else:
        frame:
            xalign 0.5
            yalign 0.69
            xsize 990
            background Solid("#08242df2")
            padding (34, 30)
            vbox:
                spacing 18
                text "THE COAST IS (MOSTLY) BACK" size 42 bold True color "#ffbd59" xalign 0.5 text_align 0.5
                text "You rebuilt the porch, reopened the bait shop, and gave the gator a command center. It has already requested a bigger budget." size 27 xalign 0.5 text_align 0.5
                text "FINAL SCORE  •  [coins] COINS  •  [moves] MERGES" size 26 color "#bce4dc" xalign 0.5
                textbutton "PLAY AGAIN" action Function(fm_reset_game) xfill True style "fm_button" text_style "fm_button_text"
                textbutton "MAIN MENU" action MainMenu(confirm=False) xfill True style "fm_button" text_style "fm_button_text"

screen pause_menu():
    tag menu
    add "images/storm_porch.webp" xysize (1080, 1920)
    add Solid("#02131bea")
    frame:
        xalign 0.5
        yalign 0.5
        xsize 850
        background Solid("#08242df5")
        padding (32, 30)
        vbox:
            spacing 18
            text "PAUSED" size 48 bold True color "#ffbd59" xalign 0.5
            textbutton "RESUME" action Return() xfill True style "fm_button" text_style "fm_button_text"
            textbutton "SAVE GAME" action ShowMenu("save") xfill True style "fm_button" text_style "fm_button_text"
            textbutton "LOAD GAME" action ShowMenu("load") xfill True style "fm_button" text_style "fm_button_text"
            textbutton "SETTINGS" action ShowMenu("preferences") xfill True style "fm_button" text_style "fm_button_text"
            textbutton "MAIN MENU" action MainMenu(confirm=False) xfill True style "fm_button" text_style "fm_button_text"
