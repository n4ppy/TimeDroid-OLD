package cc.nappy.TimeDroid.test;

import java.util.Calendar;

import android.database.Cursor;
import android.test.mock.MockCursor;

import junit.framework.TestCase;

import cc.nappy.TimeDroid.*;
import cc.nappy.TimeDroid.Tricks.strFormat;

public class TrickTest extends TestCase {
	private Calendar testCal = Calendar.getInstance();
	private Calendar resultCal = Calendar.getInstance();

	// compare only date and time fields
	private void compareCalendars(Calendar testCal, Calendar resultCal, Boolean secondsZero) {
		assertEquals(testCal.get(Calendar.YEAR) , resultCal.get(Calendar.YEAR));
		assertEquals(testCal.get(Calendar.MONTH ) , resultCal.get(Calendar.MONTH));
		assertEquals(testCal.get(Calendar.DAY_OF_MONTH) , resultCal.get(Calendar.DAY_OF_MONTH));
		assertEquals(testCal.get(Calendar.HOUR) , resultCal.get(Calendar.HOUR));
		assertEquals(testCal.get(Calendar.MINUTE) , resultCal.get(Calendar.MINUTE));
		if (secondsZero)
			assertEquals(00 , resultCal.get(Calendar.SECOND));
		else
		   assertEquals(testCal.get(Calendar.SECOND) , resultCal.get(Calendar.SECOND));

	}
	
	@Override
	public void setUp() throws Exception {
	    super.setUp();
	    // ! month is ZERO based ! so test with "06" !
	    testCal.set(1999, 5, 26, 23, 11, 52);
	}

	/************/
	/* curTotal */
	/************/
	
//public void testcurTotal_emptycur() {
//		Cursor cur = MockCursor;
//		assertEquals("00:00:00", Tricks.curTotal(cur, true));
//	}
	
	public void testcurTotal_nocur() {
		Cursor cur = null;
		assertEquals("00:00:00", Tricks.returnTotalTime(cur, true));
	}
	
	/********************/
	/* formatDateAsLong */
	/********************/

	public void testformatDateAsLong() {
		assertEquals(19990626231152L, Tricks.formatDateAsLong(testCal));	
	}
	
    /********************************/
	/* getCalendarFromFormattedLong */
    /********************************/

	public void testgetCalendarFromFormattedLong() {
		compareCalendars(testCal, Tricks.getCalendarFromFormattedLong(19990626231152L), false);	
	}

	public void testgetCalendarFromFormattedLong_fail() {
		assertEquals(null,Tricks.getCalendarFromFormattedLong(-1));	
	}
	
	/***********/
	/* tot2Str */
	/***********/

	public void testtot2Str_withsecs() {
		assertEquals("23:11:52", Tricks.tot2Str(83512, false));
	}
	
	public void testtot2Str_nosecs() {
		assertEquals("23:11", Tricks.tot2Str(83460, true));
	}
	
	/***********/
	/* str2Tot */
	/***********/
	
	public void teststr2Tot_fail() {
		assertEquals(0, Tricks.str2Tot("xxxxxx"));
	}
	
	public void teststr2Tot_nosecs() {
		assertEquals(83460, Tricks.str2Tot("23:11"));
	}
	
	public void teststr2Tot_withsecs() {
		assertEquals(83512, Tricks.str2Tot("23:11:52"));
	}
	
	
	/**************/
	/* cal2String */
	/**************/
	
	public void testcal2String_timeOnly() {
        assertEquals("23:11:52", Tricks.cal2String(testCal, strFormat.timeOnly));
	}

	public void testcal2String_dateOnly() {
        assertEquals("26/06/1999", Tricks.cal2String(testCal, strFormat.dateOnly));
	}

	public void testcal2String_dateAndTime() {
        assertEquals("26/06/1999 23:11", Tricks.cal2String(testCal, strFormat.dateAndTime));
	}
	
	/****************/
	/* dstrTstr2cal */
	/****************/
	
	public void testdstrTstr2cal_withsecs() {
		resultCal = Tricks.dstrTstr2cal("26/06/1999","23:11:52");
		compareCalendars(testCal, resultCal, false);
	}
	
	public void testdstrTstr2cal_nosecs() {
		resultCal = Tricks.dstrTstr2cal("26/06/1999","23:11:00");
		compareCalendars(testCal, resultCal, true);
	}

	public void testdstrTstr2cal_catch() {
		resultCal = Tricks.dstrTstr2cal("xxxxxxxxx","yy");
		assertEquals(null , resultCal);
	}
}
