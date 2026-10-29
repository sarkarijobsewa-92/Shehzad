package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.JobStatus
import com.example.model.PaymentStatus
import com.example.model.TASKLY_SUPPORT_EMAIL
import com.example.ui.theme.*

@Composable
fun TasklyHeader(
    onAdminClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSupportClick: (() -> Unit)? = null,
    onDomainClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("taskly_header_card"),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Taskly Logo & Brand Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.taskly_logo),
                        contentDescription = "Taskly Official Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Task",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = KaamWalaNavy
                                )
                            )
                            Text(
                                text = "ly",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = KaamWalaOrangePrimary
                                )
                            )
                        }
                        Text(
                            text = "Stop being idle. Start earning.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500
                            )
                        )
                    }
                }

                // Header Actions: Support & Admin
                var showInternalSupportDialog by remember { mutableStateOf(false) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (onSupportClick != null) {
                                onSupportClick()
                            } else {
                                showInternalSupportDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_support_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = "Help & Support Desk",
                            tint = KaamWalaOrangeDark
                        )
                    }

                    IconButton(
                        onClick = onAdminClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("header_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin & Ads Console",
                            tint = KaamWalaNavy
                        )
                    }
                }

                if (showInternalSupportDialog) {
                    TasklySupportDialog(
                        onDismiss = { showInternalSupportDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        PaymentStatus.PENDING_CUSTOMER_PAYMENT -> Triple(KaamWalaYellowContainer, Color(0xFFB45309), Icons.Default.Pending)
        PaymentStatus.PAYMENT_SUBMITTED_UTR -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), Icons.Default.HourglassTop)
        PaymentStatus.VERIFIED_IN_ESCROW -> Triple(KaamWalaGreenContainer, KaamWalaGreenText, Icons.Default.CheckCircle)
        PaymentStatus.CASH_PAID_ON_SITE -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Payments)
        PaymentStatus.TASK_COMPLETED -> Triple(Color(0xFFDFF6DD), Color(0xFF166534), Icons.Default.TaskAlt)
        PaymentStatus.WORKER_PAYOUT_RELEASED -> Triple(KaamWalaGreenContainer, KaamWalaGreenText, Icons.Default.Payments)
        PaymentStatus.DISPUTED -> Triple(KaamWalaRedContainer, Color(0xFFB91C1C), Icons.Default.ReportProblem)
        PaymentStatus.REFUNDED -> Triple(Slate200, Slate700, Icons.Default.Undo)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun CommissionBreakdownCard(
    customerBudget: Double,
    commissionAmount: Double,
    workerPayout: Double,
    ratePercent: Double = 10.0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Slate300)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRANSPARENT 10% FEE BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Slate500
                )
                Text(
                    text = "Min ₹100 • No Hidden Charges",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = KaamWalaGreenText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Customer Total Budget:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
                Text(
                    text = "₹${"%.2f".format(customerBudget)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate900
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Taskly Platform Commission (${ratePercent.toInt()}%):",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaamWalaOrangeDark
                )
                Text(
                    text = "- ₹${"%.2f".format(commissionAmount)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = KaamWalaOrangeDark
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Slate200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KaamWalaGreenText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Net Worker Payout:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = KaamWalaGreenText
                    )
                }
                Text(
                    text = "₹${"%.2f".format(workerPayout)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = KaamWalaGreenText
                )
            }
        }
    }
}

/**
 * Status indicator for tasks/jobs on Taskly (e.g., 'Open', 'In Progress', 'Completed').
 * Provides instant visual clarity on task availability.
 */
@Composable
fun JobStatusBadge(
    status: JobStatus,
    isCurrentWorkerActiveJob: Boolean = false,
    modifier: Modifier = Modifier
) {
    val style = when (status) {
        JobStatus.OPEN -> {
            JobStatusStyle(
                bgColor = Color(0xFFDCFCE7),
                textColor = Color(0xFF15803D),
                borderColor = Color(0xFF86EFAC),
                icon = Icons.Default.CheckCircle,
                label = "Open"
            )
        }
        JobStatus.IN_PROGRESS -> {
            if (isCurrentWorkerActiveJob) {
                JobStatusStyle(
                    bgColor = KaamWalaOrangeContainer,
                    textColor = KaamWalaOrangeDark,
                    borderColor = KaamWalaOrangePrimary.copy(alpha = 0.5f),
                    icon = Icons.Default.HourglassTop,
                    label = "Your Active Task"
                )
            } else {
                JobStatusStyle(
                    bgColor = Color(0xFFFEF3C7),
                    textColor = Color(0xFFB45309),
                    borderColor = Color(0xFFFDE68A),
                    icon = Icons.Default.Sync,
                    label = "In Progress"
                )
            }
        }
        JobStatus.COMPLETED -> {
            JobStatusStyle(
                bgColor = Color(0xFFF1F5F9),
                textColor = Color(0xFF475569),
                borderColor = Color(0xFFCBD5E1),
                icon = Icons.Default.TaskAlt,
                label = "Completed"
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = style.bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, style.borderColor),
        modifier = modifier.testTag("job_status_badge_${status.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.textColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = style.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = style.textColor
                )
            )
        }
    }
}

private data class JobStatusStyle(
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

/**
 * Support email and contact card for Taskly queries: sarkarijobsewa@gmail.com
 */
@Composable
fun SupportContactCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(KaamWalaOrangePrimary.copy(alpha = 0.35f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = KaamWalaOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "TASKLY HELP & SUPPORT DESK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = KaamWalaGreenContainer
                ) {
                    Text(
                        text = "24/7 EMAIL ASSISTANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = KaamWalaGreenText
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Have questions regarding job listings, wallet top-ups, commission deduction, or payment dispute?",
                style = MaterialTheme.typography.bodySmall,
                color = Slate700
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = KaamWalaNavy,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Support Email for Queries",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Slate500
                            )
                            Text(
                                text = TASKLY_SUPPORT_EMAIL,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KaamWalaNavy
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Taskly Support Email", TASKLY_SUPPORT_EMAIL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Support email copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy support email",
                                tint = Slate700,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$TASKLY_SUPPORT_EMAIL?subject=${Uri.encode("Taskly Support Query")}")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Email Query"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch email app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Taskly Support Dialog modal
 */
@Composable
fun TasklySupportDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$TASKLY_SUPPORT_EMAIL?subject=${Uri.encode("Taskly Support Query")}")
                    }
                    try {
                        context.startActivity(Intent.createChooser(emailIntent, "Send Email Query"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Support email: $TASKLY_SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = KaamWalaOrangePrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Taskly Support & Help Desk", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "If you have any questions, encounter payment issues, or require assistance with worker/owner tasks, reach out to our team at:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Official Support Email:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                text = TASKLY_SUPPORT_EMAIL,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = KaamWalaNavy)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Taskly Support Email", TASKLY_SUPPORT_EMAIL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Support email copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", tint = KaamWalaNavy)
                        }
                    }
                }
            }
        }
    )
}
