package com.example.data.engine

import com.example.domain.engine.AstrologyEngine
import com.example.domain.engine.TransitCalculator
import com.example.domain.engine.VargaCalculator
import com.example.domain.engine.VimshottariDashaCalculator
import com.example.domain.engine.PanchangCalculator
import com.example.domain.models.*
import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SweDate
import de.thmac.swisseph.SwissEph
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Authoritative, deterministic Vedic Astrology calculation engine powered by Swiss Ephemeris.
 * 
 * Calculation Methodology:
 * - Zodiac: Sidereal (Nirayana)
 * - Ayanamsa: Lahiri / Chitra Paksha (SE_SIDM_LAHIRI)
 * - Ephemeris: Moshier offline high-precision analytical ephemeris
 * - Time: UTC conversion from authoritative ZoneId with DST awareness
 * - Houses: Vedic Whole Sign System (Rashi Bhava Chakra) relative to calculated Sidereal Lagna
 * - Lunar Mansions: 27 Nakshatras with 4 Padas each (13°20' span, 3°20' pada span)
 * - Nodes: Mean Lunar Node (Rahu) and exact 180° opposite (Ketu)
 * - Dasha: Authoritative Vimshottari Dasha derived from calculated sidereal Moon longitude
 */
class SwissEphAstrologyEngine : AstrologyEngine {

    // Thread-safe caching for zero-redundancy computation
    private val profileCache = ConcurrentHashMap<BirthData, AstrologyProfile>()
    private val chartCache = ConcurrentHashMap<Pair<BirthData, String>, Chart>()
    private val dashaCache = ConcurrentHashMap<Pair<BirthData, String>, DashaTimeline>()
    private val transitCache = ConcurrentHashMap<String, TransitSnapshot>()

    // ThreadLocal SwissEph instance for thread-safe computations without lock contention
    private val swissEphThreadLocal = ThreadLocal.withInitial {
        val swe = SwissEph()
        swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        swe
    }

    override suspend fun calculateProfile(birthData: BirthData): Result<AstrologyProfile> {
        return try {
            // Check cache
            profileCache[birthData]?.let { return Result.success(it) }

            // Validate birth data
            validateBirthData(birthData)

            val zonedDateTime = ZonedDateTime.of(birthData.date, birthData.time, birthData.timeZone)
            val utcDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)

            val hourDecimalUt = utcDateTime.hour +
                    (utcDateTime.minute / 60.0) +
                    (utcDateTime.second / 3600.0) +
                    (utcDateTime.nano / 3_600_000_000_000.0)

            val sweDate = SweDate(
                utcDateTime.year,
                utcDateTime.monthValue,
                utcDateTime.dayOfMonth,
                hourDecimalUt
            )
            val tjdUt = sweDate.julDay

            val swe = swissEphThreadLocal.get()
            swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)

            val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED

            // 1. Calculate Ayanamsa
            val ayanamsa = swe.swe_get_ayanamsa_ut(tjdUt)

            // 2. Calculate Lagna (Ascendant) & Houses
            val cusps = DoubleArray(13)
            val ascmc = DoubleArray(10)
            val houseResult = swe.swe_houses(
                tjdUt,
                flags,
                birthData.location.latitude,
                birthData.location.longitude,
                'P'.code,
                cusps,
                ascmc
            )
            if (houseResult != SweConst.OK) {
                return Result.failure(AppError.CalculationError("Failed to calculate Ascendant / Houses"))
            }

            val lagnaLongitude = normalizeDegree(ascmc[0])
            val lagnaRashi = Rashi.fromLongitude(lagnaLongitude)
            val lagnaDegreeInSign = lagnaLongitude % 30.0
            val (lagnaNakshatraEnum, lagnaPada) = Nakshatra.fromLongitude(lagnaLongitude)

            // 3. Calculate 9 Planetary Bodies (Navagraha)
            val bodiesToCalculate: List<Pair<String, Int>> = listOf(
                Pair("Sun", SweConst.SE_SUN),
                Pair("Moon", SweConst.SE_MOON),
                Pair("Mars", SweConst.SE_MARS),
                Pair("Mercury", SweConst.SE_MERCURY),
                Pair("Jupiter", SweConst.SE_JUPITER),
                Pair("Venus", SweConst.SE_VENUS),
                Pair("Saturn", SweConst.SE_SATURN),
                Pair("Rahu", SweConst.SE_MEAN_NODE)
            )

            val planetPositions = mutableListOf<PlanetPosition>()
            val xx = DoubleArray(6)
            val serr = StringBuffer()

            var moonLongitude = 0.0
            var rahuLongitude = 0.0
            var rahuSpeed = 0.0

            for ((name, bodyId) in bodiesToCalculate) {
                val ret = swe.swe_calc_ut(tjdUt, bodyId, flags, xx, serr)
                if (ret < 0) {
                    return Result.failure(AppError.CalculationError("Error calculating $name: $serr"))
                }
                val lon = normalizeDegree(xx[0])
                val speed = xx[3]
                val rashi = Rashi.fromLongitude(lon)
                val degreeInSign = lon % 30.0
                val (nakshatraEnum, pada) = Nakshatra.fromLongitude(lon)
                val house = calculateWholeSignHouse(rashi.index, lagnaRashi.index)
                val isRetrograde = speed < 0

                if (bodyId == SweConst.SE_MOON) {
                    moonLongitude = lon
                }
                if (bodyId == SweConst.SE_MEAN_NODE) {
                    rahuLongitude = lon
                    rahuSpeed = speed
                }

                planetPositions.add(
                    PlanetPosition(
                        planet = name,
                        sign = "${rashi.sanskritName} (${rashi.englishName})",
                        signIndex = rashi.index,
                        totalLongitude = lon,
                        degreeInSign = degreeInSign,
                        house = house,
                        isRetrograde = isRetrograde,
                        nakshatra = nakshatraEnum.sanskritName,
                        nakshatraLord = nakshatraEnum.lord,
                        nakshatraPada = pada,
                        speed = speed
                    )
                )
            }

            // 4. Calculate Ketu (Exactly 180° opposite to Rahu)
            val ketuLongitude = normalizeDegree(rahuLongitude + 180.0)
            val ketuRashi = Rashi.fromLongitude(ketuLongitude)
            val ketuDegreeInSign = ketuLongitude % 30.0
            val (ketuNakshatraEnum, ketuPada) = Nakshatra.fromLongitude(ketuLongitude)
            val ketuHouse = calculateWholeSignHouse(ketuRashi.index, lagnaRashi.index)

            planetPositions.add(
                PlanetPosition(
                    planet = "Ketu",
                    sign = "${ketuRashi.sanskritName} (${ketuRashi.englishName})",
                    signIndex = ketuRashi.index,
                    totalLongitude = ketuLongitude,
                    degreeInSign = ketuDegreeInSign,
                    house = ketuHouse,
                    isRetrograde = true, // Mean Ketu is always retrograde
                    nakshatra = ketuNakshatraEnum.sanskritName,
                    nakshatraLord = ketuNakshatraEnum.lord,
                    nakshatraPada = ketuPada,
                    speed = rahuSpeed
                )
            )

            // Moon Sign and Nakshatra
            val moonRashi = Rashi.fromLongitude(moonLongitude)
            val (moonNakshatraEnum, moonPada) = Nakshatra.fromLongitude(moonLongitude)

            val rashiChart = Chart(
                type = "D1",
                positions = planetPositions,
                vargaType = VargaType.D1,
                title = "D1 — Rashi (Natal Chart)",
                sanskritTitle = "Rashi Chakra",
                description = "Foundational life blueprint, physical constitution & vitality",
                ascendantSign = "${lagnaRashi.sanskritName} (${lagnaRashi.englishName})",
                ascendantSignIndex = lagnaRashi.index,
                ascendantDegreeInSign = lagnaDegreeInSign,
                ascendantNakshatra = lagnaNakshatraEnum.sanskritName,
                ascendantPada = lagnaPada
            )

            val metadata = CalculationMetadata(
                ayanamsaDegree = ayanamsa,
                julianDayUt = tjdUt,
                calculatedUtcIso = utcDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )

            val profile = AstrologyProfile(
                birthData = birthData,
                rashiChart = rashiChart,
                lagna = "${lagnaRashi.sanskritName} (${lagnaRashi.englishName})",
                lagnaSignIndex = lagnaRashi.index,
                lagnaLongitude = lagnaLongitude,
                lagnaDegreeInSign = lagnaDegreeInSign,
                lagnaNakshatra = lagnaNakshatraEnum.sanskritName,
                lagnaPada = lagnaPada,
                moonSign = "${moonRashi.sanskritName} (${moonRashi.englishName})",
                moonSignIndex = moonRashi.index,
                nakshatra = moonNakshatraEnum.sanskritName,
                nakshatraPada = moonPada,
                nakshatraLord = moonNakshatraEnum.lord,
                planetPositions = planetPositions,
                metadata = metadata
            )

            // Cache result
            profileCache[birthData] = profile
            Result.success(profile)
        } catch (e: AppError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AppError.CalculationError(e.message ?: "Calculation error occurred"))
        }
    }

    override suspend fun calculateChart(birthData: BirthData, chartType: String): Result<Chart> {
        val vargaType = VargaType.fromCode(chartType)
        val cacheKey = Pair(birthData, vargaType.code)
        chartCache[cacheKey]?.let { return Result.success(it) }

        val profileResult = calculateProfile(birthData)
        if (profileResult.isFailure) {
            return Result.failure(profileResult.exceptionOrNull() ?: AppError.CalculationError("Profile calculation failed"))
        }

        val profile = profileResult.getOrThrow()
        val chart = if (vargaType == VargaType.D1) {
            profile.rashiChart
        } else {
            VargaCalculator.calculateVargaChart(profile, vargaType)
        }
        chartCache[cacheKey] = chart
        return Result.success(chart)
    }

    override suspend fun calculateDashaTimeline(
        birthData: BirthData,
        targetDateTime: ZonedDateTime?
    ): Result<DashaTimeline> {
        val targetZoned = targetDateTime ?: ZonedDateTime.now(birthData.timeZone)
        val cacheKey = Pair(birthData, targetZoned.toString())
        dashaCache[cacheKey]?.let { return Result.success(it) }

        val profileResult = calculateProfile(birthData)
        if (profileResult.isFailure) {
            return Result.failure(
                profileResult.exceptionOrNull() ?: AppError.CalculationError("Profile calculation failed")
            )
        }

        val profile = profileResult.getOrThrow()
        val moonPosition = profile.planetPositions.firstOrNull { it.planet == "Moon" }
            ?: return Result.failure(AppError.CalculationError("Moon position not found in calculated profile"))

        return try {
            val timeline = VimshottariDashaCalculator.calculateTimeline(
                birthData = birthData,
                moonLongitude = moonPosition.totalLongitude,
                targetDateTime = targetZoned,
                metadata = profile.metadata
            )
            dashaCache[cacheKey] = timeline
            Result.success(timeline)
        } catch (e: Exception) {
            Result.failure(AppError.CalculationError(e.message ?: "Failed to calculate Dasha timeline"))
        }
    }

    override suspend fun calculatePanchang(
        date: ZonedDateTime,
        location: BirthLocation
    ): Result<com.example.domain.models.PanchangSnapshot> {
        return try {
            val snapshot = PanchangCalculator.calculatePanchang(date, location, swissEphThreadLocal.get())
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(AppError.CalculationError(e.message ?: "Failed to calculate Panchang"))
        }
    }
    override suspend fun calculateTransitSnapshot(
        transitDateTime: ZonedDateTime,
        location: BirthLocation,
        natalProfile: AstrologyProfile?
    ): Result<TransitSnapshot> {
        return try {
            validateTransitInput(transitDateTime, location)

            val natalRefKey = if (natalProfile != null) "${natalProfile.moonSignIndex}_${natalProfile.lagnaSignIndex}" else "none"
            val cacheKey = "${transitDateTime.toInstant().toEpochMilli()}_${location.latitude}_${location.longitude}_${location.altitudeMeters}_$natalRefKey"
            transitCache[cacheKey]?.let { return Result.success(it) }

            val utcDateTime = transitDateTime.withZoneSameInstant(ZoneOffset.UTC)

            val hourDecimalUt = utcDateTime.hour +
                    (utcDateTime.minute / 60.0) +
                    (utcDateTime.second / 3600.0) +
                    (utcDateTime.nano / 3_600_000_000_000.0)

            val sweDate = SweDate(
                utcDateTime.year,
                utcDateTime.monthValue,
                utcDateTime.dayOfMonth,
                hourDecimalUt
            )
            val tjdUt = sweDate.julDay

            val swe = swissEphThreadLocal.get()
            swe.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)

            val flags = SweConst.SEFLG_MOSEPH or SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED

            // 1. Calculate Ayanamsa
            val ayanamsa = swe.swe_get_ayanamsa_ut(tjdUt)

            // 2. Calculate Transit Ascendant (Lagna) at the given location
            val cusps = DoubleArray(13)
            val ascmc = DoubleArray(10)
            val houseResult = swe.swe_houses(
                tjdUt,
                flags,
                location.latitude,
                location.longitude,
                'P'.code,
                cusps,
                ascmc
            )
            val transitLagnaLongitude = if (houseResult == SweConst.OK) normalizeDegree(ascmc[0]) else null
            val transitLagnaRashi = transitLagnaLongitude?.let { Rashi.fromLongitude(it) }
            val transitLagnaDegreeInSign = transitLagnaLongitude?.let { it % 30.0 }

            val bodiesToCalculate: List<Pair<String, Int>> = listOf(
                Pair("Sun", SweConst.SE_SUN),
                Pair("Moon", SweConst.SE_MOON),
                Pair("Mars", SweConst.SE_MARS),
                Pair("Mercury", SweConst.SE_MERCURY),
                Pair("Jupiter", SweConst.SE_JUPITER),
                Pair("Venus", SweConst.SE_VENUS),
                Pair("Saturn", SweConst.SE_SATURN),
                Pair("Rahu", SweConst.SE_MEAN_NODE)
            )

            val transitPositions = mutableListOf<TransitPosition>()
            val xx = DoubleArray(6)
            val serr = StringBuffer()

            var rahuLongitude = 0.0
            var rahuSpeed = 0.0

            val natalMoonSignIndex = natalProfile?.moonSignIndex
            val natalLagnaSignIndex = natalProfile?.lagnaSignIndex

            for ((name, bodyId) in bodiesToCalculate) {
                val ret = swe.swe_calc_ut(tjdUt, bodyId, flags, xx, serr)
                if (ret < 0) {
                    return Result.failure(AppError.CalculationError("Error calculating transit for $name: $serr"))
                }
                val lon = normalizeDegree(xx[0])
                val speed = xx[3]
                val rashi = Rashi.fromLongitude(lon)
                val degreeInSign = lon % 30.0
                val (nakshatraEnum, pada) = Nakshatra.fromLongitude(lon)
                val isRetrograde = TransitCalculator.isPlanetRetrograde(name, speed)

                val houseFromMoon = natalMoonSignIndex?.let { TransitCalculator.calculateRelativeHouse(rashi.index, it) }
                val houseFromLagna = natalLagnaSignIndex?.let { TransitCalculator.calculateRelativeHouse(rashi.index, it) }

                if (bodyId == SweConst.SE_MEAN_NODE) {
                    rahuLongitude = lon
                    rahuSpeed = speed
                }

                transitPositions.add(
                    TransitPosition(
                        planet = name,
                        totalLongitude = lon,
                        sign = "${rashi.sanskritName} (${rashi.englishName})",
                        signIndex = rashi.index,
                        degreeInSign = degreeInSign,
                        isRetrograde = isRetrograde,
                        speed = speed,
                        nakshatra = nakshatraEnum.sanskritName,
                        nakshatraLord = nakshatraEnum.lord,
                        nakshatraPada = pada,
                        houseFromMoon = houseFromMoon,
                        houseFromLagna = houseFromLagna
                    )
                )
            }

            // Ketu is exactly 180° opposite to Rahu
            val ketuLongitude = normalizeDegree(rahuLongitude + 180.0)
            val ketuRashi = Rashi.fromLongitude(ketuLongitude)
            val ketuDegreeInSign = ketuLongitude % 30.0
            val (ketuNakshatraEnum, ketuPada) = Nakshatra.fromLongitude(ketuLongitude)
            val ketuHouseFromMoon = natalMoonSignIndex?.let { TransitCalculator.calculateRelativeHouse(ketuRashi.index, it) }
            val ketuHouseFromLagna = natalLagnaSignIndex?.let { TransitCalculator.calculateRelativeHouse(ketuRashi.index, it) }

            transitPositions.add(
                TransitPosition(
                    planet = "Ketu",
                    totalLongitude = ketuLongitude,
                    sign = "${ketuRashi.sanskritName} (${ketuRashi.englishName})",
                    signIndex = ketuRashi.index,
                    degreeInSign = ketuDegreeInSign,
                    isRetrograde = true,
                    speed = rahuSpeed,
                    nakshatra = ketuNakshatraEnum.sanskritName,
                    nakshatraLord = ketuNakshatraEnum.lord,
                    nakshatraPada = ketuPada,
                    houseFromMoon = ketuHouseFromMoon,
                    houseFromLagna = ketuHouseFromLagna
                )
            )

            val metadata = CalculationMetadata(
                ephemerisEngine = "Swiss Ephemeris (Moshier Sidereal)",
                ayanamsaName = "Lahiri (Chitra Paksha)",
                ayanamsaDegree = ayanamsa,
                houseSystem = "Vedic Whole Sign (Rashi Bhava)",
                julianDayUt = tjdUt,
                calculatedUtcIso = utcDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            )

            val natalReference = natalProfile?.let {
                NatalTransitReference(
                    moonSign = it.moonSign,
                    moonSignIndex = it.moonSignIndex,
                    moonNakshatra = "${it.nakshatra} (Pada ${it.nakshatraPada})",
                    lagnaSign = it.lagna,
                    lagnaSignIndex = it.lagnaSignIndex,
                    lagnaDegreeInSign = it.lagnaDegreeInSign
                )
            }

            val snapshot = TransitSnapshot(
                transitDateTime = transitDateTime,
                location = location,
                positions = transitPositions,
                metadata = metadata,
                natalReference = natalReference,
                transitAscendantSign = transitLagnaRashi?.let { "${it.sanskritName} (${it.englishName})" },
                transitAscendantSignIndex = transitLagnaRashi?.index,
                transitAscendantDegree = transitLagnaDegreeInSign
            )

            transitCache[cacheKey] = snapshot
            Result.success(snapshot)
        } catch (e: AppError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AppError.CalculationError(e.message ?: "Failed to calculate planetary transits"))
        }
    }

    private fun validateTransitInput(dateTime: ZonedDateTime, location: BirthLocation) {
        if (location.latitude !in -90.0..90.0) {
            throw AppError.InvalidBirthData("Latitude must be between -90 and 90")
        }
        if (location.longitude !in -180.0..180.0) {
            throw AppError.InvalidBirthData("Longitude must be between -180 and 180")
        }
        if (dateTime.year !in 1000..3000) {
            throw AppError.InvalidBirthData("Transit year must be within reasonable astronomical range (1000-3000)")
        }
    }

    private fun validateBirthData(birthData: BirthData) {
        if (birthData.location.latitude !in -90.0..90.0) {
            throw AppError.InvalidBirthData("Latitude must be between -90 and 90")
        }
        if (birthData.location.longitude !in -180.0..180.0) {
            throw AppError.InvalidBirthData("Longitude must be between -180 and 180")
        }
        if (birthData.date.year !in 1000..3000) {
            throw AppError.InvalidBirthData("Year must be within reasonable astronomical range (1000-3000)")
        }
    }

    private fun normalizeDegree(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun calculateWholeSignHouse(planetSignIndex: Int, lagnaSignIndex: Int): Int {
        return ((planetSignIndex - lagnaSignIndex).mod(12)) + 1
    }
}
