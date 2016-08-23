package com.bluetoothle.activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluetoothle.db.MySQLiteHelper;
import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;
import com.bluetoothle.view.CustomGridview;

import cm.example.lz_4000tbluetooth.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "NewApi", "InflateParams" })
public class SampleSettingActivity extends Activity implements OnClickListener {
	private EditText et_search, et_changeSample, et_addSample;
	private Button btn_search, btn_result, btn_edit, btn_modify, btn_delete, btn_addmore, btn_sure, btn_sureAdd;
	private TextView tv_SampleName, tv_oldSampleName, tv_AddOldSampleName;
	private CustomGridview gridview;
	private RelativeLayout rl_back;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private AlertDialog alertDialog_sample;
	private AlertDialog alertDialog_sample_change;
	private AlertDialog alertDialog_sample_addmore;
	private SimpleAdapter adapter;
	private BluetoothLeService mBluetoothLeService;
	private MySQLiteHelper mMySQLiteHelper;
	private SQLiteDatabase db;
	private View view_sample;
	private View view_sample_change;
	private View view_addmore;
	private String NowSampleName;
	private byte[] NowSampleIndexByte;
	private byte[] NowSampleNameByte;
	private byte[] NewSampleNameByte;
	private byte[] addsampleIndexByte;
	private byte[] addsaplenameByte;
	private String NewSampleName;
	private String searchSampleName;
	private String AddSampleName;
	private int AddNewSampleIndex;
	private static int deleteSample = 0;
	private static int addSample = 0;
	private int ClickPosition;
	// 点击图标编辑专用
	private int ClickPosition_edit;
	private String NowSampleName_edit;
	private byte[] NowSampleIndexByte_edit;
	private byte[] NowSampleNameByte_edit;
	// 代码管理服务生命周期
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			mMySQLiteHelper = new MySQLiteHelper(SampleSettingActivity.this);
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
					map.put("Samplenumber", cursor.getString(1));
					map.put("SampleName", SampleName);
					map.put("SampleIndex", cursor.getBlob(2));
					map.put("SampleNameByte", cursor.getBlob(4));
					list.add(map);
				}
				adapter = new SimpleAdapter(SampleSettingActivity.this, list, R.layout.gv_allsample_item, new String[] { "SampleName" }, new int[] { R.id.tv_SampleName });
				gridview.setAdapter(adapter);

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
							map.put("SampleIndex", new byte[] { byteData[i + 8], byteData[i + 9] });
							Log.e(" 样品名称索引byte", DecodeUtils.bytesToHexString(new byte[] { byteData[i + 8], byteData[i + 9] }));
							// 样品索引编号
							map.put("Samplenumber", DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 9], byteData[i + 8] }, 10));
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
				adapter = new SimpleAdapter(SampleSettingActivity.this, list, R.layout.gv_allsample_item, new String[] { "SampleName" }, new int[] { R.id.tv_SampleName });
				gridview.setAdapter(adapter);
				// 样品名称设置成功返回
				if (byteData[0] == 0x7e && byteData[1] == 0x17 && byteData[4] == 0x01) {
					if (deleteSample == 1) {
						Log.e("AAAAA", "删除");
						deleteSample = 0;
						// 修改list和数据库
						list.get(ClickPosition).put("SampleName", "");
						list.get(ClickPosition).put("SampleNameByte", "");
						ContentValues values = new ContentValues();
						values.put("samplename", "");
						values.put("samplenamebyte", "");
						db.update("sample", values, "samplenumber=?", new String[] { ClickPosition + "" });
						adapter.notifyDataSetChanged();
						alertDialog_sample.dismiss();
						Toast.makeText(SampleSettingActivity.this, "删除成功", 0).show();
					} else if (addSample == 1) {
						addSample = 0;
						// 将数据添加到数据库
						ContentValues values = new ContentValues();
						values.put("samplenumber", AddNewSampleIndex);
						values.put("sampleindex", addsampleIndexByte);
						values.put("samplename", AddSampleName);
						values.put("samplenamebyte", addsaplenameByte);
						db.insert("sample", null, values);
						// 添加到list
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("SampleName", AddSampleName);
						// 样品名称索引byte
						map.put("SampleIndex", addsampleIndexByte);
						// 样品索引编号
						map.put("Samplenumber", AddNewSampleIndex);
						list.add(map);
						adapter.notifyDataSetChanged();
						alertDialog_sample_addmore.dismiss();
						Toast.makeText(SampleSettingActivity.this, "添加成功", 0).show();
					} else {
						// 修改list和数据库
						list.get(ClickPosition).put("SampleName", NewSampleName);
						list.get(ClickPosition).put("SampleNameByte", NewSampleNameByte);
						ContentValues values = new ContentValues();
						values.put("samplename", NewSampleName);
						values.put("samplenamebyte", NewSampleNameByte);
						db.update("sample", values, "samplenumber=?", new String[] { ClickPosition + "" });
						adapter.notifyDataSetChanged();
						alertDialog_sample_change.dismiss();
						Toast.makeText(SampleSettingActivity.this, "修改成功", 0).show();
					}

				}
				db.close();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_samplesetting);
		init();
		// 绑定服务
		Intent gattServiceIntent = new Intent(SampleSettingActivity.this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		// grid点击监听事件
		gridview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ClickPosition = position;
				view_sample = LayoutInflater.from(SampleSettingActivity.this).inflate(R.layout.alertdialog_sample_view, null);
				AlertDialog.Builder builder = new AlertDialog.Builder(SampleSettingActivity.this);
				alertDialog_sample = builder.setView(view_sample).show();
				btn_modify = (Button) view_sample.findViewById(R.id.btn_modify);
				btn_delete = (Button) view_sample.findViewById(R.id.btn_delete);
				btn_addmore = (Button) view_sample.findViewById(R.id.btn_addmore);
				tv_SampleName = (TextView) view_sample.findViewById(R.id.tv_SampleName);
				btn_modify.setOnClickListener(SampleSettingActivity.this);
				btn_delete.setOnClickListener(SampleSettingActivity.this);
				btn_addmore.setOnClickListener(SampleSettingActivity.this);
				NowSampleName = list.get(position).get("SampleName").toString();
				NowSampleIndexByte = (byte[]) list.get(position).get("SampleIndex");
				NowSampleNameByte = (byte[]) list.get(position).get("SampleNameByte");
				tv_SampleName.setText(NowSampleName);
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

	private void init() {
		et_search = (EditText) this.findViewById(R.id.et_search);
		btn_search = (Button) this.findViewById(R.id.btn_search);
		btn_result = (Button) this.findViewById(R.id.btn_result);
		btn_edit = (Button) this.findViewById(R.id.btn_edit);
		gridview = (CustomGridview) this.findViewById(R.id.gv_allSample);
		rl_back = (RelativeLayout) this.findViewById(R.id.rl_back);
		btn_search.setOnClickListener(this);
		btn_result.setOnClickListener(this);
		btn_edit.setOnClickListener(this);
		rl_back.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_search:
			searchSampleName = et_search.getText().toString();
			for (int i = 0; i < list.size(); i++) {
				String sampleName = list.get(i).get("SampleName").toString().trim();
				if (searchSampleName.equals(sampleName)) {
					btn_result.setText(sampleName);
					btn_edit.setVisibility(View.VISIBLE);
					ClickPosition_edit = i;
					NowSampleName_edit = list.get(i).get("SampleName").toString();
					NowSampleIndexByte_edit = (byte[]) list.get(i).get("SampleIndex");
					NowSampleNameByte_edit = (byte[]) list.get(i).get("SampleNameByte");
                    break;
				}
			}
			break;

		case R.id.btn_sureAdd:
			addSample = 1;
			// 索引byte
			AddNewSampleIndex = list.size();
			String stringNewSampleIndex = Integer.toHexString(AddNewSampleIndex);
			String finalNewSampleIndex = "";// byte类型
			if (stringNewSampleIndex.length() == 2) {
				finalNewSampleIndex = stringNewSampleIndex + "00";
			} else if (stringNewSampleIndex.length() == 3) {
				String first = stringNewSampleIndex.substring(1, 3);
				String second = stringNewSampleIndex.substring(0, 1);
				finalNewSampleIndex = first + "0" + second;
			}
			// 物品名称byte
			ByteArrayOutputStream outputStream0 = new ByteArrayOutputStream();
			try {
				AddSampleName = et_addSample.getText().toString().trim();
				outputStream0.write(DecodeUtils.gbkToString(AddSampleName));
				for (int j = outputStream0.toByteArray().length; j < 6; j++) {
					outputStream0.write(0x20);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			byte[] addByte = new byte[18];
			byte[] addfirstByte = new byte[] { 0x7E, 0x16, 0x0C, 0x00, 0x01, 0x00, 0x01, 0x00 };
			addsampleIndexByte = DecodeUtils.HexString2Bytes(finalNewSampleIndex);
			addsaplenameByte = outputStream0.toByteArray();
			byte[] addtailByte = new byte[] { 0x00, (byte) 0xAA };
			System.arraycopy(addfirstByte, 0, addByte, 0, 8);
			System.arraycopy(addsampleIndexByte, 0, addByte, 8, 2);
			System.arraycopy(addsaplenameByte, 0, addByte, 10, 6);
			System.arraycopy(addtailByte, 0, addByte, 16, 2);
			Log.e("AAAAAAAAAAAAA", "" + DecodeUtils.byte2HexStr(addByte));
			mBluetoothLeService.write(addByte);
			break;

		case R.id.btn_edit:
			ClickPosition=ClickPosition_edit;
			NowSampleName =NowSampleName_edit;
			NowSampleIndexByte =NowSampleIndexByte_edit;
			NowSampleNameByte =NowSampleNameByte_edit;
			NowSampleName=btn_result.getText().toString();
			view_sample = LayoutInflater.from(SampleSettingActivity.this).inflate(R.layout.alertdialog_sample_view, null);
			AlertDialog.Builder builder0 = new AlertDialog.Builder(SampleSettingActivity.this);
			alertDialog_sample = builder0.setView(view_sample).show();
			btn_modify = (Button) view_sample.findViewById(R.id.btn_modify);
			btn_delete = (Button) view_sample.findViewById(R.id.btn_delete);
			btn_addmore = (Button) view_sample.findViewById(R.id.btn_addmore);
			tv_SampleName = (TextView) view_sample.findViewById(R.id.tv_SampleName);
			btn_modify.setOnClickListener(SampleSettingActivity.this);
			btn_delete.setOnClickListener(SampleSettingActivity.this);
			btn_addmore.setOnClickListener(SampleSettingActivity.this);
			tv_SampleName.setText(NowSampleName);
			break;

		case R.id.btn_modify:
			alertDialog_sample.dismiss();
			view_sample_change = LayoutInflater.from(SampleSettingActivity.this).inflate(R.layout.alertdialog_changesample_view, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(SampleSettingActivity.this);
			alertDialog_sample_change = builder.setView(view_sample_change).show();
			et_changeSample = (EditText) view_sample_change.findViewById(R.id.et_changeSample);
			tv_oldSampleName = (TextView) view_sample_change.findViewById(R.id.tv_oldSampleName);
			btn_sure = (Button) view_sample_change.findViewById(R.id.btn_sure);
			btn_sure.setOnClickListener(SampleSettingActivity.this);
			tv_oldSampleName.setText(NowSampleName);
			break;

		case R.id.btn_delete:
			deleteSample = 1;
			byte[] deleteByte = new byte[18];
			byte[] deletefirstByte = new byte[] { 0x7E, 0x16, 0x0C, 0x00, 0x01, 0x00, 0x01, 0x00 };
			byte[] deletemiddleByte = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x0, 0x00 };
			byte[] deletetailByte = new byte[] { 0x00, (byte) 0xAA };
			System.arraycopy(deletefirstByte, 0, deleteByte, 0, 8);
			System.arraycopy(NowSampleIndexByte, 0, deleteByte, 8, 2);
			System.arraycopy(deletemiddleByte, 0, deleteByte, 10, 6);
			System.arraycopy(deletetailByte, 0, deleteByte, 16, 2);
			Log.e("删除", DecodeUtils.byte2HexStr(deleteByte) + "");
			mBluetoothLeService.write(deleteByte);
			break;

		case R.id.btn_addmore:
			alertDialog_sample.dismiss();
			view_addmore = LayoutInflater.from(SampleSettingActivity.this).inflate(R.layout.alertdialog_addsample_view, null);
			AlertDialog.Builder builder1 = new AlertDialog.Builder(SampleSettingActivity.this);
			alertDialog_sample_addmore = builder1.setView(view_addmore).show();
			et_addSample = (EditText) view_addmore.findViewById(R.id.et_addSample);
			btn_sureAdd = (Button) view_addmore.findViewById(R.id.btn_sureAdd);
			btn_sureAdd.setOnClickListener(SampleSettingActivity.this);

			break;
		case R.id.btn_sure:
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try {
				NewSampleName = et_changeSample.getText().toString().trim();
				outputStream.write(DecodeUtils.gbkToString(NewSampleName));
				for (int j = outputStream.toByteArray().length; j < 6; j++) {
					outputStream.write(0x20);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			NewSampleNameByte = outputStream.toByteArray();
			byte[] dataByte = new byte[18];
			byte[] firstByte = new byte[] { 0x7E, 0x16, 0x0C, 0x00, 0x01, 0x00, 0x001, 0x00 };
			byte[] tailByte = new byte[] { 0x00, (byte) 0xAA };
			System.arraycopy(firstByte, 0, dataByte, 0, 8);
			System.arraycopy(NowSampleIndexByte, 0, dataByte, 8, 2);
			System.arraycopy(NewSampleNameByte, 0, dataByte, 10, 6);
			System.arraycopy(tailByte, 0, dataByte, 16, 2);
			Log.e("修改", DecodeUtils.byte2HexStr(dataByte) + "");
			mBluetoothLeService.write(dataByte);
			break;
		case R.id.rl_back:
			this.finish();
			break;
		}

	}

}
