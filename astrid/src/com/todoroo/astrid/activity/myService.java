package com.todoroo.astrid.activity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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

import com.aroundroidgroup.locationTags.LocationTagService;
import com.aroundroidgroup.locationTags.NotesDbAdapter;
import com.aroundroidgroup.map.Misc;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
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

    private final  boolean isThreadOn = false;
    public final String TAG = "myService";

    Set<Long> alreadyNotified = new HashSet<Long>();

    private static NotesDbAdapter mDbHelper = null;

    Notifications notificatons = new Notifications();

    @Override
    public void onStart(Intent intent, int startId) {

        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Toast.makeText(this, "The Service was popoed1 ...", Toast.LENGTH_LONG).show();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mDbHelper==null){
            mDbHelper = new NotesDbAdapter(this);
            mDbHelper.open();
        }
        Toast.makeText(this, "The Service was popoed2 ...", Toast.LENGTH_LONG).show();
        gpsSetup();

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        mDbHelper.close();
        mDbHelper = null;
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
                for (String str: LocationTagService.getLocationTags(task.getId()))
                    notify(task.getId(),location,str);
            }
        } finally {
            cursor.close();
        }

    }

    private void notify(long id,Location myLocation, String str) {
        Toast.makeText(this, "popo", Toast.LENGTH_LONG).show();

        if (Misc.getPlaces(str,10,myLocation,5).isEmpty()){
            Toast.makeText(this, "yes", Toast.LENGTH_LONG).show();
            if (mDbHelper.fetchNote(id).getCount()>0){
                mDbHelper.deleteNote(id);
                Notifications.cancelNotifications(id);
            }
        }else{
            Toast.makeText(this, "no", Toast.LENGTH_LONG).show();
            if (mDbHelper.fetchNote(id).getCount()==0){
                long res = mDbHelper.createNote(id);
                Toast.makeText(this, "res is "+res+" count is "+mDbHelper.fetchNote(id).getCount(), Toast.LENGTH_LONG).show();
                notificatons.showTaskNotification(id,
                        ReminderService.TYPE_SNOOZE, "You are near");
            }
        }

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

        @Override
        public void run() {

            while(true){
                try {
                    Thread.sleep(sleepTime);

                    HttpPost http_post = new HttpPost("https://aroundroid.appspot.com/aroundgps");

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                    nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(userLastLocation.getLatitude())));
                    nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(userLastLocation.getLongitude())));
                    nameValuePairs.add(new BasicNameValuePair("USERS", "NaamaKeshet@gmail.comXXXtomer.keshet@gmail.comXXXa@b.comXXXg@c.com"));
                    http_post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse result = http_client.execute(http_post);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(result.getEntity().getContent()));
                    //TODO : remove this check
                    StringBuffer sb = new StringBuffer();
                    String first_line;
                    while ((first_line=reader.readLine())!=null){
                        sb.append(first_line+"\n");
                    }
                    @SuppressWarnings("unused")
                    String x = sb.toString();
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

        }
    }


}


}

