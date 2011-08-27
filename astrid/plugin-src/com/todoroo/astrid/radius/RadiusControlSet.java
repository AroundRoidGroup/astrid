package com.todoroo.astrid.radius;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aroundroidgroup.locationTags.LocationService;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.api.R;
import com.todoroo.astrid.data.Task;

/**
 * The control set for the car radius and the foot radius
 *
 */
public class RadiusControlSet implements TaskEditControlSet{
    private final CheckBox enabled;
    private final LinearLayout radiusContainer;
    private final SeekBar carRadiusSelector;
    private final SeekBar footRadiusSelector;
    private final TextView footValue;
    private final TextView carValue;
    private final LocationService locService= new LocationService();


    public RadiusControlSet(final Activity activity, ViewGroup parent) {
        DependencyInjectionService.getInstance().inject(this);
        LayoutInflater.from(activity).inflate(R.layout.radius_control, parent, true);
        enabled = (CheckBox) activity.findViewById(R.id.radiusEnabled);
        radiusContainer = (LinearLayout) activity.findViewById(R.id.radiusContainer);
        footRadiusSelector = (SeekBar) activity.findViewById(R.id.footRaduis);
        carRadiusSelector = (SeekBar) activity.findViewById(R.id.carRaduis);
        footValue = (TextView) activity.findViewById(R.id.footRadiusValue);
        carValue = (TextView) activity.findViewById(R.id.carRadiusValue);


        enabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                radiusContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                carRadiusSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                      boolean fromUser) {
                        carValue.setText(String.valueOf(progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                       //
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                       //
                    }
                        });
                footRadiusSelector.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                      boolean fromUser) {
                        footValue.setText(String.valueOf(progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        //
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        //
                    }
                });
            }
        });

    }

    @Override
    public void readFromTask(Task task) {
        int  footRadius = locService.getFootRadius(task.getId());
        int  carRadius = locService.getCarRadius(task.getId());
        // Get current value from settings

        if(footRadius== -1)
            footRadius = Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_foot_radius_key));
        if(carRadius== -1)
            carRadius = Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_car_radius_key));

        // Setup footRadiusSelector/carRadiusSelector and the view text
           footRadiusSelector.setProgress(footRadius);
           carRadiusSelector.setProgress(carRadius);
           carValue.setText(carRadius+""); //$NON-NLS-1$
           footValue.setText(footRadius+""); //$NON-NLS-1$
           if(footRadius!=Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_foot_radius_key)) ||
                   carRadius!=Integer.parseInt(Preferences.getStringValue(R.string.p_rmd_default_car_radius_key))){
               enabled.setChecked(false);
               enabled.setChecked(true);
           }else{
               enabled.setChecked(true);
               enabled.setChecked(false);
           }
    }

    @Override
    public String writeToModel(Task task) {
        if(!enabled.isChecked())
            return null;
        if (locService.syncFootRadius(task.getId(), Integer.parseInt((String)footValue.getText())) ||
        locService.syncCarRadius(task.getId(), Integer.parseInt((String)carValue.getText())))
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        return null;
    }

}
