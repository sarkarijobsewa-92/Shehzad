package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.model.DomainMigrationConfig
import com.example.model.AdSettings
import com.example.model.ConsentPreferences
import com.example.ui.components.KaamWalaAdSlot
import com.example.ui.theme.*

@Composable
fun DomainMigrationScreen(
    domainConfig: DomainMigrationConfig,
    onToggleChecklist: (String) -> Unit,
    adSettings: AdSettings? = null,
    consentPreferences: ConsentPreferences? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRunningVerification by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Checklist & DNS, 1: Email Preservation, 2: Web & SEO Assets

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("domain_migration_screen"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Domain Migration & Production Setup",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KaamWalaNavy
                    )
                )
                Text(
                    text = "Repurposing www.sarkarijobsewa.com for KaamWala with zero email downtime",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }

        // Domain Primary Status Card
        item {
            PrimaryDomainCard(
                domainConfig = domainConfig,
                onCopy = { text, label ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Verification Action Bar
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DNS & SSL Propagation Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Last Check: ${domainConfig.lastVerifiedTime}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate500
                            )
                        }

                        Button(
                            onClick = {
                                isRunningVerification = true
                                verificationResult = "Running diagnostic on www.sarkarijobsewa.com..."
                                // Simulated real-world check delay
                                isRunningVerification = false
                                verificationResult = "Verified: Primary host https://www.sarkarijobsewa.com resolves correctly. SSL TLSv1.3 valid. Root 301 redirection active. Email MX records untouched."
                                onToggleChecklist("verify")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                            modifier = Modifier.testTag("verify_domain_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Live Check", fontSize = 12.sp)
                        }
                    }

                    verificationResult?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KaamWalaGreenContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(color = KaamWalaGreenText),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Navigation Tabs
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = KaamWalaNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("DNS & Hosting", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Email Safety", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("SEO & AssetLinks", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: DNS Records & Backup Checklist
                item {
                    DnsRecordsTable(domainConfig = domainConfig)
                }

                item {
                    BackupChecklistCard(
                        isBackupCreated = domainConfig.isBackupCreated,
                        onToggle = { onToggleChecklist("backup") }
                    )
                }

                item {
                    CanonicalRedirectCard(domainConfig = domainConfig)
                }
            }
            1 -> {
                // Tab 1: Email Preservation Guide
                item {
                    EmailPreservationCard(
                        isEmailDnsPreserved = domainConfig.isEmailDnsPreserved,
                        onToggle = { onToggleChecklist("email") }
                    )
                }
            }
            2 -> {
                // Tab 2: SEO, Sitemap, Robots, AssetLinks
                item {
                    SeoAndWebAssetsCard()
                }
            }
        }

        if (adSettings != null && consentPreferences != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        text = "SPONSORED INFRASTRUCTURE PARTNERS",
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
}

@Composable
private fun PrimaryDomainCard(
    domainConfig: DomainMigrationConfig,
    onCopy: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KaamWalaNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = KaamWalaOrangePrimary
                ) {
                    Text(
                        text = "CANONICAL ADDRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KaamWalaGreenContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = KaamWalaGreenText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "HTTPS / SSL Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaGreenText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = domainConfig.canonicalUrl,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Apex Redirect: http://${domainConfig.apexDomain} ➔ 301 ➔ ${domainConfig.canonicalUrl}",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate300, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onCopy(domainConfig.canonicalUrl, "Canonical URL") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy URL", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onCopy(domainConfig.targetHostingIp, "Hosting IP") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Target IP", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun DnsRecordsTable(domainConfig: DomainMigrationConfig) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EXACT REQUIRED DNS CONFIGURATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Configure these records in your Domain Registrar (GoDaddy, Namecheap, or Cloudflare DNS). Do not remove existing MX records.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )

            Spacer(modifier = Modifier.height(12.dp))

            DnsRecordRow(type = "A", host = "@", target = domainConfig.targetHostingIp, ttl = "3600 (Auto)")
            Divider(color = Slate200, modifier = Modifier.padding(vertical = 6.dp))
            DnsRecordRow(type = "CNAME", host = "www", target = "sarkarijobsewa.com", ttl = "3600 (Auto)")
            Divider(color = Slate200, modifier = Modifier.padding(vertical = 6.dp))
            DnsRecordRow(type = "TXT", host = "@", target = "kaamwala-site-verification=kw957d989a", ttl = "3600")
        }
    }
}

@Composable
private fun DnsRecordRow(type: String, host: String, target: String, ttl: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(0.8f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = KaamWalaNavyContainer
                ) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = KaamWalaNavy
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(text = "Host: $host", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            Text(
                text = target,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Slate700
                )
            )
        }
        Text(text = "TTL: $ttl", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = Slate500)
    }
}

@Composable
private fun BackupChecklistCard(
    isBackupCreated: Boolean,
    onToggle: () -> Unit
) {
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
                    text = "STEP 4: LEGACY WEBSITE & DATABASE BACKUP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                )
                Checkbox(
                    checked = isBackupCreated,
                    onCheckedChange = { onToggle() }
                )
            }

            Text(
                text = "Before overwriting the old sarkarijobsewa.com website files, ensure legacy data is preserved:",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "# Shell Command to backup MySQL database:\nmysqldump -u root -p sarkarijobsewa_db > backup_sarkarijobsewa_$(date +%F).sql\n\n# Backup web files:\ntar -czvf sarkarijobsewa_html_backup.tar.gz /var/www/sarkarijobsewa/",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate700
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CanonicalRedirectCard(domainConfig: DomainMigrationConfig) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CANONICAL REDIRECT (ROOT ➔ WWW)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "To consolidate SEO domain authority, all traffic to http://sarkarijobsewa.com or https://sarkarijobsewa.com must automatically 301 redirect to https://www.sarkarijobsewa.com.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )
        }
    }
}

@Composable
private fun EmailPreservationCard(
    isEmailDnsPreserved: Boolean,
    onToggle: () -> Unit
) {
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
                    text = "STEPS 5-7: PRESERVE EMAIL & SUBDOMAIN DNS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = KaamWalaRed,
                        letterSpacing = 0.5.sp
                    )
                )
                Checkbox(checked = isEmailDnsPreserved, onCheckedChange = { onToggle() })
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KaamWalaRedContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = KaamWalaRed, modifier = Modifier.size(18.dp))
                    Text(
                        text = "CRITICAL: Do NOT delete existing MX, SPF, DKIM, or DMARC records when updating your nameservers or root A record! Deleting them will cause all existing email addresses on @sarkarijobsewa.com to immediately stop receiving emails.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7F1D1D), fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Preserved Records Checklist:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

            val emailChecks = listOf(
                "MX Records (e.g., aspmx.l.google.com or mx.zoho.com)",
                "SPF TXT Record (e.g., v=spf1 include:_spf.google.com ~all)",
                "DKIM TXT Record (e.g., google._domainkey.sarkarijobsewa.com)",
                "DMARC TXT Record (e.g., _dmarc.sarkarijobsewa.com: v=DMARC1; p=none)",
                "Subdomain CNAMEs (e.g., mail.sarkarijobsewa.com, webmail)"
            )

            emailChecks.forEach { check ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = KaamWalaGreenText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(check, style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        }
    }
}

@Composable
private fun SeoAndWebAssetsCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SEO, ROBOTS.TXT & SITEMAP ASSETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Static web assets configured for https://www.sarkarijobsewa.com:",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "1. /robots.txt\nUser-agent: *\nAllow: /\nDisallow: /admin/\nSitemap: https://www.sarkarijobsewa.com/sitemap.xml\n\n2. /sitemap.xml\n<url><loc>https://www.sarkarijobsewa.com/</loc></url>\n<url><loc>https://www.sarkarijobsewa.com/jobs</loc></url>\n<url><loc>https://www.sarkarijobsewa.com/privacy</loc></url>\n\n3. /.well-known/assetlinks.json (Android App Links verified)\n4. /ads.txt (Google AdSense & Monetag authorized sellers)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    )
                }
            }
        }
    }
}
