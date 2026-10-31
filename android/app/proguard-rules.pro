# kotlinx.serialization keeps its own generated serializers; keep the model classes readable.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.nasmusic.data.**$$serializer { *; }
-keepclassmembers class com.nasmusic.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.nasmusic.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
