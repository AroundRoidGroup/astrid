package com.aroundroidgroup.map;

public class placeInfo {
	String title;

	String streetAddress;
	String region;
	String city;
	String country;

	String staticMapUrl;
	String url;

	double lng;
	double lat;

	public placeInfo(String title, String streetAddress, String region,
			String city, String country, double lng, double lat,
			String staticMapUrl, String url) {

		if (title != null) this.title = title;
		if (streetAddress != null) this.streetAddress = streetAddress;
		if (region != null) this.region = region;
		if (city != null) this.city = city;
		if (country != null) this.country = country;
		if (staticMapUrl != null) this.staticMapUrl = staticMapUrl;
		if (url != null) this.url = url;
		this.lng = lng;
		this.lat = lat;
	}

	@Override
    public String toString() {
		String s = ""; //$NON-NLS-1$
		s += "Title: " + title + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "street Address: " + streetAddress + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "region: " + region + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "city: " + city + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "country: " + country + "\n";  //$NON-NLS-1$//$NON-NLS-2$
		s += "static Map Url: " + staticMapUrl + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "url: " + url + "\n";  //$NON-NLS-1$//$NON-NLS-2$
		s += "lng: " + lng + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		s += "lat: " + lat + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		return s;
	}

	public String getRegion() {
		return region;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getStaticMapUrl() {
		return staticMapUrl;
	}

	public double getLng() {
		return lng;
	}

	public double getLat() {
		return lat;
	}

	public String getUrl() {
		return url;
	}

	public String getCountry() {
		return country;
	}

	public String getTitle() {
		return title;
	}
}
