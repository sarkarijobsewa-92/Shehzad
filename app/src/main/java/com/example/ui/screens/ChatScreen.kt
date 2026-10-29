package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.containsPhoneNumber
import com.example.model.containsVulgarLanguage
import com.example.model.AdSettings
import com.example.model.ConsentPreferences
import com.example.ui.components.KaamWalaAdSlot
import com.example.ui.theme.*

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSendMessage: (jobId: String, jobTitle: String, role: ChatRole, senderName: String, text: String) -> Unit,
    adSettings: AdSettings? = null,
    consentPreferences: ConsentPreferences? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var currentRole by remember { mutableStateOf(ChatRole.WORKER) }
    var selectedJobId by remember { mutableStateOf("TSK-102") }
    var selectedJobTitle by remember { mutableStateOf("Emergency Ceiling Fan Repair") }
    var safetyWarningDialog by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .testTag("chat_screen")
    ) {
        // Privacy & Safety Shield Header Banner
        Surface(
            color = KaamWalaNavy,
            modifier = Modifier.fillMaxWidth()
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
                        Surface(
                            shape = CircleShape,
                            color = KaamWalaGreenContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = KaamWalaGreenText,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Taskly Protected Messenger",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Zero Vulgarity & Contact Shield Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = KaamWalaOrangeLight
                                )
                            )
                        }
                    }

                    // Role Switcher
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            FilterChip(
                                selected = currentRole == ChatRole.WORKER,
                                onClick = { currentRole = ChatRole.WORKER },
                                label = { Text("Worker", fontSize = 10.sp, color = Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KaamWalaOrangePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = currentRole == ChatRole.OWNER,
                                onClick = { currentRole = ChatRole.OWNER },
                                label = { Text("Owner", fontSize = 10.sp, color = Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KaamWalaOrangePrimary,
                                    selectedLabelColor = Color.White
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Active Task Context
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Task: $selectedJobTitle ($selectedJobId)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Slate300
                            )
                        )
                        Text(
                            text = "Escrow Protected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = KaamWalaGreen
                            )
                        )
                    }
                }
            }
        }

        // Policy Warning Banner
        Surface(
            color = Color(0xFFFEF3C7),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                Text(
                    text = "Rules: 1. No vulgar or abusive language. 2. Never share phone numbers or outside accounts. Violations result in immediate ban.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color(0xFF78350F))
                )
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg = msg, isCurrentSender = (msg.senderRole == currentRole))
            }
        }

        if (adSettings != null && consentPreferences != null) {
            KaamWalaAdSlot(
                slotConfig = adSettings.contentSlot,
                globalAdsEnabled = adSettings.globalAdsEnabled,
                adSettings = adSettings,
                consentPreferences = consentPreferences,
                minHeight = 60.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }

        // Message Input Row
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Real-time detection warnings
                val containsPhone = remember(inputText) { containsPhoneNumber(inputText) }
                val containsVulgar = remember(inputText) { containsVulgarLanguage(inputText) }
                val hasSafetyViolation = containsPhone || containsVulgar

                AnimatedVisibility(visible = hasSafetyViolation) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KaamWalaRedContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = KaamWalaRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = when {
                                    containsVulgar -> "Vulgar or abusive language detected! Please maintain polite etiquette."
                                    else -> "Phone number detected! Sharing phone numbers is strictly prohibited."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = KaamWalaRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a polite message (no phone or profanity)...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate100,
                            unfocusedContainerColor = Slate100,
                            focusedBorderColor = if (hasSafetyViolation) KaamWalaRed else KaamWalaOrangePrimary,
                            unfocusedBorderColor = if (hasSafetyViolation) KaamWalaRed else Slate300
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            if (textToSend.isEmpty()) return@IconButton

                            if (containsVulgarLanguage(textToSend)) {
                                safetyWarningDialog = "⚠️ Message Blocked: Your message contains vulgar, abusive, or inappropriate language. Taskly enforces a zero-tolerance policy against vulgar words to protect all community members."
                                onSendMessage(
                                    selectedJobId,
                                    selectedJobTitle,
                                    currentRole,
                                    if (currentRole == ChatRole.WORKER) "Sunil Kumar (Worker)" else "Ananya Raman (Owner)",
                                    textToSend
                                )
                                inputText = ""
                            } else if (containsPhoneNumber(textToSend)) {
                                safetyWarningDialog = "⚠️ Message Blocked: You attempted to send a phone number. For your security and escrow guarantee, phone numbers and personal contact information cannot be shared."
                                onSendMessage(
                                    selectedJobId,
                                    selectedJobTitle,
                                    currentRole,
                                    if (currentRole == ChatRole.WORKER) "Sunil Kumar (Worker)" else "Ananya Raman (Owner)",
                                    textToSend
                                )
                                inputText = ""
                            } else {
                                onSendMessage(
                                    selectedJobId,
                                    selectedJobTitle,
                                    currentRole,
                                    if (currentRole == ChatRole.WORKER) "Sunil Kumar (Worker)" else "Ananya Raman (Owner)",
                                    textToSend
                                )
                                inputText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (hasSafetyViolation) KaamWalaRed else KaamWalaOrangePrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = if (hasSafetyViolation) Icons.Default.Block else Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Safety Warning Alert Dialog
    safetyWarningDialog?.let { warning ->
        AlertDialog(
            onDismissRequest = { safetyWarningDialog = null },
            confirmButton = {
                Button(
                    onClick = { safetyWarningDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                ) {
                    Text("I Understand")
                }
            },
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = KaamWalaRed, modifier = Modifier.size(32.dp))
            },
            title = { Text("Taskly Safety Shield", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
            }
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, isCurrentSender: Boolean) {
    if (msg.senderRole == ChatRole.SYSTEM) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate200,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate700,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    if (msg.isBlocked) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = KaamWalaRedContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, KaamWalaRed.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = KaamWalaRed, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Safety Violation Detected",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = KaamWalaRed)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7F1D1D), fontSize = 11.sp)
                )
            }
        }
        return
    }

    val bubbleAlignment = if (isCurrentSender) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isCurrentSender) KaamWalaOrangePrimary else Color.White
    val textColor = if (isCurrentSender) Color.White else Slate900

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = bubbleAlignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentSender) 16.dp else 4.dp,
                bottomEnd = if (isCurrentSender) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.dp,
            border = if (!isCurrentSender) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Slate200)) else null,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentSender) Color.White.copy(alpha = 0.85f) else KaamWalaNavy,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = msg.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = if (isCurrentSender) Color.White.copy(alpha = 0.7f) else Slate500
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                )
            }
        }
    }
}
