# Sikkanam

A free, offline income and expense tracker for Android. Sikkanam (சிக்கனம்) is Tamil for thrift.

## Features (v1)

- Add income and expense entries with amount, category, and optional note
- Balance, income, and expense totals
- Data stays on the device: no account, no server, no ads

## Planned

- Zakat calculator (nisab and haul)
- Halal and haram income tagging, with purification tracking
- Sadaqah and donation log
- Interest (riba) flags on expenses
- Hijri date display

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Room for local storage
- Min SDK 24, target SDK 35

## Getting started

1. Install Android Studio (it bundles the JDK and the Android SDK).
2. Open this folder in Android Studio. It will create the Gradle wrapper and sync the project.
3. Run the `app` configuration on an emulator or a device.

Command line, once the wrapper exists:

```bash
./gradlew :app:assembleDebug
```

## Status

The first version has not been compiled or run yet. The Android SDK was not available when the project was created.
