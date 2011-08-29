package com.aroundroidgroup.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Task;

public class LocationControlSet implements TaskEditControlSet{
    private final Map<String, String> mSpecific = new HashMap<String, String>();
    private final List<String> mTypes = new ArrayList<String>();
    private final LocationService locationService = new LocationService();
    private final Activity mActivity;
    private long mTaskID;
    private boolean readTask = false;

    public static final String TASK_ID = "taskID"; //$NON-NLS-1$
    public static final String SPECIFIC_TO_LOAD = "specific"; //$NON-NLS-1$
    public static final String TYPE_TO_LOAD = "kind"; //$NON-NLS-1$
    public static final String PEOPLE_TO_LOAD = "people"; //$NON-NLS-1$

    public LocationControlSet(Activity activity) {
        mActivity = activity;
        readTask = false;

        Button b = (Button) activity.findViewById(R.id.location_button);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContextManager.getContext(), SpecificMapLocation.class);

                /* Adding the task ID to the intent */
                intent.putExtra(TASK_ID, mTaskID);

                /* Adding the specific locations to the intent */
                String[] specific = new String[2 * mSpecific.size()];
                int i = 0;
                for (Map.Entry<String, String> pair : mSpecific.entrySet()) {
                    specific[i] = pair.getKey();
                    specific[i + 1] = pair.getValue();
                    i += 2;
                }
                intent.putExtra(SPECIFIC_TO_LOAD, specific);

                /* Adding the kind locations to the intent */
                i = 0;
//                for (Map.Entry<String, List<String>> pair : mTypes.entrySet())
//                    i += pair.getValue().size();
                String[] types = new String[mTypes.size()];
//                i = 0;
//                for (Map.Entry<String, List<String>> pair : mTypes.entrySet()) {
//                    List<String> lst = pair.getValue();
//                    types[i++] = pair.getKey() + lst.size();
//                    for (int k = 0 ; k < lst.size() ; k++)
//                        types[i++] = lst.get(k);
//                }
                for (String type : mTypes)
                    types[i++] = type;
                intent.putExtra(TYPE_TO_LOAD, types);

                LocationControlSet.this.mActivity.startActivityForResult(intent, TaskEditActivity.REQUEST_CODE_SpecificMapLocation);
            }
        });
    }

    public void updateSpecificPoints(String[] points, String[] addresses) {
        if (points != null) {
            mSpecific.clear();
            for (int i = 0 ; i < points.length ; i++)
                mSpecific.put(points[i], addresses[i]);
        }
    }

    public void updateTypes(String[] typesToUpdate) {
        if (typesToUpdate != null) {
            mTypes.clear();
            for (String s : typesToUpdate)
            mTypes.add(s);
        }
    }

    @Override
    public void readFromTask(Task task) {
        if (!readTask) {

            mTaskID = task.getId();

            /* adding existed specific points */
            String[] allSpecific =  locationService.getLocationsBySpecificAsArray(mTaskID);
            firstLoop: for (String s : allSpecific) {
                for (Map.Entry<String, String> pair : mSpecific.entrySet())
                    if (s.equalsIgnoreCase(pair.getKey().toString()))
                        continue firstLoop;
                mSpecific.put(s, null);
            }

            /* adding existed types */
            String[] allTypes = locationService.getLocationsByTypeAsArray(mTaskID);
            for (String type : allTypes) {

                mTypes.add(type);
            }
            readTask = true;
        }
    }

    @Override
    public String writeToModel(Task task) {
        LinkedHashSet<String> mashu = new LinkedHashSet<String>();
        if (mSpecific != null) {

            for (Map.Entry<String, String> pair : mSpecific.entrySet())
                mashu.add(pair.getKey());
            if(locationService.syncLocationsBySpecific(task.getId(), mashu))
                task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
        mashu.clear();
        for (String type : mTypes) {
            mashu.add(type);

        }
        if(locationService.syncLocationsByType(task.getId(), mashu))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        return null;
    }

}
