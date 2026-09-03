package com.example.domain.models

sealed class AppError : Exception() {
    class InvalidBirthData(override val message: String) : AppError()
    class CalculationError(override val message: String) : AppError()
    class AIInterpretationError(override val message: String) : AppError()
    class PersistenceError(override val message: String) : AppError()
}
