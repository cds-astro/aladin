# Aladin for Android

A **native** reimplementation (Kotlin + Jetpack Compose) of the
Aladin Sky Atlas (CDS) concept for Android, with a UI designed for mobile: it is **not**
a port of the Aladin Desktop (Swing) UI, but a new touch interface.

It reuses the same CDS web services as Aladin Desktop:

- **HiPS** (sky image surveys) — nested HEALPix tiles.
- **Simbad** — object search by name or coordinates.
- **VizieR** — astronomical catalogs (cone search).

## Features (MVP)

- HiPS survey viewer with touch pan/zoom (TAN gnomonic projection).
- Survey selector (DSS2 color, 2MASS, ALLWISE, GALEX, …).
- Object search by name (Simbad) and map centering.
- Tap the sky to identify the object (Simbad cone search).
- VizieR catalog overlays (Gaia DR3, 2MASS, Tycho-2, SDSS…).
- Manual markers and angular distance measurements (great circle).
- Status bar with center RA/Dec/FoV.

## Architecture

```
app/src/main/java/com/aladin/android/
  AladinApp.kt               Application
  MainActivity.kt            Entry point (Compose)
  SkyViewModel.kt            State (StateFlow): camera, surveys, catalogs, markers
  model/                     SkyPosition, Marker, Measurement, CatalogRow
  sky/
    Healpix.kt               Nested HEALPix (forward: center and corners)
    Projection.kt            TAN projection (forward/inverse) with RA wraparound
    SkyCamera.kt             Camera state (pan/zoom/recenter)
    SkyRenderer.kt           NESTED recursion for visible tiles
  data/
    net/HttpClient.kt        Shared OkHttpClient
    hips/                    HiPSProperties, HiPSSurvey, SurveyRegistry, TileRepository
    simbad/SimbadService.kt  Simbad search (VOTable)
    vizier/VizieRService.kt  VizieR cone search (VOTable)
    VotableParser.kt         Minimal VOTable parser (TABLEDATA)
  ui/
    theme/                   Color, Theme, Type (Compose)
    SkyCanvas.kt             Canvas that draws tiles + overlays and manages gestures
    SkyScreen.kt             Top bar, drawer, bottom sheets, FABs, status bar
```

### Implementation Notes

- **HiPS Rendering**: recursion of the NESTED tree (the children of `(order,npix)` are
  `4*npix+{0,1,2,3}` in `order+1`), so only HEALPix *forward* is needed
  (center and corners). Each tile is placed affinely (rotation+scale around
  the projected center) — correct approximation at moderate FoV.
- **Projection**: TAN (gnomonic), the one used by Aladin by default.
- **Tiles**: memory cache (LruCache) + disk cache (OkHttp Cache),
  asynchronous loading with repaint when the bitmap arrives.

## Build

Requirements: Android SDK (platform 35 + build-tools), JDK 17+.

```bash
cp local.properties.example local.properties   # adjust sdk.dir
./gradlew assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and press Run.

> Note: this project was generated without being able to compile it on the origin machine
> (there was no Android platform installed in the SDK). Review before signing.