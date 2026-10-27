# Sikkanam

A free, offline income and expense tracker for Android. The logo is the Tamil word சிக்கனம் (sikkanam), which means thrift.

## Developer

Developed by Mohammed Ashraf. Website: [smatechsolution.com](https://smatechsolution.com)

## License

Sikkanam is free software, released under the [GNU General Public License v3.0](LICENSE).

## Features

**Tracking**
- Add, edit, and delete income and expense entries, with a category and an optional note
- Month-by-month view, with previous and next month navigation
- Balance, income, and expense totals for the month
- Search by category or note, and filter by income or expense
- Spending by category, shown as bars
- Export all transactions to CSV

**Budgets**
- Monthly limit per category, with progress and an over-budget warning

**Islamic finance**
- Mark income as halal, doubtful, or haram, and track purification due from haram income
- Record sadaqah and purification payments
- Mark expenses that are interest (riba), and total the interest paid each month
- Zakat calculator with silver or gold nisab, using the 2.5% rate
- Today's date in the Hijri calendar

Data stays on the device. There's no account and no ads. The internet is used only for the optional check for a newer version (GitHub builds only).

## Not yet built

- Accounts and transfers between them
- Recurring transactions
- Multiple currencies
- Attachments, such as receipt photos
- Cloud sync and backup
- Biometric lock and home screen widget
- Tamil and Arabic translations
- Reports over a year

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- Room for local storage
- Min SDK 24, target SDK 35

## Getting started

1. Install Android Studio (it bundles the JDK and the Android SDK).
2. Open this folder in Android Studio. It will sync the project.
3. Run the `app` configuration on an emulator or a device.

Command line:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## Status

The debug APK is built by GitHub Actions on every push to `main`.
