package com.todoroo.astrid.activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class myService extends Service{
    Integer sum = 0;
    boolean isThreadOn = false;
    public final String TAG = "myService";

    private static Object serviceSingelton = null;


    public static boolean isRunning(){
        return (serviceSingelton!=null);
    }


    @Override
    public void onCreate() {
        serviceSingelton = new Object();
        super.onCreate();
        Toast.makeText(this,"onCreate", Toast.LENGTH_LONG).show();
        Log.d(TAG," onCreate");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isThreadOn)
  	   {
  		   isThreadOn = true;
  		   //SumCalc sumCalc = new SumCalc();
  		   //sumCalc.start();
  	       new nearReminder().start();
   	   	   Toast.makeText(this,"onStartCommand. Run New Thread", Toast.LENGTH_LONG).show();
    	}
  	   else
	   	   Toast.makeText(this,"onStartCommand. sum is:" + sum, Toast.LENGTH_LONG).show();

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        serviceSingelton = null;
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

            //TaskService taskService = new TaskService();



            while (true){

                //Location l = Misc.getDeviceLocation();
                //double d = l.getLongitude();

                try {
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            }
            /*
            while(DateUtilities.now()%120000>200)
                a++;

            TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE,
                    Task.IMPORTANCE, Task.DUE_DATE).where(Criterion.and(TaskCriteria.isActive(),
                            TaskCriteria.isVisible())).
                            orderBy(SortHelper.defaultTaskOrder()).limit(30));
            /*try {

                Task task = new Task();
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToNext();
                    task.readFromCursor(cursor);
                    if (motifunc(task).length>0)
                        new Notifications().showTaskNotification(task.getId(),
                                ReminderService.TYPE_SNOOZE, "You are near");
                    }
            } finally {
                cursor.close();
            }*/

            //taskService.query(new Query())

        }
    }


}

