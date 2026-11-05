# Contributing to ArjunFit

Thanks for helping. ArjunFit is a Kotlin + Jetpack Compose fitness app built to be easy to learn from.

## Get it running

1. Install [Android Studio](https://developer.android.com/studio) (it includes the JDK and Android SDK).
2. Clone the repository and open the folder in Android Studio.
3. In **Build › Select Build Variant**, choose **`openDebug`**. It needs no extra downloads.
   The `samsungDebug` variant also needs the Samsung Health Data SDK (see `README.md`); its licence
   doesn't allow it in this repository.
4. Run on a phone or emulator.

Command line:

```bash
./gradlew assembleOpenDebug           # build the app
./gradlew testOpenDebugUnitTest       # unit tests
./gradlew lintOpenDebug               # Android lint
```

## Find your way around

Read `docs/architecture/SYSTEM_ARCHITECTURE.md` first. In short:

- `app/src/main/.../data/health/`: the `HealthDataSource` interface, shared models, source selection.
- `app/src/samsung/`: code that uses the Samsung SDK (only in the `samsung` build).
- `app/src/main/.../ui/`: Compose screens; each has a small, pure `*Model.kt` that is unit-tested.
- `app/src/main/.../data/`: Room databases, settings, the AI Coach client.

### Adding a new health data source

1. Implement `HealthDataSource` (see `SamsungHealthSource` for a full example). Return the shared
   models from `data/health/model`; never expose SDK types to the UI.
2. Declare only READ permissions, and only for data the app shows.
3. Add it to the list passed to `HealthSources` in `ui/ArjunFitApp.kt`, and to `SourceKind`.
4. Add tests with a fake, like `HealthSourcesTest`.

## Rules

- **Never commit** API keys, `.env`, `local.properties`, keystores, the Samsung SDK AAR, or real
  health data and screenshots of it. CI scans every commit with Gitleaks.
- Keep changes small: one feature or fix per pull request.
- New logic gets unit tests. Database changes need a migration and the updated `app/schemas/` file.
- Use clear, simple English in the UI, comments and docs.
- Respect the system "remove animations" setting (`LocalReduceMotion`) and keep touch targets ≥ 48 dp.
- Add a line to `CHANGELOG.md` under "Unreleased".

## Pull requests

1. Create a branch from `main`.
2. Make your change; run `./gradlew testOpenDebugUnitTest lintOpenDebug`.
3. Open a pull request and fill in the template. CI must pass before review.

Security problems: see `docs/SECURITY.md`; don't open a public issue.
