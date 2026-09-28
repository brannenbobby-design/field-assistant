init python:
    gui.init(1920, 1080)

define gui.text_size = 38
define gui.name_text_size = 46
define gui.interface_text_size = 34
define gui.button_text_size = 36
define gui.choice_button_text_size = 36
define gui.dialogue_xpos = 220
define gui.dialogue_width = 1480
define gui.dialogue_ypos = 790
define gui.dialogue_height = 240

style default:
    color "#f2eadb"
    outlines [(2, "#00000099", 0, 0)]

style button:
    background Solid("#1d2528dd")
    hover_background Solid("#37474ddd")
    padding (28, 18)
    xminimum 360

style button_text:
    color "#f2eadb"
    hover_color "#ffd166"
    text_align 0.5

style choice_button:
    background Solid("#172126ee")
    hover_background Solid("#31454dee")
    padding (34, 22)
    xminimum 820

style choice_button_text:
    color "#ffffff"
    hover_color "#ffd166"

style fm_panel is frame:
    background Solid("#10181de8")
    padding (30, 24)

style fm_title is text:
    size 46
    color "#ffd166"
    outlines [(3, "#000000cc", 0, 0)]
