package de.holoscope.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import de.holoscope.datasets.Dataset;
import de.holoscope.utils.ImageUtils;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Bene on 18.04.2015.
 */
public class ComputeHologramTask extends ImageProgressTask {



    public ComputeHologramTask(Context context) {
        super(context);
        this.progressDialog.setMessage("Assembling Hologram image...");
    }


    @Override
    protected Void doInBackground(Float... params) {
        //Params are given in mm and need to be converted into meter
        float zMax = params[1]*1e-3f;
        float zMin = params[0]*1e-3f;
        float zResolution = params[2]*1e-3f;



        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.GERMANY).format(new Date());
        String save_path = "Hologram/"+"Hologram_"+timestamp;
        String path = Dataset.getImageFolder(save_path);
        File myDir = new File(path);
        myDir.mkdirs();

        int i = 0;

        Log.i("Reconstructionparameters:", "zMax="+String.valueOf(zMax)+" zMin="+String.valueOf(zMin)+" zResolution="+String.valueOf(zResolution));

        for (float z = zMin; z <= zMax; z += zResolution) {
            float progress = (z - zMin) / (zMax - zMin);
            onProgressUpdate();





            //File resultBmp = new File(Dataset.getImagePath("Hologram", "Hologram", ".jpg", i, 0));

            //Will be /sdcard/Holoscope/Singlemode/Singlemode_XXYYYZZ
            //Log.i("Saving @: ", DatasetHologram.getResultImagePath("Hologram", i));
            try {
                Bitmap result = computeHologram(z);
                FileOutputStream fos = new FileOutputStream(path+i);
                result.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (FileNotFoundException e) {
                Log.e("Error: ", "Unable to save file! Sorry!" + e);
                return null;
            }
            i++;

        }


        return null;
    }


    private Bitmap computeHologram(float z) {


        Log.i("Info", "Start Compute Hologram");

        Mat mRgba = new Mat();
        mRgba = Highgui.imread(Dataset.DATA_PATH_HOLOGRAM);
        Bitmap resultBMP = null;


        Log.i("Info", "Load Success");


        Mat fourier = new Mat();
        //fourier = ImageUtils.fresnelPropagator(mRgba, z);

        float progress = (z/ Dataset.ZMAX);
        onProgressUpdate(-1, (int)(progress * 100));

        return ImageUtils.toBitmap(fourier);

    }






}
