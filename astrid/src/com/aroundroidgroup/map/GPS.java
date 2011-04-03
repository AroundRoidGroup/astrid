package com.aroundroidgroup.map;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPS {

	private Location location;
	private boolean alreadyObtainedLocation = false;
	public GPS(Activity activity) {
		gpsSetup(activity);
	}
	
	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			makeUseOfNewLocation(location);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {	}

		public void onProviderDisabled(String provider) {}
	};
	
	private void gpsSetup(Activity activity) {
		LocationManager locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String provider = locationManager.getBestProvider(criteria, true);
		Location location = locationManager.getLastKnownLocation(provider);
		makeUseOfNewLocation(location);
		locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
	}
	
	private void makeUseOfNewLocation(Location location) {
		alreadyObtainedLocation = true;
		this.location = location;
	}
	
	public Location getLastDeviceLocation() {
			return location;
	}
}
