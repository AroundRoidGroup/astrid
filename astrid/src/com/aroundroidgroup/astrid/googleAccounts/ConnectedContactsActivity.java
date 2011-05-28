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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aroundroidgroup.astrid.gpsServices.ContactsHelper;
import com.aroundroidgroup.astrid.gpsServices.ContactsHelper.idNameMail;
import com.timsu.astrid.R;

public class ConnectedContactsActivity extends ListActivity {

    //TODO on on resume fill data

    public static final int SCAN_ID = Menu.FIRST;

    private AroundroidDbAdapter mDbHelper;
    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();
    private ContactsHelper conHel;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contactsf_list);
        mDbHelper = new AroundroidDbAdapter(this);
        mDbHelper.open();
        conHel = new ContactsHelper(getContentResolver());
        fillData();
        Toast.makeText(getApplicationContext(), "Hit scan button from menu to scan for friend in the contact list!", Toast.LENGTH_SHORT).show();
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
          public void onItemClick(AdapterView<?> parent, View view,
              int position, long id) {
            // When clicked, show a toast with the TextView text
            Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                Toast.LENGTH_SHORT).show();
            idNameMail idnm = (idNameMail)parent.getAdapter().getItem(position);
            Toast.makeText(getApplicationContext(), idnm.id,
                    Toast.LENGTH_SHORT).show();
          }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        //TODO externalize strings
        menu.add(0, SCAN_ID, 0, "Scan now!"); //$NON-NLS-1$
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case SCAN_ID:
            if (prs.isConnected()){
                new ScanContactsTask().execute();
            }
            else{
                Toast.makeText(getApplicationContext(), "Not connected to the people location service!", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fillData() {

        Cursor cur = mDbHelper.fetchAllPeopleWContact();
        List<idNameMail> idnmList = new ArrayList<idNameMail>();
        while(cur.moveToNext()){
            int index = cur.getColumnIndex(AroundroidDbAdapter.KEY_CONTACTID);
            int rowID = cur.getInt(index);
            List<idNameMail> idNm = conHel.oneFriendWithGoogle(rowID);
            if (idNm!=null&&idNm.size()>0){
                idnmList.add(idNm.get(0));
            }
        }
        cur.close();

        ArrayAdapter<idNameMail> lastAdapter;

        lastAdapter = new ArrayAdapter<idNameMail>(this,R.layout.contactsf_row, idnmList);

        setListAdapter(lastAdapter);



    }

    private class ScanContactsTask extends AsyncTask<Void, Void, Boolean> {

        //assuming prs is connected
        private boolean scanContacts() {
            List<idNameMail> chINM = conHel.friendsWithGoogle();
            String[] peopleArr = new String[chINM.size()];
            int i =0;
            for (idNameMail idnm : chINM){
                peopleArr[i++] = idnm.mail;
            }
            //TODO - sychronized to be parralled with the GPSService
            //TODO - send and update only the ones that does not aleady exists
            //the list is sorted!
            List<FriendProps> lfp =  prs.getPeopleLocations(peopleArr, null);
            if (lfp!=null && lfp.size()>0){
                FriendProps exampleProps = new FriendProps();
                for (idNameMail idnm : chINM){
                    exampleProps.setMail(idnm.mail);
                    int index = Collections.binarySearch(lfp, exampleProps, FriendProps.getMailComparator());
                    if (index>=0){
                        FriendProps findMe = lfp.get(index);
                        Cursor curMail = mDbHelper.fetchByMail(idnm.mail);
                        if (curMail!=null && curMail.moveToFirst()){
                            long l = curMail.getLong(0);
                            curMail.close();
                            mDbHelper.updatePeople(l, findMe.getDlat(), findMe.getDlon(), findMe.getTimestamp(), Long.parseLong(idnm.id));
                        }
                        else{
                            //TODO change to one function
                            long rowId = mDbHelper.createPeople(idnm.mail, Long.parseLong(idnm.id));
                            mDbHelper.updatePeople(rowId, findMe.getDlat(), findMe.getDlon(), findMe.getTimestamp(), Long.parseLong(idnm.id));
                        }

                    }
                }
                return true;
            }
            else{
                return false;
            }
        }


        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                fillData();
                Toast.makeText(getApplicationContext(), "Scan was a Marvelous success!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Scan faild, cannot connect to service", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return scanContacts();
        }
    }
}
