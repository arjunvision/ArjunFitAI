# GPS activity tracking (planned, Stage 4)

**Status: not built yet.** This is the design, checked against official documentation on
2026-11-03. Re-check versions before implementing.

## What it will do

Record walks, runs and rides with the phone's GPS: live route map, distance, current and average pace,
speed, elapsed time, pause/resume and auto-pause, kilometre splits, a summary, route history, personal
records, elevation gain where the phone has a barometer, and GPX export.

## Location

- Google's Fused Location Provider (`com.google.android.gms:play-services-location`, 21.4.0 at the
  time of writing), `Priority.PRIORITY_HIGH_ACCURACY`, updates every 1–2 seconds while recording.
  Google warns high accuracy "may cause significant battery drain", so recording runs only between
  Start and Finish. [Battery guidance](https://developer.android.com/develop/sensors-and-location/location/battery)
- A **foreground service** keeps recording with the screen off, as Google recommends for fitness
  tracking. [Scenarios](https://developer.android.com/develop/sensors-and-location/location/battery/scenarios)
  - Manifest: `android:foregroundServiceType="location"`, plus `FOREGROUND_SERVICE` and
    `FOREGROUND_SERVICE_LOCATION`.
  - Location permission must already be granted, and the service starts from the Record screen while
    the app is visible.
  - A visible notification shows the activity while recording.
  - **No background-location permission** is requested: a foreground service started by the user keeps
    "while in use" access with the screen off.
  - [Service types](https://developer.android.com/develop/background-work/services/fgs/service-types),
    [launching](https://developer.android.com/develop/background-work/services/fgs/launch),
    [location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions)

## Maps: MapLibre with OpenFreeMap (proposed, to be confirmed in Stage 4)

| | MapLibre + OpenFreeMap | Mapbox | Google Maps |
|---|---|---|---|
| Licence | BSD-2 (open source) | Proprietary | Google terms |
| Key | None | Access token | API key and billing account |
| Cost | Free | 25k monthly users free, then paid | Map loads listed as unlimited |

OpenStreetMap credit must stay visible on the map, and `tile.openstreetmap.org` must not be used by
apps. Sources: [MapLibre Native](https://github.com/maplibre/maplibre-native),
[OpenFreeMap](https://openfreemap.org/), [Mapbox pricing](https://www.mapbox.com/pricing),
[OSM tile policy](https://operations.osmfoundation.org/policies/tiles/),
[attribution](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines).

## Calculations (all unit-tested)

- Distance: sum of distances between accepted points; points with poor accuracy are dropped.
- Pace: minutes per km; current pace from a short rolling window.
- Auto-pause: below a speed threshold for a few seconds. Each pause starts a new GPX track segment.
- Elevation gain: from the barometer (`Sensor.TYPE_PRESSURE`), using differences between readings, which
  Android's docs say give good results. Hidden on phones without a barometer.
  [`SensorManager.getAltitude`](https://developer.android.com/reference/android/hardware/SensorManager#getAltitude(float,%20float))
- GPX 1.1 export with UTC times. [GPX 1.1 schema](https://www.topografix.com/gpx/1/1/)

## Privacy

- Routes stay on the phone and are private by default.
- Privacy zones hide the start and end of a route near places you mark (home, college, work).
- A new database table means a Room migration (schemas are exported in `app/schemas/`).

## Google Play

Play Console needs a foreground-service declaration with a short video of a user starting tracking.
[Play FGS requirements](https://support.google.com/googleplay/android-developer/answer/13392821)

## Real-phone tests

GPS accuracy against a known route, battery use over one hour, screen-off recording, recording with no
internet, location permission denied, location turned off mid-activity. See `docs/TESTING.md`.
