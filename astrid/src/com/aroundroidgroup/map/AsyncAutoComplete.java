package com.aroundroidgroup.map;

import java.util.ArrayList;
import java.util.List;

import com.todoroo.astrid.activity.SpecificMapLocation;

public class AsyncAutoComplete implements Runnable{

    private final String text;


    public AsyncAutoComplete(String text) {
        this.text = new String(text);
    }

    @Override
    public void run() {
        List<String> c = new ArrayList<String>();
        for (String s : Misc.types)
            c.add(s);
        SpecificMapLocation.updateSuggestions(c);

    }

}
