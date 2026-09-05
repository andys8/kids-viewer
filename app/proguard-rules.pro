# The app itself uses no reflection, and Compose, Media3 and Coil all ship their own consumer
# rules, so R8 needs almost nothing from here.

# Keep the crash reporter's stack traces readable. Without this, line numbers are stripped and
# the report the app shows after a crash is far less useful. The mapping file published with
# each release turns the remaining obfuscated names back into the originals.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
