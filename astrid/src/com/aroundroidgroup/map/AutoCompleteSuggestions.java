package com.aroundroidgroup.map;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;

public class AutoCompleteSuggestions extends ListActivity {

    private static ArrayAdapter<String> mAdapter;
    private EditText mSearchBox;
    private Button mAdd;
    private String mCenter = null;
    private double mRadius = 0.0;

    public static final String AUTOCOMPLETE_CENTER = "center"; //$NON-NLS-1$
    public static final String AUTOCOMPLETE_RADIUS = "radius"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mCenter = bundle.getString(AUTOCOMPLETE_CENTER);
            mRadius = bundle.getDouble(AUTOCOMPLETE_RADIUS);
        }
        else {
            setResult(RESULT_CANCELED);
            finish();
        }
        View header = getLayoutInflater().inflate(R.layout.autocomplete_header, null);
        ListView listView = getListView();
        listView.addHeaderView(header);

        mSearchBox = (EditText) findViewById(R.id.searchbox);

        setListAdapter(new ArrayAdapter<String>(this, R.layout.autocomplete_row, new String[] { "chicken" }));

        ListView lv = getListView();
        lv.setTextFilterEnabled(true);

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                        Toast.LENGTH_SHORT).show();
            }});

        mSearchBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> c = null;
                try {
                    String searchText = mSearchBox.getText().toString();
                    DPoint center = new DPoint(mCenter);
                    c = Misc.googleAutoCompleteQuery(searchText, center, mRadius);
                    for (String type : Misc.types)
                        c.add(type);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (c != null) {
                    mAdapter = new ArrayAdapter<String>(AutoCompleteSuggestions.this, R.layout.autocomplete_row, c);
                    mAdapter.sort(new Comparator<String>() {

                        @Override
                        public int compare(String object1, String object2) {
                            boolean firstObj = false;
                            boolean secondObj = false;
                            for (String type : Misc.types) {
                                if (type.equals(object1))
                                    firstObj = true;
                                if (type.equals(object2))
                                    secondObj = true;
                            }
                            if (firstObj && !secondObj)
                                return -1;
                            if (!firstObj && secondObj)
                                return 1;
                            return 0;
                        }
                    });
                    setListAdapter(mAdapter);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                return;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub
                return;
            }
        });

        mSearchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    mSearchBox.setText(""); //$NON-NLS-1$
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                // only will trigger it if no physical keyboard is open
                mgr.showSoftInput(mSearchBox, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        return super.onOptionsItemSelected(item);
    }

}
