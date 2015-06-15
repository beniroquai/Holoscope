package de.serenegiant.usbcameratest5;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: MainActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb and jin/libuvc folder may have a different license, see the respective files.
*/

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import de.holoscope.R;
import de.holoscope.activity.GlobalApplicationClass;
import de.holoscope.activity.MainActivity;
import de.holoscope.bluetooth.BluetoothService;
import de.holoscope.datasets.Dataset;
import de.serenegiant.encoder.MediaAudioEncoder;
import de.serenegiant.encoder.MediaEncoder;
import de.serenegiant.encoder.MediaMuxerWrapper;
import de.serenegiant.encoder.MediaVideoEncoder;
import de.serenegiant.usb.IFrameCallback;
import de.serenegiant.usb.USBMonitor;
import de.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import de.serenegiant.usb.USBMonitor.UsbControlBlock;
import de.serenegiant.usb.UVCCamera;
import de.serenegiant.widget.CameraViewInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class MainActivityWebcam extends MainActivity {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MainActivity";

	private int zval, ledval;
	static String type;




	/**
	 * preview resolution(width)
	 * if your camera does not support specific resolution and mode,
	 * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
	 */
	private static final int PREVIEW_WIDTH = 640;
	/**
	 * preview resolution(height)
	 * if your camera does not support specific resolution and mode,
	 * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
	 */
	private static final int PREVIEW_HEIGHT = 480;
	/**
	 * preview mode
	 * if your camera does not support specific resolution and mode,
	 * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
	 * 0:YUYV, other:MJPEG
	 */
	private static final int PREVIEW_MODE = UVCCamera.FRAME_FORMAT_MJPEG;

	/**
	 * for accessing USB
	 */
	private USBMonitor mUSBMonitor;
	/**
	 * Handler to execute camera releated methods sequentially on private thread
	 */
	private CameraHandler mHandler;
	/**
	 * for camera preview display
	 */
	private CameraViewInterface mUVCCameraView;
	/**
	 * for open&start / stop&close camera preview
	 */
	private ToggleButton mCameraButton;
	/**
	 * button for start/stop recording
	 */
	private ImageButton mCaptureButton;
	/**
	 * button for start/stop renaming
	 */
	private Button renameButton;


	private BluetoothService mBluetoothService = null;


	int i,j;



	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		type = getIntent().getExtras().getString("type", "Data");
		ledval = getIntent().getExtras().getInt("LEDVAL", 0);
		zval = getIntent().getExtras().getInt("ZVAL", 0);
		Toast.makeText(getApplicationContext(), "Datatype is"+type, Toast.LENGTH_SHORT).show();


		GlobalApplicationClass BTAppClass = (GlobalApplicationClass) getApplication();
		mBluetoothService = BTAppClass.getBluetoothService();



		if (DEBUG) Log.v(TAG, "onCreate:");
		setContentView(R.layout.activity_main_webcam);
		mCameraButton = (ToggleButton)findViewById(R.id.camera_button);
		mCameraButton.setOnClickListener(mOnClickListener);
		mCaptureButton = (ImageButton)findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(mOnClickListener);
		mCaptureButton.setVisibility(View.INVISIBLE);
		renameButton= (Button)findViewById(R.id.rename_button);
		renameButton.setOnClickListener(mOnClickListener);


		final View view = findViewById(R.id.camera_view);

		mUVCCameraView = (CameraViewInterface)view;
		mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
		mHandler = CameraHandler.createHandler(this, mUVCCameraView);

		//Amarino.connect(this, DatasetHologram.DEVICE_ADDRESS);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume:");
		mUSBMonitor.register();
		if (mUVCCameraView != null)
			mUVCCameraView.onResume();

		//Amarino.connect(this, DatasetHologram.DEVICE_ADDRESS);
	}

	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
//		mHandler.stopRecording();
//		mHandler.stopPreview();
		mHandler.closeCamera();
		if (mUVCCameraView != null)
			mUVCCameraView.onPause();
		mCameraButton.setChecked(false);
		mCaptureButton.setVisibility(View.INVISIBLE);
		mUSBMonitor.unregister();
		super.onPause();
		//Amarino.disconnect(this, DatasetHologram.DEVICE_ADDRESS);
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		if (mHandler != null) {
			mHandler = null;
		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraView = null;
		mCameraButton = null;
		mCaptureButton = null;
		super.onDestroy();

		//Amarino.disconnect(this, DatasetHologram.DEVICE_ADDRESS);

	}

	/**
	 * event handler when click camera / capture button / Initbutton
	 */



	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
				case R.id.camera_button:
					if (!mHandler.isCameraOpened()) {
						CameraDialog.showDialog(MainActivityWebcam.this);
					} else {
						mHandler.closeCamera();
						mCaptureButton.setVisibility(View.INVISIBLE);
						Toast.makeText(getApplicationContext(), "Camera was closed!", Toast.LENGTH_SHORT).show();
					}
					break;
				case R.id.capture_button:
					if (mHandler.isCameraOpened()) {
						final File dir = new File(Dataset.getImageFolder(type));
						DeleteRecursive(dir);
						Toast.makeText(getApplicationContext(), "Pictures Deleted: "+ Dataset.getImageFolder(type), Toast.LENGTH_SHORT).show();

						Toast.makeText(getApplicationContext(), "Start Acquiring Data", Toast.LENGTH_SHORT).show();
						acquireData(ledval, zval, type);
						j=0;

					}
					break;
				case R.id.rename_button:
					renameImages(ledval,zval, type);
			}
		}
	};


	private void acquireData(final int ledMax, final int zMax, String type){



		Log.i("Webcam", "Start Acquiring Data");

		final int ledMax_temp = ledMax;
		final int zMax_temp = zMax;
		final String type_temp = type;

		i=0;




		final Handler handler = new Handler();
		handler.post(new Runnable() {
			@Override
			public void run() {
				Log.i("Incrementor=", String.valueOf(i));

				Dataset.actLEDVAL = i;
				Dataset.actZVAL = j;
				mHandler.captureStill();
				setLED(i);

				i++;

				handler.postDelayed(this, 1500);

				if(i== ledMax){	// If all LEDs passed, go one Z-UP
					j++;
					setZ(500);

				}
				if (i > ledMax+1) {
					i = 0;

					if (j < zval ) {
						acquireData(ledMax, zMax, type_temp); // Erneuter Aufrauf mit neuer Z-Position

					}

					handler.removeCallbacks(null);
					handler.removeMessages(0);
				}

			}

		});


	}


	public void renameImages(int led, int z, String type){

		final File dir = new File(Dataset.getImageFolder(type));
		List<File> renamable = getListFiles(dir);

		Toast.makeText(getApplicationContext(), "Detected Files in Dir: "+renamable.size(), Toast.LENGTH_SHORT).show();

		for(int i_save = 0; i_save <= z; i_save++){
			for(int j_save = 0; j_save< led; j_save++){
				File newFile = new File(dir, type+String.valueOf(i_save)+String.valueOf(j_save)+ ".png");
				Log.i("Filepath Result: ", dir+type+String.valueOf(i_save)+String.valueOf(j_save)+ ".png");
				if((i_save*z+j_save)<=renamable.size()) {
					renamable.get(i_save * z + j_save).renameTo(newFile);
				}
			}
		}

	}

	List<File> getListFiles(File parentDir) {
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


	private static void DeleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				DeleteRecursive(child);

		fileOrDirectory.delete();
	}

	private void setZ(int intZpos){
		if (mBluetoothService != null) {

			try {
				if (intZpos > 10000 || intZpos < -10000) intZpos = 0;
				sendData("zP," + String.valueOf(intZpos) + "_");   //Signal for initilization

				//Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'c', intZpos);
			} catch (NumberFormatException e) {
				//Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'c', 0);
			}
		}
	}
	private void setLED(int lednumber){
		//Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'b', lednumber);

		if (mBluetoothService != null) {


			if (lednumber > Dataset.LEDVAL || lednumber < 0) lednumber = 0;
			try {
				sendData("nL," + String.valueOf(lednumber) + "_");   //Signal for initilization
			} catch (NumberFormatException e) {
				//Amarino.sendDataToArduino(this, DatasetHologram.DEVICE_ADDRESS, 'b', intLED);
			}
		}

	}


	private Surface mSurface;
	private void startPreview() {
		final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
		if (mSurface != null) {
			mSurface.release();
		}
		mSurface = new Surface(st);
		mHandler.startPreview(mSurface);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCaptureButton.setVisibility(View.VISIBLE);
			}
		});
	}

	private final OnDeviceConnectListener mOnDeviceConnectListener=new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			Toast.makeText(MainActivityWebcam.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (DEBUG) Log.v(TAG, "onConnect:");
			mHandler.openCamera(ctrlBlock);
			startPreview();
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			if (DEBUG) Log.v(TAG, "onDisconnect:");
			if (mHandler != null) {
				mHandler.closeCamera();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mCaptureButton.setVisibility(View.INVISIBLE);
						mCameraButton.setChecked(false);
					}
				});
			}
		}
		@Override
		public void onDettach(final UsbDevice device) {
			Toast.makeText(MainActivityWebcam.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel() {
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	/**
	 * Handler class to execute camera releated methods sequentially on private thread
	 */
	private static final class CameraHandler extends Handler {
		private static final int MSG_OPEN = 0;
		private static final int MSG_CLOSE = 1;
		private static final int MSG_PREVIEW_START = 2;
		private static final int MSG_PREVIEW_STOP = 3;
		private static final int MSG_CAPTURE_STILL = 4;
		private static final int MSG_CAPTURE_START = 5;
		private static final int MSG_CAPTURE_STOP = 6;
		private static final int MSG_MEDIA_UPDATE = 7;
		private static final int MSG_RELEASE = 9;


		private final WeakReference<CameraThread> mWeakThread;

		public static final CameraHandler createHandler(final MainActivityWebcam parent, final CameraViewInterface cameraView) {
			final CameraThread thread = new CameraThread(parent, cameraView);
			thread.start();
			return thread.getHandler();
		}

		private CameraHandler(final CameraThread thread) {
			mWeakThread = new WeakReference<CameraThread>(thread);
		}

		public boolean isCameraOpened() {
			final CameraThread thread = mWeakThread.get();
			return thread != null ? thread.isCameraOpened() : false;
		}

		public boolean isRecording() {
			final CameraThread thread = mWeakThread.get();
			return thread != null ? thread.isRecording() :false;
		}

		public void openCamera(final UsbControlBlock ctrlBlock) {
			sendMessage(obtainMessage(MSG_OPEN, ctrlBlock));
		}

		public void closeCamera() {
			stopPreview();
			sendEmptyMessage(MSG_CLOSE);
		}

		public void startPreview(final Surface sureface) {
			if (sureface != null)
				sendMessage(obtainMessage(MSG_PREVIEW_START, sureface));
		}

		public void stopPreview() {
			stopRecording();
			final CameraThread thread = mWeakThread.get();
			if (thread == null) return;
			synchronized (thread.mSync) {
				sendEmptyMessage(MSG_PREVIEW_STOP);
				// wait for actually preview stopped to avoid releasing Surface/SurfaceTexture
				// while preview is still running.
				// therefore this method will take a time to execute
				try {
					thread.mSync.wait();
				} catch (final InterruptedException e) {
				}
			}
		}

		public void captureStill() {
			sendEmptyMessage(MSG_CAPTURE_STILL);
		}

		public void startRecording() {
			sendEmptyMessage(MSG_CAPTURE_START);
		}

		public void stopRecording() {
			sendEmptyMessage(MSG_CAPTURE_STOP);
		}

		@Override
		public void handleMessage(final Message msg) {
			final CameraThread thread = mWeakThread.get();
			if (thread == null) return;
			switch (msg.what) {
				case MSG_OPEN:
					thread.handleOpen((UsbControlBlock)msg.obj);
					break;
				case MSG_CLOSE:
					thread.handleClose();
					break;
				case MSG_PREVIEW_START:
					thread.handleStartPreview((Surface)msg.obj);
					break;
				case MSG_PREVIEW_STOP:
					thread.handleStopPreview();
					break;
				case MSG_CAPTURE_STILL:

					thread.handleCaptureStill();
					break;
				case MSG_CAPTURE_START:
					thread.handleStartRecording();
					break;
				case MSG_CAPTURE_STOP:
					thread.handleStopRecording();
					break;
				case MSG_MEDIA_UPDATE:
					thread.handleUpdateMedia((String)msg.obj);
					break;
				case MSG_RELEASE:
					thread.handleRelease();
					break;
				default:
					throw new RuntimeException("unsupported message:what=" + msg.what);
			}
		}


		private static final class CameraThread extends Thread {
			private static final String TAG_THREAD = "CameraThread";
			private final Object mSync = new Object();
			private final WeakReference<MainActivityWebcam> mWeakParent;
			private final WeakReference<CameraViewInterface> mWeakCameraView;
			private boolean mIsRecording;
			/**
			 * shutter sound
			 */
			private SoundPool mSoundPool;
			private int mSoundId;
			private CameraHandler mHandler;
			/**
			 * for accessing UVC camera
			 */
			private UVCCamera mUVCCamera;
			/**
			 * muxer for audio/video recording
			 */
			private MediaMuxerWrapper mMuxer;
			/**
			 * for video recording
			 */
			private MediaVideoEncoder mVideoEncoder;

			private CameraThread(final MainActivityWebcam parent, final CameraViewInterface cameraView) {
				super("CameraThread");
				mWeakParent = new WeakReference<MainActivityWebcam>(parent);
				mWeakCameraView = new WeakReference<CameraViewInterface>(cameraView);
				loadSutterSound(parent);
			}

			@Override
			protected void finalize() throws Throwable {
				Log.i(TAG, "CameraThread#finalize");
				super.finalize();
			}

			public CameraHandler getHandler() {
				if (DEBUG) Log.v(TAG_THREAD, "getHandler:");
				synchronized (mSync) {
					if (mHandler == null)
						try {
							mSync.wait();
						} catch (final InterruptedException e) {
						}
				}
				return mHandler;
			}

			public boolean isCameraOpened() {
				return mUVCCamera != null;
			}

			public boolean isRecording() {
				return (mUVCCamera != null) && (mMuxer != null);
			}

			public void handleOpen(final UsbControlBlock ctrlBlock) {
				if (DEBUG) Log.v(TAG_THREAD, "handleOpen:");
				handleClose();
				mUVCCamera = new UVCCamera();
				mUVCCamera.open(ctrlBlock);
			}

			public void handleClose() {
				if (DEBUG) Log.v(TAG_THREAD, "handleClose:");
				handleStopRecording();
				if (mUVCCamera != null) {
					mUVCCamera.stopPreview();
					mUVCCamera.destroy();
					mUVCCamera = null;
				}
			}

			public void handleStartPreview(final Surface surface) {
				if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
				if (mUVCCamera == null) return;
				try {
					mUVCCamera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
				} catch (final IllegalArgumentException e) {
					try {
						// fallback to YUV mode
						mUVCCamera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
					} catch (final IllegalArgumentException e1) {
						handleClose();
					}
				}
				if (mUVCCamera != null) {
					mUVCCamera.setPreviewDisplay(surface);
					mUVCCamera.startPreview();
				}
			}

			public void handleStopPreview() {
				if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
				if (mUVCCamera != null) {
					mUVCCamera.stopPreview();
				}
				synchronized (mSync) {
					mSync.notifyAll();
				}
			}

			public void handleCaptureStill() {
				if (DEBUG) Log.v(TAG_THREAD, "handleCaptureStill:");
				final MainActivityWebcam parent = mWeakParent.get();
				if (parent == null) return;
				mSoundPool.play(mSoundId, 0.2f, 0.2f, 0, 0, 1.0f);	// play shutter sound
				final Bitmap bitmap = mWeakCameraView.get().captureStillImage();
				try {
					// get buffered output stream for saving a captured still image as a file on external storage.
					// the file name is came from current time.
					// You should use extension name as same as CompressFormat when calling Bitmap#compress.

					//Get Filepath for saving images from DatasetHologram.java:
                    /*
                    public static String getFilenameWebcam(String datafilename, int depth, int led) {
                    return datafilename + String.valueOf(depth) + String.valueOf(led)  + ".png";
                    }
                    public static String getPathWebcam(String datapath) {
                    return DATASET_PATH + datapath + "/";
                    }
                    */
					//Log.e("Filepath: ",  Dataset.getPathWebcam(Dataset.DATA_PATH)+ Dataset.getFilenameWebcam(Dataset.DATA_TYPE, Dataset.actZVAL, Dataset.actLEDVAL));

//DatasetHologram.getFilenameWebcam(DatasetHologram.DATA_TYPE, DatasetHologram.actZVAL, DatasetHologram.actLEDVAL
					final File outputFile = MediaMuxerWrapper.getCaptureFile(Dataset.getImageFolder(type), type+String.valueOf(Dataset.actZVAL)+String.valueOf(Dataset.actLEDVAL));
					final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));

					try {
						try {
							bitmap.compress(CompressFormat.PNG, 100, os);
							os.flush();
							mHandler.sendMessage(mHandler.obtainMessage(MSG_MEDIA_UPDATE, outputFile.getPath()));
						} catch (final IOException e) {
						}
					} finally {
						os.close();
					}
				} catch (final FileNotFoundException e) {
				} catch (final IOException e) {
				}
			}

			public void handleStartRecording() {
				if (DEBUG) Log.v(TAG_THREAD, "handleStartRecording:");
				try {
					if ((mUVCCamera == null) || (mMuxer != null)) return;
					mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.
					// for video capturing using MediaVideoEncoder
					mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener);
					if (true) {
						// for audio capturing
						new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
					}
					mMuxer.prepare();
					mMuxer.startRecording();
					mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
				} catch (final IOException e) {
					Log.e(TAG, "startCapture:", e);
				}
			}

			public void handleStopRecording() {
				if (DEBUG) Log.v(TAG_THREAD, "handleStopRecording:mMuxer=" + mMuxer);
				mVideoEncoder = null;
				if (mMuxer != null) {
					mMuxer.stopRecording();
					mMuxer = null;
					// you should not wait here
				}
				if (mUVCCamera != null)
					mUVCCamera.setFrameCallback(null, 0);
			}

			public void handleUpdateMedia(final String path) {
				if (DEBUG) Log.v(TAG_THREAD, "handleUpdateMedia:path=" + path);
				final MainActivityWebcam parent = mWeakParent.get();
				if (parent != null && parent.getApplicationContext() != null) {
					try {
						if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
						MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{ path }, null, null);
					} catch (final Exception e) {
						Log.e(TAG, "handleUpdateMedia:", e);
					}
					if (parent.isDestroyed())
						handleRelease();
				} else {
					Log.w(TAG, "MainActivity already destroyed");
					// give up to add this movice to MediaStore now.
					// Seeing this movie on Gallery app etc. will take a lot of time.
					handleRelease();
				}
			}

			public void handleRelease() {
				if (DEBUG) Log.v(TAG_THREAD, "handleRelease:");
				handleClose();
				if (!mIsRecording)
					Looper.myLooper().quit();
			}

			private final IFrameCallback mIFrameCallback = new IFrameCallback() {
				@Override
				public void onFrame(final ByteBuffer frame) {
					if (mVideoEncoder != null) {
						mVideoEncoder.frameAvailableSoon();
						mVideoEncoder.encode(frame);
					}
				}
			};

			private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
				@Override
				public void onPrepared(final MediaEncoder encoder) {
					if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
					mIsRecording = true;
				}

				@Override
				public void onStopped(final MediaEncoder encoder) {
					if (DEBUG) Log.v(TAG_THREAD, "onStopped:encoder=" + encoder);
					if (encoder instanceof MediaVideoEncoder)
						try {
							mIsRecording = false;
							final MainActivityWebcam parent = mWeakParent.get();
							final String path = encoder.getOutputPath();
							if (!TextUtils.isEmpty(path)) {
								mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_MEDIA_UPDATE, path), 1000);
							} else {
								if (parent == null || parent.isDestroyed()) {
									handleRelease();
								}
							}
						} catch (final Exception e) {
							Log.e(TAG, "onPrepared:", e);
						}
				}
			};

			/**
			 * prepare and load shutter sound for still image capturing
			 */
			@SuppressWarnings("deprecation")
			private void loadSutterSound(final Context context) {
				// get system stream type using refrection
				int streamType;
				try {
					final Class<?> audioSystemClass = Class.forName("android.media.AudioSystem");
					final Field sseField = audioSystemClass.getDeclaredField("STREAM_SYSTEM_ENFORCED");
					streamType = sseField.getInt(null);
				} catch (final Exception e) {
					streamType = AudioManager.STREAM_SYSTEM;	// set appropriate according to your app policy
				}
				if (mSoundPool != null) {
					try {
						mSoundPool.release();
					} catch (final Exception e) {
					}
					mSoundPool = null;
				}
				// load sutter sound from resource
				mSoundPool = new SoundPool(2, streamType, 0);
				mSoundId = mSoundPool.load(context, R.raw.camera_click, 1);
			}

			@Override
			public void run() {
				Looper.prepare();
				synchronized (mSync) {
					mHandler = new CameraHandler(this);
					mSync.notifyAll();
				}
				Looper.loop();
				synchronized (mSync) {
					mHandler = null;
					mSoundPool.release();
					mSoundPool = null;
					mSync.notifyAll();
				}
			}
		}
	}
}
