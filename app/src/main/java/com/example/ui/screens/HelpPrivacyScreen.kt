package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdSettings
import com.example.model.ConsentPreferences
import com.example.model.TASKLY_SUPPORT_EMAIL
import com.example.ui.components.KaamWalaAdSlot
import com.example.ui.components.SupportContactCard
import com.example.ui.theme.*

@Composable
fun HelpPrivacyScreen(
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    onUpdateConsent: (personalized: Boolean, analytics: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var personalizedAds by remember(consentPreferences) { mutableStateOf(consentPreferences.personalizedAdsAccepted) }
    var analytics by remember(consentPreferences) { mutableStateOf(consentPreferences.analyticsAccepted) }
    var showSavedNotice by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("help_privacy_screen"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Help, Support & Privacy Desk",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KaamWalaNavy
                    )
                )
                Text(
                    text = "Support query desk, task guidelines & DPDP Act compliance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }

        // Support Email Card (sarkarijobsewa@gmail.com)
        item {
            SupportContactCard()
        }

        // DPDP Consent Control Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = KaamWalaNavy)
                        Text(
                            text = "PRIVACY & ADVERTISING CONSENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "In accordance with India's Digital Personal Data Protection Act (DPDP Act 2023) and global ad compliance standards, you have full control over your data and tracking preferences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Essential Services (Always On)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Essential App & Escrow Services", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Required for user logins, task bookings, and UPI escrow reconciliation.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Slate500)
                        }
                        Switch(checked = true, onCheckedChange = null, enabled = false)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

                    // Personalized Advertising
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personalized Advertising", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Permits Google AdSense and Monetag to show relevant ads based on device identifiers. If disabled, only generic ads will be shown.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Slate500)
                        }
                        Switch(
                            checked = personalizedAds,
                            onCheckedChange = { personalizedAds = it },
                            modifier = Modifier.testTag("consent_personalized_toggle")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

                    // Analytics & Performance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Platform Performance & Analytics", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Helps us optimize task discovery and mobile app stability.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Slate500)
                        }
                        Switch(
                            checked = analytics,
                            onCheckedChange = { analytics = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onUpdateConsent(personalizedAds, analytics)
                            showSavedNotice = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Privacy Preferences")
                    }

                    if (showSavedNotice) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Preferences saved. Consent recorded under DPDP Act protocols.",
                            style = MaterialTheme.typography.bodySmall.copy(color = KaamWalaGreenText),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        // How KaamWala Works & Worker Safety Guide
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "HOW KAAMWALA WORKS & WORKER SAFETY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val safetyPoints = listOf(
                        "1. Transparent 10% Fee: KaamWala charges a flat 10% commission on completed tasks. For a ₹500 task, platform receives ₹50 and worker receives ₹450.",
                        "2. Escrow Protection: Customer deposits money before you travel to work. Never start unverified jobs.",
                        "3. Direct UPI Settlement: Funds are transferred directly to worker UPI IDs (e.g. GPay, PhonePe, Paytm).",
                        "4. Dispute Resolution: If work is disputed, funds remain held in escrow until evidence is reviewed by a human moderator."
                    )

                    safetyPoints.forEach { pt ->
                        Text(
                            text = pt,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate700,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // AD SLOT 3: OPTIONAL CONTENT ADVERTISEMENT
        // "At the bottom of informational pages, help articles, or educational content. Controlled by administrator."
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SPONSORED PARTNERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                KaamWalaAdSlot(
                    slotConfig = adSettings.contentSlot,
                    globalAdsEnabled = adSettings.globalAdsEnabled,
                    adSettings = adSettings,
                    consentPreferences = consentPreferences,
                    minHeight = 80.dp
                )
            }
        }
    }
}
