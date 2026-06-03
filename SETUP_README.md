# Deep (Android) - Wherez App Setup

## One Required Step Before Running: Google Maps API Key

This app uses Google Maps. You need a free API key:

1. Go to https://console.cloud.google.com/
2. Create a project → Enable "Maps SDK for Android"
3. Create an API Key under "Credentials"
4. Open `app/src/main/AndroidManifest.xml`
5. Add this inside `<application>` tag:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

## App Features (converted from iOS Wherez)

| iOS Feature | Android Equivalent |
|---|---|
| CLLocationManager | LocationManager + FusedLocationProvider |
| MKMapView | Google Maps Compose |
| ABPeoplePickerNavigationController | ContactsContract + PickContact |
| UITableViewController (HiddenVC) | Compose SettingsScreen |
| Page-curl animation | Slide navigation |
| CLGeocoder reverseGeocode | Android Geocoder |
| API call to 76.103.100.81:3001 | OkHttp/URL.readText() |

## Project Structure

```
app/src/main/java/com/example/deep/
├── MainActivity.kt          ← Map + location + contacts + API (ViewController.m)
├── SettingsActivity.kt      ← Privacy level picker (HiddenViewController.m)
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Permissions Requested at Runtime
- `ACCESS_FINE_LOCATION` — GPS location
- `READ_CONTACTS` — Contact picker
- `INTERNET` — API call to wherez server
