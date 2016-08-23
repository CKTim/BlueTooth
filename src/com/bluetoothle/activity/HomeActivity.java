package com.bluetoothle.activity;


import com.bluetoothle.service.BluetoothLeService;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class HomeActivity extends Activity implements OnClickListener {
	private Button btn_SampleDetection, btn_QueryRecord, btn_SystemSettings, btn_ifConnected, btn_back;
	private boolean result;
	private String mDeviceName;
	private String mDeviceAddress;
	private Handler mhandler = new Handler();
	private Runnable runnable;
	private Thread mThread;
	private SharedPreferences sp;
	private Editor editor;
	// private Runnable runnable;
	private BluetoothLeService mBluetoothLeService;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	// ������������������
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			// �ж��Ƿ��Ѿ���ʼ����������
			if (!mBluetoothLeService.initialize()) {
				Log.e("a", "�޷���ʼ������");
				finish();
			}
			// �Զ����ӵ�װ���ϳɹ�������ʼ����
			result = mBluetoothLeService.connect(mDeviceAddress);
		}

		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
			Log.e("a", "����ֹͣ");
		}
	};

	// ����һ���㲥�������������մ�Bluetooth�����෵�ع����Ĺ㲥
	private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				result = true;
				Log.e("AAAAA", "���յ����ӳɹ��Ĺ㲥");
				btn_ifConnected.setBackground(getResources().getDrawable(R.drawable.connected));
				mhandler.removeCallbacks(runnable);
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				result = false;
				Log.e("AAAAA", "���յ�����ʧ�ܵĹ㲥");
				btn_ifConnected.setBackground(getResources().getDrawable(R.drawable.disconnected));
				// ����ʧ�ܺ�ʹ��һ����ʱ��������������
				runnable = new Runnable() {

					@Override
					public void run() {
						if (result == false) {
							Log.e("AAAAAA", "����ʧ�ܣ�ÿ10s������������");
							// �ж��Ƿ��Ѿ���ʼ����������
							if (!mBluetoothLeService.initialize()) {
								Log.e("a", "�޷���ʼ������");
								finish();
							}
							// ������������
							mBluetoothLeService.connect(mDeviceAddress);
							mhandler.postDelayed(this, 10000);
						}
					}
				};
				mThread = new Thread(runnable);
				mThread.start();

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

			} else if (BluetoothLeService.EXTRA_DATA.equals(action)) {
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] bb = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		// ��ȡ��SearchDeviceActivity���ݹ�����name��address
		Intent intent = getIntent();
		mDeviceName = intent.getStringExtra("DeviceName");
		mDeviceAddress = intent.getStringExtra("DeviceAddress");
		// ע�����
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private void init() {
		btn_SampleDetection = (Button) this.findViewById(R.id.btn_SampleDetection);
		btn_QueryRecord = (Button) this.findViewById(R.id.btn_QueryRecord);
		btn_SystemSettings = (Button) this.findViewById(R.id.btn_SystemSettings);
		btn_ifConnected = (Button) this.findViewById(R.id.btn_connected);
		btn_back = (Button) this.findViewById(R.id.btn_homeBack);
		btn_SampleDetection.setOnClickListener(this);
		btn_QueryRecord.setOnClickListener(this);
		btn_SystemSettings.setOnClickListener(this);
		btn_ifConnected.setOnClickListener(this);
		btn_back.setOnClickListener(this);
//		//��װ�ظ�Ĭ�ϵļ��ʱ��ͼ��ֵ
//		sp = this.getSharedPreferences("LZ-4000(T)", this.MODE_PRIVATE);
//        editor=sp.edit();
//		editor.putFloat("CompareValue",(float)0);
//		editor.putString("DetectionTime", "20");
//		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// ע��㲥
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBluetoothLeService.disconnect();
		mhandler.removeCallbacks(runnable);
		unbindService(mServiceConnection);
	}

	// ����һ��������
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_RSSI);
		intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
		return intentFilter;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_SampleDetection:
			Intent intent0 = new Intent(HomeActivity.this, FunctionActivity.class);
			intent0.putExtra("type", "a");
			startActivity(intent0);
			break;

		case R.id.btn_QueryRecord:
			Intent intent1 = new Intent(HomeActivity.this, FunctionActivity.class);
			intent1.putExtra("type", "b");
			startActivity(intent1);

			break;
		case R.id.btn_SystemSettings:
			Intent intent2 = new Intent(HomeActivity.this, FunctionActivity.class);
			intent2.putExtra("type", "c");
			startActivity(intent2);
			break;
		case R.id.btn_connected:

			break;
		case R.id.btn_homeBack:
			this.finish();
			break;
		}

	}

}
