package com.example.deep

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.deep.ui.theme.DeepTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.URL
import java.util.Locale

// ── Server URL ────────────────────────────────────────────────────────────────
// Change to "http://10.0.2.2:3001" when testing with the Android emulator
// and the wherez-server running locally on your Windows machine.
private const val WHEREZ_SERVER_URL = "http://76.103.100.81:3001"
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            DeepTheme {
                WherezApp()
            }
        }
    }
}

data class LocationInfo(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val name: String = "",
    val streetNo: String = "",
    val streetName: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "",
    val county: String = "",
    val locality: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WherezApp() {
    val context = LocalContext.current
    var locationInfo by remember { mutableStateOf(LocationInfo()) }
    var statusText by remember { mutableStateOf("Tap 'Wherez?' to find your location") }
    var showSettings by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var selectedPrivacyLevel by remember { mutableStateOf("City and State") }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationUpdates(context, locationInfo, mapView, currentMarker) { info, status, marker ->
                locationInfo = info
                statusText = status
                currentMarker = marker
            }
        } else {
            statusText = "Location permission denied"
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            statusText = "Contacts access granted. Pick a contact."
        } else {
            statusText = "Contacts permission denied"
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIndex   = c.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val contactId = c.getString(idIndex)
                    val name      = c.getString(nameIndex) ?: ""
                    var contact   = "$name,"
                    var contactAddress = ""

                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId), null
                    )
                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val typeIndex  = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                            val phone = pc.getString(phoneIndex) ?: ""
                            val type  = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                context.resources, pc.getInt(typeIndex), ""
                            ).toString()
                            contact += "$type:$phone,"
                        }
                    }

                    val emailCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId), null
                    )
                    emailCursor?.use { ec ->
                        while (ec.moveToNext()) {
                            val emailIndex = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                            contact += "${ec.getString(emailIndex)},"
                        }
                    }

                    val addressCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                        arrayOf(contactId), null
                    )
                    addressCursor?.use { ac ->
                        if (ac.moveToFirst()) {
                            val addrIndex = ac.getColumnIndex(
                                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
                            )
                            contactAddress = ac.getString(addrIndex) ?: ""
                        }
                    }

                    contactName = contact
                    statusText  = if (contactAddress.isNotEmpty())
                        "Looking up: $contactAddress"
                    else
                        "Contact: $name (no address)"

                    if (contactAddress.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val geocoder  = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(contactAddress, 1)
                                withContext(Dispatchers.Main) {
                                    val addr = addresses?.firstOrNull()
                                    if (addr != null) {
                                        val lat = addr.latitude
                                        val lng = addr.longitude
                                        mapView?.let { map ->
                                            currentMarker?.let { map.overlays.remove(it) }
                                            val marker = Marker(map).apply {
                                                position = GeoPoint(lat, lng)
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                title = "$name's location"
                                            }
                                            map.overlays.add(marker)
                                            map.controller.animateTo(GeoPoint(lat, lng))
                                            map.controller.setZoom(12.0)
                                            map.invalidate()
                                            currentMarker = marker
                                        }
                                        val info = LocationInfo(
                                            latitude  = lat,
                                            longitude = lng,
                                            city      = addr.locality ?: "",
                                            state     = addr.adminArea ?: "",
                                            country   = addr.countryName ?: ""
                                        )
                                        locationInfo = info
                                        statusText = buildString {
                                            append("$name: ")
                                            if (info.city.isNotEmpty())    append("${info.city}, ")
                                            if (info.state.isNotEmpty())   append("${info.state}, ")
                                            if (info.country.isNotEmpty()) append(info.country)
                                        }.trimEnd(',', ' ')
                                    } else {
                                        statusText = "Could not find location for: $contactAddress"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusText = "Geocoding error: ${e.message}"
                                }
                            }
                        }
                    }

                    // Also call the wherez server and log to MongoDB
                    CoroutineScope(Dispatchers.IO).launch {
                        val serverMsg = makeApiCall(contact)
                        val saveMsg   = saveLocation(contact, locationInfo)
                        withContext(Dispatchers.Main) {
                            println("Server: $serverMsg | Save: $saveMsg")
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsScreen(
            locationInfo         = locationInfo,
            selectedPrivacyLevel = selectedPrivacyLevel,
            onPrivacyLevelSelected = { selectedPrivacyLevel = it },
            onDismiss = { showSettings = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(10.0)
                        controller.setCenter(GeoPoint(0.0, 0.0))
                        mapView = this
                    }
                },
                update = { view -> mapView = view }
            )

            if (statusText.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    color  = Color.Black.copy(alpha = 0.7f),
                    shape  = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text     = statusText,
                        color    = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showSettings = true },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier        = Modifier.size(56.dp),
                    contentPadding  = PaddingValues(0.dp)
                ) {
                    Text("⚙", fontSize = 22.sp)
                }

                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            startLocationUpdates(
                                context, locationInfo, mapView, currentMarker
                            ) { info, status, marker ->
                                locationInfo  = info
                                statusText    = status
                                currentMarker = marker
                            }
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.height(48.dp).width(130.dp)
                ) {
                    Text("Wherez?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.READ_CONTACTS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        } else {
                            contactPickerLauncher.launch(null)
                        }
                    },
                    colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    modifier       = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("👤", fontSize = 22.sp)
                }
            }
        }
    }
}

// ── Location updates ──────────────────────────────────────────────────────────

fun startLocationUpdates(
    context: android.content.Context,
    currentInfo: LocationInfo,
    mapView: MapView?,
    currentMarker: Marker?,
    onUpdate: (LocationInfo, String, Marker?) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        onUpdate(currentInfo, "Location permission not granted", currentMarker)
        return
    }

    val locationManager =
        context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val lat = location.latitude
            val lng = location.longitude

            CoroutineScope(Dispatchers.Main).launch {
                mapView?.let { map ->
                    map.controller.animateTo(GeoPoint(lat, lng))
                    map.controller.setZoom(15.0)
                    currentMarker?.let { map.overlays.remove(it) }
                    val marker = Marker(map).apply {
                        position = GeoPoint(lat, lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "You are here"
                    }
                    map.overlays.add(marker)
                    map.invalidate()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val geocoder  = Geocoder(context, Locale.getDefault())
                            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                            withContext(Dispatchers.Main) {
                                val addr = addresses?.firstOrNull()
                                val info = LocationInfo(
                                    latitude    = lat,
                                    longitude   = lng,
                                    name        = addr?.featureName ?: "",
                                    streetNo    = addr?.subThoroughfare ?: "",
                                    streetName  = addr?.thoroughfare ?: "",
                                    city        = addr?.locality ?: "",
                                    state       = addr?.adminArea ?: "",
                                    zip         = addr?.postalCode ?: "",
                                    country     = addr?.countryName ?: "",
                                    county      = addr?.subAdminArea ?: "",
                                    locality    = addr?.subLocality ?: ""
                                )
                                val statusMsg = buildString {
                                    if (info.city.isNotEmpty())    append("${info.city}, ")
                                    if (info.state.isNotEmpty())   append("${info.state} ")
                                    if (info.country.isNotEmpty()) append(info.country)
                                    if (isEmpty()) append("Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}")
                                }
                                onUpdate(info, statusMsg, marker)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                onUpdate(
                                    LocationInfo(latitude = lat, longitude = lng),
                                    "Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}",
                                    marker
                                )
                            }
                        }
                    }
                }
            }
            locationManager.removeUpdates(this)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    try {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 500f, listener)
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 500f, listener)
        }
        val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (last != null) listener.onLocationChanged(last)
        else onUpdate(currentInfo, "Acquiring location…", currentMarker)
    } catch (e: Exception) {
        onUpdate(currentInfo, "Location error: ${e.message}", currentMarker)
    }
}

// ── Wherez server API calls ───────────────────────────────────────────────────

suspend fun makeApiCall(contact: String): String {
    return try {
        val encoded = java.net.URLEncoder.encode(contact, "UTF-8")
        val urlStr  = "$WHEREZ_SERVER_URL/?id=$encoded"
        val response = withContext(Dispatchers.IO) {
            val connection = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout    = 8_000
            try {
                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
        val json = JSONObject(response)
        val id   = json.optString("id", "")
        val msg  = json.optString("msg", "")
        "$id says $msg"
    } catch (e: java.net.SocketTimeoutException) {
        "Server timeout — is the wherez server running?"
    } catch (e: Exception) {
        "Server Error: ${e.message}"
    }
}

suspend fun saveLocation(contact: String, info: LocationInfo): String {
    return try {
        val encoded = java.net.URLEncoder.encode(contact, "UTF-8")
        val params  = buildString {
            append("streetNo=${java.net.URLEncoder.encode(info.streetNo, "UTF-8")}")
            append("&streetName=${java.net.URLEncoder.encode(info.streetName, "UTF-8")}")
            append("&city=${java.net.URLEncoder.encode(info.city, "UTF-8")}")
            append("&state=${java.net.URLEncoder.encode(info.state, "UTF-8")}")
            append("&zip=${java.net.URLEncoder.encode(info.zip, "UTF-8")}")
            append("&country=${java.net.URLEncoder.encode(info.country, "UTF-8")}")
            append("&lat=${info.latitude}&lng=${info.longitude}")
        }
        val urlStr = "$WHEREZ_SERVER_URL/save/$encoded?$params"
        withContext(Dispatchers.IO) {
            val connection = URL(urlStr).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout    = 8_000
            try {
                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
    } catch (e: Exception) {
        "Save error: ${e.message}"
    }
}
