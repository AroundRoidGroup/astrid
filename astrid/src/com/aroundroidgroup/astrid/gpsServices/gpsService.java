package com.aroundroidgroup.astrid.gpsServices;

import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequestService;
import com.todoroo.andlib.utility.DateUtilities;

public class GPSService extends Service{

    private DataRefresher refreshData = null;

    private final LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

    private Location userLastLocation = null;
    private final Object userLocationLock = new Object();

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    //TODO find a better method for doing this
    public static Account account = null;
    public static int connectCount = 0;



    public Location getUserLastLocation(){
        synchronized (userLocationLock){
            return userLastLocation;
        }
    }

    private Location setUserLastLocation(Location l){
        synchronized (userLocationLock){
            Location temp = userLastLocation;
            userLastLocation = l;
            return temp;
        }
    }

    int mStartMode;       // indicates how to behave if the service is killed

    public void startPeopleRequests(Account acc){
        PeopleRequestService.getPeopleRequestService().connectToService(acc, this);
    }

    @Override
    public void onCreate() {
        // The service is being created
        refreshData = new DataRefresher();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        if (!refreshData.isAlive()){
            refreshData.start();
        }
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        //should be null
        return null;
    }

    @Override
    public synchronized void onDestroy() {
        // The service is no longer used and is being destroyed
        if (refreshData.isAlive()){
            refreshData.setExit();
            try {
                wait();
            } catch (InterruptedException e) {
                //do nothing!
            }
        }
    }

    private synchronized void okDestroy(){
        notifyAll();
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            //makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) { }

        public void onProviderEnabled(String provider) { }

        public void onProviderDisabled(String provider) { }
    };

    private class DataRefresher extends Thread{
        private boolean toExit = false;


        private final int defaultSleepTime = 1000;
        private final int defaultLocationInvalidateTime = 1000 * 20;

        private final int sleepTime = defaultSleepTime;
        private final int locationInvalidateTime = defaultLocationInvalidateTime;

        public void setExit(){
            this.toExit = true;
        }

        @Override
        public void run() {
            //TODO consider making userLastLocation a database entry

            //adapter to the people database
            PeopleAdapter pa = new PeopleAdapter();
            //initiate GPS
            while (!toExit){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
                if (!prs.isConnected() && connectCount>0){
                    connectCount--;
                    startPeopleRequests(account);
                }

                //Toast.makeText(GPSService.this, "Looping!", Toast.LENGTH_LONG).show();

                //make userLastLocation null if it is irrelevant because of time
                Location prevLocation = getUserLastLocation();
                //TODO check if time is supported
                if (prevLocation!=null && (DateUtilities.now()-prevLocation.getTime()>locationInvalidateTime)){
                    setUserLastLocation(null);
                }


                //TODO : check if this values can stay the same if no change to the database was made

                //calculate minimal radius for businesses
                int minBusiness = 0;
                //calculate radius for friends
                int minFriends = 0;

                //calculate minimal radius for change.
                int minTotal = Math.min(minBusiness, minFriends);


                //register to radius if needed - will take care of business notifications, and userLastLocation!
                //HEY !! - specific should by handled by addProximityAlert!?

                //locationManager.requestLocationUpdates(provider, minTime, minDistance, listener)


                //check for friends, regardless of location changed (maybe friend where moved!
                //TODO GET ALL PEOPLE, OR ALL PEOPLE THAT REQUIRE UPDATE
                String peopleArr[] = null;
                Location currentLocation = getUserLastLocation();
                //check if friends is enabled and connected and needed
                if (currentLocation!=null &&  prs.isConnected() && peopleArr.length>0){
                    List<FriendProps> lfp = prs.getPeopleLocations(peopleArr,currentLocation);
                    pa.updatePeople(lfp);
                    //TODO check if notifications are needed and notify the relevant tasks
                }




            }
            okDestroy();
        }



    }

}
