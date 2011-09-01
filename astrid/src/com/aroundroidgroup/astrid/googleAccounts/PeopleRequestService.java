package com.aroundroidgroup.astrid.googleAccounts;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;

/***
 * responsible of handling requests and responses to the server, and updating the database (for people update request)
 * @author Tomer
 *
 */
public class PeopleRequestService {

    private final ConnectionManager arcm;

    private boolean isOn = false;

    //private constuctor - singelton!
    private PeopleRequestService(){
        arcm = new ConnectionManager();
    }

    private static PeopleRequestService singleton = null;

    /***
     *
     * @return people request service singleton instance
     */
    public static PeopleRequestService getPeopleRequestService(){
        if (singleton==null){
            singleton = new PeopleRequestService();
        }
        return singleton;
    }

    /***
     *
     * @return is the service connected
     */
    public boolean isConnected() {
        return arcm.isConnected();
    }

    /***
     *
     * @return  is the service connecting
     */
    public boolean isConnecting() {
        return arcm.isConnecting();
    }


    /***
     *
     * @return the name of the connected account
     */
    public String getAccountString(){
        return arcm.getAccountString();
    }


    /***
     * connects to the service using an account and context
     * @param a the account to connect
     * @param c context
     */
    public void connectToService(Account a,Context c){
        setOn(true);
        //if is connecting or connected igonres the call.
        if (arcm.isConnecting() || arcm.isConnected()){
            return;
        }
        arcm.setProps(a, c);
        arcm.connect();
    }


    /***
     * sending an invitation to a friend
     * @param friend the mail of the friend to be invited
     * @return true is mail was sent. false if mail not sent or error occured
     */
    public boolean inviteFriend(String friend){
        if (!isConnected()){
            return false;
        }
        try {
            boolean res = PeopleRequest.inviteMail(friend, arcm);
            return res;
        } catch (ClientProtocolException e) {
            //do nothing
        } catch (IOException e) {
            //do nothing
        }
        //if error occurs the service is stopped.
        this.stop();
        return false;
    }

    /***
     * create a new database record or updates an old one with the matching friend props
     * @param fp the friend props to update
     * @param aDba the aroundroid data base adapter that is connected to the database
     */
    private void propsToDatabase(FriendProps fp , AroundroidDbAdapter aDba){
        Cursor cur = aDba.fetchByMail(fp.getMail());
        long rowid;
        if (cur==null){
            //ignore
            return;
        }
        if (!cur.moveToFirst()){
            rowid = aDba.createPeople(fp.getMail());
        }else{
            rowid = cur.getLong(cur.getColumnIndex(AroundroidDbAdapter.KEY_ROWID));
        }
        cur.close();
        if (rowid == -1L){
            //ignore
            return;
        }
        aDba.updatePeople(rowid, fp.getDlat(), fp.getDlon(), fp.getTimestamp(), null, fp.getValid());


    }

    /***
     * updates the record (or creating) for every mail address in peopleArr, in the aroundroid local database. Stopping the service in case of error.
     * @param peopleArr hold the email addresses
     * @param myFp the current user gps coordinates and information. may be null
     * @param aDba the aroundroid db adapter connected to the database
     * @return sorted list of friend props of the people corresponding to the people in peopleArr, or null if error occured
     */
    public List<FriendProps> updatePeopleLocations(String[] peopleArr, FriendProps myFp, AroundroidDbAdapter aDba) {
        if (!isConnected()){
            return null;
        }
        try {
            List<FriendProps> lfp = PeopleRequest.requestPeople(myFp,peopleArr, arcm);
            Collections.sort(lfp, FriendProps.getMailComparator());
            for (FriendProps fp : lfp){
                propsToDatabase(fp, aDba);
            }
            return lfp;
        } catch (ClientProtocolException e) {
            int x = 5;
            x = 3;
            //nothing
        } catch (IOException e) {
            int x = 5;
            x = 3;
            //nothing
        } catch (ParserConfigurationException e) {
            int x = 5;
            x = 3;
            //nothing
        } catch (SAXException e) {
            int x = 5;
            x = 3;
            //nothing
        }
        //on error the service is stopped and null is returned
        this.stop();
        return null;
    }

    /**
     * sets server on or off
     * @param isOn
     */
    private void setOn(boolean isOn) {
        this.isOn = isOn;
    }

    /***
     *
     * @return true if service is on (not connected, ON).
     */
    public boolean isOn() {
        return isOn;
    }

    /***
     * resumes the service
     */
    public void resume(){
        if (arcm.isProps()){
            arcm.connect();
        }
    }

    /***
     * stops the service
     */
    public void stop(){
        setOn(false);
        arcm.stop();
    }

}