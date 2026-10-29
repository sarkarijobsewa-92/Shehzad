package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.CommissionBreakdownCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.theme.*

@Composable
fun PaymentsScreen(
    payments: List<PaymentRecord>,
    workerProfile: WorkerProfile,
    ownerProfile: OwnerProfile = OwnerProfile(),
    jobs: List<Job> = emptyList(),
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    onSubmitUtr: (paymentId: String, utr: String, customerUpi: String) -> Unit,
    onStatusChange: (paymentId: String, newStatus: PaymentStatus, note: String) -> Unit,
    onProcessCashPayment: (paymentId: String) -> Unit,
    onPayCommissionDues: (Double) -> Unit,
    onReleaseOnlinePayout: (paymentId: String) -> Unit,
    onMarkWorkDone: (jobId: String, paymentType: WorkDonePaymentType) -> Unit = { _, _ -> },
    onAddMoneyOwner: (Double) -> Unit = {},
    onAddMoneyWorker: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var paymentToSubmitUtr by remember { mutableStateOf<PaymentRecord?>(null) }
    var paymentToManage by remember { mutableStateOf<PaymentRecord?>(null) }
    var cashConfirmDialog by remember { mutableStateOf<PaymentRecord?>(null) }
    var showOwnerTopupModal by remember { mutableStateOf(false) }
    var showWorkerTopupModal by remember { mutableStateOf(false) }
    var jobForWorkDoneDialog by remember { mutableStateOf<Job?>(null) }

    val filteredPayments = remember(payments, selectedFilter) {
        if (selectedFilter == null) payments
        else payments.filter { it.status == selectedFilter }
    }

    val inProgressJobs = remember(jobs) {
        jobs.filter { it.status == JobStatus.IN_PROGRESS }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("payments_screen_list"),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Title
        item {
            Column {
                Text(
                    text = "Wallets & Transparent Escrow",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = KaamWalaNavy
                    )
                )
                Text(
                    text = "Owner Wallet • Worker Wallet • 10% Fee • Negative Limit -₹200 • 'Work Done' Gate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )
            }
        }

        // 1. OWNER WALLET CARD
        item {
            OwnerWalletCard(
                profile = ownerProfile,
                onAddMoney = { showOwnerTopupModal = true }
            )
        }

        // 2. WORKER WALLET & COMMISSION KHATA CARD
        item {
            WorkerCommissionKhataCard(
                profile = workerProfile,
                onPayDues = { showWorkerTopupModal = true }
            )
        }

        // 3. ACTIVE TASKS & "WORK DONE" SEQUENTIAL GATE
        if (inProgressJobs.isNotEmpty()) {
            item {
                ActiveJobsWorkDoneSection(
                    jobs = inProgressJobs,
                    onWorkDoneClick = { jobForWorkDoneDialog = it }
                )
            }
        }

        // Cash Payment & 10% Commission Deduction Explanation Card
        item {
            CashCommissionPolicyCard()
        }

        // Status Filter Chips
        item {
            Column {
                Text(
                    text = "LIFECYCLE STATUS FILTERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${payments.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KaamWalaNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(PaymentStatus.values()) { status ->
                        val count = payments.count { it.status == status }
                        FilterChip(
                            selected = selectedFilter == status,
                            onClick = { selectedFilter = if (selectedFilter == status) null else status },
                            label = { Text("${status.label} ($count)") }
                        )
                    }
                }
            }
        }

        // In-page Ad placement for Payments & Khata webpage
        item {
            com.example.ui.components.KaamWalaAdSlot(
                slotConfig = adSettings.contentSlot,
                globalAdsEnabled = adSettings.globalAdsEnabled,
                adSettings = adSettings,
                consentPreferences = consentPreferences,
                minHeight = 70.dp
            )
        }

        // Transaction List
        if (filteredPayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Slate500, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No payment transactions found in this filter.", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    }
                }
            }
        } else {
            items(filteredPayments, key = { it.id }) { record ->
                PaymentRecordCard(
                    record = record,
                    onSubmitUtrClick = { paymentToSubmitUtr = record },
                    onCashPaidClick = { cashConfirmDialog = record },
                    onReleasePayoutClick = { onReleaseOnlinePayout(record.id) },
                    onManageClick = { paymentToManage = record }
                )
            }
        }

        // Support queries for payments, settlements & escrows
        item {
            com.example.ui.components.SupportContactCard()
        }
    }

    // Cash Payment Confirmation Modal
    cashConfirmDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { cashConfirmDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        onProcessCashPayment(record.id)
                        cashConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                ) {
                    Text("Confirm Cash Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { cashConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(Icons.Default.Payments, contentDescription = null, tint = KaamWalaOrangePrimary, modifier = Modifier.size(32.dp))
            },
            title = { Text("Record Cash Payment from Owner") },
            text = {
                Column {
                    Text(
                        text = "Owner paid full amount of ₹${"%.0f".format(record.totalAmount)} directly in cash to the worker.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Platform Rule: The 10% commission (₹${"%.0f".format(record.platformCommission)}) will be added to the worker's negative balance and deducted from his next online payment.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = KaamWalaOrangeDark)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Maximum negative balance limit is ₹200. If negative balance reaches ₹200, the worker cannot take new jobs until paying dues.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                    )
                }
            }
        )
    }

    // UTR Submission Modal
    paymentToSubmitUtr?.let { record ->
        SubmitUtrModal(
            record = record,
            onDismiss = { paymentToSubmitUtr = null },
            onSubmit = { utr, upi ->
                onSubmitUtr(record.id, utr, upi)
                paymentToSubmitUtr = null
            }
        )
    }

    // Status Management Modal
    paymentToManage?.let { record ->
        ManagePaymentStatusModal(
            record = record,
            onDismiss = { paymentToManage = null },
            onUpdate = { newStatus, note ->
                onStatusChange(record.id, newStatus, note)
                paymentToManage = null
            }
        )
    }

    // Owner "Work Done" Confirmation Dialog
    jobForWorkDoneDialog?.let { job ->
        com.example.ui.screens.WorkDoneModalDialog(
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

    // Owner Top-up Modal
    if (showOwnerTopupModal) {
        OwnerTopupModal(
            currentBalance = ownerProfile.walletBalance,
            onDismiss = { showOwnerTopupModal = false },
            onAddMoney = { amount ->
                onAddMoneyOwner(amount)
                showOwnerTopupModal = false
            }
        )
    }

    // Worker Top-up / Clear Dues Modal
    if (showWorkerTopupModal) {
        com.example.ui.screens.AddWorkerMoneyModal(
            currentBalance = workerProfile.walletBalance,
            outstandingDue = workerProfile.outstandingDue,
            onDismiss = { showWorkerTopupModal = false },
            onAddMoney = { amount ->
                onAddMoneyWorker(amount)
                showWorkerTopupModal = false
            }
        )
    }
}

@Composable
private fun OwnerWalletCard(
    profile: OwnerProfile,
    onAddMoney: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate300)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_wallet_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KaamWalaNavy,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "OWNER WALLET (${profile.name})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaNavy,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "₹${"%.2f".format(profile.walletBalance)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                        )
                    }
                }

                Button(
                    onClick = onAddMoney,
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_money_owner_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Money", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 Rule: When owner pays worker online, 10% platform commission is automatically deducted, and 90% net earnings are immediately credited to worker's wallet.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate700,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveJobsWorkDoneSection(
    jobs: List<Job>,
    onWorkDoneClick: (Job) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(KaamWalaOrangeDark)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_jobs_work_done_section")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.PendingActions, contentDescription = null, tint = KaamWalaOrangeDark, modifier = Modifier.size(18.dp))
                    Text(
                        text = "TASKS IN PROGRESS (${jobs.size})",
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
                        text = "AWAITING 'WORK DONE'",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Worker cannot choose the next work until the owner clicks the 'Work Done' button below.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate700)
            )

            Spacer(modifier = Modifier.height(10.dp))

            jobs.forEach { job ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "Worker: ${job.assignedWorkerName ?: "Assigned Worker"} • Pay: ₹${job.customerBudget.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700
                            )
                        }

                        Button(
                            onClick = { onWorkDoneClick(job) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangeDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("payments_work_done_${job.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Work Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerCommissionKhataCard(
    profile: WorkerProfile,
    onPayDues: () -> Unit
) {
    val isBlocked = profile.isBlockedByNegativeBalance
    val cardColor = if (isBlocked) KaamWalaRedContainer else Color(0xFFEFF6FF)
    val borderColor = if (isBlocked) KaamWalaRed else Color(0xFFBFDBFE)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth().testTag("worker_wallet_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WORKER WALLET (${profile.name})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isBlocked) KaamWalaRed else KaamWalaNavy,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = if (profile.walletBalance < 0) "Cash Commission Due: -₹${"%.0f".format(profile.outstandingDue)}" else "Balance: ₹${"%.2f".format(profile.walletBalance)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isBlocked) KaamWalaRed else Slate900
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBlocked) KaamWalaRed else KaamWalaGreen
                ) {
                    Text(
                        text = if (isBlocked) "WORK BLOCKED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Debt Progress Indicator (0 to 200)
            val dueAmount = profile.outstandingDue.toFloat()
            val progress = (dueAmount / 200f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isBlocked) KaamWalaRed else KaamWalaOrangePrimary,
                trackColor = Slate200
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dues: ₹${"%.0f".format(dueAmount)} / Max -₹200.00 (Lock at -₹199)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = if (isBlocked) KaamWalaRed else Slate700
                )

                Button(
                    onClick = onPayDues,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) KaamWalaRed else KaamWalaNavy
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("worker_wallet_add_money_btn")
                ) {
                    Text(if (profile.walletBalance < 0) "Make ₹0 (Clear Dues)" else "Add Money", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isBlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "❌ WORK BLOCKED: Your wallet balance has reached -₹${"%.0f".format(profile.outstandingDue)} (-₹199 threshold reached). You cannot choose new work until you add money and make your wallet balance ₹0.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF7F1D1D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun OwnerTopupModal(
    currentBalance: Double,
    onDismiss: () -> Unit,
    onAddMoney: (Double) -> Unit
) {
    val context = LocalContext.current
    val merchantUpi = "8360608312@mbk"
    var topupAmount by remember { mutableStateOf("1000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val parsed = topupAmount.toDoubleOrNull() ?: 0.0
                    if (parsed > 0) {
                        onAddMoney(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                modifier = Modifier.testTag("confirm_owner_topup_btn")
            ) {
                Text("Confirm Top Up (UPI)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        icon = {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = KaamWalaNavy, modifier = Modifier.size(32.dp))
        },
        title = { Text("Add Money to Owner Wallet", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Current Owner Balance: ₹${"%.2f".format(currentBalance)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = topupAmount,
                    onValueChange = { topupAmount = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Top Up Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500.0, 1000.0, 2500.0).forEach { amt ->
                        SuggestionChip(
                            onClick = { topupAmount = amt.toInt().toString() },
                            label = { Text("+₹${amt.toInt()}") }
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
                            text = "TOP UP TRANSACTION UPI",
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
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Taskly Merchant UPI", merchantUpi))
                                    Toast.makeText(context, "UPI ID copied: $merchantUpi", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_topup_upi_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = KaamWalaOrangeLight, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val amt = topupAmount.toDoubleOrNull() ?: 1000.0
                                    val upiUri = Uri.parse("upi://pay?pa=$merchantUpi&pn=Taskly&am=$amt&cu=INR&tn=TasklyWalletTopup")
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

@Composable
private fun CashCommissionPolicyCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KaamWalaOrangeDark, modifier = Modifier.size(18.dp))
                Text(
                    text = "CASH PAYMENT & COMMISSION DEDUCTION RULES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = KaamWalaOrangeDark,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val rules = listOf(
                "• Cash Payments: If owner pays full amount in cash on-site, the 10% platform commission is debited to the worker's khata.",
                "• Automatic Offset: Dues are deducted automatically from the worker's next online escrow payout.",
                "• Strict ₹200 Limit: Maximum negative balance cannot exceed ₹200.",
                "• Work Suspension: If negative balance reaches ₹200, the worker cannot work or apply until they pay up via UPI."
            )

            rules.forEach { rule ->
                Text(
                    text = rule,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MerchantUpiDestinationCard(
    merchantUpi: String,
    onCopy: () -> Unit,
    onPayNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("merchant_upi_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KaamWalaNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = KaamWalaOrangeLight, modifier = Modifier.size(18.dp))
                    Text(
                        text = "KAAMWALA ESCROW & COMMISSION UPI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = KaamWalaOrangeLight,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Configured UPI",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Merchant VPA / UPI ID",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate300)
                        )
                        Text(
                            text = merchantUpi,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("copy_upi_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = KaamWalaOrangeLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPayNow,
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch UPI App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCopy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Text("Copy ID", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PaymentRecordCard(
    record: PaymentRecord,
    onSubmitUtrClick: () -> Unit,
    onCashPaidClick: () -> Unit,
    onReleasePayoutClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_card_${record.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate200)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.id} • ${record.paymentMode.label}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Slate500
                )
                PaymentStatusBadge(status = record.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = record.jobTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Slate900
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Customer: ${record.customerName}", style = MaterialTheme.typography.bodySmall, color = Slate700)
                    Text(text = "Worker: ${record.workerName}", style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total: ₹${"%.2f".format(record.totalAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "Fee (10%): ₹${"%.2f".format(record.platformCommission)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaamWalaOrangeDark
                    )
                    Text(
                        text = "Worker: ₹${"%.2f".format(record.workerAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = KaamWalaGreenText
                    )
                }
            }

            if (record.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: ${record.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.status == PaymentStatus.PENDING_CUSTOMER_PAYMENT) {
                    Button(
                        onClick = onSubmitUtrClick,
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Online UTR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCashPaidClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Paid Cash", fontSize = 11.sp)
                    }
                } else if (record.status == PaymentStatus.VERIFIED_IN_ESCROW) {
                    Button(
                        onClick = onReleasePayoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = KaamWalaGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Release Payout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onManageClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Audit / Lifecycle", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SubmitUtrModal(
    record: PaymentRecord,
    onDismiss: () -> Unit,
    onSubmit: (utr: String, upi: String) -> Unit
) {
    val context = LocalContext.current
    val escrowUpi = "8360608312@mbk"
    var utr by remember { mutableStateOf("") }
    var customerUpi by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (utr.trim().length < 8) {
                        error = "Please enter a valid 12-digit bank UTR / Ref number."
                    } else {
                        onSubmit(utr.trim(), customerUpi.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                modifier = Modifier.testTag("submit_utr_btn")
            ) {
                Text("Confirm Reference")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Submit Payment Reference") },
        text = {
            Column {
                Text(
                    text = "Transfer ₹${"%.0f".format(record.totalAmount)} to Taskly Escrow UPI to confirm escrow reservation.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Active Transaction UPI Details Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = KaamWalaNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ESCROW PAYMENT UPI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = KaamWalaOrangeLight,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = escrowUpi,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Taskly Escrow UPI", escrowUpi))
                                    Toast.makeText(context, "UPI ID copied: $escrowUpi", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp).testTag("copy_escrow_upi_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = KaamWalaOrangeLight, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val upiUri = Uri.parse("upi://pay?pa=$escrowUpi&pn=Taskly&am=${record.totalAmount}&cu=INR&tn=TasklyEscrow_${record.id}")
                                    val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                    context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No UPI app found. Please copy UPI ID $escrowUpi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch UPI App to Pay", fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = utr,
                    onValueChange = { utr = it },
                    label = { Text("12-Digit UTR / Transaction Ref *") },
                    placeholder = { Text("e.g., 429810839210") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("utr_number_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerUpi,
                    onValueChange = { customerUpi = it },
                    label = { Text("Your UPI ID (Optional)") },
                    placeholder = { Text("e.g., yourname@oksbi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                error?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = KaamWalaRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@Composable
private fun ManagePaymentStatusModal(
    record: PaymentRecord,
    onDismiss: () -> Unit,
    onUpdate: (PaymentStatus, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(record.status) }
    var note by remember { mutableStateOf(record.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onUpdate(selectedStatus, note) },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
            ) {
                Text("Update Lifecycle State")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Manage Payment Lifecycle") },
        text = {
            Column {
                Text("Transaction ID: ${record.id}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                Text("Current: ${record.status.label}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))

                Spacer(modifier = Modifier.height(8.dp))
                Text("Select new state:", style = MaterialTheme.typography.labelSmall)

                PaymentStatus.values().forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(status.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(status.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate500))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Audit / Lifecycle Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
