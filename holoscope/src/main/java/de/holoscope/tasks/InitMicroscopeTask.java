package de.holoscope.tasks;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import de.holoscope.R;
import de.holoscope.activity.GlobalApplicationClass;
import de.holoscope.activity.MainActivity;
import de.holoscope.bluetooth.BluetoothService;
import de.holoscope.datasets.Dataset;


//import at.abraxas.amarino.Amarino;

/**
 * Created by Bene on 18.04.2015.
 */
public class InitMicroscopeTask extends MainActivity {

    private static final String TAG = "Init";
    private static final boolean D = true;
    private BluetoothService mBluetoothService = null;

    private TextView acquireTextView;




    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        progress = new ProgressDialog(this);

        GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();




        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        mBluetoothService = BTAppClass.getBluetoothService();
        if (mBluetoothService != null)
            acquireTextView.setText("Connected");
        else
            acquireTextView.setText("Not Connected");
        acquireTextView.setTextColor(Color.YELLOW);

    }


    public void sendINIT(View view) {
        //meetAndroid.registerFunction(initMicroscope, 'a');
        //Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'a', true);

        if (mBluetoothService != null) {
            progress.setMessage("Initialize Microscope. ");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();

            //sendData("iN," + String.valueOf(intZpos) + "_");
            sendData(String.format("init,%d", (int) -1));   //Signal for initilization

            final int totalProgressTime = 200;

            final Thread t = new Thread() {

                @Override
                public void run() {

                    int jumpTime = 0;
                    while (jumpTime < totalProgressTime) {
                        try {
                            sleep(2000);
                            jumpTime += 20;
                            Log.e("Jumptime", String.valueOf(jumpTime));
                            progress.setProgress(jumpTime);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                    progress.cancel();
                    sendData(String.format("init,%d", (int) 0));

                }
            };
            t.start();


        }
    }


    public void sendZPos(View view) {
        //meetAndroid.registerFunction(setZPosition, 'c');
        EditText editZpos = (EditText) findViewById(R.id.setZPos);

        if (mBluetoothService != null) {

            try {
                int intZpos = Integer.valueOf(editZpos.getText().toString());
                if (intZpos > 10000 || intZpos < -10000) intZpos = 0;
                sendData("zP,"+String.valueOf(intZpos)+"_");   //Signal for initilization

                //Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'c', intZpos);
            } catch (NumberFormatException e) {
                //Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'c', 0);
            }
            progress.setMessage("send Z-Position");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();

            final int totalProgressTime = 5;

            final Thread t = new Thread() {

                @Override
                public void run() {

                    try {
                        sleep(200);
                        progress.setProgress(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    progress.cancel();

                }
            };
            t.start();

        }
    }


    public void sendLED(View view) {
        //meetAndroid.registerFunction(setLED, 'b');

        if (mBluetoothService != null) {

            EditText editLED = (EditText) findViewById(R.id.setLEDNumber);

            try {
                int intLED = Integer.valueOf(editLED.getText().toString());
                if (intLED > Dataset.LEDVAL || intLED < 0) intLED = 0;

                sendData("nL,"+String.valueOf(intLED)+"_");   //Signal for initilization
            } catch (NumberFormatException e) {
                //Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'b', intLED);
            }


            progress.setMessage("send LED Value");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();


            final Thread t = new Thread() {

                @Override
                public void run() {

                    try {
                        sleep(200);
                        progress.setProgress(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    progress.cancel();

                }
            };
            t.start();


        }
    }

    public void onDestroy() {
        super.onDestroy();
        //Amarino.disconnect(this, DatasetHologram.DEVICE_ADDRESS);
    }

/*
    //meetAndroid.registerFunction(initMicroscope, 'a');
    //meetAndroid.registerFunction(setLED, 'b');
    //meetAndroid.registerFunction(setZPosition, 'c');
*/


}
