package com.aroundroidgroup.astrid.gpsServices;

import java.util.Collections;
import java.util.List;

import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.FriendProps;
import com.aroundroidgroup.locationTags.LocationService;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;

public class Notificator {
    public static void notifyAboutPeopleLocation(Task task,Location myLocation, double lat, double lon) {
        float[] arr = new float[3];
        //TODO : check array

        Location.distanceBetween(
                myLocation.getLatitude(),
                myLocation.getLongitude(),
                lat,lon, arr);
        float dist = arr[0];

        //distance - 100 kilometers
        //TODO change to Task.getRadius
        if (dist>100*1000)
            Notifications.cancelLocationNotification(task.getId());
        else
            ReminderService.getInstance().getScheduler().createAlarm(task, DateUtilities.now(), ReminderService.TYPE_LOCATION);
    }


    //assuming lfp is sorted by mail
    public static void notifyAllPeople(Location currentLocation,
            List<FriendProps> lfp, LocationService ls) {
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
                    Notificator.notifyAboutPeopleLocation(task, currentLocation,findMe.getDlat(),findMe.getDlon());
                }
            }
        }
    }



}
