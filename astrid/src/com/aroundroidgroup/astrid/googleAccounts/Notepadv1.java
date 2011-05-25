/*
z * Copyright (C) 2008 Google Inc.
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

package com.aroundroidgroup.astrid.googleAccounts;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class Notepadv1 extends ListActivity {
	public static final int SCAN_ID = Menu.FIRST;

	private final int mNoteNumber = 1;
	private AroundroidDbAdapter mDbHelper;
	private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notepad_list);
        mDbHelper = new AroundroidDbAdapter(this);
        mDbHelper.open();
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, SCAN_ID, 0, R.string.menu_insert);
        return result;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        case SCAN_ID:
            if (prs.isConnected()){
            scanContacts();
            }
            else{
                Toast.makeText(getApplicationContext(), "Not connected to the people location service!", Toast.LENGTH_SHORT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void scanContacts() {

        fillData();
    }

    private void fillData() {
        // Get all of the notes from the database and create the item list
        Cursor c = mDbHelper.fetchAllPeople();
        startManagingCursor(c);

        String[] from = new String[] { AroundroidDbAdapter.KEY_ROWID };
        int[] to = new int[] { R.id.text1 };

        // Now create an array adapter and set it to display using our row
        SimpleCursorAdapter notes =
            new SimpleCursorAdapter(this, R.layout.notes_row, c, from, to);
        setListAdapter(notes);

    }
}
