package de.holoscope.datasets;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.List;

/**
 * Created by Bene on 18.04.2015.
 */
public class Dataset {

    private String TAG = "Dataset";

    public int XCROP = 507;
    public int YCROP = 96;

    public int WIDTH = 3264;   // 3264
    public int HEIGHT = 2448;  //2448

    public File[] fileList = null;
    public int fileCount = 0;

    public String ARRAY_TYPE = "domeA";




    public String DATASET_NAME = "";
    public String DATASET_TYPE = ""; // Can be multimode, brightfield_scan, full_scan
    public String DATASET_HEADER = "";

    //Autofocus Parameter
    public static String UNITS = "mm";
    public static double PIXELSIZE = 1.2e-6;
    public static float ZINC = 1;
    public static float ZMIN = 10;
    public static float ZMAX = 30;

    public static final String DEVICE_ADDRESS = "20:15:03:31:24:26"; //TODO Right Adress??


    public static int Z_SAMPLES = 50;

    public static String BTAddress = "20:15:03:31:24:26";

    //Acquistion PArameters
    public static double WAVELENGTH = 650;
    public static int ZVAL = 4;
    public static int LEDVAL = 5;

    public static String DATASET_PATH = "/sdcard/Holoscope/"; //Environment.getExternalStorageDirectory()
    public static final String DATASET = "Data";
    public static final String DATASET_RAW = "0";
    public static String DATASET_PATH_TEMP;
    public static String DATA_PATH_Temp;

    public static String DATA_PATH_HOLOGRAM;
    public static String DATA_PATH_PHASE;
    public static List<File> SUPERRESOLUTION_FILE_LIST;



    //Variable Data for Webcam Filenames
    public static int actZVAL;              // Value for captured LED
    public static int actLEDVAL;            // Value for captured Z-Position
    public static String DATA_TYPE;          // Value for acquired Type
    public static String DATA_PATH;      // Value for path


    //Call it like that: getImagePath("raw", "singlemode", ".jpg", 0, 1)
    //result will be: /sdcard/datasets/raw/singlemode01.jpg
    public static String getImagePath(String data_folder, String data_type, String extension, int z_number, int led_number) {
        return DATASET_PATH + data_folder + "/" + data_type +   String.format("%d%d", z_number, led_number) + extension;
    }


    public static String getImageFolder(String data_folder) {
        return DATASET_PATH + data_folder+"/";
    }


    //TODO REMOVE all PATH Methods:



        public void buildFileListFromPath(String path) {

            Log.d(TAG, String.format("FilePath is: %s", path));
            // Extract directory:
            DATASET_PATH = path;
            Log.d(TAG, String.format("Path is: %s", DATASET_PATH));
            File fileList2 = new File(DATASET_PATH);

            fileList = fileList2.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String name = pathname.getName().toLowerCase();
                    return name.endsWith(".jpeg") && pathname.isFile();
                }
            });
        /*
        for (int i = 0; i<mDataset.fileList.length; i++)
        {
      	  Log.d(TAG,mDataset.fileList[i].toString());
        }
        */

            fileCount = fileList.length;
            String firstFileName = fileList[0].getAbsoluteFile().toString();
            // Define the dataset type
            if (firstFileName.contains("Brightfield_Scan")) {
                DATASET_TYPE = "brightfield";
                DATASET_HEADER = firstFileName.substring(path.lastIndexOf(File.separator) + 1, firstFileName.lastIndexOf("_scanning_")) + "_scanning_";
                Log.d(TAG, String.format("BF Scan Header is: %s", DATASET_HEADER));
            } else if (firstFileName.contains("multimode")) {
                DATASET_TYPE = "multimode";
                DATASET_HEADER = "milti_";
                Log.d(TAG, String.format("Header is: %s", DATASET_HEADER));
            } else if (firstFileName.contains("Full_Scan")) {
                DATASET_TYPE = "full_scan";
                DATASET_HEADER = firstFileName.substring(firstFileName.lastIndexOf(File.separator) + 1, firstFileName.lastIndexOf("_scanning_"));
                Log.d(TAG, String.format("Full Scan Header is: %s", DATASET_HEADER));
            }

            // Name the Dataset after the directory
            DATASET_NAME = DATASET_PATH.substring(0, DATASET_PATH.lastIndexOf(File.separator));
        }


    }



