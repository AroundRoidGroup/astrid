package com.aroundroidgroup.map;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.SpecificMapLocation;

public class Focaccia extends Activity {

    private String[] resources;
    private String addressText = null;
    private boolean neededToReverseGecode = false;
    public static final String SOURCE_ADJUSTEDMAP = "AdjustedMap"; //$NON-NLS-1$
    public static final String SOURCE_SPECIFICMAP = "SpecificMap"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.focaccia_activity);

        /* getting the data that was sent from the SpecificLocationMap activity  */
        /* this data contains the tapped overlay's index as the first element    */
        /* and the coordinates that the tapped overlay represent                 */
        Bundle bundle = getIntent().getExtras();
        resources = bundle.getStringArray(SOURCE_ADJUSTEDMAP);
        if (resources == null)
            resources = bundle.getStringArray(SOURCE_SPECIFICMAP);
        TextView tv = (TextView)findViewById(R.id.locationType);
        tv.setText(resources[2]);

        TextView addressTV = (TextView)findViewById(R.id.locationAddress);

        try {
            /* getting the address by the coordinates only if the location never been reversed gecoded */
            if (resources[4] == null) {
                neededToReverseGecode = true;
                addressText = Geocoding.reverseGeocoding(new DPoint(resources[1]));
            }
            else addressText = resources[4];
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* if gecoding process succeeded, the address is shown. otherwise the    */
        /* coordinates are shown                                                 */
        if (addressText != null)
            addressTV.setText(addressText);
        else addressTV.setText(resources[1]);

        ImageButton removeButton = (ImageButton)findViewById(R.id.removeOverlay);
        if (resources[5].equals("0") == true) { //$NON-NLS-1$
            removeButton.setClickable(false);
            removeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.location_info));
        }
        if (resources[5].equals("1") == true) { //$NON-NLS-1$
            removeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.remove_overlay));
            removeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /* popping up a dialog so the user could confirm the remove */
                AlertDialog dialog = new AlertDialog.Builder(Focaccia.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);

                /* setting the dialog title */
                dialog.setTitle("Confirm Remove Location"); //$NON-NLS-1$

                /* setting the dialog content message */
                dialog.setMessage("Are you sure you want to remove this location ?"); //$NON-NLS-1$

                /* setting the confirm button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        Intent intent = new Intent();
                        intent.putExtra(SOURCE_ADJUSTEDMAP, resources[0]);
                        setResult(SpecificMapLocation.FOCACCIA_RESULT_CODE, intent);
                        Focaccia.this.finish();
                    }
                });

                /* setting the refuse button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        return;
                    }
                });
                dialog.show();
            }
        });

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Toast.makeText(this, "press back", Toast.LENGTH_LONG).show(); //$NON-NLS-1$
            String[] dataToSend = new String[2];
            dataToSend[0] = resources[0];
            if (neededToReverseGecode)
                dataToSend[1] = addressText;
            else dataToSend[1] = null;
            Intent intent = new Intent();
            intent.putExtra(SOURCE_ADJUSTEDMAP, dataToSend);
            setResult(SpecificMapLocation.FOCACCIA_RESULT_CODE_BACK_PRESSED, intent);
            Focaccia.this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
