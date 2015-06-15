package de.acquisition;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;

import java.io.FileOutputStream;
import java.security.Policy;
import java.util.List;

/**
 * Created by Bene on 25.05.2015.
 */

public class CameraView extends JavaCameraView implements Camera.PictureCallback {


    private static final String TAG = "Sample::Tutorial3View";
    private String mPictureFileName;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Camera.Size> getResolutionList() {
        String FlattenValues = mCamera.getParameters().flatten();
        Log.e("General Values: ", FlattenValues);
        return mCamera.getParameters().getSupportedPictureSizes();

    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        //Camera.Parameters params = mCamera.getParameters();
        if (mCamera == null || mCamera.getParameters() == null) {
            Toast.makeText(getContext(), "Error", Toast.LENGTH_SHORT).show();
        } else {
            String FlattenValues = mCamera.getParameters().flatten();
            Log.e("General Values: ", FlattenValues);
            
            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mCamera.getParameters());
            mCamera.startPreview();
            boolean mLightOn = true;



            mCamera.getParameters().setFocusMode("FOCUS_MODE_INFINITY");
            String supportedIsoValues = mCamera.getParameters().get("iso-values"); //supported values, comma separated String
            Log.e("Isovalues", supportedIsoValues);
           
            mCamera.getParameters().setPictureSize(resolution.width, resolution.height);
            mCamera.setParameters(mCamera.getParameters());
        }

        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;

        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}
