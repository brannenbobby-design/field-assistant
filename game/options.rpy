# Android CI: Ren'Py lint and APK build run on pushes and pull requests.
define config.name = _("Florida Man: Unsupervised")
define config.version = "0.3.2"
define build.name = "FloridaManUnsupervised"
define build.version = "0.3.2"
define build.directory_name = "FloridaManUnsupervised-0.3.2"
define build.executable_name = "FloridaManUnsupervised"

init python:
    build.classify("**~", None)
    build.classify("**.bak", None)
    build.classify("**/.**", None)
