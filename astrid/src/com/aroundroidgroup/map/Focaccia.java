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

public class Focaccia extends Activity {

    public static final int RESULT_CODE_DELETE = 1;
    public static final int RESULT_CODE_DELETE_ALL = 2;

    public static final String TASK_NAME = "taskName"; //$NON-NLS-1$
    public static final String SHOW_NAME = "name"; //$NON-NLS-1$
    public static final String SHOW_ADDRESS = "address"; //$NON-NLS-1$
    public static final String SHOW_TITLE = "title"; //$NON-NLS-1$
    public static final String SHOW_SNIPPET = "snippet"; //$NON-NLS-1$
    public static final String SHOW_AMOUNT_BY_EXTRAS = "amount"; //$NON-NLS-1$
    public static final String CMENU_EXTRAS = "contextMenuExtras"; //$NON-NLS-1$
    public static final String READ_ONLY = "read_only"; //$NON-NLS-1$
    public static final String DELETE = "delete"; //$NON-NLS-1$
    public static final String DELETE_ALL = "delete_all"; //$NON-NLS-1$
    public static final String NO_ADDRESS_WARNING = "warning"; //$NON-NLS-1$

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
        String header = bundle.getString(TASK_NAME);
        setTitle(header);

        String name = bundle.getString(SHOW_NAME);
        final String address = bundle.getString(SHOW_ADDRESS);
        final String snippet = bundle.getString(SHOW_SNIPPET);
        final String amount = bundle.getString(SHOW_AMOUNT_BY_EXTRAS);
        final String title = bundle.getString(SHOW_TITLE);
        final String delete = bundle.getString(DELETE);
        final String deleteAll = bundle.getString(DELETE_ALL);

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
                if (deleteAll != null)
                    dialog.setMessage("Are you sure you want to remove all locations ?"); //$NON-NLS-1$
                else dialog.setMessage("Are you sure you want to remove this location ?"); //$NON-NLS-1$

                /* setting the confirm button text and action to be executed if it has been chosen */
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", //$NON-NLS-1$
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dg, int which) {
                        if (deleteAll != null)
                            setResult(RESULT_CODE_DELETE_ALL);
                        else
                            setResult(RESULT_CODE_DELETE);
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
        if (delete != null || deleteAll != null) {
            removeButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.remove_overlay));
            removeButton.setOnClickListener(deleteButtonListener);
        }
        else removeButton.setVisibility(View.GONE);

        okButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.location_info));
        okButton.setOnClickListener(okButtonListener);
        if (name != null) {
            if (amount != null)
                name += " (" + amount + " results)";  //$NON-NLS-1$//$NON-NLS-2$
            mType.setText(Html.fromHtml("<b>" + mType.getText() + "</b> " + name)); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else mType.setVisibility(View.GONE);
        if (address != null) {
            if (!address.equals(NO_ADDRESS_WARNING))
                mAddress.setText(Html.fromHtml("<b>" + mAddress.getText() + "</b> " + address)); //$NON-NLS-1$ //$NON-NLS-2$
            else mAddress.setText(Html.fromHtml("<b>" + mAddress.getText() + "</b><font color='red'> No location found!</font>")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else mAddress.setVisibility(View.GONE);
        if (title != null)
            mName.setText(Html.fromHtml("<b>" + mName.getText() + "</b> " + title));  //$NON-NLS-1$//$NON-NLS-2$
        else mName.setVisibility(View.GONE);
        if (snippet != null)
            mSnippet.setText(Html.fromHtml("<b>" + mSnippet.getText() + "</b> " + snippet));  //$NON-NLS-1$//$NON-NLS-2$
        else mSnippet.setVisibility(View.GONE);
    }

}
