package com.mohgwatch.core.util

import com.mohgwatch.core.model.GlucoseUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class GlucoseFormatterTest {
    @Test fun theSameStoredMeasurementUsesTheSelectedDisplayUnit() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("180", GlucoseFormatter.format(180f, GlucoseUnit.MG_DL))
            assertEquals("10.0", GlucoseFormatter.format(180f, GlucoseUnit.MMOL_L))
            assertEquals("180 mg/dL", GlucoseFormatter.formatWithUnit(180f, GlucoseUnit.MG_DL))
            assertEquals("10.0 mmol/L", GlucoseFormatter.formatWithUnit(180f, GlucoseUnit.MMOL_L))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun mmolDecimalSeparatorFollowsTheCurrentLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"))
            assertEquals("3,9 mmol/L", GlucoseFormatter.formatWithUnit(70f, GlucoseUnit.MMOL_L))
        } finally {
            Locale.setDefault(original)
        }
    }
}
