# EMV NFC library - keep reflection targets
-keep class com.github.devnied.emvnfccard.** { *; }
-keep class fr.devnied.bitlib.** { *; }

# Logback Android
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**
-dontwarn org.slf4j.**
