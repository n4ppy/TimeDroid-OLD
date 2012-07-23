package cc.nappy.TimeDroid;

// android:targetPackage="cc.nappy.TimeDroid"


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

// http://www.kaloer.com/android-preferences

public class TimePreferences extends PreferenceActivity  {
    private TimeDbAdapter mDbHelper;
    private Preference CustList; 
    static final int CUST_LIST_REQUEST = 999;
    private Preference ProjList;
    static final int PROJ_LIST_REQUEST = 998;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.preferences);
    	
        // start custlist by hand so we get onactivityresult back (and can refresh the pref spinner
    	CustList = (Preference) findPreference("CustList");
    	CustList.setOnPreferenceClickListener( new Preference.OnPreferenceClickListener() {
    		@Override
    		public boolean onPreferenceClick(Preference preference) {
    			Intent i = new Intent(getBaseContext(), CustomersList.class);
    			startActivityForResult(i, CUST_LIST_REQUEST);
    			return true;
    		}
    	});
    	
        // start projlist by hand so we get onactivityresult back (and can refresh the pref spinner
    	ProjList = (Preference) findPreference("ProjList");
    	ProjList.setOnPreferenceClickListener( new Preference.OnPreferenceClickListener() {
    		@Override
    		public boolean onPreferenceClick(Preference preference) {
    			Intent i = new Intent(getBaseContext(), ProjectsList.class);
    			startActivityForResult(i, PROJ_LIST_REQUEST);
    			return true;
    		}
    	});

    	// on create fill both spinners
    	spinnerfiller(true, true);
    }

    // simple way to refresh spinners
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CUST_LIST_REQUEST) 
			spinnerfiller(false, true);
        if (requestCode == PROJ_LIST_REQUEST) 
			spinnerfiller(true, false);
    }
    
    /**
     * Fill cust and/or proj spinners
     * @param proj
     * @param cust
     */
    private void spinnerfiller(Boolean proj, Boolean cust) {
    	// set up a connection to the database
    	mDbHelper = new TimeDbAdapter(this);
    	mDbHelper.open();

    	if (proj) {
    		ListPreference listPreferenceProject = (ListPreference) findPreference("ProjectPref");
    		if (listPreferenceProject != null) {
    			Cursor c = mDbHelper.fetchAllActiveProjects();
    			startManagingCursor(c);

    			int count = c.getCount();
    			CharSequence[] entries = new CharSequence[count];
    			CharSequence[] entryValues = new CharSequence[count];

    			c.moveToFirst();
    			for(int i=0; i < count; i++) {
    				entries[i] = c.getString(c.getColumnIndexOrThrow(TimeDbAdapter.PROJECT_DESCRIPTION));
    				entryValues[i] = c.getString(c.getColumnIndexOrThrow(TimeDbAdapter.PROJECT_ROWID));
    				c.moveToNext();
    			}

    			listPreferenceProject.setEntries(entries);
    			listPreferenceProject.setEntryValues(entryValues);
    			listPreferenceProject.setDefaultValue("-1");   
    		}
    	}

    	if (cust) {
    		ListPreference listPreferenceCustomer = (ListPreference) findPreference("CustomerPref");
    		if (listPreferenceCustomer != null) {
    			Cursor c = mDbHelper.fetchAllActiveCustomers();
    			startManagingCursor(c);

    			int count = c.getCount();
    			CharSequence[] entries = new CharSequence[count];
    			CharSequence[] entryValues = new CharSequence[count];

    			c.moveToFirst();
    			for(int i=0; i < count; i++) {
    				entries[i] = c.getString(c.getColumnIndexOrThrow(TimeDbAdapter.CUSTOMER_NAME));
    				entryValues[i] = c.getString(c.getColumnIndexOrThrow(TimeDbAdapter.CUSTOMER_ROWID));
    				c.moveToNext();
    			}

    			listPreferenceCustomer.setEntries(entries);
    			listPreferenceCustomer.setEntryValues(entryValues);
    			listPreferenceCustomer.setDefaultValue("-1");   
    		}
    	}

    	mDbHelper.close();
    }
 }


