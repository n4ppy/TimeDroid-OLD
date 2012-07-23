/*
 * Copyright (C) 2010 NappySoft
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cc.nappy.TimeDroid;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// BUG 08-30 12:24:56.881: ERROR/Cursor(332): Finalizing a Cursor that has not been deactivated or closed. database = /data/data/cc.nappy.TimeDroid/databases/Timedata, table = times, query = SELECT _id FROM times WHERE active = 1
// BUG 08-30 12:24:56.881: ERROR/Cursor(332):     at cc.nappy.TimeDroid.TimeDbAdapter.activateTime(TimeDbAdapter.java:379)
// BUG 08-30 12:24:56.881: ERROR/Cursor(332):     at cc.nappy.TimeDroid.TimeEdit$7.onClick(TimeEdit.java:222)


/**
 * Defines the basic CRUD operations for TimeDroid
 * http://www.sqlite.org/datatype3.html
 * http://www.sqlite.org/lang_datefunc.html
 * @author FSchiphorst
 */

public class TimeDbAdapter {

    private static final String TAG = "TimeDroidDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /* Database details */
    private static final String DATABASE_NAME = "Timedata";
    private static final int DATABASE_VERSION = 2;
    
    /* Database sql statements */
    /*  time table 
     *  Store times worked
     *  When changing check the fetchAllReportTimes function (hardcoded) 
     *  */
    public static final String TIME_ROWID = "_id"; // id of the record
    public static final String TIME_START = "start"; // start of the task
    public static final String TIME_ACTIVE = "active"; // active = counter running 
    public static final String TIME_CURRENT = "current"; // start of current run (can be restarted)
    public static final String TIME_TOTAL = "total"; // total time on the job in seconds
    
    public static final String TIME_CUSTOMER = "customer"; // id of the customer
    public static final String TIME_PROJECT = "project"; // id of the project
    public static final String TIME_DESCRIPTION = "description"; // description of the task
    
    public static final String TIME_ARCHIVED = "archived"; // archived = not visible in list

    public static final String TIME_TABLE = "times";
    private static final String TIME_CREATE = new StringBuilder()
        .append("create table ").append(TIME_TABLE).append(" (_id integer primary key autoincrement, ")
                                                   .append("start integer not null, ")
                                                   .append("active integer not null, ")
                                                   .append("current integer not null, ")
                                                   .append("total integer not null, ")

                                                   .append("customer integer not null, ")
                                                   .append("project integer not null, ")
                                                   .append("description text not null, ")
                                     
                                                   .append("archived integer not null);").toString();
    private static final String TIME_DROP = new StringBuffer().append("DROP TABLE IF EXISTS  ")
                                                              .append(TIME_TABLE)
                                                              .append(";").toString();

    /* customers table 
     * Store customer names and if they are active (=visible for selection)
     * */
    public static final String CUSTOMER_NAME = "name";
    public static final String CUSTOMER_ACTIVE = "active";
    public static final String CUSTOMER_ROWID = "_id";

    private static final String CUSTOMERS_TABLE = "customers";
    private static final String CUSTOMERS_CREATE = new StringBuilder()
                                .append("create table ").append(CUSTOMERS_TABLE).append(" (_id integer primary key autoincrement, ")
                                .append("name text not null, active int not null);").toString();
    private static final String CUSTOMERS_DROP = new StringBuilder()
                                .append("DROP TABLE IF EXISTS  ").append(CUSTOMERS_TABLE).append(";").toString();

    /* project table 
     * Store project information and if they are active (=visible for selection)
     * */
    public static final String PROJECT_DESCRIPTION = "description";
    public static final String PROJECT_ACTIVE = "active";
    public static final String PROJECT_ROWID = "_id";

    private static final String PROJECTS_TABLE = "projects";
    private static final String PROJECTS_CREATE = new StringBuilder()
                                .append("create table ").append(PROJECTS_TABLE).append(" (_id integer primary key autoincrement, ")
                                .append("description text not null, active int not null);").toString();
    private static final String PROJECTS_DROP = new StringBuilder()
                                .append("DROP TABLE IF EXISTS  ").append(PROJECTS_TABLE).append(";").toString();


    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TIME_CREATE);
            db.execSQL(CUSTOMERS_CREATE);
            db.execSQL(PROJECTS_CREATE);
            // add initial values
            db.execSQL("insert into projects (description, active) values ('Project',1);");
            db.execSQL("insert into customers (name, active) values ('Customer',1);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, new StringBuilder() .append("Upgrading database from version ").append(oldVersion).append(" to ")
                                           .append(newVersion).append(", which will destroy all old data").toString());
            /* change if needed this as we want to keep the times etc;) */
//            if (newVersion == 2 && oldVersion == 1) {
//            	// added seconds to total 
//            	db.execSQL("update times set total = total * 100;");
//            }	
            if (newVersion < 0) { // dummy for now keep these tables
                db.execSQL(TIME_DROP);
                db.execSQL(TIME_CREATE);
            	db.execSQL(CUSTOMERS_DROP);
            	db.execSQL(PROJECTS_DROP);
            	onCreate(db);
            }
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public TimeDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public TimeDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }

    /******************************************************/
    /*********************** TIMES *********************/
    /******************************************************/
    
    /**
     * Create a new time using the data provided.
     * If the successfully created return the new rowId , 
     * otherwise return a -1 to indicate failure.
     * 
     * @param customerId ID of client
     * @param projectId ID of project
     * @param description of task
     * @param start start of first time
     * @param active if time is active
     * @param total total time on task in seconds 
     * @return rowId or -1 if failed
     */
    public long createTime(long customerId, long projectId, String description, Calendar  start, long total) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(TIME_CUSTOMER, customerId);
        initialValues.put(TIME_PROJECT, projectId);
        initialValues.put(TIME_DESCRIPTION, description);
        initialValues.put(TIME_START, Tricks.formatDateAsLong(start));
        initialValues.put(TIME_CURRENT, Tricks.formatDateAsLong(start));
        initialValues.put(TIME_ACTIVE, 0);
        initialValues.put(TIME_ARCHIVED, 0);
        initialValues.put(TIME_TOTAL, total);
        
        return mDb.insert(TIME_TABLE, null, initialValues);
    }

    /**
     * Delete the time with the given rowId
     * 
     * @param rowId id of time to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTime(long rowId) {

        return mDb.delete(TIME_TABLE, TIME_ROWID + " = ?", new String[] {Long.toString(rowId)}) > 0;
    }

    /**
     * Return a Cursor over the list of all times in the database
     * 
     * @return Cursor over all times
     */
    public Cursor fetchAllTimes() {

        return mDb.query(TIME_TABLE, new String[] {TIME_ROWID, TIME_CUSTOMER,
        		TIME_PROJECT, TIME_DESCRIPTION, TIME_START, TIME_ACTIVE, TIME_TOTAL, TIME_CURRENT, TIME_ARCHIVED}, null, null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all times in the database
     * 
     * @return Cursor over all times
     */
    public Cursor fetchAllReportTimes(Calendar reportFrom, Calendar reportToIncl) {
    	long fromLong = Tricks.formatDateAsLong(reportFrom);
    	long toLong = Tricks.formatDateAsLong(reportToIncl) ;
    	fromLong = (long)(fromLong / 1000000) * 1000000; // wipe time
    	toLong = (long)(toLong / 1000000) * 1000000 + 999999; // max out time on that day

    	// use actual names to keep it readable
    	StringBuilder MY_QUERY = new StringBuilder()
                   .append("SELECT ")
    	           .append(" times._id, times.description as tdesc, start, times.active, total, current, archived")
    	           .append(", customers._id as crow, customers.name")
    	           .append(", projects._id as prow, projects.description as pdesc")
    	           .append(" FROM times") 
                   .append(" INNER JOIN customers ON times.customer = customers._id")
                   .append(" INNER JOIN projects ON times.project = projects._id")
                   .append(" WHERE start >= ? AND start <= ?")
                   .append(" ORDER BY start");
    	return mDb.rawQuery(MY_QUERY.toString(), new String[]{Long.toString(fromLong), Long.toString(toLong)});
    }

    /**
     * Return a Cursor over the list of all times that are active in the database
     * 
     * @return Cursor over all active times
     */
    public Cursor fetchAllActiveTimes() {

        return mDb.query(TIME_TABLE, new String[] {TIME_ROWID, TIME_CUSTOMER,
        		TIME_PROJECT, TIME_DESCRIPTION, TIME_START, TIME_ACTIVE, TIME_TOTAL, TIME_CURRENT}, TIME_ARCHIVED + " = 0", null, null, null, null);
    }

    /**
     * Return a Cursor over the list of all times that are active in the database
     * 
     * @return Cursor over all active times
     */
    public Cursor fetchTemplateTimes(int defLookBack, int defLookBackLimit) {
        Calendar cStartTime = Calendar.getInstance();
        cStartTime.add(Calendar.DAY_OF_MONTH, -defLookBack);

    	long fromLong = Tricks.formatDateAsLong(cStartTime);
    	fromLong = (long)(fromLong / 1000000) * 1000000; // wipe time

    	// Add 1 for the "Select template" 
    	defLookBackLimit++;
    	
    	// use actual names to keep it readable
    	StringBuilder MY_QUERY = new StringBuilder()
    	// not needed anymore as we now have a button
//  	.append(" SELECT -1 as _id , 'Select template' as desc")
//  	.append(" UNION")
        	.append(" SELECT ")
        	.append(" times._id as _id, projects.description || ' ' || times.description as desc")
        	.append(" FROM times")
        	.append(" INNER JOIN projects ON times.project = projects._id")
        	.append(" WHERE start >= ?")
          	.append(" LIMIT ?"); 
    	return mDb.rawQuery(MY_QUERY.toString(), new String[]{Long.toString(fromLong),Integer.toString(defLookBackLimit)});
    }

    /**
     * Return a Cursor positioned at the time that matches the given rowId
     * 
     * @param rowId id of time to retrieve
     * @return Cursor positioned to matching time, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchTime(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, TIME_TABLE, new String[] {TIME_ROWID, TIME_CUSTOMER,
                		TIME_PROJECT, TIME_DESCRIPTION, TIME_START, TIME_ACTIVE, TIME_TOTAL, TIME_CURRENT}, TIME_ROWID + " = ?", new String[]{Long.toString(rowId)},
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the time using the details provided. The time to be updated is
     * specified using the rowId, and it is altered to use date passed in
     * 
     * @param rowID
     * @param customerId ID of client
     * @param projectId ID of project
     * @param description of task 
     * @param start start of first time
     * @param total total time on task in seconds
     * @return true if the time was successfully updated, false otherwise
     */
    public boolean updateTime(long rowId, long customerId, long projectId, String description, Calendar start, long total) {
        ContentValues args = new ContentValues();
        args.put(TIME_CUSTOMER, customerId);
        args.put(TIME_PROJECT, projectId);
        args.put(TIME_DESCRIPTION, description);
        args.put(TIME_START, Tricks.formatDateAsLong(start));
        args.put(TIME_TOTAL, total);
        
        return mDb.update(TIME_TABLE, args, TIME_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }
    
    /**
     * Close this time (by deactivating = recalculate if needed and then archiving
     * 
     * @param rowId
     * @return true if the time was successfully updated, false otherwise 
     */
    public boolean closeTime(long rowId) {
    	//   set total and disable active
    	deactivateTime(rowId, false); // deactivate / recalculate total 
        ContentValues args = new ContentValues();
        args.put(TIME_ARCHIVED, 1);
        
        return mDb.update(TIME_TABLE, args, TIME_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }
    
    /**
     * Deactivate the time = calculate total and set active to 0
     * 
     * @param rowId
     * @param keepactivate to keep time active = update total time before edit
     *        if time is not active it will stay that way (= non active)
     * @return true if the time was successfully updated, false otherwise
     */
    public boolean deactivateTime(long rowId, boolean keepactivate) {
        ContentValues args = new ContentValues();
        int active = 0;
    	// Get this record
    	Cursor curVal = fetchTime(rowId);  
    	if (curVal != null) {
        	// Check if it is active
    		if (curVal.getInt(curVal.getColumnIndex(TimeDbAdapter.TIME_ACTIVE)) == 1) {
    	    	if (keepactivate)
    	    		active = 1;
    			// get total
    			long tot = curVal.getLong(curVal.getColumnIndex(TimeDbAdapter.TIME_TOTAL));
    	    	// calculate difference between now and current start time
                Calendar cCurrentTime = Calendar.getInstance();
                cCurrentTime.setTime(Tricks.getCalendarFromFormattedLong(curVal.getLong(curVal.getColumnIndex(TimeDbAdapter.TIME_CURRENT))).getTime());
                long diff = Calendar.getInstance().getTimeInMillis() - cCurrentTime.getTimeInMillis();
                // as seconds
                diff /= 1000;

    	    	// set the value to update the record and update current (or we get double time ;)
                args.put(TIME_TOTAL, tot + diff);
                args.put(TIME_CURRENT, Tricks.formatDateAsLong(Calendar.getInstance()));
    		}
    	}
    	args.put(TIME_ACTIVE, active);
        
        return mDb.update(TIME_TABLE, args, TIME_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }
    
    /**
     * Deactivate all other active times (should be 1) and set active and current time to now
     * 
     * @param rowId
     * @return true if the time was successfully updated, false otherwise
     */
    public boolean activateTime(long rowId) {
    	// deactivate all times
        Cursor mCursor =
        	mDb.query(TIME_TABLE, new String[] {TIME_ROWID}, TIME_ACTIVE + " = 1", null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
            while (mCursor.isAfterLast() == false) {
            	deactivateTime(mCursor.getLong(mCursor.getColumnIndex(TimeDbAdapter.TIME_ROWID)), false);
            	mCursor.moveToNext();
            }
        }
    	
    	// set running = now and active = 1
        ContentValues args = new ContentValues();
        args.put(TIME_ACTIVE, 1);
        args.put(TIME_CURRENT, Tricks.formatDateAsLong(Calendar.getInstance()));
        
        return mDb.update(TIME_TABLE, args, TIME_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }

    public void closeAllTimes() {
    	// find all if there is an active time
        Cursor mCursor =
        	mDb.query(TIME_TABLE, new String[] {TIME_ROWID}, TIME_ARCHIVED + " = 0", null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
            while (mCursor.isAfterLast() == false) {
            	// close all the times (will auto deactivate)
            	closeTime(mCursor.getLong(mCursor.getColumnIndex(TimeDbAdapter.TIME_ROWID)));
            	mCursor.moveToNext();
            }
        }
    }

    /******************************************************/
    /*********************** CUSTOMERS *********************/
    /******************************************************/
    
    /**
     * Create a new customer using the name provided.
     * If the successfully created return the new rowId , 
     * otherwise return a -1 to indicate failure.
     * 
     * @param name
     * @param active
     * @return rowId or -1 if failed
     */
    public long createCustomer(String name, Boolean active) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(CUSTOMER_NAME, name);
        initialValues.put(CUSTOMER_ACTIVE , active ? 1 : 0);

        return mDb.insert(CUSTOMERS_TABLE, null, initialValues);
    }

    /**
     * Delete the customer with the given rowId
     * 
     * @param rowId id of customer to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteCustomer(long rowId) {
        // check if the code is in use
    	if (mDb.query(TIME_TABLE, new String[] {TIME_ROWID}, 
    			TIME_CUSTOMER + "=" + rowId, null, null, null, null).getCount() > 0) {
    		return false; // code stil in use
    	}
        return mDb.delete(CUSTOMERS_TABLE, CUSTOMER_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }

    /**
     * Return a Cursor over the list of all customers in the database
     * 
     * @return Cursor over all customers
     */
    public Cursor fetchAllCustomers() {
        return mDb.query(CUSTOMERS_TABLE, new String[] {CUSTOMER_ROWID, CUSTOMER_NAME,
                CUSTOMER_ACTIVE}, null, null, null, null, null);
    }

    public Cursor fetchAllActiveCustomers() {
        return mDb.query(CUSTOMERS_TABLE, new String[] {CUSTOMER_ROWID, CUSTOMER_NAME
                }, CUSTOMER_ACTIVE + " = 1", null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the customer that matches the given rowId
     * 
     * @param rowId id of customer to retrieve
     * @return Cursor positioned to matching customer, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchCustomer(long rowId) throws SQLException {
        Cursor mCursor =

                mDb.query(true, CUSTOMERS_TABLE, new String[] {CUSTOMER_ROWID,
                		CUSTOMER_NAME, CUSTOMER_ACTIVE}, CUSTOMER_ROWID + " = ?", new String[]{Long.toString(rowId)},
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Update the customer the details provided. 
     * 
     * @param rowId id of customer to update
     * @param name 
     * @param active
     * @return true if the customer was successfully updated, false otherwise
     */
    public boolean updateCustomer(long rowId, String name, Boolean active) {
        ContentValues args = new ContentValues();
        args.put(CUSTOMER_NAME, name);
        args.put(CUSTOMER_ACTIVE, active ? 1 : 0);

        return mDb.update(CUSTOMERS_TABLE, args, CUSTOMER_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }
    /******************************************************/
    /*********************** PROJECTS *********************/
    /******************************************************/
    
    /**
     * Create a new project using the name provided.
     * If the successfully created return the new rowId , 
     * otherwise return a -1 to indicate failure.
     * 
     * @param description the description of the setting
     * @param value the value of the setting
     * @return rowId or -1 if failed
     */
    public long createProject(String description, Boolean active) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(PROJECT_DESCRIPTION, description);
        initialValues.put(PROJECT_ACTIVE, active ? 1 : 0);

        return mDb.insert(PROJECTS_TABLE, null, initialValues);
    }

    /**
     * Delete the project with the given rowId
     * 
     * @param rowId id of project to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteProject(long rowId) {
        // check if the code is in use
    	if (mDb.query(TIME_TABLE, new String[] {TIME_ROWID}, 
    			TIME_PROJECT + "=" + rowId, null, null, null, null).getCount() > 0) {
    		return false; // code stil in use
    	}
        return mDb.delete(PROJECTS_TABLE, PROJECT_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }

    /**
     * Return a Cursor over the list of all projects in the database
     * 
     * @return Cursor over all project
     */
    public Cursor fetchAllProjects() {

        return mDb.query(PROJECTS_TABLE, new String[] {PROJECT_ROWID, PROJECT_DESCRIPTION,
        		PROJECT_ACTIVE}, null, null, null, null, null);
    }

    public Cursor fetchAllActiveProjects() {

        return mDb.query(PROJECTS_TABLE, new String[] {PROJECT_ROWID, PROJECT_DESCRIPTION
        		}, PROJECT_ACTIVE + " = 1", null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the project that matches the given rowId
     * 
     * @param rowId id of project to retrieve
     * @return Cursor positioned to matching project, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchProject(long rowId) throws SQLException {

        Cursor mCursor =

                mDb.query(true, PROJECTS_TABLE, new String[] {PROJECT_ROWID,
                		PROJECT_DESCRIPTION, PROJECT_ACTIVE}, PROJECT_ROWID + " = ?", new String[]{Long.toString(rowId)},
                        null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the project using the details provided.
     * 
     * @param rowId id of project to update
     * @param description 
     * @param active
     * @return true if the project was successfully updated, false otherwise
     */
    public boolean updateProject(long rowId, String description, Boolean active) {
        ContentValues args = new ContentValues();
        args.put(PROJECT_DESCRIPTION, description);
        args.put(PROJECT_ACTIVE, active ? 1 : 0);

        return mDb.update(PROJECTS_TABLE, args, PROJECT_ROWID + " = ?", new String[]{Long.toString(rowId)}) > 0;
    }
}
