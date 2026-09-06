package com.example.domain.muhurta

import com.example.domain.models.AppError
import com.example.domain.models.MuhurtaRequest
import com.example.domain.panchang.LocationContextResolver
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Validates Muhurta search requests, ensuring safe date boundaries,
 * valid coordinates, and correct range limitations.
 */
object MuhurtaRequestValidator {

    private const val MAX_DATE_RANGE_DAYS = 31L
    private val MIN_SUPPORTED_DATE = LocalDate.of(1900, 1, 1)
    private val MAX_SUPPORTED_DATE = LocalDate.of(2100, 12, 31)

    fun validate(request: MuhurtaRequest) {
        if (!LocationContextResolver.isValidCoordinates(request.location.latitude, request.location.longitude)) {
            throw AppError.InvalidBirthData(
                "Invalid location coordinates: lat=${request.location.latitude}, lon=${request.location.longitude}. Must be within [-90, +90] and [-180, +180]."
            )
        }

        if (request.startDate.isBefore(MIN_SUPPORTED_DATE) || request.startDate.isAfter(MAX_SUPPORTED_DATE)) {
            throw AppError.InvalidBirthData(
                "Start date ${request.startDate} is out of supported astronomical range (1900-2100)."
            )
        }

        if (request.endDate.isBefore(MIN_SUPPORTED_DATE) || request.endDate.isAfter(MAX_SUPPORTED_DATE)) {
            throw AppError.InvalidBirthData(
                "End date ${request.endDate} is out of supported astronomical range (1900-2100)."
            )
        }

        if (request.endDate.isBefore(request.startDate)) {
            throw AppError.InvalidBirthData(
                "End date ${request.endDate} cannot be before start date ${request.startDate}."
            )
        }

        val daysDifference = ChronoUnit.DAYS.between(request.startDate, request.endDate)
        if (daysDifference > MAX_DATE_RANGE_DAYS) {
            throw AppError.InvalidBirthData(
                "Date range exceeds maximum search span of $MAX_DATE_RANGE_DAYS days (requested $daysDifference days). Please narrow search window."
            )
        }
    }
}
