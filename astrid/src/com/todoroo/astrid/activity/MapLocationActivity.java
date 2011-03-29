package com.todoroo.astrid.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.aroundroidgroup.map.DPoint;
import com.aroundroidgroup.map.IPoint;
import com.aroundroidgroup.map.ListOfLocations;
import com.aroundroidgroup.map.SimpleParser;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;


public class MapLocationActivity extends MapActivity  {

    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private final Task mCurrentTask = null;


	MapView mapView;
	HelloItemizedOverlay itemizedoverlay;
	List<Overlay> mapOverlays;
	/** Called when the activity is first created. */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_main);
		mapView = (MapView) findViewById(R.id.mapview);

		/* determine the central point in the map to be Tel-Aviv, Israel */

		mapView.getController().setCenter(new GeoPoint(32068611, 34788208));

		/* enable zoom option */
		mapView.setBuiltInZoomControls(true);
		mapOverlays = mapView.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.icon_32);
		itemizedoverlay = new HelloItemizedOverlay(drawable);
				GeoPoint point = new GeoPoint(19240000,-99120000);
				OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!", "I'm angrito la birdo!");
				GeoPoint point2 = new GeoPoint(35410000, 139460000);
				OverlayItem overlayitem2 = new OverlayItem(point2, "Sekai, konichiwa!", "I'm in Japan!");
				itemizedoverlay.addOverlay(overlayitem);
				itemizedoverlay.addOverlay(overlayitem2);
		mapOverlays.add(itemizedoverlay);
	}

	public IPoint degToGeo(DPoint dp) {
		return new IPoint((int)(dp.getX() * 1000000), (int)(dp.getY() * 1000000));
	}

	public void addLocation(View view) {
		try {
			String what = ((EditText)findViewById(R.id.fldWhat)).getEditableText().toString();
			String radius = ((EditText)findViewById(R.id.fldRds)).getEditableText().toString();
			String from = ((EditText)findViewById(R.id.fldFrom)).getEditableText().toString();
			DPoint points = null;
			points = SimpleParser.getCoords(from);
			Map<String, String> map = ListOfLocations.send(what, Double.parseDouble(radius), points);
			if (map != null) {
				IPoint geoP;
				for (Map.Entry<String, String> p : map.entrySet()) {
					points = SimpleParser.getCoords(p.getValue());
					if (points != null) {
						geoP = degToGeo(points);
						GeoPoint point = new GeoPoint(geoP.getX() , geoP.getY());
						itemizedoverlay.addOverlay(new OverlayItem(point, what,  p.getKey()));
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class HelloItemizedOverlay extends ItemizedOverlay<OverlayItem> {
		private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		Context mContext;

		public HelloItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
			// TODO Auto-generated constructor stub
		}

		public HelloItemizedOverlay(Drawable defaultMarker, Context context) {
			super(defaultMarker);
			mContext = context;
		}

		@Override
		protected boolean onTap(int index) {
			return true;
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
//		public void removeOverlay(OverlayItem overlay) {
//			mOverlays.remove(overlay);
//			populate();
//		}
	}
}

/*
public class MapLocationActivity extends Activity {
    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    private Task mCurrentTask = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_main);
        Bundle b = getIntent().getExtras();
        mCurrentTask = (Task)b.getParcelable(MAP_EXTRA_TASK) ;

        TextView tv = (TextView) findViewById(R.id.textview);
        tv.setText(LocationTagService.getLocationTags(mCurrentTask.getId())[0]);
    }

}
*/
