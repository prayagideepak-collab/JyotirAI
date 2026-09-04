#!/bin/bash
sed -i 's/val snapshot = PanchangCalculator.calculatePanchang(date, location, swissEphThreadLocal.get()) { utcIso ->/val snapshot = PanchangCalculator.calculatePanchang(date, location, swissEphThreadLocal.get())/g' app/src/main/java/com/example/data/engine/SwissEphAstrologyEngine.kt
sed -i '/CalculationMetadata(/,/^            }/d' app/src/main/java/com/example/data/engine/SwissEphAstrologyEngine.kt
