package com.todoroo.astrid.activity;

import android.app.Activity;
import android.os.Bundle;

import com.timsu.astrid.R;

public class MapLocationActivity extends Activity {
    public static final String MAP_EXTRA_TASK = "task"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_main);
    }

}
