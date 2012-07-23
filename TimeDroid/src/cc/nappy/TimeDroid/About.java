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

import android.app.Activity;
import android.os.Bundle;

/**
 * Simple about dialog
 * @author FSchiphorst
 *
 */
public class About extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
   }  
}

// v0.16.0 	frasch	bugfix TemplateOn = 0 onpause 	
// V1.0.0	frasch	add LIMIT to select template (def 5)
// V1.1.0	frasch	make spinner more up to date by filling data every time
//					spinner is opende through button click
// V1.2.0	frasch	added tests to get to a TDD situation
// V1.3.0	frasch	clean up screens and change to 4.0

