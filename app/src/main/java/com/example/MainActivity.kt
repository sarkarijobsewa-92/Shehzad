package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChatRole
import com.example.model.PaymentStatus
import com.example.repository.KaamWalaRepository
import com.example.ui.components.TasklyHeader
import com.example.ui.screens.*
import com.example.ui.theme.KaamWalaNavy
import com.example.ui.theme.KaamWalaOrangePrimary
import com.example.ui.theme.MyApplicationTheme

enum class AppNavDestination(val label: String, val icon: ImageVector) {
    JOBS("Jobs", Icons.Default.Work),
    POST_TASK("Post", Icons.Default.AddCircle),
    PAYMENTS("Payments", Icons.Default.Payments),
    CHAT("Chat", Icons.Default.Chat),
    SUPPORT("Support", Icons.Default.SupportAgent),
    DOMAIN("Domain", Icons.Default.Public),
    ADMIN("Admin & Ads", Icons.Default.AdminPanelSettings)
}

class MainActivity : ComponentActivity() {

    private val repository = KaamWalaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialNav = resolveInitialNavigation(intent)

        setContent {
            MyApplicationTheme {
                TasklyApp(repository = repository, initialNav = initialNav)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun resolveInitialNavigation(intent: Intent?): AppNavDestination {
        val data: Uri? = intent?.data
        if (data != null && data.host?.contains("sarkarijobsewa.com") == true) {
            val path = data.path ?: ""
            return when {
                path.startsWith("/payments") -> AppNavDestination.PAYMENTS
                path.startsWith("/post") -> AppNavDestination.POST_TASK
                path.startsWith("/chat") -> AppNavDestination.CHAT
                path.startsWith("/admin") -> AppNavDestination.ADMIN
                path.startsWith("/domain") -> AppNavDestination.DOMAIN
                else -> AppNavDestination.JOBS
            }
        }
        return AppNavDestination.JOBS
    }
}

@Composable
fun TasklyApp(
    repository: KaamWalaRepository,
    initialNav: AppNavDestination
) {
    var currentDestination by remember { mutableStateOf(initialNav) }

    val jobs by repository.jobs.collectAsStateWithLifecycle()
    val ownerProfile by repository.ownerProfile.collectAsStateWithLifecycle()
    val workerProfile by repository.workerProfile.collectAsStateWithLifecycle()
    val filterByMySkills by repository.filterByMySkills.collectAsStateWithLifecycle()
    val payments by repository.payments.collectAsStateWithLifecycle()
    val chatMessages by repository.chatMessages.collectAsStateWithLifecycle()
    val adSettings by repository.adSettings.collectAsStateWithLifecycle()
    val consentPreferences by repository.consentPreferences.collectAsStateWithLifecycle()
    val domainConfig by repository.domainConfig.collectAsStateWithLifecycle()
    val selectedCategory by repository.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by repository.searchQuery.collectAsStateWithLifecycle()

    var showConsentDialog by remember { mutableStateOf(consentPreferences.isPromptShown) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("taskly_main_scaffold"),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TasklyHeader(
                    onAdminClick = { currentDestination = AppNavDestination.ADMIN },
                    onSupportClick = { currentDestination = AppNavDestination.SUPPORT }
                )
                // ALWAYS AN AD PLACEMENT UNDER LOGO BAR (Visible across each page)
                com.example.ui.components.KaamWalaAdSlot(
                    slotConfig = adSettings.headerSlot,
                    globalAdsEnabled = adSettings.globalAdsEnabled,
                    adSettings = adSettings,
                    consentPreferences = consentPreferences,
                    minHeight = 60.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Filter out DOMAIN to hide old/mismatched website from navigation tabs
                val activeTabs = AppNavDestination.values().filter { it != AppNavDestination.DOMAIN }
                activeTabs.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontSize = 10.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KaamWalaOrangePrimary,
                            selectedTextColor = KaamWalaOrangePrimary,
                            indicatorColor = Color(0xFFFFF1E6),
                            unselectedIconColor = KaamWalaNavy.copy(alpha = 0.6f),
                            unselectedTextColor = KaamWalaNavy.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentDestination != AppNavDestination.JOBS) {
                BackHandler {
                    currentDestination = AppNavDestination.JOBS
                }
            }

            when (currentDestination) {
                AppNavDestination.JOBS -> {
                    JobFeedScreen(
                        jobs = jobs,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        workerProfile = workerProfile,
                        ownerProfile = ownerProfile,
                        filterByMySkills = filterByMySkills,
                        onCategorySelected = { repository.setCategory(it) },
                        onSearchChanged = { repository.setSearchQuery(it) },
                        onToggleFilterByMySkills = { repository.toggleFilterByMySkills(it) },
                        onUpdateWorkerSkills = { repository.updateWorkerSkills(it) },
                        onChooseWork = { jobId -> repository.chooseWork(jobId) },
                        onMarkWorkDone = { jobId, paymentType -> repository.markWorkDone(jobId, paymentType) },
                        onAddMoneyWorker = { amount -> repository.addMoneyToWorkerWallet(amount) },
                        onAddMoneyOwner = { amount -> repository.addMoneyToOwnerWallet(amount) },
                        onPayDuesClick = { amount ->
                            currentDestination = AppNavDestination.PAYMENTS
                        },
                        onOpenChatClick = { job ->
                            currentDestination = AppNavDestination.CHAT
                        },
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        onPostTaskClick = { currentDestination = AppNavDestination.POST_TASK }
                    )
                }
                AppNavDestination.POST_TASK -> {
                    PostJobScreen(
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        onJobCreated = { title, desc, cat, customCat, loc, city, budget, wage, empName ->
                            repository.addJob(title, desc, cat, customCat, loc, city, budget, wage, empName)
                        },
                        onNavigateToPayments = { currentDestination = AppNavDestination.PAYMENTS }
                    )
                }
                AppNavDestination.PAYMENTS -> {
                    PaymentsScreen(
                        payments = payments,
                        workerProfile = workerProfile,
                        ownerProfile = ownerProfile,
                        jobs = jobs,
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        onSubmitUtr = { id: String, utr: String, upi: String -> repository.submitUtrReference(id, utr, upi) },
                        onStatusChange = { id: String, status: PaymentStatus, note: String -> repository.updatePaymentStatus(id, status, note) },
                        onProcessCashPayment = { id: String -> repository.processCashPayment(id) },
                        onPayCommissionDues = { amount: Double -> repository.payWorkerCommissionDues(amount) },
                        onReleaseOnlinePayout = { id: String -> repository.releaseWorkerOnlinePayout(id) },
                        onMarkWorkDone = { jobId, paymentType -> repository.markWorkDone(jobId, paymentType) },
                        onAddMoneyOwner = { amount -> repository.addMoneyToOwnerWallet(amount) },
                        onAddMoneyWorker = { amount -> repository.addMoneyToWorkerWallet(amount) }
                    )
                }
                AppNavDestination.CHAT -> {
                    ChatScreen(
                        messages = chatMessages,
                        onSendMessage = { jId, jTitle, role, name, text ->
                            repository.sendChatMessage(jId, jTitle, role, name, text)
                        },
                        adSettings = adSettings,
                        consentPreferences = consentPreferences
                    )
                }
                AppNavDestination.SUPPORT -> {
                    HelpPrivacyScreen(
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        onUpdateConsent = { personalized, analytics ->
                            repository.updateConsent(personalized, analytics)
                        }
                    )
                }
                AppNavDestination.DOMAIN -> {
                    DomainMigrationScreen(
                        domainConfig = domainConfig,
                        onToggleChecklist = { step -> repository.toggleDomainChecklist(step) },
                        adSettings = adSettings,
                        consentPreferences = consentPreferences
                    )
                }
                AppNavDestination.ADMIN -> {
                    AdminAdSettingsScreen(
                        adSettings = adSettings,
                        consentPreferences = consentPreferences,
                        onUpdateGlobalAds = { repository.updateGlobalAds(it) },
                        onUpdateSlotConfig = { slot, enabled, provider, unitId ->
                            repository.updateSlotConfig(slot, enabled, provider, unitId)
                        },
                        onUpdateIdentifiers = { pubId, monetagId, freq ->
                            repository.updateAdIdentifiers(pubId, monetagId, freq)
                        },
                        onTogglePreviewMode = { repository.togglePreviewMode(it) }
                    )
                }
            }
        }
    }

    // DPDP Act Consent Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateConsent(personalizedAds = true, analytics = true)
                        showConsentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                    modifier = Modifier.testTag("consent_accept_all_btn")
                ) {
                    Text("Accept & Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repository.updateConsent(personalizedAds = false, analytics = false)
                        showConsentDialog = false
                    }
                ) {
                    Text("Essential Only (Privacy Mode)")
                }
            },
            title = {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = KaamWalaNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data Privacy & Consent (DPDP Act)")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Taskly protects worker and owner privacy. Personal phone numbers and vulgar language are strictly prohibited and shielded.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can choose personalized ads or stick to privacy-safe essential services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }
        )
    }
}
