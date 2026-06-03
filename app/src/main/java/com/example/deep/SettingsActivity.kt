package com.example.deep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PrivacyOption(
    val section: String,
    val label: String,
    val key: String
)

val privacyOptions = listOf(
    PrivacyOption("Coordinates", "Coordinates", "coordinates"),
    PrivacyOption("Address Detail", "Street Address", "street_address"),
    PrivacyOption("Address Detail", "Street Name Only", "street_name"),
    PrivacyOption("Address Detail", "City and State", "city_state"),
    PrivacyOption("Address Detail", "State Only", "state_only"),
    PrivacyOption("Address Detail", "Zip Code", "zip"),
    PrivacyOption("Address Detail", "Country", "country"),
    PrivacyOption("Local", "County", "county"),
    PrivacyOption("Local", "Locality", "locality"),
    PrivacyOption("Privacy", "None (Hidden)", "none")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    locationInfo: LocationInfo,
    selectedPrivacyLevel: String,
    onPrivacyLevelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedPrivacyLevel) }

    val displayValue = remember(selected, locationInfo) {
        when (selected) {
            "coordinates" -> "Lat: ${"%.6f".format(locationInfo.latitude)}, Lng: ${"%.6f".format(locationInfo.longitude)}"
            "street_address" -> "${locationInfo.streetNo} ${locationInfo.streetName}".trim()
            "street_name" -> locationInfo.streetName
            "city_state" -> "${locationInfo.city}, ${locationInfo.state}".trim(',', ' ')
            "state_only" -> locationInfo.state
            "zip" -> locationInfo.zip
            "country" -> locationInfo.country
            "county" -> locationInfo.county
            "locality" -> locationInfo.locality
            "none" -> "Not Sharing Location"
            else -> ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Privacy Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        onPrivacyLevelSelected(selected)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Current value display
            if (displayValue.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    color = Color(0xFF1565C0).copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sharing as:", fontSize = 12.sp, color = Color.Gray)
                        Text(displayValue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                    }
                }
            }

            Text(
                "Your friends can only see the selected detail:",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val grouped = privacyOptions.groupBy { it.section }
                grouped.forEach { (section, options) ->
                    item {
                        Text(
                            text = section.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    options.forEach { option ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .clickable {
                                        selected = option.key
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option.label, fontSize = 16.sp)
                                if (selected == option.key) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF1565C0)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
