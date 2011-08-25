package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aroundroidgroup.astrid.gpsServices.ContactsHelper;
import com.aroundroidgroup.astrid.gpsServices.GPSService;
import com.aroundroidgroup.locationTags.LocationService;
import com.timsu.astrid.R;

public class ManageContactsActivity extends ListActivity{

    public static final String PEOPLE_BACK = "peopleBack"; //$NON-NLS-1$


    private static final int DIALOG_MAIL_METHOD = 0;
    private static final int DIALOG_CONTACT_METHOD = 1;
    private static final int DIALOG_ALREADY_SCANNED = 2;
    private static final int DIALOG_HURRAY = 3;
    private static final int DIALOG_HURRAY2 = 4;
    private static final int DIALOG_NOT_CONNECTED = 5;
    private static final int DIALOG_ALREADY_FOUND = 6;


    //TODO doesnt scan agian for people in list because they are not in locationService.getAllLocationByPeople


    private AroundroidDbAdapter mDbHelper;

    private Long taskId;

    public final static String taskIDSTR = "taskID"; //$NON-NLS-1$
    public final static String peopleArraySTR = "peopleArrrr"; //$NON-NLS-1$

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    private static boolean alreadyScannedSometime = false;

    public static synchronized boolean getAlreadyScannedSometime(Boolean bool){
        boolean res = alreadyScannedSometime;
        if (bool!=null){
            alreadyScannedSometime = bool;
        }
        return res;
    }

    private ContactsHelper conHel;


    private String[] originalPeople;

    //TODO make list parallel
    private final LinkedHashSet<String> peopleHashSet = new LinkedHashSet<String>();

    private final LocationService myLocationService = new LocationService();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.people_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {



        switch (item.getItemId()) {
        case R.id.peoplelocation_menu_mail:
            if (!prs.isConnected()){
                //if not connected prompt connection
                showDialog(DIALOG_NOT_CONNECTED);
            }else{
                showDialog(DIALOG_MAIL_METHOD);
            }
            break;
        case R.id.peopleLocation_menu_contacts:
            if (!prs.isConnected()){
                //if not connected prompt connection
                showDialog(DIALOG_NOT_CONNECTED);
            }
            else if (!getAlreadyScannedSometime(null)){
                new ScanContactsTask().execute(new Void[0]);
            } else {
                showDialog(DIALOG_ALREADY_SCANNED);
            }
            break;
        case R.id.peoplelocation_menu_login:
            Intent intent = new Intent(ManageContactsActivity.this, PeopleLocationPreferneces.class);
            startActivity(intent);
            break;
        }


        return super.onOptionsItemSelected(item);
    }

    private Dialog createNotConnectedDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You are not connected right now to Aroundroid People Location Service. You must connected to procced. would you like to do that now?")
        .setTitle("Not Connected!")
        .setPositiveButton("Yes (recommanded)", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //TODO move to connection screen here
                Intent intent = new Intent(ManageContactsActivity.this, PeopleLocationPreferneces.class);
                startActivity(intent);
            }
        })
        .setNegativeButton("Hell, No!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        return alert;

    }

    private Dialog createNoFriendsDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Oh no!");
        builder.setMessage("Non of your contacts are using Aroundroid.\n:(");
        builder.setNeutralButton("Ok...",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }


    private Dialog createHurrayDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hurray!");
        builder.setMessage("Your friend is registered and was added to your tracking list.");
        builder.setNeutralButton("Ok",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog createHurray2Dialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hurray!");
        builder.setMessage("Your friend was added to your tracking list.");
        builder.setNeutralButton("Ok",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog createAlreadyAddedDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Already in the List!");
        builder.setMessage("Your friend is already in your list!");
        builder.setNeutralButton("Ok",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do nothing
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog createContactsDialog(){
        Cursor cur = mDbHelper.fetchAllPeopleWContactRegistered();
        //TODO deal with cur error
        int numberOfFriends = cur.getCount();

        //if no friends create ok dialog. if has friends create list dialog.
        if (numberOfFriends<=0){
            cur.close();
            GPSService.lockDeletes(false);
            return createNoFriendsDialog();
        }
        final ArrayList<String> mailList = new ArrayList<String>(numberOfFriends);
        ArrayList<String> displayMailList = new ArrayList<String>(numberOfFriends);
        final boolean[] tickArray = new boolean[(numberOfFriends)];
        if (cur.moveToFirst()){
            int i =0;
            do{
                String mail = cur.getString(cur.getColumnIndex(AroundroidDbAdapter.KEY_MAIL));
                mailList.add(mail);
                if (peopleHashSet.contains(mail)){
                    tickArray[i] = true;
                }
                else{
                    tickArray[i] = false;
                }
                i++;
                Long contactID = cur.getLong(cur.getColumnIndex(AroundroidDbAdapter.KEY_CONTACTID));
                String displayName = conHel.oneDisplayName(contactID);
                if (displayName==null){
                    displayName = "*Contact information unavailable*";
                }
                displayMailList.add(displayName + "\n(" + mail + ")");
            }while (cur.moveToNext());
        }
        cur.close();


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick contacts to track");
        builder.setMultiChoiceItems(displayMailList.toArray(new String[0]), tickArray, new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                tickArray[which] = isChecked;

            }
        }).setPositiveButton("Add All", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (int i =0 ; i<tickArray.length ; i++){
                    if (tickArray[i]){
                        peopleHashSet.add(mailList.get(i));
                    }
                    else{
                        peopleHashSet.remove(mailList.get(i));
                    }
                }
                GPSService.lockDeletes(false);
                fillData();

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        }).setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                GPSService.lockDeletes(false);

            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private boolean friendInList(String friendMail){
        return peopleHashSet.contains(friendMail);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_HURRAY:
            dialog = createHurrayDialog();
            break;
        case DIALOG_MAIL_METHOD:
            dialog = createAddDialog();
            break;
        case DIALOG_CONTACT_METHOD:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to exit?")
            .setCancelable(false)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ManageContactsActivity.this.finish();
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            dialog = alert;
            break;
        case DIALOG_NOT_CONNECTED:
            dialog = createNotConnectedDialog();
            break;
        case DIALOG_ALREADY_FOUND:
            dialog = createAlreadyAddedDialog();
            break;
        case DIALOG_HURRAY2:
            dialog = createHurray2Dialog();
            break;
        case DIALOG_ALREADY_SCANNED:
            dialog = createAlreadyScannedDialog();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }

    private Dialog createAlreadyScannedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("The contact list has been scanned already. Would you like to skip the scanning process?")
        .setTitle("Contacts Aready Scanned!")
        .setPositiveButton("Yes (recommanded)", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                createContactsDialog().show();
            }
        })
        .setNegativeButton("No. I like scanning stuff.", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new ScanContactsTask().execute(new Void[0]);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog createAddAnywayDialog(final String friendMail) {
        final Dialog loginDialog = new Dialog(this);
        loginDialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        loginDialog.setTitle("Friend not registered");

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.add_anyway_dialog, null);
        loginDialog.setContentView(dialogView);

        TextView tv = (TextView) dialogView.findViewById(R.id.mail_not_using);
        tv.setText("It seems that your friend ( "+friendMail+" ) is not using Aroundroid. Would you like to add him anyway?");

        Button addButton = (Button) dialogView.findViewById(R.id.add_anyway_button);
        Button cancelButton = (Button) dialogView
        .findViewById(R.id.cancel_anyway_button);
        final CheckBox cb = (CheckBox) dialogView
        .findViewById(R.id.mail_checkbox);

        cb.setChecked(true);

        addButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                if (!friendInList(friendMail)){
                    peopleHashSet.add(friendMail);
                    fillData();
                    if (cb.isChecked()){
                        // send mail
                        new InviteFriendTask().execute(new String[]{friendMail});
                    }
                }
                loginDialog.dismiss();
                showDialog(DIALOG_HURRAY2);
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                loginDialog.cancel();
            }
        });

        return loginDialog;
    }

    private class InviteFriendTask extends AsyncTask<String, Void, Boolean> {


        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                Toast.makeText(getApplicationContext(), "An invitation was sent to your friend!", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Did not send an invatation.", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {

            boolean res = prs.inviteFriend(params[0]);

            return res;
        }
    }

    private Dialog createAddDialog() {

        final Dialog loginDialog = new Dialog(this);
        loginDialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        loginDialog.setTitle("Enter friend's e-mail address");

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = li.inflate(R.layout.add_friend_dialog, null);
        loginDialog.setContentView(dialogView);

        Button addButton = (Button) dialogView.findViewById(R.id.add_button);
        Button cancelButton = (Button) dialogView
        .findViewById(R.id.cancel_button);
        final EditText et_email = (EditText) dialogView.findViewById(R.id.uname_id);

        addButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {

                //Toast.makeText(getBaseContext(), "Please enter email address.",
                //Toast.LENGTH_LONG).show();
                String friendMail = et_email.getText().toString();
                if (friendInList(friendMail)){
                    showDialog(DIALOG_ALREADY_FOUND);
                }else{
                    loginDialog.dismiss();
                    new ScanOneFriendTask().execute(new String[]{friendMail});
                }
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                loginDialog.cancel();
            }
        });

        return loginDialog;


    }

    private void fillData(){
        //sync data before
        if (taskId!=null){
            myLocationService.syncLocationsByPeople(taskId, peopleHashSet);
        }
        // Create a list of FriendPropsWithContactId, that will be put to our ListActivity.
        // Refreshing the list
        ArrayAdapter<FriendPropsWithContactId> adapter = new FriendAdapter(this,conHel,
                getProps(peopleHashSet));
        setListAdapter(adapter);
    }

    private Dialog createDeleteDialog(final String friendMail) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure that you want to delete " + friendMail + " from your tracking friend list?")
        .setTitle("Delete friend")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                peopleHashSet.remove(friendMail);
                fillData();
            }
        })
        .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        return alert;

    }

    /*
    private void saveNewPeople(){
        myLocationService.syncLocationsByPeople(taskID,peopleHashSet);
    }
     */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.friend_list_layout);
        conHel = new ContactsHelper(getContentResolver());
        mDbHelper = new AroundroidDbAdapter(this);
        mDbHelper.open();
        Bundle extras = getIntent().getExtras();
        String[] peopleWeGot = null;
        if (extras != null) {
            peopleWeGot = extras.getStringArray(peopleArraySTR);
            taskId = extras.getLong(taskIDSTR);
        }
        else{
            taskId = null;
        }

        if (peopleWeGot!=null){
            originalPeople = peopleWeGot;
        }
        else if (taskId!=null){
            originalPeople = myLocationService.getLocationsByPeopleAsArray(taskId);
        }
        else{
            originalPeople = new String[0];
        }

        for (String s : originalPeople)
            peopleHashSet.add(s);

        fillData();
        ListView list = getListView();
        list.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                FriendProps mailChosen = (FriendProps)parent.getAdapter().getItem(position);
                createDeleteDialog(mailChosen.getMail()).show();
                // Return true to consume the click event. In this case the
                // onListItemClick listener is not called anymore.
                return true;
            }
        });



    }

    private final Handler mHan = new Handler();
    final int mDelayMillis = 10 * 1000;
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            fillData();
            mHan.postDelayed(this, mDelayMillis);
        }
    };
    private void setUITimer(){
        mHan.removeCallbacks(mUpdateTimeTask);
        mHan.postDelayed(mUpdateTimeTask, mDelayMillis);

    }

    private List<FriendPropsWithContactId> getProps(LinkedHashSet<String> peopleHashSet2) {

        //TODO deal with error
        List<FriendPropsWithContactId> list = new ArrayList<FriendPropsWithContactId>();
        for (String mail : peopleHashSet2){
            list.add(get(mail));
        }
        return list;
    }

    private FriendPropsWithContactId get(String s) {
        Cursor cur = mDbHelper.fetchByMail(s);
        if (!cur.moveToFirst()){
            cur.close();
            long rowId = mDbHelper.createPeople(s);
            //TODO assming create succesful
            cur = mDbHelper.fetchPeople(rowId);
        }
        //friendPROPSWITHCONTACTID CRITICAL PART

        FriendProps fp = AroundroidDbAdapter.userToFP(cur);
        FriendPropsWithContactId fpwci = new FriendPropsWithContactId(cur.getLong(cur.getColumnIndex(AroundroidDbAdapter.KEY_CONTACTID)), fp);
        cur.close();
        return fpwci;


    }

    /*
    private final Handler mListFillHandler = new Handler();
    private synchronized void fillListSmart(){
        new Thread() {
            @Override
            public void run() {
                mListFillHandler.post(new Runnable() {
                    public void run() {
                        fillData();
                        GPSService.lockDeletes(false);
                    }
                });
            }
        }.start();
    }
    */

    private class ScanContactsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (isFinishing()){
                GPSService.lockDeletes(false);
                return;
            }
            _progressDialog.dismiss();
            if (result){
                createContactsDialog().show();
            }
            else{
                //TODO dont know what to do here
            }


        }

        private ProgressDialog _progressDialog;

        @Override
        protected void onPreExecute(){
            _progressDialog = ProgressDialog.show(
                    ManageContactsActivity.this,
                    "Looking for contacts",
                    "Scaning you Contact List to find friends that use AroundRoid. This may take a while...",
                    true,
                    true,
                    new DialogInterface.OnCancelListener(){
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            ScanContactsTask.this.cancel(true);
                        }
                    }
            );
        }

        //assuming mail in lower case
        @Override
        protected Boolean doInBackground(Void... params) {
            Set<Entry<String, Long>> friendSet = conHel.friendsWithMail();
            if (isCancelled()){
                return false;
            }

            GPSService.lockDeletes(true);
            ArrayList<String> al = new ArrayList<String>();
            for(Entry<String, Long> en : friendSet){
                Cursor cur = mDbHelper.fetchByMail(en.getKey());
                if (cur==null){
                    break;
                }
                if (!cur.moveToFirst()){
                    //record not found
                    mDbHelper.createPeople(en.getKey(),en.getValue());
                }
                else {
                    Long contactId = cur.getLong(cur.getColumnIndex(AroundroidDbAdapter.KEY_CONTACTID));
                    if (contactId == -2){
                        //record found with no contact id.
                        //UPDATE A NEW CONTACT ID
                        Long oldRecordId = cur.getLong(cur.getColumnIndex((AroundroidDbAdapter.KEY_ROWID)));
                        mDbHelper.updatePeople(oldRecordId, en.getValue());
                    }
                }
                al.add(en.getKey());
                cur.close();
            }
            if (isCancelled()){
                GPSService.lockDeletes(false);
                return false;
            }
            //TODO limit update people location to few people
            if (al.size()<=0){
                GPSService.lockDeletes(false);
                return true;
            }
            List <FriendProps> lfp = prs.updatePeopleLocations(al.toArray(new String[0]), null, mDbHelper);
            if (lfp == null){
                GPSService.lockDeletes(false);
                return false;
            }
            else{
                getAlreadyScannedSometime(true);
                return true;
            }
        }


    }


    private class ScanOneFriendTask extends AsyncTask<String, Void, Boolean> {

        private boolean error = false;

        private String friend;

        private ProgressDialog _progressDialog;

        @Override
        protected void onPreExecute(){
            _progressDialog = ProgressDialog.show(
                    ManageContactsActivity.this,
                    "Checking friend",
                    "Finding out if your friend is using Aroundroid. Please wait...",
                    true,
                    true,
                    new DialogInterface.OnCancelListener(){
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            ScanOneFriendTask.this.cancel(true);
                        }
                    }
            );
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //TODO check this:
            if (isFinishing()){
                return;
            }
            _progressDialog.dismiss();
            if (!error){

                if (result){
                    fillData();
                    //friend is using Aroundroid
                    showDialog(DIALOG_HURRAY);
                }
                else{
                    //friend is not using Aroundroid
                    createAddAnywayDialog(friend).show();
                }
            }else{

            }
            //TODO check for error
        }

        //assuming mail in lower case
        @Override
        protected Boolean doInBackground(String... params) {
            boolean returnVal = false;
            friend = params[0];
            Cursor cur  = mDbHelper.fetchByMail(friend);
            if (cur ==null){
                return false;
            }
            if (!cur.moveToFirst()){
                cur.close();
                //fetch
                List<FriendProps> lfp =  prs.updatePeopleLocations(params,null,mDbHelper);
                if (lfp==null){
                    error = true;
                    return false;
                }
                cur = mDbHelper.fetchByMail(params[0]);
                if (cur == null || !cur.moveToFirst()){
                    if (cur!=null){
                        cur.close();
                    }
                    error = true;
                    return false;
                }
            }

            FriendProps fp = AroundroidDbAdapter.userToFP(cur);
            if (fp!=null){
                if (fp.isRegistered()){
                    returnVal = true;
                    //TODO check this:
                    if (!isFinishing()){
                        if (!friendInList(fp.getMail())){
                            //TODO add this check to the onResult function too.
                            peopleHashSet.add(fp.getMail());;
                        }
                    }
                }
            }else{
                error = true;
            }


            cur.close();
            return returnVal;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(PEOPLE_BACK, peopleHashSet);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mHan.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        setUITimer();
    }






}
