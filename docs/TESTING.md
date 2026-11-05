# Testing

## Automated (run on every push by GitHub Actions)

```bash
./gradlew testOpenDebugUnitTest lintOpenDebug   # what CI runs (no Samsung SDK needed)
./gradlew testSamsungDebugUnitTest              # also needs the Samsung AAR in app/libs/
```

Unit tests live in `app/src/test/` and run on the JVM. They cover the pure logic behind the screens:

| Area | Test file |
|---|---|
| Source selection and the source manager | `data/health/SourceSelectionTest.kt`, `HealthSourcesTest.kt` |
| Home dashboard numbers and formats | `ui/home/HomeModelTest.kt` |
| Steps, heart rate, sleep, SpO2 details | `ui/detail/DetailModelsTest.kt` |
| Nutrition, weight, progress, workouts | `ui/nutrition`, `ui/weight`, `ui/progress`, `ui/workout` |
| Health sync screen, profile | `ui/sync`, `ui/profile` |
| AI Coach summary, history and Markdown | `ui/coach/CoachModelTest.kt`, `CoachMarkdownTest.kt` |

Lint (Android's official code checker) fails the build on errors, for example calling an API that
doesn't exist on Android 10, the oldest version ArjunFit supports.

**Rule:** a test counts as passed only when it has actually run. Results are uploaded as a CI artifact.

## Not automated yet (planned)

- Room database and migration tests (schemas are exported to `app/schemas/` for this).
- Compose UI and navigation tests.
- Permission flows (granted, denied, denied twice).
- Health Connect source with fake data; Demo source.
- GPS distance, pace, splits and auto-pause calculations (from day one of Stage 4).

## Manual test on a real phone

Run before every phone-test hand-off. Write down the app version from Profile › About.

**Every build**

- [ ] App opens; splash, then Home.
- [ ] Home shows steps, heart rate, sleep, SpO2 and weight from the data source.
- [ ] Steps and Heart Rate detail charts load for day, week and month.
- [ ] Sleep shows stages; Oxygen shows readings.
- [ ] Nutrition shows food and water; logging food and water works.
- [ ] Weight: log a weight; it appears on Home and Progress.
- [ ] Workout: start a plan, log a set, rest timer vibrates.
- [ ] Coach: send a message; reply appears and is formatted.
- [ ] Pull to refresh on Home.

**Data source problems**

- [ ] Samsung Health permission denied: clear message and a way to ask again.
- [ ] Samsung Health developer mode off: clear message with the fix.
- [ ] No internet: Coach says so; everything else still works.

**Planned for GPS (Stage 4)**: accuracy on a known route, one-hour battery drain, screen-off recording,
offline recording, location permission denied, location switched off mid-activity.
