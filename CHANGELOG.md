# Changelog

All notable changes to ArjunFit. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/). The app version is shown in Profile › About.

## [Unreleased] — 0.9.0-alpha2

### Fixed
- Crash risk on Android 10 and 11: sleep, active-time and step-history screens used Java methods that
  only exist on newer Android (`Duration.toMinutesPart`, `LocalDate.datesUntil`, `Stream.toList`).

### Added
- GitHub Actions: builds the `open` variant, runs lint and unit tests, and scans the whole history for
  secrets with Gitleaks on every push and pull request.
- Dependabot update pull requests for Gradle dependencies and GitHub Actions.
- Room schema export (`app/schemas/`) so future database changes can ship tested migrations.
- Documentation: system architecture, Health Connect and GPS designs, privacy, security policy, testing,
  contributing guide, issue and pull-request templates.

## [0.9.0-alpha1] — 2026-11-03

### Added
- One `HealthDataSource` interface for all health data sources, shared models in `data/health`, and
  source selection (Samsung, then Health Connect; Demo only when chosen).
- Two builds: `samsung` (with the Samsung Health Data SDK) and `open` (without it).

### Changed
- Samsung Health reading moved behind the interface; behaviour unchanged.

## [0.8.2] — 2026-11-03
### Changed
- AI Coach renders Markdown, labels replies that were cut off, and keeps answers shorter.

## [0.8.1] — 2026-11-03
### Added
- AI Coach on Claude with your own API key, encrypted with Android Keystore, after explicit consent.

## [0.7.0] — 2026-11-02
### Added
- Progress: Winter Arc timeline, weight, activity and workout views.

## [0.6.0] — 2026-11-02
### Added
- Workouts: hub, plans, exercise library (free-exercise-db), live workout with rest timer.

## [0.5.0] — 2026-11-02
### Added
- Nutrition and weight tracker with an in-app log.

## [0.4.0] — 2026-11-02
### Added
- Heart rate, steps, sleep and blood-oxygen detail screens.

## [0.3.0] — 2026-11-02
### Added
- Connect device, health sync, profile and settings.

## [0.2.0] — 2026-11-02
### Added
- Theme, logo icon, splash and Home.

## [0.1.1] — 2026-11-02
### Fixed
- Seconds shown for short exercises; heart-rate measurements counted correctly.

## [0.1.0] — 2026-11-02
### Added
- Read-only Samsung Health connection showing real Galaxy Fit3 data.
