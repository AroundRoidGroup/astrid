package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import com.todoroo.astrid.activity.SpecificMapLocation;
import com.todoroo.astrid.activity.myService;

public class AsyncAutoComplete implements Runnable{

    private static final String[] types = {"accounting", "airport", "amusement park", "aquarium", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "art gallery", "atm", "bakery", "bank", "bar", "beauty salon", "bicycle store", "book store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
        "bowling alley", "bus station", "cafe", "campground", "car dealer", "car rental", "car repair", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "car wash", "casino", "cemetery", "church", "city hall", "clothing store", "convenience store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "courthouse", "dentist", "department store", "doctor", "electrician", "electronics store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "embassy", "establishment", "finance", "fire station", "florist", "food", "funeral home", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "furniture store", "gas station", "general contractor", "geocode", "grocery or supermarket", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "gym", "hair care", "hardware store", "health", "hindu temple", "home goods store", "hospital", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "insurance agency", "jewelry store", "laundry", "lawyer", "library", "liquor store", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "local government office", "locksmith", "lodging", "meal delivery", "meal takeaway", "mosque", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "movie rental", "movie theater", "moving company", "museum", "night club", "painter", "park", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "parking", "pet store", "pharmacy", "physiotherapist", "place of worship", "plumber", "police", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "post office", "real estate agency", "restaurant", "roofing contractor", "rv park", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "shoe store", "shopping mall", "spa", "stadium", "storage", "store", "subway station", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "synagogue", "taxi stand", "train station", "travel agency", "university", "veterinary care", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "zoo"};     //$NON-NLS-1$
    private final String text;

    public AsyncAutoComplete(String text) {
        this.text = new String(text);
    }

    @Override
    public void run() {
        try {
            List<String> c = Misc.googleAutoCompleteQuery(text, myService.getLastUserLocation());
            for (String s : types)
                c.add(s);
            SpecificMapLocation.updateSuggestions(c);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
