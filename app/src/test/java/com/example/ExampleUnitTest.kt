package com.example

import com.example.model.*
import com.example.repository.KaamWalaRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMinimumPayAndCommissionBreakdown() {
        val commission = calculateCommission(500.0)
        assertEquals(500.0, commission.customerBudget, 0.01)
        assertEquals(50.0, commission.platformCommission, 0.01)
        assertEquals(450.0, commission.workerNetPayout, 0.01)
        assertTrue(commission.isValidMinimumPay)

        val minCommission = calculateCommission(100.0)
        assertEquals(10.0, minCommission.platformCommission, 0.01)
        assertEquals(90.0, minCommission.workerNetPayout, 0.01)
        assertTrue(minCommission.isValidMinimumPay)
    }

    @Test
    fun testOwnerWalletAndWorkerWalletOnlinePaymentFlow() {
        val repository = KaamWalaRepository()
        // Owner starts with ₹2500, worker starts with -₹50 (from seeded cash task PAY-5002)
        assertEquals(2500.0, repository.ownerProfile.value.walletBalance, 0.01)
        assertEquals(-50.0, repository.workerProfile.value.walletBalance, 0.01)

        // Clear initial ₹50 dues to start clean at ₹0
        repository.payWorkerCommissionDues(50.0)
        assertEquals(0.0, repository.workerProfile.value.walletBalance, 0.01)

        // Owner adds ₹500 to wallet
        repository.addMoneyToOwnerWallet(500.0)
        assertEquals(3000.0, repository.ownerProfile.value.walletBalance, 0.01)

        // Worker chooses job TSK-102 (Budget ₹500)
        val chooseResult = repository.chooseWork("TSK-102")
        assertTrue(chooseResult.isSuccess)
        assertEquals("TSK-102", repository.workerProfile.value.activeJobId)

        // Owner confirms Work Done via Owner Wallet
        val doneResult = repository.markWorkDone("TSK-102", WorkDonePaymentType.OWNER_WALLET)
        assertTrue(doneResult.isSuccess)

        // Owner wallet should be debited gross ₹500 -> ₹2500
        assertEquals(2500.0, repository.ownerProfile.value.walletBalance, 0.01)
        // Worker wallet should be credited net 90% (₹450) after 10% fee (₹50)
        assertEquals(450.0, repository.workerProfile.value.walletBalance, 0.01)
        // Worker active job cleared
        assertNull(repository.workerProfile.value.activeJobId)
    }

    @Test
    fun testCashPaymentDeductsCommissionAndGoesNegativeUpToMinus200() {
        val repository = KaamWalaRepository()
        // Reset worker balance to 0 for controlled test
        repository.payWorkerCommissionDues(50.0)
        assertEquals(0.0, repository.workerProfile.value.walletBalance, 0.01)

        // Worker chooses job TSK-101 (Budget ₹800, 10% fee = ₹80)
        repository.chooseWork("TSK-101")

        // Owner pays cash on-site and clicks Work Done
        val doneResult = repository.markWorkDone("TSK-101", WorkDonePaymentType.CASH)
        assertTrue(doneResult.isSuccess)

        // Worker wallet balance drops by ₹80 from 0 -> -₹80.0
        assertEquals(-80.0, repository.workerProfile.value.walletBalance, 0.01)
        assertFalse(repository.workerProfile.value.isBlockedByNegativeBalance)

        // Another cash job: TSK-103 (Budget ₹750, 10% fee = ₹75)
        repository.chooseWork("TSK-103")
        repository.markWorkDone("TSK-103", WorkDonePaymentType.CASH)

        // Worker balance drops by ₹75 -> -₹155.0
        assertEquals(-155.0, repository.workerProfile.value.walletBalance, 0.01)
        assertFalse(repository.workerProfile.value.isBlockedByNegativeBalance)

        // Another cash job: TSK-106 (Budget ₹500, 10% fee = ₹50)
        repository.chooseWork("TSK-106")
        repository.markWorkDone("TSK-106", WorkDonePaymentType.CASH)

        // -155 - 50 = -205, but negative amount cannot exceed -200 (capped at -200.0)
        assertEquals(-200.0, repository.workerProfile.value.walletBalance, 0.01)

        // Balance reached -199 or worse (-200): worker is blocked from choosing new work
        assertTrue(repository.workerProfile.value.isBlockedByNegativeBalance)
        assertFalse(repository.workerProfile.value.canAcceptNewWork)

        // Trying to choose new work should fail
        val blockedChoose = repository.chooseWork("TSK-105")
        assertTrue(blockedChoose.isFailure)

        // Worker adds money to wallet to bring balance back to zero
        repository.addMoneyToWorkerWallet(200.0)
        assertEquals(0.0, repository.workerProfile.value.walletBalance, 0.01)
        assertFalse(repository.workerProfile.value.isBlockedByNegativeBalance)
        assertTrue(repository.workerProfile.value.canAcceptNewWork)

        // Now worker can choose new work again!
        val unblockedChoose = repository.chooseWork("TSK-105")
        assertTrue(unblockedChoose.isSuccess)
    }

    @Test
    fun testSequentialWorkDoneGate() {
        val repository = KaamWalaRepository()

        // 1. Worker chooses first job TSK-101
        val res1 = repository.chooseWork("TSK-101")
        assertTrue(res1.isSuccess)
        assertEquals("TSK-101", repository.workerProfile.value.activeJobId)

        // 2. Worker attempts to choose a second job while TSK-101 is in progress
        val res2 = repository.chooseWork("TSK-102")
        assertTrue(res2.isFailure)
        assertTrue(res2.exceptionOrNull()?.message?.contains("active job in progress") == true)

        // 3. Owner clicks "Work Done" for TSK-101
        val doneResult = repository.markWorkDone("TSK-101", WorkDonePaymentType.OWNER_WALLET)
        assertTrue(doneResult.isSuccess)
        assertNull(repository.workerProfile.value.activeJobId)

        // 4. Now that owner clicked "Work Done", worker is free to choose the next work!
        val res3 = repository.chooseWork("TSK-102")
        assertTrue(res3.isSuccess)
        assertEquals("TSK-102", repository.workerProfile.value.activeJobId)
    }

    @Test
    fun testSupportEmailAndJobStatusIndicators() {
        // Verify official query support email
        assertEquals("sarkarijobsewa@gmail.com", TASKLY_SUPPORT_EMAIL)

        val repository = KaamWalaRepository()
        val allJobs = repository.jobs.value

        // Verify that jobs have status indicators for clarity on task availability
        val openJobs = allJobs.filter { it.status == JobStatus.OPEN }
        val inProgressJobs = allJobs.filter { it.status == JobStatus.IN_PROGRESS }
        val completedJobs = allJobs.filter { it.status == JobStatus.COMPLETED }

        assertTrue("Should have open jobs available for workers", openJobs.isNotEmpty())
        assertTrue("Should have in-progress job demonstration", inProgressJobs.isNotEmpty())
        assertTrue("Should have completed job demonstration", completedJobs.isNotEmpty())

        assertEquals("Open", JobStatus.OPEN.label)
        assertEquals("In Progress", JobStatus.IN_PROGRESS.label)
        assertEquals("Completed", JobStatus.COMPLETED.label)
    }

    @Test
    fun testMonetagAdConfiguration() {
        val repository = KaamWalaRepository()
        val adSettings = repository.adSettings.value

        // Verify Monetag domain and Zone ID from uploaded sw.js
        assertEquals("3nbf4.com", adSettings.monetagDomain)
        assertEquals("10831624", adSettings.monetagZoneId)

        // Verify all ad slots are configured for Monetag
        assertEquals(AdProvider.MONETAG, adSettings.headerSlot.provider)
        assertEquals(AdProvider.MONETAG, adSettings.homepageSlot.provider)
        assertEquals(AdProvider.MONETAG, adSettings.jobFeedSlot.provider)
        assertEquals(AdProvider.MONETAG, adSettings.contentSlot.provider)

        assertEquals("10831624", adSettings.headerSlot.adUnitOrZoneId)
        assertEquals("10831624", adSettings.homepageSlot.adUnitOrZoneId)
        assertEquals("10831624", adSettings.jobFeedSlot.adUnitOrZoneId)
        assertEquals("10831624", adSettings.contentSlot.adUnitOrZoneId)
    }
}
