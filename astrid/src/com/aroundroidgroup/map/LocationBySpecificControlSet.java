package com.aroundroidgroup.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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
    private final List<DPoint> specificPoints = new ArrayList<DPoint>();
    private final LocationService locationService = new LocationService();
    private final Activity activity;

    public LocationBySpecificControlSet(Activity activity) {
        this.activity = activity;
        Button b = (Button) activity.findViewById(R.id.location_by_specific_button);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                Toast.makeText(LocationBySpecificControlSet.this.activity, "ovedddddddd", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ContextManager.getContext(), SpecificMapLocation.class);
                String[] sa = new String[specificPoints.size()];
                for (int i = 0 ; i < specificPoints.size() ; i++)
                    sa[i] = new String(specificPoints.get(i).getX() + "," + specificPoints.get(i).getY());
                intent.putExtra(SpecificMapLocation.SPECIFIC_POINTS, sa);
                LocationBySpecificControlSet.this.activity.startActivityForResult(intent, 1);
            }
        });
    }

    public void updateSpecificPoints(DPoint[] points) {
        for (DPoint d : points)
            specificPoints.add(d);
    }

    @Override
    public void readFromTask(Task task) {
        String[] allSpecific =  locationService.getLocationsBySpecificAsArray(task.getId());
        for (String s : allSpecific)
            specificPoints.add(new DPoint(Double.parseDouble(s.substring(0, s.indexOf(','))),
                    Double.parseDouble(s.substring(s.indexOf(',') + 1))));
    }

    @Override
    public String writeToModel(Task task) {
        LinkedHashSet<String> mashu = new LinkedHashSet<String>();
        for (DPoint dp : specificPoints)
            mashu.add(new String(dp.getX() + "," + dp.getY()));
        locationService.syncLocationsBySpecific(task.getId(), mashu);

        if(locationService.syncLocationsByType(task.getId(), mashu))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        return null;
    }

}
