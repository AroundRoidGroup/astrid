package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import com.todoroo.astrid.activity.SpecificMapLocation;
import com.todoroo.astrid.activity.myService;

public class AsyncAutoComplete implements Runnable{

    private final String text;


    public AsyncAutoComplete(String text) {
        this.text = new String(text);
    }

    @Override
    public void run() {
        try {
            List<String> c = Misc.googleAutoCompleteQuery(text, myService.getLastUserLocation());
            for (String s : Misc.types)
                c.add(s);
            SpecificMapLocation.updateSuggestions(c);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
