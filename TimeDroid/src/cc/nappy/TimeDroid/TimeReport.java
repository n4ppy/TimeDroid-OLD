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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Reports on time and option to edit / delete time
 * Option to export files to dropbox
 * @author FSchiphorst
 *
 */
public class TimeReport extends ListActivity {
    private static final int DATESTART_DIALOG_ID = 100;
    private static final int DATEEND_DIALOG_ID = 101;
    
    // the main menu
    private static final int TODAY_ID = Menu.FIRST + 1;
    private static final int MONTHTHIS_ID = Menu.FIRST + 2;
    private static final int MONTHLAST_ID = Menu.FIRST + 3;
    private static final int ALLTIME_ID = Menu.FIRST + 4;
    private static final int EXPORTLIST_ID = Menu.FIRST + 5;

    // add 100 we will always pass through main menu this is item menu
    private static final int DELETE_ID = Menu.FIRST + 103;

    private TimeDbAdapter mDbHelper;
    private MyAdapter mListAdapter;
    
    private EditText etStartDate;
    private EditText etEndDate;
    private TextView tvTotalTime;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.time_report);

        mDbHelper = new TimeDbAdapter(this);
        mDbHelper.open();
        
        etStartDate = (EditText) findViewById(R.id.startReportDate);
        etEndDate = (EditText) findViewById(R.id.endReportDate);
        tvTotalTime = (TextView) findViewById(R.id.reportTotal);
        // default to today
        Calendar cal = Calendar.getInstance();
        etStartDate.setText(Tricks.cal2String(cal, Tricks.strFormat.dateOnly));
        etEndDate.setText(Tricks.cal2String(cal, Tricks.strFormat.dateOnly));

        // add a click listener to the start date to display date picker
        etStartDate.setOnClickListener(new EditText.OnClickListener() {
            @Override
        	public void onClick(View v) {
        		showDialog(DATESTART_DIALOG_ID);
        	}
        });
        
        // add a click listener to the end date to display date picker
        etEndDate.setOnClickListener(new EditText.OnClickListener() {
            @Override
        	public void onClick(View v) {
        		showDialog(DATEEND_DIALOG_ID);
        	}
        });
        
        fillData();
        registerForContextMenu(getListView());        
    }

    private void fillData() {
        // Get all of the rows from the database and create the item list
    	Cursor TimeCursor = mDbHelper.fetchAllReportTimes
    	       (Tricks.dstrTstr2cal(etStartDate.getText().toString(), "00:00:00"), 
    	        Tricks.dstrTstr2cal(etEndDate.getText().toString(), "00:00:00"));
        startManagingCursor(TimeCursor);

        tvTotalTime.setText(Tricks.returnTotalTime(TimeCursor, true));

        // cursor seems to survive the abuse :)

        mListAdapter = new MyAdapter(TimeReport.this, TimeCursor); 
        setListAdapter(mListAdapter);
    }
    
    private class MyAdapter extends ResourceCursorAdapter { 
    	// _id is expected!!
        public MyAdapter(Context context, Cursor cur) { 
            super(context, R.layout.time_report_row, cur); 
        } 
 
        @Override 
        public View newView(Context context, Cursor cur, ViewGroup parent) { 
            LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
            return li.inflate(R.layout.time_report_row, parent, false); 
        } 

        @Override 
        public void bindView(View view, Context context, Cursor cur) {
        	// add date (as we may watch more then one day here)
            TextView tvListText1 = (TextView)view.findViewById(R.id.textTimeRepProj); 
            TextView tvListText2 = (TextView)view.findViewById(R.id.textTimeRepStart); 
            TextView tvListText3 = (TextView)view.findViewById(R.id.textTimeRepTotal); 
            TextView tvListText4 = (TextView)view.findViewById(R.id.textTimeRepDesc); 
            ToggleButton tbListTBut1 = (ToggleButton)view.findViewById(R.id.toggleButtonTimeRepActive);

            tvListText1.setText(cur.getString(cur.getColumnIndex("pdesc"))); 
            tvListText4.setText(cur.getString(cur.getColumnIndex("tdesc"))); 
            tvListText2.setText(Tricks.cal2String(Tricks.getCalendarFromFormattedLong(cur.getLong(cur.getColumnIndex("start"))), Tricks.strFormat.dateAndTime));
            
            // calculate total time
            long tot = cur.getLong(cur.getColumnIndex("total"));

            tvListText3.setText(Tricks.tot2Str(tot, true));
            
            tbListTBut1.setChecked((cur.getInt(cur.getColumnIndex("active")) == 1 ? true:false));
        } 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, TODAY_ID, TODAY_ID, R.string.menu_report_today);
        menu.add(0, MONTHTHIS_ID, MONTHTHIS_ID, R.string.menu_report_monththis);
        menu.add(0, MONTHLAST_ID, MONTHLAST_ID, R.string.menu_report_monthlast);
        menu.add(0, ALLTIME_ID, ALLTIME_ID, R.string.menu_report_alltime);
        menu.add(0, EXPORTLIST_ID, EXPORTLIST_ID, R.string.menu_report_export);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Calendar cal = Calendar.getInstance();
    	// month and date tricks
        switch(item.getItemId()) {
        case TODAY_ID:
            // set both to today
        	etStartDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
            etEndDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
        	fillData();
            return true;
        case MONTHTHIS_ID:
        	// set filters to this month
        	cal.set(Calendar.DAY_OF_MONTH, 1); // first of the month
        	etStartDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
        	cal.add(Calendar.MONTH, 1); // set to next month
        	cal.add(Calendar.DAY_OF_MONTH, - 1); // Subtract 1 = last day of this month
            etEndDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
        	fillData();
            return true;
        case MONTHLAST_ID:
        	// set filters to next month
        	cal.set(Calendar.DAY_OF_MONTH, 1); // first of the month
        	cal.add(Calendar.MONTH, -1); // set to last month
        	etStartDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
        	cal.add(Calendar.MONTH, 1); // set to next last = this month ;)
        	cal.add(Calendar.DAY_OF_MONTH, - 1); // Subtract 1 = last day of last month
            etEndDate.setText(Tricks.cal2String(cal,Tricks.strFormat.dateOnly));
        	fillData();
            return true;
        case ALLTIME_ID:
            // set uber min and max dates :)
        	etStartDate.setText("01/01/1900");
            etEndDate.setText("31/12/2999");
        	fillData();
            return true;
        case EXPORTLIST_ID:
        	// export current view to csv
        	exportCSV();
        	Toast.makeText(this, R.string.confirmexport, Toast.LENGTH_LONG).show();
        	fillData();
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_time_delete);
	}

    long DeleteID = 0;
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
    	case DELETE_ID:
        	DeleteID = info.id;
        	
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage(R.string.warning_lose_time)
        	       .setCancelable(false)
        	       .setPositiveButton(this.getString(R.string.but_confirm), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	               mDbHelper.deleteTime(DeleteID);
        	               fillData();
        	           }
        	       })
        	       .setNegativeButton(this.getString(R.string.but_cancel), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	alert.show();
        	
	        return true;
		}
		return super.onContextItemSelected(item);
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, TimeEdit.class);
        i.putExtra("_id", id); 
        i.putExtra("MODE", TimeEdit.ACTIVITY_EDIT);
        startActivityForResult(i, TimeEdit.ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
            fillData();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	Calendar cal = null; 
    	 switch (id) {
    	 case DATESTART_DIALOG_ID:
    		 cal = Tricks.dstrTstr2cal(etStartDate.getText().toString(), "00:00:00");
    		 return new DatePickerDialog(this,
    				 mDateStartSetListener,
    				 cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    	 case DATEEND_DIALOG_ID:
    		 cal = Tricks.dstrTstr2cal(etEndDate.getText().toString(), "00:00:00");
    		 return new DatePickerDialog(this,
    				 mDateEndSetListener,
    				 cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
  		 }
    	 return null;
    }

    // the call back received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateStartSetListener =
    	new DatePickerDialog.OnDateSetListener() {
    	    public void onDateSet(DatePicker view, int year,
    	    		              int monthOfYear, int dayOfMonth) {
    	    	etStartDate.setText(
    			new StringBuilder()
    			// Month is 0 based so add 1
    			.append(String.format("%02d",dayOfMonth)).append("/")
    			.append(String.format("%02d",monthOfYear + 1)).append("/")
    			.append(String.format("%02d",year)));
    	    	fillData();
    	    }
    };

    // the call back received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateEndSetListener =
    	new DatePickerDialog.OnDateSetListener() {
    	    public void onDateSet(DatePicker view, int year,
    	    		              int monthOfYear, int dayOfMonth) {
    	    	etEndDate.setText(
    			new StringBuilder()
    			// Month is 0 based so add 1
    			.append(String.format("%02d",dayOfMonth)).append("/")
    			.append(String.format("%02d",monthOfYear + 1)).append("/")
    			.append(String.format("%02d",year)));
    	    	fillData();
    	    }
    };
    
	void exportCSV() {
		// export as CSV file
		try {
			Cursor ExportCursor = mDbHelper.fetchAllReportTimes
 	                             (Tricks.dstrTstr2cal(etStartDate.getText().toString(), "00:00:00"), 
 	                              Tricks.dstrTstr2cal(etEndDate.getText().toString(), "00:00:00"));
			
			// check if we can write a file
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()){

		        boolean validRecord = ExportCursor.moveToFirst();

				if (validRecord) {
					// we have at least one record
					// open the export file
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    String fName = prefs.getString("ExportFilenamePref",this.getString(R.string.default_filename_export));
					File timefile = new File(root, fName);
					FileWriter timewriter = new FileWriter(timefile);
					BufferedWriter out = new BufferedWriter(timewriter);

					// write a header
					out.write("Id");
					out.write(",Date");
					out.write(",Time");
					out.write(",Active");
					out.write(",Current");
					out.write(",Total");
					out.write(",ClientId");
					out.write(",Client");
					out.write(",ProjectId");
					out.write(",Project");
					out.write(",Description");
					out.write(",Archived");
					out.newLine();

					while (validRecord) {
						// while we have reocords write the time to the file
						try {
							Calendar start = Tricks.getCalendarFromFormattedLong(
									ExportCursor.getLong(ExportCursor.getColumnIndex("start")));
							Calendar current = Tricks.getCalendarFromFormattedLong(
								      ExportCursor.getLong(ExportCursor.getColumnIndex("current")));
				            long tot = ExportCursor.getLong(ExportCursor.getColumnIndex("total"));

				            out.write(new StringBuilder()
			    	                  .append(ExportCursor.getString(ExportCursor.getColumnIndex("_id"))) 
   							          .append(",")
   							          .append(Tricks.cal2String(start, Tricks.strFormat.dateOnly))
   							          .append(",")
   							          .append(Tricks.cal2String(start, Tricks.strFormat.timeOnly))
   							          .append(",")
   							          .append(ExportCursor.getString(ExportCursor.getColumnIndex("active"))) 
   							          .append(",") 
   							          .append(Tricks.cal2String(current, Tricks.strFormat.dateAndTime)) 
   							          .append(",")
   							          .append(Tricks.tot2Str(tot, false))
   							          .append(",")
   							          .append( ExportCursor.getString(ExportCursor.getColumnIndex("crow")))
   							          .append(",")
   							          .append( ExportCursor.getString(ExportCursor.getColumnIndex("name"))) 
							          .append(",")
							          .append( ExportCursor.getString(ExportCursor.getColumnIndex("prow"))) 
   							          .append(",")
   							          .append( ExportCursor.getString(ExportCursor.getColumnIndex("pdesc"))) 
   							          .append(",")
   							          .append( ExportCursor.getString(ExportCursor.getColumnIndex("tdesc")))
   							          .append(",")
   							          .append(ExportCursor.getString(ExportCursor.getColumnIndex("archived"))).toString());
						}
						catch (Exception e) {
                           // just in case we have a dodgy value (in testing)
						}
						out.newLine();						

						// get the next record
						validRecord = ExportCursor.moveToNext();
					}

					ExportCursor.close();
					// close the file after last record
					out.close();
					
					// test pref
				    if (prefs.getBoolean("DropBoxPref", true)) {
						// send away to dropbox
						final Intent DBIntent = new Intent(android.content.Intent.ACTION_SEND);
						DBIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(timefile));
						DBIntent.setType("*/*");
						// select dropbox
						DBIntent.setPackage("com.dropbox.android");
						// wait for the send (or back key to abort)
						startActivityForResult(DBIntent,0);
				    }
				}
			}
			else {
	        	Toast.makeText(this, "No Write", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			// log any errors
			Log.e("Export", e.getMessage());
		}
	}

}
