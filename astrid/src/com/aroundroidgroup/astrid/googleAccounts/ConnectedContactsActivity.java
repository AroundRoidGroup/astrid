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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.aroundroidgroup.astrid.gpsServices.ContactsHelper;
import com.aroundroidgroup.astrid.gpsServices.ContactsHelper.idNameMail;
import com.timsu.astrid.R;

public class ConnectedContactsActivity extends ListActivity {


    //TODO check STALE DATA EXCEPTION WHEN ROTATING DEVICE!

    //TODO find out why when back is pressed all collapse

    //TODO fix UI problems

    //TODO on on resume fill data

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }

    public static final int ADD_PEOPLE_RESULT_CODE = 1;
    public static final String FRIEND_MAIL = "mail";

    public static final int SCAN_ID = Menu.FIRST;
    private CharSequence cs;
    private AroundroidDbAdapter mDbHelper;
    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();
    private ContactsHelper conHel;

    private AlertDialog ConnectDialog;
    private AlertDialog connectOK;
    private AlertDialog connectFAIL;


    private void initDialogs(){
        connectOK = new AlertDialog.Builder(ConnectedContactsActivity.this).create();
        connectFAIL = new AlertDialog.Builder(ConnectedContactsActivity.this).create();
        /* popping up a connectOK so the user could confirm his location choice */
        /* setting the dialog title */
        connectOK.setTitle("Hooray!"); //$NON-NLS-1$

        /* setting the dialog message */
        connectOK.setMessage("Your friend is registered!" ); //$NON-NLS-1$

        /* setting the confirm button text and action to be executed if it has been chosen */
        connectOK.setButton(DialogInterface.BUTTON_POSITIVE, "OK", //$NON-NLS-1$
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dg, int which) {
                Intent intent = new Intent();
                intent.putExtra(FRIEND_MAIL, cs);
                setResult(RESULT_OK, intent);
                finish();
            }

        });


        /* popping up a connectFAIL so the user could confirm his location choice */
        /* setting the dialog title */
        connectFAIL.setTitle("Friend not registered"); //$NON-NLS-1$

        /* setting the dialog message */
        connectFAIL.setMessage("It seems that a your friend is not registered to the service. Would you like to Invite him? (recommanded)" ); //$NON-NLS-1$

        /* setting the confirm button text and action to be executed if it has been chosen */
        connectFAIL.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dg, int which) {
                Toast.makeText(getApplicationContext(), "An invatation is being sent...", Toast.LENGTH_LONG).show();
                new InviteFriendTask().execute(new String[]{cs.toString()});
                connectFAIL.cancel();
            }

        });
        connectFAIL.setButton(DialogInterface.BUTTON_NEGATIVE, "No", //$NON-NLS-1$
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dg, int which) {
                connectFAIL.cancel();
            }
        });


    }

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

        Button submitBtn = (Button) findViewById(R.id.submitContactButton);

        initDialogs();

        ConnectDialog=  new AlertDialog.Builder(ConnectedContactsActivity.this).create();

        /* popping up a ConnectDialog so the user could confirm his location choice */
        /* setting the dialog title */
        ConnectDialog.setTitle("Checking friend"); //$NON-NLS-1$

        /* setting the dialog message */
        ConnectDialog.setMessage("Please wait while Aroundroid finds out if your friend also uses Aroundroid!" ); //$NON-NLS-1$

        /* setting the confirm button text and action to be executed if it has been chosen */
        ConnectDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Cancel", //$NON-NLS-1$
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dg, int which) {
                /* exit */
                ConnectDialog.cancel();
            }

        });


        submitBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cs = ((EditText)findViewById(R.id.editTextFriendMail)).getText();
                if (!cs.equals("")){
                    if (prs.isConnected()){
                        ConnectDialog.show();
                        new ScanOneFriendTask().execute(new String[]{cs.toString().toLowerCase()});
                        /*
                    Intent intent = new Intent();
                    intent.putExtra(FRIEND_MAIL, cs);
                    setResult(RESULT_OK, intent);
                    finish();
                         */
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "NOT CONNECTED TO PEOPLE LOCATION SERVICE!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        ListView lv = getListView();

        lv.setTextFilterEnabled(true);

        setResult(RESULT_CANCELED);

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                //TODO ALON : open YES NO MESSAGE WOLUD YOU LIKE TO CHOOSE .GETTEXT() ?
                // When clicked, show a toast with the TextView text
                idNameMail idnm = (idNameMail)parent.getAdapter().getItem(position);
                Toast.makeText(getApplicationContext(), "Added: " + idnm.name, //$NON-NLS-1$
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra(FRIEND_MAIL, idnm.mail);
                setResult(RESULT_OK, intent);
                finish();
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

        if (idnmList.size()==0){
            Toast.makeText(getApplicationContext(), "No Friends were found!", Toast.LENGTH_SHORT).show();
        }



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
            if (lfp!=null && lfp.size()>0 ){
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
            }

            return lfp!=null;

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

    private class ScanOneFriendTask extends AsyncTask<String, Void, Boolean> {




        @Override
        protected void onPostExecute(Boolean result) {
            ConnectDialog.cancel();
            if (result){
                //active press ok
                connectOK.show();
            }
            else{
                //
                connectFAIL.show();
            }
        }

        //assuming mail in lower case
        @Override
        protected Boolean doInBackground(String... params) {
            boolean returnVal = false;
            Cursor cur  = mDbHelper.fetchByMail(params[0]);
            if (cur!=null && cur.moveToFirst()){
                returnVal = true;
            }else{
                List<FriendProps> lfp = prs.getPeopleLocations(params, null);
                if (lfp==null){
                    returnVal = false;
                }
                else
                    if (lfp.size()==0){
                        returnVal = false;
                    }
                    else{
                        FriendProps fp = lfp.get(0);

                        if (fp.getMail().compareTo(params[0])==0){
                            returnVal = true;
                        }
                    }
            }

            if (cur!=null){
                cur.close();
            }

            return returnVal;
        }
    }


    private class InviteFriendTask extends AsyncTask<String, Void, Boolean> {


        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                Toast.makeText(getApplicationContext(), "An invitation was sent to your friend!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Did not send an invatation.", Toast.LENGTH_LONG).show();
                connectFAIL.show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {

            boolean res = prs.inviteFriend(params[0]);

            return res;
        }
    }

}
