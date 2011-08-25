package com.aroundroidgroup.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.activity.SpecificMapLocation;

public class Focaccia extends Activity {

    public static final int DELETE = 1;
    public static final int DELETE_ALL = 2;

    private TextView mType;
    private TextView mSnippet;
    private TextView mName;
    private TextView mAddress;
    private ImageButton okButton;
    private ImageButton removeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.focaccia_activity);

        /* getting the data that was sent from the SpecificLocationMap activity  */
        /* or AdjustedMap class */
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) /* no extras implies bad access to this class, hence quitting */
            return;

        /* setting the title of the activity to be the task name */
        String tNameSML = bundle.getString(SpecificMapLocation.TASK_NAME);
        String tNameAM = bundle.getString(AdjustedMap.TASK_NAME);
        setTitle((tNameSML == null) ? tNameAM : tNameSML);

        String nameAM = bundle.getString(AdjustedMap.SHOW_NAME);
        final String addressAM = bundle.getString(AdjustedMap.SHOW_ADDRESS);
        final String snippetAM = bundle.getString(AdjustedMap.SHOW_SNIPPET);
        final String amountAM = bundle.getString(AdjustedMap.SHOW_AMOUNT_BY_EXTRAS);
        final String titleAM = bundle.getString(AdjustedMap.SHOW_TITLE);
        final String deleteAM = bundle.getString(AdjustedMap.DELETE);
        final String noDeleteAM = bundle.getString(AdjustedMap.READ_ONLY);

        final String extrasMEL = bundle.getString(SpecificMapLocation.CMENU_EXTRAS);
        String nameMEL = bundle.getString(SpecificMapLocation.SHOW_NAME);
        final String addressMEL = bundle.getString(SpecificMapLocation.SHOW_ADDRESS);
        final String snippetMEL = bundle.getString(SpecificMapLocation.SHOW_SNIPPET);
        final String amountMEL = bundle.getString(SpecificMapLocation.SHOW_AMOUNT_BY_EXTRAS);
        final String titleMEL = bundle.getString(SpecificMapLocation.SHOW_TITLE);
        final String deleteMEL = bundle.getString(SpecificMapLocation.DELETE);
        final String deleteAllMEL = bundle.getString(SpecificMapLocation.DELETE_ALL);
        final String noDeleteMEL = bundle.getString(SpecificMapLocation.READ_ONLY);

        mType = (TextView)findViewById(R.id.locationType);
        mSnippet = (TextView)findViewById(R.id.locationSnippet);
        mAddress = (TextView)findViewById(R.id.locationAddress);
        mName = (TextView)findViewById(R.id.locationName);
        okButton = (ImageButton)findViewById(R.id.iOK);
        removeButton = (ImageButton)findViewById(R.id.iRemove);

        OnClickListener okButtonListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        };

        OnClickListener deleteButtonListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                /* popping up a dialog so the user could confirm the remove */
                AlertDialog dialog = new AlertDialog.Builder(Focaccia.this).create();
                dialog.setIcon(android.R.drawable.ic_dialog_alert);

                /* setting the dialog title */
                dialog.setTitle("Confirm Remove"); //$NON-NLS-1$

                /* setting the dialog content message */
                if (deleteAllMEL != null)
                    dialog.setMessage("Are you sure you want to remove all locations ?"); //$NON-NLS-1$
                else dialog.setMessage("Are you sure you want to remove this location ?"); //$NON-NLS-1$

                /* setting the confirm button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        if (deleteAllMEL != null)
                            setResult(DELETE_ALL);
                        else
                            setResult(DELETE);
                        finish();
                    }
                });

                /* setting the refuse button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        setResult(RESULT_CANCELED);
                        finish();
                        return;
                    }
                });
                dialog.show();
            }
        };

        /* adding the functionality to the removeButton because DELETE option is enabled */
        if (deleteAM != null || deleteMEL != null || deleteAllMEL != null) {
            removeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.remove_overlay));
            removeButton.setOnClickListener(deleteButtonListener);
        }
        else removeButton.setVisibility(View.GONE);

        okButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.location_info));
        okButton.setOnClickListener(okButtonListener);
        if (nameAM != null || nameMEL != null) {
            if (amountAM != null)
                nameAM += " (" + amountAM + " results)";  //$NON-NLS-1$//$NON-NLS-2$
            if (amountMEL != null)
                nameMEL += " (" + amountMEL + " results)";  //$NON-NLS-1$//$NON-NLS-2$
            mType.setText(Html.fromHtml("<b>" + mType.getText() + "</b> " + ((nameAM != null) ? nameAM : nameMEL))); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else mType.setVisibility(View.GONE);
        if (addressAM != null || addressMEL != null)
            mAddress.setText(Html.fromHtml("<b>" + mAddress.getText() + "</b> " + ((addressAM != null) ? addressAM : addressMEL))); //$NON-NLS-1$ //$NON-NLS-2$
        else mAddress.setVisibility(View.GONE);
        if (titleAM != null || titleMEL != null)
            mName.setText(Html.fromHtml("<b>" + mName.getText() + "</b> " + ((titleAM != null) ? titleAM : titleMEL)));  //$NON-NLS-1$//$NON-NLS-2$
        else mName.setVisibility(View.GONE);
        if (snippetAM != null || snippetMEL != null)
            mSnippet.setText(Html.fromHtml("<b>" + mSnippet.getText() + "</b> " + ((snippetAM != null) ? snippetAM : snippetMEL)));  //$NON-NLS-1$//$NON-NLS-2$
        else mSnippet.setVisibility(View.GONE);
    }

}
