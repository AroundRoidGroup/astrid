package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.os.AsyncTask;

import com.todoroo.astrid.activity.SpecificMapLocation;

public class AutoComplete extends AsyncTask<String, Integer, List<String>> {

    private final SpecificMapLocation sml;

    public AutoComplete(SpecificMapLocation sml) {
        this.sml = sml;
    }

    @Override
    protected List<String> doInBackground(String... strings) {
        List<String> c = null;
        try {
            DPoint d = new DPoint(40.714867,-74.006009);
            c = Misc.googleAutoCompleteQuery(strings[0], d);
            for (String s : Misc.types)
                c.add(s);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return c;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        return;
    }

    @Override
    protected void onPostExecute(List<String> result) {
        SpecificMapLocation.updateSuggestions(result);
        return;
    }
}