package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.aroundroidgroup.locationTags.LocationService;
import com.timsu.astrid.R;

public class ManageContactsActivity extends ListActivity{

    public static final int INSERT_ID = Menu.FIRST;
    public static final int INSERT2_ID = Menu.FIRST + 1;

    private static final int DIALOG_MAIL_METHOD = 0;
    private static final int DIALOG_CONTACT_METHOD = 1;

    private AroundroidDbAdapter mDbHelper;

    private Long taskID;

    public final static String taskIDSTR = "taskID"; //$NON-NLS-1$

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case INSERT_ID:
            showDialog(DIALOG_MAIL_METHOD);
            break;
        case INSERT2_ID:
            showDialog(DIALOG_CONTACT_METHOD);
            break;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
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
        default:
            dialog = null;
        }
        return dialog;
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

        addButton.setOnClickListener(new OnClickListener() {
        // @Override
        public void onClick(View v) {

        Toast.makeText(getBaseContext(), "Please enter email adress.",
        Toast.LENGTH_LONG).show();
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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

        // Create an array of Strings, that will be put to our ListActivity
        ArrayAdapter<FriendProps> adapter = new FriendAdapter(this,
                getProps(peopleList));
        setListAdapter(adapter);
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


}
