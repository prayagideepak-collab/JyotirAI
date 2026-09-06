package com.example.domain.panchang

import com.example.domain.models.Paksha
import com.example.domain.models.PanchangResult
import com.example.domain.models.PanchangSnapshot
import java.time.format.DateTimeFormatter

/**
 * Hindi Presentation Layer for Panchang data.
 * Keeps calculation logic completely language-agnostic while providing natural,
 * accurate Hindi translations for UI display and TTS speech.
 */
object PanchangHindiPresenter {

    private val TIME_FORMATTER_HI = DateTimeFormatter.ofPattern("hh:mm a")

    fun formatTithi(result: PanchangResult): String {
        val pakshaStr = if (result.paksha == Paksha.SHUKLA) "शुक्ल पक्ष" else "कृष्ण पक्ष"
        return "${result.tithi.hindiName} ($pakshaStr)"
    }

    fun formatNakshatra(result: PanchangResult): String {
        return "${result.nakshatra.nakshatra.sanskritName} (चरण ${result.nakshatra.pada})"
    }

    fun formatVara(result: PanchangResult): String {
        return "${result.vara.hindiName} (${result.vara.sanskritName})"
    }

    fun formatYoga(result: PanchangResult): String {
        return result.yoga.hindiName
    }

    fun formatKarana(result: PanchangResult): String {
        return result.karana.hindiName
    }

    fun formatSunSign(result: PanchangResult): String {
        return result.sunContext.sign.sanskritName
    }

    fun formatMoonSign(result: PanchangResult): String {
        return result.moonContext.sign.sanskritName
    }

    fun formatSunrise(result: PanchangResult): String {
        return result.sunrise?.format(TIME_FORMATTER_HI) ?: "उपलब्ध नहीं"
    }

    fun formatSunset(result: PanchangResult): String {
        return result.sunset?.format(TIME_FORMATTER_HI) ?: "उपलब्ध नहीं"
    }

    fun formatSpeechSummary(result: PanchangResult): String {
        return formatSpeechSummary(result.toSnapshot())
    }

    fun formatSpeechSummary(snapshot: PanchangSnapshot): String {
        val sb = StringBuilder()
        sb.append("दैनिक पंचांग विवरण। ")
        sb.append("स्थान: ${snapshot.location.placeName}। ")
        sb.append("वार: ${snapshot.vara.hindiName}। ")
        sb.append("तिथि: ${snapshot.tithi.hindiName}, ${if (snapshot.paksha == Paksha.SHUKLA) "शुक्ल पक्ष" else "कृष्ण पक्ष"}। ")
        sb.append("नक्षत्र: ${snapshot.nakshatra.nakshatra.sanskritName}, चरण ${snapshot.nakshatra.pada}। ")
        sb.append("योग: ${snapshot.yoga.name}। ")
        sb.append("करण: ${snapshot.karana.name}। ")

        if (snapshot.sunrise != null) {
            sb.append("सूर्योदय: ${snapshot.sunrise.format(TIME_FORMATTER_HI)}। ")
        }
        if (snapshot.sunset != null) {
            sb.append("सूर्यास्त: ${snapshot.sunset.format(TIME_FORMATTER_HI)}। ")
        }

        snapshot.muhurta?.let { muhurta ->
            muhurta.brahmaMuhurta?.let { bm ->
                sb.append("ब्रह्म मुहूर्त: ${bm.start.format(TIME_FORMATTER_HI)} से ${bm.end.format(TIME_FORMATTER_HI)}। ")
            }
            muhurta.abhijitMuhurta?.let { am ->
                sb.append("अभिजित मुहूर्त: ${am.start.format(TIME_FORMATTER_HI)} से ${am.end.format(TIME_FORMATTER_HI)}। ")
            }
            muhurta.rahukaal?.let { rk ->
                sb.append("राहुकाल: ${rk.start.format(TIME_FORMATTER_HI)} से ${rk.end.format(TIME_FORMATTER_HI)}। ")
            }
        }

        return sb.toString().trim()
    }
}
