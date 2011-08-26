package com.aroundroidgroup.astrid.gpsServices;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.locationTags.LocationFields;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.Misc;
import com.skyhookwireless.wps.WPSLocation;
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
/**
 *
 * @author Shiran
 *
 */
public class Notificator {

    static LocationService locationService = new LocationService();

    public static void notifyAboutPeopleLocation(Task task,double speed, double myLat, double myLon, double lat, double lon) {
        float[] arr = new float[1];

        Location.distanceBetween(
                myLat,
                myLon,
                lat,lon, arr);
        float dist = arr[0];
        int radius;
        if (speed>LocationFields.carSpeedThreshold)
            radius = locationService.getCarRadius(task.getId());
        else
            radius = locationService.getFootRadius(task.getId());

        if (dist>radius)
            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);
    }


    //assuming lfp is sorted by mail
    //TODO this!
    public static void notifyAllPeople(FriendProps myFp,double speed,
            List<FriendProps> lfp, LocationService ls) {
        if (!myFp.isValid()){
            return;
        }
        //notify the tasks
        TodorooCursor<Task> cursor = AstridQueries.getDefaultCursor();
        Task task = new Task();
        for (int i = 0; i < cursor.getCount(); i++) {
            FriendProps exampleProps = new FriendProps();
            cursor.moveToNext();
            task.readFromCursor(cursor);
            String[] mails = ls.getLocationsByPeopleAsArray(task.getId());
            for (String str : mails){
                exampleProps.setMail(str);
                int index = Collections.binarySearch(lfp, exampleProps, FriendProps.getMailComparator());
                if (index>=0){
                    FriendProps findMe = lfp.get(index);
                    if (findMe.isValid()){
                        Notificator.notifyAboutPeopleLocation(task, myFp.getDlat(),myFp.getDlon(),speed,findMe.getDlat(),findMe.getDlon());
                    }
                }
            }
        }
        cursor.close();
    }

    public static void handleByTypeAndBySpecificNotification(WPSLocation location) {
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
                notifyAboutLocationIfNeeded(task,location, false);
            }
        } finally {
            cursor.close();
        }
    }

    private static void notifyAboutLocationIfNeeded(Task task,WPSLocation location, boolean inDriveMode) {
        //Toast.makeText(ContextManager.getContext(), "popo", Toast.LENGTH_LONG).show();
        int radius;
        if (inDriveMode)
            radius = locationService.getCarRadius(task.getId());
        else
            radius = locationService.getFootRadius(task.getId());
        if (!(notifyAboutSpecificLocationNeeded(task, location, radius) ||
                notifyAboutTypeOfLocationNeeded(task, location, radius)))
            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);
    }

    private static boolean notifyAboutTypeOfLocationNeeded(Task task,
            WPSLocation location, int radius) {
        DPoint loc = new DPoint(location.getLatitude(),location.getLongitude());
        for (String str: locationService.getLocationsByTypeAsArray(task.getId()))
            try {
                Map<String, DPoint> places = Misc.googlePlacesQuery(str,loc,radius);
                List<DPoint> blackList = locationService.getLocationsByTypeBlacklist(task.getId(), str);
                outer_loop: for (DPoint d: places.values())
                    for (DPoint badD: blackList){
                        if (Double.compare(d.getX(), badD.getX())==0 && Double.compare(d.getY(), badD.getY())==0)
                            continue outer_loop;
                        return true; //gets here if the location was not blacklisted
                    }
                return false;
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return false;
    }

    private static boolean notifyAboutSpecificLocationNeeded(Task task,
            WPSLocation location, int radius) {
        for (String str: locationService.getLocationsBySpecificAsArray(task.getId())){
            DPoint dp = new DPoint(str);
            float[] arr = new float[1];
            Location.distanceBetween(
                    location.getLatitude(),
                    location.getLongitude(),
                    dp.getX(),dp.getY(), arr);
            if (arr[0]<=radius)
                return true;
        }
        return false;
    }


}
