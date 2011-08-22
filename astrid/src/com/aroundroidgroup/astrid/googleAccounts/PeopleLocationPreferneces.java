package com.aroundroidgroup.astrid.googleAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;

import com.aroundroidgroup.astrid.gpsServices.GPSService;
import com.timsu.astrid.R;

public class PeopleLocationPreferneces extends PreferenceActivity {

    /*
     private final CharSequence[] calListEntries = null;

     private final CharSequence[] calListEntryValues = null;
     */
    protected AccountManager accountManager;

    private ListPreference loginList;
    private Preference Logout;
    private Preference status;
    private Resources r;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.preferences_peope_location);

        r = getResources();
        loginList = (ListPreference) findPreference("listPref");
        Logout =  findPreference("Logout");
        status = findPreference(r.getString(R.string.sync_SPr_status_key));


        accountManager = AccountManager.get(getApplicationContext());
        final Account[] accounts = accountManager.getAccountsByType("com.google"); //$NON-NLS-1$
        final String[] strArray =new String[accounts.length ];
        if (accounts.length == 0){
            loginList.setEnabled(false);
        }
        else{

            for(int i=0;i < accounts.length ; i++){
                strArray[i]=  accounts[i].name;
            }
        }

        loginList.setEntries(strArray);
        loginList.setEntryValues(strArray);

/*
        status.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {
                if(!(prs.isConnected() || prs.isConnecting()))


                    return true;

            }
        });

*/

        loginList.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                Account account=null;
                int position;
                for (position = 0; position<strArray.length; position++)
                    if ((accounts[position].name).compareTo((String) arg1)==0){
                        account = accounts[position];
                        break;
                    }


                GPSService.account = account;

                GPSService.connectCount = 1;

                return true;
            }
        });




        Logout.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference p) {

                AlertDialog.Builder builder = new AlertDialog.Builder(PeopleLocationPreferneces.this);
                builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        prs.stop();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Logout.setEnabled(true);
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;

            }
        });

    }
    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();
    private final Handler mHan = new Handler();
    final int mDelayMillis = 1 * 1000;
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (prs.isConnected()){
                setUItoConnected();
            }
            else if (prs.isConnecting()){
                setUItoConnecting();
            }
            else{
                setUItoNotConnected();
            }
            mHan.postDelayed(this, mDelayMillis);
        }

    };

    private void setUItoConnecting() {
        View view = findViewById(R.id.status);
        view.setBackgroundColor(Color.BLUE);

        status.setTitle(r.getString(R.string.logging_in_status));
        ////LOGOUT


        Logout.setEnabled(true);
        loginList.setEnabled(false);

    }

    private void setUItoNotConnected() {
        View view = findViewById(R.id.status);
        view.setBackgroundColor(Color.RED);
        Preference pref = findPreference(r.getString(R.string.sync_SPr_status_key));
        pref.setTitle(r.getString(R.string.sync_status_loggedout));
        ////LOGOUT

        Logout.setEnabled(false);
        loginList.setEnabled(true);

    }

    private void setUItoConnected() {
        View view = findViewById(R.id.status);
        view.setBackgroundColor(Color.rgb(0, 75, 0));
        Preference pref = findPreference(r.getString(R.string.sync_SPr_status_key));
        pref.setTitle(prs.getAccountString());

        //view.getd
        // loginList.getDialog();
        loginList.setEnabled(false);
        Logout.setEnabled(true);


    }

    private void setUITimer(){
        mHan.removeCallbacks(mUpdateTimeTask);
        mHan.postDelayed(mUpdateTimeTask, mDelayMillis);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mHan.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUITimer();
    }

    private static class AccountHolder{
        private Account acc;
        public AccountHolder(Account acc){
            this.setAcc(acc);
        }
        private void setAcc(Account acc) {
            this.acc = acc;
        }
        public Account getAcc() {
            return acc;
        }

        @Override
        public String toString(){
            return "Mail: " + acc.name; //$NON-NLS-1$
        }

        public static AccountHolder[] accountHoldersFromAccounts(Account[] accounts){
            AccountHolder[] acchArr= new AccountHolder[accounts.length];
            for (int i =0 ; i< accounts.length ; i++){
                acchArr[i] = new AccountHolder(accounts[i]);
            }
            return acchArr;
        }

    }
}
