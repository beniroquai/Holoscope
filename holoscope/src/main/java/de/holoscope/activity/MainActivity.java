package de.holoscope.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import de.acquisition.AcquireActivity;
import de.holoscope.R;

import de.holoscope.bluetooth.BluetoothDeviceListActivity;
import de.holoscope.bluetooth.BluetoothService;
import de.holoscope.datasets.Dataset;
import de.holoscope.miracast.MiracastActivity;
import de.holoscope.tasks.InitMicroscopeTask;
import de.serenegiant.usbcameratest5.MainActivityWebcam;

public class MainActivity extends OpenCVActivity {
    private static final String TAG = "MainActivity";

    private Button acquirePicture;
    private Button startOpenCV;
    private Button computeHologram;
    private Button viewHologram;
    private Button setParameters;
    private Button initializeMicroscope;
    private Button viewData;
    private Button estimateShift;
    private Button viewPresentation;



    private static final boolean Debug = true;
    String defaultAddress = Dataset.BTAddress;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private static final int ACTIVITY_CHOOSE_FILE = 3;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceMACAddress = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mBluetoothService = null;

    Intent serverIntent = null;
    public boolean btConnection;

    public Dataset mDataset =  new Dataset();

    Button btnConnectBluetooth;
    TextView connStatusTextView, connDeviceNameTextView, connMACAddressTextView;





 //Register Native Function, will be evaluated later
    public native void FindFeatures(long matAddrGr, long matAddrRgba);
/*

    static {
        System.loadLibrary("hello");
    }

    public native String hello();
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load native library after(!) OpenCV initialization
        System.loadLibrary("mixed_sample");

        btnConnectBluetooth = (Button) findViewById(R.id.connectBluetooth);
        computeHologram = (Button) findViewById(R.id.computeHologram);
        setParameters = (Button) findViewById(R.id.setParameters);
        viewHologram = (Button) findViewById(R.id.viewHologram);
        acquirePicture = (Button) findViewById(R.id.acquirePicture);
        viewPresentation = (Button) findViewById(R.id.viewPresentation);
        startOpenCV = (Button) findViewById(R.id.startOpenCV);
        initializeMicroscope = (Button) findViewById(R.id.initMicroscope);
        viewData = (Button) findViewById(R.id.viewData);
        estimateShift = (Button) findViewById(R.id.estimateShift);

//TODO ACQUIRE Background

        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
        connDeviceNameTextView = (TextView) findViewById(R.id.connDeviceNameTextView);
        connMACAddressTextView = (TextView) findViewById(R.id.connMACAddressTextView);

        //TextView textView = (TextView) findViewById(R.id.textview);
        //textView.setText(hello());


        // START BLUETOOTH STUFF

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Create the BT service object
        mBluetoothService = new BluetoothService(this, mHandler);

        // See if we've already got an instance of the bluetooth connection going, and if so update the UI to reflect this
        GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();

        // Set the global Dataset object
        BTAppClass.setDataset(mDataset);
        if (BTAppClass.getBluetoothService() != null)
        {
            //Toast.makeText(this, "Bluetooth class is active!", Toast.LENGTH_LONG).show();
            mBluetoothService = BTAppClass.getBluetoothService();

            connDeviceNameTextView.setText(mBluetoothService.getDeviceName());
            connMACAddressTextView.setText(mBluetoothService.getDeviceAddress());

            connStatusTextView.setText("Connected to Array");
            btnConnectBluetooth.setText("Disconnect from Array");
            btConnection = true;
        } else {
            startBluetooth();
            // Try and connect to the default class
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(defaultAddress);
            // Attempt to connect to the device
            mBluetoothService.connect(device);
            BTAppClass.setBluetoothService(mBluetoothService);
        }



        btnConnectBluetooth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btConnection)
                {
                    sendData("endConnection");
                    stopBTService();
                }
                else
                    startBTService();
            }
        });
        // END BLUETOOTH STUFF-------------------------------------------------------------------------------


        viewPresentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), PhaseActivity.class);
                startActivity(intent);
            }
        });


        computeHologram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), HologramActivity.class);
                startActivity(intent);
    }
});

        viewHologram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.startViewActivity("hologram", true);
            }
        });

        setParameters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), SetParametersActivity.class);
                startActivity(intent);


            }
        });

        acquirePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MainActivityWebcam.class);
                intent.putExtra("type", "Data");
                intent.putExtra("LEDVAL", Dataset.LEDVAL);
                intent.putExtra("ZVAL", Dataset.ZVAL);
                startActivity(intent);


            }
        });
        startOpenCV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(),AcquireActivity.class);
                //intent.putExtra("type", "Background");
                //intent.putExtra("LEDVAL", DatasetHologram.LEDVAL);
                //intent.putExtra("ZVAL", DatasetHologram.ZVAL);

                //TODO Intents for Datatype, ZVAL, LEDVAL
                startActivity(intent);


            }
        });
        initializeMicroscope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(v.getContext(), InitMicroscopeTask.class);
                startActivity(intent);

            }
        });
        viewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.startViewActivity("Data", true);
            }
        });
        estimateShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //new ComputeHologramTask(MainActivity.this).execute();
                Intent intent = new Intent(v.getContext(), SuperresolutionActivity.class);
                startActivity(intent);
            }
        });

    }

    //ensure app is in portrait orientation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    //fire intent to start activity with proper configuration for type
    protected void startViewActivity(String type, boolean useSlider) {
        Intent intent = new Intent(this, ZoomableImageActivity.class);
        intent.putExtra("type", type);
        intent.putExtra("useSlider", useSlider);
        startActivity(intent);
    }



    protected void startBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Asking to enable BT");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) {
                startBTService();
            }
        }
    }





    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(Debug) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            connStatusTextView.setText("Connected to Array");
                            btnConnectBluetooth.setText("Disconnect from Array");
                            btConnection = true;
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            connStatusTextView.setText("Connecting to Array");
                            connDeviceNameTextView.setText("");
                            connMACAddressTextView.setText("");
                            btConnection = false;
                            break;
                        case BluetoothService.STATE_LISTEN:
                            connStatusTextView.setText("Disconnected");
                            btnConnectBluetooth.setText("Connect to Array");
                            connDeviceNameTextView.setText("");
                            connMACAddressTextView.setText("");
                            btConnection = false;
                            break;
                        case BluetoothService.STATE_NONE:
                            connStatusTextView.setText("Disconnected");
                            btnConnectBluetooth.setText("Connect to Array");
                            connDeviceNameTextView.setText("");
                            connMACAddressTextView.setText("");
                            btConnection = false;
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    mConnectedDeviceMACAddress = msg.getData().getString(DEVICE_ADDRESS);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    connDeviceNameTextView.setText(mConnectedDeviceName);
                    connMACAddressTextView.setText(mConnectedDeviceMACAddress);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Debug) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Show the address for debugging
                    if(Debug)Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == RESULT_OK) {
                    // Bluetooth is now enabled, so set up the Bluetooth service
                    startBTService();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
        if(requestCode == ACTIVITY_CHOOSE_FILE)
        {
            if (data != null)
            {
                Uri uri = data.getData();
                //String FilePath = getRealPathFromURI(uri);
                //mDataset.buildFileListFromPath(FilePath);
            }
        }
        if (resultCode != RESULT_OK) return;
    }

    private void startBTService()
    {
        Log.d(TAG, "Starting BT Service");

        // Initialize the buffer for outgoing memBtAdapterssages
        mOutStringBuffer = new StringBuffer("");

        serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

        // Tie the service to the global application context
        GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
        BTAppClass.setBluetoothService(mBluetoothService);

    }
    private void stopBTService() {
        Log.d(TAG, "Stopping BT Service");
        mBluetoothService.stop();
    }


    public void sendData(String message) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

}
