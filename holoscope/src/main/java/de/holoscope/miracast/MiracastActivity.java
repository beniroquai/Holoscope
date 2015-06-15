package de.holoscope.miracast;


import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.util.SparseArray;
import android.view.Display;
import android.widget.TextView;

import de.holoscope.R;

public class MiracastActivity extends Activity {

    private DisplayManager mDisplayManager;

    private final SparseArray<RemotePresentation> mActivePresentations = new SparseArray<RemotePresentation>();


    private Display current=null;
    private boolean isFirstRun=true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.miracast_local_display);
        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);

        Display[] displays= mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (displays.length != 0) {

            if (current != null || isFirstRun) {
                isFirstRun = false;
                current=displays[0];
            }

            if(current != null){
                showPresentation(current, 1);
            }
        }

    }

    protected void onResume() {
        super.onResume();

        //mDisplayManager.registerDisplayListener(this, null);
    }

    private void showPresentation(Display display, int i) {
        RemotePresentation presentation = new RemotePresentation(this, display);
presentation.number = 12;
        mActivePresentations.put(display.getDisplayId(), presentation);
        presentation.show();
    }

    private void hidePresentation(Display display) {
        final int displayId = display.getDisplayId();
        RemotePresentation presentation = mActivePresentations.get(displayId);
        if (presentation == null) {
            return;
        }

        presentation.dismiss();
        mActivePresentations.delete(displayId);
    }


    private final class RemotePresentation extends Presentation {
        public RemotePresentation(Context context, Display display) {
            super(context, display);
        }

        int number = 0;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.miracast_remote_display);

            TextView textNumber;
            //textNumber = (TextView)findViewById(R.id.textnumber);
            //textNumber.setText("Current Number is: " + String.valueOf(number));

        }
    }
}
