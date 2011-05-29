package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.xml.sax.SAXException;

import android.location.Location;

import com.aroundroidgroup.astrid.googleAccounts.AroundRoidAppConstants;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest;
import com.aroundroidgroup.astrid.googleAccounts.PeopleRequest.FriendProps;
import com.todoroo.astrid.activity.myService;

public class mapFunctions {

    public static final int SNIPPET_ADDRESS = 0;
    public static final int SNIPPET_EMAIL = 1;
    public static final int TITLE_BUSINESS_NAME = 2;
    public static final int TITLE_SPECIFIC = 3;
    public static final int TITLE_TASK_NAME = 4;

    public static int[] addTagsToMap(AdjustedMap map, String overlayUniqueName, String[] locationTypes, double radius) {
        int i = 0;
        int[] feedback = new int[locationTypes.length];
        for (String type : locationTypes) {
            try {
                Map<String, DPoint> kindLocations = Misc.googlePlacesQuery(type, myService.getLastUserLocation(), radius);
                if (!kindLocations.isEmpty())
                    feedback[i] = 1;
                /* running on all the tags (bank, post-office, ATM, etc...) */
                for (Map.Entry<String, DPoint> p : kindLocations.entrySet())
                    map.addItemToOverlay(Misc.degToGeo(p.getValue()), p.getKey(),
                            type, Geocoding.reverseGeocoding(p.getValue()),
                            overlayUniqueName);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
        return feedback;
    }

    public static void addLocationSetToMap(AdjustedMap map, String overlayUniqueName, DPoint[] locations, String title) {
        for (DPoint d : locations)
            map.addItemToOverlay(Misc.degToGeo(d), title, d.toString(), null, overlayUniqueName);
    }

    public static void addPeopleToMap(AdjustedMap map, String overlayUniqueName, String[] people) {
          try {
              String cat = AroundRoidAppConstants.join(people, "::"); //$NON-NLS-1$
              myService.httpLock.lock();
              try{
                  List<FriendProps> fp = PeopleRequest.requestPeople(new Location(new String()), cat);
                  for (FriendProps f : fp) {
                      //TODO replace the 4th parameter from null to address ????????
                      map.addItemToOverlay(Misc.degToGeo(new DPoint(Double.parseDouble(f.getLat()), Double.parseDouble(f.getLon()))),
                              "Person Name", f.getMail(), null, overlayUniqueName); //$NON-NLS-1$
                  }
              } finally {
                  myService.httpLock.unlock();
              }
          } catch (ClientProtocolException e) {
              e.printStackTrace();
          } catch (IOException e) {
              e.printStackTrace();
          } catch (ParserConfigurationException e) {
              e.printStackTrace();
          } catch (SAXException e) {
              e.printStackTrace();
          }
    }
}
