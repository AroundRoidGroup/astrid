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

	@Override
	    public void onCreate() {
	          super.onCreate();
	     	   Toast.makeText(this,"onCreate", Toast.LENGTH_LONG).show();
	   		Log.d(TAG," onCreate");

	    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
  	   if(!isThreadOn)
  	   {
  		   isThreadOn = true;
  		   SumCalc sumCalc = new SumCalc();
  		   sumCalc.start();
   	   	   Toast.makeText(this,"onStartCommand. Run New Thread", Toast.LENGTH_LONG).show();
    	}
  	   else
	   	   Toast.makeText(this,"onStartCommand. sum is:" + sum, Toast.LENGTH_LONG).show();

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

    public class SumCalc extends Thread {

        @Override
        public void run() {
        	sum = 0;
            for(Integer idx = 0; idx< 1000000; idx ++)
            {
          	   sum++;
            }
            isThreadOn = false;
        }
    }


}

