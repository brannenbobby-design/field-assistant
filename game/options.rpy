define config.name = _("Florida Man: Storm Cleanup")
define config.version = "1.0.0"
define build.name = "FloridaManStormCleanup"
define build.version = "1.0.0"
define build.directory_name = "FloridaManStormCleanup-1.0.0"
define build.executable_name = "FloridaManStormCleanup"
define config.screen_width = 1080
define config.screen_height = 1920
define config.default_fullscreen = True
define config.save_directory = "FloridaManStormCleanup-1"
define config.check_conflicting_properties = True

init python:
    build.classify("**~", None)
    build.classify("**.bak", None)
    build.classify("**/.**", None)
