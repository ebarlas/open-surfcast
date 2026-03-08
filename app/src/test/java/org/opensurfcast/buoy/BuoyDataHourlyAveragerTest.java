package org.opensurfcast.buoy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BuoyDataHourlyAveragerTest {

    // ========== Helper methods ==========

    private static BuoyStdMetData stdMet(int year, int month, int day, int hour, int minute) {
        BuoyStdMetData d = new BuoyStdMetData();
        d.setYear(year);
        d.setMonth(month);
        d.setDay(day);
        d.setHour(hour);
        d.setMinute(minute);
        d.setEpochSeconds(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
                .toEpochSecond());
        return d;
    }

    private static BuoySpecWaveData specWave(int year, int month, int day, int hour, int minute) {
        BuoySpecWaveData d = new BuoySpecWaveData();
        d.setYear(year);
        d.setMonth(month);
        d.setDay(day);
        d.setHour(hour);
        d.setMinute(minute);
        d.setEpochSeconds(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
                .toEpochSecond());
        return d;
    }

    private static long epochAt(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC)
                .toEpochSecond();
    }

    // ========== Empty / null input ==========

    @Test
    public void testEmptyInputReturnsEmptyList() {
        assertTrue(BuoyDataHourlyAverager.averageStdMetByHour(new ArrayList<>()).isEmpty());
        assertTrue(BuoyDataHourlyAverager.averageSpecWaveByHour(new ArrayList<>()).isEmpty());
    }

    @Test
    public void testNullInputReturnsEmptyList() {
        assertTrue(BuoyDataHourlyAverager.averageStdMetByHour(null).isEmpty());
        assertTrue(BuoyDataHourlyAverager.averageSpecWaveByHour(null).isEmpty());
    }

    // ========== Single observation passthrough ==========

    @Test
    public void testSingleObservationPassesThroughStdMet() {
        BuoyStdMetData d = stdMet(2026, 3, 7, 10, 30);
        d.setWindSpeed(5.0);
        d.setWaveHeight(1.5);
        d.setWindDirection(180);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Collections.singletonList(d));

        assertEquals(1, result.size());
        BuoyStdMetData r = result.get(0);
        assertEquals(2026, r.getYear());
        assertEquals(3, r.getMonth());
        assertEquals(7, r.getDay());
        assertEquals(10, r.getHour());
        assertEquals(0, r.getMinute());
        assertEquals(epochAt(2026, 3, 7, 10), r.getEpochSeconds());
        assertEquals(5.0, r.getWindSpeed(), 0.001);
        assertEquals(1.5, r.getWaveHeight(), 0.001);
        assertEquals(Integer.valueOf(180), r.getWindDirection());
    }

    // ========== Multiple observations collapse to one ==========

    @Test
    public void testMultipleObservationsCollapseToOneHour() {
        BuoyStdMetData d1 = stdMet(2026, 3, 7, 10, 0);
        d1.setWindSpeed(4.0);
        BuoyStdMetData d2 = stdMet(2026, 3, 7, 10, 10);
        d2.setWindSpeed(6.0);
        BuoyStdMetData d3 = stdMet(2026, 3, 7, 10, 20);
        d3.setWindSpeed(8.0);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2, d3));

        assertEquals(1, result.size());
        assertEquals(6.0, result.get(0).getWindSpeed(), 0.001);
    }

    // ========== Arithmetic mean of Double fields ==========

    @Test
    public void testArithmeticMeanOfDoubleFields() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 0, 0);
        d1.setAirTemperature(10.0);
        d1.setPressure(1010.0);

        BuoyStdMetData d2 = stdMet(2026, 1, 1, 0, 30);
        d2.setAirTemperature(14.0);
        d2.setPressure(1012.0);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertEquals(12.0, result.get(0).getAirTemperature(), 0.001);
        assertEquals(1011.0, result.get(0).getPressure(), 0.001);
    }

    // ========== Null fields excluded from averaging ==========

    @Test
    public void testNullFieldsExcludedFromAverage() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 0, 0);
        d1.setWaveHeight(2.0);

        BuoyStdMetData d2 = stdMet(2026, 1, 1, 0, 10);
        // waveHeight is null

        BuoyStdMetData d3 = stdMet(2026, 1, 1, 0, 20);
        d3.setWaveHeight(4.0);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2, d3));

        assertEquals(1, result.size());
        assertEquals(3.0, result.get(0).getWaveHeight(), 0.001);
    }

    @Test
    public void testAllNullFieldReturnsNull() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 0, 0);
        BuoyStdMetData d2 = stdMet(2026, 1, 1, 0, 30);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertNull(result.get(0).getWaveHeight());
        assertNull(result.get(0).getWindSpeed());
    }

    // ========== Direction fields use most recent ==========

    @Test
    public void testDirectionFieldUseMostRecent() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 0, 0);
        d1.setWindDirection(90);
        d1.setMeanWaveDirection(180);

        BuoyStdMetData d2 = stdMet(2026, 1, 1, 0, 30);
        d2.setWindDirection(270);
        d2.setMeanWaveDirection(350);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(270), result.get(0).getWindDirection());
        assertEquals(Integer.valueOf(350), result.get(0).getMeanWaveDirection());
    }

    // ========== Categorical fields use most recent ==========

    @Test
    public void testCategoricalFieldsUseMostRecent() {
        BuoySpecWaveData d1 = specWave(2026, 1, 1, 0, 0);
        d1.setSteepness("SWELL");
        d1.setSwellDirection("N");
        d1.setWindWaveDirection("E");

        BuoySpecWaveData d2 = specWave(2026, 1, 1, 0, 30);
        d2.setSteepness("STEEP");
        d2.setSwellDirection("NW");
        d2.setWindWaveDirection("SE");

        List<BuoySpecWaveData> result = BuoyDataHourlyAverager.averageSpecWaveByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertEquals("STEEP", result.get(0).getSteepness());
        assertEquals("NW", result.get(0).getSwellDirection());
        assertEquals("SE", result.get(0).getWindWaveDirection());
    }

    // ========== Multiple hours produce multiple bins ==========

    @Test
    public void testMultipleHoursProduceMultipleBins() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 10, 0);
        d1.setWindSpeed(4.0);
        BuoyStdMetData d2 = stdMet(2026, 1, 1, 10, 30);
        d2.setWindSpeed(6.0);

        BuoyStdMetData d3 = stdMet(2026, 1, 1, 11, 0);
        d3.setWindSpeed(10.0);
        BuoyStdMetData d4 = stdMet(2026, 1, 1, 11, 30);
        d4.setWindSpeed(12.0);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2, d3, d4));

        assertEquals(2, result.size());

        assertEquals(10, result.get(0).getHour());
        assertEquals(5.0, result.get(0).getWindSpeed(), 0.001);
        assertEquals(epochAt(2026, 1, 1, 10), result.get(0).getEpochSeconds());

        assertEquals(11, result.get(1).getHour());
        assertEquals(11.0, result.get(1).getWindSpeed(), 0.001);
        assertEquals(epochAt(2026, 1, 1, 11), result.get(1).getEpochSeconds());
    }

    // ========== Spec wave averaging ==========

    @Test
    public void testSpecWaveNumericFieldsAveraged() {
        BuoySpecWaveData d1 = specWave(2026, 3, 7, 14, 0);
        d1.setWaveHeight(1.0);
        d1.setSwellHeight(0.8);
        d1.setSwellPeriod(10.0);

        BuoySpecWaveData d2 = specWave(2026, 3, 7, 14, 30);
        d2.setWaveHeight(2.0);
        d2.setSwellHeight(1.2);
        d2.setSwellPeriod(12.0);

        List<BuoySpecWaveData> result = BuoyDataHourlyAverager.averageSpecWaveByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertEquals(1.5, result.get(0).getWaveHeight(), 0.001);
        assertEquals(1.0, result.get(0).getSwellHeight(), 0.001);
        assertEquals(11.0, result.get(0).getSwellPeriod(), 0.001);
    }

    // ========== Output minute is always zero ==========

    @Test
    public void testOutputMinuteIsZero() {
        BuoyStdMetData d = stdMet(2026, 6, 15, 8, 42);
        d.setWindSpeed(3.0);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Collections.singletonList(d));

        assertEquals(0, result.get(0).getMinute());
    }

    // ========== Most recent logic handles unsorted input ==========

    @Test
    public void testMostRecentHandlesUnsortedInput() {
        BuoyStdMetData d1 = stdMet(2026, 1, 1, 5, 50);
        d1.setWindDirection(350);

        BuoyStdMetData d2 = stdMet(2026, 1, 1, 5, 10);
        d2.setWindDirection(90);

        BuoyStdMetData d3 = stdMet(2026, 1, 1, 5, 30);
        d3.setWindDirection(180);

        List<BuoyStdMetData> result = BuoyDataHourlyAverager.averageStdMetByHour(
                Arrays.asList(d1, d2, d3));

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(350), result.get(0).getWindDirection());
    }

    // ========== Spec wave direction uses most recent ==========

    @Test
    public void testSpecWaveMeanWaveDirectionUsesMostRecent() {
        BuoySpecWaveData d1 = specWave(2026, 1, 1, 0, 0);
        d1.setMeanWaveDirection(90);

        BuoySpecWaveData d2 = specWave(2026, 1, 1, 0, 30);
        d2.setMeanWaveDirection(270);

        List<BuoySpecWaveData> result = BuoyDataHourlyAverager.averageSpecWaveByHour(
                Arrays.asList(d1, d2));

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(270), result.get(0).getMeanWaveDirection());
    }
}
