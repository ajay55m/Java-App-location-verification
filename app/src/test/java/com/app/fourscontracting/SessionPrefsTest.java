package com.app.fourscontracting;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

public class SessionPrefsTest {

    @Test
    public void testIsSameCalendarDay_sameDay() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.AUGUST, 20, 9, 0, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.AUGUST, 20, 16, 30, 0);

        assertTrue(SessionPrefs.isSameCalendarDay(cal1.getTimeInMillis(), cal2.getTimeInMillis()));
    }

    @Test
    public void testIsSameCalendarDay_differentDays() {
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.set(2026, Calendar.AUGUST, 19, 16, 0, 0);

        Calendar calToday = Calendar.getInstance();
        calToday.set(2026, Calendar.AUGUST, 20, 9, 0, 0);

        assertFalse(SessionPrefs.isSameCalendarDay(calYesterday.getTimeInMillis(), calToday.getTimeInMillis()));
    }

    @Test
    public void testIsSameCalendarDay_invalidTimes() {
        assertFalse(SessionPrefs.isSameCalendarDay(0, System.currentTimeMillis()));
        assertFalse(SessionPrefs.isSameCalendarDay(-1, System.currentTimeMillis()));
    }
}
