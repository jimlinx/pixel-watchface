# PixelFace

A digital watch face for Pixel Watch 3 (Wear OS 5 or newer).

It shows time with seconds, date, battery, steps, heart rate, today's distance and the
currently playing track. It has three tap buttons: torch, media controls and Swim.com.

## Project layout

| Module | What it is |
|---|---|
| `watchface` | The watch face, written in Watch Face Format v2 (XML only, no code). Layout lives in `watchface/src/main/res/raw/watchface.xml`. |
| `companion` | Small Kotlin app, "PixelFace Data". It provides two complications the format cannot do on its own: today's distance (from Health Services) and now playing (from media sessions). |

## Build

Open the folder in Android Studio, or from a terminal:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew assembleDebug
```

APKs land in `watchface/build/outputs/apk/debug/` and `companion/build/outputs/apk/debug/`.

## Install on the watch

1. On the watch, open Settings > System > About > Versions and tap Build number 7 times.
2. Open Settings > Developer options. Turn on ADB debugging and Wireless debugging.
3. In Wireless debugging, tap "Pair new device" and note the pairing code, IP and port.
4. From the Mac (the watch and Mac must be on the same Wi-Fi):

```sh
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB pair <ip>:<pairing-port>          # enter the pairing code
$ADB connect <ip>:<debug-port>         # port shown on the Wireless debugging screen
$ADB install -r companion/build/outputs/apk/debug/companion-debug.apk
$ADB install -r watchface/build/outputs/apk/debug/watchface-debug.apk

# Now-playing needs notification access. Wear OS has no settings screen for it, so grant it here:
$ADB shell cmd notification allow_listener com.jimlin.pixelface.companion/com.jimlin.pixelface.companion.MediaListenerService
```

5. On the watch, open the "PixelFace Data" app and tap "Allow distance".
6. Long-press the current watch face, swipe to "Add", and pick PixelFace.

## Set up the shortcuts

Long-press the face and tap "Edit". Then:

- **Left button**: choose "App shortcut", then Flashlight.
- **Right button**: choose "App shortcut", then Swim.com.
- **Middle button**: fixed. It opens the system media controls.
- **Distance** and **Now playing** default to PixelFace Data. If they show empty, pick
  "PixelFace Data" for those slots.
- **Accent color**: cyan, lime, orange or pink.

Tapping the track title opens the media controls, the same as the middle button.

## Notes

- Distance is shown in kilometres.
  Health Services batches updates, so it can lag a few minutes behind.
- Heart rate is blank until the watch has a reading. The watch asks for sensor permission
  the first time the face is used.
- Phone media: the companion reads media sessions on the watch, and media notifications
  as a fallback. Whether phone playback appears depends on how Wear OS bridges it on your
  watch. Watch-side players (YouTube Music, Spotify on the watch) always work.

## Testing without a music app (debug builds only)

```sh
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell am broadcast -a com.jimlin.pixelface.FAKE_MEDIA -p com.jimlin.pixelface.companion --es title "Test" --es artist "Artist"
$ADB shell am broadcast -a com.jimlin.pixelface.FAKE_MEDIA -p com.jimlin.pixelface.companion --ez stop true
```

## Validate the watch face XML

Google's validator lives in https://github.com/google/watchface. Build it with
`./gradlew :specification:validator:executable-jar` in `third_party/wff`, then:

```sh
java -jar wff-validator.jar 2 watchface/src/main/res/raw/watchface.xml
```

## Credits

The heart and footprint icons are from [Material Symbols](https://github.com/google/material-design-icons) by Google, licensed under Apache 2.0.
