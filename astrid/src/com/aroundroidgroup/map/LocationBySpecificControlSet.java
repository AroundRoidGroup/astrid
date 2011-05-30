package com.aroundroidgroup.map;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.aroundroidgroup.locationTags.LocationService;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.SpecificMapLocation;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;

public class LocationBySpecificControlSet implements TaskEditControlSet{
    private final Map<DPoint, String> pointsAndAddresses = new HashMap<DPoint, String>();
    private final LocationService locationService = new LocationService();
    private final Activity activity;
    private long taskID;

    public LocationBySpecificControlSet(Activity activity) {
        this.activity = activity;
        Button b = (Button) activity.findViewById(R.id.location_button);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContextManager.getContext(), SpecificMapLocation.class);
                String[] specificLocations = new String[pointsAndAddresses.size() + 1];

                /* sending the taskID as first parameter */
                specificLocations[0] = Long.toString(taskID);
                int i = 1;
                for (Map.Entry<DPoint, String> pair : pointsAndAddresses.entrySet()) {
                    specificLocations[i] = pair.getKey().toString();
                    i++;
                }
                intent.putExtra(SpecificMapLocation.SPECIFIC_POINTS, specificLocations);
                LocationBySpecificControlSet.this.activity.startActivityForResult(intent, 1);
            }
        });
    }

    public void updateSpecificPoints(DPoint[] points, String[] addresses) {
        if (points != null) {
            pointsAndAddresses.clear();
            for (int i = 0 ; i < points.length ; i++)
                pointsAndAddresses.put(points[i], addresses[i]);
        }
    }

    @Override
    public void readFromTask(Task task) {
        taskID = task.getId();
        String[] allSpecific =  locationService.getLocationsBySpecificAsArray(task.getId());
        mainLoop: for (String s : allSpecific) {
            for (Map.Entry<DPoint, String> pair : pointsAndAddresses.entrySet())
                if (s.equalsIgnoreCase(pair.getKey().toString()))
                    continue mainLoop;
            pointsAndAddresses.put(new DPoint(s), null);
        }
    }

    @Override
    public String writeToModel(Task task) {
        if (pointsAndAddresses != null) {
            LinkedHashSet<String> mashu = new LinkedHashSet<String>();
            for (Map.Entry<DPoint, String> pair : pointsAndAddresses.entrySet())
                mashu.add(new String(pair.getKey().toString()));
            locationService.syncLocationsBySpecific(task.getId(), mashu);
            if(locationService.syncLocationsByType(task.getId(), mashu))
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
        return null;
    }

}
