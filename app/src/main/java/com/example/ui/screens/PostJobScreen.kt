package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.CommissionBreakdownCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    adSettings: AdSettings,
    consentPreferences: ConsentPreferences,
    onJobCreated: (
        title: String,
        description: String,
        category: JobCategory,
        customCategoryTitle: String,
        location: String,
        city: String,
        budget: Double,
        wageType: WageType,
        employerName: String
    ) -> Unit,
    onNavigateToPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(JobCategory.DELIVERY) }
    var customCategoryInput by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("New Delhi") }
    var location by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("500") }
    var wageType by remember { mutableStateOf(WageType.PER_TASK) }
    var employerName by remember { mutableStateOf("") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var postSuccess by remember { mutableStateOf(false) }

    val budgetAmount = budgetInput.toDoubleOrNull() ?: 0.0
    val commission = remember(budgetAmount) {
        calculateCommission(budgetAmount, 10.0)
    }

    // Vulgarity checks
    val isTitleVulgar = remember(title) { containsVulgarLanguage(title) }
    val isDescVulgar = remember(description) { containsVulgarLanguage(description) }
    val isCustomCategoryVulgar = remember(customCategoryInput) { containsVulgarLanguage(customCategoryInput) }
    val hasVulgarContent = isTitleVulgar || isDescVulgar || isCustomCategoryVulgar

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
            .testTag("post_job_screen")
    ) {
        // Screen Header
        Text(
            text = "Post a New Task / Hire on Taskly",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = KaamWalaNavy
            )
        )
        Text(
            text = "Hire local gig workers with automated 10% platform fee and escrow protection.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate500
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Vulgarity & Safety Shield Notice
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFEFF6FF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = KaamWalaNavy, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        text = "Safety & Etiquette Policy",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = KaamWalaNavy)
                    )
                    Text(
                        text = "Vulgar language and personal phone numbers are strictly prohibited. Task coordination is conducted via In-App Chat.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Slate700)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Commission Preview Highlight
        CommissionBreakdownCard(
            customerBudget = commission.customerBudget,
            commissionAmount = commission.platformCommission,
            workerPayout = commission.workerNetPayout,
            ratePercent = 10.0
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title *") },
            placeholder = { Text("e.g., Deliver packages from Lajpat Nagar") },
            isError = isTitleVulgar,
            supportingText = {
                if (isTitleVulgar) {
                    Text("Inappropriate/vulgar language detected", color = KaamWalaRed, fontSize = 11.sp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_title_input"),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = showCategoryMenu,
            onExpandedChange = { showCategoryMenu = !showCategoryMenu }
        ) {
            OutlinedTextField(
                value = selectedCategory.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Task Category *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("post_category_select"),
                shape = RoundedCornerShape(10.dp)
            )

            ExposedDropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { showCategoryMenu = false }
            ) {
                JobCategory.values().forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (category == JobCategory.OTHER) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = KaamWalaOrangePrimary, modifier = Modifier.size(16.dp))
                                }
                                Text(category.displayName, fontWeight = if (category == JobCategory.OTHER) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        onClick = {
                            selectedCategory = category
                            showCategoryMenu = false
                        }
                    )
                }
            }
        }

        // Custom Category Field if "OTHER" is selected
        if (selectedCategory == JobCategory.OTHER) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = customCategoryInput,
                onValueChange = { customCategoryInput = it },
                label = { Text("Specify Work Needed (Custom Job) *") },
                placeholder = { Text("e.g., Terrace Gardening, Pet Care, Furniture Assembly...") },
                isError = isCustomCategoryVulgar,
                supportingText = {
                    if (isCustomCategoryVulgar) {
                        Text("Inappropriate/vulgar language detected", color = KaamWalaRed, fontSize = 11.sp)
                    } else {
                        Text("Describe the custom work type for workers", fontSize = 11.sp, color = Slate500)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_custom_category_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Detailed Requirements / Description *") },
            placeholder = { Text("Mention timing, tools required, items to handle...") },
            isError = isDescVulgar,
            supportingText = {
                if (isDescVulgar) {
                    Text("Inappropriate/vulgar language detected", color = KaamWalaRed, fontSize = 11.sp)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("post_description_input"),
            shape = RoundedCornerShape(10.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        // City & Location
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City *") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("post_city_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Area / Landmark *") },
                placeholder = { Text("Sector / Colony") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("post_location_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Budget & Wage Type (With Minimum ₹100 Validation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Customer Budget (₹) *") },
                supportingText = { Text("Minimum pay is ₹100", fontSize = 10.sp, color = if (budgetAmount < 100.0) KaamWalaRed else Slate500) },
                isError = budgetAmount < 100.0 && budgetInput.isNotEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("post_budget_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wage Type",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    FilterChip(
                        selected = wageType == WageType.PER_TASK,
                        onClick = { wageType = WageType.PER_TASK },
                        label = { Text("Task", fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = wageType == WageType.PER_DAY,
                        onClick = { wageType = WageType.PER_DAY },
                        label = { Text("Daily", fontSize = 11.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Employer Name
        OutlinedTextField(
            value = employerName,
            onValueChange = { employerName = it },
            label = { Text("Your Name / Business Name *") },
            placeholder = { Text("e.g. Ramesh Gupta") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_employer_name_input"),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = KaamWalaRedContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    color = KaamWalaRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // In-page Ad placement for Post a Task webpage
        com.example.ui.components.KaamWalaAdSlot(
            slotConfig = adSettings.contentSlot,
            globalAdsEnabled = adSettings.globalAdsEnabled,
            adSettings = adSettings,
            consentPreferences = consentPreferences,
            minHeight = 70.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Submit Button
        Button(
            onClick = {
                if (title.isBlank()) {
                    errorMessage = "Please enter a task title."
                } else if (description.isBlank()) {
                    errorMessage = "Please describe the work required."
                } else if (selectedCategory == JobCategory.OTHER && customCategoryInput.isBlank()) {
                    errorMessage = "Please specify the custom work needed in the 'Other' category field."
                } else if (hasVulgarContent) {
                    errorMessage = "Please remove vulgar, abusive, or offensive language before submitting."
                } else if (location.isBlank()) {
                    errorMessage = "Please enter the area/landmark."
                } else if (budgetAmount < MINIMUM_JOB_PAY) {
                    errorMessage = "Minimum pay for any job is ₹100. Please enter ₹100 or higher."
                } else if (employerName.isBlank()) {
                    errorMessage = "Please provide your contact/business name."
                } else {
                    errorMessage = null
                    onJobCreated(
                        title.trim(),
                        description.trim(),
                        selectedCategory,
                        customCategoryInput.trim(),
                        location.trim(),
                        city.trim(),
                        budgetAmount,
                        wageType,
                        employerName.trim()
                    )
                    postSuccess = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = KaamWalaOrangePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_post_job_btn")
        ) {
            Icon(Icons.Default.Publish, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Post Task & Create Escrow (₹${"%.0f".format(budgetAmount.coerceAtLeast(100.0))})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }

    if (postSuccess) {
        AlertDialog(
            onDismissRequest = { postSuccess = false },
            confirmButton = {
                Button(
                    onClick = {
                        postSuccess = false
                        onNavigateToPayments()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaamWalaNavy)
                ) {
                    Text("Go to Payments & Escrow")
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KaamWalaGreenText)
                    Text("Task Posted on Taskly!")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your task is now live on Taskly! Workers matching this work category can discover and apply.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimum ₹100 budget requirement met. Contact numbers are shielded and all discussions occur through the moderated in-app chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        )
    }
}
