package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.aroundroidgroup.locationTags.LocationService;
import com.timsu.astrid.R;

public class ManageContactsActivity extends ListActivity{

    public static final int INSERT_ID = Menu.FIRST;
    public static final int INSERT2_ID = Menu.FIRST + 1;

    private static final int DIALOG_MAIL_METHOD = 0;
    private static final int DIALOG_CONTACT_METHOD = 1;
    private static final int DIALOG_WAIT_FRIEND = 2;
    private static final int DIALOG_HURRAY = 3;
    private static final int DIALOG_ADD_ANYWAY = 4;
    private static final int DIALOG_NOT_CONNECTED = 5;
    private static final int DIALOG_ALREADY_FOUND = 6;

    private AroundroidDbAdapter mDbHelper;

    private Long taskID;

    public final static String taskIDSTR = "taskID"; //$NON-NLS-1$

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();


    private String[] originalPeople;

    private final ArrayList<String> peopleList = new ArrayList<String>();

    private final LocationService myLocationService = new LocationService();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //TODO xml and Inflate menu instead of creating it here
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_add_friend);
        menu.add(0, INSERT2_ID, 0, R.string.menu_add_friend2);
        return result;
    }

    //TODO remove this
    private int counter = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()) {
        case INSERT_ID:
            if (!prs.isConnected()){
                //if not connected prompt connection
                showDialog(DIALOG_NOT_CONNECTED);
            }else{
                showDialog(DIALOG_MAIL_METHOD);
            }
            break;
        case INSERT2_ID:
            //TODO remove this
            peopleList.add(String.valueOf(counter++));
            fillData();
            if (!prs.isConnected()){
                //if not connected prompt connection
                showDialog(DIALOG_NOT_CONNECTED);
            }
            else{
                showDialog(DIALOG_CONTACT_METHOD);

            }
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
                Intent intent = new Intent(ManageContactsActivity.this, AccountList.class);
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

    private boolean wait_canceled;
    private Dialog createWaitDialog() {
        wait_canceled = false;
        ProgressDialog pdialog = ProgressDialog.show(ManageContactsActivity.this, "Checking Friend",
                "Finding out if your friend is using Aroundroid. Please wait...", true);
        pdialog.setCanceledOnTouchOutside(true);

        pdialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                removeDialog(DIALOG_WAIT_FRIEND);

            }
        });

        pdialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                wait_canceled = true;

            }
        });

        return pdialog;

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

    private boolean friendInList(String friendMail){
        boolean alreadyFound = false;
        for (String mail : peopleList){
            if (friendMail.compareTo(mail)==0){
                alreadyFound = true;
                break;
            }
        }
        return alreadyFound;
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
        case DIALOG_WAIT_FRIEND:
            dialog = createWaitDialog();
            break;
        case DIALOG_ADD_ANYWAY:
            dialog = createAddAnywayDialog();
            break;
        case DIALOG_NOT_CONNECTED:
            dialog = createNotConnectedDialog();
            break;
        case DIALOG_ALREADY_FOUND:
            dialog = createAlreadyAddedDialog();
            break;
        default:
            dialog = null;
        }
        return dialog;
    }

    private Dialog createAddAnywayDialog() {
        // TODO Auto-generated method stub
        return null;
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
                    showDialog(DIALOG_WAIT_FRIEND);
                    loginDialog.dismiss();
                    new ScanOneFriendTask().execute(new String[]{friendMail});
                }
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {
            // @Override
            public void onClick(View v) {
                loginDialog.dismiss();
            }
        });

        return loginDialog;


    }

    private void fillData(){
        // Create a list of FriendProps, that will be put to our ListActivity.
        // Refreshing the list
        ArrayAdapter<FriendProps> adapter = new FriendAdapter(this,
                getProps(peopleList));
        setListAdapter(adapter);
    }

    private Dialog createDeleteDialog(final String friendMail, final int pos) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure that you want to delete " + friendMail + " from your tracking friend list?")
        .setTitle("Delete friend")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                peopleList.remove(pos);
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.friend_list_layout);
        mDbHelper = new AroundroidDbAdapter(this);
        mDbHelper.open();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            taskID = extras.getLong(taskIDSTR);
        }
        else{
            taskID = null;
        }

        if (taskID!=null){
            originalPeople = myLocationService.getLocationsByPeopleAsArray(taskID);
            for (String s : originalPeople)
                peopleList.add(s);
        }

        fillData();
        ListView list = getListView();
        list.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                    int position, long id) {
                FriendProps mailChosen = (FriendProps)parent.getAdapter().getItem(position);
                createDeleteDialog(mailChosen.getMail(), position).show();
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

    private List<FriendProps> getProps(List<String> names) {

        //TODO deal with error
        List<FriendProps> list = new ArrayList<FriendProps>();
        for (String mail : names){
            list.add(get(mail));
        }
        return list;
    }

    private FriendProps get(String s) {
        Cursor cur = mDbHelper.fetchByMail(s);
        if (!cur.moveToFirst()){
            cur.close();
            long rowId = mDbHelper.createPeople(s);
            //TODO assming create succesful
            cur = mDbHelper.fetchPeople(rowId);
        }
        FriendProps fp = AroundroidDbAdapter.userToFP(cur);
        cur.close();
        return fp;
    }

    private final Handler mListFillHandler = new Handler();
    private synchronized void fillListSmart(){
        new Thread() {
            @Override
            public void run() {
                mListFillHandler.post(new Runnable() {
                    public void run() {
                        fillData();
                    }
                });
            }
        }.start();
    }

    private class ScanOneFriendTask extends AsyncTask<String, Void, Boolean> {

        private boolean error = false;

        @Override
        protected void onPostExecute(Boolean result) {
            //TODO check this:
            if (isFinishing() || wait_canceled){
                return;
            }
            if (!error){
                dismissDialog(DIALOG_WAIT_FRIEND);
                if (result){
                    //friend is using Aroundroid
                    showDialog(DIALOG_HURRAY);
                }
                else{
                    //friend is not using Aroundroid
                    showDialog(DIALOG_ADD_ANYWAY);
                }
            }else{

            }
            //TODO check for error
        }

        //assuming mail in lower case
        @Override
        protected Boolean doInBackground(String... params) {
            boolean returnVal = false;
            Cursor cur  = mDbHelper.fetchByMail(params[0]);
            if (cur ==null){
                return false;
            }
            if (!cur.moveToFirst()){
                cur.close();
                //fetch
                prs.updatePeopleLocations(params,null,mDbHelper);
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
                            peopleList.add(fp.getMail());
                            fillListSmart();
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
