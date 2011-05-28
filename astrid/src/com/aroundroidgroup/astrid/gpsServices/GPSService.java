package com.aroundroidgroup.astrid.gpsServices;

import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequestService;
import com.aroundroidgroup.locationTags.LocationService;
import com.todoroo.andlib.utility.DateUtilities;



public class GPSService extends Service{

    private DataRefresher refreshData = null;

    //private final ContactsHelper contactsHelper= new ContactsHelper(getContentResolver());

    public final String TAG = "GPSService";

    private final static LocationService threadLocationService = new LocationService();

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
    public void onStart(Intent intent, int startId) {

        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Toast.makeText(getApplicationContext(), "OnStart!?!", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onCreate() {
        Toast.makeText(getApplicationContext(), "OnCreate", Toast.LENGTH_LONG).show();
        // The service is being created
        refreshData = new DataRefresher();
        aDba.open();
        aDba.dropPeople();
        aDba.createPeople("me",-1L);
        gpsSetup();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        if (!refreshData.isAlive()){
            refreshData.start();
        }
        return START_STICKY;
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
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Location loc = locationManager.getLastKnownLocation(provider);
        if (loc!=null){
            makeUseOfNewLocation(loc);
        }
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }

    private final Handler mHandler = new Handler();
    private String mToastMsg;
    private final Runnable mUpdateResults = new Runnable() {
        public void run() {
            if (mToastMsg!=null){
                Toast.makeText(GPSService.this, mToastMsg, Toast.LENGTH_LONG).show();
            }
        }
    };

    private  synchronized void toastMe(String toastMsg){
        this.mToastMsg = toastMsg;
        new Thread() {
            @Override
            public void run() {
                mHandler.post(mUpdateResults);
            }
        }.start();
    }

    private class DataRefresher extends Thread{
        private boolean toExit = false;


        private final int defaultSleepTime = 1000 * 20;
        private final int defaultLocationInvalidateTime = 1000 * 120;

        private final int sleepTime = defaultSleepTime;
        private final int locationInvalidateTime = defaultLocationInvalidateTime;

        private long lastConnectionTime;

        private final long maxWait = 1000 * 90;

        private boolean reported = false;

        public void setExit(){
            this.toExit = true;
        }

        @Override
        public void run() {
            //TODO consider making userLastLocation a database entry

            //TODO if isConnecting for to long, force close
            //initiate GPS
            while (!toExit){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
                if (!prs.isConnected()){
                    if (!prs.isConnecting()){
                        if (connectCount>0){
                            reported = false;
                            //TODO stop doesn't really works
                            prs.stop();
                            connectCount--;
                            lastConnectionTime = DateUtilities.now();
                            startPeopleRequests(account);
                        }
                        else if (!reported && (prs.isOn())){
                            reported = true;
                            toastMe("Connection lost!!!");//$NON-NLS-1$
                        }
                    }
                    else if (DateUtilities.now()-lastConnectionTime>maxWait){
                        prs.stop();
                    }
                }
                else if (!reported){
                    reported = true;
                    toastMe("Connected! Hooray!"); //$NON-NLS-1$
                }


                //Toast.makeText(GPSService.this, "Looping!", Toast.LENGTH_LONG).show();

                //make userLastLocation null if it is irrelevant because of time
                Location prevLocation = getUserLastLocation();

                //TODO find out the date problem
                if (prevLocation!=null && (DateUtilities.now()-prevLocation.getTime()>locationInvalidateTime)){
                    //setUserLastLocation(null);
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
                    for (String people : peopleArr){
                        Cursor curMail  =aDba.fetchByMail(people);
                        if (curMail==null || !curMail.moveToFirst()){
                            aDba.createPeople(people);
                        }
                    }
                    if ( peopleArr.length>0){
                        List<FriendProps> lfp = prs.getPeopleLocations(peopleArr,currentLocation);
                        for (FriendProps fp : lfp){
                            Cursor c = aDba.fetchByMail(fp.getMail());
                            if (c!=null && c.moveToFirst()){
                                long id = c.getLong(0);
                                c.close();
                                aDba.updatePeople(id,fp.getDlat(),fp.getDlon(),fp.getTimestamp());
                            }

                        }
                        //TODO doesnt notify!?
                        Notificator.notifyAllPeople(currentLocation,lfp,threadLocationService);
                    }
                }



            }
            okDestroy();
        }





    }

    protected void makeUseOfNewLocation(Location location) {
        Toast.makeText(getApplicationContext(), "Coords are: Lat - "+location.getLatitude()+" ,Lon - " + location.getLongitude(), Toast.LENGTH_LONG).show();
        //TODO fetch by other id
        Cursor cur = aDba.fetchByMail("me");
        if (cur!=null && cur.moveToFirst()){
            long l = cur.getLong(0);
            cur.close();
            aDba.updatePeople(l,location.getLatitude(), location.getLongitude(), location.getTime());
        }
        setUserLastLocation(location);
        Notificator.handleByTypeAndBySpecificNotification(location);
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
            Toast.makeText(getApplicationContext(), "GPS Enabled!", Toast.LENGTH_LONG).show();
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Disabled!", Toast.LENGTH_LONG).show();
        }
    };




}
