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
        Account[] accounts = accountManager.getAccountsByType("com.google"); //$NON-NLS-1$
        AccountHolder[] accountHolders = AccountHolder.accountHoldersFromAccounts(accounts);
        if (accounts.length>0){
            this.setListAdapter(new ArrayAdapter<AccountHolder>(this, R.layout.creds_list_item, accountHolders));
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Account account = ((AccountHolder)getListView().getItemAtPosition(position)).getAcc();
        GPSService.account = account;
        GPSService.connectCount = 1;
        finish();
        /*Intent intent = new Intent(this, AppInfo.class);
		intent.putExtra("account", account);
		startActivity(intent);
         */
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