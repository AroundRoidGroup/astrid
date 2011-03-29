package com.todoroo.astrid.activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aroundroidgroup.locationTags.LocationTagService;
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
    Integer sum = 0;
    boolean isThreadOn = false;
    public final String TAG = "myService";

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this,"onCreate", Toast.LENGTH_LONG).show();
        Log.d(TAG," onCreate");
        new nearReminder().start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*if(!isThreadOn)
  	   {
  		   isThreadOn = true;
  		   SumCalc sumCalc = new SumCalc();
  		   sumCalc.start();
   	   	   Toast.makeText(this,"onStartCommand. Run New Thread", Toast.LENGTH_LONG).show();
    	}
  	   else
	   	   Toast.makeText(this,"onStartCommand. sum is:" + sum, Toast.LENGTH_LONG).show();
         */
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

            //Task task = (Task) b.getParcelable(MAP_EXTRA_TASK);
            //long itemId = task.getId();

            //   Toast.makeText(myService.this,"onStartCommand. Run New Thread", Toast.LENGTH_LONG).show();

            TaskService taskService = new TaskService();

            int a =0;
            while(DateUtilities.now()%120000>200)
                a++;

            TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE,
                    Task.IMPORTANCE, Task.DUE_DATE).where(Criterion.and(TaskCriteria.isActive(),
                            TaskCriteria.isVisible())).
                            orderBy(SortHelper.defaultTaskOrder()).limit(30));
            try {

                Task task = new Task();
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToNext();
                    task.readFromCursor(cursor);
                    if (LocationTagService.getLocationTags(task.getId()).length!=0){
                        new Notifications().showTaskNotification(task.getId(),
                                ReminderService.TYPE_SNOOZE, "You are near!!!111");
                    }
                    /*
                    StringBuilder taskTags = new StringBuilder();
                    TodorooCursor<Metadata> tagCursor = TagService.getInstance().getTags(task.getId());
                    try {
                        for(tagCursor.moveToFirst(); !tagCursor.isAfterLast(); tagCursor.moveToNext())
                            taskTags.append(tagCursor.get(TagService.TAG)).append(TAG_SEPARATOR);
                    } finally {
                        tagCursor.close();
                    }

                    Object[] values = new Object[7];
                    values[0] = task.getValue(Task.TITLE);
                    values[1] = importanceColors[task.getValue(Task.IMPORTANCE)];
                    values[2] = task.getValue(Task.DUE_DATE);
                    values[3] = task.getValue(Task.DUE_DATE);
                    values[4] = task.getValue(Task.IMPORTANCE);
                    values[5] = task.getId();
                    values[6] = taskTags.toString();

                    ret.addRow(values);
                     */}
            } finally {
                cursor.close();
            }

            //taskService.query(new Query())
        }
    }


}

