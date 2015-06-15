package de.serenegiant.usbcameratest5;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 * 
 * Copyright (c) 2014 saki t_saki@serenegiant.com
 * 
 * File name: CameraDialog.java
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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Spinner;

import de.holoscope.R;
import de.serenegiant.usb.DeviceFilter;
import de.serenegiant.usb.USBMonitor;

public class CameraDialog extends DialogFragment {
	private static final String TAG = CameraDialog.class.getSimpleName();
	
	/**
	 * Helper method
	 * @param parent FragmentActivity
	 * @return
	 */
	public static CameraDialog showDialog(Activity parent/* add parameters here if you need */) {
		CameraDialog dialog = newInstance(/* add parameters here if you need */);
		try {
			dialog.show(parent.getFragmentManager(), TAG);
		} catch (IllegalStateException e) {
			dialog = null;
		}
    	return dialog;
	}

	/**
	 * ヘルパーメソッド
	 * @param parent Fragment
	 * @return
	 */
	public static CameraDialog showDialog(Fragment parent) {
		CameraDialog dialog = newInstance();
		dialog.setTargetFragment(parent, 1);
		try {
			dialog.show(parent.getFragmentManager(), TAG);
		} catch (IllegalStateException e) {
			dialog = null;
		}
    	return dialog;
	}

	public static CameraDialog newInstance(/* add parameters here if you need */) {
		final CameraDialog dialog = new CameraDialog();
		final Bundle args = new Bundle();
		// add parameters here if you need
		dialog.setArguments(args);
		return dialog;
	}
	
	protected USBMonitor mUSBMonitor;
	private Spinner mSpinner;
	private DeviceListAdapter mDeviceListAdapter;

	public CameraDialog(/* no arguments */) {
		// Fragment need default constructor
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (mUSBMonitor == null)
        try {
    		mUSBMonitor = ((MainActivityWebcam)activity).getUSBMonitor();
        } catch (ClassCastException e) {
        	Log.w(TAG, "onAttach:", e);
    	} catch (NullPointerException e) {
        	Log.w(TAG, "onAttach:", e);
        }
		if (mUSBMonitor == null) {
        	throw new ClassCastException(activity.toString() + " must implement #getUSBMonitor");
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null)
			savedInstanceState = getArguments();
	}

	@Override
	public void onSaveInstanceState(Bundle saveInstanceState) {
		final Bundle args = getArguments();
		if (args != null)
			saveInstanceState.putAll(args);
		super.onSaveInstanceState(saveInstanceState);
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(initView());
    	builder.setTitle(R.string.select);
	    builder.setPositiveButton(android.R.string.ok, mOnDialogClickListener);
	    builder.setNegativeButton(android.R.string.cancel , mOnDialogClickListener);
	    builder.setNeutralButton(R.string.refresh, null);
	    final Dialog dialog = builder.create();
	    dialog.setCancelable(true);
	    dialog.setCanceledOnTouchOutside(true);
        return dialog;
	}

	/**
	 * create view that this fragment shows
	 * @return
	 */
	private final View initView() {
		final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_camera, null);
		mSpinner = (Spinner)rootView.findViewById(R.id.spinner1);
		final View empty = rootView.findViewById(android.R.id.empty);
		mSpinner.setEmptyView(empty);
		return rootView;
	}


	@Override
	public void onResume() {
		super.onResume();
		updateDevices();
	    final Button button = (Button)getDialog().findViewById(android.R.id.button3);
	    if (button != null) {
	    	button.setOnClickListener(mOnClickListener);
	    }
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case android.R.id.button3:
				updateDevices();
				break;
			}
		}
	};
	
	private final DialogInterface.OnClickListener mOnDialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				final Object item = mSpinner.getSelectedItem();
				if (item instanceof UsbDevice) {
					mUSBMonitor.requestPermission((UsbDevice)item);
				}
				break;
			}
		}
	};

	public void updateDevices() {
//		mUSBMonitor.dumpDevices();
		final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
		mDeviceListAdapter = new DeviceListAdapter(getActivity(), mUSBMonitor.getDeviceList(filter.get(0)));
		mSpinner.setAdapter(mDeviceListAdapter);
	}
	
	private static final class DeviceListAdapter extends BaseAdapter {

		private final LayoutInflater mInflater;
		private final List<UsbDevice> mList;

		public DeviceListAdapter(Context context, List<UsbDevice>list) {
			mInflater = LayoutInflater.from(context);
			mList = list != null ? list : new ArrayList<UsbDevice>();
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public UsbDevice getItem(int position) {
			if ((position >= 0) && (position < mList.size()))
				return mList.get(position);
			else
				return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listitem_device, parent, false);
			}
			if (convertView instanceof CheckedTextView) {
				final UsbDevice device = getItem(position);
				//((CheckedTextView)convertView).setText(String.format("UVC Camera:(%x:%x)", device.getVendorId(), device.getProductId()));
			}
			return convertView;
		}
	}
}
