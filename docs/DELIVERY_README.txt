THUNDER STACK - DELIVERY PACKAGE

Contents:
- ThunderStack.apk: installable Android debug build used for final device QA.
- source/: complete Android Studio project source without generated build caches.
- store-listing-en.md: English short and full store descriptions.

Application id: studio.cortex.thunderstack
Minimum Android version: Android 7.0 (API 24)
Target Android version: Android 15 (API 35)

Build command:
gradlew.bat :app:assembleDebug

APK output:
app/build/outputs/apk/debug/app-debug.apk

The final APK was installed and checked on the Seeker test phone.
