package com.bluetoothle.activity;

import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UdpSettingActivity extends Activity {
	private Button btn_sure;
	private EditText et_port, et_ip1, et_ip2, et_ip3, et_ip4;
	private String ip1, ip2, ip3, ip4,port;
	private BluetoothLeService mBluetoothLeService;
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
				// 以太网设置返回
				if (byteData[0] == 0x7e && byteData[1] == 0x27 && byteData[4] == 0x01) {
					Toast.makeText(UdpSettingActivity.this, "UDP服务器设置成功为", 0).show();
					UdpSettingActivity.this.finish();
				} else if (byteData[0] == 0x7e && byteData[1] == 0x27 && byteData[4] == 0x00) {
					Toast.makeText(UdpSettingActivity.this, "UDP服务器设置失败", 0).show();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		super.onCreate(savedInstanceState);
		ActivityManager.getActivityManager().addActivity(this);
		setContentView(R.layout.activity_udpsetting);
		// 绑定服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		this.bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);
		init();
		btn_sure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				port=et_port.getText().toString().trim();
				ip1 = et_ip1.getText().toString().trim();
				ip2 = et_ip2.getText().toString().trim();
				ip3 = et_ip3.getText().toString().trim();
				ip4 = et_ip4.getText().toString().trim();
				if (ip1.equals("") || ip2.equals("") || ip3.equals("") || ip4.equals("")||port.equals("")) {
					Toast.makeText(UdpSettingActivity.this, "有数据没填完整", 0).show();
				} else {
					byte[] dataByte = new byte[12];
					byte[] headByte = new byte[] { 0x7E, 0x26, 0x06, 0x00 };
					byte[] middleByte = new byte[6];
					byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
					String hexPort=Integer.toHexString(Integer.parseInt(port));
					String hexPort1=hexPort.substring(0, 2);
					String hexPort2=hexPort.substring(2);
					String sendPort=hexPort2+hexPort1;
					String ipData =DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip4)));
					middleByte=DecodeUtils.HexString2Bytes(sendPort+ipData);
					System.arraycopy(headByte, 0, dataByte, 0, 4);
					System.arraycopy(middleByte, 0, dataByte,4, 6);
					System.arraycopy(tailByte, 0, dataByte, 10, 2);
					mBluetoothLeService.write(dataByte);
				}
			}
		});
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
		this.unregisterReceiver(mGattUpdateReceiver);
		this.unbindService(mServiceConnection);
		Log.e("dia", "ondestroy");
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
		return intentFilter;
	}

	private void init() {
		btn_sure = (Button) this.findViewById(R.id.btn_sure);
		et_port = (EditText) this.findViewById(R.id.et_port);
		et_ip1 = (EditText) this.findViewById(R.id.et_ip1);
		et_ip2 = (EditText) this.findViewById(R.id.et_ip3);
		et_ip3 = (EditText) this.findViewById(R.id.et_ip5);
		et_ip4 = (EditText) this.findViewById(R.id.et_ip7);

	}
}
