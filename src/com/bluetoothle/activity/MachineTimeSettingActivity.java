package com.bluetoothle.activity;

import java.util.Calendar;

import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class MachineTimeSettingActivity extends Activity implements OnClickListener {
	private Button btn_customTime, btn_synchronizationNowTime;
	private TextView tv_nowTime;
	private int mYear, mMonth, mDay, mHour, mMin, mSecond;
	private Handler mHandler = new Handler();
	private BluetoothLeService mBluetoothLeService;
	private Runnable runnable;
	// 代码管理服务生命周期
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

		}

		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};
	// 建立一个广播接受者用来接收从Bluetooth服务类返回过来的广播
	private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothLeService.EXTRA_DATA.equals(action)) {
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] byteData = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
				// Wifi设置返回
				if (byteData[0] == 0x7e && byteData[1] == 0x21 && byteData[4] == 0x01) {
					Toast.makeText(MachineTimeSettingActivity.this, "仪器时间设置成功", 0).show();
					MachineTimeSettingActivity.this.finish();
				} else if (byteData[0] == 0x7e && byteData[1] == 0x21 && byteData[4] == 0x00) {
					Toast.makeText(MachineTimeSettingActivity.this, "仪器时间设置失败", 0).show();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_mtimesetting);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		// 每一秒获取一次时间
		runnable = new Runnable() {

			@Override
			public void run() {
				mHandler.postDelayed(this, 1000);
				// 获取当前时间
				Calendar c = Calendar.getInstance();
				mYear = c.get(Calendar.YEAR);
				mMonth = c.get(Calendar.MONTH) + 1;
				mDay = c.get(Calendar.DATE);
				mHour = c.get(Calendar.HOUR_OF_DAY);
				mMin = c.get(Calendar.MINUTE);
				mSecond = c.get(Calendar.SECOND);
				tv_nowTime.setText(mYear + "/" + mMonth + "/" + mDay + "   " + mHour + ":" + mMin + ":" + mSecond);

			}
		};
		mHandler.postDelayed(runnable, 1000);
		// 绑定服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		this.bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 注册广播
		this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(runnable);
		this.unregisterReceiver(mGattUpdateReceiver);
		this.unbindService(mServiceConnection);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
		return intentFilter;
	}

	private void init() {
		btn_customTime = (Button) this.findViewById(R.id.btn_customTime);
		btn_synchronizationNowTime = (Button) this.findViewById(R.id.btn_synchronizationNowTime);
		tv_nowTime = (TextView) this.findViewById(R.id.tv_displayTime);
		btn_customTime.setOnClickListener(this);
		btn_synchronizationNowTime.setOnClickListener(this);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_customTime:
                  new DatePickerDialog(this, new OnDateSetListener() {
					
					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
						byte[] dataByte = new byte[12];
						byte[] headByte = new byte[] { 0x7E, 0x20, 0x06, 0x00 };
						byte[] timeByte = new byte[6];
						byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
						String NowTime= "10"
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(monthOfYear+1))
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(dayOfMonth))
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mHour))
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mMin))
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mSecond));
						timeByte=DecodeUtils.HexString2Bytes(NowTime);
						System.arraycopy(headByte, 0, dataByte, 0, 4);
						System.arraycopy(timeByte, 0, dataByte, 4, 6);
						System.arraycopy(tailByte, 0, dataByte, 10, 2);
						Log.e("AAAAAAA",DecodeUtils.byte2HexStr(dataByte)+"");
						mBluetoothLeService.write(dataByte);
						
					}
				}, mYear, mMonth-1, mDay).show();
			break;

		case R.id.btn_synchronizationNowTime:
			byte[] dataByte = new byte[12];
			byte[] headByte = new byte[] { 0x7E, 0x20, 0x06, 0x00 };
			byte[] timeByte = new byte[6];
			byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
			String NowTime= "10"
					+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mMonth))
					+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mDay))
					+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mHour))
					+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mMin))
					+ DecodeUtils.AddZeroToTwo(Integer.toHexString(mSecond));
			timeByte=DecodeUtils.HexString2Bytes(NowTime);
			System.arraycopy(headByte, 0, dataByte, 0, 4);
			System.arraycopy(timeByte, 0, dataByte, 4, 6);
			System.arraycopy(tailByte, 0, dataByte, 10, 2);
			Log.e("AAAAAAA",DecodeUtils.byte2HexStr(dataByte)+"");
			mBluetoothLeService.write(dataByte);
			break;
		}
	}
}
