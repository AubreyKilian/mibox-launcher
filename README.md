# mibox-launcher

An ad-free, offline home screen for a Mi Box S (Android TV / Google TV, Android 11).

- A **Favourites** row of five slots, then every other app, five across, sorted by name.
- **Hold OK** on any tile to pin it or unpin it.
- Dark theme, a clock, the date, and a **gear** that opens Android Settings
  (Settings → System → Restart is there).
- **No permissions at all.** The manifest declares no `INTERNET`, so the app cannot reach the
  network, load ads or send analytics. Check with `aapt2 dump badging app-release.apk`.
- No recommendations, no search, no live-TV row. It shows apps only.

## Build and test

Needs JDK 17 or newer and the Android SDK (platform 34, build-tools 34.0.0).
Put `sdk.dir=/path/to/Android/Sdk` in `local.properties` (git-ignored).

```sh
./gradlew testDebugUnitTest assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the local debug key and is not debuggable. That is fine for a
personal sideload. Do not publish it.

The grid logic (`HomeModel`) is pure Kotlin and covered by unit tests. The UI was also run on an
Android 11 TV emulator image (1080p) and driven with D-pad key events.

## Install on the box

1. On the box: Settings → Device Preferences → Developer options → turn on USB debugging.
   On this box that also opens ADB on TCP port 5555.
2. From your computer:
   ```sh
   adb connect <box-ip>:5555
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
   Accept the "Allow USB debugging?" dialog on the TV.
3. Turn USB debugging off again, which closes port 5555.

## Make the Home button open it

Google's TV launcher has a higher `HOME` priority than any normal app, and Android ignores the
"default home app" choice when priorities differ. `adb shell cmd package set-home-activity`
records the choice but has no effect (tested on the emulator). Two things work:

1. **Button Mapper (recommended).** Map the Home button to open this app ("Home"). Nothing on
   the system changes. Remove the mapping to go back.
2. **Disable the stock launcher (advanced).** With the stock launcher disabled, Android falls
   through to the next `HOME` app. Tested on the emulator with its launcher. On a Mi Box S the
   next candidate would be Google's setup recovery screen, so you would have to disable that too.
   Not tested on the box. Undo with `adb shell pm enable --user 0 <package>`.

## Uninstall

```sh
adb uninstall one.aubrey.launcher
```
