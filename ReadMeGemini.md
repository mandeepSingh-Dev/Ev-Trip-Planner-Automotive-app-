**Screens Flow created by Gemini** 



1. PermissionRequiredScreen (MessageTemplate)

Operations:

a. Checks runtime permissions (CAR_ENERGY, ACCESS_FINE_LOCATION) via EvPermissionHelper.

b. Explains why battery telemetry and GPS access are required to calculate range and locate chargers.

Transitions:

c. OnClick Grant Permissions
→
→ Triggers CarContext.requestPermissions()
→
→ OnGranted all permissions
→
→ 2. ChargerListScreen.

2. ChargerListScreen (PlaceListMapTemplate — Main Home Hub)

Operations:

a. Displays live VHAL SoC % and Range (km) in the top header.

b. Plots charging stations' lat/long pins (Green = Reachable, Yellow = Busy, Red = Low Range) on the host Google Map.

c. Displays scrollable charging stations list with distance, kW power, stall count, and predicted arrival SoC %.

d. Fires a CarAppExtender heads-up notification banner if battery SoC <= 15%.

Transitions:

e. OnClick Any Station Row
→
→ 3. StationDetailScreen.

f. OnClick ActionStrip Trip Plan
→
→ 7. TripSearchScreen.

g. OnClick ActionStrip Filter
→
→ 9. ChargerFilterGridScreen.

h. OnClick Row 0 (Battery & Energy Diagnostics)
→
→ 10. EnergyAnalyticsTabScreen (if unplugged) OR 5. ChargingSessionScreen (if plugged in).

3. StationDetailScreen (PaneTemplate)

Operations:

a. Shows station operator, max kW speed, CCS2 connector standard, ₹/kWh tariff, and amenities (Cafe, Restroom, Wi-Fi).

b. Displays projected arrival SoC %, estimated charging time to 80%, and estimated session cost in ₹.

Transitions:

c. OnClick Reserve Stall
→
→ Toggles 15-minute stall reservation (Stall #2 Reserved (15m)) on the same screen.

d. OnClick Route Here
→
→ 4. ChargerRoutePreviewScreen.

e. OnClick Report Issue
→
→ 12. ReportIssueGridScreen.

4. ChargerRoutePreviewScreen (RoutePreviewNavigationTemplate + EvMapSurfaceRenderer)

Operations:

a. Draws the EV range halo and route polyline (Blue = Fastest, Green = Eco) onto the hardware Surface.

b. Compares Fastest Route vs. Eco Range-Saver with recalculated arrival SoC % and charge duration to 80%.

Transitions:

c. OnClick Preconditioning: ON/OFF (ActionStrip)
→
→ Toggles HV battery thermal preconditioning (+1.5 kWh energy draw) and updates arrival SoC % on the same screen.

d. OnClick Start Navigation
→
→ 5. ActiveNavigationScreen.

5. ActiveNavigationScreen (NavigationTemplate + NavigationManager + EvMapSurfaceRenderer)

Operations:

a. Streams turn-by-turn maneuver steps, distance remaining, and live ETA (TravelEstimate).

b. Broadcasts live route telemetry (ACTION_EV_NAVIGATION_UPDATE) to Project 3 (com.oem.cluster) behind the steering wheel.

Transitions:

c. OnClick End Route
→
→ Stops navigation and pops back to 4. ChargerRoutePreviewScreen.

d. OnClick Plug In (or VHAL EV_CHARGE_PORT_CONNECTED == true from any screen)
→
→ 6. ChargingSessionScreen.

6. ChargingSessionScreen (PaneTemplate — Auto-triggered on VHAL Plug-In)

Operations:

a. Streams live VHAL charging intake (kW), current SoC %, and kWh stored.

b. Calculates minutes remaining to target charge limit and verifies GEAR_PARK safety status.

Transitions:

c. OnClick Set 100% Trip / Set 80% Daily
→
→ Switches target charge limit between 80% and 100% on the same screen.

d. OnClick Find Chargers
→
→ Pops back to 2. ChargerListScreen.

e. On VHAL Unplug (EV_CHARGE_PORT_CONNECTED == false)
→
→ 7. ChargingReceiptScreen.

7. ChargingReceiptScreen (PaneTemplate)

Operations:

a. Summarizes completed session (+kWh delivered, final SoC %, and ISO 15118 Plug & Charge auto-settled total cost in ₹).

Transitions:

b. OnClick Done
→
→ Pops back to 2. ChargerListScreen.

8. TripSearchScreen (SearchTemplate — Opened from Trip Plan button)

Operations:

a. Provides search bar + highway destination presets (Jaipur 275 km, Chandigarh 248 km, Agra 232 km).

b. Calculates required highway charging stops on the fly via MultiStopTripPlanner.

Transitions:

c. OnClick Any Destination Row
→
→ 9. TripItineraryScreen.

9. TripItineraryScreen (ListTemplate)

Operations:

a. Displays step-by-step legs of the long-distance trip with recommended DC fast-charging stops and arrival/departure SoC %.

Transitions:

b. OnClick Any Charging Stop Row or Start Trip
→
→ 4. ChargerRoutePreviewScreen.

10. ChargerFilterGridScreen (GridTemplate — Opened from Filter button)

Operations:

a. Displays 4 large touch tiles (All Chargers, Within Range, 150kW+ Ultra-Fast, Available Stalls).

Transitions:

b. OnClick Any Filter Tile
→
→ Applies the filter and pops back to 2. ChargerListScreen.

11. EnergyAnalyticsTabScreen (TabTemplate — Opened from Row 0 on ChargerListScreen)

Operations:

a. Tab 1 (Energy & Efficiency): Shows live Wh/km, pack kWh, and HVAC auxiliary load via PaneTemplate.

b. Tab 2 (Charge History): Lists past charging sessions, peak kW, and costs via ListTemplate.

Transitions:

c. OnClick Settings (ActionStrip)
→
→ 12. VehicleSettingsScreen.

12. VehicleSettingsScreen (ListTemplate)

Operations:

a. Displays ambient thermal derating status (ENV_OUTSIDE_TEMPERATURE <10°C check).

Transitions:

b. OnClick Minimum Arrival SoC Buffer
→
→ Cycles safety reserve (10%
→
→ 15%
→
→ 20%).

c. OnClick ISO 15118 Plug & Charge
→
→ Toggles between auto-certificate authentication and manual RFID.

13. ReportIssueGridScreen (GridTemplate — Opened from Report Issue on StationDetailScreen)

Operations:

a. Displays 4 crowd-sourced status tiles (Stall Blocked, Charger Offline, Slow Speed, Working Great).

Transitions:

b. OnClick Any Status Tile
→
→ Shows CarToast confirmation and pops back to 3. StationDetailScreen.