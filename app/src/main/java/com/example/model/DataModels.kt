package com.example.model

enum class JobStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

/**
 * Represents a task/job on the Taskly marketplace
 */
data class Job(
    val id: String,
    val title: String,
    val description: String,
    val category: JobCategory,
    val customCategoryTitle: String = "",
    val location: String,
    val city: String,
    val pinCode: String,
    val customerBudget: Double, // Gross customer payment, MINIMUM ₹100
    val wageType: WageType = WageType.PER_TASK,
    val durationText: String = "1-2 hours",
    val employerName: String,
    val isUrgent: Boolean = false,
    val requirements: List<String> = emptyList(),
    val postedTimeAgo: String = "Just now",
    val status: JobStatus = JobStatus.OPEN,
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null
) {
    val commission: CommissionBreakdown
        get() = calculateCommission(customerBudget)

    val categoryDisplayName: String
        get() = if (category == JobCategory.OTHER && customCategoryTitle.isNotBlank()) {
            customCategoryTitle
        } else {
            category.displayName
        }
}

enum class JobCategory(val displayName: String, val iconName: String) {
    DELIVERY("Delivery & Courier", "local_shipping"),
    HOME_SERVICES("Home Repair & Electrician", "build"),
    CLEANING_COOKING("Cleaning & Cooking", "cleaning_services"),
    RETAIL_HELPER("Shop & Store Assistant", "storefront"),
    EVENT_STAFF("Event & Catering Helper", "celebration"),
    OFFICE_ADMIN("Office Peon & Courier", "business_center"),
    MICRO_TASK("Micro-Tasks & Field Survey", "assignment"),
    LABOUR("Loading & Labour", "front_loader"),
    OTHER("Other (Custom Work)", "more_horiz")
}

enum class WageType(val label: String) {
    PER_TASK("Per Task"),
    PER_DAY("Daily Wage"),
    PER_MONTH("Monthly")
}

const val MINIMUM_JOB_PAY = 100.0
const val MAX_ALLOWED_NEGATIVE_BALANCE = -200.0
const val DEBT_LOCKOUT_THRESHOLD = -199.0
const val TASKLY_SUPPORT_EMAIL = "sarkarijobsewa@gmail.com"

/**
 * 10% Platform Commission Breakdown
 * Minimum job pay is ₹100.
 */
data class CommissionBreakdown(
    val customerBudget: Double,
    val platformRatePercentage: Double = 10.0,
    val platformCommission: Double,
    val workerNetPayout: Double,
    val isValidMinimumPay: Boolean = customerBudget >= MINIMUM_JOB_PAY
)

fun calculateCommission(budget: Double, ratePercent: Double = 10.0): CommissionBreakdown {
    val rateDecimal = ratePercent / 100.0
    val commission = (budget * rateDecimal * 100.0).toLong() / 100.0
    val workerPayout = budget - commission
    return CommissionBreakdown(
        customerBudget = budget,
        platformRatePercentage = ratePercent,
        platformCommission = commission,
        workerNetPayout = workerPayout,
        isValidMinimumPay = budget >= MINIMUM_JOB_PAY
    )
}

/**
 * Owner Profile with Wallet
 */
data class OwnerProfile(
    val id: String = "O-2001",
    val name: String = "Rajesh Sharma",
    val walletBalance: Double = 2500.0
)

enum class WorkDonePaymentType(val label: String) {
    OWNER_WALLET("Pay from Owner Wallet (Online)"),
    CASH("Pay in Cash on-site")
}

/**
 * Worker Profile including preferred skills, wallet balance, active job, and debt state.
 *
 * Rules:
 * 1. 10% commission is deducted from owner payment into worker wallet, or from worker wallet if cash.
 * 2. If wallet is 0, balance goes negative, but cannot reach -₹200.
 * 3. If wallet reaches -₹199 (or worse), worker cannot choose new work until he adds money and makes it zero (>= 0.0).
 * 4. Worker cannot choose next work until owner clicks "Work Done".
 */
data class WorkerProfile(
    val id: String = "W-1001",
    val name: String = "Sunil Kumar",
    val selectedSkills: Set<JobCategory> = setOf(
        JobCategory.DELIVERY,
        JobCategory.HOME_SERVICES,
        JobCategory.CLEANING_COOKING,
        JobCategory.OTHER
    ),
    val walletBalance: Double = -50.0,
    val activeJobId: String? = null,
    val isSuspendedDueToDebt: Boolean = false
) {
    // Backward compatibility property
    val commissionBalance: Double
        get() = walletBalance

    val outstandingDue: Double
        get() = if (walletBalance < 0.0) -walletBalance else 0.0

    // Worker cannot choose new work if wallet balance reaches -199 or debt suspension flag is active
    val isBlockedByNegativeBalance: Boolean
        get() = isSuspendedDueToDebt || walletBalance <= DEBT_LOCKOUT_THRESHOLD

    val isSuspendedForDues: Boolean
        get() = isBlockedByNegativeBalance

    val hasActiveJob: Boolean
        get() = activeJobId != null

    val canAcceptNewWork: Boolean
        get() = !isBlockedByNegativeBalance && !hasActiveJob
}

/**
 * In-App Messenger Chat models
 */
enum class ChatRole {
    WORKER,
    OWNER,
    SYSTEM
}

data class ChatMessage(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val senderName: String,
    val senderRole: ChatRole,
    val text: String,
    val timestamp: String,
    val isBlocked: Boolean = false,
    val blockedReason: String = ""
)

/**
 * Phone number detection and personal contact monitoring
 */
fun containsPhoneNumber(input: String): Boolean {
    val normalized = input.lowercase()
    val phoneKeywords = listOf("phone", "mobile", "whatsapp", "call me", "contact me", "no:", "num:")
    val hasKeyword = phoneKeywords.any { normalized.contains(it) }

    val digitsOnly = input.filter { it.isDigit() }

    if (digitsOnly.length >= 10) {
        val pattern10 = Regex("(?:\\+?91[\\s-]?)?[6-9]\\d{9}")
        val cleanString = input.replace(Regex("[\\s.-]"), "")
        if (pattern10.containsMatchIn(input) || pattern10.containsMatchIn(cleanString)) {
            return true
        }
        if (digitsOnly.length in 10..13) {
            return true
        }
    }

    val spacedPattern = Regex("[6-9](?:[\\s.-]*\\d){9}")
    if (spacedPattern.containsMatchIn(input)) {
        return true
    }

    val numberWords = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    var wordCount = 0
    val tokens = normalized.split(Regex("[\\s,.-]+"))
    for (token in tokens) {
        if (token in numberWords) {
            wordCount++
        }
    }
    if (wordCount >= 7) {
        return true
    }

    if (hasKeyword && digitsOnly.length >= 6) {
        return true
    }

    return false
}

/**
 * Vulgar Language and Profanity Filter
 */
private val VULGAR_WORDS = setOf(
    "fuck", "fucking", "fucker", "fuk", "fck",
    "shit", "bullshit", "bitch", "bitches", "bastard",
    "asshole", "ass", "dick", "pussy", "cunt",
    "porn", "sex", "nude", "whore", "slut", "motherfucker",
    "bhenchod", "benchod", "bc", "madarchod", "madharchod", "mc",
    "chutiya", "chootiya", "choot", "chuche",
    "gaand", "gand", "gandu", "lund", "lodu", "lauda", "lawda",
    "harami", "kutta", "randi", "r@ndi", "raand",
    "bsdk", "bhosdike", "bhosadi", "bhosdi", "saala", "kamina",
    "tatti", "chirkut", "suar", "harami"
)

fun containsVulgarLanguage(input: String): Boolean {
    if (input.isBlank()) return false

    val clean = input.lowercase()
        .replace('@', 'a')
        .replace('$', 's')
        .replace('0', 'o')
        .replace('1', 'i')
        .replace('!', 'i')
        .replace('*', 'u')

    val words = clean.split(Regex("[\\s,._\\-!?:;\"'()]+"))

    for (w in words) {
        if (w in VULGAR_WORDS) {
            return true
        }
    }

    for (bad in VULGAR_WORDS) {
        if (bad.length >= 4) {
            val regex = Regex("\\b${Regex.escape(bad)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(clean)) {
                return true
            }
            if (clean.contains(bad)) {
                return true
            }
        }
    }

    return false
}

enum class PaymentMode(val label: String) {
    ONLINE_ESCROW("Online UPI Escrow"),
    CASH_ON_SITE("Cash to Worker (Direct)")
}

enum class PaymentStatus(val label: String, val description: String) {
    PENDING_CUSTOMER_PAYMENT("Pending Customer Payment", "Customer initiated booking. Payment transfer to Taskly merchant UPI pending."),
    PAYMENT_SUBMITTED_UTR("UTR Reference Submitted", "Customer entered UTR/Reference number. Under merchant verification."),
    VERIFIED_IN_ESCROW("Payment Verified in Escrow", "Funds safely confirmed by merchant and reserved for worker payout."),
    CASH_PAID_ON_SITE("Cash Paid by Owner", "Owner paid worker in cash. 10% platform commission added to worker negative balance."),
    TASK_COMPLETED("Task Marked Completed", "Worker completed assignment. Payout awaiting final release."),
    WORKER_PAYOUT_RELEASED("Worker Payout Released", "Earnings successfully disbursed to worker UPI."),
    DISPUTED("Payment In Dispute", "Review requested by customer or worker. Escrow held securely."),
    REFUNDED("Refunded to Customer", "Order cancelled/dispute resolved with refund.")
}

data class PaymentRecord(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val customerName: String,
    val workerName: String,
    val totalAmount: Double,
    val platformCommission: Double,
    val workerAmount: Double,
    val paymentMode: PaymentMode = PaymentMode.ONLINE_ESCROW,
    val merchantUpiId: String = "8360608312@mbk",
    val customerUpiId: String = "",
    val workerUpiId: String = "",
    val utrNumber: String = "",
    val status: PaymentStatus,
    val timestamp: String,
    val notes: String = ""
)

enum class AdProvider(val displayName: String) {
    NONE("Disabled / None"),
    MONETAG("Monetag (Active)"),
    GOOGLE_ADSENSE("Google AdSense (Disabled)")
}

enum class AdSlotType(val slotName: String, val recommendedLocation: String) {
    HEADER_BANNER("Top Banner (Under Logo Bar)", "Always visible directly under Taskly top logo bar"),
    HOMEPAGE("Slot 1: Homepage", "Below hero section, before job discovery feed"),
    JOB_FEED("Slot 2: Job Listing Feed", "Between job card groupings (every 3 listings)"),
    CONTENT_INFO("Slot 3: Content & Pages", "Bottom of find work, post task, payments, and info pages")
}

data class AdSlotConfig(
    val slotType: AdSlotType,
    val isEnabled: Boolean,
    val provider: AdProvider,
    val adUnitOrZoneId: String,
    val customNotes: String = ""
)

data class AdSettings(
    val globalAdsEnabled: Boolean = true,
    val adSensePublisherId: String = "",
    val monetagSiteId: String = "3nbf4.com",
    val monetagDomain: String = "3nbf4.com",
    val monetagZoneId: String = "10831624",
    val monetagServiceWorkerUrl: String = "https://3nbf4.com/act/files/service-worker.min.js?r=sw",
    // Always visible top ad placement under logo bar (All Monetag)
    val headerSlot: AdSlotConfig = AdSlotConfig(
        slotType = AdSlotType.HEADER_BANNER,
        isEnabled = true,
        provider = AdProvider.MONETAG,
        adUnitOrZoneId = "10831624",
        customNotes = "Monetag Persistent Top Banner (Zone 10831624 • 3nbf4.com)"
    ),
    val homepageSlot: AdSlotConfig = AdSlotConfig(
        slotType = AdSlotType.HOMEPAGE,
        isEnabled = true,
        provider = AdProvider.MONETAG,
        adUnitOrZoneId = "10831624",
        customNotes = "Monetag Homepage Responsive Unit (Zone 10831624 • 3nbf4.com)"
    ),
    val jobFeedSlot: AdSlotConfig = AdSlotConfig(
        slotType = AdSlotType.JOB_FEED,
        isEnabled = true,
        provider = AdProvider.MONETAG,
        adUnitOrZoneId = "10831624",
        customNotes = "Monetag In-Feed Native Unit (Zone 10831624 • 3nbf4.com)"
    ),
    val contentSlot: AdSlotConfig = AdSlotConfig(
        slotType = AdSlotType.CONTENT_INFO,
        isEnabled = true,
        provider = AdProvider.MONETAG,
        adUnitOrZoneId = "10831624",
        customNotes = "Monetag In-Page Content Footer (Zone 10831624 • 3nbf4.com)"
    ),
    val jobFeedFrequency: Int = 3,
    val previewModeEnabled: Boolean = true,
    val revenueConnected: Boolean = true
)

data class ConsentPreferences(
    val isPromptShown: Boolean = true,
    val essentialAccepted: Boolean = true,
    val personalizedAdsAccepted: Boolean = true,
    val analyticsAccepted: Boolean = true,
    val consentDate: String = "2026-10-26"
)

data class DomainMigrationConfig(
    val primaryDomain: String = "www.sarkarijobsewa.com",
    val canonicalUrl: String = "https://www.sarkarijobsewa.com",
    val apexDomain: String = "sarkarijobsewa.com",
    val rootRedirectTarget: String = "https://www.sarkarijobsewa.com",
    val isSslActive: Boolean = true,
    val isBackupCreated: Boolean = true,
    val isEmailDnsPreserved: Boolean = true,
    val targetHostingIp: String = "76.76.21.21",
    val cnameTarget: String = "cname.sarkarijobsewa.com.cdn.cloudflare.net",
    val lastVerifiedTime: String = "2026-10-26 10:00 UTC",
    val isLiveVerified: Boolean = true
)
