package com.aroundroidgroup.astrid.googleAccounts;

import java.util.List;

import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;

public class PeopleRequestService {

    //Singleton
    private PeopleRequestService(){
        //empty
    }

    private static PeopleRequestService singleton = null;

    public static PeopleRequestService getPeopleRequestService(){
        if (singleton==null){
            singleton = new PeopleRequestService();
        }
        return singleton;
    }

    public boolean isConnected() {
        // TODO Auto-generated method stub
        return false;
    }

    public List<FriendProps> updateAboutPeople(String[] peopleArr, Location l) {
        // TODO Auto-generated method stub
        // TODO check if location l is null
        return null;
    }


}
