init python:
    gui.init(1080, 1920)

define gui.text_size = 32
define gui.name_text_size = 38
define gui.interface_text_size = 30
define gui.button_text_size = 30
define gui.choice_button_text_size = 30

style default:
    color "#fff6e8"
    outlines [(1, "#122027aa", 0, 1)]
    font "DejaVuSans.ttf"

style button:
    background Solid("#16313bea")
    hover_background Solid("#2e6267ee")
    padding (24, 18)

style button_text:
    color "#fff6e8"
    hover_color "#ffe08a"
    text_align 0.5

style fm_panel is frame:
    background Solid("#08242de8")
    padding (24, 22)

style fm_title is text:
    size 68
    bold True
    color "#ffb347"
    outlines [(3, "#14242acc", 0, 3)]
