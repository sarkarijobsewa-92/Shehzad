package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.KaamWalaAdSlot
import com.example.ui.theme.*

@Composable
fun AdminAdSettingsScreen(
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    onUpdateGlobalAds: (Boolean) -> Unit,
    onUpdateSlotConfig: (slotType: AdSlotType, isEnabled: Boolean, provider: AdProvider, adUnitId: String) -> Unit,
    onUpdateIdentifiers: (publisherId: String, monetagSiteId: String, frequency: Int) -> Unit,
    onTogglePreviewMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var publisherIdInput by remember { mutableStateOf(adSettings.adSensePublisherId) }
    var monetagSiteIdInput by remember { mutableStateOf(adSettings.monetagSiteId) }
    var frequencyInput by remember { mutableStateOf(adSettings.jobFeedFrequency.toString()) }

    var editingSlot by remember { mutableStateOf<AdSlotConfig?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_ad_settings_screen"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = KaamWalaNavy)
                    Text(
                        text = "Administrator Monetisation Console",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KaamWalaNavy
                        )
                    )
                }
                Text(
                    text = "Configure Google AdSense & Monetag slots, identifiers, and compliance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }

        // Global Ad Master Switch Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (adSettings.globalAdsEnabled) Color(0xFFEFF6FF) else Slate100
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (adSettings.globalAdsEnabled) Color(0xFFBFDBFE) else Slate300
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Global Advertising Monetisation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = if (adSettings.globalAdsEnabled) "Active on configured slots across mobile & web." else "All ad slots currently disabled / paused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700
                        )
                    }

                    Switch(
                        checked = adSettings.globalAdsEnabled,
                        onCheckedChange = { onUpdateGlobalAds(it) },
                        modifier = Modifier.testTag("global_ads_toggle")
                    )
                }
            }
        }

        // Monetag Integration & Zone Identifiers Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE9D5FF))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF9333EA)) {
                                Text("Monetag", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Text(
                                text = "ACTIVE AD NETWORK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B21A8),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                            Text("SW.JS VERIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFAF5FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Service Worker File: sw.js (Zone 10831624)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF581C87))
                            Text("domain: \"3nbf4.com\" • zoneId: 10831624", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF6B21A8))
                            Text("script: https://3nbf4.com/act/files/service-worker.min.js?r=sw", fontSize = 10.sp, color = Slate500)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = monetagSiteIdInput,
                        onValueChange = { monetagSiteIdInput = it },
                        label = { Text("Monetag Domain / Zone") },
                        placeholder = { Text("3nbf4.com / 10831624") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monetag_site_id_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = frequencyInput,
                        onValueChange = { frequencyInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Job Feed Ad Frequency (Every N Listings)") },
                        placeholder = { Text("3") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Google AdSense is disabled. All placements are powered exclusively by Monetag.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val freq = frequencyInput.toIntOrNull() ?: 3
                            onUpdateIdentifiers("", monetagSiteIdInput.trim(), freq)
                            Toast.makeText(context, "Monetag settings saved successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Monetag Settings")
                    }
                }
            }
        }

        // Slot Configurations
        item {
            Text(
                text = "CONFIGURED AD SLOTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Persistent Top Banner Under Logo Bar
        item {
            SlotConfigCard(
                title = "Top Banner (Under Logo Bar)",
                locationDesc = "Always visible directly under Taskly logo across all pages",
                config = adSettings.headerSlot,
                onEdit = { editingSlot = adSettings.headerSlot },
                onToggle = { enabled ->
                    onUpdateSlotConfig(
                        AdSlotType.HEADER_BANNER,
                        enabled,
                        adSettings.headerSlot.provider,
                        adSettings.headerSlot.adUnitOrZoneId
                    )
                }
            )
        }

        // Slot 1: Homepage
        item {
            SlotConfigCard(
                title = "Slot 1: Homepage Banner",
                locationDesc = "Below hero section, before main listings",
                config = adSettings.homepageSlot,
                onEdit = { editingSlot = adSettings.homepageSlot },
                onToggle = { enabled ->
                    onUpdateSlotConfig(
                        AdSlotType.HOMEPAGE,
                        enabled,
                        adSettings.homepageSlot.provider,
                        adSettings.homepageSlot.adUnitOrZoneId
                    )
                }
            )
        }

        // Slot 2: Job Listing Feed
        item {
            SlotConfigCard(
                title = "Slot 2: Job Listing Feed",
                locationDesc = "Inserted between job cards every ${adSettings.jobFeedFrequency} items",
                config = adSettings.jobFeedSlot,
                onEdit = { editingSlot = adSettings.jobFeedSlot },
                onToggle = { enabled ->
                    onUpdateSlotConfig(
                        AdSlotType.JOB_FEED,
                        enabled,
                        adSettings.jobFeedSlot.provider,
                        adSettings.jobFeedSlot.adUnitOrZoneId
                    )
                }
            )
        }

        // Slot 3: Optional Content Ad
        item {
            SlotConfigCard(
                title = "Slot 3: Content & Informational (Optional)",
                locationDesc = "Bottom of guides and privacy documentation",
                config = adSettings.contentSlot,
                onEdit = { editingSlot = adSettings.contentSlot },
                onToggle = { enabled ->
                    onUpdateSlotConfig(
                        AdSlotType.CONTENT_INFO,
                        enabled,
                        adSettings.contentSlot.provider,
                        adSettings.contentSlot.adUnitOrZoneId
                    )
                }
            )
        }

        // Live Preview Sandbox Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE AD CONTAINER PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dev Placeholders", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = adSettings.previewModeEnabled,
                                onCheckedChange = { onTogglePreviewMode(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Previewing Slot 1 (Homepage) in current state:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    KaamWalaAdSlot(
                        slotConfig = adSettings.homepageSlot,
                        globalAdsEnabled = adSettings.globalAdsEnabled,
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        minHeight = 85.dp
                    )
                }
            }
        }

        // Revenue Reporting Status (Strict Requirement: Must display clear message explaining reporting is not connected)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Slate700)
                        Text(
                            text = "Advertising Revenue Dashboard",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Authorised reporting integration has not been connected. Revenue dashboards show real provider data only when authorized Google AdSense / Monetag reporting APIs are linked. No fabricated metrics are displayed.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F), fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to edit slot
    editingSlot?.let { slot ->
        EditSlotDialog(
            slot = slot,
            onDismiss = { editingSlot = null },
            onSave = { isEnabled, provider, unitId ->
                onUpdateSlotConfig(slot.slotType, isEnabled, provider, unitId)
                editingSlot = null
            }
        )
    }
}

@Composable
private fun SlotConfigCard(
    title: String,
    locationDesc: String,
    config: AdSlotConfig,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(locationDesc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Slate500)
                }

                Switch(checked = config.isEnabled, onCheckedChange = onToggle)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Provider: ${config.provider.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = KaamWalaNavy
                    )
                    Text(
                        text = "• Unit: ${config.adUnitOrZoneId}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        color = Slate700
                    )
                }

                TextButton(onClick = onEdit) {
                    Text("Configure", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EditSlotDialog(
    slot: AdSlotConfig,
    onDismiss: () -> Unit,
    onSave: (Boolean, AdProvider, String) -> Unit
) {
    var isEnabled by remember { mutableStateOf(slot.isEnabled) }
    var selectedProvider by remember { mutableStateOf(slot.provider) }
    var unitId by remember { mutableStateOf(slot.adUnitOrZoneId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(isEnabled, selectedProvider, unitId.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Configure ${slot.slotType.slotName}") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Slot Enabled:")
                    Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Provider:", style = MaterialTheme.typography.labelMedium)

                AdProvider.values().forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProvider = provider }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(provider.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = unitId,
                    onValueChange = { unitId = it },
                    label = { Text("Ad Unit ID / Zone ID") },
                    placeholder = { Text("e.g. kw_home_banner_01") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    )
}
