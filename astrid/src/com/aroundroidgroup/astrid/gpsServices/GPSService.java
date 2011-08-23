package com.aroundroidgroup.astrid.gpsServices;

import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;

import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequestService;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.DPoint;
import com.skyhookwireless.wps.IPLocation;
import com.skyhookwireless.wps.IPLocationCallback;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;
import com.todoroo.andlib.utility.DateUtilities;



public class GPSService extends Service{

    private DataRefresher refreshData = null;

    //private final ContactsHelper contactsHelper= new ContactsHelper(getContentResolver());

    public final String TAG = "GPSService"; //$NON-NLS-1$

    private final static LocationService threadLocationService = new LocationService();

    private final WPSLocation userLastLocation = null;
    private final Object userLocationLock = new Object();

    private final AroundroidDbAdapter aDba = new AroundroidDbAdapter(this);

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    private double mySpeed;

    //TODO find a better method for doing this
    public static Account account = null;
    public static int connectCount = 0;


    int mStartMode;       // indicates how to behave if the service is killed

    public void startPeopleRequests(Account acc){
        PeopleRequestService.getPeopleRequestService().connectToService(acc, this);
    }

    @Override
    public void onStart(Intent intent, int startId) {

        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        //Toast.makeText(getApplicationContext(), "OnStart!?!", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onCreate() {
        //Toast.makeText(getApplicationContext(), "OnCreate", Toast.LENGTH_LONG).show();
        // The service is being created
        refreshData = new DataRefresher();
        aDba.open();
        aDba.dropPeople();
        aDba.createSpecialUser();
        skyhookSetup();
    }

    private static final int LOCATION_MESSAGE = 1;
    private static final int ERROR_MESSAGE = 2;
    private static final int DONE_MESSAGE = 3;
    private XPS _xps;
    private final MyLocationCallback _callback = new MyLocationCallback();
    WPSAuthentication auth = new WPSAuthentication("aroundroid", "AroundRoid");
    int currMin = threadLocationService.minimalRadiusRelevant(0);
    private void skyhookSetup(){
        _xps = new XPS(this);
        //Toast.makeText(getApplicationContext(), "service onCreate", Toast.LENGTH_LONG).show();

        _xps.getXPSLocation(auth,
                // note we convert _period to seconds
                10,
                currMin,
                _callback);
    }

    private class MyLocationCallback
    implements IPLocationCallback,
    WPSLocationCallback,
    WPSPeriodicLocationCallback
    {
        public void done()
        {
            toastMe("WPS done");
            // tell the UI thread to re-enable the buttons
        }

        public WPSContinuation handleError(WPSReturnCode error)
        {
            toastMe("WPS handleError");
            // send a message to display the error
            // return WPS_STOP if the user pressed the Stop button
            return WPSContinuation.WPS_CONTINUE;
        }

        public void handleIPLocation(IPLocation location)
        {
            // send a message to display the location
            toastMe("WPS handleIPLocation");

        }

        public void handleWPSLocation(WPSLocation location)
        {
            // send a message to display the location
            //toastMe("WPS handleWPSLocation "+location.getLatitude()+" "+location.getLongitude()+" speed: "+location.getSpeed());
            makeUseOfNewLocation(location);
        }

        public WPSContinuation handleWPSPeriodicLocation(WPSLocation location)
        {
            toastMe("handleWPSPeriodicLocation "+location.getLatitude()+" "+location.getLongitude()+" speed: "+location.getSpeed()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            makeUseOfNewLocation(location);
            // return WPS_STOP if the user pressed the Stop button
            return WPSContinuation.WPS_CONTINUE;
        }
    }

    public DPoint getNew() {
        return new DPoint(50.0, 50.0);
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
        this.aDba.close();
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



    private final Handler mHandler = new Handler();
    private String mToastMsg;
    private final Runnable mUpdateResults = new Runnable() {
        public void run() {
            if (mToastMsg!=null){
                //Toast.makeText(GPSService.this, mToastMsg, Toast.LENGTH_LONG).show();
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


        private final int defaultSleepTime = 1 * 1;
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
            //TODO CRASHES WHEN NOT CONNECTED TO SYNC FOR USER (NAAMAKESHET@GMAIL.com RED EXCAMATION MARK)

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


                //TODO move inside of prs.isconnected
                FriendProps myFp = aDba.specialUserToFP();

                //check if friends is enabled and connected and needed
                if (prs.isConnected()){
                    //TODO once a while delete from the database all records that are not in peopleArr
                    String peopleArr[] = threadLocationService.getAllLocationsByPeople();
                    if ( peopleArr.length>0){
                        List<FriendProps> lfp = prs.updatePeopleLocations(peopleArr,myFp,aDba);
                        //TODO doesn't notify!?
                        if (myFp!=null && myFp.isValid()){
                            Notificator.notifyAllPeople(myFp,mySpeed,lfp,threadLocationService);
                        }
                    }
                }



            }
            okDestroy();
        }

    }

    protected void makeUseOfNewLocation(WPSLocation location) {
        //TODO fetch by other id
        Cursor cur = aDba.createAndfetchSpecialUser();
        if (cur!=null){
            if (cur.moveToFirst()){
                long l = cur.getLong(0);
                aDba.updatePeople(l,location.getLatitude(), location.getLongitude(), location.getTime(),null, "Yes");
            }
            cur.close();
        }


        this.mySpeed = location.getSpeed();
        Notificator.handleByTypeAndBySpecificNotification(location);
        int realMin = threadLocationService.minimalRadiusRelevant(location.getSpeed());
        if (realMin!=currMin){
            currMin = realMin;
            _xps.abort();
            double speed = Math.min(location.getSpeed(),40);
            _xps.getXPSLocation(auth,
                    // note we convert _period to seconds
                    (int)(location.getSpeed()<0.5?5*60:currMin/speed),
                    currMin,
                    _callback);
        }
    }

    /*
    private void gpsSetup(){
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Location lastLoc = locationManager.getLastKnownLocation(provider);
        if (lastLoc!=null){
            //TODO fill
            //makeUseOfNewLocation(lastLoc);
        }
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            //TODO fill
            //makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //TODO empty
        }

        public void onProviderEnabled(String provider) {
            //Toast.makeText(getApplicationContext(), "GPS Enabled!", Toast.LENGTH_LONG).show();
        }
        public void onProviderDisabled(String provider) {
            //Toast.makeText(getApplicationContext(), "GPS Disabled!", Toast.LENGTH_LONG).show();

        }
    };
     */



}
