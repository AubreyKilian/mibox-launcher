# mibox-launcher

An ad-free, offline home screen for a Mi Box S (Android TV / Google TV, Android 11).

Built for one box and tested only on a Mi Box S and an Android 11 TV emulator. It should work
on other Android TV devices (it needs Android 5.0 or newer), but no promises.

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

There are no prebuilt APKs. Build your own: the release build is signed with your local debug
key and is not debuggable. That is fine for sideloading onto your own box, but don't distribute
an APK signed that way.

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

### Build from source and install with a coding agent

A coding agent such as [Claude Code](https://claude.com/claude-code) can do most of the build
and install for you. There is no prebuilt APK, so the agent builds the app from source. That
needs JDK 17 or newer, the Android SDK (platform 34, build-tools 34.0.0) and adb on your
computer. If they are not already installed, the agent has to download and install them first,
which can come to a gigabyte or more. The first Gradle build also downloads Gradle and the
project's dependencies.

You still have to do the steps on the TV yourself: turning on USB debugging and accepting the
"Allow USB debugging?" dialog. Run the agent on the computer that will build the app, on the same
network as the box, and give it a prompt like this one (put in your box's IP address, which is
under Settings → Network & Internet on the box):

```text
Install the mibox-launcher Android TV launcher on my TV box, which is at 192.168.1.50.

1. Clone https://github.com/AubreyKilian/mibox-launcher and read its README.
2. Check for JDK 17+, the Android SDK (platform 34, build-tools 34.0.0) and adb. If any are
   missing, tell me what you would install and how, and wait for my OK before installing.
3. Create local.properties with sdk.dir, then run
   ./gradlew testDebugUnitTest assembleRelease. Stop and show me any failure.
4. Tell me to turn on USB debugging on the box (Settings → Device Preferences → Developer
   options) and wait until I say it is on.
5. Run adb connect <ip>:5555. Tell me to accept the "Allow USB debugging?" dialog on the TV,
   then check adb devices until the box shows as "device", not "unauthorized".
6. Install app/build/outputs/apk/release/app-release.apk with adb install -r and confirm the
   package one.aubrey.launcher is installed.
7. Run adb disconnect and remind me to turn USB debugging off again.

Do not disable or uninstall any system apps, and do not change the default home app. Ask me
before running any command that changes the box other than installing this APK.
```

The agent stops wherever you need to act on the TV. Afterwards, set up the Home button yourself,
as described in the next section.

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

## License

MIT. See [LICENSE](LICENSE).
