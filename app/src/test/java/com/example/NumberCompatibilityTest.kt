package com.example.domain.numerology

import com.example.domain.models.BirthData
import com.example.domain.models.BirthLocation
import com.example.domain.models.UserProfile
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NumberCompatibilityTest {

    private val sampleLocation = BirthLocation(
        latitude = 28.61,
        longitude = 77.20,
        placeName = "New Delhi",
        isVerified = true
    )

    private val sampleBirthData = BirthData(
        name = "Aarav Sharma",
        date = LocalDate.of(1995, 6, 15),
        time = LocalTime.of(10, 30),
        location = sampleLocation,
        timeZone = ZoneId.of("Asia/Kolkata")
    )

    private val sampleProfile = UserProfile(
        id = "test-prof-1",
        birthData = sampleBirthData
    )

    private val engine = NumerologyEngineImpl()
    private val compatibilityEngine = NumberCompatibilityEngine()

    @Test
    fun testNumberNormalizationAndReduction() {
        val norm = NumberNormalizationUtils.normalize("98765-43210", NumberCategory.MOBILE, "My Mobile")
        assertEquals("9876543210", norm.normalizedValue)
        assertEquals(NumberCategory.MOBILE, norm.category)
        assertEquals("My Mobile", norm.customLabel)
        assertTrue(norm.reducedValue in 1..9 || norm.reducedValue in setOf(11, 22, 33))
    }

    @Test
    fun testPrivacyMasking() {
        val maskedMobile = NumberNormalizationUtils.maskSensitiveNumber("9876543210", "9876543210", NumberCategory.MOBILE)
        assertTrue(maskedMobile.startsWith("XXXXXX"))
        assertTrue(maskedMobile.endsWith("3210"))

        val house = NumberNormalizationUtils.maskSensitiveNumber("Flat 402", "402", NumberCategory.HOUSE)
        assertEquals("Flat 402", house)
    }

    @Test
    fun testMobileNumberAnalysis() {
        val norm = NumberNormalizationUtils.normalize("9876598765", NumberCategory.MOBILE, "Work Mobile")
        val numerologyResult = engine.calculateForProfile(sampleProfile)
        val result = compatibilityEngine.analyze(
            norm,
            numerologyResult,
            listOf(CompatibilityPurpose.PERSONAL, CompatibilityPurpose.FINANCIAL, CompatibilityPurpose.COMMUNICATION)
        )

        assertNotNull(result)
        assertEquals("Work Mobile", result.normalizedNumber.customLabel)
        assertTrue(result.purposeReports.containsKey(CompatibilityPurpose.FINANCIAL))
        assertTrue(result.summaryHindi.contains("Aarav Sharma"))
    }

    @Test
    fun testVehicleNumberAnalysis() {
        val norm = NumberNormalizationUtils.normalize("DL01AB5678", NumberCategory.VEHICLE, "Car")
        val numerologyResult = engine.calculateForProfile(sampleProfile)
        val result = compatibilityEngine.analyze(
            norm,
            numerologyResult,
            listOf(CompatibilityPurpose.VEHICLE, CompatibilityPurpose.GENERAL)
        )

        assertNotNull(result)
        assertTrue(result.purposeReports.containsKey(CompatibilityPurpose.VEHICLE))
        assertNotNull(result.purposeReports[CompatibilityPurpose.VEHICLE]?.titleHindi)
    }

    @Test
    fun testBusinessAndCustomNumberAnalysis() {
        val normBiz = NumberNormalizationUtils.normalize("U74999DL2020PTC123456", NumberCategory.BUSINESS, "Company Reg")
        val normCust = NumberNormalizationUtils.normalize("777", NumberCategory.CUSTOM, "Lucky 7")
        val numerologyResult = engine.calculateForProfile(sampleProfile)

        val resBiz = compatibilityEngine.analyze(normBiz, numerologyResult, listOf(CompatibilityPurpose.BUSINESS))
        val resCust = compatibilityEngine.analyze(normCust, numerologyResult, listOf(CompatibilityPurpose.PERSONAL))

        assertNotNull(resBiz)
        assertNotNull(resCust)
        assertEquals("Company Reg", resBiz.normalizedNumber.customLabel)
        assertEquals("Lucky 7", resCust.normalizedNumber.customLabel)
    }

    @Test
    fun testNumberComparison() {
        val normA = NumberNormalizationUtils.normalize("9876511111", NumberCategory.MOBILE, "Mobile A")
        val normB = NumberNormalizationUtils.normalize("9876522222", NumberCategory.MOBILE, "Mobile B")
        val numerologyResult = engine.calculateForProfile(sampleProfile)

        val comparison = compatibilityEngine.compare(
            normA,
            normB,
            numerologyResult,
            listOf(CompatibilityPurpose.PERSONAL, CompatibilityPurpose.FINANCIAL)
        )

        assertNotNull(comparison)
        assertEquals("Mobile A", comparison.labelA)
        assertEquals("Mobile B", comparison.labelB)
        assertNotNull(comparison.comparisonSummaryHindi)
    }

    @Test
    fun testInvalidNumberHandling() {
        val isValid = NumberNormalizationUtils.isValidInput("")
        assertFalse(isValid)

        val normInvalid = NumberNormalizationUtils.normalize("", NumberCategory.CUSTOM, "Empty")
        assertEquals("", normInvalid.normalizedValue)
    }
}
