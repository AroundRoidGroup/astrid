package com.aroundroidgroup.astrid.gpsServices;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.astrid.gpsServices.GPSService.LocStruct;
import com.aroundroidgroup.locationTags.LocationFields;
import com.aroundroidgroup.locationTags.LocationService;
import com.aroundroidgroup.map.DPoint;
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

public class Notificator {

    private final static LocationService locationService = new LocationService();

    /**  list of all people followed by any of the tasks   */
    private static List<FriendProps> listFriendProps;

    /**
     * returns true if and only if the task should pop a notification about
     * a person's location
     * @param task: the task to check
     * @param locStruct: the device's location
     * @param radius: the task's alert radius
     * @return true iff the task should pop a notification about
     * a person's location
     */
    private static boolean notifyAboutPeopleLocationNeeded(Task task,
            LocStruct locStruct, int radius) {

        for (String str: locationService.getLocationsByPeopleAsArray(task.getId())){
            DPoint dp = getPersonLocation(str);
            float[] arr = new float[1];
            Location.distanceBetween(
                    locStruct.getLatitude(),
                    locStruct.getLongitude(),
                    dp.getX(),dp.getY(), arr);
            if (arr[0]<=radius)
                return true;
        }
        return false;

    }

    private static DPoint getPersonLocation(String str) {
        for (FriendProps prop: listFriendProps){
            if (prop.getMail().compareTo(str)==0)
                return new DPoint(Double.parseDouble(prop.getLat()), Double.parseDouble(prop.getLon()));
        }
        return null;
    }

    /**
     * returns true if and only if the task should pop a notification about
     * a type of location
     * @param task: the task to check
     * @param locStruct: the device's location
     * @param radius: the task's alert radius
     * @return true iff the task should pop a notification about
     * a type of location
     */
    private static boolean notifyAboutTypeOfLocationNeeded(Task task,
            LocStruct locStruct, int radius) {
        DPoint loc = new DPoint(locStruct.getLatitude(),locStruct.getLongitude());
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

    /**
     * returns true if and only if the task should pop a notification about
     * a specific location
     * @param task: the task to check
     * @param locStruct: the device's location
     * @param radius: the task's alert radius
     * @return true iff the task should pop a notification about
     * a specific location
     */
    private static boolean notifyAboutSpecificLocationNeeded(Task task,
            LocStruct locStruct, int radius) {
        for (String str: locationService.getLocationsBySpecificAsArray(task.getId())){
            DPoint dp = new DPoint(str);
            float[] arr = new float[1];
            Location.distanceBetween(
                    locStruct.getLatitude(),
                    locStruct.getLongitude(),
                    dp.getX(),dp.getY(), arr);
            if (arr[0]<=radius)
                return true;
        }
        return false;
    }

    /**
     * pops location based notifications to all tasks that should be popped
     * @param mySpeed: the speed of the device
     * @param lfp: list of all people followed by any of the tasks
     * @param ls: the device's location
     */
    public static void handleNotifications(double mySpeed,
            List<FriendProps> lfp, LocStruct ls) {
        listFriendProps = lfp;
        int radius = 0;
        boolean isInCarMode = mySpeed>LocationFields.carSpeedThreshold;

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
                if (isInCarMode)
                    radius = locationService.getCarRadius(task.getId());
                else
                    radius = locationService.getFootRadius(task.getId());

                notifyAboutLocationIfNeeded(task,ls, radius);
            }
        } finally {
            cursor.close();
        }

    }

    /**
     * pops a location based notification to the user about the task if needed
     *
     * @param task: the task to notify about
     * @param ls: the location of the device
     * @param radius: the alert radius of the task
     */
    private static void notifyAboutLocationIfNeeded(Task task, LocStruct ls,
            int radius) {
        if (!(notifyAboutSpecificLocationNeeded(task, ls, radius) ||
                notifyAboutTypeOfLocationNeeded(task, ls, radius) ||
                notifyAboutPeopleLocationNeeded(task, ls, radius)))

            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);

    }
}
