# EV Trip Planner Automotive App (`com.oem.evplanner`)

An advanced, production-grade Android Automotive OS (AAOS) and Android Auto application engineered for Electric Vehicle (EV) drivers. The app integrates deeply with vehicle hardware via VHAL (Vehicle Hardware Abstraction Layer), fetches live charging points of interest via OpenChargeMap REST APIs, calculates accurate real-time energy efficiency with cold-weather thermal derating, plans multi-stop long-distance corridor trips, and broadcasts navigation telemetry to digital instrument cluster HUDs.

---

## Project Summary

`EV Trip Planner Automotive` is built using the **Android for Cars App Library (CAL)** and is designed to adhere strictly to driver distraction guidelines and AAOS quality standards. It supports seamless transitions between offline fallback charging stations and live OpenChargeMap network data, intelligent range estimation, live charging session monitoring (Plug & Charge), and driver preference customization.

---

## VHAL & Vehicle Properties

The application interfaces directly with vehicle sensor and property managers to read state-of-charge, energy consumption, and environmental metrics:
- **`EV_BATTERY_LEVEL`** (Watt-hours remaining)
- **`INFO_EV_BATTERY_CAPACITY`** (Total HV battery pack capacity in Wh)
- **`RANGE_REMAINING`** (Estimated driving distance in meters)
- **`EV_CHARGE_PORT_CONNECTED`** (Boolean sensor for auto-launching charging session telemetry)
- **`EV_BATTERY_INSTANTANEOUS_CHARGE_RATE`** (Milliwatts intake during DC fast charging)
- **`ENV_OUTSIDE_TEMPERATURE`** (Ambient temperature in °C for cold-weather range derating)
- **`CURRENT_GEAR`** (Driving state restriction enforcement and parking status checks)

---

## Core Topics & Capabilities

1. **Android for Cars App Library**: Built using templates (`PlaceListMapTemplate`, `RoutePreviewNavigationTemplate`, `PaneTemplate`, `GridTemplate`, `TabTemplate`, `SearchTemplate`, `NavigationTemplate`, `MessageTemplate`).
2. **Custom Map Surface Rendering**: Implements [`EvMapSurfaceRenderer`](file:///C:/Users/Lenovo/AndroidStudioProjects/EVTripPlannerAutomotiveapp/app/src/main/java/com/oem/evplanner/map/EvMapSurfaceRenderer.kt) via `SurfaceCallback` to draw custom EV Range Halos, Fastest (Blue) vs Eco (Green) route polylines, and destination charger pins directly on the host map canvas.
3. **Live POI Networking**: Powered by **Retrofit 3.0** and **OkHttp** to query [OpenChargeMap API](https://openchargemap.org) (`api.openchargemap.io/v3/poi`) with fallback to offline NCR/NH48 highway chargers.
4. **Range & Energy Intelligence**: [`EvRangeCalculator`](file:///C:/Users/Lenovo/AndroidStudioProjects/EVTripPlannerAutomotiveapp/app/src/main/java/com/oem/evplanner/domain/EvRangeCalculator.kt) dynamically computes live Wh/km consumption, applies an 18% cold-weather heating penalty when ambient temperature drops below 10°C, accounts for preconditioning energy draw (+1.5 kWh), and calculates DC fast-charging taper duration to 80% SoC.
5. **Multi-Stop Trip Planner**: [`MultiStopTripPlanner`](file:///C:/Users/Lenovo/AndroidStudioProjects/EVTripPlannerAutomotiveapp/app/src/main/java/com/oem/evplanner/domain/MultiStopTripPlanner.kt) automatically segments long-distance corridors (e.g., Delhi to Jaipur or Chandigarh) into energy legs and inserts optimal DC charging stops.
6. **Cross-App Instrument Cluster HUD**: [`ClusterHudBroadcaster`](file:///C:/Users/Lenovo/AndroidStudioProjects/EVTripPlannerAutomotiveapp/app/src/main/java/com/oem/evplanner/cluster/ClusterHudBroadcaster.kt) broadcasts navigation telemetry (`ACTION_EV_NAVIGATION_UPDATE`) to secondary instrument cluster displays behind the steering wheel.
7. **Voice Intent Integration**: Intercepts `geo:` intents and `ACTION_NAVIGATE` to instantly route users to the nearest compatible charging station via Google Assistant.
8. **Charging Sessions & Plug & Charge**: ISO 15118 Plug & Charge simulation, live kilowatt intake monitoring, battery temperature/thermal state reporting, and automated receipt generation upon unplugging.

---

## Libraries Used

- **Android for Cars App Library**:
  - `androidx.car.app:app`
  - `androidx.car.app:app-automotive`
  - `androidx.car.app:app-testing`
- **Networking (Retrofit & OkHttp)**:
  - `com.squareup.retrofit2:retrofit` (v3.0.0)
  - `com.squareup.retrofit2:converter-gson`
  - `com.squareup.okhttp3:okhttp` (v5.5.1)
  - `com.squareup.okhttp3:logging-interceptor`
- **Android Jetpack & Kotlin**:
  - Lifecycle Runtime KTX & Coroutines Flow
  - Compose BOM & Material 3

---

## System Architecture & Data-Flow Blueprint (`com.oem.evplanner`)

```text
====================================================================================================================
                                  1. VEHICLE HARDWARE & SYSTEM SERVICES (AAOS OS)
====================================================================================================================
  [VHAL / CAN Bus Signals]                                [AAOS Driving State]             [Google Assistant / System]
   ├─ EV_BATTERY_LEVEL (Wh)                                ├─ CURRENT_GEAR                  ├─ ACTION_NAVIGATE (geo:)
   ├─ INFO_EV_BATTERY_CAPACITY (Wh)                        └─ CarUxRestrictionsManager        └─ CarNotificationManager
   ├─ RANGE_REMAINING (meters)                                (Clamps list items to <=6          (CarAppExtender heads-up
   ├─ EV_CHARGE_PORT_CONNECTED (bool)                          when CURRENT_GEAR != PARK)         low-range alerts)
   ├─ EV_BATTERY_INSTANTANEOUS_CHARGE_RATE (mW)
   └─ ENV_OUTSIDE_TEMPERATURE (°C)
               │                                                    │                                   │
               └────────────────────────┬───────────────────────────┘                                   │
                                        ▼                                                               │
====================================================================================================================
                          2. TELEMETRY, DOMAIN & NETWORK LAYER (com.oem.evplanner)
====================================================================================================================
  +-----------------------------------------------+      +-------------------------------------------------------+
  | EvTelemetryManager                            |      | ChargingStationRepository & OpenChargeMapClient       |
  | (Car.createCar -> CarPropertyManager)         |      | (Retrofit 3.0 + OkHttp + Gson)                        |
  |  • Streams reactive StateFlow<EvTelemetryState|      |  • Fetches live POIs from api.openchargemap.io/v3/poi |
  |  • Computes live SoC %, kW intake, & Park lock|      |  • Merges with offline NCR/NH48 fallback stations     |
  +-----------------------+-----------------------+      +---------------------------+---------------------------+
                          │                                                          │
                          └────────────────────────────┬─────────────────────────────┘
                                                       ▼
  +--------------------------------------------------------------------------------------------------------------+
  | Domain Calculation & Preferences Engine                                                                      |
  |  • EvRangeCalculator: Computes live Wh/km efficiency, applies +18% cold-weather derating (<10°C),            |
  |    preconditioning energy draw (+1.5 kWh), projected Arrival SoC %, & DC taper charge duration to 80%.       |
  |  • MultiStopTripPlanner: Splits highway trips (e.g., Delhi -> Jaipur) into energy legs & inserts DC stops.   |
  |  • EvPreferencesStore: Holds min arrival SoC buffer (10%/15%/20%) & ISO 15118 Plug & Charge state.           |
  +-------------------------------------------------------+------------------------------------------------------+
                                                          │
====================================================================================================================
                       3. CAR APP LIBRARY (CAL) SESSION, HOST RENDERING & CROSS-APP BRIDGE
====================================================================================================================
                                                          ▼
  +--------------------------------------------------------------------------------------------------------------+
  | EvPlannerCarAppService  ──▶  EvPlannerSession (Lifecycle & Global Event Router)                              |
  |  • Startup Gate: Checks EvPermissionHelper -> launches PermissionRequiredScreen if CAR_ENERGY/GPS missing.   |
  |  • Global VHAL Interrupt: Watches EV_CHARGE_PORT_CONNECTED (false -> true) -> auto-pushes ChargingSession.  |
  |  • Voice Intent Handler (onNewIntent): Intercepts "Hey Google, navigate to charger" -> jumps to RoutePreview.|
  +-----------------------+-----------------------------------------------------------+--------------------------+
                          │                                                           │
          [Template Data & Actions]                                   [Hardware Surface Allocation]
                          ▼                                                           ▼
  +-----------------------------------------------+      +-------------------------------------------------------+
  | AAOS System Template Host (GAS / AOSP Host)   |      | EvMapSurfaceRenderer (SurfaceCallback)                |
  |  • Enforces driver distraction UX rules       |      |  • Draws directly on host Surface via Canvas          |
  |  • Renders Google Maps in PlaceListMapTemplate|      |  • Renders live EV Range Halo (proportional to VHAL),  |
  |  • Renders native CarAppActivity UI templates |      |    Fastest (Blue) vs Eco (Green) route polylines, &   |
  +-----------------------+-----------------------+      |    destination charger kW pins on Navigation screens. |
                          │                              +---------------------------+---------------------------+
                          │                                                          │
                          │                                    [Explicit OEM Broadcast: ACTION_EV_NAVIGATION_UPDATE]
                          │                                                          ▼
                          │                              +-------------------------------------------------------+
                          │                              | Project 3: Digital Instrument Cluster (com.oem.cluster)|
                          │                              |  • Receives station name, distance, & arrival SoC %   |
                          │                              |    to display behind the steering wheel HUD.          |
                          │                              +-------------------------------------------------------+
                          │
====================================================================================================================
                                  4. COMPLETE SCREEN FLOW & NAVIGATION TOPOLOGY
====================================================================================================================

  [App Launch] ──(Missing CAR_ENERGY / GPS)──▶ [0. PermissionRequiredScreen (MessageTemplate)]
       │                                              │ ("Grant Permissions" -> CarContext.requestPermissions)
       ▼ (Permissions Granted)                        ▼
  +--------------------------------------------------------------------------------------------------------------+
  | 1. ChargerListScreen (PlaceListMapTemplate — Host-Rendered Google Map + Live SoC/Range Header)               |
  |     • Row 0 Shortcut ─────────────────────────▶ [7. EnergyAnalyticsTabScreen (TabTemplate)]                  |
  |     • ActionStrip: "Filter" ──────────────────▶ [6. ChargerFilterGridScreen (GridTemplate)]                  |
  |     • ActionStrip: "Trip Plan" ───────────────▶ [4. TripSearchScreen (SearchTemplate)]                       |
  |     • Background Alert (SoC <= 15%) ──────────▶ [EvNotificationHelper -> CarAppExtender Heads-Up Banner]     |
  +-------------------------------------------------------+------------------------------------------------------+
                                                          │ (Tap Charger Row)
                                                          ▼
  +--------------------------------------------------------------------------------------------------------------+
  | 2. StationDetailScreen (PaneTemplate)                                                                        |
  |     • Shows operator, kW, connector, ₹/kWh tariff, arrival SoC, amenities, & 15-min "Reserve Stall" toggle.  |
  |     • Action: "Report Issue" ─────────────────▶ [8. ReportIssueGridScreen (GridTemplate)]                    |
  +-------------------------------------------------------+------------------------------------------------------+
                                                          │ (Tap "Route Here" OR from [5. TripItineraryScreen])
                                                          ▼
  +--------------------------------------------------------------------------------------------------------------+
  | 3. ChargerRoutePreviewScreen (RoutePreviewNavigationTemplate + EvMapSurfaceRenderer)                         |
  |     • Compares Fastest Route vs. Eco Range-Saver with recalculated Arrival SoC % & charge time to 80%.       |
  |     • ActionStrip: "Preconditioning: ON/OFF" (simulates HV battery thermal warming +1.5 kWh draw).           |
  +-------------------------------------------------------+------------------------------------------------------+
                                                          │ (Tap "Start Navigation")
                                                          ▼
  +--------------------------------------------------------------------------------------------------------------+
  | 9. ActiveNavigationScreen (NavigationTemplate + NavigationManager + EvMapSurfaceRenderer)                    |
  |     • Streams turn-by-turn Maneuver/Step cards, live TravelEstimate, & broadcasts HUD updates to Cluster.    |
  +-------------------------------------------------------+------------------------------------------------------+
                                                          │ (VHAL EV_CHARGE_PORT_CONNECTED == true OR "Plug In")
                                                          ▼
  +--------------------------------------------------------------------------------------------------------------+
  | 10. ChargingSessionScreen (PaneTemplate)  ──(On Unplug)──▶ [11. ChargingReceiptScreen (PaneTemplate)]        |
  |     • Live kW intake, SoC %, time to 80%/100% target,        • ISO 15118 Plug & Charge auto-settled receipt  |
  |       & GEAR_PARK safety verification.                         with total kWh delivered & ₹ billed.          |
  +--------------------------------------------------------------------------------------------------------------+
```



**Another Same High level Diagram but in short**

[AAOS HARDWARE & CLOUD]
├── VHAL / CAN Bus (Battery Wh, Range m, Gear, Plug-In, Temp °C) ──▶ EvTelemetryManager (StateFlow)
├── CarUxRestrictionsManager (Driving list cap <= 6 items) ─────────┘
└── OpenChargeMap Cloud (Retrofit 3.0 + Gson) ──────────────────────▶ ChargingStationRepository
│
▼
[DOMAIN & SESSION ENGINE]                                         EvRangeCalculator & MultiStopTripPlanner
├── EvPlannerCarAppService ──▶ EvPlannerSession                 (Arrival SoC %, Cold-Weather -18%, Trip Stops)
│    ├── Startup Gate ─────────▶ PermissionRequiredScreen                     │
│    ├── Plug-In Interrupt ────▶ Auto-pushes ChargingSessionScreen            │
│    ├── Voice Intent (geo:) ──▶ Jumps to ChargerRoutePreviewScreen           │
│    └── Low Battery (<=15%) ──▶ CarAppExtender Heads-Up Notification         │
│                                                                             │
├── EvMapSurfaceRenderer (SurfaceCallback Canvas: Range Halo + Route Line) ◀──┤
└── ClusterHudBroadcaster ────▶ Project 3: Digital Cluster (com.oem.cluster) ◀┘
│
▼
[CAR APP LIBRARY (CAL) SCREEN FLOW]
1. ChargerListScreen (PlaceListMapTemplate — Host Google Map + Live SoC Header)
   ├── Action: "Filter" ────▶ ChargerFilterGridScreen (GridTemplate)
   ├── Action: "Trip Plan" ─▶ TripSearchScreen (SearchTemplate) ──▶ TripItineraryScreen (ListTemplate)
   ├── Row 0: "Battery" ────▶ EnergyAnalyticsTabScreen (TabTemplate) / VehicleSettingsScreen
   └── Tap Station ─────────▶ 2. StationDetailScreen (PaneTemplate + Stall Reserve + ReportIssueGridScreen)
   │
   ▼
   3. ChargerRoutePreviewScreen (RoutePreviewNavigationTemplate + Preconditioning)
   │
   ▼
   4. ActiveNavigationScreen (NavigationTemplate + Turn-by-Turn + Cluster Sync)
   │ (On Plug-In)
   ▼
   5. ChargingSessionScreen (PaneTemplate) ──(On Unplug)──▶ 6. ChargingReceiptScreen