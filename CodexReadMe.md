**Screens Flow created by Codex**


EV Trip Planner Screen Flow
A. App startup and permissions
1.
EvPlannerCarAppService → EvPlannerSession
◦
The car host starts the app service and creates a session.
◦
The session initializes:
▪
EvTelemetryManager for vehicle data.
▪
ChargingStationRepository for charging station data.
▪
EvMapSurfaceRenderer for drawing on the car display.
◦
It checks required permissions.
2.
PermissionRequiredScreen
◦
If permissions are missing, this screen explains why the app needs vehicle and location access.
◦
The driver selects Grant Permissions.
◦
When all required permissions are granted:
▪
Telemetry subscriptions are refreshed.
▪
The app opens ChargerListScreen.
B. Nearby charging stations
3.
ChargerListScreen
◦
Shows the charging station list in a PlaceListMapTemplate.
◦
Each station row includes its name, distance, estimated arrival battery level, connector, power, and stall availability.
◦
Station coordinates are attached as place metadata so the car host can show station locations on the map.
◦
The screen also shows the current battery level and range.
◦
Available actions:
▪
Tap a station → open StationDetailScreen.
▪
Trip Plan → open TripSearchScreen.
▪
Filter → open ChargerFilterGridScreen.
▪
Battery and energy row → open energy analytics, or the charging monitor if plugged in.
C. Station details and route choice
4.
StationDetailScreen
◦
Shows details for the selected station.
◦
Continue / route action → opens ChargerRoutePreviewScreen.
5.
ChargerRoutePreviewScreen
◦
Shows route options and estimates for arrival battery level, remaining range, charging time, and cost.
◦
The driver can toggle battery preconditioning and select an Eco or fastest route.
◦
Start Navigation → opens ActiveNavigationScreen.
D. Navigation and charging
6.
ActiveNavigationScreen
◦
Shows navigation instructions and estimated arrival information for the selected station.
◦
End Route → ends navigation and returns to the previous screen.
◦
Plug In → opens ChargingSessionScreen.
7.
ChargingSessionScreen
◦
Shows charging status, current battery level, charging power, and estimated time to the selected charge target.
◦
The session also monitors charge-port telemetry and can open this screen automatically when the vehicle is plugged in.
E. Multi-stop trip planning
8.
TripSearchScreen
◦
Shows preset destinations and allows the driver to search.
◦
Selecting a destination calculates a trip plan using current vehicle telemetry and available stations.
◦
Select a destination → opens TripItineraryScreen.
9.
TripItineraryScreen
◦
Shows the trip destination and planned charging stops.
◦
Select a charging stop → opens ChargerRoutePreviewScreen for that station.
◦
Start Trip → opens route preview for the first charging stop, if the plan has one.
F. Supporting screens
10.
ChargerFilterGridScreen
◦
Lets the driver filter stations, such as in-range, ultra-fast, or available chargers.
◦
Choosing a filter returns to the updated ChargerListScreen.
11.
EnergyAnalyticsTabScreen
◦
Shows battery and efficiency diagnostics, plus sample charging history.
12.
Other station and vehicle screens
◦
ChargingReceiptScreen, ReportIssueGridScreen, and VehicleSettingsScreen provide receipt, issue-reporting, and vehicle-preference flows.
Flow at a glance:
App startup
→ PermissionRequiredScreen (only if permissions are missing)
→ ChargerListScreen
→ StationDetailScreen
→ ChargerRoutePreviewScreen
→ ActiveNavigationScreen
→ ChargingSessionScreen

      → TripSearchScreen
          → TripItineraryScreen
              → ChargerRoutePreviewScreen

      → ChargerFilterGridScreen
      → EnergyAnalyticsTabScreen