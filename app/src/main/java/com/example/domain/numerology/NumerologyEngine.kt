package com.example.domain.numerology

import com.example.domain.models.UserProfile
import java.time.LocalDate

/**
 * Interface contract for the Deterministic Numerology Calculation Engine.
 */
interface NumerologyEngine {

    /**
     * Calculates complete numerology profile for a given birth date and optional profile details.
     */
    fun calculate(
        birthDate: LocalDate,
        profileId: String? = null,
        profileName: String = "User",
        inputName: String? = null,
        methodology: NumerologyMethodology = NumerologyMethodology.CHALDEAN
    ): NumerologyResult

    /**
     * Convenience method for calculating directly from a UserProfile.
     */
    fun calculateForProfile(
        profile: UserProfile,
        methodology: NumerologyMethodology = NumerologyMethodology.CHALDEAN
    ): NumerologyResult

    /**
     * Clears cached numerology results for a profile or invalidated data.
     */
    fun invalidateCache(profileId: String?)

    /**
     * Clears entire numerology cache.
     */
    fun clearCache()
}

/**
 * Deterministic implementation of the Numerology Engine.
 * Supports Chaldean and Pythagorean models, master numbers (11, 22, 33), and profile-isolated caching.
 */
class NumerologyEngineImpl : NumerologyEngine {

    private val cache = mutableMapOf<String, NumerologyResult>()

    override fun calculate(
        birthDate: LocalDate,
        profileId: String?,
        profileName: String,
        inputName: String?,
        methodology: NumerologyMethodology
    ): NumerologyResult {
        // Cache key includes profileId, birthDate, inputName, methodology, and engine version
        val cacheKey = buildCacheKey(profileId, birthDate, inputName, methodology)
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        val resultId = "NUM-${birthDate.toEpochDay()}-${methodology.code}-${profileId ?: "anon"}"

        // 1. Birth Number / Day Number (Moolank)
        val day = birthDate.dayOfMonth
        val (birthNumReduced, birthNumSteps) = NumberReducer.reduce(day, preserveMasterNumbers = true)
        val birthRoot = NumberReducer.getRootSingleDigit(birthNumReduced)
        val isBirthMaster = birthNumReduced in setOf(11, 22, 33)
        val birthSig = NumerologyInterpretations.getSignification(birthRoot)

        val (birthTitleHindi, birthDescHindi) = if (isBirthMaster) {
            NumerologyInterpretations.getMasterNumberDescription(birthNumReduced)
        } else {
            Pair(birthSig.titleHindi, birthSig.descriptionHindi)
        }

        val birthNumberObj = NumerologyNumber(
            title = "Birth Number (Moolank)",
            sanskritTitle = "मूलांक (जन्म दिवस अंक)",
            hindiName = "मूलांक $birthNumReduced",
            finalNumber = birthNumReduced,
            rootSingleDigit = birthRoot,
            isMasterNumber = isBirthMaster,
            rulingPlanet = birthSig.rulingPlanet,
            rulingPlanetHindi = birthSig.rulingPlanetHindi,
            calculationExpression = "Day $day -> $birthNumReduced",
            reductionSteps = birthNumSteps,
            summaryHindi = birthSig.summaryHindi,
            descriptionHindi = birthDescHindi,
            keyStrengthsHindi = birthSig.strengthsHindi,
            cautionaryGuidanceHindi = birthSig.cautionsHindi
        )

        // 2. Life Path Number (Bhagyank / Destiny Path)
        val month = birthDate.monthValue
        val year = birthDate.year
        val fullDateSum = day + month + year
        val (lifePathReduced, lifePathSteps) = NumberReducer.reduce(fullDateSum, preserveMasterNumbers = true)
        val lifePathRoot = NumberReducer.getRootSingleDigit(lifePathReduced)
        val isLifePathMaster = lifePathReduced in setOf(11, 22, 33)
        val lifePathSig = NumerologyInterpretations.getSignification(lifePathRoot)

        val (lifePathTitleHindi, lifePathDescHindi) = if (isLifePathMaster) {
            NumerologyInterpretations.getMasterNumberDescription(lifePathReduced)
        } else {
            Pair(
                "भाग्यांक $lifePathReduced — जीवन पथ एवं कर्म दिशा",
                lifePathSig.descriptionHindi
            )
        }

        val lifePathNumberObj = NumerologyNumber(
            title = "Life Path Number (Bhagyank)",
            sanskritTitle = "भाग्यांक (जीवन पथ अंक)",
            hindiName = "भाग्यांक $lifePathReduced",
            finalNumber = lifePathReduced,
            rootSingleDigit = lifePathRoot,
            isMasterNumber = isLifePathMaster,
            rulingPlanet = lifePathSig.rulingPlanet,
            rulingPlanetHindi = lifePathSig.rulingPlanetHindi,
            calculationExpression = "$day + $month + $year = $fullDateSum -> $lifePathReduced",
            reductionSteps = lifePathSteps,
            summaryHindi = lifePathSig.summaryHindi,
            descriptionHindi = lifePathDescHindi,
            keyStrengthsHindi = lifePathSig.strengthsHindi,
            cautionaryGuidanceHindi = lifePathSig.cautionsHindi
        )

        // 3. Name Number (Namank) - Optional if name is present
        val effectiveName = inputName?.takeIf { it.isNotBlank() } ?: profileName.takeIf { it.isNotBlank() }
        val nameCalc = effectiveName?.let { NameNumerologyResolver.calculate(it, methodology) }
        val nameNumberObj = if (nameCalc != null) {
            val nameSig = NumerologyInterpretations.getSignification(nameCalc.rootSingleDigit)
            NumerologyNumber(
                title = "Name Number (Namank)",
                sanskritTitle = "नामांक (ध्वनि कंपन अंक)",
                hindiName = "नामांक ${nameCalc.reducedNumber}",
                finalNumber = nameCalc.reducedNumber,
                rootSingleDigit = nameCalc.rootSingleDigit,
                isMasterNumber = nameCalc.isMasterNumber,
                rulingPlanet = nameSig.rulingPlanet,
                rulingPlanetHindi = nameSig.rulingPlanetHindi,
                calculationExpression = "${nameCalc.cleanName} (${nameCalc.characterValues.joinToString("+") { "${it.first}:${it.second}" }}) = ${nameCalc.rawSum} -> ${nameCalc.reducedNumber}",
                reductionSteps = nameCalc.reductionSteps,
                summaryHindi = "नाम का कुल ध्वनि कंपन अंक ${nameCalc.reducedNumber} (${nameSig.rulingPlanetHindi}) है।",
                descriptionHindi = nameSig.descriptionHindi,
                keyStrengthsHindi = nameSig.strengthsHindi,
                cautionaryGuidanceHindi = nameSig.cautionsHindi
            )
        } else null

        // 4. Attitude Number / Sun Number (Day + Month)
        val dayMonthSum = day + month
        val (attReduced, attSteps) = NumberReducer.reduce(dayMonthSum, preserveMasterNumbers = false)
        val attSig = NumerologyInterpretations.getSignification(attReduced)
        val attitudeNumberObj = NumerologyNumber(
            title = "Attitude / Sun Number",
            sanskritTitle = "सूर्य अंक (स्वभाव प्रवृत्ति)",
            hindiName = "सूर्य अंक $attReduced",
            finalNumber = attReduced,
            rootSingleDigit = attReduced,
            isMasterNumber = false,
            rulingPlanet = attSig.rulingPlanet,
            rulingPlanetHindi = attSig.rulingPlanetHindi,
            calculationExpression = "$day + $month = $dayMonthSum -> $attReduced",
            reductionSteps = attSteps,
            summaryHindi = "प्राथमिक स्वभाव दृष्टिकोण एवं बाहरी संपर्क शैली अंक $attReduced से प्रभावित है।",
            descriptionHindi = attSig.descriptionHindi,
            keyStrengthsHindi = attSig.strengthsHindi,
            cautionaryGuidanceHindi = attSig.cautionsHindi
        )

        // Synthesize favorable alignments
        val favorableNumbers = (birthSig.favorableNumbers + lifePathSig.favorableNumbers).distinct()
        val neutralNumbers = (birthSig.neutralNumbers + lifePathSig.neutralNumbers).distinct().filterNot { favorableNumbers.contains(it) }
        val challengingNumbers = (birthSig.challengingNumbers + lifePathSig.challengingNumbers).distinct().filterNot { favorableNumbers.contains(it) }

        val limitations = mutableListOf<String>()
        if (nameCalc != null && nameCalc.unmappedCharacters.isNotEmpty()) {
            limitations.add("नाम में कुछ गैर-मानक अक्षरों को छोड़ दिया गया: ${nameCalc.unmappedCharacters.joinToString(", ")}")
        }

        val result = NumerologyResult(
            resultId = resultId,
            profileId = profileId,
            profileName = profileName,
            birthDate = birthDate,
            inputName = effectiveName,
            methodology = methodology,
            calculationEngineVersion = "1.0.0-phase12",
            calculationState = NumerologyCalculationState.SUCCESS,
            birthNumber = birthNumberObj,
            lifePathNumber = lifePathNumberObj,
            nameNumber = nameNumberObj,
            attitudeNumber = attitudeNumberObj,
            favorableNumbers = favorableNumbers,
            neutralNumbers = neutralNumbers,
            challengingNumbers = challengingNumbers,
            favorableDaysHindi = (birthSig.favorableDaysHindi + lifePathSig.favorableDaysHindi).distinct(),
            favorableColorsHindi = (birthSig.favorableColorsHindi + lifePathSig.favorableColorsHindi).distinct(),
            traditionalRemediesHindi = (birthSig.remediesHindi + lifePathSig.remediesHindi).distinct(),
            limitations = limitations
        )

        synchronized(cache) {
            cache[cacheKey] = result
        }

        return result
    }

    override fun calculateForProfile(
        profile: UserProfile,
        methodology: NumerologyMethodology
    ): NumerologyResult {
        return calculate(
            birthDate = profile.date,
            profileId = profile.id,
            profileName = profile.name,
            inputName = profile.name,
            methodology = methodology
        )
    }

    override fun invalidateCache(profileId: String?) {
        if (profileId == null) return
        synchronized(cache) {
            cache.keys.filter { it.contains("pid=$profileId") }.forEach { cache.remove(it) }
        }
    }

    override fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }

    private fun buildCacheKey(
        profileId: String?,
        birthDate: LocalDate,
        inputName: String?,
        methodology: NumerologyMethodology
    ): String {
        return "pid=${profileId ?: "none"}|date=$birthDate|name=${inputName ?: ""}|meth=${methodology.code}"
    }
}
