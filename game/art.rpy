image bg wayne_house:
    "images/bg_wayne_house.webp"
    xysize (1920, 1080)

image bg dale_house:
    "images/bg_dale_house.webp"
    xysize (1920, 1080)

image bg gator_mart:
    "images/bg_gator_mart.webp"
    xysize (1920, 1080)

image bg beach:
    "images/bg_beach.webp"
    xysize (1920, 1080)

image bg sand_trap:
    "images/bg_sand_trap.webp"
    xysize (1920, 1080)

image bg storage_lot:
    "images/bg_storage_lot.webp"
    xysize (1920, 1080)

image bg laundry:
    "images/bg_laundry.webp"
    xysize (1920, 1080)

image bg wednesday:
    "images/bg_wednesday.webp"
    xysize (1920, 1080)

image wayne = "images/wayne.webp"
image dale = "images/dale.webp"
image kelsey = "images/kelsey.webp"
image frank = "images/frank.webp"
image peanut = "images/peanut.webp"

define fm_scene = Dissolve(0.25)

transform fm_left:
    xalign 0.17
    yalign 1.0
    alpha 0.0
    xoffset -70
    easeout 0.22 alpha 1.0 xoffset 0

transform fm_right:
    xalign 0.83
    yalign 1.0
    alpha 0.0
    xoffset 70
    easeout 0.22 alpha 1.0 xoffset 0

transform fm_center:
    xalign 0.5
    yalign 1.0
    alpha 0.0
    yoffset 24
    easeout 0.22 alpha 1.0 yoffset 0

transform fm_menu_wayne:
    xalign 0.64
    yalign 1.04
    zoom 1.03
    alpha 0.0
    xoffset 45
    pause 0.08
    easeout 0.45 alpha 1.0 xoffset 0

transform fm_menu_kelsey:
    xalign 0.88
    yalign 1.03
    zoom 1.0
    alpha 0.0
    xoffset 70
    pause 0.16
    easeout 0.48 alpha 1.0 xoffset 0
