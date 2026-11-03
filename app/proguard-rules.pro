# Release minification is off (see app/build.gradle.kts). If you enable it, keep the Samsung
# Health Data SDK and the Gson-serialised types it uses.
-keep class com.samsung.android.sdk.health.data.** { *; }
-keep class com.anthropic.** { *; }
