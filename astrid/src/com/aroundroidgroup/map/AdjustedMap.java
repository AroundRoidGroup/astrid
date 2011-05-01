package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.aroundroidgroup.locationTags.LocationService;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;

public class AdjustedMap extends MapView {

    Context context = null;
    private DPoint lastPointedLocation = null;
    private long currentTastID;
    private String address = null;
    private MapItemizedOverlay specificOverlays;
    private List<Overlay> mapOverlays;

    public AdjustedMap(Context context, String apiKey) {
        super(context, apiKey);
        this.context = context;
        initOverlays();
    }

    public AdjustedMap(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initOverlays();
    }

    public AdjustedMap(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initOverlays();
    }

    public void associateMapWithTask(long taskID) {
        currentTastID = taskID;
    }

    private void initOverlays() {
        mapOverlays = getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.icon_pp);
        specificOverlays = new MapItemizedOverlay(drawable);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int actionType = event.getAction();
        switch (actionType) {
        case MotionEvent.ACTION_UP:
            GeoPoint p = this.getProjection().fromPixels((int)event.getX(), (int)event.getY());
            lastPointedLocation = Misc.geoToDeg(p);

            /* popping up a dialog so the user could confirm his location choice */
            AlertDialog dialog = new AlertDialog.Builder(context).create();
            dialog.setIcon(android.R.drawable.ic_dialog_alert);

            /* setting the dialog title */
            dialog.setTitle("Confirm Specific Location"); //$NON-NLS-1$

            /* setting the dialog message. in this case, asking the user if he's sure he */
            /* wants to add the tapped location to the task, so the task will be specific- */
            /* location based */


            try {
                address = Geocoding.reverseGeocoding(lastPointedLocation);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (address != null) {
                dialog.setMessage("Would you like to add the following location:\n" + address); //$NON-NLS-1$

                /* setting the confirm button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        /* adding the location to the task */
                        LocationService x = new LocationService();
                        String[] allSpecific = x.getLocationsBySpecificAsArray(currentTastID);
                        LinkedHashSet<String> la = new LinkedHashSet<String>();
                        for (int i = 0 ; i < allSpecific.length ; i++)
                            la.add(allSpecific[i]);
                        la.add(lastPointedLocation.getX() + "," + lastPointedLocation.getY()); //$NON-NLS-1$
                        x.syncLocationsBySpecific(currentTastID, la);
                       specificOverlays.addOverlay(new OverlayItem(Misc.degToGeo(lastPointedLocation), address, "Specific Location"));
                       mapOverlays.add(specificOverlays);
                    }
                });
                /* setting the refuse button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
            }
            else Toast.makeText(context, "fall back!", Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
        }
        return super.dispatchTouchEvent(event);
    }

    public DPoint getPointedCoordinates() {
        return lastPointedLocation;
    }

    public class MapItemizedOverlay extends ItemizedOverlay<OverlayItem> {
        private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
        Context mContext;

        public MapItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
            // TODO Auto-generated constructor stub
        }

        public MapItemizedOverlay(Drawable defaultMarker, Context context) {
            super(defaultMarker);
            mContext = context;
        }

        @Override
        protected OverlayItem createItem(int i) {
            return mOverlays.get(i);
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

        public void addOverlay(OverlayItem overlay) {
            mOverlays.add(overlay);
            populate();
        }
    }

}
