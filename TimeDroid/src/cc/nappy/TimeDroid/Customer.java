/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class Customer extends Activity {
	private EditText mNameText;
    private CheckBox mActiveCheck;
    private Long mRowId;
    private TimeDbAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new TimeDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.customer_edit);

        mNameText = (EditText) findViewById(R.id.name );
        mActiveCheck = (CheckBox) findViewById(R.id.customer_active);
      
        Button confirmButton = (Button) findViewById(R.id.confirm);
       
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(TimeDbAdapter.CUSTOMER_ROWID) : null;
        if (mRowId == null) {
        	Bundle extras = getIntent().getExtras();
        	mRowId = extras != null ? extras.getLong(TimeDbAdapter.CUSTOMER_ROWID) : null;
        }
       
        populateFields();
        
        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
          
        });
    }
    
	private void populateFields() {
    	if (mRowId != null) { 
    		Cursor customer = mDbHelper.fetchCustomer(mRowId);
    		startManagingCursor(customer);
    		mNameText.setText(customer.getString(customer.getColumnIndexOrThrow(TimeDbAdapter.CUSTOMER_NAME)));
    		mActiveCheck.setChecked(customer.getShort(customer.getColumnIndexOrThrow(TimeDbAdapter.CUSTOMER_ACTIVE)) != 0);
    	}
    	else {
    		// default to checked
    		mActiveCheck.setChecked(true);
    	}
    }
    
    @Override    
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putLong(TimeDbAdapter.CUSTOMER_ROWID, mRowId);
    }
    
    @Override    
    protected void onPause() {
    	super.onPause();
    	saveState();
    }
    
    @Override    
    protected void onResume() {
    	super.onResume();
    	populateFields();
    }
    
    private void saveState() {
    	String name = mNameText.getText().toString();
    	Boolean active = mActiveCheck.isChecked();
    	if (mRowId == null) {
    		long id = mDbHelper.createCustomer(name, active);
    		if (id > 0) {
    			mRowId = id;
    		}
    	} else {
    		mDbHelper.updateCustomer(mRowId, name, active);
    	}
    }
}
