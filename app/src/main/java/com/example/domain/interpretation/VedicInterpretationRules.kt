package com.example.domain.interpretation

import com.example.domain.models.*

/**
 * Classical Vedic Astrology rules and interpretive dictionaries derived from
 * foundational treatises (Brihat Parashara Hora Shastra, Phaladeepika, Saravali).
 *
 * All interpretations are framed educationally, contextually, and non-fatalistically.
 */
object VedicInterpretationRules {

    // Natural Planetary Relationships (Mitra, Sama, Shatru) according to Parashara
    // Map: Planet -> (Friends, Neutrals, Enemies)
    private val NATURAL_RELATIONSHIPS: Map<String, Triple<List<String>, List<String>, List<String>>> = mapOf(
        "Sun" to Triple(listOf("Moon", "Mars", "Jupiter"), listOf("Mercury"), listOf("Venus", "Saturn", "Rahu", "Ketu")),
        "Moon" to Triple(listOf("Sun", "Mercury"), listOf("Mars", "Jupiter", "Venus", "Saturn"), listOf("Rahu", "Ketu")),
        "Mars" to Triple(listOf("Sun", "Moon", "Jupiter"), listOf("Venus", "Saturn"), listOf("Mercury", "Rahu")),
        "Mercury" to Triple(listOf("Sun", "Venus"), listOf("Mars", "Jupiter", "Saturn"), listOf("Moon")),
        "Jupiter" to Triple(listOf("Sun", "Moon", "Mars"), listOf("Saturn"), listOf("Mercury", "Venus")),
        "Venus" to Triple(listOf("Mercury", "Saturn"), listOf("Mars", "Jupiter"), listOf("Sun", "Moon")),
        "Saturn" to Triple(listOf("Mercury", "Venus"), listOf("Jupiter"), listOf("Sun", "Moon", "Mars")),
        "Rahu" to Triple(listOf("Venus", "Saturn"), listOf("Mercury", "Jupiter"), listOf("Sun", "Moon", "Mars")),
        "Ketu" to Triple(listOf("Sun", "Mars", "Jupiter"), listOf("Mercury", "Venus"), listOf("Moon", "Saturn"))
    )

    fun getNaturalRelationship(planetA: String, planetB: String): String {
        if (planetA.equals(planetB, ignoreCase = true)) return "Self-Ruled / Harmonious"
        val rels = NATURAL_RELATIONSHIPS[planetA.capitalizeFirst()] ?: return "Neutral"
        val b = planetB.capitalizeFirst()
        return when {
            rels.first.contains(b) -> "Natural Friend (Mitra)"
            rels.third.contains(b) -> "Natural Adversary (Shatru)"
            else -> "Neutral (Sama)"
        }
    }

    fun isFriendlyRelationship(planetA: String, planetB: String): Boolean {
        if (planetA.equals(planetB, ignoreCase = true)) return true
        val rels = NATURAL_RELATIONSHIPS[planetA.capitalizeFirst()] ?: return false
        return rels.first.contains(planetB.capitalizeFirst())
    }

    fun isEnemyRelationship(planetA: String, planetB: String): Boolean {
        if (planetA.equals(planetB, ignoreCase = true)) return false
        val rels = NATURAL_RELATIONSHIPS[planetA.capitalizeFirst()] ?: return false
        return rels.third.contains(planetB.capitalizeFirst())
    }

    // 1. Lagna Interpretations (12 Signs)
    fun getLagnaInterpretation(rashi: Rashi): Pair<String, String> = when (rashi) {
        Rashi.ARIES -> Pair(
            "Mesha (Aries) Ascendant — Dynamic Initiative & Vitality",
            "As a fiery movable sign ruled by Mars (Mangala), Aries Lagna endows a pioneering spirit, courage, and keen ambition. You thrive on self-initiated projects, physical vitality, and direct action. Cultivating steady patience enhances your natural leadership."
        )
        Rashi.TAURUS -> Pair(
            "Vrishabha (Taurus) Ascendant — Grounded Stability & Aesthetic Grace",
            "An earthy fixed sign ruled by Venus (Shukra), Taurus Lagna bestows emotional equanimity, aesthetic sensitivity, and an innate appreciation for stability and sustained resources. Your strength lies in patient endurance and deliberate cultivation of security."
        )
        Rashi.GEMINI -> Pair(
            "Mithuna (Gemini) Ascendant — Inquisitive Intellect & Versatility",
            "An airy dual sign governed by Mercury (Budha), Gemini Lagna grants intellectual agility, expressive communication, and versatile curiosity. You excel at analytical synthesis and interpersonal exchange, benefiting from focused channelization of your varied interests."
        )
        Rashi.CANCER -> Pair(
            "Karka (Cancer) Ascendant — Empathic Intuition & Protective Nurture",
            "A watery movable sign ruled by the Moon (Chandra), Cancer Lagna imparts deep emotional receptivity, maternal protectiveness, and strong devotion to family. Your intuitive perception is keen, blooming when balanced with emotional grounding."
        )
        Rashi.LEO -> Pair(
            "Simha (Leo) Ascendant — Noble Sovereignty & Creative Radiance",
            "A fiery fixed sign governed by the Sun (Surya), Leo Lagna reflects dignity, self-respect, moral warmth, and organizational leadership. You possess a natural presence of authority and generosity, inspiring others through principled conduct."
        )
        Rashi.VIRGO -> Pair(
            "Kanya (Virgo) Ascendant — Discerning Analysis & Methodical Service",
            "An earthy dual sign ruled by Mercury (Budha), Virgo Lagna confers sharp discernment, practical methodology, and a dedication to service and refinement. You possess a meticulous eye for order, thriving when self-criticism is moderated with self-compassion."
        )
        Rashi.LIBRA -> Pair(
            "Tula (Libra) Ascendant — Harmonious Equilibrium & Relational Wisdom",
            "An airy movable sign ruled by Venus (Shukra), Libra Lagna bestows an instinctive orientation toward justice, diplomatic harmony, and balanced relationships. You seek equilibrium in all domains, excelling in collaborative endeavors."
        )
        Rashi.SCORPIO -> Pair(
            "Vrishchika (Scorpio) Ascendant — Deep Introspection & Transformative Resolve",
            "A watery fixed sign ruled by Mars (Mangala) and Ketu, Scorpio Lagna imparts penetrating insight, unwavering endurance, and a transformative inner life. You perceive beneath superficial veneers and possess immense regenerative resilience."
        )
        Rashi.SAGITTARIUS -> Pair(
            "Dhanu (Sagittarius) Ascendant — Dharmic Vision & Philosophical Aspiration",
            "A fiery dual sign governed by Jupiter (Guru), Sagittarius Lagna inspires philosophical breadth, devotion to truth, and noble aspirations. You are motivated by higher principles, broad horizons, and righteous endeavor."
        )
        Rashi.CAPRICORN -> Pair(
            "Makara (Capricorn) Ascendant — Pragmatic Duty & Tenacious Achievement",
            "An earthy movable sign ruled by Saturn (Shani), Capricorn Lagna manifests patience, structural discipline, and perseverance through duty. You build lasting foundations through method and responsibility, gaining stature through time."
        )
        Rashi.AQUARIUS -> Pair(
            "Kumbha (Aquarius) Ascendant — Humanitarian Intellect & Progressive Vision",
            "An airy fixed sign governed by Saturn (Shani) and Rahu, Aquarius Lagna imparts universal consciousness, systemic thinking, and egalitarian ideals. You look toward collective progress and intellectual innovation with objective poise."
        )
        Rashi.PISCES -> Pair(
            "Meena (Pisces) Ascendant — Spiritual Transcendence & Boundless Compassion",
            "A watery dual sign governed by Jupiter (Guru), Pisces Lagna confers intuitive spiritual depth, imaginative richness, and universal empathy. You resonate with transcendent dimensions, finding fulfillment through charitable and introspective pursuits."
        )
    }

    // 2. Lagna Lord in 12 Houses
    fun getLagnaLordInHouseInterpretation(lord: String, house: Int): Pair<String, String> {
        val title = "Lagna Lord ($lord) in House $house"
        val desc = when (house) {
            1 -> "Lagna lord in the 1st house (Tanu Bhava) creates strong vitality, prominent self-reliance, robust physical presence, and independent personal drive."
            2 -> "Lagna lord in the 2nd house (Dhana Bhava) focuses personal energy into resource accumulation, family heritage, articulated speech, and steady values."
            3 -> "Lagna lord in the 3rd house (Sahaja Bhava) stimulates courage, self-effort (Parakrama), communicative dexterity, and close sibling or peer connections."
            4 -> "Lagna lord in the 4th house (Sukha Bhava) emphasizes domestic harmony, maternal bonding, academic learning, real estate, and inner emotional contentment."
            5 -> "Lagna lord in the 5th house (Putra Bhava) connects self-identity with creative intelligence, speculative acumen, mantra/vidya, and joyful progeny."
            6 -> "Lagna lord in the 6th house (Ari Bhava) cultivates problem-solving tenacity, service orientation, competitiveness, and the capacity to overcome adversaries."
            7 -> "Lagna lord in the 7th house (Yuvati Bhava) directs primary life focus toward partnerships, diplomacy, public presence, and contractual relationships."
            8 -> "Lagna lord in the 8th house (Randhra Bhava) induces transformative life experiences, interest in esoteric mysteries, occult research, and deep psychological endurance."
            9 -> "Lagna lord in the 9th house (Dharma Bhava) bestows divine grace (Bhagya), philosophical pilgrimage, righteous conduct, and mentor guidance."
            10 -> "Lagna lord in the 10th house (Karma Bhava) promotes prominent professional status, leadership responsibility, societal impact, and dutiful public action."
            11 -> "Lagna lord in the 11th house (Labha Bhava) supports expansive social networks, fulfillment of long-term aspirations, and steady material or professional gains."
            12 -> "Lagna lord in the 12th house (Vyaya Bhava) fosters introspective solitude, charitable renunciation, spiritual liberation (Moksha), and potential foreign connections."
            else -> "Lagna lord in house $house provides a distinctive focal point for the native's life path."
        }
        return Pair(title, desc)
    }

    // 3. Moon Sign & Mind
    fun getMoonSignInterpretation(rashi: Rashi): Pair<String, String> = when (rashi) {
        Rashi.ARIES -> Pair(
            "Moon in Aries (Mesha) — Swift Emotional Impulse",
            "The emotional core reacts quickly and directly. You process feelings with immediacy and courage, preferring active resolution over lingering hesitation."
        )
        Rashi.TAURUS -> Pair(
            "Moon in Taurus (Vrishabha) — Exalted Emotional Serenity",
            "The Moon is in its classical exaltation (Uchcha) sign, granting tranquil emotional equilibrium, steadfast devotion, sensory contentment, and deep inner peace."
        )
        Rashi.GEMINI -> Pair(
            "Moon in Gemini (Mithuna) — Inquisitive Mental Agility",
            "The mind thrives on intellectual engagement, communicative articulation, and variety. Emotional processing occurs predominantly through rational dialogue."
        )
        Rashi.CANCER -> Pair(
            "Moon in Cancer (Karka) — Swakshetra Protective Receptivity",
            "The Moon occupies its own sign, bestowing profound empathic sensitivity, intuitive depth, strong memory, and protective maternal warmth."
        )
        Rashi.LEO -> Pair(
            "Moon in Leo (Simha) — Warm Heart & Dignified Sentiment",
            "Emotions are experienced with grandeur, generous warmth, and noble self-respect. You express affection openly and take pride in honorable conduct."
        )
        Rashi.VIRGO -> Pair(
            "Moon in Virgo (Kanya) — Analytical Emotional Prudence",
            "The mind tends toward orderly processing, careful evaluation, and practical problem-solving. Emotional serenity comes through purposeful daily organization."
        )
        Rashi.LIBRA -> Pair(
            "Moon in Libra (Tula) — Harmonious & Empathetic Balance",
            "The emotional compass seeks fairness, diplomatic agreement, and aesthetic beauty. You value peaceful reciprocity in emotional interactions."
        )
        Rashi.SCORPIO -> Pair(
            "Moon in Scorpio (Vrishchika) — Intense Emotional Transformation",
            "The Moon is in classical debilitation (Neecha), creating intensely deep, private feelings and penetrating psychological instincts that catalyze profound regeneration."
        )
        Rashi.SAGITTARIUS -> Pair(
            "Moon in Sagittarius (Dhanu) — Optimistic & Dharmic Outlook",
            "The mind is naturally buoyed by philosophical optimism, righteous faith, and a longing for broader philosophical and moral understanding."
        )
        Rashi.CAPRICORN -> Pair(
            "Moon in Capricorn (Makara) — Sober Responsibility & Emotional Reserve",
            "Emotions are tempered with realism, pragmatic duty, and self-restraint. You process emotional challenges through disciplined, constructive action."
        )
        Rashi.AQUARIUS -> Pair(
            "Moon in Aquarius (Kumbha) — Objective & Humanitarian Consciousness",
            "The mind adopts an expansive, humanitarian perspective. You experience emotions through an intellectual lens, prioritizing collective goodwill."
        )
        Rashi.PISCES -> Pair(
            "Moon in Pisces (Meena) — Poetic Imagination & Universal Empathy",
            "The inner emotional landscape is porous, highly creative, and spiritually receptive. You feel subtle atmospheric shifts and possess boundless compassionate kindness."
        )
    }

    // 4. Nakshatra Spiritual & Psychological Key
    fun getNakshatraSignification(nakshatra: Nakshatra): String = when (nakshatra) {
        Nakshatra.ASHWINI -> "Ashwini (governed by Ketu, deities Ashwini Kumaras): Healing vitality, swift enterprise, and pioneering agility."
        Nakshatra.BHARANI -> "Bharani (governed by Venus, deity Yama): Transformative restraint, moral duty, intense creative drive, and enduring endurance."
        Nakshatra.KRITTIKA -> "Krittika (governed by Sun, deity Agni): Purifying discernment, decisive intellect, courage, and luminous focus."
        Nakshatra.ROHINI -> "Rohini (governed by Moon, deity Brahma): Creative fertility, charm, agricultural prosperity, and aesthetic refinement."
        Nakshatra.MRIGASHIRSHA -> "Mrigashirsha (governed by Mars, deity Soma): The searching mind, intellectual curiosity, gentle exploration, and aesthetic pursuit."
        Nakshatra.ARDRA -> "Ardra (governed by Rahu, deity Rudra): Transformative catharsis, sharp intellectual perception, and breakthrough clarity through storms."
        Nakshatra.PUNARVASU -> "Punarvasu (governed by Jupiter, deity Aditi): Renewal of light, restorative benevolence, ethical integrity, and domestic abundance."
        Nakshatra.PUSHYA -> "Pushya (governed by Saturn, deity Brihaspati): Supreme spiritual nourishment, steadfast wisdom, dharmic duty, and auspicious sustenance."
        Nakshatra.ASHLESHA -> "Ashlesha (governed by Mercury, deity Sarpas): Kundalini awareness, esoteric discernment, psychological depth, and protective caution."
        Nakshatra.MAGHA -> "Magha (governed by Ketu, deity Pitris): Ancestral heritage, royal dignity, traditional authority, and soul leadership."
        Nakshatra.PURVA_PHALGUNI -> "Purva Phalguni (governed by Venus, deity Bhaga): Harmonious relaxation, social charm, creative arts, and celebratory fortune."
        Nakshatra.UTTARA_PHALGUNI -> "Uttara Phalguni (governed by Sun, deity Aryaman): Principled patronship, reliable contracts, marital alliance, and generous stability."
        Nakshatra.HASTA -> "Hasta (governed by Moon, deity Savitar): Dexterous craftsmanship, analytical precision, healing touch, and resourceful intelligence."
        Nakshatra.CHITRA -> "Chitra (governed by Mars, deity Tvashtar): Brilliant architecture, aesthetic craftsmanship, radiant beauty, and meticulous design."
        Nakshatra.SWATI -> "Swati (governed by Rahu, deity Vayu): Independent flexibility, diplomatic trade, intellectual balance, and gentle movement."
        Nakshatra.VISHAKHA -> "Vishakha (governed by Jupiter, deities Indra-Agni): Singular determination, goal-oriented triumph, spiritual zeal, and focused stamina."
        Nakshatra.ANURADHA -> "Anuradha (governed by Saturn, deity Mitra): Devoted friendship, collaborative harmony, perseverance, and spiritual devotion (Bhakti)."
        Nakshatra.JYESHTHA -> "Jyeshtha (governed by Mercury, deity Indra): Elder responsibility, heroic courage, keen protective wisdom, and authoritative presence."
        Nakshatra.MULA -> "Mula (governed by Ketu, deity Nirriti): Root investigation, dissolution of illusions, deep research, and core transformative truth."
        Nakshatra.PURVA_ASHADHA -> "Purva Ashadha (governed by Venus, deity Apah): Invincible conviction, purifying flow, artistic eloquence, and philosophical resilience."
        Nakshatra.UTTARA_ASHADHA -> "Uttara Ashadha (governed by Sun, deity Vishvedevas): Permanent victory through righteousness, universal ethics, quiet majesty, and structured leadership."
        Nakshatra.SHRAVANA -> "Shravana (governed by Moon, deity Vishnu): Attentive listening, oral tradition, scholarly learning, and universal preservation."
        Nakshatra.DHANISHTA -> "Dhanishta (governed by Mars, deities Eight Vasus): Rhythmic harmony, musical or organizational timing, material wealth, and generous charisma."
        Nakshatra.SHATABHISHA -> "Shatabhisha (governed by Rahu, deity Varuna): One hundred cosmic physicians, esoteric healing, solitude, research, and cyclical rejuvenation."
        Nakshatra.PURVA_BHADRAPADA -> "Purva Bhadrapada (governed by Jupiter, deity Aja Ekapada): Ascetic aspiration, intense spiritual discipline, visionary depth, and noble detachment."
        Nakshatra.UTTARA_BHADRAPADA -> "Uttara Bhadrapada (governed by Saturn, deity Ahirbudhanya): Serpent of the deep ocean, profound contemplative serenity, wisdom, and emotional stability."
        Nakshatra.REVATI -> "Revati (governed by Mercury, deity Pushan): Compassionate shepherd, safe journey, benevolent prosperity, and transcendent completion."
    }

    // 5. Planetary Dignity Interpretations
    fun getDignitySummary(planet: String, dignity: PlanetDignity): String = when (dignity) {
        PlanetDignity.EXALTED -> "$planet is in classical Exaltation (Uchcha), indicating elevated potential, radiant expression, and potent capacity to deliver its highest significations."
        PlanetDignity.MOOLATRIKONA -> "$planet occupies its Moolatrikona portion, signifying purposeful strength, focused executive capacity, and reliable structural support."
        PlanetDignity.OWN_SIGN -> "$planet is situated in its Own Sign (Swakshetra), conferring natural comfort, unhindered agency, and solid foundation in its portfolio."
        PlanetDignity.FRIEND -> "$planet resides in a Friendly Sign (Mitra Rashi), providing cooperative support, constructive channels, and cordial expression."
        PlanetDignity.NEUTRAL -> "$planet is in a Neutral Sign (Sama Rashi), producing balanced, conventional, and context-dependent manifestations."
        PlanetDignity.ENEMY -> "$planet is located in an Inimical Sign (Shatru Rashi), requiring conscious effort, patient adaptation, and intentional problem-solving."
        PlanetDignity.DEBILITATED -> "$planet is placed in its Debilitation Sign (Neecha), representing internal friction, humility-inducing lessons, and a necessity to cultivate conscious inner strength."
    }

    // 6. Retrograde (Vakri) Interpretations
    fun getRetrogradeInterpretation(planet: String): String = when (planet.lowercase().trim()) {
        "mercury" -> "Mercury Retrograde (Budha Vakri): Encourages deep introspection, non-standard communicative synthesis, and rigorous re-verification of plans before execution."
        "venus" -> "Venus Retrograde (Shukra Vakri): Inspires internal re-evaluation of aesthetic choices, interpersonal relationship dynamics, and personal core values."
        "mars" -> "Mars Retrograde (Mangala Vakri): Directs energetic ambition inward, counseling measured stamina, deliberate pacing, and conscious avoidance of hasty conflict."
        "jupiter" -> "Jupiter Retrograde (Guru Vakri): Cultivates profound spiritual questioning, idiosyncratic philosophical insights, and an independent moral compass."
        "saturn" -> "Saturn Retrograde (Shani Vakri): Deepens karmic responsibility, prompting sustained diligence, internal discipline, and long-term re-structuring of foundations."
        else -> "$planet is in Retrograde motion, emphasizing internalized energy and reflexive deliberation in its significations."
    }

    // 7. Planet in House Interpretations (Whole Sign)
    fun getPlanetInHouseInterpretation(planet: String, house: Int): String {
        val p = planet.capitalizeFirst()
        return when (p) {
            "Sun" -> when (house) {
                1 -> "Sun in the 1st House: Imparts dignified presence, noble self-esteem, strong vitality, and natural leadership inclination."
                2 -> "Sun in the 2nd House: Directs authoritative energy into speech, family values, and financial self-determination."
                3 -> "Sun in the 3rd House: Stimulates courageous initiative, confident communication, and pioneering enterprise."
                4 -> "Sun in the 4th House: Instills deep connection to home roots, parental dignity, and an inner sense of sanctuary."
                5 -> "Sun in the 5th House: Energizes creative brilliance, intellectual pride, speculative vision, and mentoring capacity."
                6 -> "Sun in the 6th House: Bestows powerful resilience against adversity, competitive determination, and service stamina."
                7 -> "Sun in the 7th House: Brings dynamic focus to contractual alliances, public standing, and relationship equality."
                8 -> "Sun in the 8th House: Catalyzes transformative self-discovery, interest in hidden truths, and intense regenerative will."
                9 -> "Sun in the 9th House: Illuminates higher dharma, philosophical pilgrimage, respect for mentors, and ethical righteousness."
                10 -> "Sun in the 10th House: The Sun gains Digbala (directional strength), conferring executive authority, professional recognition, and public honor."
                11 -> "Sun in the 11th House: Expands influential social networks, realization of aspirations, and dignified peer alliances."
                12 -> "Sun in the 12th House: Promotes spiritual introspection, solitude for contemplative research, and transcendent detachment."
                else -> "Sun in House $house highlights vitality and purpose in that life domain."
            }
            "Moon" -> when (house) {
                1 -> "Moon in the 1st House: Enhances emotional sensitivity, adaptable charisma, intuitive warmth, and public receptivity."
                2 -> "Moon in the 2nd House: Fosters emotional security through family cohesion, melodious speech, and nourishing resources."
                3 -> "Moon in the 3rd House: Promotes imaginative curiosity, communicative versatility, and pleasant sibling connections."
                4 -> "Moon in the 4th House: The Moon gains Digbala (directional strength), yielding profound domestic contentment, maternal grace, and emotional tranquility."
                5 -> "Moon in the 5th House: Encourages poetic creativity, romantic devotion, pedagogical affection, and intuitive intelligence."
                6 -> "Moon in the 6th House: Orients the caring instinct toward daily healthcare, therapeutic routines, and empathetic service."
                7 -> "Moon in the 7th House: Cultivates empathetic partnership dynamics, collaborative sensitivity, and public rapport."
                8 -> "Moon in the 8th House: Deepens psychic intuition, emotional metamorphosis, and interest in hidden or psychological mysteries."
                9 -> "Moon in the 9th House: Inspires devotional spirituality, open-minded travel, ethical grace, and righteous optimism."
                10 -> "Moon in the 10th House: Creates a socially visible, responsive career path involving public service or mass communication."
                11 -> "Moon in the 11th House: Attracts supportive friendships, diverse social circles, and fluid fulfillment of heartfelt desires."
                12 -> "Moon in the 12th House: Inclines toward peaceful solitude, meditative imagination, compassionate charity, and spiritual dreams."
                else -> "Moon in House $house reflects emotional focus in that sector."
            }
            "Jupiter" -> when (house) {
                1 -> "Jupiter in the 1st House: Jupiter gains Digbala, gracing the personality with wisdom, expansive benevolence, moral stature, and optimism."
                2 -> "Jupiter in the 2nd House: Cultivates truthful speech, financial prudence, family honor, and wholesome culinary inclinations."
                3 -> "Jupiter in the 3rd House: Bestows philosophical writing, uplifting communication, and constructive sibling relationships."
                4 -> "Jupiter in the 4th House: Enriches home life with spiritual calm, scholarly knowledge, spacious dwelling, and maternal blessings."
                5 -> "Jupiter in the 5th House: Highly auspicious for intellect, purva-punya (past merit), counsel, pedagogy, and virtuous progeny."
                6 -> "Jupiter in the 6th House: Shields against prolonged enmity through ethical diplomacy, healing aptitude, and fair service."
                7 -> "Jupiter in the 7th House: Blesses marital harmony, wise partnerships, upright business conduct, and mutual respect."
                8 -> "Jupiter in the 8th House: Protects through crisis, supports legacy insights, longevity, and deep esoteric philosophical research."
                9 -> "Jupiter in the 9th House: Residing in its natural house of Dharma, Jupiter bestows divine fortune, righteous teachers, and philosophical grace."
                10 -> "Jupiter in the 10th House: Elevates professional ethics, advisory reputation, judicial or administrative leadership, and societal respect."
                11 -> "Jupiter in the 11th House: Promotes substantial fulfillment of goals, noble friendships, charitable patronage, and sustained prosperity."
                12 -> "Jupiter in the 12th House: Fosters profound spiritual liberation, generous philanthropy, and solitary meditation."
                else -> "Jupiter in House $house expands auspicious wisdom and growth."
            }
            "Saturn" -> when (house) {
                1 -> "Saturn in the 1st House: Imparts sober maturity, patient self-restraint, philosophical seriousness, and enduring perseverance."
                2 -> "Saturn in the 2nd House: Inculcates frugal financial management, disciplined speech, and deliberate, cautious resource building."
                3 -> "Saturn in the 3rd House: Bestows resolute courage, tireless work ethic, manual dexterity, and unyielding persistence."
                4 -> "Saturn in the 4th House: Emphasizes grounded domestic duties, architectural appreciation, and emotional self-reliance."
                5 -> "Saturn in the 5th House: Promotes disciplined scholarship, structured creative pursuits, and thoughtful, measured investment."
                6 -> "Saturn in the 6th House: Highly praised classically for dismantling obstacles, steady resilience against competitors, and disciplined service."
                7 -> "Saturn in the 7th House: Saturn gains Digbala, emphasizing mature commitment, contractual fidelity, and enduring partnerships."
                8 -> "Saturn in the 8th House: Signifies durability, patient endurance through transition, research stamina, and long-term perspective."
                9 -> "Saturn in the 9th House: Inspires traditional, orthodox spiritual respect, rigorous philosophy, and hard-earned higher knowledge."
                10 -> "Saturn in the 10th House: Creates enduring professional stature through steady merit, organizational leadership, and unswerving responsibility."
                11 -> "Saturn in the 11th House: Auspicious placement for long-term realization of plans, steadfast elder allies, and sustained communal gains."
                12 -> "Saturn in the 12th House: Promotes meditative asceticism, quiet withdrawal, disciplined foreign pursuits, and detachment from vanity."
                else -> "Saturn in House $house builds endurance and structured responsibility."
            }
            else -> "$p in House $house contributes its unique archetypal influence to that sector of life."
        }
    }

    // 8. D1 ↔ D9 Comparative Rules
    fun interpretD1D9Comparative(
        vargottamaPlanets: List<String>,
        strengthenedInD9: List<String>,
        weakenedInD9: List<String>,
        d1Lagna: String,
        d9Lagna: String?
    ): String {
        val sb = StringBuilder()
        if (vargottamaPlanets.isNotEmpty()) {
            val list = vargottamaPlanets.joinToString(", ")
            sb.append("Vargottama Grahas ($list): Occupying identical signs in both Rashi (D1) and Navamsha (D9), these planets achieve exceptional internal coherence and classical resilience, reliably delivering their potential with psychological confidence. ")
        }
        if (strengthenedInD9.isNotEmpty()) {
            val list = strengthenedInD9.joinToString(", ")
            sb.append("D9 Dignity Elevation ($list): These planets gain exalted or own sign dignity in the Navamsha, showing that hidden inner strength, soul purpose, and long-term potential exceed initial surface appearances. ")
        }
        if (weakenedInD9.isNotEmpty()) {
            val list = weakenedInD9.joinToString(", ")
            sb.append("D9 Navamsha Friction ($list): These planets encounter debilitated or inimical signs in D9, indicating that outer confidence requires conscious inner discipline and patience to sustain. ")
        }
        if (d9Lagna != null) {
            sb.append("Navamsha Lagna ($d9Lagna) provides the subtle dharmic blueprint of the inner self, operating as the soul's complementary compass to the natal physical Lagna ($d1Lagna).")
        }
        return sb.toString().trim()
    }

    // 9. D1 ↔ D10 Career Guidance Rules
    fun interpretD10CareerThemes(
        tenthHouseLord: String?,
        tenthHousePlanetsD1: List<String>,
        tenthHousePlanetsD10: List<String>,
        keyPlanetsD10: List<String>
    ): List<String> {
        val themes = mutableListOf<String>()
        val combinedPlanets = (tenthHousePlanetsD1 + tenthHousePlanetsD10 + listOfNotNull(tenthHouseLord)).distinct()

        if (combinedPlanets.any { it.equals("Sun", true) || it.equals("Mars", true) }) {
            themes.add("Executive Direction & Leadership: Capacity for organizational command, public administration, entrepreneurship, or engineering initiatives.")
        }
        if (combinedPlanets.any { it.equals("Jupiter", true) }) {
            themes.add("Advisory & Pedagogical Counsel: Inclination toward legal, educational, financial advisory, ethical mentorship, or strategic consulting roles.")
        }
        if (combinedPlanets.any { it.equals("Mercury", true) }) {
            themes.add("Analytical & Media Commerce: Dexterity in communications, data architecture, commerce, journalism, or analytical problem solving.")
        }
        if (combinedPlanets.any { it.equals("Venus", true) }) {
            themes.add("Creative Innovation & Diplomacy: Aptitude for design, cultural production, aesthetic branding, hospitality, or interpersonal negotiations.")
        }
        if (combinedPlanets.any { it.equals("Saturn", true) }) {
            themes.add("Systemic Infrastructure & Industrial Endurance: Talent for long-term project stewardship, governance, public logistics, and durable operational mastery.")
        }
        if (themes.isEmpty()) {
            themes.add("Balanced Professional Versatility: Adaptable application of skill, methodical career pacing, and collaborative workplace achievement.")
        }
        return themes
    }

    // 10. Transit (Gochar) Rules from Moon
    fun evaluateSaturnSadeSati(moonSignIndex: Int, transitSaturnSignIndex: Int): Triple<String?, Boolean, Boolean> {
        val houseFromMoon = ((transitSaturnSignIndex - moonSignIndex).mod(12)) + 1
        var sadeSatiPhase: String? = null
        var isKantaka = false
        var isAshtama = false

        when (houseFromMoon) {
            12 -> sadeSatiPhase = "Rising Phase (12th from Moon): Shifting subconscious patterns, prudent expenditure management, and inner restructuring."
            1 -> sadeSatiPhase = "Peak Phase (Janma Shani / 1st from Moon): Direct test of mental resilience, health mindfulness, and foundational life refactoring."
            2 -> sadeSatiPhase = "Setting Phase (2nd from Moon): Stabilization of resources, family alignment, and practical closure of previous 7.5-year cycle lessons."
            4 -> isKantaka = true // Kantaka Shani
            8 -> isAshtama = true // Ashtama Shani
        }

        return Triple(sadeSatiPhase, isKantaka, isAshtama)
    }

    fun evaluateJupiterTransitFromMoon(houseFromMoon: Int): Pair<InterpretationFactorPolarity, String> = when (houseFromMoon) {
        2, 5, 7, 9, 11 -> Pair(
            InterpretationFactorPolarity.SUPPORTIVE,
            "Jupiter Transiting House $houseFromMoon from Moon: Classical auspicious Gochar period conferring wisdom, social goodwill, educational luck, and ethical expansion."
        )
        1 -> Pair(
            InterpretationFactorPolarity.NEUTRAL,
            "Jupiter in 1st House from Moon: Expansion of self-awareness and philosophical curiosity, prompting healthy self-moderation."
        )
        else -> Pair(
            InterpretationFactorPolarity.NEUTRAL,
            "Jupiter in House $houseFromMoon from Moon: Working behind the scenes to cultivate internal maturity, duty, and quiet discernment."
        )
    }

    private fun String.capitalizeFirst(): String =
        this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
