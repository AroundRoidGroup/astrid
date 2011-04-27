package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aroundroidgroup.astrid.googleAccounts.AroundRoidAppConstants;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.Misc;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.TaskService;


public class myService extends Service{
    private static Location userLastLocation;

    private static DefaultHttpClient http_client = new DefaultHttpClient();
    private static CheckFriendThread cft;


    public final String TAG = "myService";

    private final Notifications notificatons = new Notifications();

    private final LocationService locationService = new LocationService();
    @Override
    public void onStart(Intent intent, int startId) {

        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Toast.makeText(this, "The Service was popoed1 ...", Toast.LENGTH_LONG).show();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "The Service was popoed2 ...", Toast.LENGTH_LONG).show();
        gpsSetup();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "The Service was destroyed ...", Toast.LENGTH_LONG).show();
        Log.d(TAG," onDestroy");

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate(Bundle savedInstanceState) {

        Toast.makeText(this, "The Service was popoed ...", Toast.LENGTH_LONG).show();
        //       gpsSetup();
    }


    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            makeUseOfNewLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) { }

        public void onProviderEnabled(String provider) { }

        public void onProviderDisabled(String provider) { }
    };

    private void gpsSetup(){
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        makeUseOfNewLocation(location);
        locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
    }


    public void makeUseOfNewLocation(Location location) {
        userLastLocation = location;
        Toast.makeText(this, location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();
        TaskService taskService = new TaskService();

        TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE,
                Task.IMPORTANCE, Task.DUE_DATE).where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible())).
                        orderBy(SortHelper.defaultTaskOrder()).limit(30));
        try {

            Task task = new Task();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                task.readFromCursor(cursor);
                for (String str: locationService.getLocationsByTypeAsArray(task.getId()))
                    notifyAboutLocation(task,location,str);
            }
        } finally {
            cursor.close();
        }

    }

    private static void notifyAboutPeopleLocation(Task task,Location myLocation, FriendProps fp) {
        //Toast.makeText(ContextManager.getContext(), "popo", Toast.LENGTH_LONG).show();
        float[] arr = new float[3];
        //TODO : check array
        Location.distanceBetween(myLocation.getLatitude(), myLocation.getLongitude(),Double.parseDouble(fp.getLat()), Double.parseDouble(fp.getLon()), arr);
        float dist = arr[0];

        //distense - 100 kilometers
        if (dist>100*1000)
            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);

    }


//    private static boolean isFar(Location myLocation, FriendProps fp) {
//        return !(Math.abs((myLocation.getLatitude()-Double.parseDouble(fp.getLat())))<1);
//    }


    private static void notifyAboutLocation(Task task,Location myLocation, String str) {
        //Toast.makeText(ContextManager.getContext(), "popo", Toast.LENGTH_LONG).show();


        if (Misc.getPlaces(str,10,myLocation,5).isEmpty())
            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);

    }
    public static Location getLastUserLocation() {
        return userLastLocation;
    }

    public static boolean restartClient(){
        //TODO : work

        if (cft==null || !cft.isAlive()){
            return false;
        }
        return true;
    }

    public static  boolean startCheckFriendThread(){
        if (cft==null || !cft.isAlive()){
            cft = new CheckFriendThread();
            cft.start();
            return true;
        }
        return false;
    }

    public static DefaultHttpClient getHttpClient(){
        return myService.http_client;
    }


    protected static class CheckFriendThread extends Thread{

        private final long sleepTime = 1000* 0;

        private final LocationService threadLocationService = new LocationService();


        @SuppressWarnings("null")
        @Override
        public void run() {

            while(true){
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                TaskService taskService = new TaskService();

                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE,
                        Task.IMPORTANCE, Task.DUE_DATE).where(Criterion.and(TaskCriteria.isActive(),
                                TaskCriteria.isVisible())).
                                orderBy(SortHelper.defaultTaskOrder()).limit(30));
                try {

                    Task task = new Task();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        cursor.moveToNext();
                        task.readFromCursor(cursor);
                        List<FriendProps> lfp = null;
                        try {
                            String peopleString = AroundRoidAppConstants.join(threadLocationService.getLocationsByPeopleAsArray(task.getId())
                                    ,AroundRoidAppConstants.usersDelimiter);
                            lfp = PeopleRequest.requestPeople(userLastLocation,peopleString);
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

                        for (FriendProps myFriend: lfp)
                            notifyAboutPeopleLocation(task,userLastLocation,myFriend);
                    }
                } finally {
                    cursor.close();
                }



            }



        }
    }

}

