package de.acquisition;



import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import de.holoscope.R;

public class AcquireSettings extends DialogFragment {
    public static String TAG = "Settings Dialog";

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;
    private Button acqSettingsSuperresolution;
    private Button acqSettingsTimelapse;
    private Button acqSettingsSinglemode;
    private TextView acquireSettingsSetDatasetName;
    private TextView acquireSettingsMultiModeCountTextView;
    private TextView acquireSettingsSetNAEditText;
    private TextView acquireSettingsSetMultiModeDelayEditText;
    private CheckBox acquireSettingsHDRCheckbox;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public static interface OnCompleteListener {
        public abstract void onComplete(String time);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.acquire_settings_layout, null);



        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(content);
        // Add action buttons
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String mmCountValue = acquireSettingsMultiModeCountTextView.getText().toString();
                String naValue = acquireSettingsSetNAEditText.getText().toString();
                String mmDelayValue = acquireSettingsSetMultiModeDelayEditText.getText().toString();
                String datasetName = acquireSettingsSetDatasetName.getText().toString();
                Log.d(TAG, String.format("mmCount: %s", mmCountValue));
                Log.d(TAG,String.format("mmDelay: %s", mmDelayValue));
                Log.d(TAG,String.format("new na: %s", naValue));
                AcquireActivity callingActivity = (AcquireActivity) getActivity();
                callingActivity.setMultiModeCount(Integer.parseInt(mmCountValue));
                callingActivity.setMultiModeDelay(Integer.parseInt(mmDelayValue));
                callingActivity.setNA(Float.parseFloat(naValue));
                callingActivity.setDatasetFolder(datasetName);
                callingActivity.setHDR(acquireSettingsHDRCheckbox.isChecked());
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        AcquireActivity callingActivity = (AcquireActivity) getActivity();

        acquireSettingsMultiModeCountTextView = (TextView) content.findViewById(R.id.acquireSettingsMultiModeCountTextView);
        acquireSettingsMultiModeCountTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
        acquireSettingsMultiModeCountTextView.setText(String.format("%d", callingActivity.mmCount));

        acquireSettingsSetNAEditText = (TextView) content.findViewById(R.id.acquireSettingsSetNAEditText);
        acquireSettingsSetNAEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        //acquireSettingsSetNAEditText.setText(String.format("%.2f", callingActivity.brightfieldNA));

        acquireSettingsSetMultiModeDelayEditText = (TextView) content.findViewById(R.id.acquireSettingsSetMultiModeDelayEditText);
        acquireSettingsSetMultiModeDelayEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        acquireSettingsSetMultiModeDelayEditText.setText(String.format("%d", callingActivity.mmDelay));

        acquireSettingsSetDatasetName = (TextView) content.findViewById(R.id.acquireSettingsSetDatasetName);
        acquireSettingsSetDatasetName.setInputType(InputType.TYPE_CLASS_TEXT);
        acquireSettingsSetDatasetName.setText(callingActivity.datasetFolder);

        acquireSettingsHDRCheckbox = (CheckBox) content.findViewById(R.id.acquireSettingsHDRCheckbox);
        acquireSettingsHDRCheckbox.setChecked(callingActivity.usingHDR);

        acqSettingsTimelapse = (Button) content.findViewById(R.id.acqSettingsTimelapse);
        acqSettingsTimelapse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AcquireActivity callingActivity = (AcquireActivity) getActivity();
                callingActivity.setAcquireType("TimeLapse"); //Superresolution  SingleMode
            }
        });

        acqSettingsSuperresolution = (Button) content.findViewById(R.id.acqSettingsSuperresolution);
        acqSettingsSuperresolution.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AcquireActivity callingActivity = (AcquireActivity) getActivity();
                callingActivity.setAcquireType("Superresolution"); //  SingleMode
            }
        });
        acqSettingsSinglemode = (Button) content.findViewById(R.id.acqSettingsSinglemode); // CLEARS THE ARRAY FOR NOW
        acqSettingsSinglemode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AcquireActivity callingActivity = (AcquireActivity) getActivity();
                callingActivity.setAcquireType("SingleMode"); //
            }
        });

        return builder.create();
    }


}