/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")savedInstanceState;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.nappy.TimeDroid;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * Create and Edit a time record
 * Bundle extra MODE sets the mode to operate
 * @author FSchiphorst
 *
 */
public class TimeEdit extends Activity {
    public static final int ACTIVITY_CREATE = 50;
    public static final int ACTIVITY_EDIT = 51;
    public static final int ACTIVITY_COPY = 52; 
	
    private static final int DATE_DIALOG_ID = 0;
    private static final int TIME_DIALOG_ID = 1;
    private static final int TOTAL_DIALOG_ID = 2;
    
    private TimeDbAdapter mDbHelper;
    private TextView tvStartDate;
    private TextView tvStartTime;
    private TextView tvTotalTime;
    private TextView tvDescription;
    
    private int MODE;
    private long EDITrowID;
    private String saveSecs;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_edit);

        // set up a connection to the database
        mDbHelper = new TimeDbAdapter(this);
        mDbHelper.open();

        // set up the customer spinner
        Cursor cc = mDbHelper.fetchAllActiveCustomers();
        startManagingCursor(cc);
        
        // create an array to specify which fields we want to display 
    	String[] fromc = new String[]{TimeDbAdapter.CUSTOMER_NAME }; 
    	// create an array of the display item we want to bind our data to 
    	int[] toc = new int[]{android.R.id.text1}; 
    	// create simple cursor adapter 
    	SimpleCursorAdapter adapterc = 
    	new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, cc, fromc, toc ); 
    	adapterc.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item ); 
    	// get reference to our spinner 
    	Spinner spinnerc = (Spinner) findViewById(R.id.ClientSpinner);
	    spinnerc.setAdapter(adapterc);

	    // set up the projects spinner
        Cursor cp = mDbHelper.fetchAllActiveProjects();
        startManagingCursor(cp);
        
        // create an array to specify which fields we want to display 
    	String[] fromp = new String[]{TimeDbAdapter.PROJECT_DESCRIPTION}; 
    	// create an array of the display item we want to bind our data to 
    	int[] top = new int[]{android.R.id.text1}; 
    	// create simple cursor adapter 
    	SimpleCursorAdapter adapterp = 
    	new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, cp, fromp, top ); 
    	adapterp.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item ); 
    	// get reference to our spinner 
    	Spinner spinnerp = (Spinner) findViewById(R.id.ProjectSpinner);
	    spinnerp.setAdapter(adapterp);

        // capture our View elements
        tvStartDate = (TextView) findViewById(R.id.StartDate);
        tvStartTime = (TextView) findViewById(R.id.StartTime);
        tvTotalTime = (TextView) findViewById(R.id.TotalTime);
        tvDescription = (TextView) findViewById(R.id.Description);

        // add a click listener to the start date to display date picker
        tvStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
        	public void onClick(View v) {
        		showDialog(DATE_DIALOG_ID);
        	}
        });
        
        // add a click listener to the start time to display time picker
        tvStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
        	public void onClick(View v) {
        		showDialog(TIME_DIALOG_ID);
        	}
        });
        
        // add a click listener to the total time to display time picker
        tvTotalTime.setOnClickListener(new View.OnClickListener() {
            @Override
        	public void onClick(View v) {
        		showDialog(TOTAL_DIALOG_ID);
        	}
        });
        
        Calendar cStartTime = Calendar.getInstance();
        int cID = -1;
        int pID = -1;
        
    	Bundle bundle = getIntent().getExtras();
        MODE = bundle.getInt("MODE");
        if(MODE == ACTIVITY_CREATE) {
        	// CREATE MODE set defaults
            // get the preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String defDesc = prefs.getString("DescriptionPref",this.getString(R.string.default_description));
            // get the spinner values from def (or -1)
            cID = Integer.parseInt(prefs.getString("CustomerPref","-1"));
            pID = Integer.parseInt(prefs.getString("ProjectPref","-1"));
            
            tvDescription.setText(defDesc);  
            // set 00:00 for total time
            tvTotalTime.setText("00:00:00");
        }
        
        if(MODE == ACTIVITY_COPY) {
        	// get data from timerow 
        	long COPYrowID = bundle.getLong(TimeDbAdapter.TIME_ROWID,-1);
        	// always deactivate in case we are/were active and keep active if it was
            Cursor cur = mDbHelper.fetchTime(COPYrowID);

            // get the spinner values from DB
            cID = cur.getInt(cur.getColumnIndex(TimeDbAdapter.TIME_CUSTOMER)); 
            pID = cur.getInt(cur.getColumnIndex(TimeDbAdapter.TIME_PROJECT)); 
            
            // set the description
            tvDescription.setText(cur.getString(cur.getColumnIndex(TimeDbAdapter.TIME_DESCRIPTION)));

            // set 00:00 for total time
            tvTotalTime.setText("00:00:00");
            
            // set mode to create so this gets saved
            MODE = ACTIVITY_CREATE;
        }
        
        if(MODE == ACTIVITY_EDIT) {
        	// EDIT mode get id and load from DB
        	EDITrowID = bundle.getLong(TimeDbAdapter.TIME_ROWID,-1);
        	
        	// always deactivate in case we are/were active and keep active if it was
        	mDbHelper.deactivateTime(EDITrowID, true);
            Cursor cur = mDbHelper.fetchTime(EDITrowID);

            // get the spinner values from DB
            cID = cur.getInt(cur.getColumnIndex(TimeDbAdapter.TIME_CUSTOMER)); 
            pID = cur.getInt(cur.getColumnIndex(TimeDbAdapter.TIME_PROJECT)); 
            
            // set the description
            tvDescription.setText(cur.getString(cur.getColumnIndex(TimeDbAdapter.TIME_DESCRIPTION)));

            // get the start time
            cStartTime.setTime(Tricks.getCalendarFromFormattedLong(cur.getLong(cur.getColumnIndex(TimeDbAdapter.TIME_START))).getTime());
            
            // set the total time
            long tot = cur.getLong(cur.getColumnIndex(TimeDbAdapter.TIME_TOTAL));
            tvTotalTime.setText(Tricks.tot2Str(tot, false));
        }

        if (cID > 0)  {
        	// set the customer(id) to find by value we have to spin the spinner?
        	for (int i = 0; i < spinnerc.getCount(); i++) {
        		Cursor value = (Cursor) spinnerc.getItemAtPosition(i);
        		long sid = value.getLong(value.getColumnIndex("_id"));
        		if (sid == cID) {
        			spinnerc.setSelection(i);
        			break;
        		}
        	}
        }
        
        if (pID > 0) {
        	// set the project(id) to find by value we have to spin the spinner?            
        	for (int i = 0; i < spinnerp.getCount(); i++) {
        		Cursor value = (Cursor) spinnerp.getItemAtPosition(i);
        		long sid = value.getLong(value.getColumnIndex("_id"));
        		if (sid == pID) {
        			spinnerp.setSelection(i);
        			break;
        		}
        	}
        }

        // set in create or edit now update screen
        tvStartDate.setText(Tricks.cal2String(cStartTime, Tricks.strFormat.dateOnly));
        tvStartTime.setText(Tricks.cal2String(cStartTime, Tricks.strFormat.timeOnly));
        
        // get link to the save button on the form cancel = back key
        Button saveButton = (Button) findViewById(R.id.ButtonSave);
        
        // create button handlers
        saveButton.setOnClickListener(new Button.OnClickListener() {
        	
            public void onClick(View view) {
            	Spinner spinnerc = (Spinner) findViewById(R.id.ClientSpinner);
            	Spinner spinnerp = (Spinner) findViewById(R.id.ProjectSpinner);
            	long tot = Tricks.str2Tot(tvTotalTime.getText().toString());
            	Calendar start = Tricks.dstrTstr2cal(tvStartDate.getText().toString(),tvStartTime.getText().toString());
                if(MODE == ACTIVITY_CREATE) {
                	long rowId = mDbHelper.createTime(spinnerc.getSelectedItemId(), 
   			                                          spinnerp.getSelectedItemId(),
   			                                          tvDescription.getText().toString(),
   			                                          start,
   			                                          tot); 
                	if (rowId != -1) {
                		// activate the row
                		mDbHelper.activateTime(rowId);
                	}
                }
                if(MODE == ACTIVITY_EDIT) {
                	mDbHelper.updateTime( EDITrowID, 
                			spinnerc.getSelectedItemId(), 
                			spinnerp.getSelectedItemId(), 
                			tvDescription.getText().toString(), 
                			start, 
                			tot);
                }
            	setResult(RESULT_OK);
                finish();
            }

        });
   }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Calendar cal = null; 
    	 switch (id) {
    	 case DATE_DIALOG_ID:
    		 // don't care about the time
    		 cal = Tricks.dstrTstr2cal(tvStartDate.getText().toString(), "00:00:00");
    		 return new DatePickerDialog(this,
    				 mDateSetListener,
    				 cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    	 case TIME_DIALOG_ID:
    		 // don't care about the date
    		 cal = Tricks.dstrTstr2cal("01/01/2000", tvStartTime.getText().toString());
    		 return new TimePickerDialog(this,
    				 mTimeSetListener,
    				 cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
    	 case TOTAL_DIALOG_ID:
    		 // don't care about the date
    		 String tt = tvTotalTime.getText().toString();
             saveSecs = tt.substring(6,8);
    		 cal = Tricks.dstrTstr2cal( "01/01/2000", tt);
    		 return new TimePickerDialog(this,
    				 mTotalSetListener,
    				 cal.get(Calendar.HOUR_OF_DAY) , cal.get(Calendar.MINUTE), true);
    		 }
    	 return null;
    }
    
    // the call back received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener =
    	new DatePickerDialog.OnDateSetListener() {
    	    public void onDateSet(DatePicker view, int year,
    	    		              int monthOfYear, int dayOfMonth) {
    	    	tvStartDate.setText(
    			new StringBuilder()
    			// Month is 0 based so add 1
    			.append(String.format("%02d",dayOfMonth)).append("/")
    			.append(String.format("%02d",monthOfYear + 1)).append("/")
    			.append(String.format("%02d",year)));
    	    }
    };

    // the call back received when the user "sets" the time in the dialog
    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
    	new TimePickerDialog.OnTimeSetListener() {
    	    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    	    	tvStartTime.setText(
    			new StringBuilder()
    			.append(String.format("%02d",hourOfDay)).append(":")
    			.append(String.format("%02d",minute)));
    	    }
    };

    // the call back received when the user "sets" the time in the dialog
    private TimePickerDialog.OnTimeSetListener mTotalSetListener =
    	new TimePickerDialog.OnTimeSetListener() {
    	    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    	    	tvTotalTime.setText(
    			new StringBuilder()
    			.append(String.format("%02d",hourOfDay)).append(":")
    			.append(String.format("%02d",minute)).append(":")
    			.append(saveSecs));
    	    }
    };

}
