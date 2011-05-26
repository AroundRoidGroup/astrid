package com.aroundroidgroup.astrid.googleAccounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.aroundroidgroup.astrid.gpsServices.GPSService;
import com.timsu.astrid.R;

public class AccountList extends ListActivity {
    protected AccountManager accountManager;
    protected Intent intent;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.creds_list);
        accountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        if (accounts.length>0){
            this.setListAdapter(new ArrayAdapter<Account>(this, R.layout.creds_list_item, accounts));
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Account account = (Account)getListView().getItemAtPosition(position);
        GPSService.account = account;
        GPSService.connectCount = 1;
        finish();
        /*Intent intent = new Intent(this, AppInfo.class);
		intent.putExtra("account", account);
		startActivity(intent);
         */
    }
}