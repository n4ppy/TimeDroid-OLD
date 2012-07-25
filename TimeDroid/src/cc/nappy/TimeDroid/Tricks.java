package cc.nappy.TimeDroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.database.Cursor;

/**
 * Tricks to convert stuff to stuff
 * @author FSchiphorst
 */
public class Tricks {
	
	/**
	 * returns total time for all the records 
	 * @param cur
	 * @return total as string
	 */
	public static String returnTotalTime(Cursor cur, boolean noSeconds) {
		String rVal = "00:00:00";
		
        if (cur != null) {
        	long totTotal = 0;
        	cur.moveToFirst();
            while (cur.isAfterLast() == false) {
            	long tot = cur.getLong(cur.getColumnIndex("total"));
                // check if this is an active time = more time
            	// this bit "borrowed" from DB ;) 
            	if (cur.getInt(cur.getColumnIndex(TimeDbAdapter.TIME_ACTIVE)) == 1) {
        	    	// calculate difference between now and current start time
                    Calendar cCurrentTime = Calendar.getInstance();
                    cCurrentTime.setTime(Tricks.getCalendarFromFormattedLong(cur.getLong(cur.getColumnIndex(TimeDbAdapter.TIME_CURRENT))).getTime());
                    long diff = Calendar.getInstance().getTimeInMillis() - cCurrentTime.getTimeInMillis();
                    // as seconds
                    diff /= 1000;

                    tot += diff;
                } 

            	totTotal += tot;
            	cur.moveToNext();
            }
            
            // make it a time
            rVal = tot2Str(totTotal, noSeconds);
        }
		return rVal;
	}
	
    /* date and time stuff 
     * http://joesapps.blogspot.com/2011/02/managing-date-and-time-data-in-android.html
     */
    private static final String DATE_FORMAT = "yyyyMMddHHmmss";
    private static final SimpleDateFormat dateFormat = new
    SimpleDateFormat(DATE_FORMAT);
    
    public static long formatDateAsLong(Calendar cal){
        return Long.parseLong(dateFormat.format(cal.getTime()));
     }
     
     public static Calendar getCalendarFromFormattedLong(long l){
        try {
        	Calendar c = Calendar.getInstance();
        	c.setTime(dateFormat.parse(String.valueOf(l)));
        	return c;
        } catch (ParseException e) {
        		return null;
        }
     }

     /* date conversion functions  assume "dd/MM/yyyy" date format */
     public static String tot2Str(long tot, boolean noSeconds) {
    	long toth = (int)(tot / 3600); // 1h = 3600s
    	tot -= toth * 3600;
    	long totm = (int)(tot / 60);
    	long tots = tot % 60;

    	StringBuilder sb = new StringBuilder()
 		.append(String.format("%02d",toth)).append(":")
 		.append(String.format("%02d",totm));
    	if (!noSeconds) 
    		sb.append(":").append(String.format("%02d",tots));
    	return sb.toString();
     }
     
     public static long str2Tot(String str) {
     	long rVal = 0;
     	str = str.trim();
     	try {
     		if (str.length() > 5)
         		rVal = Integer.parseInt(str.substring(0,2)) * 3600 + Integer.parseInt(str.substring(3,5)) * 60 + Integer.parseInt(str.substring(6,8));    		
     		else
        		rVal = Integer.parseInt(str.substring(0,2)) * 3600 + Integer.parseInt(str.substring(3,5)) * 60;    		
     	}
     	catch (Exception e) {
     		// 0
     	}
     	return rVal;
     }
     
     public static enum strFormat {
    	 dateAndTime,
    	 dateOnly,
    	 timeOnly
     }
     
     public static String cal2String(Calendar cal, strFormat sf) {
    	 String DATE_FORMAT = "";
    	 switch (sf) {
    	 case dateAndTime:
    		 DATE_FORMAT = "dd/MM/yyyy HH:mm";
    		 break;
    	 case dateOnly:
    		 DATE_FORMAT = "dd/MM/yyyy";
    		 break;
    	 case timeOnly:
    		 DATE_FORMAT = "HH:mm:ss";
    		 break;
    	 }
         SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
      	
         return dateFormat.format(cal.getTime());    	 
     }
     
     public static Calendar dstrTstr2cal(String dStr, String tStr) {
     	Calendar rVal = Calendar.getInstance();
     	dStr = dStr.trim();
     	tStr = tStr.trim();
     	if (tStr.length() <= 5)
     		tStr += ":00";
     	try {
     		// date and time string to yyyyMMddHHmmss 
     		rVal.setTime(dateFormat.parse(new StringBuilder().append(dStr.substring(6,10))
                                                             .append(dStr.substring(3,5))
                                                             .append(dStr.substring(0,2))
                                                             .append(tStr.substring(0,2))
                                                             .append(tStr.substring(3,5))
                                                             .append(tStr.substring(6,8)).toString()));    		
     	}
     	catch (Exception e) {
     		rVal = null;
     	}
     	return rVal;
     }

}
