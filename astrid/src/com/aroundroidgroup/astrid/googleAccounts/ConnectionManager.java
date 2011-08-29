package com.aroundroidgroup.astrid.googleAccounts;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

public class ConnectionManager {

    private Account chosenAccount;
    private Context cont;
    private AccountManager accountManager;

    private boolean isConnecting;

    private boolean isConnected;

    private boolean props;

    private String lastToken;

    private final DefaultHttpClient http_client =  new DefaultHttpClient();;

    public synchronized HttpResponse executeOnHttp(HttpUriRequest hur) {
        if (!isConnected()){
            return null;
        }
        //TODO remove this
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            return http_client.execute(hur);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void setProps(Account account,Context c){
        if (isConnecting() || isConnecting()){
            return;
        }
        chosenAccount = account;
        cont  = c;
        props = true;
    }

    public void connect(){
        if (!isProps() || isConnecting()){
            return;
        }
        setConnecting(true);
        accountManager = AccountManager.get(cont.getApplicationContext());
        accountManager.getAuthToken(chosenAccount, "ah", false, new GetAuthTokenCallback(), null); //$NON-NLS-1$
    }

    public String getAccountString(){
        return chosenAccount.name;
    }

    public boolean reconnect(){
        if (isProps()){
            connect();
            return true;
        }
        return false;
    }

    public void stop() {
        this.setConnected(false);
        this.setConnecting(false);
    }

    protected void onGetAuthToken(Bundle bundle) {
        this.lastToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        new GetCookieTask().execute(lastToken);
    }

    private void setConnecting(boolean isConnecting) {
        this.isConnecting = isConnecting;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    private void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isProps() {
        return props;
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {

        public void run(AccountManagerFuture<Bundle> result) {
            //TODO error when trying to open new activity1!! (when there is a problem with the authentication of an account)
            Bundle bundle;
            boolean ok  = false;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    cont.startActivity(intent);
                } else {
                    onGetAuthToken(bundle);
                }
                ok = true;
            } catch (OperationCanceledException e) {
                setConnecting(ok);
            } catch (AuthenticatorException e) {
                setConnecting(ok);
            } catch (IOException e) {
                setConnecting(ok);
            }
        }

    }


    //TODO change from async task to something real.
    private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... tokens) {
            try {
                // Don't follow redirects
                http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
                HttpGet http_get = new HttpGet(AroundRoidAppConstants.loginUrl+ tokens[0]);
                HttpResponse response;
                response = http_client.execute(http_get);
                if(response.getStatusLine().getStatusCode() != 302){
                    // Response should be a redirect
                    return false;
                }
                for(Cookie cookie : http_client.getCookieStore().getCookies()) {
                    if(cookie.getName().equals("ACSID") || cookie.getName().equals("SACSID")) //$NON-NLS-1$ //$NON-NLS-2$
                        return true;
                }
            } catch (ClientProtocolException e) {
                // TODO fill
            } catch (IOException e) {
                // TODO fill
            } finally {
                http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                //TODO fill
                setConnected(true);
                setConnecting(false);
            }
            else{
                setConnecting(false);;
                accountManager.invalidateAuthToken(chosenAccount.type, lastToken);
                //accountManager.getAuthToken(chosenAccount, "ah", false, new GetAuthTokenCallback(), null); //$NON-NLS-1$
            }
        }
    }

}