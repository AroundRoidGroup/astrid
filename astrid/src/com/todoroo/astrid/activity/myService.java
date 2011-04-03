package com.todoroo.astrid.activity;
import java.util.HashSet;
import java.util.Set;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aroundroidgroup.locationTags.LocationTagService;
import com.aroundroidgroup.map.Misc;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.TaskService;

public class myService extends Service{
    boolean isThreadOn = false;
    public final String TAG = "myService";
    Set<Long> alreadyNotified = new HashSet<Long>();
    Notifications notificatons = new Notifications();

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this,"onCreate", Toast.LENGTH_LONG).show();
        Log.d(TAG," onCreate");
        //Misc.gpsSetup(this);
        new nearReminder().start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isThreadOn)
        {
            isThreadOn = true;
            new nearReminder().start();
        }
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

    public class nearReminder extends Thread {

        @Override
        public void run() {
            while(true){
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
                            notify(task.getId(),str);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        private void notify(long id, String str) {
            if (Misc.getPlacesAroundMe(str, 100, 5).isEmpty()){
                if (alreadyNotified.contains(id)){
                    alreadyNotified.remove(id);
                 //   Notifications.cancelNotifications(id);
                }
            }else{
                if (!alreadyNotified.contains(id)){
                    alreadyNotified.add(id);
                  //  notificatons.showTaskNotification(id,
                  //          ReminderService.TYPE_SNOOZE, "You are near");
                }
            }

        }
    }


}

