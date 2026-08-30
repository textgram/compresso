-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.compresso.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.compresso.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
