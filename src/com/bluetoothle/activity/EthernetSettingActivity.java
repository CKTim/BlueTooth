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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class EthernetSettingActivity extends Activity {
	private EditText et_ip1, et_ip2, et_ip3, et_ip4, et_netmask1, et_netmask2, et_netmask3, et_netmask4, et_gateway1, et_gateway2, et_gateway3, et_gateway4;
	private CheckBox cb_isSelect;
	private Button btn_sure;
	private String ip1, ip2, ip3, ip4, netmask1, netmask2, netmask3, netmask4, gateway1, gateway2, gateway3, gateway4;
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
				if (byteData[0] == 0x7e && byteData[1] == 0x25 && byteData[4] == 0x01) {
					Toast.makeText(EthernetSettingActivity.this, "以太网设置成功", 0).show();
					EthernetSettingActivity.this.finish();
				} else if (byteData[0] == 0x7e && byteData[1] == 0x25 && byteData[4] == 0x00) {
					Toast.makeText(EthernetSettingActivity.this, "以太网设置失败", 0).show();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_ethernetsetting);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		// 绑定服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		this.bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);
		// 确定按钮监听事件
		btn_sure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ip1 = et_ip1.getText().toString().trim();
				ip2 = et_ip2.getText().toString().trim();
				ip3 = et_ip3.getText().toString().trim();
				ip4 = et_ip4.getText().toString().trim();
				netmask1 = et_netmask1.getText().toString().trim();
				netmask2 = et_netmask2.getText().toString().trim();
				netmask3 = et_netmask3.getText().toString().trim();
				netmask4 = et_netmask4.getText().toString().trim();
				gateway1 = et_gateway1.getText().toString().trim();
				gateway2 = et_gateway2.getText().toString().trim();
				gateway3 = et_gateway3.getText().toString().trim();
				gateway4 = et_gateway4.getText().toString().trim();
				if (ip1.equals("") || ip2.equals("") || ip3.equals("") || ip4.equals("") || netmask1.equals("") || netmask2.equals("") || netmask3.equals("")
						|| netmask4.equals("") || gateway1.equals("") || gateway2.equals("") || gateway3.equals("") || gateway4.equals("")) {
                       Toast.makeText(EthernetSettingActivity.this, "有数据没填", 0).show();
				}else{
				byte[] dataByte = new byte[19];
				byte[] headByte = new byte[] { 0x7E, 0x24, 0x0D, 0x00 };
				byte[] middleByte = new byte[13];
				byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
				String data = "";
				if (cb_isSelect.isChecked()) {
					data = "01" + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip4)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask4)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway4)));
				} else {
					data = "00" + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(ip4)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(netmask4)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway1))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway2)))
							+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway3))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(gateway4)));
				}
				middleByte = DecodeUtils.HexString2Bytes(data);
				System.arraycopy(headByte, 0, dataByte, 0, 4);
				System.arraycopy(middleByte, 0, dataByte, 4, 13);
				System.arraycopy(tailByte, 0, dataByte, 17, 2);
				mBluetoothLeService.write(dataByte);
				Log.e("AAAAAAAA", DecodeUtils.byte2HexStr(dataByte) + "");
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
		et_ip1 = (EditText) this.findViewById(R.id.et_ip1);
		et_ip2 = (EditText) this.findViewById(R.id.et_ip3);
		et_ip3 = (EditText) this.findViewById(R.id.et_ip5);
		et_ip4 = (EditText) this.findViewById(R.id.et_ip7);
		et_netmask1 = (EditText) this.findViewById(R.id.et_netmask1);
		et_netmask2 = (EditText) this.findViewById(R.id.et_netmask3);
		et_netmask3 = (EditText) this.findViewById(R.id.et_netmask5);
		et_netmask4 = (EditText) this.findViewById(R.id.et_netmask7);
		et_gateway1 = (EditText) this.findViewById(R.id.et_gateway1);
		et_gateway2 = (EditText) this.findViewById(R.id.et_gateway3);
		et_gateway3 = (EditText) this.findViewById(R.id.et_gateway5);
		et_gateway4 = (EditText) this.findViewById(R.id.et_gateway7);
		cb_isSelect = (CheckBox) this.findViewById(R.id.cb_ifAuto);
		btn_sure = (Button) this.findViewById(R.id.btn_sure);

	}
}
