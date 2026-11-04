# Настройки хранятся через kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.akira.island.data.**$$serializer { *; }
-keepclassmembers class com.akira.island.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.akira.island.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gizli API köprüsü (gerçek bulanıklık)
-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**
