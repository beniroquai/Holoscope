package de.holoscope.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.holoscope.R;
import de.holoscope.datasets.Dataset;
import de.holoscope.utils.FolderChooseUtils;
import de.holoscope.view.ZoomableImageView;

public class ZoomableImageActivity extends OpenCVActivity {
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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Button dirChooseButton;

        imageType = getIntent().getExtras().getString("type");
        useSlider = getIntent().getExtras().getBoolean("useSlider", true);





        if (useSlider)
            setContentView(R.layout.slider_image_view);
        else if(useSlider && imageType.equals("Data")==false)
            setContentView(R.layout.slider_image_view);
        else
            setContentView(R.layout.image_view);

        imageInfo = (TextView) findViewById(R.id.image_info);

        if (useSlider)
            imageInfo.setText(imageType + " at 0.0 " + Dataset.UNITS);
        else
            imageInfo.setText(imageType);

        imageView = (ZoomableImageView) findViewById(R.id.imageView);


        dirChooseButton = (Button) findViewById(R.id.chooseDirButton);
        //VIEW Hologram

        Log.i("imageType", imageType);
        Log.i("imageType", String.valueOf(imageType.equals("Data")));


        if (useSlider) {
            focusDepth = (SeekBar) findViewById(R.id.focusDepth);
            focusDepth.setEnabled(false);
            focusDepth.setMax(SEEK_SIZE);
            focusDepth.setProgress(SEEK_SIZE / 2);


            dirChooseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create DirectoryChooserDialog and register a callback
                    FolderChooseUtils directoryChooserDialog =
                            new FolderChooseUtils(ZoomableImageActivity.this,
                                    new FolderChooseUtils.ChosenDirectoryListener() {
                                        @Override
                                        public void onChosenDir(String chosenDir) {
                                            m_chosenDir = chosenDir;
                                            global_path = chosenDir;
                                            Toast.makeText(
                                                    ZoomableImageActivity.this, "Chosen directory: " +
                                                            chosenDir, Toast.LENGTH_LONG).show();

                                            if(chosenDir!=null){
                                                //Find all Files in that directory, Provide Filenames as List
                                                File findFile = new File(global_path);
                                                fileList = getListFiles(findFile);
                                                Log.i("List", String.valueOf(fileList));
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





            focusDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


                    if (fileList != null) {

                        float progress_temp = progress;

                        int i = (int) ((progress_temp/100)*fileList.size());


                        if((fileList.size()>i&fileList.size()>=0))
                        {
                            String file = String.valueOf(fileList.get(i));
                            Bitmap bmp = BitmapFactory.decodeFile(file);
                            if (bmp != null) {
                                imageView.setImage(bmp);
                                imageInfo.setText(imageType + " at " + "LED: " + i);
                            }
                        }




                    }

                    else {
                        //TODO doesnt work anymore!
                        int znumber = (int) progress / Dataset.ZVAL;    // z.B. 6/5 = 1 %5=27
                        int lednumber = (int) progress % Dataset.ZVAL;    // z.B. 7%5=2

                        String file = global_path + "/" + imageType + String.format("%d%d", znumber, lednumber) + ".png";


                        Bitmap bmp = BitmapFactory.decodeFile(file);

                        Log.i("Progress", String.valueOf(progress));
                        Log.i("lednumber", "" + lednumber);
                        Log.i("znumber", "" + znumber);
                        Log.i("Imagefolder: ", file);
                        if (bmp != null) {
                            imageView.setImage(bmp);
                            imageInfo.setText(imageType + " at " + "LED: " + (lednumber) + "Z-Value: " + (znumber));
                        }
                    }
                }



                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    //do nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //do nothing
                }
            });

        }

        else {

            dirChooseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create DirectoryChooserDialog and register a callback
                    FolderChooseUtils directoryChooserDialog =
                            new FolderChooseUtils(ZoomableImageActivity.this,
                                    new FolderChooseUtils.ChosenDirectoryListener() {
                                        @Override
                                        public void onChosenDir(String chosenDir) {
                                            m_chosenDir = chosenDir;
                                            Toast.makeText(
                                                    ZoomableImageActivity.this, "Chosen directory: " +
                                                            chosenDir, Toast.LENGTH_LONG).show();
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

            focusDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    String file = global_path + "/" + imageType + String.format("%d%d", 0, 0) + ".png";

                    Bitmap bmp = BitmapFactory.decodeFile(file);

                    if (bmp != null) {
                        imageView.setImage(bmp);
                        imageInfo.setText(imageType + " at " + "LED: " + (0) + "Z-Value: " + (0));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    //do nothing
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //do nothing
                }
            });


        }

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
}
