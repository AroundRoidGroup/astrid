package com.aroundroidgroup.astrid.gpsServices;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

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

    private WPSLocation userLastLocation = null;
    private final Object userLocationLock = new Object();

    private final AroundroidDbAdapter aDba = new AroundroidDbAdapter(this);

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    //TODO find a better method for doing this
    public static Account account = null;
    public static int connectCount = 0;

    public WPSLocation getUserLastLocation(){
        synchronized (userLocationLock){
            return userLastLocation;
        }
    }

    private WPSLocation setUserLastLocation(WPSLocation location){
        synchronized (userLocationLock){
            WPSLocation temp = userLastLocation;
            userLastLocation = location;
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
        aDba.createAndfetchSpecialUser();
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
        Toast.makeText(getApplicationContext(), "service onCreate", Toast.LENGTH_LONG).show();

        _xps.getXPSLocation(auth,
                            // note we convert _period to seconds
                            30,
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
            toastMe("handleWPSPeriodicLocation "+location.getLatitude()+" "+location.getLongitude()+" speed: "+location.getSpeed());
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
                Toast.makeText(GPSService.this, mToastMsg, Toast.LENGTH_LONG).show();
            }
        }
    };

    private final List<UpdataeListener> listeners = new ArrayList<UpdataeListener>();

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

                DPoint dp = aDba.specialUserToDPoint();
                if (dp!=null){
                    toastMe("LAT :" + String.valueOf(dp.getX()) + ", LON :" + String.valueOf(dp.getY()) );
                }
                else{
                    toastMe("NO LOCATION");
                }


                //Toast.makeText(GPSService.this, "Looping!", Toast.LENGTH_LONG).show();

                //make userLastLocation null if it is irrelevant because of time
                WPSLocation prevLocation = getUserLastLocation();

                //TODO find out the date problem
                if (prevLocation!=null && (DateUtilities.now()-prevLocation.getTime() <=locationInvalidateTime)){
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

                WPSLocation currentLocation = getUserLastLocation();
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

    protected void makeUseOfNewLocation(WPSLocation location) {
       //TODO fetch by other id
        Cursor cur = aDba.createAndfetchSpecialUser();
        if (cur!=null && cur.moveToFirst()){
            long l = cur.getLong(0);
            cur.close();
            aDba.updatePeople(l,location.getLatitude(), location.getLongitude(), location.getTime());
        }
        setUserLastLocation(location);
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
        for (UpdataeListener listener: listeners)
            listener.onEvenet();
    }

    public void addListener(UpdataeListener listener){
        listeners.add(listener);
    }

    public void removeFromListener(UpdataeListener listener){
        listeners.remove(listener);
    }
}
