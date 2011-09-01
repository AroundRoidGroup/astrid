package com.aroundroidgroup.astrid.gpsServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundRoidAppConstants;
import com.aroundroidgroup.astrid.googleAccounts.AroundroidDbAdapter;
import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.googleAccounts.FriendPropsWithContactId;
import com.aroundroidgroup.astrid.googleAccounts.ManageContactsActivity;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequestService;
import com.aroundroidgroup.locationTags.LocationService;
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

    private final boolean USING_MOCK_LOCATIONS = true;

    private DataRefresher refreshData = null;

    //private final ContactsHelper contactsHelper= new ContactsHelper(getContentResolver());

    public final String TAG = "GPSService"; //$NON-NLS-1$

    private final static LocationService threadLocationService = new LocationService();


    private final AroundroidDbAdapter aDba = new AroundroidDbAdapter(this);

    private final PeopleRequestService prs = PeopleRequestService.getPeopleRequestService();

    private ContactsHelper conHel;

    private double mySpeed;

    public static Account account = null;
    public static int connectCount = 0;

    private static Object deleteObj = new Object();
    private static boolean holdDeletes = false;

    public static void lockDeletes(boolean lockIt){
        synchronized(deleteObj){
            holdDeletes = lockIt;
        }
    }

    int mStartMode;       // indicates how to behave if the service is killed

    public void startPeopleRequests(Account acc){
        PeopleRequestService.getPeopleRequestService().connectToService(acc, this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

    }

    @Override
    public void onCreate() {
        // The service is being created
        refreshData = new DataRefresher();
        conHel = new ContactsHelper(getContentResolver());
        aDba.open();
        //aDba.dropPeople();
        Cursor cur = aDba.createAndfetchSpecialUser();
        if (cur!=null){
            cur.close();
        }
        cleanDataBase(threadLocationService.getAllLocationsByPeople());


        if (!USING_MOCK_LOCATIONS){
            skyhookSetup();
        }
        else{
            gpsSetup();
            startUsingMockLocations();
        }

        refreshData.start();
    }

    private XPS _xps;
    private final MyLocationCallback _callback = new MyLocationCallback();
    WPSAuthentication auth = new WPSAuthentication("aroundroid", "AroundRoid"); //$NON-NLS-1$ //$NON-NLS-2$
    int currMin = threadLocationService.minimalRadiusRelevant(0);
    private void skyhookSetup(){
        _xps = new XPS(this);

        _xps.getXPSLocation(auth,
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
            // tell the UI thread to re-enable the buttons
        }

        public WPSContinuation handleError(WPSReturnCode error)
        {
            // send a message to display the error
            // return WPS_STOP if the user pressed the Stop button
            return WPSContinuation.WPS_CONTINUE;
        }

        public void handleIPLocation(IPLocation location)
        {
            // send a message to display the location

        }

        public void handleWPSLocation(WPSLocation location)
        {
            // send a message to display the location
            makeUseOfNewLocation(location);
        }

        public WPSContinuation handleWPSPeriodicLocation(WPSLocation location)
        {
            makeUseOfNewLocation(location);
            // return WPS_STOP if the user pressed the Stop button
            return WPSContinuation.WPS_CONTINUE;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) {
        //should be null
        return null;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        if (refreshData.isAlive()){
            refreshData.setExit();
            try {
                refreshData.join();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        if (USING_MOCK_LOCATIONS){
            LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(locationListener);
            mockLocationCreator.requestStop();
        }else{
            _xps.abort();
        }
        this.aDba.close();
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

    private void cleanDataBase(String[] realPeople) {
        synchronized (deleteObj){
            ManageContactsActivity.getAlreadyScannedSometime(false);
            Set<String> hs = new TreeSet<String>();
            for (String s : realPeople){
                hs.add(s);
            }
            Cursor cur = aDba.fetchAllPeople();
            if (cur==null){
                return;
            }
            if (cur.moveToFirst()){
                do{
                    boolean deleted = false;
                    //String mail = cur.getString(cur.getColumnIndex(AroundroidDbAdapter.KEY_MAIL));
                    long rowId = cur.getLong(cur.getColumnIndex(AroundroidDbAdapter.KEY_ROWID));
                    FriendPropsWithContactId fpwci = AroundroidDbAdapter.userToFPWithContactId(cur);
                    if (!holdDeletes && !hs.contains(fpwci.getMail()) && fpwci.getContactId()>=0){
                        aDba.deletePeople(rowId);
                        deleted = true;
                    }
                    else if (hs.contains(fpwci.getMail()) && fpwci.getContactId()>=0 && conHel.oneDisplayName(fpwci.getContactId())==null){
                        aDba.updatePeople(rowId, -2);
                    }
                    if (!deleted && !AroundRoidAppConstants.timeCheckValid(fpwci.getTimestamp()) && fpwci.isValid() ){
                        aDba.updatePeople(rowId, fpwci.getDlat(), fpwci.getDlon(), fpwci.getTimestamp(),fpwci.getContactId(),AroundRoidAppConstants.STATUS_OFFLINE);
                    }
                }while (cur.moveToNext());
            }
            cur.close();

        }
    }



    private class DataRefresher extends Thread{
        private boolean toExit = false;


        private final int defaultSleepTime = 3 * 1000;

        private final int peiodicServer = 15;


        private final int sleepTime = defaultSleepTime;

        private long lastConnectionTime;

        private final long maxWait = 1000 * 30;

        private final int peiodicDataScanMax = 120;

        private int loopCounter = -1;

        private boolean reported = false;

        public void setExit(){
            this.toExit = true;
        }

        private void handleConnection(){
            if (!prs.isConnected()){
                if (!prs.isConnecting()){

                    if (connectCount>0){
                        reported = false;
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
                    reported = true;
                    toastMe("Connection lost!!!");//$NON-NLS-1$
                }
            }
            else if (!reported){
                reported = true;
                toastMe("Connected! Hooray!"); //$NON-NLS-1$
            }
        }

        @Override
        public void run() {
            while (!toExit){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    continue;
                }

                handleConnection();
                //check if friends is enabled and connected and needed
                if (prs.isConnected()){
                    ++loopCounter;
                    if ((loopCounter % peiodicDataScanMax == 0)){
                        //periodcly cleaning the database
                        loopCounter = 0;
                        cleanDataBase(threadLocationService.getAllLocationsByPeople());
                    }
                    if ((loopCounter % peiodicServer)==0){
                        String peopleArr[] = threadLocationService.getAllLocationsByPeople();
                        FriendProps myFp = aDba.specialUserToFP();
                        //periodicly update the server and from the server
                        prs.updatePeopleLocations(peopleArr,myFp,aDba);
                    }
                }
            }
        }



    }

    private void makeUseWithoutLocation(LocStruct ls){
        Cursor cur = aDba.createAndfetchSpecialUser();
        if (cur!=null){
            if (cur.moveToFirst()){
                long l = cur.getLong(0);
                aDba.updatePeople(l,ls.getLatitude(), ls.getLongitude(), ls.getTime(),null, "Yes"); //$NON-NLS-1$
            }
            cur.close();
        }
        this.mySpeed = ls.getSpeed();


        String peopleArr[] = threadLocationService.getAllLocationsByPeople();
        List<FriendProps> lfp = new ArrayList<FriendProps>(peopleArr.length);
        for (String dude : peopleArr){
            Cursor curPeople = aDba.fetchByMail(dude);
            if (curPeople==null){
                continue;
            }
            if (curPeople.moveToFirst()){
                FriendProps fp = AroundroidDbAdapter.userToFP(curPeople);
                if (fp!=null){
                    lfp.add(fp);
                }
            }
            curPeople.close();
        }

        Notificator.handleNotifications(mySpeed,lfp,ls);

    }

    public static class LocStruct{

        private final double latitude;
        private final double longitude;
        private final double speed;
        private final long time;

        public LocStruct(double latitude, double longitude, double speed, long time){
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
            this.speed = speed;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getSpeed() {
            return speed;
        }

        public long getTime() {
            return time;
        }
    }

    protected void makeUseOfNewLocation(WPSLocation location) {
        if (aDba==null){
            return;
        }
        int realMin = threadLocationService.minimalRadiusRelevant(location.getSpeed());
        if (realMin!=currMin){
            currMin = realMin;
            _xps.abort();
            double Xspeed = Math.min(location.getSpeed(),40);
            _xps.getXPSLocation(auth,
                    // note we convert _period to seconds
                    (int)(location.getSpeed()<0.5?5*60:currMin/Xspeed),
                    currMin,
                    _callback);
        }
        makeUseWithoutLocation(new LocStruct(location.getLatitude(),location.getLongitude(),location.getSpeed(),location.getTime()));
    }

    protected void makeUseOfNewLocation(Location location){
        makeUseWithoutLocation(new LocStruct(location.getLatitude(),location.getLongitude(),location.getSpeed(),location.getTime()));
    }


    private void gpsSetup(){
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {

        public void onLocationChanged(Location location) {
            //toastMe("GPS location changed: " + location.hasSpeed()); //$NON-NLS-1$
            makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //
        }

        public void onProviderEnabled(String provider) {
            //
        }
        public void onProviderDisabled(String provider) {
            //

        }
    };


    private MockLocationCreator mockLocationCreator;
    private Thread mockLocationThread;
    private void startUsingMockLocations(){
        // start using mock locations
        try {
            mockLocationCreator = new MockLocationCreator(this.getApplicationContext());
            try {
                mockLocationCreator.openLocationList();
                mockLocationThread = new Thread(mockLocationCreator);
                mockLocationThread.start();
                Toast.makeText(this.getApplicationContext(), "Mock locations are in use", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            } catch (IOException e) {
                Toast.makeText(this.getApplicationContext(), "Error: Unable to open / read data file", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
                mockLocationCreator = null;
            }
        } catch(SecurityException e) {
            Toast.makeText(this.getApplicationContext(), "Error: Insufficient Privileges", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            Log.e(TAG, "unable to use mock locations, insufficient privileges", e); //$NON-NLS-1$
        }

    }

}
