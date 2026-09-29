init python:
    gui.init(1920, 1080)

define gui.text_size = 38
define gui.name_text_size = 42
define gui.interface_text_size = 32
define gui.button_text_size = 32
define gui.choice_button_text_size = 34

style default:
    color "#f2eadb"
    outlines [(2, "#00000099", 0, 0)]

style button:
    background Solid("#142126e8")
    hover_background Solid("#31454dee")
    padding (26, 16)
    xminimum 340

style button_text:
    color "#f2eadb"
    hover_color "#ffd166"
    text_align 0.5

style fm_panel is frame:
    background Solid("#0a1419e8")
    padding (30, 24)

style fm_title is text:
    size 44
    color "#ffd166"
    outlines [(3, "#000000cc", 0, 0)]
