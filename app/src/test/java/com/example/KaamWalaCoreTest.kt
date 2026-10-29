package com.example

import com.example.model.*
import com.example.repository.KaamWalaRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KaamWalaCoreTest {

    private lateinit var repository: KaamWalaRepository

    @Before
    fun setUp() {
        repository = KaamWalaRepository()
    }

    @Test
    fun testBenchmark10PercentCommissionCalculation() {
        // Benchmark test:
        // "For a ₹500 task, show a proposed ₹50 commission and ₹450 worker amount"
        val breakdown500 = calculateCommission(500.0, 10.0)
        assertEquals(500.0, breakdown500.customerBudget, 0.001)
        assertEquals(10.0, breakdown500.platformRatePercentage, 0.001)
        assertEquals(50.0, breakdown500.platformCommission, 0.001)
        assertEquals(450.0, breakdown500.workerNetPayout, 0.001)
        assertTrue(breakdown500.isValidMinimumPay)

        // For minimum ₹100 task
        val breakdown100 = calculateCommission(100.0, 10.0)
        assertEquals(10.0, breakdown100.platformCommission, 0.001)
        assertEquals(90.0, breakdown100.workerNetPayout, 0.001)
        assertTrue(breakdown100.isValidMinimumPay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMinimumPayRequirementRejectsUnder100() {
        repository.addJob(
            title = "Small Task Under Minimum",
            description = "Quick errand",
            category = JobCategory.MICRO_TASK,
            location = "Delhi",
            city = "New Delhi",
            budget = 80.0, // Invalid: under ₹100!
            wageType = WageType.PER_TASK,
            employerName = "Test Employer"
        )
    }

    @Test
    fun testOtherCategoryOptionWithCustomWorkTitle() {
        // Requirement 2:
        // "if work needed to be done done is not mentioned in the like, make another option called 'Other' where owner can write the job he needs to be done"
        val customJob = repository.addJob(
            title = "Weekend Dog Walking & Feeding",
            description = "Walk two golden retrievers in the park morning and evening.",
            category = JobCategory.OTHER,
            customCategoryTitle = "Dog Walking & Pet Care",
            location = "Indiranagar",
            city = "Bengaluru",
            budget = 400.0,
            wageType = WageType.PER_TASK,
            employerName = "Ananya"
        )

        assertEquals(JobCategory.OTHER, customJob.category)
        assertEquals("Dog Walking & Pet Care", customJob.customCategoryTitle)
        assertEquals("Dog Walking & Pet Care", customJob.categoryDisplayName)
    }

    @Test
    fun testVulgarLanguageFilterInChatAndJobPost() {
        // Requirement 3: "Make sure there is no use of vulgar language in the app by owner or worker"

        // 1. Polite chat message succeeds
        val politeResult = repository.sendChatMessage(
            jobId = "TSK-101",
            jobTitle = "Delivery",
            senderRole = ChatRole.WORKER,
            senderName = "Sunil",
            messageText = "Good morning, I will arrive at your address on time."
        )
        assertTrue(politeResult.isSuccess)

        // 2. Chat message with vulgar language is BLOCKED
        assertTrue(containsVulgarLanguage("You are a bhenchod"))
        assertTrue(containsVulgarLanguage("What bullshit is this"))
        assertTrue(containsVulgarLanguage("Stop doing chutiya things"))

        val blockedResult = repository.sendChatMessage(
            jobId = "TSK-101",
            jobTitle = "Delivery",
            senderRole = ChatRole.OWNER,
            senderName = "Bad Actor",
            messageText = "Get here quickly you bastard"
        )
        assertTrue(blockedResult.isFailure)
        assertTrue(blockedResult.exceptionOrNull() is SecurityException)

        // Verify blocked message saved with safety shield reason
        val lastMsg = repository.chatMessages.value.last()
        assertTrue(lastMsg.isBlocked)
        assertTrue(lastMsg.blockedReason.contains("Vulgar"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVulgarLanguageInJobPostThrowsException() {
        // Vulgar language in task creation is blocked
        repository.addJob(
            title = "Need someone to clean this shit",
            description = "Work is very bad",
            category = JobCategory.CLEANING_COOKING,
            location = "Delhi",
            city = "New Delhi",
            budget = 500.0,
            wageType = WageType.PER_TASK,
            employerName = "Tester"
        )
    }

    @Test
    fun testAdPlacementFrequencyIs3() {
        // Requirement 4: "Make sure there is ad placement after 3 ads ."
        assertEquals(3, repository.adSettings.value.jobFeedFrequency)
    }

    @Test
    fun testCashPaymentAndNegativeBalanceLimitRule() {
        // Reset worker balance to 0
        repository.payWorkerCommissionDues(50.0)
        assertEquals(0.0, repository.workerProfile.value.commissionBalance, 0.001)

        val job1 = repository.addJob(
            title = "Warehouse Job 1",
            description = "Stacking crates",
            category = JobCategory.LABOUR,
            location = "Jaipur",
            city = "Jaipur",
            budget = 1000.0,
            wageType = WageType.PER_DAY,
            employerName = "Depot"
        )
        val p1 = repository.payments.value.first { it.jobId == job1.id }
        repository.processCashPayment(p1.id)
        assertEquals(-100.0, repository.workerProfile.value.commissionBalance, 0.001)
        assertFalse(repository.workerProfile.value.isSuspendedForDues)

        val job2 = repository.addJob(
            title = "Warehouse Job 2",
            description = "Stacking crates round 2",
            category = JobCategory.LABOUR,
            location = "Jaipur",
            city = "Jaipur",
            budget = 1000.0,
            wageType = WageType.PER_DAY,
            employerName = "Depot"
        )
        val p2 = repository.payments.value.first { it.jobId == job2.id }
        repository.processCashPayment(p2.id)

        // Balance reaches -200: Worker is suspended!
        assertEquals(-200.0, repository.workerProfile.value.commissionBalance, 0.001)
        assertTrue(repository.workerProfile.value.isSuspendedForDues)

        // Pay dues restores account
        repository.payWorkerCommissionDues(200.0)
        assertEquals(0.0, repository.workerProfile.value.commissionBalance, 0.001)
        assertFalse(repository.workerProfile.value.isSuspendedForDues)
    }
}
