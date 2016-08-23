package com.bluetoothle.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SearchDeviceActivity extends Activity {
	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler = new Handler();
	private ListView lv_searchDevice;
	private MyBaseAdapter myBaseAdapter = new MyBaseAdapter();
	private List<BluetoothDevice> listDevice = new ArrayList<BluetoothDevice>();
	private List<String> listDeviceName=new ArrayList<String>();
	private final static int REQUEST_ENABLE_BT = 1;
	private long firstTime = 0;
	// 开始扫描的接口回调,返回扫描回来的device
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!listDeviceName.contains(device.getName())&&device.getName()!=null) {
						listDevice.add(device);
						listDeviceName.add(device.getName());
						myBaseAdapter.notifyDataSetChanged();
					}

				}
			});

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_searchdevice);
		ActivityManager.getActivityManager().addActivity(this);
		init();

		// 检查设备是否支持BLE蓝牙设备
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "不支持BLE蓝牙设备", Toast.LENGTH_SHORT).show();
			finish();
		}

		// 检查是否支持蓝牙的设备。
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "设备不支持", Toast.LENGTH_SHORT).show();
		} else {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 1);
			} else {
				// 判断如果已经打开蓝牙则开始搜索
				scanLeDevice(true);
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		} else if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_OK) {
			scanLeDevice(true);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void init() {
		lv_searchDevice = (ListView) this.findViewById(R.id.lv_searchDevice);
		lv_searchDevice.setAdapter(myBaseAdapter);
		lv_searchDevice.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(SearchDeviceActivity.this,
						HomeActivity.class);
				intent.putExtra("DeviceName", listDevice.get(position)
						.getName());
				intent.putExtra("DeviceAddress", listDevice.get(position)
						.getAddress());
				startActivity(intent);
				scanLeDevice(false);
			}
		});
		// 初始化一个蓝牙适配器。对API 18级以上，可以参考 bluetoothmanager。
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, 20000);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}

	// 自定义一个适配器
	public class MyBaseAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return listDevice.size();
		}

		@Override
		public Object getItem(int position) {
			return listDevice.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder = null;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = LayoutInflater.from(SearchDeviceActivity.this)
						.inflate(R.layout.lv_searchdevice_item, null);
				viewHolder.tv_DeviceName = (TextView) convertView
						.findViewById(R.id.tv_DeviceName);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tv_DeviceName
					.setText(listDevice.get(position).getName());
			return convertView;
		}

		public class ViewHolder {
			TextView tv_DeviceName;
		}

	}
	
	//返回键监听
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			long secondTime=System.currentTimeMillis();
			if(secondTime-firstTime>2000){
				 Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show(); 
				 firstTime=secondTime;
				 return true;
			}else{
				ActivityManager.getActivityManager().finishAllActivity();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
