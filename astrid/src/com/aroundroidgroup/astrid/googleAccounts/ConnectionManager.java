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

/***
 * Responsible of establishing connecting (getting connection cookie)
 * @author Tomer
 *
 */
public class ConnectionManager {

    private Account chosenAccount;
    private Context cont;
    private AccountManager accountManager;

    private boolean isConnecting;

    private boolean isConnected;

    private boolean props;

    private String lastToken;

    private final static long requestDelayTime = 1000;

    private final DefaultHttpClient http_client =  new DefaultHttpClient();;

    /***
     * send a single http request
     * @param hur the http request
     * @return http resonse on success, null if error occured
     */
    public synchronized HttpResponse executeOnHttp(HttpUriRequest hur) {
        if (!isConnected()){
            return null;
        }
        try {
            //delays requests to not overwhelm the server
            Thread.sleep(requestDelayTime);
        } catch (InterruptedException e1) {
            //do nothing
        }
        try {
            return http_client.execute(hur);
        } catch (ClientProtocolException e) {
            //do nothing
        } catch (IOException e) {
            //do nothing
        }
        //returns null in case of error
        return null;
    }

    /***
     * sets the account and the context of the connection manager
     * @param account the account
     * @param c the context
     */
    public void setProps(Account account,Context c){
        if (isConnecting() || isConnecting()){
            return;
        }
        chosenAccount = account;
        cont  = c;
        props = true;
    }

    /***
     * start the connection proccess if not already connecting or connected
     */
    public void connect(){
        if (!isProps() || isConnecting()){
            return;
        }
        setConnecting(true);
        accountManager = AccountManager.get(cont.getApplicationContext());
        accountManager.getAuthToken(chosenAccount, "ah", false, new GetAuthTokenCallback(), null); //$NON-NLS-1$
    }

    /***
     *
     * @return the account name
     */
    public String getAccountString(){
        return chosenAccount.name;
    }

    /***
     * tries to reconnect using the stored account and properties
     * @return
     */
    public boolean reconnect(){
        if (isProps()){
            connect();
            return true;
        }
        return false;
    }

    /***
     * stops the connection
     */
    public void stop() {
        this.setConnected(false);
        this.setConnecting(false);
    }

    /***
     * when authentication token is recived, tris to get connection cookie
     * @param bundle contains the authentication token
     */
    protected void onGetAuthToken(Bundle bundle) {
        this.lastToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        new GetCookieTask().execute(lastToken);
    }

    /***
     * set to connecting status
     * @param isConnecting
     */
    private void setConnecting(boolean isConnecting) {
        this.isConnecting = isConnecting;
    }

    /***
     *
     * @return is connecting right now
     */
    public boolean isConnecting() {
        return isConnecting;
    }

    /***
     * set connected status
     * @param isConnected
     */
    private void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    /***
     *
     * @return is connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /***
     *
     * @return is initialized with account and context
     */
    public boolean isProps() {
        return props;
    }

    /***
     * handels authentication callback. tries to start accpet/decline activity if needed
     * @author Tomer
     *
     */
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



    /***
     * send a request to the server and hopes that the secured cookie is included in the response sent back.
     * @author Tomer
     *
     */
    private class GetCookieTask extends AsyncTask<String, Void, Boolean> {

        @Override
        /***
         * on failure to get cookie or error returns false. otherwise true
         */
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
                //nothing
            } catch (IOException e) {
                //nothing
            } finally {
                http_client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result){
                //if result ok the connection manager is connected
                setConnected(true);
                setConnecting(false);
            }
            else{
                //the connection proccess failed. invalidating authentication cookie just in case
                setConnecting(false);
                accountManager.invalidateAuthToken(chosenAccount.type, lastToken);
            }
        }
    }

}
