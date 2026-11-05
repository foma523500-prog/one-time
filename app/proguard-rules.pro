# 默认不开启混淆；若将来开启，保留 kotlinx.serialization 生成的序列化器
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.onetime.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.onetime.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
