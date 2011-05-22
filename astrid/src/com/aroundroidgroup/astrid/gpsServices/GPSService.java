package com.aroundroidgroup.astrid.gpsServices;

import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequestService;
import com.aroundroidgroup.locationTags.LocationService;
import com.todoroo.andlib.utility.DateUtilities;



public class GPSService extends Service{

    private DataRefresher refreshData = null;

    private final LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

    private final ContactsHelper contactsHelper= new ContactsHelper(getContentResolver());

    private final boolean recommendedFriendsMade = false;

    private final LocationService threadLocationService = new LocationService();

    private Location userLastLocation = null;
    private final Object userLocationLock = new Object();

    private final AroundroidDbAdapter aDba = new AroundroidDbAdapter(this);

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
        aDba.open();
        aDba.dropPeople();
        gpsSetup();
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

    private void gpsSetup(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }

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

            //initiate GPS
            while (!toExit){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
                if (!prs.isConnected()){
                    if (connectCount>0){
                        //TODO stop doesn't really works
                        prs.stop();
                        connectCount--;
                        startPeopleRequests(account);
                    }
                    else if (prs.isOn()){
                        //toast the user that the connection is lost!
                    }
                }


                //Toast.makeText(GPSService.this, "Looping!", Toast.LENGTH_LONG).show();

                //make userLastLocation null if it is irrelevant because of time
                Location prevLocation = getUserLastLocation();
                if (prevLocation!=null && (DateUtilities.now()-prevLocation.getTime()>locationInvalidateTime)){
                    setUserLastLocation(null);
                }

                ///////////////////////////////////RADDDDDDDDDDIIIIIIIIIIUUUUUUUUUUUUSSSSSSSSSSSSSSS
                //TODO for now the gps setup in OnCreate must be moved here
                //calculate minimal radius for businesses
                int minBusiness = 0;
                //calculate radius for friends
                int minFriends = 0;
                //calculate minimal radius for change.
                @SuppressWarnings("unused")
                int minTotal = Math.min(minBusiness, minFriends);
                //register to radius if needed - will take care of business notifications, and userLastLocation!
                //HEY !! - specific should by handled by addProximityAlert!?
                //locationManager.requestLocationUpdates(provider, minTime, minDistance, listener)
                ///////////////////////////////////RADDDDDDDDDDIIIIIIIIIIUUUUUUUUUUUUSSSSSSSSSSSSSSS

                Location currentLocation = getUserLastLocation();
                //check if friends is enabled and connected and needed
                if (currentLocation!=null &&  prs.isConnected()){
                    String peopleArr[] = threadLocationService.getAllLocationsByPeople();
                    if ( peopleArr.length>0){
                        List<FriendProps> lfp = prs.getPeopleLocations(peopleArr,currentLocation);
                        Collections.sort(lfp, FriendProps.getMailComparator());
                        for (FriendProps fp : lfp){
                            aDba.updatePeople(fp.getLat(),fp.getLon(),fp.getTime());
                        }
                        Notificator.notifyAllPeople(currentLocation,lfp,threadLocationService);
                    }
                }



            }
            okDestroy();
        }





    }

    protected void makeUseOfNewLocation(Location location) {
        setUserLastLocation(location);
        //TODO deal with business

    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //TODO empty
        }

        public void onProviderEnabled(String provider) {
            //TODO empty
        }

        public void onProviderDisabled(String provider) {
            //TODO empty
        }
    };

}
