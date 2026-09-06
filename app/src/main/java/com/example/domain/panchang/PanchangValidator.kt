package com.example.domain.panchang

import com.example.domain.models.PanchangLocationContext
import com.example.domain.models.PanchangResult
import com.example.domain.models.PanchangResultState
import java.time.LocalDate

/**
 * Validates Panchang inputs and calculation outputs.
 * Detects edge cases, polar conditions, and assigns appropriate result states.
 */
object PanchangValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val state: PanchangResultState,
        val limitations: List<String> = emptyList(),
        val errorMessage: String? = null
    )

    fun validateInput(date: LocalDate, location: PanchangLocationContext): ValidationResult {
        val limitations = mutableListOf<String>()

        if (date.year < PanchangDateResolver.MIN_SUPPORTED_YEAR || date.year > PanchangDateResolver.MAX_SUPPORTED_YEAR) {
            return ValidationResult(
                isValid = false,
                state = PanchangResultState.INSUFFICIENT_DATA,
                errorMessage = "Selected year ${date.year} is outside supported ephemeris range (1900-2100)."
            )
        }

        if (!LocationContextResolver.isValidCoordinates(location.latitude, location.longitude)) {
            return ValidationResult(
                isValid = false,
                state = PanchangResultState.CALCULATION_ERROR,
                errorMessage = "Invalid coordinates: (${location.latitude}, ${location.longitude})."
            )
        }

        if (kotlin.math.abs(location.latitude) > 65.0) {
            limitations.add("High latitude (${location.latitude}°): Sunrise/Sunset may be subject to polar day/night conditions.")
        }

        return ValidationResult(
            isValid = true,
            state = if (limitations.isNotEmpty()) PanchangResultState.LIMITED_DATA else PanchangResultState.SUCCESS,
            limitations = limitations
        )
    }

    fun validateOutput(result: PanchangResult): PanchangResultState {
        val limitations = result.calculationLimitations.toMutableList()

        if (result.tithi.index !in 1..30) return PanchangResultState.CALCULATION_ERROR
        if (result.nakshatra.nakshatra.index !in 0..26) return PanchangResultState.CALCULATION_ERROR
        if (result.nakshatra.pada !in 1..4) return PanchangResultState.CALCULATION_ERROR
        if (result.yoga.index !in 1..27) return PanchangResultState.CALCULATION_ERROR
        if (result.karana.index !in 1..60) return PanchangResultState.CALCULATION_ERROR

        if (result.sunrise == null || result.sunset == null) {
            return PanchangResultState.LIMITED_DATA
        }

        return if (limitations.isNotEmpty()) PanchangResultState.LIMITED_DATA else PanchangResultState.SUCCESS
    }
}
