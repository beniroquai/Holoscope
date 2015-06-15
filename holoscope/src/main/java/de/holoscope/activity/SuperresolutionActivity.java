package de.holoscope.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

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
import de.holoscope.tasks.SuperresolutionTask;
import de.holoscope.utils.FolderChooseUtils;
import de.holoscope.utils.ImageUtils;
import de.holoscope.view.ZoomableImageView;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

/**
 * Created by Bene on 31.05.2015.
 */
public class SuperresolutionActivity extends OpenCVActivity {
    private static final String TAG = "ZoomableImageActivity";
    private static final int SEEK_SIZE = 100;

    private TextView imageInfo;
    private SeekBar focusDepth;
    private ZoomableImageView imageView;

    private String imageType;
    private boolean useSlider;

    private String m_chosenDir = "";
    private boolean m_newFolderEnabled = true;

    private String global_path;

    List<File> fileList;

    private String acquireType = "SingleMode";

    Button btnMakeSuperresoltuion;
    Button dirChooseButton;

    private TextView acquireTextView;
    private TextView acquireTextView2;
    private TextView timeLeftTextView;
    private ProgressBar acquireProgressBar;

    Bitmap bmpLR;
    Bitmap bmpSR;

    int iLED = 0;       // Running Number for Superresolution

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_superresolution);

        acquireTextView = (TextView) findViewById(R.id.acquireStatusTextView);
        acquireTextView2 = (TextView) findViewById(R.id.acquireStatusTextView2);
        timeLeftTextView = (TextView) findViewById(R.id.timeLeftTextView);
        acquireProgressBar = (ProgressBar) findViewById(R.id.acquireProgressBar);



        // Load native library after(!) OpenCV initialization
        //System.loadLibrary("mixed_sample");

        imageInfo = (TextView) findViewById(R.id.image_info);
        imageInfo.setText(imageType + " at 0.0 " + Dataset.UNITS);

        imageView = (ZoomableImageView) findViewById(R.id.imageView);

        btnMakeSuperresoltuion = (Button) findViewById(R.id.btnStartSuperres);
        dirChooseButton = (Button) findViewById(R.id.chooseDirButton);
        //VIEW Hologram


        dirChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create DirectoryChooserDialog and register a callback
                FolderChooseUtils directoryChooserDialog =
                        new FolderChooseUtils(SuperresolutionActivity.this,
                                new FolderChooseUtils.ChosenDirectoryListener() {
                                    @Override
                                    public void onChosenDir(String chosenDir) {
                                        m_chosenDir = chosenDir;
                                        global_path = chosenDir;
                                        Toast.makeText(
                                                SuperresolutionActivity.this, "Chosen directory: " +
                                                        chosenDir, Toast.LENGTH_LONG).show();
                                        if (chosenDir != null) {
                                            //Find all Files in that directory, Provide Filenames as List
                                            File findFile = new File(global_path);
                                            Dataset.SUPERRESOLUTION_FILE_LIST = getListFiles(findFile);
                                            Log.i("List", String.valueOf(Dataset.SUPERRESOLUTION_FILE_LIST));
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


        btnMakeSuperresoltuion.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Log.i("List", String.valueOf(Dataset.SUPERRESOLUTION_FILE_LIST));



                        //new ComputeHologramTask(MainActivity.this).execute();
                        //new SuperresolutionTask(SuperresolutionActivity.this).execute((float) Dataset.LEDVAL, (float) Dataset.ZVAL);

                        Log.i("SuperresolutionMode", "Running");
                        new runSuperresolution().execute();







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


    private class runSuperresolution extends AsyncTask<Void, Void, Void> {
        int n = 0;
        long t = 0;

        int ABSOLUTE_CROP = 100; // Pixel which will be lost due to shift opperation
        Mat outSR;              // Allocate Mat for Result SR
        double MAG_RESULT = 2.;     // Magnification from LR Images to SR

        //Optical Flow
        public Mat firstFrame;
        public MatOfPoint lastCorners;


        //Imgproc.goodFeaturesToTrack(thisFrameGray, thisCorners, maxCorners, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, k);
        private final static double qualityLevel = 0.1;//0.35;
        private final static double minDistance = 50;//10;
        private final static int blockSize = 10;//8;
        private final static boolean useHarrisDetector = true;//true;
        private final double k = 1.0;
        private final static int maxCorners = 100;

        //params.setExposureCompensation(params.getMinExposureCompensation());

        private final int ledNumber = Math.round(Dataset.LEDVAL);
        private final int zSamples = Math.round(Dataset.ZVAL);

        ShiftParams[] shiftVector = new ShiftParams[zSamples * ledNumber];

        Mat tempLED;            // Mat for actual Image
        Mat matLR = new Mat();


        //Variables for Filepath
        String timestamp;
        String save_path;
        String path;
        File myDir;



        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            timeLeftTextView.setText("Time left:");
            acquireTextView.setText("Superresolution - MODE: ");
            acquireProgressBar.setVisibility(View.VISIBLE);     // Make invisible at first, then have it pop up
            acquireProgressBar.setMax(Dataset.LEDVAL+1);        // All LED images and Superresolution image


            firstFrame = null;
            lastCorners = new MatOfPoint();


            int i = 0;
            // Generate PAth and Folder for Image SAving

            timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.GERMANY).format(new Date());
            save_path = "Superresolution/Superresolution_" + timestamp;
            path = Dataset.getImageFolder(save_path);
            myDir = new File(path);
            myDir.mkdirs();




        }

        @Override
        protected void onProgressUpdate(Void...params)
        {
            acquireProgressBar.setProgress(iLED);
            imageView.setImage(bmpSR);

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
        protected Void doInBackground(Void... params) { //todo cHANGE the correct sending commands


            for (iLED = 0; iLED < Dataset.SUPERRESOLUTION_FILE_LIST.size(); iLED++) {

                //try {
                    File tempImage = Dataset.SUPERRESOLUTION_FILE_LIST.get(iLED);
                        tempLED = getGrayscaleFrameFromBitmap(BitmapFactory.decodeFile(String.valueOf(tempImage)));
                    shiftVector[iLED] = FindShift(tempLED);

                    Size sizeSR = new Size((tempLED.width() - 2 * ABSOLUTE_CROP) * 2, (tempLED.height() - 2 * ABSOLUTE_CROP) * 2);
                    matLR = new Mat();

                    Imgproc.resize(allignImage(shiftVector[iLED], tempLED), matLR, new Size(), MAG_RESULT, MAG_RESULT, Imgproc.INTER_CUBIC);
                    //Imgproc.resize(allignImage(shiftVector[iLED], tempLED), matLR, sizeSR);

                    //Save aligned LR Image
                    try {

                        bmpLR = ImageUtils.toBitmap(matLR);
                        File resultLR = new File(path + "Superresolution_" + iLED + ".jpg");
                        FileOutputStream fos = new FileOutputStream(resultLR);
                        bmpLR.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                        onProgressUpdate();
                    } catch (FileNotFoundException e) {
                        Log.e("Error: ", "Unable to save file! Sorry!");
                        return null;
                    }

                    //Start adding Images to Superresolution


                    // you can't pass an empty img to accumulateWeighted()
                    if (outSR == null) {
                        Log.i("OutSR", "Create OutSR:" + String.valueOf(matLR.size()) + String.valueOf(matLR.width()));
                        //outSR = matLR;
                        //Mat outSR = Mat.zeros(matLR.size(), matLR.type());
                        //outSR = matLR;
                        outSR = Mat.zeros(matLR.size(), CvType.CV_32F);
                    }

                    //
                    //Core.addWeighted(matLR, 0.5, outSR, 0.5, 0.0, outSR);



                    try {

                        Log.e("i", String.valueOf(iLED));
                        if (matLR.type() == CvType.CV_8UC1 || matLR.type() == CvType.CV_8UC3 || matLR.type() == CvType.CV_8UC4) {
                            Imgproc.accumulateWeighted(matLR, outSR, 0.5);
                        }
                    } catch (java.lang.Throwable t) {
                        Log.e("Accumulate", "Error");
                    }
                /*} catch (java.lang.Throwable t) {
                    Log.e("Error", "unable to do Superresolution" + String.valueOf(t)); //this'll tell you what class has been thrown
                }
                */

                n++;
            }
            try {
                outSR.convertTo(outSR, CvType.CV_8UC1);
                bmpSR = ImageUtils.toBitmap(outSR);
                File resultpathSR = new File(path + "Superresolution_Result.jpg");
                FileOutputStream fos = new FileOutputStream(resultpathSR);
                Log.i("Saving @: ", String.valueOf(resultpathSR));
                bmpSR.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } catch (FileNotFoundException e) {
                Log.e("Error: ", "Unable to save file! Sorry!");
                return null;
            }
            n++;

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (bmpLR != null) {
                imageView.setImage(bmpSR);
                imageInfo.setText("Picture from LED" + String.valueOf(n));
            }
        }


        private MatOfPoint2f MatOfPointTo2f(MatOfPoint mop) {
            //mop.convertTo(mop2f, CvType.CV_32FC2);
            Point[] points = mop.toArray();
            return new MatOfPoint2f(points);
        }

        private class ShiftParams {
            double shiftX;
            double shiftY;

            public ShiftParams(double shiftX, double shiftY) {
                this.shiftX = shiftX;
                this.shiftY = shiftY;
            }

            public double getshiftX() {
                return shiftX;
            }

            public double getshiftY() {
                return shiftY;
            }
        }


        private ShiftParams FindShift(Mat bm) {
            Mat thisFrameGray = new Mat();
            MatOfPoint thisCorners = new MatOfPoint();

// GET X/Y Shift in Pixel Integer Values relative to first Image


            double varianceX = 0.0;
            double varianceY = 0.0;

            try {
                thisCorners = new MatOfPoint();
                thisFrameGray = bm;

                //Imgproc.goodFeaturesToTrack(thisFrameGray, thisCorners, maxCorners, qualityLevel, minDistance);

                Imgproc.goodFeaturesToTrack(thisFrameGray, thisCorners, maxCorners, qualityLevel, minDistance, new Mat(), blockSize,
                        useHarrisDetector, k);

                if (firstFrame == null) {
                    //First run, get first Image Corners - Every Other Image Shift is relative to first one
                    firstFrame = thisFrameGray.clone();
                    Imgproc.goodFeaturesToTrack(thisFrameGray, lastCorners, maxCorners, qualityLevel, minDistance);
                } else {
                    Imgproc.goodFeaturesToTrack(thisFrameGray, thisCorners, maxCorners, qualityLevel, minDistance);
                    Mat flow = new Mat(firstFrame.size(), CvType.CV_32FC2);

                    MatOfPoint2f thisPoints = MatOfPointTo2f(thisCorners);
                    MatOfPoint2f lastPoints = MatOfPointTo2f(lastCorners);

                    MatOfByte status = new MatOfByte();
                    MatOfFloat err = new MatOfFloat();
                    Video.calcOpticalFlowPyrLK(firstFrame, thisFrameGray, lastPoints, thisPoints, status, err);


                    //TODO: Use lastPoints[i] + thisPoints[i] / time to calculate pixel distance traveled per unit of time.
                    Point[] lastPointsArray = lastPoints.toArray();
                    Point[] thisPointsArray = thisPoints.toArray();
                    byte[] statusArray = status.toArray();
                    int totalPoints = lastPointsArray.length;
                    int totalVarianceX = 0;
                    int totalVarianceY = 0;
                    for (int i = 0; i < lastPoints.toArray().length; i++) {
                        if (statusArray[i] == 1) {
                            totalVarianceX += lastPointsArray[i].x - thisPointsArray[i].x;
                            totalVarianceY += lastPointsArray[i].y - thisPointsArray[i].y;
                            //Log.i("VarianceX", String.valueOf(totalVarianceX));
                            //Log.i("VarianceY", String.valueOf(totalVarianceY));
                            //Log.i("XPos_last", String.valueOf(lastPointsArray[i].x));
                            //Log.i("XPos_this", String.valueOf(thisPointsArray[i].x));

                        }
                    }

                    //lastCorners.fromArray(lastCorners.toArray());

                    varianceX = (double) totalVarianceX / totalPoints;
                    varianceY = (double) totalVarianceY / totalPoints;

                    Log.i("VarianceX", String.valueOf(varianceX));
                    Log.i("VarianceY", String.valueOf(varianceY));

                    //Log.d("MjpegView status", status.dump());
                    //Log.d("MjpegView err", err.dump());

                }
            } catch (Exception ex) {
                Log.e("MjpegView", "Something went wrong", ex);
            }


            return new ShiftParams(varianceX, varianceY);

            //Log.d("Last Features 2", "#" + lastCorners.dump());
        }


        private Mat allignImage(ShiftParams shiftParams, Mat inputImage) {

            int height = (int) inputImage.size().height;
            int width = (int) inputImage.size().width;

            int shiftX = (int) ceil(shiftParams.getshiftX());
            int shiftY = (int) ceil(shiftParams.getshiftY());

            //Rect region_of_interest = Rect(x, y, w, h);
            //This will cut the ROI which has the same Spatial information than the others
            //Dimensions will stay the same compared to the other, the Value Abssolute_Crop
            // takes care of the outer boundary which will be cut away for correct allignment
            Rect roi = new Rect(ABSOLUTE_CROP - shiftX, ABSOLUTE_CROP - shiftY, width - 2 * ABSOLUTE_CROP, height - 2 * ABSOLUTE_CROP);
            Log.i("Rect", String.valueOf(roi));
            Mat image_roi = new Mat(inputImage, roi);
            return image_roi;
        }


        //TODO Depricated - other method works better/faster!
        private Mat shiftImage(Mat inputImage[], ShiftParams[] shiftParams) {
            int height = (int) inputImage[1].size().height;
            int width = (int) inputImage[1].size().width;

            Log.i("Height:", String.valueOf(height));
            Log.i("width:", String.valueOf(width));

            int scalefactor = 2;
            int imagenumber = 4;

            int[] dx = {0, 0, 1, 1};
            int[] dy = {0, 1, 0, 1};

            int x_dim = width - width % scalefactor;
            int y_dim = height - height % scalefactor;

            double value = 0;

            int p_shift_x = 0, p_shift_y = 0, px = 0, py = 0;

            Mat superresult = new Mat(new Size(2 * x_dim, 2 * y_dim), CvType.CV_8UC1);

            for (int k = 0; k < imagenumber; k++) {
                float progress = (k / imagenumber);
                //onProgressUpdate(-1, (int) (progress * 100));

                for (int y = 0; y < y_dim; y++) {


                    float progressLine = y / y_dim;
                    //onProgressUpdate(-2, (int) progressLine);
                    Log.i("Y-Wert", String.valueOf(y));

                    for (int x = 0; x < x_dim; x++) {

                        p_shift_x = (int) floor(shiftParams[k].getshiftX() + x);
                        p_shift_y = (int) floor(shiftParams[k].getshiftX() + y);


                        if (p_shift_x < 0) p_shift_x = 0;
                        else if (p_shift_x > x_dim - 1) p_shift_x = 0;


                        if (p_shift_y < 0) p_shift_y = 0;
                        else if (p_shift_y > y_dim - 1) p_shift_y = 0;


                        px = x * scalefactor + dx[k];
                        py = y * scalefactor + dy[k];


                        try {
                            superresult.put(py, px, inputImage[k].get(p_shift_y, p_shift_x)[0]);
                        } catch (NullPointerException e) {
                            Log.e("Error: ", String.valueOf(p_shift_x) + " und " + String.valueOf(p_shift_y));
                            superresult.put(py, px, 0);
                        }

                    }
                    //Log.i("Grayvalue: ", String.valueOf(inputImage[k].get(p_shift_y,p_shift_y)[0]));
                }
            }
            return superresult;

        }

        private Mat getGrayscaleFrameFromBitmap(Bitmap bm) {
            Mat thisFrameRgb = new Mat();
            Mat thisFrameGray = new Mat();
            Utils.bitmapToMat(bm, thisFrameRgb);
            Imgproc.cvtColor(thisFrameRgb, thisFrameGray, Imgproc.COLOR_RGB2GRAY);
            return thisFrameGray;
        }


    }

}
