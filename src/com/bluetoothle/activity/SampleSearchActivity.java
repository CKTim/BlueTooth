package com.bluetoothle.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluetoothle.db.MySQLiteHelper;
import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;
import com.bluetoothle.view.CustomGridview;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SampleSearchActivity extends Activity implements OnClickListener {
	private CustomGridview mGridView;
	private Button btn_search, btn_accessWay, btn_result;
	private RelativeLayout rl_back;
	private TextView tv_changeSample;
	private EditText et_search;
	private String searchName;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> list_forward = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> list_behind = new ArrayList<Map<String, Object>>();
	private MyBaseAdapter mMyBaseAdapter;
	private BluetoothLeService mBluetoothLeService;
	private MySQLiteHelper mMySQLiteHelper;
	private SQLiteDatabase db;
	// 代码管理服务生命周期
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			mMySQLiteHelper = new MySQLiteHelper(SampleSearchActivity.this);
			db = mMySQLiteHelper.getWritableDatabase();
			// 查询获得游标,判断表中是否已经有数据了，有的话直接从表中读取，没有的话发送查询指令
			Cursor cursor = db.query("sample", null, null, null, null, null, null);
			if (cursor.getCount() == 0) {
				// 发送查询所有物品名称指令
				mBluetoothLeService.write(new byte[] { 0x7e, 0x14, 0x00, 0x00, 0x00, (byte) 0xaa });
				Log.e("AAAAA", "数据库Sample表没东西，需要发送指令去加载");
			} else {
				// 直接从数据库读取
				Log.e("AAAAA", "直接从数据库读取");
				list = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					// 根据列的索引直接读取 比如第2列的值
					String SampleName = cursor.getString(3);
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("SampleName", SampleName);
					list.add(map);
				}
				// 判断list是否大于15,大于15则隐藏15后面的内容，显示....
				if (list.size() > 15) {
					for (int i = 0; i < 15; i++) {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("SampleName", list.get(i).get("SampleName"));
						if (i == 14) {
							map.put("SampleName", "....");
						}
						list_forward.add(map);
					}
					mMyBaseAdapter = new MyBaseAdapter(list_forward);
					mGridView.setAdapter(mMyBaseAdapter);
				}

			}
			cursor.close();
			db.close();

		}

		public void onServiceDisconnected(ComponentName name) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

	// 建立一个广播接受者用来接收从Bluetooth服务类返回过来的广播
	private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothLeService.EXTRA_DATA.equals(action)) {
				db = mMySQLiteHelper.getWritableDatabase();
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] byteData = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
				Log.e("AAAAAAA", DecodeUtils.bytesToHexString(byteData));
				// 所有样品名称装载
				for (int i = 0; i < byteData.length; i++) {
					if (byteData[i] == 0x7e && byteData[i + 1] == 0x15) {
						try {
							Map<String, Object> map = new HashMap<String, Object>();
							// 名称
							map.put("SampleName",
									DecodeUtils.stringToGbk(DecodeUtils.bytesToHexString(new byte[] { byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13],
											byteData[i + 14], byteData[i + 15] })));
							// 样品名称索引byte
							map.put("SampleIndex", new byte[] { byteData[i + 9], byteData[i + 8] });
							Log.e("AAAAAA", DecodeUtils.bytesToHexString(new byte[] { byteData[i + 9], byteData[i + 8] }));
							list.add(map);
							// 将数据添加到数据库
							ContentValues values = new ContentValues();
							values.put("samplenumber", DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 9], byteData[i + 8] }, 10));
							values.put("sampleindex", new byte[] { byteData[i + 8], byteData[i + 9] });
							values.put("samplename", map.get("SampleName") + "");
							values.put("samplenamebyte", new byte[] { byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13], byteData[i + 14], byteData[i + 15] });
							db.insert("sample", null, values);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				// 判断list是否大于15,用于隐藏后面部分数据
				if (list.size() > 15) {
					for (int i = 0; i < 15; i++) {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("SampleName", list.get(i).get("SampleName"));
						if (i == 14) {
							map.put("SampleName", "....");
						}
						list_forward.add(map);
					}
					mMyBaseAdapter = new MyBaseAdapter(list_forward);
					mGridView.setAdapter(mMyBaseAdapter);
				}
				db.close();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_samplesearch);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		// 绑定服务
		Intent gattServiceIntent = new Intent(SampleSearchActivity.this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		// gridview监听事件
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (list_forward.get(position).get("SampleName").toString().equals("....")) {
					list_forward.get(14).put("SampleName", list.get(14).get("SampleName").toString());
					for (int i = 15; i < list.size(); i++) {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("SampleName", list.get(i).get("SampleName"));
						list_behind.add(map);
					}
					list_forward.addAll(list_behind);
					mMyBaseAdapter.notifyDataSetChanged();
				} else {
					Intent intentBack = new Intent();
					intentBack.putExtra("SampleName", list.get(position).get("SampleName").toString());
					intentBack.putExtra("AccessWaySelect", "false");
					SampleSearchActivity.this.setResult(RESULT_OK, intentBack);
					SampleSearchActivity.this.finish();
				}

			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		// 注册广播
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
		unbindService(mServiceConnection);
	}

	private void init() {
		mGridView = (CustomGridview) this.findViewById(R.id.gv_allSample);
		et_search = (EditText) this.findViewById(R.id.et_search);
		btn_search = (Button) this.findViewById(R.id.btn_search);
		btn_accessWay = (Button) this.findViewById(R.id.btn_accessWay);
		btn_result = (Button) this.findViewById(R.id.btn_result);
		rl_back = (RelativeLayout) this.findViewById(R.id.rl_back);
		tv_changeSample = (TextView) this.findViewById(R.id.tv_changeSample);
		btn_search.setOnClickListener(this);
		btn_accessWay.setOnClickListener(this);
		btn_result.setOnClickListener(this);
		rl_back.setOnClickListener(this);
		tv_changeSample.setOnClickListener(this);
	}

	// 自定义一个适配器
	public class MyBaseAdapter extends BaseAdapter {
		private List<Map<String, Object>> list;

		public MyBaseAdapter(List<Map<String, Object>> listData) {
			this.list = listData;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = LayoutInflater.from(SampleSearchActivity.this).inflate(R.layout.gv_allsample_item, null);
				viewHolder.tv_SampleName = (TextView) convertView.findViewById(R.id.tv_SampleName);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tv_SampleName.setText(list.get(position).get("SampleName").toString());
			return convertView;
		}

		public class ViewHolder {
			TextView tv_SampleName;
		}

	}

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
		case R.id.btn_search:
			searchName = et_search.getText().toString().trim();
			for (int i = 0; i < list.size(); i++) {
				String sampleName = list.get(i).get("SampleName").toString().trim();
				if (searchName.equals(sampleName)) {
					btn_result.setText(list.get(i).get("SampleName").toString());
					btn_result.setVisibility(View.VISIBLE);
					btn_accessWay.setVisibility(View.VISIBLE);
					btn_accessWay.setSelected(false);
				}
			}

			break;

		case R.id.btn_accessWay:
			if (btn_accessWay.isSelected()) {
				btn_accessWay.setSelected(false);
			} else {
				btn_accessWay.setSelected(true);
			}
			break;

		case R.id.btn_result:
			if (btn_accessWay.isSelected()) {
				Intent intentBack = new Intent();
				intentBack.putExtra("SampleName", searchName);
				intentBack.putExtra("AccessWaySelect", "true");
				SampleSearchActivity.this.setResult(RESULT_OK, intentBack);
				SampleSearchActivity.this.finish();
			} else {
				Intent intentBack = new Intent();
				intentBack.putExtra("SampleName", searchName);
				intentBack.putExtra("AccessWaySelect", "false");
				SampleSearchActivity.this.setResult(RESULT_OK, intentBack);
				SampleSearchActivity.this.finish();
			}

			break;
		case R.id.rl_back:
			this.finish();
			break;
		case R.id.tv_changeSample:
			Intent intent = new Intent(SampleSearchActivity.this, SampleSettingActivity.class);
			startActivity(intent);
			break;
		}

	}

}
