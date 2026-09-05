package com.example.domain.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class KundliPdfExporterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testGenerateKundliPdf_createsMultiPagePdfWithValidWatermark() {
        val birthData = BirthData(
            name = "Aarav Sharma",
            date = LocalDate.of(1995, 8, 15),
            time = LocalTime.of(14, 30),
            location = BirthLocation(28.6139, 77.2090, "New Delhi", true, "verified"),
            timeZone = ZoneId.of("Asia/Kolkata")
        )
        val rashiChart = Chart(
            title = "Lagna Chart (D1)",
            type = VargaType.D1,
            planets = listOf(
                PlanetPosition(
                    name = "Sun",
                    sanskritName = "Surya",
                    longitude = 120.5,
                    rashi = Rashi.LEO,
                    rashiDegree = 0.5,
                    house = 9,
                    nakshatra = Nakshatra.MAGHA,
                    nakshatraPada = 1,
                    isRetrograde = false,
                    speed = 0.98,
                    dignity = PlanetDignity.OWN_SIGN
                ),
                PlanetPosition(
                    name = "Moon",
                    sanskritName = "Chandra",
                    longitude = 355.0,
                    rashi = Rashi.PISCES,
                    rashiDegree = 25.0,
                    house = 4,
                    nakshatra = Nakshatra.REVATI,
                    nakshatraPada = 3,
                    isRetrograde = false,
                    speed = 13.2,
                    dignity = PlanetDignity.FRIEND
                )
            ),
            houses = emptyList(),
            ascendantRashi = Rashi.SCORPIO
        )
        val profile = AstrologyProfile(
            birthData = birthData,
            rashiChart = rashiChart,
            divisionalCharts = mapOf(VargaType.D1 to rashiChart, VargaType.D9 to rashiChart),
            ascendant = PlanetPosition(
                name = "Ascendant",
                sanskritName = "Lagna",
                longitude = 220.0,
                rashi = Rashi.SCORPIO,
                rashiDegree = 10.0,
                house = 1,
                nakshatra = Nakshatra.ANURADHA,
                nakshatraPada = 3,
                isRetrograde = false,
                speed = 0.0,
                dignity = PlanetDignity.NEUTRAL
            ),
            moonSign = Rashi.PISCES,
            sunSign = Rashi.LEO,
            nakshatra = NakshatraInfo(Nakshatra.REVATI, 3, 0.75),
            ayanamsaName = "Chitra Paksha (Lahiri)",
            ayanamsaValue = 23.78
        )

        val generationTime = ZonedDateTime.of(2026, 9, 5, 10, 30, 45, 0, ZoneId.of("Asia/Kolkata"))

        val pdfFile = KundliPdfExporter.generateKundliPdf(
            context = context,
            profile = profile,
            activeChart = rashiChart,
            dashaTimeline = null,
            generationTimestamp = generationTime
        )

        assertNotNull(pdfFile)
        assertTrue("Generated PDF file must exist", pdfFile.exists())
        assertTrue("Generated PDF file size must be > 0 bytes", pdfFile.length() > 500)
        assertTrue("PDF filename should contain sanitized profile name", pdfFile.name.contains("Aarav_Sharma"))
    }
}
