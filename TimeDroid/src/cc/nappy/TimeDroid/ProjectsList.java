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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ProjectsList extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;

    private TimeDbAdapter mDbHelper;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_list);

        mDbHelper = new TimeDbAdapter(this);
        mDbHelper.open();
        
        fillData();
        registerForContextMenu(getListView());
        
    }
    
    private void fillData() {
        // Get all of the rows from the database and create the item list
        Cursor TimecodeCursor = mDbHelper.fetchAllProjects();
        startManagingCursor(TimecodeCursor);
        
        MyAdapter timecodes = new MyAdapter(ProjectsList.this, TimecodeCursor);
        setListAdapter(timecodes);
    }
    
    private class MyAdapter extends ResourceCursorAdapter { 
   	 
        public MyAdapter(Context context, Cursor cur) { 
            super(context, R.layout.project_row, cur); 
        } 
 
        @Override 
        public View newView(Context context, Cursor cur, ViewGroup parent) { 
            LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
            return li.inflate(R.layout.project_row, parent, false); 
        } 
 
        @Override 
        public void bindView(View view, Context context, Cursor cur) { 
            TextView tvListText = (TextView)view.findViewById(R.id.textProject); 
            CheckBox cbListCheck = (CheckBox)view.findViewById(R.id.checkProject); 
 
            tvListText.setText(cur.getString(cur.getColumnIndex(TimeDbAdapter.PROJECT_DESCRIPTION))); 
            cbListCheck.setChecked((cur.getInt(cur.getColumnIndex(TimeDbAdapter.PROJECT_ACTIVE)) == 0? false:true)); 
        } 
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_project_insert);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case INSERT_ID:
            createTimecode();
            return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_project_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case DELETE_ID:
    		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        if (!mDbHelper.deleteProject(info.id)) {
	        	Toast.makeText(this, R.string.warning_project_in_use, Toast.LENGTH_LONG).show(); 
	        }
	        fillData();
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    private void createTimecode() {
        Intent i = new Intent(this, Project.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, Project.class);
        i.putExtra(TimeDbAdapter.PROJECT_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
            fillData();
    }
}
