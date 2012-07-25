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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * This is the start point of the app = list of times (as this is a time keeping
 * app :)
 * 
 * @author FSchiphorst
 * 
 */
public class TimeList extends ListActivity {
	// the main menu
	private static final int CLOSEALL_ID = Menu.FIRST + 1;
	private static final int REPORT_ID = Menu.FIRST + 2;
	private static final int PREFERENCES_ID = Menu.FIRST + 3;

	// add 100 we will always pass through main menu this is item menu
	private static final int CLOSE_ID = Menu.FIRST + 102;
	private static final int DELETE_ID = Menu.FIRST + 103;

	private TimeDbAdapter mDbHelper;
	private MyAdapter mListAdapter;
	private TextView tvTotalTime;
	private Spinner spTemplate;

	private Handler mHandler = new Handler();
	private Cursor TimeCursor;

	private int defLookBack = 5;
	private int defLookBackLimit = 6;


	private int TemplateOn = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		TemplateOn = 0;

		super.onCreate(savedInstanceState);

		setContentView(R.layout.time_list);

		mDbHelper = new TimeDbAdapter(this);
		mDbHelper.open();

		// add button when list is empty = easy add for first
		Button addTimeButton = (Button) findViewById(R.id.buttonAddTime);
		tvTotalTime = (TextView) findViewById(R.id.timeTotal);
		Button addTemplateButton = (Button) findViewById(R.id.buttonTemplate);

		// get the preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		try {
			defLookBack = Integer.parseInt(prefs.getString("TemplateDaysPref",
					"5"));
			defLookBackLimit = Integer.parseInt(prefs.getString(
					"TemplateItemsPref", "5"));
		} catch (Exception e) {
			// don't handle exception
		}

		// get reference to our spinner
		spTemplate = (Spinner) findViewById(R.id.spinnerTemplate);
//		// set up the template spinner
//		Cursor tt = mDbHelper.fetchTemplateTimes(defLookBack, defLookBackLimit);
//		startManagingCursor(tt);
//
//		// create an array to specify which fields we want to display
//		String[] fromc = new String[] { "desc" };
//		// create an array of the display item we want to bind our data to
//		int[] toc = new int[] { android.R.id.text1 };
//     	// create simple cursor adapter
//		SimpleCursorAdapter adapterc = new SimpleCursorAdapter(this,
//				android.R.layout.simple_spinner_item, tt, fromc, toc);
//		adapterc
//				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		spTemplate.setAdapter(adapterc);

		spTemplate
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						// check if we should trigger
						if (TemplateOn-- == 1) { // changed ++ to -- set to 1 in button 
							long rid = id; // parent.getItemIdAtPosition(pos)
							// reset selection
							parent.setSelection(0);
							// create time based
							createTime(rid);
							fillData();
						}
					}

					public void onNothingSelected(AdapterView<?> parent) {
						// Do nothing.
					}
				});

		addTimeButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				// Add Time
				createTime(-1);
				fillData();
			}
		});

		addTemplateButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				// set up the template spinner
				Cursor tt = mDbHelper.fetchTemplateTimes(defLookBack, defLookBackLimit);
				startManagingCursor(tt);

				// create an array to specify which fields we want to display
				String[] fromc = new String[] { "desc" };
				// create an array of the display item we want to bind our data to
				int[] toc = new int[] { android.R.id.text1 };
				// create simple cursor adapter
				SimpleCursorAdapter adapterc = new SimpleCursorAdapter(getBaseContext(),
						android.R.layout.simple_spinner_item, tt, fromc, toc);
				adapterc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spTemplate.setAdapter(adapterc);

				spTemplate.performClick();

				// set the listener to on
				TemplateOn = 1;
			}
		});

		fillData();
		registerForContextMenu(getListView());
	}

	private static int updateEvery = 30000;

	private void fillData() {
		// remove timers
		mHandler.removeCallbacks(mUpdateTimeTask);
		// Get all of the rows from the database and create the item list
		TimeCursor = mDbHelper.fetchAllActiveTimes();
		startManagingCursor(TimeCursor);
		if (TimeCursor.getCount() > 0) {
			// check every 30 seconds
			mHandler.postDelayed(mUpdateTimeTask, updateEvery);
		}

		tvTotalTime.setText(Tricks.returnTotalTime(TimeCursor, false));

		mListAdapter = new MyAdapter(TimeList.this, TimeCursor);
		setListAdapter(mListAdapter);

		TemplateOn = 0;
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			tvTotalTime.setText(Tricks.returnTotalTime(TimeCursor, false));
			if (TimeCursor.getCount() > 0) {
				// check every 30 seconds
				mHandler.postDelayed(mUpdateTimeTask, updateEvery);
			}
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		// stop the timer (we are no longer looking)
		mHandler.removeCallbacks(mUpdateTimeTask);
		// reset the counter
		TemplateOn = 0;
	}

	private class MyAdapter extends ResourceCursorAdapter {

		public MyAdapter(Context context, Cursor cur) {
			super(context, R.layout.time_row, cur);
		}

		@Override
		public View newView(Context context, Cursor cur, ViewGroup parent) {
			LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			return li.inflate(R.layout.time_row, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cur) {
			TextView tvListText1 = (TextView) view
					.findViewById(R.id.textTimeDesc);
			TextView tvListText2 = (TextView) view
					.findViewById(R.id.textTimeStart);
			TextView tvListText3 = (TextView) view
					.findViewById(R.id.textTimeTotal);
			ToggleButton tbListTBut1 = (ToggleButton) view
					.findViewById(R.id.toggleButtonTimeActive);

			tvListText1.setText(cur.getString(cur
					.getColumnIndex(TimeDbAdapter.TIME_DESCRIPTION)));
			tvListText2.setText(Tricks.cal2String(Tricks
					.getCalendarFromFormattedLong(cur.getLong(cur
							.getColumnIndex(TimeDbAdapter.TIME_START))),
					Tricks.strFormat.timeOnly));

			// calculate total time
			long tot = cur
					.getLong(cur.getColumnIndex(TimeDbAdapter.TIME_TOTAL));

			tvListText3.setText(Tricks.tot2Str(tot, true));

			tbListTBut1.setChecked((cur.getInt(cur
					.getColumnIndex(TimeDbAdapter.TIME_ACTIVE)) == 1 ? true
					: false));
			// "remember" the rowid
			tbListTBut1.setTag(cur.getString(cur
					.getColumnIndex(TimeDbAdapter.TIME_ROWID)));

			// build a onclick for the button
			tbListTBut1.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					ToggleButton tbListTBut1 = (ToggleButton) v
							.findViewById(R.id.toggleButtonTimeActive);
					// pick up the rowid
					long rowid = Long
							.parseLong(tbListTBut1.getTag().toString());
					// Perform action on clicks
					if (tbListTBut1.isChecked()) {
						// activate
						mDbHelper.activateTime(rowid);
						fillData();
					} else {
						// deactivate
						mDbHelper.deactivateTime(rowid, false);
						fillData();
					}
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CLOSEALL_ID, CLOSEALL_ID, R.string.menu_closeall);
		menu.add(0, REPORT_ID, REPORT_ID, R.string.menu_reports);
		menu.add(0, PREFERENCES_ID, PREFERENCES_ID, R.string.menu_preferences);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case CLOSEALL_ID:
			// create a close all
			mDbHelper.closeAllTimes();
			Toast.makeText(this, R.string.confirmclosed, Toast.LENGTH_LONG)
					.show();
			fillData();
			return true;
		case REPORT_ID:
			// a report screen (similar to list less options)
			i = new Intent(this, TimeReport.class);
			startActivityForResult(i, 0);
			return true;
		case PREFERENCES_ID:
			i = new Intent(this, TimePreferences.class);
			startActivityForResult(i, 0);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CLOSE_ID, 0, R.string.menu_time_close);
		menu.add(0, DELETE_ID, 0, R.string.menu_time_delete);
	}

	long DeleteID = 0;

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case CLOSE_ID:
			mDbHelper.closeTime(info.id);
			fillData();
			return true;
		case DELETE_ID:
			DeleteID = info.id;

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.warning_lose_time).setCancelable(false)
					.setPositiveButton(this.getString(R.string.but_confirm),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mDbHelper.deleteTime(DeleteID);
									fillData();
								}
							}).setNegativeButton(
							this.getString(R.string.but_cancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();

			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void createTime(long timeId) {
		Intent i = new Intent(this, TimeEdit.class);
		if (timeId > 0) {
			// we have a previous timeId so use that to copy
			i.putExtra("MODE", TimeEdit.ACTIVITY_COPY);
			i.putExtra(TimeDbAdapter.TIME_ROWID, timeId);
		} else {
			i.putExtra("MODE", TimeEdit.ACTIVITY_CREATE);
		}
		startActivityForResult(i, TimeEdit.ACTIVITY_CREATE);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, TimeEdit.class);
		i.putExtra(TimeDbAdapter.TIME_ROWID, id);
		i.putExtra("MODE", TimeEdit.ACTIVITY_EDIT);
		startActivityForResult(i, TimeEdit.ACTIVITY_EDIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();
	}

}
