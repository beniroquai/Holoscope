package de.holoscope.activity;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.holoscope.R;
import de.holoscope.datasets.Dataset;
import de.holoscope.tasks.ComputeHologramTask;
import de.holoscope.utils.FileChooseUtils;
import de.holoscope.utils.FolderChooseUtils;
import de.holoscope.utils.ImageUtils;
import de.holoscope.utils.complexMatrix;
import de.holoscope.view.ZoomableImageView;

import static java.lang.Math.ceil;

/**
 * Created by Bene on 30.05.2015.
 */
public class HologramActivity extends OpenCVActivity {
    private static final String TAG = "HologramActivity";
    private static final int SEEK_SIZE = 100;

    private TextView imageInfo;
    private SeekBar focusDepth;
    private ZoomableImageView imageView;

    private String imageType;
    private boolean useSlider;

    private String m_chosenDir = "";
    private boolean m_newFolderEnabled = true;

    private String global_path=null;

    List<File> fileList = null;

    boolean fileSelect = false;
    boolean folderSelect = false;

    Button dirChooseButton;
    Button fileChooseButton;
    Button btnMakeHologramm;

    private TextView acquireTextView;
    private TextView acquireTextView2;
    private TextView timeLeftTextView;
    private ProgressBar acquireProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hologram);

        imageInfo = (TextView) findViewById(R.id.image_info);
        imageInfo.setText(imageType + " at 0.0 " + Dataset.UNITS);

        imageView = (ZoomableImageView) findViewById(R.id.imageView);

        btnMakeHologramm = (Button) findViewById(R.id.btnMakeHologramm);
        dirChooseButton = (Button) findViewById(R.id.btnChooseDir);
        fileChooseButton = (Button) findViewById(R.id.btnChooseFile);

        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        acquireTextView2 = (TextView) findViewById(R.id.acquireStatusTextView2);
        timeLeftTextView = (TextView) findViewById(R.id.timeLeftTextView);
        acquireProgressBar = (ProgressBar) findViewById(R.id.acquireProgressBar);

        dirChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create DirectoryChooserDialog and register a callback
                FolderChooseUtils directoryChooserDialog =
                        new FolderChooseUtils(HologramActivity.this,
                                new FolderChooseUtils.ChosenDirectoryListener() {
                                    @Override
                                    public void onChosenDir(String chosenDir) {
                                        m_chosenDir = chosenDir;
                                        global_path = chosenDir;
                                        Toast.makeText(
                                                HologramActivity.this, "Chosen directory: " +
                                                        chosenDir, Toast.LENGTH_LONG).show();
                                        if (chosenDir != null) {
                                            //Find all Files in that directory, Provide Filenames as List
                                            File findFile = new File(global_path);
                                            fileList = getListFiles(findFile);
                                            Log.i("List", String.valueOf(fileList));
                                            folderSelect = true;
                                        }
                                    }
                                });
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(m_newFolderEnabled);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(m_chosenDir);
                m_newFolderEnabled = !m_newFolderEnabled;


            }
        });


        fileChooseButton.setOnClickListener(new View.OnClickListener() {
            String m_chosen;
            @Override
            public void onClick(View v) {
                FileChooseUtils FolderChooseDialog =  new FileChooseUtils(HologramActivity.this, "FileOpen",
                        new FileChooseUtils.SimpleFileDialogListener()

                        {
                            @Override
                            public void onChosenDir(String chosenDir)
                            {
                                // The code in this function will be executed when the dialog OK button is pushed
                                m_chosen = chosenDir;
                                Toast.makeText(HologramActivity.this, "Chosen FileOpenDialog File: " +
                                        m_chosen, Toast.LENGTH_LONG).show();
                                global_path = chosenDir;
                                fileSelect = true;
                            }
                        });

                FolderChooseDialog.chooseFile_or_Dir();
            }
        });


        btnMakeHologramm.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {

                if(folderSelect == true) {
                    if (fileList.size()!=0) {
                        Dataset.DATA_PATH_HOLOGRAM = String.valueOf(fileList.get(0));
                        //new ComputeHologramTask(MainActivity.this).execute();
                        new ComputeHologramTask(HologramActivity.this).execute(Dataset.ZMIN, Dataset.ZMAX, Dataset.ZINC);
                        folderSelect = false;
                    }
                }

                else if(fileSelect == true){
                    Dataset.DATA_PATH_HOLOGRAM = (global_path);
                    //new ComputeHologramTask(HologramActivity.this).execute(Dataset.ZMIN, Dataset.ZMAX, Dataset.ZINC);
                    new runHologram().execute();
                    fileSelect = false;
                }


            }
        });
    }

    @Override
    public void postOpenCVLoad() {
        super.postOpenCVLoad();
        if (useSlider)
            focusDepth.setEnabled(true);
    }

    public List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }







    private class runHologram extends AsyncTask<Void, Void, Void> {

        float zMax = Dataset.ZMAX*1e-3f;
        float zMin = Dataset.ZMIN*1e-3f;
        float zResolution = Dataset.ZINC*1e-3f;
        float z = 0;
        //Variables for Filepath
        String timestamp;
        String save_path;
        String path;
        File myDir;

        int i=0;

        Bitmap bmpHolo = null;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            acquireTextView.setText("Hologramreconstruction - MODE: ");
            acquireProgressBar.setVisibility(View.VISIBLE);     // Make invisible at first, then have it pop up
            acquireProgressBar.setMax((int) ceil((zMax-zMin)/zResolution));        // All LED images and Superresolution image

            int i = 0;
            // Generate PAth and Folder for Image SAving

            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.GERMANY).format(new Date());
            save_path = "Hologram/"+"Hologram_"+timestamp;
            path = Dataset.getImageFolder(save_path);
            myDir = new File(path);
            myDir.mkdirs();

        }
        //AsyncTask.cancel(true);


        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(i);

            acquireTextView.setText("Z-Position=" + z + "  Task" + String.valueOf(i) + "/" + String.valueOf((int) ((zMax-zMin)/zResolution)));
            acquireTextView2.setText("Task" + String.valueOf(i) + "/" + String.valueOf((int) ((zMax - zMin) / zResolution)));



            imageView.setImage(bmpHolo);

            //TODO Update Photo in Main Activity
        }
        void mSleep(int sleepVal) {
            try {
                Thread.sleep(sleepVal);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected Void doInBackground(Void... params) {
            //Params are given in mm and need to be converted into meter

            Log.i("Reconstructionparameters:", "zMax="+String.valueOf(zMax)+" zMin="+String.valueOf(zMin)+" zResolution="+String.valueOf(zResolution));

            for (z = zMin; z <= zMax; z += zResolution) {

                Log.i("Values", String.valueOf(z)+"_"+String.valueOf(Dataset.WAVELENGTH)+"_"+String.valueOf(Dataset.PIXELSIZE));
                publishProgress();

                try {
                    bmpHolo = computeHologram(z, Dataset.WAVELENGTH, Dataset.PIXELSIZE);
                    File resultHolo = new File(path + "Hologram_at_z-" + String.valueOf(z*1000000)+"mm" + ".jpg");
                    FileOutputStream fos = new FileOutputStream(resultHolo);
                    bmpHolo.compress(Bitmap.CompressFormat.PNG, 100, fos);

                } catch (FileNotFoundException e) {
                    Log.e("Error: ", "Unable to save file! Sorry!" + e);
                    return null;
                }
                i++;

            }


            return null;
        }


        private Bitmap computeHologram(float z, double lambda, double pixelsize) {

            complexMatrix mRgba = new complexMatrix();

            mRgba.phaseMat = new Mat();
            mRgba.magMat = new Mat();
            mRgba.imagMat = new Mat();
            mRgba.realMat = new Mat();


            mRgba.magMat = Highgui.imread(Dataset.DATA_PATH_HOLOGRAM);
            mRgba.phaseMat = Mat.zeros(mRgba.magMat.size(), mRgba.magMat.type());

            Imgproc.cvtColor(mRgba.magMat, mRgba.magMat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(mRgba.phaseMat, mRgba.phaseMat, Imgproc.COLOR_BGR2GRAY);
            mRgba.magMat.convertTo(mRgba.magMat, CvType.CV_32FC1);
            mRgba.phaseMat.convertTo(mRgba.phaseMat, CvType.CV_32FC1);

            Core.sqrt(mRgba.magMat, mRgba.magMat);

            Core.polarToCart(mRgba.magMat, mRgba.phaseMat, mRgba.realMat, mRgba.imagMat);

            mRgba.magMat.convertTo(mRgba.magMat, CvType.CV_32FC1);
            mRgba.phaseMat.convertTo(mRgba.phaseMat, CvType.CV_32FC1);

            /*

            complexMatrix mRgba = new complexMatrix();
            mRgba.realMat = Highgui.imread(Dataset.DATA_PATH_HOLOGRAM);
            mRgba.imagMat = Mat.zeros(mRgba.realMat.rows(), mRgba.realMat.cols(), CvType.CV_32FC1);



            Imgproc.cvtColor(mRgba.realMat, mRgba.realMat, Imgproc.COLOR_BGR2GRAY);

            mRgba.realMat.convertTo(mRgba.realMat, CvType.CV_32FC1);
            Core.sqrt(mRgba.realMat, mRgba.realMat);
*/

            complexMatrix fourier = ImageUtils.fresnelPropagator(mRgba, z, lambda, pixelsize);


            // ----------- Change Compolex Fourier into mag and log-scale
            Mat mag = new Mat(fourier.realMat.size(), fourier.realMat.type());

            Core.magnitude(fourier.realMat, fourier.imagMat, mag);// planes[0] = magnitude
            Core.pow(mag, 2., mag);

            Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX);

            Mat realResult = new Mat(mag.size(), CvType.CV_8UC1);

            mag.convertTo(realResult, CvType.CV_8UC1);

            return ImageUtils.toBitmap(realResult);

        }

        @Override
        protected void onPostExecute(Void result) {
            if (result != null) {
                imageView.setImage(bmpHolo);
                Toast.makeText(
                        HologramActivity.this, "Ready!", Toast.LENGTH_LONG).show();

            }
        }





    }


}
