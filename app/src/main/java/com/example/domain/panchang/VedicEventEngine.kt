package com.example.domain.panchang

import com.example.domain.models.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

enum class VedicEventType {
    TITHI,
    NAKSHATRA,
    YOGA,
    KARANA,
    RAHU_KAAL,
    YAMAGANDA,
    GULIKA,
    ABHIJIT,
    BRAHMA_MUHURTA,
    SUNRISE,
    SUNSET,
    OTHER
}

enum class VedicEventStatus {
    ACTIVE,
    UPCOMING,
    COMPLETED
}

data class VedicEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: VedicEventType,
    val name: String,
    val displayName: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime? = null,
    val priority: Int, // lower number = higher priority
    val status: VedicEventStatus,
    val calculationDate: LocalDate,
    val description: String = ""
)

object VedicEventEngine {

    fun calculateVikramSamvat(date: LocalDate): Int {
        val year = date.year
        return if (date.monthValue >= 4) year + 57 else year + 56
    }

    fun generateTimeline(snapshot: PanchangSnapshot, currentTime: ZonedDateTime = ZonedDateTime.now(snapshot.requestedDateTime.zone)): List<VedicEvent> {
        val events = mutableListOf<VedicEvent>()
        val calcDate = snapshot.requestedDateTime.toLocalDate()

        // 1. Tithi Event
        snapshot.tithi.let { t ->
            val start = t.startTime ?: snapshot.requestedDateTime.minusHours(6)
            val end = t.endTime ?: snapshot.requestedDateTime.plusHours(18)
            val status = when {
                currentTime.isBefore(start) -> VedicEventStatus.UPCOMING
                currentTime.isAfter(end) -> VedicEventStatus.COMPLETED
                else -> VedicEventStatus.ACTIVE
            }
            events.add(
                VedicEvent(
                    type = VedicEventType.TITHI,
                    name = "Tithi: ${t.hindiName}",
                    displayName = "तिथि: ${t.hindiName} (${if (snapshot.paksha == Paksha.SHUKLA) "शुक्ल पक्ष" else "कृष्ण पक्ष"})",
                    startTime = start,
                    endTime = end,
                    priority = 1,
                    status = status,
                    calculationDate = calcDate,
                    description = "वर्तमान तिथि समाप्त होने में"
                )
            )
        }

        // 2. Nakshatra Event
        val nakContext = snapshot.nakshatra
        val nakStart = nakContext.startTime ?: snapshot.requestedDateTime.minusHours(4)
        val nakEnd = nakContext.endTime ?: snapshot.requestedDateTime.plusHours(20)
        val nakStatus = when {
            currentTime.isBefore(nakStart) -> VedicEventStatus.UPCOMING
            currentTime.isAfter(nakEnd) -> VedicEventStatus.COMPLETED
            else -> VedicEventStatus.ACTIVE
        }
        events.add(
            VedicEvent(
                type = VedicEventType.NAKSHATRA,
                name = "Nakshatra: ${nakContext.nakshatra.sanskritName}",
                displayName = "नक्षत्र: ${nakContext.nakshatra.sanskritName} (चरण ${nakContext.pada})",
                startTime = nakStart,
                endTime = nakEnd,
                priority = 2,
                status = nakStatus,
                calculationDate = calcDate,
                description = "नक्षत्र परिवर्तन"
            )
        )

        // 3. Yoga Event
        val yogaContext = snapshot.yoga
        val yogaStart = yogaContext.startTime ?: snapshot.requestedDateTime.minusHours(5)
        val yogaEnd = yogaContext.endTime ?: snapshot.requestedDateTime.plusHours(19)
        val yogaStatus = when {
            currentTime.isBefore(yogaStart) -> VedicEventStatus.UPCOMING
            currentTime.isAfter(yogaEnd) -> VedicEventStatus.COMPLETED
            else -> VedicEventStatus.ACTIVE
        }
        events.add(
            VedicEvent(
                type = VedicEventType.YOGA,
                name = "Yoga: ${yogaContext.hindiName}",
                displayName = "योग: ${yogaContext.hindiName}",
                startTime = yogaStart,
                endTime = yogaEnd,
                priority = 4,
                status = yogaStatus,
                calculationDate = calcDate,
                description = "योग परिवर्तन"
            )
        )

        // 4. Sunrise & Sunset
        snapshot.sunrise?.let { sr ->
            val status = if (currentTime.isBefore(sr)) VedicEventStatus.UPCOMING else VedicEventStatus.COMPLETED
            events.add(
                VedicEvent(
                    type = VedicEventType.SUNRISE,
                    name = "Sunrise",
                    displayName = "सूर्योदय",
                    startTime = sr,
                    endTime = sr.plusMinutes(1),
                    priority = 3,
                    status = status,
                    calculationDate = calcDate,
                    description = "सूर्योदय समय"
                )
            )
        }

        snapshot.sunset?.let { ss ->
            val status = if (currentTime.isBefore(ss)) VedicEventStatus.UPCOMING else VedicEventStatus.COMPLETED
            events.add(
                VedicEvent(
                    type = VedicEventType.SUNSET,
                    name = "Sunset",
                    displayName = "सूर्यास्त",
                    startTime = ss,
                    endTime = ss.plusMinutes(1),
                    priority = 3,
                    status = status,
                    calculationDate = calcDate,
                    description = "सूर्यास्त समय"
                )
            )
        }

        // 5. Muhurtas
        snapshot.muhurta?.let { m ->
            m.rahukaal?.let { rk ->
                val status = when {
                    currentTime.isBefore(rk.start) -> VedicEventStatus.UPCOMING
                    currentTime.isAfter(rk.end) -> VedicEventStatus.COMPLETED
                    else -> VedicEventStatus.ACTIVE
                }
                events.add(
                    VedicEvent(
                        type = VedicEventType.RAHU_KAAL,
                        name = "Rahu Kaal",
                        displayName = "राहुकाल",
                        startTime = rk.start,
                        endTime = rk.end,
                        priority = 1,
                        status = status,
                        calculationDate = calcDate,
                        description = "राहुकाल अवधि"
                    )
                )
            }

            m.brahmaMuhurta?.let { bm ->
                val status = when {
                    currentTime.isBefore(bm.start) -> VedicEventStatus.UPCOMING
                    currentTime.isAfter(bm.end) -> VedicEventStatus.COMPLETED
                    else -> VedicEventStatus.ACTIVE
                }
                events.add(
                    VedicEvent(
                        type = VedicEventType.BRAHMA_MUHURTA,
                        name = "Brahma Muhurta",
                        displayName = "ब्रह्म मुहूर्त",
                        startTime = bm.start,
                        endTime = bm.end,
                        priority = 2,
                        status = status,
                        calculationDate = calcDate,
                        description = "ब्रह्म मुहूर्त"
                    )
                )
            }

            m.abhijitMuhurta?.let { am ->
                val status = when {
                    currentTime.isBefore(am.start) -> VedicEventStatus.UPCOMING
                    currentTime.isAfter(am.end) -> VedicEventStatus.COMPLETED
                    else -> VedicEventStatus.ACTIVE
                }
                events.add(
                    VedicEvent(
                        type = VedicEventType.ABHIJIT,
                        name = "Abhijit Muhurta",
                        displayName = "अभिजित मुहूर्त",
                        startTime = am.start,
                        endTime = am.end,
                        priority = 3,
                        status = status,
                        calculationDate = calcDate,
                        description = "अभिजित मुहूर्त"
                    )
                )
            }

            m.yamaganda?.let { ym ->
                val status = when {
                    currentTime.isBefore(ym.start) -> VedicEventStatus.UPCOMING
                    currentTime.isAfter(ym.end) -> VedicEventStatus.COMPLETED
                    else -> VedicEventStatus.ACTIVE
                }
                events.add(
                    VedicEvent(
                        type = VedicEventType.YAMAGANDA,
                        name = "Yamaganda",
                        displayName = "यमगण्ड काल",
                        startTime = ym.start,
                        endTime = ym.end,
                        priority = 3,
                        status = status,
                        calculationDate = calcDate,
                        description = "यमगण्ड काल"
                    )
                )
            }

            m.gulikaKaal?.let { gk ->
                val status = when {
                    currentTime.isBefore(gk.start) -> VedicEventStatus.UPCOMING
                    currentTime.isAfter(gk.end) -> VedicEventStatus.COMPLETED
                    else -> VedicEventStatus.ACTIVE
                }
                events.add(
                    VedicEvent(
                        type = VedicEventType.GULIKA,
                        name = "Gulika Kaal",
                        displayName = "गुलिक काल",
                        startTime = gk.start,
                        endTime = gk.end,
                        priority = 3,
                        status = status,
                        calculationDate = calcDate,
                        description = "गुलिक काल"
                    )
                )
            }
        }

        return events.sortedWith(compareBy({ it.startTime }, { it.priority }))
    }

    fun getActiveEvents(events: List<VedicEvent>, currentTime: ZonedDateTime): List<VedicEvent> {
        return events.filter { it.status == VedicEventStatus.ACTIVE || (currentTime.isAfter(it.startTime) && (it.endTime == null || currentTime.isBefore(it.endTime))) }
    }

    fun getNearestUpcomingEvent(events: List<VedicEvent>, currentTime: ZonedDateTime): VedicEvent? {
        return events.filter { it.startTime.isAfter(currentTime) || it.status == VedicEventStatus.UPCOMING }
            .minByOrNull { it.startTime }
    }

    fun getEndingSoonEvent(events: List<VedicEvent>, currentTime: ZonedDateTime): VedicEvent? {
        return events.filter { (it.status == VedicEventStatus.ACTIVE || (currentTime.isAfter(it.startTime) && it.endTime != null && currentTime.isBefore(it.endTime))) && it.endTime != null }
            .minByOrNull { it.endTime!! }
    }
}
