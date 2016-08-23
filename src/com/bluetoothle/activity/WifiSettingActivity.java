package com.bluetoothle.activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;

import cm.example.lz_4000tbluetooth.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WifiSettingActivity extends Activity implements OnClickListener {
	private RelativeLayout rl_back, rl_content;
	private ImageView iv_wifi;
	private TextView tv_backgroud, tv_wifiName;
	private ListView lv;
	private View customAlertDialogView;
	private EditText et_password;
	private Button btn_sure, btn_dismiss;
	private List<ScanResult> mWifiList = new ArrayList<ScanResult>();
	private List<WifiConfiguration> mWifiConfigList = new ArrayList<WifiConfiguration>();
	private MyBaseAdapter mAdapter = new MyBaseAdapter();
	private WifiManager mWifiManager;
	private AlertDialog alertDialog;
	private BluetoothLeService mBluetoothLeService;
	private String WifiName;
	private String PassWord;
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
				if (byteData[0] == 0x7e && byteData[1] == 0x23 && byteData[4] == 0x01) {
					Toast.makeText(WifiSettingActivity.this, "wifi设置成功", 0).show();
					alertDialog.dismiss();
				} else if (byteData[0] == 0x7e && byteData[1] == 0x23 && byteData[4] == 0x00) {
					Toast.makeText(WifiSettingActivity.this, "wifi设置失败", 0).show();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wifisetting);
		init();
		// listview点击监听事件
		lv.setOnItemClickListener(new OnItemClickListener() {

			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				customAlertDialogView = LayoutInflater.from(WifiSettingActivity.this).inflate(R.layout.alertdialog_wifipassword_view, null);
				et_password = (EditText) customAlertDialogView.findViewById(R.id.et_password);
				btn_sure = (Button) customAlertDialogView.findViewById(R.id.btn_sure);
				btn_dismiss = (Button) customAlertDialogView.findViewById(R.id.btn_dismiss);
				tv_wifiName = (TextView) customAlertDialogView.findViewById(R.id.tv_WifiName);
				btn_sure.setOnClickListener(WifiSettingActivity.this);
				btn_dismiss.setOnClickListener(WifiSettingActivity.this);
				// 获取WIfi的名称
				WifiName = mWifiList.get(position).SSID;
				tv_wifiName.setText(WifiName);
				AlertDialog.Builder builder = new AlertDialog.Builder(WifiSettingActivity.this);
				alertDialog = builder.setView(customAlertDialogView).show();
				alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			}
		});
		// 绑定服务
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		WifiSettingActivity.this.bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);

	}

	@Override
	protected void onResume() {
		super.onResume();
		// 注册广播
		WifiSettingActivity.this.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WifiSettingActivity.this.unregisterReceiver(mGattUpdateReceiver);
		WifiSettingActivity.this.unbindService(mServiceConnection);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
		return intentFilter;
	}

	private void init() {
		rl_back = (RelativeLayout) this.findViewById(R.id.rl_back);
		iv_wifi = (ImageView) this.findViewById(R.id.iv_wifi);
		tv_backgroud = (TextView) this.findViewById(R.id.tv_backgroud);
		rl_content = (RelativeLayout) this.findViewById(R.id.rl_content);
		lv = (ListView) this.findViewById(R.id.lv_scanResult);
		rl_back.setOnClickListener(this);
		rl_content.setOnClickListener(this);
		mWifiManager = (WifiManager) this.getSystemService(this.WIFI_SERVICE);

	}

	// 自定义一个适配器
	public class MyBaseAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mWifiList.size();
		}

		@Override
		public Object getItem(int position) {
			return mWifiList.get(position);
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
				convertView = LayoutInflater.from(WifiSettingActivity.this).inflate(R.layout.lv_wifisetting_item, null);
				viewHolder.tv_WifiName = (TextView) convertView.findViewById(R.id.tv_WifiName);
				viewHolder.iv_wifiLevel = (ImageView) convertView.findViewById(R.id.iv_wifiLevel);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tv_WifiName.setText(mWifiList.get(position).SSID);
			return convertView;
		}

		public class ViewHolder {
			TextView tv_WifiName;
			ImageView iv_wifiLevel;
		}

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_back:
			this.finish();
			break;

		case R.id.rl_content:
			if (!mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(true);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mWifiManager.startScan();
			mWifiList = mWifiManager.getScanResults();
			mWifiConfigList = mWifiManager.getConfiguredNetworks();
			lv.setAdapter(mAdapter);
			iv_wifi.setVisibility(View.GONE);
			tv_backgroud.setVisibility(View.GONE);
			lv.setVisibility(View.VISIBLE);
			break;

		case R.id.btn_sure:
			PassWord = et_password.getText().toString();
			byte[] dataByte = new byte[66];
			byte[] headByte = new byte[] { 0x7E, 0x22, 0x3C, 0x00 };
			byte[] ssidByte = new byte[30];
			byte[] passwordByte = new byte[30];
			byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
			ByteArrayOutputStream OutputStream = new ByteArrayOutputStream();
			ByteArrayOutputStream OutputStream2 = new ByteArrayOutputStream();
			int restName = 30 - WifiName.length();
			int restpassWord = 30 - PassWord.length();
			// 遍历wifi名称
			for (int i = 0; i < WifiName.length(); i++) {
				char c = WifiName.charAt(i);
				byte b = (byte) c;
				OutputStream.write(b);
			}
			for (int j = WifiName.length(); j < 30; j++) {
				byte d = 0x00;
				OutputStream.write(d);
			}
			// 遍历wifi密码
			for (int i = 0; i < PassWord.length(); i++) {
				char c = PassWord.charAt(i);
				byte b = (byte) c;
				OutputStream2.write(b);
			}
			for (int j = PassWord.length(); j < 30; j++) {
				byte d = 0x00;
				OutputStream2.write(d);
			}
			try {
				OutputStream.close();
				OutputStream2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ssidByte = OutputStream.toByteArray();
			passwordByte = OutputStream2.toByteArray();
			// 拼接所有byte
			System.arraycopy(headByte, 0, dataByte, 0, 4);
			System.arraycopy(ssidByte, 0, dataByte, 4, 30);
			System.arraycopy(passwordByte, 0, dataByte, 34, 30);
			System.arraycopy(tailByte, 0, dataByte, 64, 2);
			Log.e("AAAAAA", DecodeUtils.byte2HexStr(dataByte) + "");
			Log.e("AAAAAA", DecodeUtils.bytesToHexString(dataByte) + "");
			try {
				byte[] firstByte = new byte[20];
				System.arraycopy(dataByte,0, firstByte, 0, 20);
				mBluetoothLeService.write(firstByte);
				Thread.sleep(100);
				byte[] secondByte = new byte[20];
				System.arraycopy(dataByte,20, secondByte, 0, 20);
				mBluetoothLeService.write(secondByte);
				Thread.sleep(100);
				byte[] thirdByte = new byte[20];
				System.arraycopy(dataByte,40, thirdByte, 0, 20);
				mBluetoothLeService.write(thirdByte);
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			byte[] finlaByte = new byte[6];
			System.arraycopy(dataByte, 60, finlaByte, 0, 6);
			mBluetoothLeService.write(finlaByte);
			break;

		case R.id.btn_dismiss:
			alertDialog.dismiss();
			break;

		}

	}

}
