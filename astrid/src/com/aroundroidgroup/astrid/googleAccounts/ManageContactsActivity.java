package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.aroundroidgroup.locationTags.LocationService;
import com.timsu.astrid.R;

public class ManageContactsActivity extends ListActivity{

    public static final int INSERT_ID = Menu.FIRST;

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
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case INSERT_ID:

            return true;
        }
        return super.onOptionsItemSelected(item);
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
