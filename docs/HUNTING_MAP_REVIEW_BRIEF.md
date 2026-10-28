# Hunting Map System — Architecture & Feasibility Review

**Status:** Proposal/prototype branch only. The existing app's `index.html` and APK workflow are not changed by this document.  
**Target project:** Small Game Records NL  
**Review goal:** Ask a second AI/developer to check feasibility, risks, missing requirements, and integration steps before merging anything into the live app.

## Copy/paste prompt for another AI

Please review this proposal as a senior Android/GIS engineer. Do not modify the repository or claim the feature works just because a mock-up exists. Identify what is feasible, what requires native Android code, data/provider costs, licensing concerns, implementation gaps, and a staged test plan. Inspect the existing repository and its APK build workflow before recommending integration. Preserve the current “Days hunted” feature and existing records. Return: (1) feasibility verdict, (2) recommended stack, (3) exact files/modules to add or change, (4) permissions and background-location plan, (5) offline data design, (6) authoritative Newfoundland and Labrador boundary-data plan, (7) licensing/cost risks, (8) acceptance tests, and (9) safest PR/rollback plan.

## Desired product

Build a modern hunting navigation map inspired by the usability patterns of high-end hunting apps, without copying proprietary source code or assets. Prioritize Newfoundland and Labrador; include Labrador as well as the island, and keep a path open for all Canadian provinces later.

### Map experience
- Full-screen, touch-friendly map with pan, zoom, compass/orientation support where available, and a “locate me” button.
- Road/label map and satellite/hybrid view. Satellite imagery must use a provider that explicitly permits the planned usage; do not pretend an open street-map style is satellite imagery.
- Optional terrain/topographic layer when a suitable licensed elevation/terrain source is selected.
- Independently switchable overlays: official hunting management zones, closed areas, municipal boundaries, and other government layers only where authoritative, current data is available.
- Legend and source/update date for each boundary layer.
- Waypoints: truck, camp, trailhead, hunting spot, observation, harvest, and custom pin. Allow edit/delete and export.
- Optional track recording with elapsed time, distance, and route history. Tracking should be user-controlled and clearly indicate when active.
- Return-to-truck direction/distance. It is a straight-line bearing unless real routing is separately implemented; label it accurately.
- Boundary proximity alerts only after verified boundary data and location accuracy handling are implemented. Treat as advisory, never a legal guarantee.

### GPS and Android permissions
- Foreground location permission requested in context, with explanation and a graceful denial state.
- Background location is a separate, optional capability. Request it only if continuous background track recording is genuinely implemented and required; comply with current Android permission/background execution rules and persistent notification requirements.
- Handle approximate vs precise location, GPS unavailable, battery saver, permission revoked, app process restart, and stale fixes.
- Store tracks/waypoints locally by default. Ask before sharing or exporting location data.

### Offline-first requirements
- User selects a region and downloads it explicitly.
- Wi-Fi-only is the default for large downloads, with progress, estimated size where possible, pause/resume, cancel, delete, and available-storage checks.
- Offline package must include all needed style assets, map tiles, and the boundary datasets for that region; test airplane mode, not just cached online use.
- Never promise nationwide high-zoom offline coverage without storage estimates and licensing/provider confirmation.
- Government boundary updates should show dataset version/date and require a network connection to check for updates. Use background work only for small metadata checks; large downloads stay Wi-Fi-only by default.

## Recommended technical direction

### First determine the actual app wrapper
The repository currently has a simple web app entry point in `index.html` and a GitHub Actions workflow that builds an Android debug APK. Before implementation, inspect the Android wrapper and its WebView settings. Do not assume browser geolocation, background tracking, or offline map APIs work inside the existing wrapper without testing.

### Map engine
- **Android/native target:** Evaluate MapLibre Native Android for map rendering, style layers, and offline region packs. It is a strong open-source candidate and supports offline region concepts. Official docs: https://maplibre.org/maplibre-native/android/api/ and https://maplibre.org/maplibre-compose/offline/
- **Web prototype only:** MapLibre GL JS can prove the interface and online map styling, but a browser prototype is not proof of Android background GPS or offline-pack support.
- **Base map data:** OpenStreetMap-derived data can provide roads and geographic features, subject to attribution and tile/service terms. Do not use the public OSM tile service as a bulk/offline tile source; choose a provider whose terms permit the use case or host/prepare tiles lawfully.
- **Satellite imagery:** Select a commercial or government imagery provider after checking licensing, attribution, offline caching, costs, and API-key security. Google Maps Platform has its own terms and should not be treated as an unrestricted offline tile source.
- **Terrain:** Add later, after a valid DEM/elevation source and offline approach are chosen.

### Government boundary data
Start from the Newfoundland and Labrador government's official hunting guide and boundary mapping pages:
- https://www.gov.nl.ca/fal/public-education/wildlife/hunting/boundry/
- https://www.gov.nl.ca/hunting-trapping-guide/2025-26/boundary-areas-and-online-mapping/
- Current 2026–2027 Open Seasons Hunting and Trapping Order: https://www.assembly.nl.ca/legislation/sr/annualregs/2026/nr260043.htm

Important: official online maps may be reference maps rather than downloadable GIS datasets. Determine whether authoritative GIS/GeoJSON/shapefile services exist and whether reuse is permitted. If only PDFs/images are available, do not invent precise vector boundaries from screenshots. Clearly label any temporary image overlay as approximate and not for navigation/legal decisions. Verify closed areas, small-game zones, big-game zones, municipal boundaries, and data dates separately. Expand to other provinces only after each jurisdiction's official data and licences are assessed.

## Proposed delivery stages

1. **Repository audit:** Inspect the APK wrapper, Android manifest, app entry point, permissions, storage, and current build workflow. Capture a baseline build before changes.
2. **UI prototype:** Test map screen, map-type selector, layer drawer, locate-me button, waypoint sheet, and status/errors. Keep this separate from production records.
3. **Online Android map:** Integrate the selected native engine/provider and verify actual Android device behavior and attribution.
4. **GPS + track recording:** Add permissions, visible recording state, lifecycle handling, persistence, and battery testing.
5. **Authoritative boundaries:** Add only verified datasets with provenance, date, legend, and update path.
6. **Offline downloads:** Implement region packages and Wi-Fi-first controls. Verify cold-start offline use in airplane mode.
7. **Integration:** Connect waypoints/harvest records only after a deliberate schema and migration plan. Preserve the existing Days hunted behavior and all user data.
8. **Field test:** Test on an Android phone in low-signal/no-signal conditions; compare recorded tracks against known routes and verify the map does not imply legal certainty.
9. **Release:** Build via GitHub Actions, inspect logs/artifact, test installation and update behavior, and keep rollback instructions.

## Acceptance checklist

- [ ] Current app and “Days hunted” behavior are unchanged until an explicitly reviewed integration.
- [ ] Map loads reliably on a real Android phone and displays useful errors when offline.
- [ ] User sees and can deny location permission without the app crashing.
- [ ] Live position updates correctly; tracking state is visible and stoppable.
- [ ] Background tracking works only if implemented with compliant Android permissions/notification and battery tests.
- [ ] Waypoints survive app restart and can be exported/deleted.
- [ ] Each boundary layer has source, date, legend, and known limitations.
- [ ] No invented or screenshot-traced legal boundaries.
- [ ] Offline region works after force-stop/relaunch in airplane mode, including its selected overlays.
- [ ] Wi-Fi-only default is honored for large downloads.
- [ ] Attribution, service terms, API keys, and provider costs are documented.
- [ ] GitHub Actions builds a test APK; artifact is installed and tested before any production merge.

## Main feasibility risks
1. Satellite imagery is the biggest licensing/cost/offline constraint; it is not automatically included by MapLibre.
2. Background GPS requires native Android work and permission-policy compliance; a web-only page is insufficient.
3. Official hunting zone data may be published as online maps/PDFs rather than reusable GIS vectors.
4. Nationwide high-zoom offline maps can consume substantial storage and bandwidth.
5. A successful web preview does not establish that the Android APK is integrated or field-ready.

## Source references
- MapLibre Android API: https://maplibre.org/maplibre-native/android/api/
- MapLibre offline maps: https://maplibre.org/maplibre-compose/offline/
- NL government boundary mapping: https://www.gov.nl.ca/fal/public-education/wildlife/hunting/boundry/
- NL 2025–26 online mapping guide: https://www.gov.nl.ca/hunting-trapping-guide/2025-26/boundary-areas-and-online-mapping/
- NL 2026–27 regulation: https://www.assembly.nl.ca/legislation/sr/annualregs/2026/nr260043.htm
