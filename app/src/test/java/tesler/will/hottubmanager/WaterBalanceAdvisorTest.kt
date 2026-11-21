package tesler.will.hottubmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tesler.will.hottubmanager.domain.chemistry.BalanceStep
import tesler.will.hottubmanager.domain.chemistry.ParameterStatus
import tesler.will.hottubmanager.domain.chemistry.WaterBalanceAdvisor
import tesler.will.hottubmanager.domain.chemistry.WaterReadingInput

class WaterBalanceAdvisorTest {

    private val advisor = WaterBalanceAdvisor()

    @Test
    fun `TA always takes priority even when pH and CH are out of range`() {
        val advice = advisor.evaluate(
            WaterReadingInput(
                totalAlkalinityPpm = 50,
                ph = 8.2,
                calciumHardnessPpm = 100
            )
        )
        assertEquals(BalanceStep.TOTAL_ALKALINITY, advice.activeStep)
        assertEquals(ParameterStatus.LOW, advice.taStatus)
        assertTrue(advice.isPhLocked)
        assertTrue(advice.isChLocked)
        assertEquals("Alkalinity Increaser", advice.chemicalName)
        assertEquals("3 tablespoons", advice.doseText)
        assertTrue(advice.needs15MinWaitTimer)
    }

    @Test
    fun `high TA recommends pH Decreaser 1-2 teaspoons`() {
        val advice = advisor.evaluate(
            WaterReadingInput(totalAlkalinityPpm = 150)
        )
        assertEquals(BalanceStep.TOTAL_ALKALINITY, advice.activeStep)
        assertEquals(ParameterStatus.HIGH, advice.taStatus)
        assertEquals("pH Decreaser", advice.chemicalName)
        assertEquals("1–2 teaspoons", advice.doseText)
        assertTrue(advice.needs15MinWaitTimer)
    }

    @Test
    fun `when TA is in range 80-120 pH unlocks and CH remains locked`() {
        val advice = advisor.evaluate(
            WaterReadingInput(
                totalAlkalinityPpm = 100,
                ph = 8.0,
                calciumHardnessPpm = 100
            )
        )
        assertEquals(BalanceStep.PH, advice.activeStep)
        assertFalse(advice.isPhLocked)
        assertTrue(advice.isChLocked)
        assertEquals("pH Decreaser", advice.chemicalName)
        assertEquals("1–2 teaspoons", advice.doseText)
        assertTrue(advice.needs15MinWaitTimer)
    }

    @Test
    fun `low pH recommends Alkalinity Increaser 3 tablespoons`() {
        val advice = advisor.evaluate(
            WaterReadingInput(
                totalAlkalinityPpm = 90,
                ph = 6.8
            )
        )
        assertEquals(BalanceStep.PH, advice.activeStep)
        assertEquals(ParameterStatus.LOW, advice.phStatus)
        assertEquals("Alkalinity Increaser", advice.chemicalName)
        assertEquals("3 tablespoons", advice.doseText)
    }

    @Test
    fun `when TA and pH are in range CH unlocks and low CH recommends Calcium Hardness Increaser`() {
        val advice = advisor.evaluate(
            WaterReadingInput(
                totalAlkalinityPpm = 100,
                ph = 7.5,
                calciumHardnessPpm = 120
            )
        )
        assertEquals(BalanceStep.CALCIUM_HARDNESS, advice.activeStep)
        assertFalse(advice.isPhLocked)
        assertFalse(advice.isChLocked)
        assertEquals("Calcium Hardness Increaser", advice.chemicalName)
        assertEquals("3 tablespoons", advice.doseText)
        assertTrue(advice.needs15MinWaitTimer)
    }

    @Test
    fun `all three parameters in range returns fully balanced`() {
        val advice = advisor.evaluate(
            WaterReadingInput(
                totalAlkalinityPpm = 100,
                ph = 7.4,
                calciumHardnessPpm = 200
            )
        )
        assertEquals(BalanceStep.ALL_BALANCED, advice.activeStep)
        assertTrue(advice.isFullyBalanced)
        assertFalse(advice.needs15MinWaitTimer)
        assertNull(advice.chemicalName)
    }

    @Test
    fun `empty input starts at Step 1 even with manualOverrideOrder true`() {
        val advice = advisor.evaluate(WaterReadingInput(), manualOverrideOrder = true)
        assertEquals(BalanceStep.TOTAL_ALKALINITY, advice.activeStep)
        assertFalse(advice.isFullyBalanced)
    }
}
