define config.name = _("Florida Man: Unsupervised")
define config.version = "0.3.0"
define build.name = "FloridaManUnsupervised"
define build.version = "0.3.0"
define build.directory_name = "FloridaManUnsupervised-0.3.0"
define build.executable_name = "FloridaManUnsupervised"

init python:
    build.classify("**~", None)
    build.classify("**.bak", None)
    build.classify("**/.**", None)
