package com.aroundroidgroup.locationTags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

public class LocationByPeopleControlSet implements TaskEditControlSet{

    // --- instance variables

    private final Spinner locationSpinner;
    private final LocationService locationService = new LocationService();
    private final String[] allLocations;
    private final LinearLayout locationContainer;
    private final Activity activity;

    public LocationByPeopleControlSet(Activity activity, int locationContainer) {
        allLocations = locationService.getAllLocationsByPeople();
        this.activity = activity;
        this.locationContainer = (LinearLayout) activity.findViewById(locationContainer);
        this.locationSpinner = (Spinner) activity.findViewById(R.id.locations_by_people_dropdown);

        if(allLocations.length == 0) {
            locationSpinner.setVisibility(View.GONE);
        } else {
            ArrayList<String> dropDownList = new ArrayList<String>(Arrays.asList(allLocations));
          //TODO: if becomes the final control set, change the string here, and in location_by_poeople_row.xml and in task_edit_activity of this to be like in the case of locations by type
            dropDownList.add(0,"add a friend"); //$NON-NLS-1$
            ArrayAdapter<String> locationsAdapter = new ArrayAdapter<String>(activity,
                    android.R.layout.simple_spinner_item,
                    dropDownList);
            locationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            locationSpinner.setAdapter(locationsAdapter);
            locationSpinner.setSelection(0);

            locationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int position, long arg3) {
                    if(position == 0 || position > allLocations.length)
                        return;
                    addLocation(allLocations[position - 1], true);
                    locationSpinner.setSelection(0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // nothing!
                }
            });
        }
    }

    @Override
    public void readFromTask(Task task) {
        locationContainer.removeAllViews();

        if(task.getId() != AbstractModel.NO_ID) {
            TodorooCursor<Metadata> cursor = locationService.getLocationsByPeople(task.getId());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String location = cursor.get(LocationFields.peopleLocations);
                    addLocation(location, true);
                }
            } finally {
                cursor.close();
            }
        }
        addLocation("", false); //$NON-NLS-1$



    }

    @Override
    public String writeToModel(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(locationContainer.getChildCount() == 0)
            return null;

        LinkedHashSet<String> locations = new LinkedHashSet<String>();
        for(int i = 0; i < locationContainer.getChildCount(); i++) {
            TextView locationName = (TextView)locationContainer.getChildAt(i).findViewById(R.id.text12);
            if(locationName.getText().length() == 0)
                continue;

            locations.add(locationName.getText().toString());
        }

        if(locationService.syncLocationsByPeople(task.getId(), locations))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());

        return null;
    }

    /** Adds a location to the location field */
    boolean addLocation(String locationName, boolean reuse) {
        LayoutInflater inflater = activity.getLayoutInflater();

        // check if already exists
        TextView lastText = null;
        for(int i = 0; i < locationContainer.getChildCount(); i++) {
            View view = locationContainer.getChildAt(i);
            lastText = (TextView) view.findViewById(R.id.text12);
            if(lastText.getText().equals(locationName))
                return false;
        }

        final View locationItem;
        if(reuse && lastText != null && lastText.getText().length() == 0) {
            locationItem = (View) lastText.getParent();
        } else {
            locationItem = inflater.inflate(R.layout.location_by_people_row, null);
            locationContainer.addView(locationItem);
        }

        final AutoCompleteTextView textView = (AutoCompleteTextView)locationItem.
            findViewById(R.id.text12);
        textView.setText(locationName);
        ArrayAdapter<String> locationsAdapter =
            new ArrayAdapter<String>(activity,
                    android.R.layout.simple_dropdown_item_1line, allLocations);
        textView.setAdapter(locationsAdapter);

        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(!(count > 0 && locationContainer.getChildAt(locationContainer.getChildCount()-1) ==
                    locationItem))
                    return;
                ImageButton reminderAddButton;
                reminderAddButton = (ImageButton)locationItem.findViewById(R.id.button2);
                reminderAddButton.setVisibility(View.VISIBLE);
            }
        });

        textView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL)
                    return false;
                if(getLastTextView().getText().length() != 0) {
                    addLocation("", false); //$NON-NLS-1$
                }
                return true;
            }
        });

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)locationItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView lastView = getLastTextView();
                if(locationContainer.getChildCount()<2){
                    textView.setText(""); //$NON-NLS-1$
                    locationItem.findViewById(R.id.button2).setVisibility(View.GONE);
                    return;
                }
                if(lastView == textView){
                    View oneBeforeLastView = locationContainer.getChildAt(locationContainer.getChildCount()-2);
                    oneBeforeLastView.findViewById(R.id.button2).setVisibility(View.VISIBLE);
                }

                    if(locationContainer.getChildCount() > 1)
                        locationContainer.removeView(locationItem);
                    else
                        textView.setText(""); //$NON-NLS-1$
            }
        });

        ImageView reminderAddButton = (ImageView) locationItem.findViewById(R.id.button2);
        reminderAddButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView lastView = getLastTextView();
                if(lastView == textView && textView.getText().length() == 0)
                    return;
                addLocation("", false); //$NON-NLS-1$
                locationItem.findViewById(R.id.button2).setVisibility(View.GONE);
            }
        });
        reminderAddButton.setVisibility(View.GONE);
        return true;
    }

    /**
     * Get location container last text view. might be null
     * @return
     */
    private TextView getLastTextView() {
        if(locationContainer.getChildCount() == 0)
            return null;
        View lastItem = locationContainer.getChildAt(locationContainer.getChildCount()-1);
        TextView lastText = (TextView) lastItem.findViewById(R.id.text11);
        return lastText;
    }
}
