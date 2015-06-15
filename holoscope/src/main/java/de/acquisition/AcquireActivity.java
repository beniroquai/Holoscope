package de.acquisition;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.preso.PresentationFragment;
import com.commonsware.cwac.preso.PresentationHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.holoscope.R;
import de.holoscope.activity.GlobalApplicationClass;
import de.holoscope.bluetooth.BluetoothService;
import de.holoscope.datasets.Dataset;
import de.presentation.PresentationActivity;
import de.presentation.SamplePresentationFragment;
import de.presentation.SlideshowService;

/**
 * Created by Bene on 26.05.2015.
 */
public class AcquireActivity extends Activity implements View.OnTouchListener, AcquireSettings.NoticeDialogListener {

    //Some Varialbes for Bluetooth and GUI
    private static final String TAG = "cCS_Acquire";
    private static final boolean D = true;
    private Camera mCamera;
    private AcquireSurfaceView mPreview;
    private BluetoothService mBluetoothService = null;

    private String acquireType = "SingleMode";
    Button btnSetup, btnAcquire;
    private TextView acquireTextView;
    private TextView acquireTextView2;
    private TextView timeLeftTextView;
    private ProgressBar acquireProgressBar;

    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    public String fileName = "default";
    public boolean cameraReady = true;
    public int mmCount = 2; // Number of acquisitions in Multimode
    public int mmDelay = 1;
    public String datasetFolder = "Dataset/";
    public boolean usingHDR = false;
    public Dataset mDataset;

    Handler handler;

    int[] activateLED = new int[Dataset.LEDVAL];

    public DialogFragment settingsDialogFragment;

    //Initiate Presentation
    private DisplayManager mDisplayManager;
    private final SparseArray<RemotePresentation> mActivePresentations = new SparseArray<RemotePresentation>();
    private Display current=null;
    private boolean isFirstRun=true;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acquire);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
        mBluetoothService = BTAppClass.getBluetoothService();

        //Some Presentation stuff - Connect to external Display
        handler=new Handler(Looper.getMainLooper());
        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);

        Display[] displays= mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (displays.length != 0) {

            //Setup external Display as default display for Pinholes
            if (current != null || isFirstRun) {
                isFirstRun = false;
                current=displays[0];
            }
        }
        showPresentation(current, 0);


        Log.i("Acquire Type", acquireType);


        //Set Text and Button in UI
        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        acquireTextView2 = (TextView) findViewById(R.id.acquireStatusTextView2);
        timeLeftTextView = (TextView) findViewById(R.id.timeLeftTextView);

        acquireProgressBar = (ProgressBar) findViewById(R.id.acquireProgressBar);
        acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up

        btnSetup = (Button) findViewById(R.id.btnSetup);
        btnAcquire = (Button) findViewById(R.id.btnSaveFrame);

        //Setup Colours
        acquireTextView.setTextColor(Color.GREEN);
        acquireTextView2.setTextColor(Color.GREEN);
        timeLeftTextView.setTextColor(Color.GREEN);
        btnSetup.setTextColor(Color.parseColor("GREEN")); //SET CUSTOM COLOR
        btnAcquire.setTextColor(Color.parseColor("GREEN")); //SET CUSTOM COLOR

        getActionBar().setDisplayHomeAsUpEnabled(true);

        //Generate Dataset
        mDataset = BTAppClass.getDataset();

        if (mBluetoothService != null)
            acquireTextView.setText(String.format("MODE: %s, ARRAY: Connected", acquireType));
        else
            acquireTextView.setText(String.format("MODE: %s ARRAY: Not Connected", acquireType));


        settingsDialogFragment = new AcquireSettings();

        btnAcquire.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if (mBluetoothService != null){
                //if (mBluetoothService == null){ //TODO change to !=mull

                if (acquireType.equals("SingleMode")) {//THis Mode will only acquire one picture at given Z and LED - VAl
                    //TODO uebergebe LED Nummer von Auswahl, wenn mehr als eins, dann default mitte
                    //int iLED = getLED();
                    int iLED = 5;
                    new runSingleMode().execute();

               } else if (acquireType.equals("Superresolution")) {
                    //TODO Run Singlemode with all LEDs
                    Log.i("State", "Superresolution");
                    int[] iLEDarray = {0, 1, 2, 3, 4, 5, 6, 7, 8};
                    //iLEDarray = getLEDarray(); //TODO take care of this
                    new runSuperresolution().execute();
                } else if (acquireType.equals("TimeLapse")) {
                    Log.i("State", "Timelapse");
                    new runTimelapse().execute();

                }
            }
        });


        btnSetup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSettingsDialog();
            }
        });


        // Get the instance of Camera
        mCamera = getCameraInstance();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // access camera Parameters
        final Camera.Parameters camParams = mCamera.getParameters();

        //set color effects to none
        camParams.setColorEffect(Camera.Parameters.EFFECT_MONO); //TODO NO_EFFECT? Is deprecated since illumination is monochromatic

        //set antibanding to none
        if (camParams.getAntibanding() != null) {
            camParams.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
        }

        // set white balance
        if (camParams.getWhiteBalance() != null) {
            camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        // set images to maximum resolution
        List<Camera.Size> sizes = camParams.getSupportedPictureSizes();
        Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {

            if ((int)sizes.get(i).width < 4095) //Handle Texture maximum

                size = sizes.get(i);
            String msg = String.format("%dh,%dw",sizes.get(i).height,sizes.get(i).width);
            Log.i(TAG, msg);
            Log.i("Size", String.valueOf(sizes.get(i).width));

        }

        size = sizes.get(3); //TODO Hardcoded!
        camParams.setPictureSize(size.width, size.height);
        acquireTextView2.setText(String.format("%dx%d", size.width,size.height));

        // Turn on AEC
        camParams.setAutoExposureLock(false);

        // Set parameters
        mCamera.setParameters(camParams);

        // Get camera ID of rear camera
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.canDisableShutterSound) {
            mCamera.enableShutterSound(false);
        }

        // Callbacks for camera acquires
        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                //Log.i("Log", "onShutter'd");
            }
        };

        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera)
            {
                FileOutputStream outStream = null;
                //Log.d(TAG, "onPictureTaken - jpeg");
                String path = fileName + ".jpeg";

                try {

                    outStream = new FileOutputStream(String.format(path));
                    outStream.write(data);
                    outStream.close();
                    Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to path: " + path);
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "onPictureTaken - jpeg - directory not found");
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onPictureTaken - jpeg - IO Exception");
                } finally {
                }
                camera.startPreview();
                cameraReady = true;
                // Add file to the mediaStore
                //MediaScannerConnection.scanFile(AcquireActivity.this,new String[] { path }, null,null);
            }
        };

        // Create our Preview view and set it as the content of our activity.
        mPreview = new AcquireSurfaceView(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_view);
        preview.addView(mPreview);

        mPreview.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                final Camera.Parameters myParams = mCamera.getParameters();
                myParams.setAutoExposureLock(true);
                myParams.setExposureCompensation(0);
                mCamera.autoFocus(null);
                mCamera.setParameters(myParams);
                return false;
            }
        });
        // Set the NA of the objective on the arduino
        sendData(String.format("na,%d", (int) Math.round(1*100)));  //TODO Take care of right numbers!

        // turn on center LED to start
        String cmd = String.format("p%d", 1);       //TODO take care of right command
        sendData(cmd);

    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



    private void showPresentation(Display display, int i) {

        if(display!=null){

            RemotePresentation presentation = new RemotePresentation(this, display);
            presentation.number = i;
            mActivePresentations.put(display.getDisplayId(), presentation);
            presentation.show();

        }
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




      class RemotePresentation extends Presentation {
        public RemotePresentation(Context context, Display display) {
            super(context, display);
        }

          private ImageView image;
          int number = 0;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.miracast_remote_display);

            //getActionBar().hide();

            int[] SLIDES= { R.drawable.img0,
                    R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4
                   };

            int imageResource = R.drawable.img0;
            try{
                Drawable image1 = getResources().getDrawable(SLIDES[number]);
                image = (ImageView) findViewById(R.id.imageView1);
                image.setImageDrawable(image1);//SLIDES[number] % mmCount);

            }
            catch(ArrayIndexOutOfBoundsException e){

            }



        }
    }

    private class runSuperresolution extends AsyncTask<Void, Void, Void>
    {

        long t = 0;
        int i = 0;
        Camera.Parameters camParams;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new Date());
        String save_path = "SuperresolutionRAW/"+"Superresolution_"+timestamp;
        String path = Dataset.getImageFolder(save_path); //Will be /sdcard/Holoscope/Singlemode/Singlemode_XXYYYZZ

        File myDir = new File(path);

        //params.setExposureCompensation(params.getMinExposureCompensation());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showPresentation(current, 0);

            timeLeftTextView.setText("Time left:");
            acquireTextView.setText(String.format("Acquiring - MODE: %s", acquireType));
            acquireProgressBar.setVisibility(View.VISIBLE); // Make invisible at first, then have it pop up
            acquireProgressBar.setMax(Dataset.LEDVAL);

            camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(false);
            mCamera.setParameters(camParams);

            sendData("db"); // TODO INIT COMMAND?
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Log.d(TAG,String.format("!!!AEC Compensation: %d max, %d min", camParams.getMaxExposureCompensation(), camParams.getMinExposureCompensation()));
            //Log.d(TAG,mCamera.getParameters().flatten());
            //params.setExposureCompensation(params.getMinExposureCompensation());
            camParams.setAutoExposureLock(true);
            //params.setExposureCompensation(params.getMinExposureCompensation());
            mCamera.setParameters(camParams);
            myDir.mkdirs();
            Log.i("MKDir", path);




        }

        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(i+1);
            long elapsed = SystemClock.elapsedRealtime() - t;
            t = SystemClock.elapsedRealtime();
            float timeLeft = (float)(((long)(mmCount - i)*elapsed)/1000.0);
            timeLeftTextView.setText(String.format("Time left: %.2f seconds, %d/%d images saved", timeLeft, i+1, Dataset.LEDVAL));
            //Log.d(TAG, String.format("Time left: %.2f seconds", timeLeft));
            showPresentation(current, i);
        }

        void mSleep(int sleepVal)
        {
            try {
                Thread.sleep(sleepVal);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) { //todo cHANGE the correct sending commands
            //Looper.prepare();

            sendData("xx"); // Clear the array first
            // Wait for the data to propigate down the chain

            t = SystemClock.elapsedRealtime();
            long startTime = SystemClock.elapsedRealtime();

            mSleep(100);
            //textNumber.setText("Current Number is: " + String.valueOf(number));

            for (i = 0; i < Dataset.LEDVAL; i++) // one count i per cycle
            {


                if (usingHDR)
                {
                    camParams.setSceneMode("hdr");
                    mCamera.setParameters(camParams);
                }

                publishProgress();
                mSleep(1000);
                // Acquire Picture
                sendData("dl"); //TODO Change COMMAND
                cameraReady = false;
                captureImage(Dataset.getImageFolder(save_path)+ String.format("timelapse_%d_",  1) + String.format("%3d", SystemClock.elapsedRealtime()));
                //captureImage(Dataset.getImageFolder(save_path));
                while(!cameraReady)
                {
                    mSleep(1);
                }

                // Undo HDR and make sure AEC is locked
                if(usingHDR)
                {
                    camParams.setSceneMode("auto");
                    camParams.setAutoExposureLock(true);
                    mCamera.setParameters(camParams);
                }

                // User-defined delay between captures for a time-series
                if (mmDelay != 0)
                {
                    camParams.setSceneMode("auto");
                    camParams.setAutoExposureLock(true);
                    mCamera.setParameters(camParams);
                    sendData("x"); // Clear the Array

                    //Log.d(TAG,String.format("Sleeping for %d ms", (int)(Math.round(mmDelay*1000f))));
                    mSleep((int)(Math.round(mmDelay*1000f)));
                }
                publishProgress();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up

            //String cmd = String.format("p%d", centerLED);
            String cmd = "bf";
            sendData(cmd);
            timeLeftTextView.setText(" ");

            showPresentation(current, 0);

            Camera.Parameters params = mCamera.getParameters();
            params.setAutoExposureLock(false);
            mCamera.setParameters(params);
            updateFileStructure(myDir.getAbsolutePath());
            mDataset.DATASET_PATH_TEMP = Environment.getExternalStorageDirectory()+path;
            mDataset.DATA_PATH_Temp = acquireType;
        }
    }


    private class runSingleMode extends AsyncTask<Void, Void, Void>
    {
        int n = 0;
        long t = 0;

        Camera.Parameters camParams;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new Date());
        String save_path = "Singlemode/"+"Singlemode_"+timestamp;
        String path = Dataset.getImageFolder(save_path); //Will be /sdcard/Holoscope/Singlemode/Singlemode_XXYYYZZ

        File myDir = new File(path);
        //params.setExposureCompensation(params.getMinExposureCompensation());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            timeLeftTextView.setText("Time left:");
            acquireTextView.setText(String.format("Acquiring - MODE: %s", acquireType));
            acquireProgressBar.setVisibility(View.VISIBLE); // Make invisible at first, then have it pop up
            acquireProgressBar.setMax(5 * mmCount);

            camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(false);
            mCamera.setParameters(camParams);



            sendData("db"); // TODO INIT COMMAND?
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Log.d(TAG,String.format("!!!AEC Compensation: %d max, %d min", camParams.getMaxExposureCompensation(), camParams.getMinExposureCompensation()));
            //Log.d(TAG,mCamera.getParameters().flatten());
            //params.setExposureCompensation(params.getMinExposureCompensation());
            camParams.setAutoExposureLock(true);
            //params.setExposureCompensation(params.getMinExposureCompensation());
            mCamera.setParameters(camParams);
            myDir.mkdirs();
            Log.i("MKDir",path );
        }

        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(n);
        }

        void mSleep(int sleepVal)
        {
            try {
                Thread.sleep(sleepVal);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) { //todo cHANGE the correct sending commands

            sendData("xx"); // Clear the array first
            // Wait for the data to propigate down the chain
            //TODO change Send Data!
            n=0;


                // Top
                sendData("dt"); // TODO send LED number dfault 5



            mSleep(200); //Let AEC stabalize if it's on
                if (usingHDR)
                {
                    camParams.setSceneMode("hdr");
                    mCamera.setParameters(camParams);
                }
                cameraReady = false;
                //captureImage(Dataset.getImageFolder(save_path, "Singlemode"+SystemClock.elapsedRealtime());
                captureImage(Dataset.getImageFolder(save_path) + String.format("single_%d_", 1) + String.format("%3d", SystemClock.elapsedRealtime()));
            //captureImage(path + String.format("top_%d_", i + 1) + String.format("%3d", SystemClock.elapsedRealtime()));
                while(!cameraReady)
                {
                    mSleep(1);
                }
                n++;
                publishProgress();





                // Undo HDR and make sure AEC is locked
                if(usingHDR)
                {
                    camParams.setSceneMode("auto");
                    camParams.setAutoExposureLock(true);
                    mCamera.setParameters(camParams);
                }



            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up

            //String cmd = String.format("p%d", centerLED);
            String cmd = "bf";      //TODO change command!
            sendData(cmd);
            timeLeftTextView.setText(" ");

            Camera.Parameters params = mCamera.getParameters();
            params.setAutoExposureLock(false);
            mCamera.setParameters(params);
            updateFileStructure(myDir.getAbsolutePath());
            mDataset.DATASET_PATH_TEMP = Environment.getExternalStorageDirectory()+path;
            mDataset.DATA_PATH_Temp = acquireType;



        }
    }




    private class runTimelapse extends AsyncTask<Void, Void, Void>
    {

        long t = 0;
        int i = 0;
        Camera.Parameters camParams;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(new Date());
        String save_path = "Timelapse/"+"Timelapse_"+timestamp;
        String path = Dataset.getImageFolder(save_path); //Will be /sdcard/Holoscope/Singlemode/Singlemode_XXYYYZZ

        File myDir = new File(path);

        //params.setExposureCompensation(params.getMinExposureCompensation());

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showPresentation(current, 0);

            timeLeftTextView.setText("Time left:");
            acquireTextView.setText(String.format("Acquiring - MODE: %s", acquireType));
            acquireProgressBar.setVisibility(View.VISIBLE); // Make invisible at first, then have it pop up
            acquireProgressBar.setMax(mmCount);

            camParams = mCamera.getParameters();
            camParams.setAutoExposureLock(false);
            mCamera.setParameters(camParams);

            sendData("db"); // TODO INIT COMMAND?
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //Log.d(TAG,String.format("!!!AEC Compensation: %d max, %d min", camParams.getMaxExposureCompensation(), camParams.getMinExposureCompensation()));
            //Log.d(TAG,mCamera.getParameters().flatten());
            //params.setExposureCompensation(params.getMinExposureCompensation());
            camParams.setAutoExposureLock(true);
            //params.setExposureCompensation(params.getMinExposureCompensation());
            mCamera.setParameters(camParams);
            myDir.mkdirs();
            Log.i("MKDir", path);




        }

        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(i);
            long elapsed = SystemClock.elapsedRealtime() - t;
            t = SystemClock.elapsedRealtime();
            float timeLeft = (float)(((long)(mmCount - i)*elapsed)/1000.0);
            timeLeftTextView.setText(String.format("Time left: %.2f seconds, %d/%d images saved", timeLeft, i, mmCount));
            //Log.d(TAG, String.format("Time left: %.2f seconds", timeLeft));
            //showPresentation(current, 0);
        }

        void mSleep(int sleepVal)
        {
            try {
                Thread.sleep(sleepVal);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) { //todo cHANGE the correct sending commands
            //Looper.prepare();

            sendData("xx"); // Clear the array first
            // Wait for the data to propigate down the chain

            t = SystemClock.elapsedRealtime();
            long startTime = SystemClock.elapsedRealtime();

            mSleep(100);
            //textNumber.setText("Current Number is: " + String.valueOf(number));

            for (i = 0; i < mmCount; i++) // one count i per cycle
            {
                if (usingHDR)
                {
                    camParams.setSceneMode("hdr");
                    mCamera.setParameters(camParams);
                }

                // Acquire Picture
                sendData("dl"); //TODO Change COMMAND
                cameraReady = false;
                captureImage(Dataset.getImageFolder(save_path)+ String.format("timelapse_%d_",  1) + String.format("%3d", SystemClock.elapsedRealtime()));
                //captureImage(Dataset.getImageFolder(save_path));
                while(!cameraReady)
                {
                    mSleep(1);
                }

                publishProgress();


                // Undo HDR and make sure AEC is locked
                if(usingHDR)
                {
                    camParams.setSceneMode("auto");
                    camParams.setAutoExposureLock(true);
                    mCamera.setParameters(camParams);
                }

                // User-defined delay between captures for a time-series
                if (mmDelay != 0)
                {
                    camParams.setSceneMode("auto");
                    camParams.setAutoExposureLock(true);
                    mCamera.setParameters(camParams);
                    sendData("x"); // Clear the Array

                    //Log.d(TAG,String.format("Sleeping for %d ms", (int)(Math.round(mmDelay*1000f))));
                    mSleep((int)(Math.round(mmDelay*1000f)));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            acquireProgressBar.setVisibility(View.INVISIBLE); // Make invisible at first, then have it pop up

            //String cmd = String.format("p%d", centerLED);
            String cmd = "bf";
            sendData(cmd);
            timeLeftTextView.setText(" ");

            hidePresentation(current);

            Camera.Parameters params = mCamera.getParameters();
            params.setAutoExposureLock(false);
            mCamera.setParameters(params);
            updateFileStructure(myDir.getAbsolutePath());
            mDataset.DATASET_PATH_TEMP = Environment.getExternalStorageDirectory()+path;
            mDataset.DATA_PATH_Temp = acquireType;
        }
    }


    public void captureImage(String fileHeader)
    {
        fileName = fileHeader;
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public void sendData(String message) {
        //message = message + "\n";
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
//            Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            if (D) Log.d(TAG, message);
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }


    }




//TODO integrate gallery
    //fire intent to start activity with proper configuration for acquire type
    protected void startGalleryActivity() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/CellScope/20140815_163448496/"), "image/*");
        startActivity(intent);
    }

    public void updateFileStructure(String currPath) {
        File f = new File(currPath);
        File[] fileList = f.listFiles();
        ArrayList<String> arrayFiles = new ArrayList<String>();
        if (!(fileList.length == 0))
        {
            for (int i=0; i<fileList.length; i++)
                arrayFiles.add(currPath+"/"+fileList[i].getName());
        }

        String[] fileListString = new String[arrayFiles.size()];
        fileListString = arrayFiles.toArray(fileListString);
        MediaScannerConnection.scanFile(AcquireActivity.this,
                fileListString, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        //Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    public void openSettingsDialog()
    {
        settingsDialogFragment.show(getFragmentManager(), "acquireSettings");

    }
    public void setMultiModeCount(int count)
    {
        mmCount = count;
    }

    public void setNA(float na)
    {
        //brightfieldNA = na;
        sendData(String.format("na,%d", (int) Math.round(na * 100)));
    }

    public void setMultiModeDelay(int delay)
    {
        mmDelay = delay;
    }

    public void setDatasetFolder(String name)
    {
        datasetFolder = name;
    }

    public void setAcquireType(String aType)
    {
        acquireType = aType;
        sendData("bf");
        Camera.Parameters camParams = mCamera.getParameters();
        camParams.setAutoExposureLock(false);
        mCamera.setParameters(camParams);
        Log.i("Acqtype", acquireType);
    }



    public void setHDR(boolean state)
    {
        if (state)
            usingHDR = true;
        else
            usingHDR = false;

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }




}