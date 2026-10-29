package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

/**
 * Reusable, policy-compliant Advertising Container
 * - Clearly labeled as "Advertisement" or "Sponsored"
 * - Separate visual identity from organic job cards
 * - Fixed/reserved minimum height to avoid Cumulative Layout Shift (CLS)
 * - Never mimics job listings, no deceptive click targets, no auto-refresh
 */
@Composable
fun KaamWalaAdSlot(
    slotConfig: AdSlotConfig,
    globalAdsEnabled: Boolean,
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    modifier: Modifier = Modifier,
    minHeight: Dp = 100.dp
) {
    // If ads are disabled globally or slot is turned off, do not render or reserve space
    if (!globalAdsEnabled || !slotConfig.isEnabled || slotConfig.provider == AdProvider.NONE) {
        if (adSettings.previewModeEnabled) {
            // Neutral development placeholder explaining why slot is inactive
            InactiveAdPlaceholder(slotConfig = slotConfig, modifier = modifier)
        }
        return
    }

    val isPersonalized = consentPreferences.personalizedAdsAccepted

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .testTag("ad_slot_${slotConfig.slotType.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Slate100
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate300)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Distinct Ad Label and Provider Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ad Information",
                        tint = Slate500,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ADVERTISEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Slate500
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (slotConfig.provider) {
                        AdProvider.GOOGLE_ADSENSE -> Color(0xFFE8F0FE)
                        AdProvider.MONETAG -> Color(0xFFF3E8FF)
                        else -> Slate200
                    }
                ) {
                    Text(
                        text = slotConfig.provider.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = when (slotConfig.provider) {
                            AdProvider.GOOGLE_ADSENSE -> Color(0xFF1967D2)
                            AdProvider.MONETAG -> Color(0xFF6B21A8)
                            else -> Slate700
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Ad Container Body (Monetag Exclusive Network - Zone 10831624)
            when (slotConfig.provider) {
                AdProvider.NONE -> {
                    // Handled above
                }
                else -> {
                    MonetagAdContent(
                        slotConfig = slotConfig,
                        siteId = adSettings.monetagSiteId
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Consent and Policy transparency notice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersonalized) "Personalized Ads (DPDP Consent Active)" else "Non-Personalized Ad (Privacy Mode)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = Slate500
                )
                Text(
                    text = "ID: ${slotConfig.adUnitOrZoneId}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun AdSenseAdContent(
    slotConfig: AdSlotConfig,
    publisherId: String,
    isPersonalized: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4285F4)
                ) {
                    Text(
                        text = "Google",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Google AdSense Responsive Unit",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Slot: ${slotConfig.slotType.slotName} • Unit: ${slotConfig.adUnitOrZoneId}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate500
            )
            Text(
                text = "Publisher: $publisherId",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Slate500
            )
        }
    }
}

@Composable
private fun MonetagAdContent(
    slotConfig: AdSlotConfig,
    siteId: String
) {
    val zoneId = if (slotConfig.adUnitOrZoneId.isNotBlank() && slotConfig.adUnitOrZoneId != "tsk_content_footer_03") {
        slotConfig.adUnitOrZoneId
    } else {
        "10831624"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE9D5FF), RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF9333EA)
                ) {
                    Text(
                        text = "Monetag",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Monetag MultiTag & Native Banner",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF581C87)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Domain: 3nbf4.com • Zone ID: $zoneId",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color(0xFF6B21A8)
            )
            Text(
                text = "Integrated via sw.js service-worker (Slot: ${slotConfig.slotType.slotName})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Slate500
            )
        }
    }
}

@Composable
fun InactiveAdPlaceholder(
    slotConfig: AdSlotConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Slate100)
            .border(1.dp, Slate200, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ad Space Reserved (${slotConfig.slotType.slotName}) — Disabled in Admin Settings",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Slate500
            ),
            textAlign = TextAlign.Center
        )
    }
}
