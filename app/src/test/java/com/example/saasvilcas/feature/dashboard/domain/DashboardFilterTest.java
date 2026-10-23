package com.example.saasvilcas.feature.dashboard.domain;

import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class DashboardFilterTest {
    @Test public void todayUsesLimaEvenWhenUtcIsAlreadyTheNextDay() throws Exception {
        SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd HH:mm"); utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("2026-10-03",DashboardFilter.iso(DashboardFilter.today(utc.parse("2026-10-04 02:00").getTime())));
    }
    @Test public void sundayBelongsToWeekStartingMondayAcrossMonthBoundary() {
        long today=DashboardFilter.day(2026,9,4); DashboardFilter filter=DashboardFilter.forPeriod(1,today,today,today);
        assertEquals("desde=2026-09-28&hasta=2026-10-04",filter.query()); assertEquals(filter.toDay,filter.chartToDay);
    }
    @Test public void weekChartIncludesFutureDaysButKpisEndToday() {
        long today=DashboardFilter.day(2026,8,30); DashboardFilter filter=DashboardFilter.forPeriod(1,today,today,today);
        assertEquals("desde=2026-09-28&hasta=2026-09-30",filter.query());
        assertEquals("desde=2026-09-28&hasta=2026-10-04",filter.chartQuery());
    }
    @Test public void monthHandlesLeapYearAndYearUsesNaturalChartEnd() {
        long today=DashboardFilter.day(2024,1,12);
        assertEquals("desde=2024-02-01&hasta=2024-02-29",DashboardFilter.forPeriod(2,today,today,today).chartQuery());
        assertEquals("desde=2024-01-01&hasta=2024-02-12",DashboardFilter.forPeriod(3,today,today,today).query());
        assertEquals("desde=2024-01-01&hasta=2024-12-31",DashboardFilter.forPeriod(3,today,today,today).chartQuery());
    }
    @Test public void customRangeIsInclusiveAndDoesNotPadTheChart() {
        long from=DashboardFilter.day(2026,0,9),to=DashboardFilter.day(2026,0,20);
        DashboardFilter filter=DashboardFilter.forPeriod(4,to,from,to);
        assertEquals("desde=2026-01-09&hasta=2026-01-20",filter.query()); assertEquals(filter.query(),filter.chartQuery());
    }
    @Test(expected=IllegalArgumentException.class) public void reversedRangeIsRejected() {
        new DashboardFilter(4,20,10,10);
    }
}
