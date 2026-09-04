package com.example.domain

import com.example.domain.models.BirthLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.Assert.assertNull

class BirthLocationTest {

    @Test
    fun testValidCoordinates() {
        val location = BirthLocation(28.6139, 77.2090, "New Delhi", 216.0, "Asia/Kolkata")
        assertEquals(28.6139, location.latitude, 0.0001)
        assertEquals(77.2090, location.longitude, 0.0001)
    }

    @Test
    fun testInvalidLatitude() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(91.0, 77.2, "Invalid", 0.0, "Asia/Kolkata")
        }
    }

    @Test
    fun testInvalidLongitude() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(28.0, 181.0, "Invalid", 0.0, "Asia/Kolkata")
        }
    }

    @Test
    fun testNanAndInfinityRejection() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(Double.NaN, 77.0, "NaN Lat")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(28.0, Double.POSITIVE_INFINITY, "Inf Lon")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(28.0, 77.0, "Inf Alt", Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun testValidTimeZone() {
        val loc = BirthLocation(28.0, 77.0, "Valid", timeZoneId = "Asia/Kolkata")
        assertEquals("Asia/Kolkata", loc.timeZoneId)
    }

    @Test
    fun testInvalidTimeZone() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(28.0, 77.0, "Invalid", timeZoneId = "Invalid/Zone")
        }
    }
    
    @Test
    fun testBlankPlaceName() {
        assertThrows(IllegalArgumentException::class.java) {
            BirthLocation(28.0, 77.0, "   ")
        }
    }
    
    @Test
    fun testNullAltitude() {
        val loc = BirthLocation(28.0, 77.0, "Null Alt", altitudeMeters = null)
        assertNull(loc.altitudeMeters)
    }
}
