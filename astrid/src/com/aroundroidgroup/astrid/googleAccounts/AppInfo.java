package com.aroundroidgroup.astrid.googleAccounts;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.myService;

public class AppInfo extends Activity {
    private Account chosenAccount;
    private String lastToken;

    private TextView statusText;

    DefaultHttpClient http_client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_info);
        http_client = myService.getHttpClient();
        statusText = (TextView)findViewById(R.id.loginStatusInformationText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account account = (Account)intent.getExtras().get("account"); //$NON-NLS-1$
        chosenAccount = account;
        statusText.setText("Loading Authentication Token..."); //$NON-NLS-1$
        accountManager.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null); //$NON-NLS-1$
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {


        public void run(AccountManagerFuture<Bundle> result) {


            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    startActivity(intent);
                } else {
                    onGetAuthToken(bundle);
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    protected void onGetAuthToken(Bundle bundle) {
        String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        statusText.setText("Recieving Cookie.."); //$NON-NLS-1$
        new GetCookieTask().execute(auth_token);
    }

    private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... tokens) {
            try {
                // Don't follow redirects
                http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

                HttpGet http_get = new HttpGet(AroundRoidAppConstants.loginUrl+ tokens[0]);
                HttpResponse response;
                response = http_client.execute(http_get);

                if(response.getStatusLine().getStatusCode() != 302)
                    // Response should be a redirect
                    return false;

                for(Cookie cookie : http_client.getCookieStore().getCookies()) {
                    if(cookie.getName().equals("ACSID") || cookie.getName().equals("SACSID")) //$NON-NLS-1$ //$NON-NLS-2$
                        return true;
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
            }
            lastToken = tokens[0];
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                statusText.setText("Cookie is OK! :D"); //$NON-NLS-1$
                //TODO : deal with singleton
                myService.startCheckFriendThread();
            }
            else{
                statusText.setText("Authetication Token was invalid. Getting a new one.."); //$NON-NLS-1$
                AccountManager accountManager = AccountManager.get(getApplicationContext());
                accountManager.invalidateAuthToken(chosenAccount.type, lastToken);
                accountManager.getAuthToken(chosenAccount, "ah", false, new GetAuthTokenCallback(), null); //$NON-NLS-1$
            }
        }
    }



}

