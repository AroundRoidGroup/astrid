package com.todoroo.astrid.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.aroundroidgroup.locationTags.LocationTagService;
import com.timsu.astrid.R;
import com.todoroo.astrid.data.Task;

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
