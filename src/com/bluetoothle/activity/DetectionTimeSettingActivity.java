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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DetectionTimeSettingActivity extends Activity implements OnClickListener {
	private EditText et_DetectionTime;
	private Button btn_sure;
	private TextView tv_oldDectionTime;
	private SharedPreferences sp;
	private Editor editor;
	private String oldDetectionTime;
	private String newDetectionTime;
	private BluetoothLeService mBluetoothLeService;
	// ������������������
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

		}

		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};
	// ����һ���㲥�������������մ�Bluetooth�����෵�ع����Ĺ㲥
	private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothLeService.EXTRA_DATA.equals(action)) {
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] byteData = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
				// Wifi���÷���
				if (byteData[0] == 0x7e && byteData[1] == 0x1B && byteData[4] == 0x01) {
					editor = sp.edit();
					editor.putString("DetectionTime", newDetectionTime);
					editor.commit();
					Log.e("���ʱ��",sp.getString("DetectionTime", newDetectionTime));
					Toast.makeText(DetectionTimeSettingActivity.this, "���ʱ�����óɹ�Ϊ" + sp.getString("DetectionTime", "0")+"��", 0).show();
					DetectionTimeSettingActivity.this.finish();
				} else if (byteData[0] == 0x7e && byteData[1] == 0x1B && byteData[4] == 0x00) {
					Toast.makeText(DetectionTimeSettingActivity.this, "���ʱ������ʧ��", 0).show();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// ȥ��������
		setContentView(R.layout.activit_dtimesetting);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		// �󶨷���
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		this.bindService(gattServiceIntent, mServiceConnection, this.BIND_AUTO_CREATE);

	}

	@Override
	protected void onResume() {
		super.onResume();
		// ע��㲥
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
		et_DetectionTime = (EditText) this.findViewById(R.id.et_detectionTime);
		tv_oldDectionTime = (TextView) this.findViewById(R.id.tv_displayTime);
		btn_sure = (Button) this.findViewById(R.id.btn_sure);
		btn_sure.setOnClickListener(this);
		// ��ñ����Ĭ�ϼ��ʱ��
		sp = this.getSharedPreferences("LZ-4000(T)", this.MODE_PRIVATE);
		oldDetectionTime = sp.getString("DetectionTime", "480");
		tv_oldDectionTime.setText("��ǰ�ļ��ʱ��Ϊ" + oldDetectionTime + "s");
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_sure:
			newDetectionTime = et_DetectionTime.getText().toString();
			if(newDetectionTime.trim().equals("")){
				Toast.makeText(DetectionTimeSettingActivity.this, "��û�������κ���ֵ", 0).show();
				break;
			}
			if(Integer.parseInt(newDetectionTime)<10||Integer.parseInt(newDetectionTime)>999){
				Toast.makeText(DetectionTimeSettingActivity.this, "�㲻������С��10���ߴ���999������", 0).show();
				break;
			}
			byte[] dataByte = new byte[8];
			byte[] headByte = new byte[] { 0x7E, 0x1A, 0x02, 0x00 };
			byte[] timeByte = new byte[2];
			byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
			String et_time=Integer.toHexString(Integer.parseInt(newDetectionTime));
			String HexnewDetectionTime="";
			if(et_time.length()==1){
				 HexnewDetectionTime="0"+et_time+"00";
			}else if(et_time.length()==2){
				 HexnewDetectionTime=et_time+"00";
			}else if(et_time.length()==3){
				String first=et_time.substring(1, 3);
				String third=et_time.substring(0,1);
				HexnewDetectionTime=first+"0"+third;
			}else if(et_time.length()==4){
				 HexnewDetectionTime=et_time;
			}
			timeByte=DecodeUtils.HexString2Bytes(HexnewDetectionTime);
			System.arraycopy(headByte, 0, dataByte, 0, 4);
			System.arraycopy(timeByte, 0, dataByte, 4, 2);
			System.arraycopy(tailByte, 0, dataByte, 6, 2);
			mBluetoothLeService.write(dataByte);
			Log.e("AAAAAAAA",DecodeUtils.byte2HexStr(dataByte)+"");
			break;
		}

	}
}
