package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.components.CommissionBreakdownCard
import com.example.ui.components.JobStatusBadge
import com.example.ui.components.KaamWalaAdSlot
import com.example.ui.theme.*

@Composable
fun JobFeedScreen(
    jobs: List<Job>,
    selectedCategory: JobCategory?,
    searchQuery: String,
    workerProfile: WorkerProfile,
    ownerProfile: OwnerProfile = OwnerProfile(),
    filterByMySkills: Boolean,
    onCategorySelected: (JobCategory?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleFilterByMySkills: (Boolean) -> Unit,
    onUpdateWorkerSkills: (Set<JobCategory>) -> Unit,
    onChooseWork: (String) -> Unit = {},
    onMarkWorkDone: (String, WorkDonePaymentType) -> Unit = { _, _ -> },
    onAddMoneyWorker: (Double) -> Unit = {},
    onAddMoneyOwner: (Double) -> Unit = {},
    onPayDuesClick: (Double) -> Unit,
    onOpenChatClick: (Job) -> Unit,
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    onPostTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedJobForDetails by remember { mutableStateOf<Job?>(null) }
    var applicationSubmittedJobTitle by remember { mutableStateOf<String?>(null) }
    var showSkillsManagerModal by remember { mutableStateOf(false) }
    var showSuspendedWarningDialog by remember { mutableStateOf(false) }
    var showAddMoneyModal by remember { mutableStateOf(false) }
    var jobForWorkDoneDialog by remember { mutableStateOf<Job?>(null) }
    var confirmChooseJob by remember { mutableStateOf<Job?>(null) }
    var lockReasonDialogText by remember { mutableStateOf<String?>(null) }

    // Active job object if currently in progress
    val activeJob = remember(jobs, workerProfile.activeJobId) {
        workerProfile.activeJobId?.let { id -> jobs.firstOrNull { it.id == id } }
    }

    // Filter jobs based on worker skills and category
    val filteredJobs = remember(jobs, selectedCategory, searchQuery, filterByMySkills, workerProfile.selectedSkills) {
        jobs.filter { job ->
            val matchesSkill = !filterByMySkills || workerProfile.selectedSkills.contains(job.category)
            val matchesCategory = selectedCategory == null || job.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    job.title.contains(searchQuery, ignoreCase = true) ||
                    job.categoryDisplayName.contains(searchQuery, ignoreCase = true) ||
                    job.city.contains(searchQuery, ignoreCase = true) ||
                    job.location.contains(searchQuery, ignoreCase = true) ||
                    job.description.contains(searchQuery, ignoreCase = true)
            matchesSkill && matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("job_feed_list"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Section with Taskly Branding & New Logo
        item {
            TasklyHeroSection(
                onPostTaskClick = onPostTaskClick,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Active Task In Progress Banner (Worker cannot choose next work until owner clicks Work Done)
        if (workerProfile.hasActiveJob) {
            item {
                ActiveTaskInProgressBanner(
                    activeJob = activeJob,
                    activeJobId = workerProfile.activeJobId ?: "",
                    onChatClick = { activeJob?.let { onOpenChatClick(it) } },
                    onWorkDoneClick = {
                        activeJob?.let { jobForWorkDoneDialog = it }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Worker Balance & Negative Commission Khata Banner
        item {
            WorkerCommissionBalanceBanner(
                profile = workerProfile,
                onPayDuesClick = { showAddMoneyModal = true },
                onManageSkillsClick = { showSkillsManagerModal = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // AD SLOT 1: HOMEPAGE ADVERTISEMENT
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                KaamWalaAdSlot(
                    slotConfig = adSettings.homepageSlot,
                    globalAdsEnabled = adSettings.globalAdsEnabled,
                    adSettings = adSettings,
                    consentPreferences = consentPreferences,
                    minHeight = 90.dp
                )
            }
        }

        // Worker Skills Filter Bar
        item {
            WorkerSkillFilterBar(
                workerProfile = workerProfile,
                filterByMySkills = filterByMySkills,
                onToggleMySkills = onToggleFilterByMySkills,
                onManageSkillsClick = { showSkillsManagerModal = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Search & Filter Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search tasks (e.g., Electrician, Gardening, Delivery)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate500)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Slate500)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = KaamWalaOrangePrimary,
                        unfocusedBorderColor = Slate300
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelected(null) },
                            label = { Text("All Categories (${filteredJobs.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KaamWalaNavy,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_all")
                        )
                    }
                    items(JobCategory.values()) { category ->
                        val isWorkerSkill = workerProfile.selectedSkills.contains(category)
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                onCategorySelected(if (selectedCategory == category) null else category)
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isWorkerSkill) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                    Text(category.displayName)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KaamWalaOrangePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASK DISCOVERY FEED (${filteredJobs.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Slate500
                    )
                    Text(
                        text = "Ad after every 3 jobs • Min ₹100",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = KaamWalaOrangeDark
                        )
                    )
                }
            }
        }

        // Job Feed with AD SLOT 2 inserted after every 3 items (Requirement 4)
        if (filteredJobs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterByMySkills) "No jobs match your selected skills" else "No tasks found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add more skills to your profile or toggle off the 'My Skills' filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showSkillsManagerModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary)
                        ) {
                            Text("Edit My Skills (${workerProfile.selectedSkills.size} active)")
                        }
                    }
                }
            }
        } else {
            // Requirement 4: Ad placement after 3 jobs
            val frequency = 3

            filteredJobs.forEachIndexed { index, job ->
                item(key = job.id) {
                    JobCardItem(
                        job = job,
                        workerProfile = workerProfile,
                        onClick = { selectedJobForDetails = job },
                        onChooseWork = {
                            if (workerProfile.isBlockedByNegativeBalance) {
                                lockReasonDialogText = "Your wallet balance is -₹${"%.0f".format(workerProfile.outstandingDue)} (reached -₹199 threshold). You cannot choose new work until you add money to make your wallet balance ₹0."
                            } else if (workerProfile.hasActiveJob) {
                                lockReasonDialogText = "You already have an active job in progress ('${activeJob?.title ?: workerProfile.activeJobId}'). You cannot choose the next work until the owner clicks the 'Work Done' button."
                            } else {
                                confirmChooseJob = job
                            }
                        },
                        onOpenChat = { onOpenChatClick(job) },
                        onWorkDone = {
                            jobForWorkDoneDialog = job
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // AD SLOT 2: Inserted after every 3 job listings
                if ((index + 1) % frequency == 0 && index < filteredJobs.lastIndex) {
                    item(key = "ad_slot_feed_${index}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            KaamWalaAdSlot(
                                slotConfig = adSettings.jobFeedSlot,
                                globalAdsEnabled = adSettings.globalAdsEnabled,
                                adSettings = adSettings,
                                consentPreferences = consentPreferences,
                                minHeight = 85.dp
                            )
                        }
                    }
                }
            }
        }
    }

    // Worker Skills Management Dialog
    if (showSkillsManagerModal) {
        WorkerSkillsSelectionModal(
            currentSkills = workerProfile.selectedSkills,
            onDismiss = { showSkillsManagerModal = false },
            onSave = { updatedSkills ->
                onUpdateWorkerSkills(updatedSkills)
                showSkillsManagerModal = false
            }
        )
    }

    // Work Lock Reason Dialog (Explains sequential lock or negative debt limit)
    lockReasonDialogText?.let { reason ->
        AlertDialog(
            onDismissRequest = { lockReasonDialogText = null },
            confirmButton = {
                if (workerProfile.isBlockedByNegativeBalance) {
                    Button(
                        onClick = {
                            lockReasonDialogText = null
                            showAddMoneyModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary)
                    ) {
                        Text("Add Money to Make ₹0")
                    }
                } else if (workerProfile.hasActiveJob) {
                    Button(
                        onClick = {
                            lockReasonDialogText = null
                            activeJob?.let { jobForWorkDoneDialog = it }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                    ) {
                        Text("Owner: Confirm Work Done")
                    }
                } else {
                    Button(
                        onClick = { lockReasonDialogText = null },
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                    ) {
                        Text("Got it")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { lockReasonDialogText = null }) {
                    Text("Close")
                }
            },
            icon = {
                Icon(
                    imageVector = if (workerProfile.isBlockedByNegativeBalance) Icons.Default.Block else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (workerProfile.isBlockedByNegativeBalance) KaamWalaRed else KaamWalaOrangeDark,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (workerProfile.isBlockedByNegativeBalance) "Work Blocked (Debt Limit Reached)" else "Task Sequential Lock",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (workerProfile.isBlockedByNegativeBalance) {
                        Text(
                            text = "Platform Rule: Negative balance cannot reach -₹200. If balance reaches -₹199, you cannot choose new work until you top up your wallet and restore balance to ₹0.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = KaamWalaRed)
                        )
                    } else if (workerProfile.hasActiveJob) {
                        Text(
                            text = "Platform Rule: Worker cannot choose the next work until the owner clicks the 'Work Done' button.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = KaamWalaOrangeDark)
                        )
                    }
                }
            }
        )
    }

    // Confirm Choose Work Dialog
    confirmChooseJob?.let { job ->
        AlertDialog(
            onDismissRequest = { confirmChooseJob = null },
            confirmButton = {
                Button(
                    onClick = {
                        onChooseWork(job.id)
                        confirmChooseJob = null
                        applicationSubmittedJobTitle = job.title
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                    modifier = Modifier.testTag("confirm_choose_work_btn")
                ) {
                    Text("Accept & Start Work", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmChooseJob = null }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(Icons.Default.Handshake, contentDescription = null, tint = KaamWalaOrangePrimary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Choose Work: ${job.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Gross Budget: ₹${job.customerBudget.toInt()} (Net Payout: ₹${job.commission.workerNetPayout.toInt()} after 10% Taskly fee).",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Important: Once you choose this task, it will become your active job. You cannot choose the next work until the owner clicks 'Work Done'.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                    )
                }
            }
        )
    }

    // Owner "Work Done" Confirmation Dialog
    jobForWorkDoneDialog?.let { job ->
        WorkDoneModalDialog(
            job = job,
            ownerProfile = ownerProfile,
            workerProfile = workerProfile,
            onDismiss = { jobForWorkDoneDialog = null },
            onConfirmWorkDone = { paymentType ->
                onMarkWorkDone(job.id, paymentType)
                jobForWorkDoneDialog = null
            },
            onAddOwnerMoneyClick = {
                onAddMoneyOwner(1000.0)
            }
        )
    }

    // Add Money to Worker Wallet Dialog
    if (showAddMoneyModal) {
        AddWorkerMoneyModal(
            currentBalance = workerProfile.walletBalance,
            outstandingDue = workerProfile.outstandingDue,
            onDismiss = { showAddMoneyModal = false },
            onAddMoney = { amount ->
                onAddMoneyWorker(amount)
                showAddMoneyModal = false
            }
        )
    }

    // Task Details & Application Modal
    selectedJobForDetails?.let { job ->
        JobDetailModal(
            job = job,
            workerProfile = workerProfile,
            onDismiss = { selectedJobForDetails = null },
            onChooseWork = {
                selectedJobForDetails = null
                if (workerProfile.isBlockedByNegativeBalance) {
                    lockReasonDialogText = "Your wallet balance is -₹${"%.0f".format(workerProfile.outstandingDue)} (reached -₹199 threshold). You cannot choose new work until you add money to make your wallet balance ₹0."
                } else if (workerProfile.hasActiveJob) {
                    lockReasonDialogText = "You already have an active job in progress ('${activeJob?.title ?: workerProfile.activeJobId}'). You cannot choose the next work until the owner clicks the 'Work Done' button."
                } else {
                    confirmChooseJob = job
                }
            },
            onOpenChat = {
                selectedJobForDetails = null
                onOpenChatClick(job)
            }
        )
    }

    // Success Application Alert
    applicationSubmittedJobTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { applicationSubmittedJobTitle = null },
            confirmButton = {
                Button(
                    onClick = { applicationSubmittedJobTitle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                ) {
                    Text("OK, Track Active Task")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KaamWalaGreenText)
                    Text("Work Assigned & Active!")
                }
            },
            text = {
                Column {
                    Text(
                        text = "You chose task: $title",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This is now your active task. Remember: you cannot choose the next work until the owner clicks the 'Work Done' button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        )
    }
}

@Composable
private fun WorkerCommissionBalanceBanner(
    profile: WorkerProfile,
    onPayDuesClick: () -> Unit,
    onManageSkillsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSuspended = profile.isSuspendedForDues
    val cardColor = if (isSuspended) KaamWalaRedContainer else Slate100
    val borderColor = if (isSuspended) KaamWalaRed else Slate300

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (isSuspended) Icons.Default.Block else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isSuspended) KaamWalaRed else KaamWalaNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "WORKER WALLET & COMMISSION KHATA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSuspended) KaamWalaRed else Slate700,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSuspended) KaamWalaRed else KaamWalaNavy
                ) {
                    Text(
                        text = if (isSuspended) "SUSPENDED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (profile.commissionBalance < 0) "Cash Commission Due: ₹${"%.0f".format(profile.outstandingDue)}" else "Balance: ₹0",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isSuspended) KaamWalaRed else Slate900
                        )
                    )
                    Text(
                        text = "Max negative limit: ₹200.00 (Current: ₹${"%.0f".format(profile.commissionBalance)})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }

                if (profile.commissionBalance < 0) {
                    Button(
                        onClick = onPayDuesClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSuspended) KaamWalaRed else KaamWalaOrangePrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Pay Due (UPI)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isSuspended) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ You have reached the maximum ₹200 negative balance limit from cash tasks. You cannot accept or apply for jobs until you clear this balance.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F1D1D)
                    )
                )
            }
        }
    }
}

@Composable
private fun WorkerSkillFilterBar(
    workerProfile: WorkerProfile,
    filterByMySkills: Boolean,
    onToggleMySkills: (Boolean) -> Unit,
    onManageSkillsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = KaamWalaOrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Show Jobs Matching My Skills (${workerProfile.selectedSkills.size})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "Worker chooses what tasks they can do (including Custom/Other)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Slate500
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = filterByMySkills,
                    onCheckedChange = onToggleMySkills,
                    modifier = Modifier.testTag("filter_my_skills_switch")
                )
                IconButton(onClick = onManageSkillsClick) {
                    Icon(Icons.Default.Tune, contentDescription = "Edit Skills", tint = KaamWalaNavy)
                }
            }
        }
    }
}

@Composable
fun WorkerSkillsSelectionModal(
    currentSkills: Set<JobCategory>,
    onDismiss: () -> Unit,
    onSave: (Set<JobCategory>) -> Unit
) {
    var selected by remember { mutableStateOf(currentSkills) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                modifier = Modifier.testTag("save_skills_btn")
            ) {
                Text("Save Skills (${selected.size} selected)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Column {
                Text("Choose What Work You Can Do", fontWeight = FontWeight.Bold)
                Text(
                    text = "Select all skills you are capable and willing to perform.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                JobCategory.values().forEach { category ->
                    val isChecked = selected.contains(category)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (isChecked) selected - category else selected + category
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                selected = if (check) selected + category else selected - category
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChecked) KaamWalaOrangeDark else Slate900
                            )
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TasklyHeroSection(
    onPostTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KaamWalaNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = KaamWalaOrangePrimary
                    ) {
                        Text(
                            text = "OFFICIAL TASKLY MARKETPLACE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Stop being idle.\nStart earning.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 28.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose your work. Guaranteed min ₹100 pay. Contacts & safety protected.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate300)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Image(
                    painter = painterResource(id = R.drawable.taskly_logo),
                    contentDescription = "Taskly Hero Graphic",
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPostTaskClick,
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_post_task_btn")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hire Workers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {},
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Zero Vulgarity", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ActiveTaskInProgressBanner(
    activeJob: Job?,
    activeJobId: String,
    onChatClick: () -> Unit,
    onWorkDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KaamWalaOrangeDark)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_task_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = KaamWalaOrangeDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ACTIVE TASK IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = KaamWalaOrangeDark,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = KaamWalaOrangeDark
                ) {
                    Text(
                        text = "TASK LOCK ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activeJob?.title ?: "Current Active Task ($activeJobId)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = Slate900
            )

            activeJob?.let { j ->
                Text(
                    text = "Owner: ${j.employerName} • Budget: ₹${j.customerBudget.toInt()} (${j.location}, ${j.city})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                    Text(
                        text = "Rule: Worker cannot choose the next work until the owner clicks the 'Work Done' button.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChatClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat Owner", fontSize = 11.sp)
                }

                Button(
                    onClick = onWorkDoneClick,
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangeDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("owner_work_done_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Owner: Work Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JobCardItem(
    job: Job,
    workerProfile: WorkerProfile,
    onClick: () -> Unit,
    onChooseWork: () -> Unit,
    onOpenChat: () -> Unit,
    onWorkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentWorkerActiveJob = workerProfile.activeJobId == job.id

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("job_card_${job.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isCurrentWorkerActiveJob) KaamWalaOrangePrimary else Slate200
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Category Badge & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = KaamWalaNavyContainer
                    ) {
                        Text(
                            text = job.categoryDisplayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = KaamWalaNavy
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Status Indicator ('Open', 'In Progress', 'Completed')
                    JobStatusBadge(
                        status = job.status,
                        isCurrentWorkerActiveJob = isCurrentWorkerActiveJob
                    )
                }

                if (job.isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = KaamWalaRedContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = KaamWalaRed, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Urgent",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KaamWalaRed
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = job.postedTimeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Job Title
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate900
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Location & Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${job.location}, ${job.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                    Text(
                        text = job.durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transparent Earnings Highlight
            val commission = job.commission
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Customer Budget: ₹${"%.0f".format(job.customerBudget)} (Min ₹100)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Worker Take-Home: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Slate700
                            )
                            Text(
                                text = "₹${"%.0f".format(commission.workerNetPayout)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = KaamWalaGreenText
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = KaamWalaOrangeContainer
                    ) {
                        Text(
                            text = "10% Taskly Fee",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaOrangeDark
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Privacy Protected Contact Bar & Choose Work Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = KaamWalaNavy, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Owner: ${job.employerName} • Protected",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenChat,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "In-App Chat", tint = KaamWalaNavy)
                    }

                    when (job.status) {
                        JobStatus.COMPLETED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KaamWalaGreenContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = KaamWalaGreenText, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Completed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaamWalaGreenText)
                                }
                            }
                        }
                        JobStatus.IN_PROGRESS -> {
                            if (isCurrentWorkerActiveJob) {
                                Button(
                                    onClick = onWorkDone,
                                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangeDark),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("card_work_done_${job.id}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Work Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onClick,
                                    enabled = false,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("In Progress", fontSize = 11.sp)
                                }
                            }
                        }
                        JobStatus.OPEN -> {
                            Button(
                                onClick = onChooseWork,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (workerProfile.canAcceptNewWork) KaamWalaNavy else Slate500
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("choose_work_${job.id}")
                            ) {
                                if (!workerProfile.canAcceptNewWork) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Choose Work", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobDetailModal(
    job: Job,
    workerProfile: WorkerProfile,
    onDismiss: () -> Unit,
    onChooseWork: () -> Unit,
    onOpenChat: () -> Unit
) {
    val commission = job.commission

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (job.status == JobStatus.COMPLETED) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Slate500)) {
                    Text("Task Completed")
                }
            } else if (job.status == JobStatus.IN_PROGRESS) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangeDark)) {
                    Text("In Progress")
                }
            } else {
                Button(
                    onClick = onChooseWork,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (workerProfile.canAcceptNewWork) KaamWalaOrangePrimary else Slate500
                    ),
                    modifier = Modifier.testTag("detail_choose_work_btn")
                ) {
                    if (!workerProfile.canAcceptNewWork) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Choose This Work", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpenChat) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("In-App Chat")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Slate700)
                }
            }
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = KaamWalaNavyContainer
                    ) {
                        Text(
                            text = job.categoryDisplayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaNavy
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    JobStatusBadge(
                        status = job.status,
                        isCurrentWorkerActiveJob = workerProfile.activeJobId == job.id
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(job.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Commission breakdown card
                CommissionBreakdownCard(
                    customerBudget = commission.customerBudget,
                    commissionAmount = commission.platformCommission,
                    workerPayout = commission.workerNetPayout,
                    ratePercent = 10.0
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sequential work lock rule
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Sequential Rule: Worker cannot choose the next work until the owner clicks the 'Work Done' button.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF78350F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wallet & negative limit notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = KaamWalaNavy, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Wallet Rule: Cash payments deduct 10% from worker wallet. Balance cannot drop below -₹200. Reaching -₹199 locks new work until wallet is restored to ₹0.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = KaamWalaNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Support email guidance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Slate500, modifier = Modifier.size(13.dp))
                    Text(
                        text = "Queries on this task? Support: $TASKLY_SUPPORT_EMAIL",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Slate500
                    )
                }
            }
        }
    )
}

/**
 * Owner "Work Done" Confirmation Modal Dialog
 */
@Composable
fun WorkDoneModalDialog(
    job: Job,
    ownerProfile: OwnerProfile,
    workerProfile: WorkerProfile,
    onDismiss: () -> Unit,
    onConfirmWorkDone: (WorkDonePaymentType) -> Unit,
    onAddOwnerMoneyClick: () -> Unit
) {
    var selectedPaymentType by remember { mutableStateOf(WorkDonePaymentType.OWNER_WALLET) }
    val grossBudget = job.customerBudget
    val commission = job.commission.platformCommission
    val workerNet = job.commission.workerNetPayout
    val hasEnoughOwnerBalance = ownerProfile.walletBalance >= grossBudget

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirmWorkDone(selectedPaymentType) },
                enabled = selectedPaymentType == WorkDonePaymentType.CASH || hasEnoughOwnerBalance,
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangeDark),
                modifier = Modifier.testTag("confirm_work_done_action_btn")
            ) {
                Text("Confirm Work Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        icon = {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KaamWalaOrangeDark, modifier = Modifier.size(36.dp))
        },
        title = { Text("Owner Action: Confirm Work Done", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Confirm completion for: ${job.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Total Job Pay: ₹${grossBudget.toInt()} (Worker: ₹${workerNet.toInt()} • 10% Taskly Fee: ₹${commission.toInt()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("How are you paying the worker?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))

                Spacer(modifier = Modifier.height(6.dp))

                // Option 1: Owner Wallet
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPaymentType == WorkDonePaymentType.OWNER_WALLET) Color(0xFFFFF7ED) else Slate50,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedPaymentType == WorkDonePaymentType.OWNER_WALLET) KaamWalaOrangePrimary else Slate300
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPaymentType = WorkDonePaymentType.OWNER_WALLET }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentType == WorkDonePaymentType.OWNER_WALLET,
                            onClick = { selectedPaymentType = WorkDonePaymentType.OWNER_WALLET }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pay from Owner Wallet", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "₹${grossBudget.toInt()} debited from Owner Wallet. ₹${workerNet.toInt()} credited to Worker Wallet (10% fee ₹${commission.toInt()} retained).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Slate700
                            )
                            Text(
                                text = "Your Owner Wallet Balance: ₹${ownerProfile.walletBalance.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasEnoughOwnerBalance) KaamWalaGreenText else KaamWalaRed
                                )
                            )
                        }
                    }
                }

                if (selectedPaymentType == WorkDonePaymentType.OWNER_WALLET && !hasEnoughOwnerBalance) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Insufficient balance (need ₹${grossBudget.toInt()})",
                            style = MaterialTheme.typography.bodySmall.copy(color = KaamWalaRed, fontSize = 11.sp)
                        )
                        TextButton(onClick = onAddOwnerMoneyClick) {
                            Text("+ Add ₹1000", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option 2: Cash on-site
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPaymentType == WorkDonePaymentType.CASH) Color(0xFFFFF7ED) else Slate50,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedPaymentType == WorkDonePaymentType.CASH) KaamWalaOrangePrimary else Slate300
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPaymentType = WorkDonePaymentType.CASH }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPaymentType == WorkDonePaymentType.CASH,
                            onClick = { selectedPaymentType = WorkDonePaymentType.CASH }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Paid Cash to Worker on-site", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "Owner paid ₹${grossBudget.toInt()} cash directly to worker. 10% commission (₹${commission.toInt()}) will be deducted from worker wallet (negative balance if ₹0).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "✅ Clicking 'Confirm Work Done' releases the sequential lock so the worker can choose their next work.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = KaamWalaNavy)
                )
            }
        }
    )
}

/**
 * Add Money to Worker Wallet Modal (Top-up & debt clearance)
 */
@Composable
fun AddWorkerMoneyModal(
    currentBalance: Double,
    outstandingDue: Double,
    onDismiss: () -> Unit,
    onAddMoney: (Double) -> Unit
) {
    val context = LocalContext.current
    val merchantUpi = "8360608312@mbk"
    var inputAmount by remember {
        mutableStateOf(if (outstandingDue > 0) outstandingDue.toInt().toString() else "200")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputAmount.toDoubleOrNull() ?: 0.0
                    if (parsed > 0) {
                        onAddMoney(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                modifier = Modifier.testTag("worker_topup_confirm_btn")
            ) {
                Text("Add Money via UPI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        icon = {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = KaamWalaNavy, modifier = Modifier.size(32.dp))
        },
        title = { Text("Add Money to Worker Wallet", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Wallet Balance: ₹${"%.0f".format(currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (currentBalance < 0) KaamWalaRed else KaamWalaGreenText
                    )
                )
                if (currentBalance < 0) {
                    Text(
                        text = "Negative balance from cash commissions. Add at least ₹${"%.0f".format(outstandingDue)} to make balance ₹0 and unblock new tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Enter Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50.0, 100.0, 200.0).forEach { amt ->
                        SuggestionChip(
                            onClick = { inputAmount = amt.toInt().toString() },
                            label = { Text("+₹${amt.toInt()}") }
                        )
                    }
                    if (outstandingDue > 0) {
                        SuggestionChip(
                            onClick = { inputAmount = outstandingDue.toInt().toString() },
                            label = { Text("Exact ₹${outstandingDue.toInt()}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction Payment UPI Card (Shown only during active transaction)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = KaamWalaNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "COMMISSION PAYMENT UPI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaOrangeLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = merchantUpi,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Taskly Commission UPI", merchantUpi))
                                    Toast.makeText(context, "UPI ID copied: $merchantUpi", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_worker_upi_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = KaamWalaOrangeLight, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val amt = inputAmount.toDoubleOrNull() ?: 200.0
                                    val upiUri = Uri.parse("upi://pay?pa=$merchantUpi&pn=Taskly&am=$amt&cu=INR&tn=TasklyWorkerCommissionPay")
                                    val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                    context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No UPI app found. Please copy UPI ID $merchantUpi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch UPI App to Pay", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    )
}
