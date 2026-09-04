package com.example.domain

import de.thmac.swisseph.SweConst
import de.thmac.swisseph.SwissEph
import de.thmac.swisseph.DblObj
import org.junit.Test
import java.time.ZonedDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import com.example.domain.models.BirthLocation

class RiseTest {
    @Test
    fun testRise() {
        val swe = SwissEph()
        val tret = DblObj()
        val serr = StringBuffer()
        
        val date = ZonedDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0), ZoneId.of("Asia/Kolkata"))
        val location = BirthLocation(28.6139, 77.2090, "New Delhi")
        
        val utcDateTime = date.withZoneSameInstant(ZoneOffset.UTC)
        val hourDecimalUt = utcDateTime.hour +
                (utcDateTime.minute / 60.0) +
                (utcDateTime.second / 3600.0)
        
        val sweDate = de.thmac.swisseph.SweDate(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            hourDecimalUt
        )
        val tjdUt = sweDate.getJulDay()
        
        val geopos = doubleArrayOf(location.longitude, location.latitude, 0.0)
        
        val flags = SweConst.SEFLG_SWIEPH
        val resRise = swe.swe_rise_trans(tjdUt, SweConst.SE_SUN, null, flags, SweConst.SE_CALC_RISE, geopos, 1013.25, 15.0, tret, serr)
        
        println("Rise Result code: $resRise")
        println("Rise JD: ${tret.`val`}")
        
        val resSet = swe.swe_rise_trans(tjdUt, SweConst.SE_SUN, null, flags, SweConst.SE_CALC_SET, geopos, 1013.25, 15.0, tret, serr)
        
        println("Set Result code: $resSet")
        println("Set JD: ${tret.`val`}")
    }
}
