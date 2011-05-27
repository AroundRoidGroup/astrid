package com.aroundroidgroup.astrid.googleAccounts;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.content.Context;

import com.skyhookwireless.wps.WPSLocation;

public class PeopleRequestService {

    private final AroundRoidConnectionManager arcm;

    private boolean isOn = false;


    //Singleton
    private PeopleRequestService(){
        arcm = new AroundRoidConnectionManager();
    }

    private static PeopleRequestService singleton = null;

    public static PeopleRequestService getPeopleRequestService(){
        if (singleton==null){
            singleton = new PeopleRequestService();
        }
        return singleton;
    }

    public boolean isConnected() {
        return arcm.isConnected();
    }

    public boolean isConnecting() {
        return arcm.isConnecting();
    }

    public void connectToService(Account a,Context c){
        setOn(true);
        if (arcm.isConnecting() || arcm.isConnected()){
            return;
        }
        arcm.setProps(a, c);
        arcm.connect();

    }

    //returns a sorted list!
    public List<FriendProps> getPeopleLocations(String[] peopleArr, WPSLocation userLocation) {
        // TODO check if location l is null
        // TODO not good implementation, cancel PeopleRequest class!
        String peopleString = AroundRoidAppConstants.join(peopleArr
                ,AroundRoidAppConstants.usersDelimiter);
        try {
            List<FriendProps> lfp = PeopleRequest.requestPeople(userLocation,peopleString, arcm);
            Collections.sort(lfp, FriendProps.getMailComparator());
            return lfp;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.stop();
        return null;
    }

    private void setOn(boolean isOn) {
        this.isOn = isOn;
    }

    public boolean isOn() {
        return isOn;
    }

    public void resume(){
        if (arcm.isProps()){
            arcm.connect();
        }
    }

    public void stop(){
        setOn(false);
        if (arcm.isConnected()){
            arcm.reconnect();
        }
    }

}
