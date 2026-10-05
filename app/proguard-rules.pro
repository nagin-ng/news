# Native bridge must keep its names so the .so can find it.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.example.agylauncher.NhCore {
    *;
}
