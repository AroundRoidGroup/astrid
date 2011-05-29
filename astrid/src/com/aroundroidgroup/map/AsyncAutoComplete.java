package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import com.todoroo.astrid.activity.SpecificMapLocation;

public class AsyncAutoComplete implements Runnable{

    private final String text;


    public AsyncAutoComplete(String text) {
        this.text = new String(text);
    }

    @Override
    public void run() {
        try {
            //TODO USERLOCATION
            if (true)
                return;
            DPoint d = new DPoint(1.0,1.0);
            List<String> c = Misc.googleAutoCompleteQuery(text, d);
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
