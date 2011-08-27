package com.aroundroidgroup.astrid.gpsServices;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.LocationManager;
import android.provider.Settings;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

/**
 * This class provides the checkGPSEnabled function which is called when
 * the user opens Astrid and checks that the GPS is enabled
 *
 */
public class GPSEnablingDialog {

    public static Activity activity;
    private static Resources r;
    private static final String userAllowsGpsEnabled = "userAllowsGpsEnabled"; //$NON-NLS-1$

    /**
     * The function checks whether the GPS is enabled in the device.
     * If it is not, it prompts the user with a dialog informing him
     * and with a link to the device's GPS settings
     * @param act: The activity from which the function is called.
     *Preferences.setBoolean("popos",true);
        b = Preferences.getBoolean("popos",false);
     */
    public static void checkGPSEnabled(Activity act){
        activity = act;
        r = act.getResources();
        final LocationManager manager = (LocationManager) act.getSystemService( Context.LOCATION_SERVICE );
        if (manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) )
            Preferences.setBoolean(userAllowsGpsEnabled,true);
        else
            if (Preferences.getBoolean(userAllowsGpsEnabled,true))
                buildAlertMessageNoGps();

    }

    private static void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setMessage(r.getString(R.string.gps_not_enabled))
        .setCancelable(false)
        .setPositiveButton(r.getString(R.string.DLG_yes), new DialogInterface.OnClickListener() {

            public void onClick(final DialogInterface dialog, final int id) {
                final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings"); //$NON-NLS-1$ //$NON-NLS-2$
                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(toLaunch);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivityForResult(intent, 0);
            }
        })
        .setNegativeButton(r.getString(R.string.DLG_no), new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                Preferences.setBoolean(userAllowsGpsEnabled,false);
                dialog.cancel();
            }
        });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
