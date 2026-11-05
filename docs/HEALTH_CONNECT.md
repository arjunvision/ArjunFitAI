# Health Connect integration (planned, Stage 3)

**Status: not built yet.** This is the design, checked against official documentation on
2026-11-03. Re-check versions before implementing.

## Why Health Connect

Health Connect is Android's shared store for health data. Many wearable companion apps copy their data
into it, so one integration reaches many devices. It is built into Android 14 and later; on Android 9–13
it is a Play Store app. Google Fit's APIs are supported only until the end of 2026, and Google points
on-device apps to Health Connect.

- Overview: https://developer.android.com/health-and-fitness/health-connect
- Google Fit migration: https://developer.android.com/health-and-fitness/health-connect/migration/fit

## Plan

- Library: `androidx.health.connect:connect-client:1.1.0` (latest stable; 1.2 is alpha). Needs
  compileSdk 36 and AGP 8.9.1+, which the project already meets.
- `HealthConnectSource` implements the existing `HealthDataSource` interface, returning the same models
  as Samsung, so no screen changes for data.
- Availability: `HealthConnectClient.getSdkStatus()`; if an install or update is needed, open the Play
  Store with Google's official onboarding link.
- READ permissions only, and only for data types the app shows: steps, heart rate, resting heart rate,
  sleep, oxygen saturation, weight, body fat, exercise, distance, total calories (active calories where
  present).
- Daily totals with `aggregate()` (not by adding raw records), which avoids double counting phone and
  watch steps.
- Manifest: a permission-rationale activity, the Android 14+ `VIEW_PERMISSION_USAGE` alias, and the
  `<queries>` entry for Android 13 and lower.
- History: by default only about 30 days before the first grant can be read. Older data (for example
  90 days of weight) needs the optional `READ_HEALTH_DATA_HISTORY` permission.
- Reads happen while the app is open; no background reads are planned.

Docs: [get started](https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started),
[data types](https://developer.android.com/health-and-fitness/guides/health-connect/plan/data-types),
[read data](https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data),
[official sample](https://github.com/android/health-samples/tree/main/health-connect/HealthConnectSample).

## What each wearable app actually shares

Compatibility depends on what the companion app writes into Health Connect, not on the watch. ArjunFit
will show "not shared by your app" instead of zero when a type is missing.

| Brand → app | Steps | Heart rate | Resting HR | Sleep stages | SpO2 | Weight | Exercise | Active kcal |
|---|---|---|---|---|---|---|---|---|
| Samsung → Samsung Health | Yes | Yes | ? | Yes | Yes | Yes | Yes | Partial |
| Fitbit / Pixel Watch → Google Health | Yes | Yes | Yes | Yes | **No** | Yes | Yes | **No** |
| Garmin → Garmin Connect | Yes\* | Yes | ? | Yes\* | ? | Yes\* | Yes\* | Yes\* |
| Amazfit → Zepp | ? | Yes\* | ? | ? | ? | Yes\* | ? | Partial\* |
| Xiaomi / Redmi → Mi Fitness | ? | ? | ? | ? | ? | ? | ? | ? |
| Phone only (Android 14+) | Yes | No | No | No | No | No | No | No |

Yes = official source. \* = secondary source only. ? = not confirmed. To be verified on real devices.

Sources: [Samsung Health and Health Connect](https://developer.samsung.com/health/blog/en/accessing-samsung-health-data-through-health-connect),
[Google Health app data types](https://support.google.com/googlehealth/answer/14506680),
[phone step counting](https://developer.android.com/health-and-fitness/guides/health-connect/develop/steps).

## Google Play requirements (before release)

Fill in the Health apps declaration in Play Console and justify each data type; show the same privacy
policy in the app's rationale screen and on the store listing. See
[declare access](https://developer.android.com/health-and-fitness/guides/health-connect/publish/declare-access)
and the [Health Connect policy](https://support.google.com/googleplay/android-developer/answer/9888170).
