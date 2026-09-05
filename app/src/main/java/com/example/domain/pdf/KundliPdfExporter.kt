package com.example.domain.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.engine.VargaCalculator
import com.example.domain.models.AstrologyProfile
import com.example.domain.models.Chart
import com.example.domain.models.DashaTimeline
import com.example.domain.models.PlanetPosition
import com.example.domain.models.VargaType
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * High-precision, on-device Kundli PDF Generation Engine.
 *
 * Strict Constraints:
 * 1. Exclusively for Kundli (birth chart, divisional charts, planetary tables, Dasha).
 * 2. Zero network activity, 100% on-device local calculation rendering.
 * 3. Prominent diagonal watermark on EVERY page containing "JyotirAI" and the EXACT PDF generation timestamp.
 * 4. Zero logging of private birth details.
 */
object KundliPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width (pt)
    private const val PAGE_HEIGHT = 842 // A4 standard height (pt)

    private val TIMESTAMP_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss z")

    /**
     * Generates a multi-page Kundli PDF document.
     */
    fun generateKundliPdf(
        context: Context,
        profile: AstrologyProfile,
        activeChart: Chart,
        dashaTimeline: DashaTimeline? = null,
        generationTimestamp: ZonedDateTime = ZonedDateTime.now()
    ): File {
        val document = PdfDocument()

        try {
            val formattedTimestamp = generationTimestamp.format(TIMESTAMP_FORMATTER)

            // -------------------------------------------------------------
            // PAGE 1: Natal Information, D1 Rashi Chart & Planetary Table
            // -------------------------------------------------------------
            val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page1 = document.startPage(pageInfo1)
            val canvas1 = page1.canvas

            // 1. Draw Page 1 Content
            drawPage1Content(canvas1, profile, activeChart)

            // 2. Draw Diagonal Watermark on Page 1
            drawDiagonalWatermark(canvas1, formattedTimestamp)

            document.finishPage(page1)

            // -------------------------------------------------------------
            // PAGE 2: D9 Navamsha / Divisional Chart & Vimshottari Dasha
            // -------------------------------------------------------------
            val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val page2 = document.startPage(pageInfo2)
            val canvas2 = page2.canvas

            // 1. Draw Page 2 Content
            drawPage2Content(canvas2, profile, dashaTimeline)

            // 2. Draw Diagonal Watermark on Page 2
            drawDiagonalWatermark(canvas2, formattedTimestamp)

            document.finishPage(page2)

            // Write PDF to app cache directory
            val outputDir = File(context.cacheDir, "kundli_reports").apply { mkdirs() }
            val sanitizedName = profile.birthData.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "Kundli" }
            val outputFile = File(outputDir, "Kundli_${sanitizedName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }

            return outputFile
        } finally {
            document.close()
        }
    }

    /**
     * Draws the mandatory diagonal watermark across the page.
     */
    fun drawDiagonalWatermark(canvas: Canvas, formattedTimestamp: String) {
        val watermarkPaint = Paint().apply {
            color = Color.argb(38, 180, 140, 40) // Subtle gold tint, clearly visible without obscuring text
            textSize = 15f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        // Rotate canvas diagonally
        canvas.rotate(-38f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)

        val watermarkText = "JyotirAI • Generated: $formattedTimestamp"

        // Draw multiple diagonal repeating lines spanning the page
        val stepY = 120f
        val startY = -PAGE_HEIGHT.toFloat()
        val endY = PAGE_HEIGHT * 2f

        var currentY = startY
        while (currentY <= endY) {
            canvas.drawText(watermarkText, PAGE_WIDTH / 2f, currentY, watermarkPaint)
            currentY += stepY
        }

        canvas.restore()
    }

    private fun drawPage1Content(canvas: Canvas, profile: AstrologyProfile, activeChart: Chart) {
        val paint = Paint().apply { isAntiAlias = true }
        var yPos = 40f

        // Background
        canvas.drawColor(Color.rgb(252, 250, 246)) // Subtle warm ivory

        // Header Banner
        paint.color = Color.rgb(26, 26, 46) // Deep navy
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText("JyotirAI • Vedic Kundli Report", 36f, yPos, paint)

        paint.color = Color.rgb(180, 130, 20) // Muted gold
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("SWISS EPHEMERIS • LAHIRI AYANAMSA (CHITRA PAKSHA)", 36f, yPos + 16f, paint)

        // Divider
        yPos += 26f
        paint.color = Color.rgb(218, 200, 160)
        paint.strokeWidth = 1.5f
        canvas.drawLine(36f, yPos, PAGE_WIDTH - 36f, yPos, paint)

        // Birth Details Box
        yPos += 20f
        val bData = profile.birthData
        drawSectionHeader(canvas, "1. NATAL BIRTH DATA", 36f, yPos)
        yPos += 16f

        paint.color = Color.rgb(40, 40, 50)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        val dateStr = String.format("%02d/%02d/%04d", bData.date.dayOfMonth, bData.date.monthValue, bData.date.year)
        val timeStr = String.format("%02d:%02d", bData.time.hour, bData.time.minute)
        val locStr = "${bData.location.placeName} (${String.format("%.2f", bData.location.latitude)}°N, ${String.format("%.2f", bData.location.longitude)}°E)"
        val tzStr = bData.location.timeZoneId ?: "UTC"

        canvas.drawText("Name: ${bData.name}", 36f, yPos, paint)
        canvas.drawText("Birth Date: $dateStr", 220f, yPos, paint)
        canvas.drawText("Birth Time: $timeStr", 380f, yPos, paint)

        yPos += 14f
        canvas.drawText("Location: $locStr", 36f, yPos, paint)
        canvas.drawText("Timezone: $tzStr", 380f, yPos, paint)

        yPos += 14f
        val ascSign = com.example.domain.models.Rashi.fromIndex(profile.lagnaSignIndex)
        canvas.drawText(
            "Ascendant (Lagna): ${ascSign.sanskritName} (${ascSign.name}) ${formatDegree(profile.lagnaDegreeInSign)} • ${profile.lagnaNakshatra} (Pada ${profile.lagnaPada})",
            36f, yPos, paint
        )

        yPos += 14f
        val moonSignRashi = com.example.domain.models.Rashi.fromIndex(profile.moonSignIndex)
        canvas.drawText(
            "Moon Sign: ${moonSignRashi.sanskritName} (${moonSignRashi.name}) • Janma Nakshatra: ${profile.nakshatra} (Pada ${profile.nakshatraPada}, Lord: ${profile.nakshatraLord})",
            36f, yPos, paint
        )

        // North Indian Diamond Chart (D1 / Active Chart)
        yPos += 22f
        drawSectionHeader(canvas, "2. ${activeChart.title.uppercase()} (${activeChart.type})", 36f, yPos)
        yPos += 14f

        val chartSize = 175f
        val chartLeft = (PAGE_WIDTH - chartSize) / 2f
        drawNorthIndianChartGeometry(canvas, chartLeft, yPos, chartSize, activeChart)

        // Planetary Positions Table
        yPos += chartSize + 24f
        drawSectionHeader(canvas, "3. PLANETARY PLACEMENTS & DIGNITIES", 36f, yPos)
        yPos += 16f

        val tablePositions = if (profile.rashiChart.positions.isNotEmpty()) {
            profile.rashiChart.positions
        } else if (profile.planetPositions.isNotEmpty()) {
            profile.planetPositions
        } else {
            activeChart.positions
        }
        drawPlanetaryTable(canvas, 36f, yPos, tablePositions)
    }

    private fun drawPage2Content(canvas: Canvas, profile: AstrologyProfile, dashaTimeline: DashaTimeline?) {
        val paint = Paint().apply { isAntiAlias = true }
        var yPos = 40f

        // Background
        canvas.drawColor(Color.rgb(252, 250, 246))

        // Header
        paint.color = Color.rgb(26, 26, 46)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        canvas.drawText("JyotirAI • Divisional Charts & Dasha Timeline", 36f, yPos, paint)

        yPos += 20f
        paint.color = Color.rgb(218, 200, 160)
        paint.strokeWidth = 1.5f
        canvas.drawLine(36f, yPos, PAGE_WIDTH - 36f, yPos, paint)

        // D9 Navamsha Chart
        yPos += 22f
        drawSectionHeader(canvas, "4. NAVAMSHA (D9) SACRED GEOMETRY", 36f, yPos)
        yPos += 14f

        val navamshaChart = try {
            VargaCalculator.calculateVargaChart(profile, VargaType.D9)
        } catch (_: Exception) {
            null
        }
        val chartSize = 160f
        val chartLeft = (PAGE_WIDTH - chartSize) / 2f

        if (navamshaChart != null) {
            drawNorthIndianChartGeometry(canvas, chartLeft, yPos, chartSize, navamshaChart)
            val subPaint = Paint().apply {
                color = Color.rgb(80, 80, 95)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("D9 Navamsha Lagna: ${navamshaChart.ascendantSign} (${navamshaChart.ascendantDegreeInSign.toInt()}°)", PAGE_WIDTH / 2f, yPos + chartSize + 12f, subPaint)
        } else {
            val errorBoxPaint = Paint().apply {
                color = Color.rgb(248, 242, 242)
                style = Paint.Style.FILL
            }
            canvas.drawRect(chartLeft, yPos, chartLeft + chartSize, yPos + chartSize, errorBoxPaint)
            val unavailPaint = Paint().apply {
                color = Color.rgb(180, 50, 50)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Navamsha (D9) Unavailable", PAGE_WIDTH / 2f, yPos + (chartSize / 2f), unavailPaint)
        }

        // Vimshottari Dasha Section
        yPos += chartSize + 26f
        drawSectionHeader(canvas, "5. VIMSHOTTARI DASHA OVERVIEW", 36f, yPos)
        yPos += 16f

        paint.color = Color.rgb(40, 40, 50)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        val effectiveTimeline = dashaTimeline ?: run {
            val moonPos = profile.planetPositions.find { it.planet.equals("moon", ignoreCase = true) }
                ?: profile.rashiChart.positions.find { it.planet.equals("moon", ignoreCase = true) }
            if (moonPos != null) {
                try {
                    com.example.domain.engine.VimshottariDashaCalculator.calculateTimeline(
                        birthData = profile.birthData,
                        moonLongitude = moonPos.totalLongitude,
                        targetDateTime = ZonedDateTime.now(),
                        metadata = profile.metadata
                    )
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }

        if (effectiveTimeline != null) {
            val currentMaha = effectiveTimeline.currentMahadasha
            val currentAntar = effectiveTimeline.currentAntardasha

            val activeDashaText = "Active Period: Mahadasha of ${currentMaha?.planet?.lord ?: "N/A"} " +
                    "(Antardasha: ${currentAntar?.antardashaLord?.lord ?: "N/A"})"
            canvas.drawText(activeDashaText, 36f, yPos, paint)
            yPos += 16f

            // Table of Mahadashas (3x3 grid)
            val dashaX = 36f
            val colWidth = 172f
            var colIndex = 0

            effectiveTimeline.mahadashaPeriods.take(9).forEach { md ->
                val startYear = md.startDate.year
                val endYear = md.endDate.year
                val isActive = md.planet == currentMaha?.planet
                val activeIndicator = if (isActive) " ★" else ""
                val rowText = "${md.planet.lord}$activeIndicator: $startYear - $endYear (${md.totalDurationYears}y)"
                canvas.drawText(rowText, dashaX + (colIndex * colWidth), yPos, paint)

                colIndex++
                if (colIndex >= 3) {
                    colIndex = 0
                    yPos += 14f
                }
            }
            if (colIndex != 0) yPos += 14f
        } else {
            canvas.drawText("120-Year Vimshottari Dasha cycles calculated from Moon's natal Nakshatra.", 36f, yPos, paint)
            yPos += 14f
        }

        // Classical Vedic Interpretive Notes
        yPos += 20f
        drawSectionHeader(canvas, "6. CLASSICAL INTERPRETIVE FRAMING", 36f, yPos)
        yPos += 16f

        paint.color = Color.rgb(80, 80, 90)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

        val disclaimerLines = listOf(
            "This Kundli report is calculated deterministically using the Swiss Ephemeris engine and Chitra Paksha (Lahiri) Ayanamsa.",
            "In accordance with classical Parashari Jyotish principles, planetary influences represent karmic tendencies and guidance,",
            "serving as a contemplative tool for self-awareness, personal growth, and righteous action (Dharma) rather than fatalism."
        )

        disclaimerLines.forEach { line ->
            canvas.drawText(line, 36f, yPos, paint)
            yPos += 12f
        }
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, x: Float, y: Float) {
        val paint = Paint().apply {
            color = Color.rgb(180, 130, 20)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, x, y, paint)
    }

    private fun drawNorthIndianChartGeometry(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        chart: Chart
    ) {
        val linePaint = Paint().apply {
            color = Color.rgb(180, 130, 20)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            color = Color.rgb(245, 240, 230)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Outer square
        canvas.drawRect(x, y, x + size, y + size, fillPaint)
        canvas.drawRect(x, y, x + size, y + size, linePaint)

        // Diagonal lines (X)
        canvas.drawLine(x, y, x + size, y + size, linePaint)
        canvas.drawLine(x + size, y, x, y + size, linePaint)

        // Inner Diamond
        val half = size / 2f
        val path = Path().apply {
            moveTo(x + half, y)
            lineTo(x + size, y + half)
            lineTo(x + half, y + size)
            lineTo(x, y + half)
            close()
        }
        canvas.drawPath(path, linePaint)

        // Text paints
        val signPaint = Paint().apply {
            color = Color.rgb(180, 130, 20)
            textSize = 6.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val planetPaint = Paint().apply {
            color = Color.rgb(26, 26, 46)
            textSize = 7f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Relative coordinates (relX, relY for planets; signRelX, signRelY for sign number)
        data class HouseLayout(val house: Int, val px: Float, val py: Float, val sx: Float, val sy: Float)
        val layouts = listOf(
            HouseLayout(1, 0.50f, 0.30f, 0.50f, 0.16f),
            HouseLayout(2, 0.26f, 0.16f, 0.36f, 0.08f),
            HouseLayout(3, 0.16f, 0.26f, 0.08f, 0.36f),
            HouseLayout(4, 0.30f, 0.50f, 0.16f, 0.50f),
            HouseLayout(5, 0.16f, 0.74f, 0.08f, 0.64f),
            HouseLayout(6, 0.26f, 0.84f, 0.36f, 0.92f),
            HouseLayout(7, 0.50f, 0.70f, 0.50f, 0.84f),
            HouseLayout(8, 0.74f, 0.84f, 0.64f, 0.92f),
            HouseLayout(9, 0.84f, 0.74f, 0.92f, 0.64f),
            HouseLayout(10, 0.70f, 0.50f, 0.84f, 0.50f),
            HouseLayout(11, 0.84f, 0.26f, 0.92f, 0.36f),
            HouseLayout(12, 0.74f, 0.16f, 0.64f, 0.08f)
        )

        layouts.forEach { hl ->
            // 1. Classical Rashi sign number (1-12)
            val rashiNumber = (chart.ascendantSignIndex + (hl.house - 1)) % 12 + 1
            canvas.drawText("$rashiNumber", x + (size * hl.sx), y + (size * hl.sy), signPaint)

            // 2. Planets occupying this house
            val planets = chart.getPlanetsInHouse(hl.house)
            val planetStr = planets.joinToString(" ") { p ->
                val isRetro = p.isRetrograde && p.planet != "Rahu" && p.planet != "Ketu"
                if (isRetro) "${p.abbreviation}[R]" else p.abbreviation
            }

            if (hl.house == 1 && chart.ascendantDegreeInSign > 0.0) {
                val ascStr = "Asc ${chart.ascendantDegreeInSign.toInt()}°"
                canvas.drawText(ascStr, x + (size * hl.px), y + (size * (hl.py - 0.06f)), signPaint)
            }

            if (planetStr.isNotBlank()) {
                canvas.drawText(planetStr, x + (size * hl.px), y + (size * hl.py), planetPaint)
            }
        }
    }

    private fun drawPlanetaryTable(canvas: Canvas, startX: Float, startY: Float, planets: List<PlanetPosition>) {
        val headerPaint = Paint().apply {
            color = Color.rgb(26, 26, 46)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowPaint = Paint().apply {
            color = Color.rgb(40, 40, 50)
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(218, 200, 160)
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val colX = floatArrayOf(startX, startX + 58f, startX + 120f, startX + 212f, startX + 288f, startX + 386f, startX + 434f)
        val headers = arrayOf("Planet", "Sanskrit", "Rashi (Sign)", "Longitude", "Nakshatra", "House", "Dignity")

        var y = startY
        // Header Row
        for (i in headers.indices) {
            canvas.drawText(headers[i], colX[i], y, headerPaint)
        }
        y += 6f
        canvas.drawLine(startX, y, PAGE_WIDTH - startX, y, borderPaint)
        y += 12f

        planets.forEach { p ->
            val isRetro = p.isRetrograde && p.planet != "Rahu" && p.planet != "Ketu"
            val planetLabel = if (isRetro) "${p.planet} (R)" else p.planet
            canvas.drawText(planetLabel, colX[0], y, rowPaint)
            canvas.drawText(p.sanskritName, colX[1], y, rowPaint)
            canvas.drawText("${p.sign} (${p.rashiEnum.sanskritName})", colX[2], y, rowPaint)
            canvas.drawText(formatDegree(p.totalLongitude), colX[3], y, rowPaint)
            canvas.drawText("${p.nakshatra} - P${p.nakshatraPada}", colX[4], y, rowPaint)
            canvas.drawText("H${p.house}", colX[5], y, rowPaint)

            val dignityStr = buildString {
                if (isRetro) append("[R] ")
                append(p.dignity.displayName)
            }
            canvas.drawText(dignityStr, colX[6], y, rowPaint)

            y += 13f
        }
    }

    private fun formatDegree(degree: Double): String {
        val deg = degree.toInt()
        val min = ((degree - deg) * 60).toInt()
        return "$deg° $min'"
    }

    /**
     * Creates an Intent to share or view the generated PDF.
     */
    fun createSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
