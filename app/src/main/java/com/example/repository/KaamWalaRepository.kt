package com.example.repository

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class KaamWalaRepository {

    // Jobs state
    private val _jobs = MutableStateFlow<List<Job>>(initialJobs())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    // Owner profile & wallet
    private val _ownerProfile = MutableStateFlow(OwnerProfile())
    val ownerProfile: StateFlow<OwnerProfile> = _ownerProfile.asStateFlow()

    // Worker profile, wallet & active job
    private val _workerProfile = MutableStateFlow(WorkerProfile())
    val workerProfile: StateFlow<WorkerProfile> = _workerProfile.asStateFlow()

    // Skill filter toggle
    private val _filterByMySkills = MutableStateFlow(true)
    val filterByMySkills: StateFlow<Boolean> = _filterByMySkills.asStateFlow()

    // Payments state
    private val _payments = MutableStateFlow<List<PaymentRecord>>(initialPayments())
    val payments: StateFlow<List<PaymentRecord>> = _payments.asStateFlow()

    // Chat messages state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(initialChatMessages())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Ad Settings state (Default frequency: 3)
    private val _adSettings = MutableStateFlow(AdSettings(jobFeedFrequency = 3))
    val adSettings: StateFlow<AdSettings> = _adSettings.asStateFlow()

    // Consent Preferences state (DPDP Act)
    private val _consentPreferences = MutableStateFlow(ConsentPreferences())
    val consentPreferences: StateFlow<ConsentPreferences> = _consentPreferences.asStateFlow()

    // Domain Migration state
    private val _domainConfig = MutableStateFlow(DomainMigrationConfig())
    val domainConfig: StateFlow<DomainMigrationConfig> = _domainConfig.asStateFlow()

    // Filtering states
    private val _selectedCategory = MutableStateFlow<JobCategory?>(null)
    val selectedCategory: StateFlow<JobCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setCategory(category: JobCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFilterByMySkills(enabled: Boolean) {
        _filterByMySkills.value = enabled
    }

    fun updateWorkerSkills(newSkills: Set<JobCategory>) {
        _workerProfile.update { it.copy(selectedSkills = newSkills) }
    }

    /**
     * Post a new task/job on Taskly.
     * Enforces:
     * 1. Minimum pay of ₹100.
     * 2. No vulgar language by owner.
     * 3. Support for "Other" custom category title.
     */
    fun addJob(
        title: String,
        description: String,
        category: JobCategory,
        customCategoryTitle: String = "",
        location: String,
        city: String,
        budget: Double,
        wageType: WageType,
        employerName: String
    ): Job {
        require(budget >= MINIMUM_JOB_PAY) {
            "Minimum pay for any job is ₹${MINIMUM_JOB_PAY.toInt()}."
        }

        require(!containsVulgarLanguage(title) && !containsVulgarLanguage(description) && !containsVulgarLanguage(customCategoryTitle)) {
            "Vulgar, offensive, or inappropriate language is strictly prohibited on Taskly."
        }

        val newJob = Job(
            id = "TSK-${System.currentTimeMillis().toString().takeLast(5)}",
            title = title,
            description = description,
            category = category,
            customCategoryTitle = if (category == JobCategory.OTHER) customCategoryTitle else "",
            location = location,
            city = city,
            pinCode = "110001",
            customerBudget = budget,
            wageType = wageType,
            durationText = if (wageType == WageType.PER_DAY) "Full Day" else "2-3 hours",
            employerName = employerName,
            isUrgent = false,
            postedTimeAgo = "Just now"
        )
        _jobs.update { listOf(newJob) + it }

        // Create initial pending payment
        val commission = newJob.commission
        val newPayment = PaymentRecord(
            id = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
            jobId = newJob.id,
            jobTitle = newJob.title,
            customerName = employerName,
            workerName = "Unassigned Worker",
            totalAmount = commission.customerBudget,
            platformCommission = commission.platformCommission,
            workerAmount = commission.workerNetPayout,
            status = PaymentStatus.PENDING_CUSTOMER_PAYMENT,
            timestamp = "Just now",
            notes = "Awaiting customer payment choice (Online Escrow or Cash on-site)."
        )
        _payments.update { listOf(newPayment) + it }
        return newJob
    }

    fun addMoneyToOwnerWallet(amount: Double) {
        require(amount > 0) { "Amount must be greater than 0" }
        _ownerProfile.update { it.copy(walletBalance = it.walletBalance + amount) }
    }

    fun addMoneyToWorkerWallet(amount: Double) {
        require(amount > 0) { "Amount must be greater than 0" }
        _workerProfile.update { profile ->
            val newBal = profile.walletBalance + amount
            profile.copy(
                walletBalance = newBal,
                isSuspendedDueToDebt = if (newBal >= 0.0) false else profile.isSuspendedDueToDebt
            )
        }
    }

    /**
     * Worker chooses/accepts work.
     * Enforces:
     * 1. Cannot choose new work if wallet balance reaches -199 rupees or debt limit until balance is >= 0.
     * 2. Cannot choose next work until owner clicks "Work Done" for current active job.
     */
    fun chooseWork(jobId: String): Result<String> {
        val profile = _workerProfile.value
        if (profile.isBlockedByNegativeBalance) {
            return Result.failure(
                IllegalStateException("Cannot choose new work: Your wallet balance has reached -₹${"%.0f".format(profile.outstandingDue)}. Add money to your wallet to make it ₹0 before taking new work.")
            )
        }
        if (profile.hasActiveJob) {
            val activeJob = _jobs.value.firstOrNull { it.id == profile.activeJobId }
            val activeTitle = activeJob?.title ?: profile.activeJobId
            return Result.failure(
                IllegalStateException("Cannot choose the next work: You already have an active job in progress ('$activeTitle'). You cannot choose new work until the owner clicks 'Work Done'.")
            )
        }

        val targetJob = _jobs.value.firstOrNull { it.id == jobId }
            ?: return Result.failure(IllegalArgumentException("Job not found."))

        if (targetJob.status != JobStatus.OPEN) {
            return Result.failure(IllegalStateException("This job is already ${targetJob.status.label.lowercase()}."))
        }

        // Assign job to worker and mark IN_PROGRESS
        _jobs.update { list ->
            list.map {
                if (it.id == jobId) {
                    it.copy(
                        status = JobStatus.IN_PROGRESS,
                        assignedWorkerId = profile.id,
                        assignedWorkerName = profile.name
                    )
                } else it
            }
        }

        // Set worker active job
        _workerProfile.update {
            it.copy(activeJobId = jobId)
        }

        return Result.success("Work chosen successfully! Current task is now in progress. Complete the work and wait for owner to click 'Work Done'.")
    }

    /**
     * Owner confirms "Work Done" button.
     * Processes payment:
     * 1. Owner Wallet: Deducts gross amount from Owner Wallet, sends net 90% into Worker Wallet, retains 10% fee.
     * 2. Cash on-site: 10% commission is deducted from Worker Wallet. If balance is 0, goes negative up to -₹200.
     *    If negative reaches -₹199, blocks new work until cleared to ₹0.
     * 3. Frees worker (clears activeJobId) so worker can choose next work.
     */
    fun markWorkDone(jobId: String, paymentType: WorkDonePaymentType): Result<String> {
        val job = _jobs.value.firstOrNull { it.id == jobId }
            ?: return Result.failure(IllegalArgumentException("Job with ID $jobId not found."))

        val grossBudget = job.customerBudget
        val commission = job.commission.platformCommission
        val workerNet = job.commission.workerNetPayout

        when (paymentType) {
            WorkDonePaymentType.OWNER_WALLET -> {
                val currentOwnerBalance = _ownerProfile.value.walletBalance
                if (currentOwnerBalance < grossBudget) {
                    val shortfall = grossBudget - currentOwnerBalance
                    return Result.failure(
                        IllegalStateException("Insufficient owner wallet balance! Current balance is ₹${currentOwnerBalance.toInt()}. Please add at least ₹${shortfall.toInt()} to owner wallet.")
                    )
                }

                // Deduct gross from owner wallet
                _ownerProfile.update { it.copy(walletBalance = it.walletBalance - grossBudget) }

                // Credit net payout to worker wallet (gross minus 10% platform commission)
                _workerProfile.update { profile ->
                    val newBal = profile.walletBalance + workerNet
                    profile.copy(
                        walletBalance = newBal,
                        activeJobId = if (profile.activeJobId == jobId) null else profile.activeJobId,
                        isSuspendedDueToDebt = if (newBal >= 0.0) false else profile.isSuspendedDueToDebt
                    )
                }

                // Mark job completed
                _jobs.update { list ->
                    list.map {
                        if (it.id == jobId) it.copy(status = JobStatus.COMPLETED) else it
                    }
                }

                // Update payment record
                _payments.update { list ->
                    val existing = list.firstOrNull { it.jobId == jobId }
                    if (existing != null) {
                        list.map {
                            if (it.jobId == jobId) {
                                it.copy(
                                    paymentMode = PaymentMode.ONLINE_ESCROW,
                                    status = PaymentStatus.TASK_COMPLETED,
                                    notes = "Owner clicked 'Work Done'. ₹${grossBudget.toInt()} paid from Owner Wallet. ₹${workerNet.toInt()} credited to Worker Wallet (10% fee ₹${commission.toInt()} deducted)."
                                )
                            } else it
                        }
                    } else {
                        val newRecord = PaymentRecord(
                            id = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
                            jobId = job.id,
                            jobTitle = job.title,
                            customerName = job.employerName,
                            workerName = job.assignedWorkerName ?: _workerProfile.value.name,
                            totalAmount = grossBudget,
                            platformCommission = commission,
                            workerAmount = workerNet,
                            paymentMode = PaymentMode.ONLINE_ESCROW,
                            status = PaymentStatus.TASK_COMPLETED,
                            timestamp = "Just now",
                            notes = "Owner clicked 'Work Done'. ₹${grossBudget.toInt()} paid from Owner Wallet. ₹${workerNet.toInt()} credited to Worker Wallet (10% fee ₹${commission.toInt()} deducted)."
                        )
                        listOf(newRecord) + list
                    }
                }

                return Result.success("Work Done confirmed! ₹${grossBudget.toInt()} debited from Owner Wallet. ₹${workerNet.toInt()} credited to Worker Wallet (10% commission deducted). Worker is now free to choose next work.")
            }

            WorkDonePaymentType.CASH -> {
                // Owner paid cash directly to worker.
                // 10% commission is deducted from worker's wallet.
                // Negative balance cannot reach minus 200 rupees (clamped at -200.0).
                // If reaches -199 rupees, cannot choose new work until balance is >= 0.
                _workerProfile.update { profile ->
                    val rawNewBal = profile.walletBalance - commission
                    val clampedBal = rawNewBal.coerceAtLeast(MAX_ALLOWED_NEGATIVE_BALANCE)
                    val nowSuspended = profile.isSuspendedDueToDebt || clampedBal <= DEBT_LOCKOUT_THRESHOLD
                    profile.copy(
                        walletBalance = clampedBal,
                        activeJobId = if (profile.activeJobId == jobId) null else profile.activeJobId,
                        isSuspendedDueToDebt = nowSuspended
                    )
                }

                // Mark job completed
                _jobs.update { list ->
                    list.map {
                        if (it.id == jobId) it.copy(status = JobStatus.COMPLETED) else it
                    }
                }

                // Update payment record
                _payments.update { list ->
                    val existing = list.firstOrNull { it.jobId == jobId }
                    if (existing != null) {
                        list.map {
                            if (it.jobId == jobId) {
                                it.copy(
                                    paymentMode = PaymentMode.CASH_ON_SITE,
                                    status = PaymentStatus.CASH_PAID_ON_SITE,
                                    notes = "Owner clicked 'Work Done' (Paid Cash). 10% commission (₹${commission.toInt()}) deducted from Worker Wallet."
                                )
                            } else it
                        }
                    } else {
                        val newRecord = PaymentRecord(
                            id = "PAY-${System.currentTimeMillis().toString().takeLast(6)}",
                            jobId = job.id,
                            jobTitle = job.title,
                            customerName = job.employerName,
                            workerName = job.assignedWorkerName ?: _workerProfile.value.name,
                            totalAmount = grossBudget,
                            platformCommission = commission,
                            workerAmount = workerNet,
                            paymentMode = PaymentMode.CASH_ON_SITE,
                            status = PaymentStatus.CASH_PAID_ON_SITE,
                            timestamp = "Just now",
                            notes = "Owner clicked 'Work Done' (Paid Cash). 10% commission (₹${commission.toInt()}) deducted from Worker Wallet."
                        )
                        listOf(newRecord) + list
                    }
                }

                val currentBal = _workerProfile.value.walletBalance
                val lockoutMsg = if (currentBal <= DEBT_LOCKOUT_THRESHOLD) {
                    " ⚠️ Warning: Worker wallet has reached ₹${"%.0f".format(currentBal)} (limit reached). Worker cannot choose new work until adding money to make wallet ₹0."
                } else {
                    " Worker is now free to choose next work."
                }

                return Result.success("Work Done confirmed! Owner paid cash. 10% platform commission (₹${commission.toInt()}) deducted from Worker Wallet. Current wallet: ₹${"%.0f".format(currentBal)}.$lockoutMsg")
            }
        }
    }

    fun processCashPayment(paymentId: String, note: String = "") {
        val payment = _payments.value.firstOrNull { it.id == paymentId } ?: return
        val commissionDue = payment.platformCommission

        _workerProfile.update { profile ->
            val rawNewBal = profile.walletBalance - commissionDue
            val clampedBal = rawNewBal.coerceAtLeast(MAX_ALLOWED_NEGATIVE_BALANCE)
            val nowSuspended = profile.isSuspendedDueToDebt || clampedBal <= DEBT_LOCKOUT_THRESHOLD
            profile.copy(
                walletBalance = clampedBal,
                isSuspendedDueToDebt = nowSuspended
            )
        }

        _payments.update { list ->
            list.map { record ->
                if (record.id == paymentId) {
                    record.copy(
                        paymentMode = PaymentMode.CASH_ON_SITE,
                        status = PaymentStatus.CASH_PAID_ON_SITE,
                        notes = "Owner paid ₹${record.totalAmount.toInt()} in full cash to worker. 10% commission (₹${commissionDue.toInt()}) deducted from worker wallet. ${note}".trim()
                    )
                } else record
            }
        }
    }

    fun payWorkerCommissionDues(amount: Double) {
        _workerProfile.update { profile ->
            val newBalance = (profile.walletBalance + amount).coerceAtMost(0.0)
            profile.copy(
                walletBalance = newBalance,
                isSuspendedDueToDebt = if (newBalance >= 0.0) false else profile.isSuspendedDueToDebt
            )
        }
    }

    fun releaseWorkerOnlinePayout(paymentId: String) {
        val payment = _payments.value.firstOrNull { it.id == paymentId } ?: return
        val workerGross = payment.workerAmount

        _workerProfile.update { profile ->
            val newBal = profile.walletBalance + workerGross
            profile.copy(
                walletBalance = newBal,
                isSuspendedDueToDebt = if (newBal >= 0.0) false else profile.isSuspendedDueToDebt
            )
        }

        _payments.update { list ->
            list.map { record ->
                if (record.id == paymentId) {
                    record.copy(
                        status = PaymentStatus.WORKER_PAYOUT_RELEASED,
                        notes = "Earnings released to worker wallet."
                    )
                } else record
            }
        }
    }

    fun submitUtrReference(paymentId: String, utr: String, customerUpi: String) {
        _payments.update { list ->
            list.map { record ->
                if (record.id == paymentId) {
                    record.copy(
                        utrNumber = utr,
                        customerUpiId = customerUpi,
                        status = PaymentStatus.PAYMENT_SUBMITTED_UTR,
                        notes = "Customer submitted UTR $utr. Pending verification against bank reconciliation."
                    )
                } else record
            }
        }
    }

    fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus, note: String = "") {
        _payments.update { list ->
            list.map { record ->
                if (record.id == paymentId) {
                    record.copy(
                        status = newStatus,
                        notes = if (note.isNotEmpty()) note else record.notes
                    )
                } else record
            }
        }
    }

    /**
     * In-App Messenger with Phone Number & Vulgar Language Monitoring
     */
    fun sendChatMessage(
        jobId: String,
        jobTitle: String,
        senderRole: ChatRole,
        senderName: String,
        messageText: String
    ): Result<ChatMessage> {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty."))
        }

        // 1. Vulgar / Abusive language detection
        if (containsVulgarLanguage(trimmed)) {
            val blockedMessage = ChatMessage(
                id = "MSG-BLK-VUL-${System.currentTimeMillis()}",
                jobId = jobId,
                jobTitle = jobTitle,
                senderName = senderName,
                senderRole = senderRole,
                text = "⚠️ [MESSAGE BLOCKED BY TASKLY SAFETY SHIELD]: Abusive or vulgar language is strictly prohibited. Keep conversations polite and professional.",
                timestamp = "Just now",
                isBlocked = true,
                blockedReason = "Vulgar / abusive language detected."
            )
            _chatMessages.update { it + blockedMessage }
            return Result.failure(SecurityException("Vulgar or abusive language is strictly prohibited on Taskly."))
        }

        // 2. Phone number detection
        if (containsPhoneNumber(trimmed)) {
            val blockedMessage = ChatMessage(
                id = "MSG-BLK-PH-${System.currentTimeMillis()}",
                jobId = jobId,
                jobTitle = jobTitle,
                senderName = senderName,
                senderRole = senderRole,
                text = "⚠️ [MESSAGE BLOCKED BY TASKLY SAFETY SHIELD]: Attempted to share phone number or personal contact details. Phone numbers are strictly prohibited to protect your safety and payment escrow.",
                timestamp = "Just now",
                isBlocked = true,
                blockedReason = "Phone number / personal contact detection triggered."
            )
            _chatMessages.update { it + blockedMessage }
            return Result.failure(SecurityException("Phone numbers cannot be shared! Keep all communications inside Taskly for your protection."))
        }

        val chatMessage = ChatMessage(
            id = "MSG-${System.currentTimeMillis()}",
            jobId = jobId,
            jobTitle = jobTitle,
            senderName = senderName,
            senderRole = senderRole,
            text = trimmed,
            timestamp = "Just now",
            isBlocked = false
        )
        _chatMessages.update { it + chatMessage }
        return Result.success(chatMessage)
    }

    // Ad Settings management
    fun updateGlobalAds(enabled: Boolean) {
        _adSettings.update { it.copy(globalAdsEnabled = enabled) }
    }

    fun updateSlotConfig(slotType: AdSlotType, isEnabled: Boolean, provider: AdProvider, adUnitId: String) {
        _adSettings.update { current ->
            when (slotType) {
                AdSlotType.HEADER_BANNER -> current.copy(
                    headerSlot = current.headerSlot.copy(
                        isEnabled = isEnabled,
                        provider = provider,
                        adUnitOrZoneId = adUnitId
                    )
                )
                AdSlotType.HOMEPAGE -> current.copy(
                    homepageSlot = current.homepageSlot.copy(
                        isEnabled = isEnabled,
                        provider = provider,
                        adUnitOrZoneId = adUnitId
                    )
                )
                AdSlotType.JOB_FEED -> current.copy(
                    jobFeedSlot = current.jobFeedSlot.copy(
                        isEnabled = isEnabled,
                        provider = provider,
                        adUnitOrZoneId = adUnitId
                    )
                )
                AdSlotType.CONTENT_INFO -> current.copy(
                    contentSlot = current.contentSlot.copy(
                        isEnabled = isEnabled,
                        provider = provider,
                        adUnitOrZoneId = adUnitId
                    )
                )
            }
        }
    }

    fun updateAdIdentifiers(publisherId: String, monetagSiteId: String, frequency: Int) {
        _adSettings.update {
            it.copy(
                adSensePublisherId = publisherId,
                monetagSiteId = monetagSiteId,
                jobFeedFrequency = frequency.coerceIn(1, 20)
            )
        }
    }

    fun togglePreviewMode(enabled: Boolean) {
        _adSettings.update { it.copy(previewModeEnabled = enabled) }
    }

    fun updateConsent(personalizedAds: Boolean, analytics: Boolean) {
        _consentPreferences.value = ConsentPreferences(
            isPromptShown = false,
            essentialAccepted = true,
            personalizedAdsAccepted = personalizedAds,
            analyticsAccepted = analytics,
            consentDate = "2026-10-26"
        )
    }

    fun toggleDomainChecklist(step: String) {
        _domainConfig.update { curr ->
            when (step) {
                "backup" -> curr.copy(isBackupCreated = !curr.isBackupCreated)
                "email" -> curr.copy(isEmailDnsPreserved = !curr.isEmailDnsPreserved)
                "ssl" -> curr.copy(isSslActive = !curr.isSslActive)
                "verify" -> curr.copy(
                    isLiveVerified = true,
                    lastVerifiedTime = "2026-10-26 10:30 UTC"
                )
                else -> curr
            }
        }
    }

    private fun initialJobs(): List<Job> = listOf(
        Job(
            id = "TSK-101",
            title = "Same-Day Retail Parcel Delivery",
            description = "Pickup 15 packages from Karol Bagh market and deliver to residential addresses in West Delhi. Fuel allowance provided upon completion.",
            category = JobCategory.DELIVERY,
            location = "Karol Bagh & Rajouri Garden",
            city = "New Delhi",
            pinCode = "110005",
            customerBudget = 800.0,
            wageType = WageType.PER_TASK,
            durationText = "4 hours",
            employerName = "Sharma Electronics & Retail",
            isUrgent = true,
            requirements = listOf("Two-wheeler required", "Valid Driving License", "Smartphone for delivery app"),
            postedTimeAgo = "15 mins ago"
        ),
        Job(
            id = "TSK-102",
            title = "Emergency Ceiling Fan & AC Board Repair",
            description = "Replace 2 burnt MCB switches and service ceiling fan in a 2BHK apartment. Tools & replacement parts available on site.",
            category = JobCategory.HOME_SERVICES,
            location = "Indiranagar, 12th Main",
            city = "Bengaluru",
            pinCode = "560038",
            customerBudget = 500.0,
            wageType = WageType.PER_TASK,
            durationText = "1.5 hours",
            employerName = "Ananya Raman",
            isUrgent = true,
            requirements = listOf("Basic electrical certification or experience", "Safety gloves", "Multimeter"),
            postedTimeAgo = "30 mins ago"
        ),
        Job(
            id = "TSK-103",
            title = "Supermarket Stocking & Display Assistant",
            description = "Organize dry grocery racks, check expiry dates, and unpack incoming crates for weekend customer rush.",
            category = JobCategory.RETAIL_HELPER,
            location = "Andheri East, MIDC",
            city = "Mumbai",
            pinCode = "400093",
            customerBudget = 750.0,
            wageType = WageType.PER_DAY,
            durationText = "Shift 10 AM - 6 PM",
            employerName = "FreshMart Supermarket",
            isUrgent = false,
            requirements = listOf("Physical fitness for lifting boxes", "Punctual", "Aadhaar Card copy"),
            postedTimeAgo = "1 hour ago",
            status = JobStatus.IN_PROGRESS,
            assignedWorkerName = "Ramesh V."
        ),
        Job(
            id = "TSK-104",
            title = "Custom Terrace Garden & Plant Pot Assembly",
            description = "Repot 25 flowering plants into ceramic containers, install drip irrigation tubes, and clear dry foliage on terrace.",
            category = JobCategory.OTHER,
            customCategoryTitle = "Terrace Gardening & Plant Care", // Demonstrating custom "Other" category!
            location = "Boring Road",
            city = "Patna",
            pinCode = "800001",
            customerBudget = 650.0,
            wageType = WageType.PER_TASK,
            durationText = "3 hours",
            employerName = "Dr. S. K. Verma",
            isUrgent = false,
            requirements = listOf("Gardening tools", "Soil handling experience"),
            postedTimeAgo = "2 hours ago",
            status = JobStatus.OPEN
        ),
        Job(
            id = "TSK-105",
            title = "Catering Helper & Food Server for Reception",
            description = "Assist catering team during an evening family banquet. Uniform apron provided at venue, dinner included.",
            category = JobCategory.EVENT_STAFF,
            location = "Gomti Nagar, Vibhuti Khand",
            city = "Lucknow",
            pinCode = "226010",
            customerBudget = 600.0,
            wageType = WageType.PER_TASK,
            durationText = "5 PM - 11 PM",
            employerName = "Awadh Heritage Banquets",
            isUrgent = false,
            requirements = listOf("Neat black trousers and white shirt", "Good interpersonal manners"),
            postedTimeAgo = "2 hours ago",
            status = JobStatus.OPEN
        ),
        Job(
            id = "TSK-106",
            title = "Deep Kitchen Cleaning & Utensil Care",
            description = "Exhaust fan degreasing, tile scrubbing, and shelf sanitization in residential home.",
            category = JobCategory.CLEANING_COOKING,
            location = "Salt Lake Sector V",
            city = "Kolkata",
            pinCode = "700091",
            customerBudget = 500.0,
            wageType = WageType.PER_TASK,
            durationText = "2 hours",
            employerName = "Banerjee Family",
            isUrgent = false,
            requirements = listOf("Cleaning chemicals knowledge", "Thorough work"),
            postedTimeAgo = "3 hours ago",
            status = JobStatus.OPEN
        ),
        Job(
            id = "TSK-107",
            title = "Warehouse Heavy Crates Unloading & Sorting",
            description = "Unload commercial truck containing hardware tools and stack neatly in designated warehouse bays.",
            category = JobCategory.LABOUR,
            location = "Transport Nagar",
            city = "Jaipur",
            pinCode = "302003",
            customerBudget = 900.0,
            wageType = WageType.PER_DAY,
            durationText = "8 hours",
            employerName = "Rajputana Logistics Depot",
            isUrgent = false,
            requirements = listOf("High physical endurance", "Safety boots recommended"),
            postedTimeAgo = "4 hours ago",
            status = JobStatus.COMPLETED,
            assignedWorkerName = "Amit K."
        ),
        Job(
            id = "TSK-108",
            title = "Quick Express Grocery Delivery",
            description = "Deliver groceries from local dark store to 2 nearby apartments within 1 km radius.",
            category = JobCategory.DELIVERY,
            location = "Sector 62",
            city = "Noida",
            pinCode = "201309",
            customerBudget = 100.0,
            wageType = WageType.PER_TASK,
            durationText = "30 mins",
            employerName = "BlinkMart Hub",
            isUrgent = true,
            requirements = listOf("Bicycle or scooter"),
            postedTimeAgo = "5 hours ago"
        )
    )

    private fun initialPayments(): List<PaymentRecord> = listOf(
        PaymentRecord(
            id = "PAY-5001",
            jobId = "TSK-102",
            jobTitle = "Emergency Ceiling Fan & AC Board Repair",
            customerName = "Ananya Raman",
            workerName = "Rakesh Kumar (Electrician)",
            totalAmount = 500.0,
            platformCommission = 50.0,
            workerAmount = 450.0,
            paymentMode = PaymentMode.ONLINE_ESCROW,
            merchantUpiId = "8360608312@mbk",
            customerUpiId = "ananya@oksbi",
            workerUpiId = "rakesh.kumar@paytm",
            utrNumber = "429810839210",
            status = PaymentStatus.VERIFIED_IN_ESCROW,
            timestamp = "Today, 09:30 AM",
            notes = "Payment of ₹500 verified in Taskly escrow. ₹450 reserved for worker."
        ),
        PaymentRecord(
            id = "PAY-5002",
            jobId = "TSK-106",
            jobTitle = "Deep Kitchen Cleaning & Utensil Care",
            customerName = "Banerjee Family",
            workerName = "Sunil Kumar",
            totalAmount = 500.0,
            platformCommission = 50.0,
            workerAmount = 450.0,
            paymentMode = PaymentMode.CASH_ON_SITE,
            status = PaymentStatus.CASH_PAID_ON_SITE,
            timestamp = "Yesterday, 06:00 PM",
            notes = "Owner paid full ₹500 in cash to worker Sunil. ₹50 platform commission added to worker negative balance."
        )
    )

    private fun initialChatMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            id = "MSG-1",
            jobId = "TSK-102",
            jobTitle = "Emergency Ceiling Fan Repair",
            senderName = "Ananya Raman",
            senderRole = ChatRole.OWNER,
            text = "Hello! Are you available to come by 2:00 PM today? We have tools on site.",
            timestamp = "10:15 AM"
        ),
        ChatMessage(
            id = "MSG-2",
            jobId = "TSK-102",
            jobTitle = "Emergency Ceiling Fan Repair",
            senderName = "Rakesh Kumar",
            senderRole = ChatRole.WORKER,
            text = "Yes, 2:00 PM works perfectly. I am bringing safety gloves and spare capacitors.",
            timestamp = "10:18 AM"
        ),
        ChatMessage(
            id = "MSG-3",
            jobId = "TSK-102",
            jobTitle = "Emergency Ceiling Fan Repair",
            senderName = "Taskly Safety Shield",
            senderRole = ChatRole.SYSTEM,
            text = "🔒 Security Notice: Personal phone numbers & vulgar language are prohibited. Chat and payments are protected by Taskly escrow.",
            timestamp = "10:19 AM"
        )
    )
}
