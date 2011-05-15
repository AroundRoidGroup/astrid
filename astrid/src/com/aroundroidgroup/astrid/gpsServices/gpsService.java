package com.aroundroidgroup.astrid.gpsServices;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

public class GPSService extends Service{

    private DataRefresher refreshData = null;

    private Location userLastLocation = null;
    private final Object userLocationLock = new Object();


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
            while (!toExit){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
                //calculate minimal radius for change.

                //register to radius if needed - will take care of specific and business notifications

                //check for friends, regardless of location changed!


            }
            okDestroy();
        }



    }

}
