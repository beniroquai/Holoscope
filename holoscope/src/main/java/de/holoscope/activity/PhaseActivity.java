package de.holoscope.activity;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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

import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

/**
 * Created by Bene on 30.05.2015.
 */
public class PhaseActivity extends OpenCVActivity {
    private static final String TAG = "PhaseActivity";
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
    EditText textZInfocus;
    EditText textThreshold;
    EditText textBackground;


    private TextView acquireTextView;
    private TextView acquireTextView2;
    private TextView timeLeftTextView;
    private ProgressBar acquireProgressBar;


    float zInfocus = Dataset.ZMAX*1e-3f;
    int thresholdVal = 50;
    int backgroundVal = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_phase);

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



        textZInfocus = (EditText) findViewById(R.id.textInFocus);
        textThreshold = (EditText) findViewById(R.id.textThreshold);
        textBackground = (EditText) findViewById(R.id.textBackground);




        dirChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create DirectoryChooserDialog and register a callback
                FolderChooseUtils directoryChooserDialog =
                        new FolderChooseUtils(PhaseActivity.this,
                                new FolderChooseUtils.ChosenDirectoryListener() {
                                    @Override
                                    public void onChosenDir(String chosenDir) {
                                        m_chosenDir = chosenDir;
                                        global_path = chosenDir;
                                        Toast.makeText(
                                                PhaseActivity.this, "Chosen directory: " +
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
                FileChooseUtils FolderChooseDialog = new FileChooseUtils(PhaseActivity.this, "FileOpen",
                        new FileChooseUtils.SimpleFileDialogListener()

                        {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                // The code in this function will be executed when the dialog OK button is pushed
                                m_chosen = chosenDir;
                                Toast.makeText(PhaseActivity.this, "Chosen FileOpenDialog File: " +
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

                zInfocus = Float.valueOf(textZInfocus.getText().toString())*1e-3f;
                thresholdVal = Integer.parseInt(textThreshold.getText().toString());
                backgroundVal = Integer.parseInt(textBackground.getText().toString());


                if (folderSelect == true) {
                    if (fileList.size() != 0) {
                        Dataset.DATA_PATH_HOLOGRAM = String.valueOf(fileList.get(0));
                        //new ComputeHologramTask(MainActivity.this).execute();
                        new ComputeHologramTask(PhaseActivity.this).execute(Dataset.ZMIN, Dataset.ZMAX, Dataset.ZINC);
                        folderSelect = false;
                    }
                } else if (fileSelect == true) {
                    Dataset.DATA_PATH_PHASE = (global_path);


                    //new ComputeHologramTask(HologramActivity.this).execute(Dataset.ZMIN, Dataset.ZMAX, Dataset.ZINC);
                    new runPhaseRecovery().execute();
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







    private class runPhaseRecovery extends AsyncTask<Void, Void, Void> {



        //Variables for Filepath
        String timestamp;
        String save_path;
        String path;
        File myDir;

        int niter = 10;
        int i=0;

        Mat phaseImage = null;
        complexMatrix source = new complexMatrix();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            acquireTextView.setText("Hologramreconstruction - MODE: ");
            acquireProgressBar.setVisibility(View.VISIBLE);     // Make invisible at first, then have it pop up
            acquireProgressBar.setMax(niter);        // All LED images and Superresolution image

            int i = 0;
            // Generate PAth and Folder for Image SAving

            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.GERMANY).format(new Date());
            save_path = "PhaseRecovery/"+"Phase"+timestamp;
            path = Dataset.getImageFolder(save_path);
            myDir = new File(path);
            myDir.mkdirs();

        }
        //AsyncTask.cancel(true);


        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(i);
            Mat temp = source.phaseMat.clone();
            Core.normalize(temp, temp, 0, 255, Core.NORM_MINMAX);
            temp.convertTo(temp, CvType.CV_8UC1);
            imageView.setImage(ImageUtils.toBitmap(temp));
            //acquireTextView.setText("Z-Position=" + z + "  Task" + String.valueOf(i) + "/" + String.valueOf((int) ((zMax-zMin)/zResolution)));
            acquireTextView2.setText("Task"+String.valueOf(i)+"/"+String.valueOf(niter));


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

            phaseImage = recoverPhase(zInfocus, Dataset.WAVELENGTH, Dataset.PIXELSIZE, niter, thresholdVal, backgroundVal);

            saveImage(phaseImage, "Phaserecovery");

            return null;
        }




        private Mat recoverPhase(float z, double lambda, double pixelsize, int niter, int threshold, int background) {

            Log.i("threshold", String.valueOf(threshold));

            Log.i("zInfocus", String.valueOf(zInfocus));
            Mat background_intesity;

            source.phaseMat = new Mat();
            source.magMat = new Mat();

            source.imagMat = new Mat();
            source.realMat = new Mat();

            Mat amplitude = Highgui.imread(Dataset.DATA_PATH_PHASE);
            Imgproc.cvtColor(amplitude, amplitude, Imgproc.COLOR_BGR2GRAY);
            amplitude.convertTo(amplitude, CvType.CV_32FC1);
            Core.sqrt(amplitude, amplitude);

            source.phaseMat = Mat.zeros(amplitude.size(), amplitude.type());

            //Core.cartToPolar(source.realMat, source.imagMat, source.magMat, source.phaseMat);
            for (i = 0; i <= niter; i++) {


                amplitude.copyTo(source.magMat);

                saveImage(source.magMat, "0_Magnitude_before_1st_Fresnel" + String.valueOf(i));
                saveImage(source.phaseMat, "0_Phase_before_1st_Fresnel" + String.valueOf(i));

                //source.realMat = toReal(source.magMat, source.phaseMat);
                //source.imagMat = toImag(source.magMat, source.phaseMat);

                Core.polarToCart(source.magMat, source.phaseMat, source.realMat, source.imagMat);

                saveImage(source.realMat, "1_Real_before_1st_Fresnel" + String.valueOf(i));
                saveImage(source.imagMat, "1_Imag_before_1st_Fresnel" + String.valueOf(i));

                source = ImageUtils.fresnelPropagator(source, -z, lambda, pixelsize);

                //source.magMat = toMag(source.realMat, source.imagMat);
                //source.phaseMat = toPhase(source.realMat, source.imagMat);

                Core.cartToPolar(source.realMat, source.imagMat, source.magMat, source.phaseMat);

                saveImage(source.magMat, "1_Magnitude_after_1st_Fresnel" + String.valueOf(i));
                saveImage(source.phaseMat, "1_Phase_after_1st_Fresnel" + String.valueOf(i));



                source.magMat.convertTo(source.magMat, CvType.CV_32FC1);

                Scalar m = Core.mean(source.magMat);

                Core.MinMaxLocResult mmr = Core.minMaxLoc(source.magMat);
                Point point = mmr.maxLoc;
                int x = (int)point.x;
                int y = (int)point.y;
                double maxval = source.magMat.get(y, x)[0];
                Log.i("Val", "x"+String.valueOf(x)+"y"+String.valueOf(y)+"maxval"+String.valueOf(maxval));

                //int mean = (int)m.val[0];
                double mean = m.val[0];
                Log.i("Mean", String.valueOf(m.val[0])+String.valueOf(mean));

                source.magMat  = substractBackground(source.magMat, threshold, background);

                saveImage(source.magMat, "threshold_filtered" + String.valueOf(i));


                //source.realMat = toReal(source.magMat, source.phaseMat);
                //source.imagMat = toImag(source.magMat, source.phaseMat);



                source = ImageUtils.fresnelPropagator(source, z, lambda, pixelsize);


                /*

*/

                //source.magMat = toMag(source.realMat, source.imagMat);
                //source.phaseMat = toPhase(source.realMat, source.imagMat);

                //Core.cartToPolar(source.realMat, source.imagMat, source.magMat, source.phaseMat);

                source.magMat = new Mat(source.realMat.size(), source.realMat.type());
                source.phaseMat = new Mat(source.realMat.size(), source.realMat.type());
                Core.magnitude(source.realMat, source.imagMat, source.magMat);// planes[0] = magnitude
                Core.phase(source.realMat, source.imagMat, source.phaseMat);// planes[0] = magnitude

                saveImage(source.magMat, "source.magMat after 2nd Fresnel" + String.valueOf(i));
                saveImage(source.phaseMat, "source.phaseMat after 2nd Fresnel" + String.valueOf(i));

                publishProgress();

            }

            Core.normalize(source.phaseMat, source.phaseMat, 0, 255, Core.NORM_MINMAX);
            source.phaseMat.convertTo(source.phaseMat, CvType.CV_8UC1);

            return source.phaseMat;

        }

        @Override
        protected void onPostExecute(Void result) {
            if (result != null) {
                //imageView.setImage(bmpHolo);
                Toast.makeText(
                        PhaseActivity.this, "Ready!", Toast.LENGTH_LONG).show();

            }
        }


        private void saveImage(Mat saveMat , String imType) {
            try {

                Mat temp = saveMat.clone();
                Core.normalize(temp, temp, 0, 255, Core.NORM_MINMAX);
                temp.convertTo(temp, CvType.CV_8UC1);

                File resultPhase = new File(path + imType  + ".jpg");
                FileOutputStream fos = new FileOutputStream(resultPhase);
                ImageUtils.toBitmap(temp).compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (FileNotFoundException e) {
                Log.e("Error: ", "Unable to save file! Sorry!" + e);
            }
        }



        Mat substractBackground(Mat inputMat, double threshold, double mean) {

            Mat resultMat = new Mat(inputMat.size(), inputMat.type());
            double thresholdTemp = 0;
            double background_intensity=0;
            double filtered =0;
            double I=0;
            double I_temp=0;

            for (int iwidth = 0; iwidth < inputMat.width(); iwidth++) {
                for (int iheight = 0; iheight < inputMat.height(); iheight++) {


                    try{
                        double[] data = inputMat.get(iheight, iwidth);

                        I = (int) data[0];

                    }
                    catch(NullPointerException e){
                        Log.e("Error", String.valueOf(e));
                    }

                    if (I > threshold) thresholdTemp = 0;            // Objects have lower grayvalue
                    else thresholdTemp = 1;                // If Object, it gets a one

                    background_intensity = thresholdTemp * mean;        // Ersetze the background with avg/mean background
                    I_temp = I * (1 - thresholdTemp);            // Zero every Pixel which has objectsupport
                    filtered = I_temp + background_intensity;            // Ersetze the zero-values from background with background intensity-grayvalue (eg. 136)

                    resultMat.put(iheight, iwidth, filtered);
                }
                Log.i("Val", "I: "+String.valueOf(I)+", thresholdTemp: "+String.valueOf(thresholdTemp)+", background_intensity: "+String.valueOf(background_intensity)+", filtered: "+String.valueOf(filtered));
            }

            return  resultMat;
        }


    }





    Mat toReal(Mat magMat, Mat phaseMat) {

        Mat resultMat = new Mat(magMat.size(), magMat.type());

        for (int iwidth = 0; iwidth < magMat.width(); iwidth++) {
            double mag=0, phase=0;
            double real=0;
            for (int iheight = 0; iheight < magMat.height(); iheight++) {

                mag = magMat.get(iheight, iwidth)[0];
                phase = phaseMat.get(iheight, iwidth)[0];
                real= mag*cos(phase);


                resultMat.put(iheight, iwidth, real);
            }
            Log.i("Val", "mag: "+String.valueOf(mag)+", phase: "+String.valueOf(phase)+", real: "+String.valueOf( mag*sin(phase)));
        }

        return  resultMat;
    }

    Mat toMag(Mat realMat, Mat imagMat) {

        Mat resultMat = new Mat(realMat.size(), realMat.type());

        for (int iwidth = 0; iwidth < realMat.width(); iwidth++) {
            double real=0, imag=0;
            double mag=0;
            for (int iheight = 0; iheight < imagMat.height(); iheight++) {

                real = realMat.get(iheight, iwidth)[0];
                imag = imagMat.get(iheight, iwidth)[0];

                mag = sqrt(imag * imag + real * real);


                resultMat.put(iheight, iwidth, mag);
            }
            Log.i("Val", "imag: "+String.valueOf(imag)+", real: "+String.valueOf(real)+", mag: "+String.valueOf(mag));
        }

        return  resultMat;
    }



    Mat toPhase(Mat realMat, Mat imagMat) {

        Mat resultMat = new Mat(realMat.size(), realMat.type());

        for (int iwidth = 0; iwidth < realMat.width(); iwidth++) {
            double real=0, imag=0;
            double phase=0;
            for (int iheight = 0; iheight < imagMat.height(); iheight++) {

                real = realMat.get(iheight, iwidth)[0];
                imag = imagMat.get(iheight, iwidth)[0];

                phase= atan2(imag, real);

                resultMat.put(iheight, iwidth, imag);
            }
            Log.i("Val", "mag: "+String.valueOf(imag)+", real: "+String.valueOf(real)+", imag: "+String.valueOf(phase));
        }

        return  resultMat;
    }


    Mat toImag(Mat magMat, Mat phaseMat) {

        Mat resultMat = new Mat(magMat.size(), magMat.type());

        for (int iwidth = 0; iwidth < magMat.width(); iwidth++) {
            double mag=0, phase=0;
            double imag=0;
            for (int iheight = 0; iheight < magMat.height(); iheight++) {

                mag = magMat.get(iheight, iwidth)[0];
                phase = phaseMat.get(iheight, iwidth)[0];
                imag= mag*sin(phase);


                resultMat.put(iheight, iwidth, imag);
            }
            Log.i("Val", "mag: "+String.valueOf(mag)+", phase: "+String.valueOf(phase)+", imag: "+String.valueOf( mag*sin(phase)));
        }

        return  resultMat;
    }




}


/*
                 Mat thresMat = new Mat(source.magMat.size(), source.magMat.type());
                //Core.normalize(thresMat, thresMat, 0, 1, Core.NORM_MINMAX); //;
                saveImage(thresMat, "thresMat_low" + String.valueOf(i));

                Imgproc.threshold(I_norm, thresMat, 0.68*255, 255, Imgproc.THRESH_BINARY);
                Core.normalize(thresMat, thresMat, 0, 1, Core.NORM_MINMAX); //;
                saveImage(thresMat, "thresMat" + String.valueOf(i));

                background_intesity = new Mat(source.magMat.size(), source.magMat.type());
                background_intesity.convertTo(background_intesity, CvType.CV_8UC1);
                Scalar m = Core.mean(source.magMat);
                //int mean = (int)m.val[0];
                m = Scalar.all((int)m.val[0]);
                Log.i("Mean", String.valueOf(m) + String.valueOf(m.val[0]));



                Core.multiply(thresMat, m, background_intesity);


                saveImage(background_intesity, "background_intensity" + String.valueOf(i));



                Core.multiply(source.magMat, thresMat, source.magMat);
                Core.add(source.magMat, background_intesity, source.magMat);

                saveImage(source.magMat, "source.magMatAfterBackgroundsub" + String.valueOf(i));



                //source.phaseMat.convertTo(source.phaseMat, CvType.CV_32FC1);
                //source.magMat.convertTo(source.magMat, source.phaseMat.type());

                //Core.polarToCart(source.magMat, source.phaseMat, source.realMat, source.imagMat);
*/