package com.aroundroidgroup.astrid.googleAccounts;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.aroundroidgroup.locationTags.LocationService;

public class ManageContactsActivity extends ListActivity{

    private AroundroidDbAdapter mDbHelper;

    private Long taskID;

    private final static String taskIDSTR = "taskID"; //$NON-NLS-1$

    private String[] originalPeople;

    private final ArrayList<String> peopleList = new ArrayList<String>();

    private final LocationService myLocationService = new LocationService();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
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
                getProps(originalPeople));
        setListAdapter(adapter);
    }

    private List<FriendProps> getProps(String[] names) {
        //TODO deal with error
        List<FriendProps> list = new ArrayList<FriendProps>();
        for (String mail : names){
            list.add(get(mail));
        }
        return list;
    }

    private FriendProps get(String s) {
        return AroundroidDbAdapter.userToFP(mDbHelper.fetchByMail(s));
    }


}
