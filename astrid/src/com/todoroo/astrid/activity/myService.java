package com.todoroo.astrid.activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

import com.aroundroidgroup.locationTags.LocationTagService;
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
                for (String str: LocationTagService.getLocationTags(task.getId()))
                    notify(task,location,str);
            }
        } finally {
            cursor.close();
        }

    }

    private void notify(Task task,Location myLocation, String str) {
        Toast.makeText(this, "popo", Toast.LENGTH_LONG).show();


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

        private final String gpsUri = "https://aroundroid.appspot.com/aroundgps"; //$NON-NLS-1$

        private class FriendProps{

            private String lat,lon;

            private String mail;

            public String getLat() {
                return lat;
            }

            public void setLat(String lat) {
                this.lat = lat;
            }

            public String getLon() {
                return lon;
            }

            public void setLon(String lon) {
                this.lon = lon;
            }

            public String getMail() {
                return mail;
            }

            public void setMail(String mail) {
                this.mail = mail;
            }

            public FriendProps() {
                // TODO Auto-generated constructor stub
            }

            @Override
            public String toString(){
                return getMail() + "::" + getLat() + "::" + getLon(); //$NON-NLS-1$ //$NON-NLS-2$
            }

        }

        private List<NameValuePair> createPostData(){
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("GPSLAT", String.valueOf(userLastLocation.getLatitude())));
            nameValuePairs.add(new BasicNameValuePair("GPSLON", String.valueOf(userLastLocation.getLongitude())));
            nameValuePairs.add(new BasicNameValuePair("USERS", "NaamaKeshet@gmail.comXXXtomer.keshet@gmail.comXXXa@b.comXXXg@c.com"));
            return nameValuePairs;
        }


        @Override
        public void run() {

            while(true){
                try {
                    Thread.sleep(sleepTime);

                    HttpPost http_post = new HttpPost(gpsUri);
                    http_post.setEntity(new UrlEncodedFormEntity(createPostData()));

                    HttpResponse result = http_client.execute(http_post);
                    InputStream is = result.getEntity().getContent();

                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(is);
                    doc.getDocumentElement().normalize();
                    //System.out.println("Root element " + doc.getDocumentElement().getNodeName());
                    NodeList nodeLst = doc.getElementsByTagName("Friend");
                    //System.out.println("Information of all employees");

                    List<FriendProps> lfp = new ArrayList<FriendProps>();

                    for (int s = 0; s < nodeLst.getLength(); s++) {

                        Node fstNode = nodeLst.item(s);
                        FriendProps fp = new FriendProps();

                        if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element fstElmnt = (Element) fstNode;
                            NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("Latitude");
                            Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                            NodeList fstNm = fstNmElmnt.getChildNodes();
                            fp.setLat(((Node) fstNm.item(0)).getNodeValue());
                            NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("Longtitude");
                            Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
                            NodeList lstNm = lstNmElmnt.getChildNodes();
                            fp.setLon(((Node) lstNm.item(0)).getNodeValue());
                            NodeList mailNmElmntLst = fstElmnt.getElementsByTagName("Mail");
                            Element mailNmElmnt = (Element) mailNmElmntLst.item(0);
                            NodeList mailNm = mailNmElmnt.getChildNodes();
                            fp.setMail(((Node) mailNm.item(0)).getNodeValue());

                            lfp.add(fp);
                        }
                    }
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }



            }


        }
    }

}

