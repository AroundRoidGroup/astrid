package com.aroundroidgroup.astrid.googleAccounts;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;

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

    public void connectToService(Account a,Context c){
        setOn(true);
        if (arcm.isConnecting() || arcm.isConnected()){
            return;
        }
        arcm.setProps(a, c);
        arcm.connect();

    }

    public List<FriendProps> getPeopleLocations(String[] peopleArr, Location l) {
        // TODO Auto-generated method stub
        // TODO check if location l is null
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
    }

}
