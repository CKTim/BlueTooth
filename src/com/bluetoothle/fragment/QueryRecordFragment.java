package com.bluetoothle.fragment;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluetoothle.db.MySQLiteHelper;
import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.DecodeUtils;

import cm.example.lz_4000tbluetooth.R;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class QueryRecordFragment extends Fragment implements OnClickListener {
	private ListView mListView;
	private Button btn_upload, btn_print, btn_back, btn_checkall, btn_synchronization;
	private RelativeLayout rl_popup_container, rl_query_time, rl_startLoad, rl_back;
	private TextView tv_queryTime;
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private PopupWindow mPopupWindow;
	private MyBaseAdapter mMyBaseAdapter;
	private View v;
	private View PopupWindowView;
	private List<Map<String, Object>> listRecord = new ArrayList<Map<String, Object>>();
	private BluetoothLeService mBluetoothLeService;
	private LocalBroadcastManager broadcastManager;
	private MySQLiteHelper mMySQLiteHelper;
	private SQLiteDatabase db;
	private byte[] allsynchronizationbyte;
	private String mYear, mMonth, mDay;
	private String Year, Month, Day;
	private HashMap<Integer, Boolean> isSelected; // ���ڱ���checkbox��״̬
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
				db=mMySQLiteHelper.getWritableDatabase();
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] byteData = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
				// ��ѯ��¼���ؽ������ת��װ��
				if (byteData[0] == 0x7e && byteData[1] == 0x11) {
					listRecord = new ArrayList<Map<String, Object>>();
					Log.e("byteData.length",byteData.length+"");
					for (int i = 0; i < byteData.length; i++) {
						if (byteData[i] == 0x7e && byteData[i+1] == 0x11) {
						try {
							String number = DecodeUtils.AddZeroToFour(String.valueOf(byteData[i + 8] + byteData[i + 9] * 256));// ���
							// ����
							byte[] byteVegetableType = new byte[] { byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13], byteData[i + 14], byteData[i + 15] };
							String vegetableName = DecodeUtils.stringToGbk(DecodeUtils.bytesToHexString(byteVegetableType));
							// ������ʱ����
							int year = byteData[i + 16];
							String month = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 17]));
							String day = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 18]));
							String hour = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 19]));
							String min = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 20]));
							String second = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 21]));
							// ������
							double a = Integer.parseInt(DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 22] }, 10));
							double b = Integer.parseInt(DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 23] }, 10)) * 256;
							double c = (a + b) / 100;
							String result = String.format("%.2f", c);
							// ���
							String Finalresult = "";
							if ((byteData[i + 22] + byteData[i + 23] * 256) > 5000) {
								Finalresult = "����";
							} else {
								Finalresult = "����";
							}
							// ��ÿ���߲˵�ʱ����,�����ʣ��߲�����ȵ�ԭbyteװ��һ���¼��ϱ��ڴ�ӡ
							byte[] printByte = new byte[] { byteData[i + 8], byteData[i + 9], byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13],
									byteData[i + 14], byteData[i + 15], byteData[i + 16], byteData[i + 17], byteData[i + 18], byteData[i + 19], byteData[i + 20], byteData[i + 21],
									byteData[i + 22], byteData[i + 23] };
							// ���浽���ݿ�
							ContentValues values = new ContentValues();
							values.put("recordNumber", number);
							values.put("vegetableName", vegetableName);
							values.put("year", year);
							values.put("month", month);
							values.put("data", day);
							values.put("hour", hour);
							values.put("min", min);
							values.put("second", second);
							values.put("InhibitionRate", result);
							values.put("result", Finalresult);
							values.put("printByte", printByte);
							values.put("synchronization", "true");
							db.insert("record", null, values);
							//װ�ص�list
							Map<String, Object> map = new HashMap<String, Object>();
                         	map.put("number", number);
							map.put("name", vegetableName);
							map.put("year", year);
							map.put("month", month);
							map.put("data", day);
							map.put("hour", hour);
							map.put("min", min);
							map.put("second", second);
							map.put("InhibitionRate", result + "");
							map.put("result",Finalresult);
							map.put("printByte", printByte);
							map.put("synchronization", "true");
							listRecord.add(map);
							// �ж��Ƿ���ˢ�����������ˢ�µ�������Ҫ���������µ�δͬ������Ҳ��ʾ����
							if (mSwipeRefreshLayout.isRefreshing()) {
								// ��������
								Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { month, day }, null, null, null);
								listRecord = new ArrayList<Map<String, Object>>();
								while (cursor.moveToNext()) {
									// �����е�����ֱ�Ӷ�ȡ �����2�е�ֵ
									Map<String, Object> map_updata = new HashMap<String, Object>();
									// ID
									map_updata.put("id", cursor.getString(0));
									// ���
									map_updata.put("number", cursor.getString(1));
									// ����
									map_updata.put("name", cursor.getString(2));
									// ������
									map_updata.put("year", cursor.getString(3));
									map_updata.put("month", cursor.getString(4));
									map_updata.put("data", cursor.getString(5));
									// ʱ����
									map_updata.put("hour", cursor.getString(6));
									map_updata.put("min", cursor.getString(7));
									map_updata.put("second", cursor.getString(8));
									// ������
									map_updata.put("InhibitionRate", cursor.getString(9));
									// ���
									map_updata.put("result", cursor.getString(10));
									// ��ÿ���߲˵�ʱ����,�����ʣ��߲�����ȵ�ԭbyteװ��һ���¼��ϱ��ڴ�ӡ
									map_updata.put("printByte", cursor.getBlob(11));
									// �Ƿ�ͬ��
									map_updata.put("synchronization", cursor.getString(12));
									listRecord.add(map_updata);
									if (listRecord.size() != 0) {
										mMyBaseAdapter = new MyBaseAdapter(listRecord);
										mListView.setAdapter(mMyBaseAdapter);
										if (mListView.getVisibility() == View.GONE) {
											mListView.setVisibility(View.VISIBLE);
											rl_startLoad.setVisibility(View.GONE);
										}
									}

								}
								cursor.close();
								mSwipeRefreshLayout.setRefreshing(false);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						}
					}
					if (mListView.getVisibility() == View.GONE) {
						mListView.setVisibility(View.VISIBLE);
						rl_startLoad.setVisibility(View.GONE);
					}
					if (listRecord.size() != 0) {
						mMyBaseAdapter = new MyBaseAdapter(listRecord);
						mListView.setAdapter(mMyBaseAdapter);
					}
				}

				// ����¼ͬ���ɹ�����
				if (byteData[0] == 0x7e && byteData[1] == 0x3B && byteData[4] == 0x01) {
					db = mMySQLiteHelper.getWritableDatabase();
					for (int i = 0; i < listRecord.size(); i++) {
						if (isSelected.get(i)) {
							// ͬ�����ݿ��޸�
							ContentValues values = new ContentValues();
							values.put("synchronization", "true");
							db.update("record", values, "_id=?", new String[] { listRecord.get(i).get("id").toString() });
							listRecord.get(i).put("synchronization", "true");
						}
					}
					mMyBaseAdapter.notifyDataSetChanged();
					db.close();
				}
//				 //��ӡ���������û��װ�سɹ�
//				 for (int i = 0; i < listRecord.size(); i++) {
//				 Log.e("AAAAAA", "list��СΪ:" + listRecord.size() + "���Ϊ:"
//				 + listRecord.get(i).get("number") + "�߲�����Ϊ:"
//				 + listRecord.get(i).get("name") + "���Ϊ:"
//				 + listRecord.get(i).get("year") + "������Ϊ:"
//				 + listRecord.get(i).get("InhibitionRate"));
//				 }
				db.close();
			}
		}
	};

	// ����һ���㲥�����ռ����ķ�������
	private BroadcastReceiver mUpdataBrocast = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("com.example.bluetooth.le.UPDATA")) {
				db=mMySQLiteHelper.getWritableDatabase();
				Log.e("AAAAAAAA", "�յ�ˢ�µĹ㲥");
				Bundle bundle = intent.getExtras();
				// ���浽���ݿ�
				ContentValues values = new ContentValues();
				values.put("recordNumber", bundle.getString("number"));
				values.put("vegetableName", bundle.getString("name"));
				values.put("year", bundle.getString("year"));
				values.put("month", bundle.getString("month"));
				values.put("data", bundle.getString("data"));
				values.put("hour", bundle.getString("hour"));
				values.put("min", bundle.getString("min"));
				values.put("second", bundle.getString("second"));
				values.put("InhibitionRate", bundle.getString("InhibitionRate"));
				values.put("result", bundle.getString("result"));
				values.put("printByte", bundle.getByteArray("printByte"));
				values.put("synchronization", "false");
				db.insert("record", null, values);
				Log.e("AAAAA", "���¼�¼�� ");
				// ��������
				Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { bundle.getString("month"), bundle.getString("data") }, null, null, null);
				listRecord = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					// �����е�����ֱ�Ӷ�ȡ �����2�е�ֵ
					Map<String, Object> map = new HashMap<String, Object>();
					// ID
					map.put("id", cursor.getString(0));
					// ���
					map.put("number", cursor.getString(1));
					// ����
					map.put("name", cursor.getString(2));
					// ������
					map.put("year", cursor.getString(3));
					map.put("month", cursor.getString(4));
					map.put("data", cursor.getString(5));
					// ʱ����
					map.put("hour", cursor.getString(6));
					map.put("min", cursor.getString(7));
					map.put("second", cursor.getString(8));
					// ������
					map.put("InhibitionRate", cursor.getString(9));
					// ���
					map.put("result", cursor.getString(10));
					// ��ÿ���߲˵�ʱ����,�����ʣ��߲�����ȵ�ԭbyteװ��һ���¼��ϱ��ڴ�ӡ
					map.put("printByte", cursor.getBlob(11));
					// �Ƿ�ͬ��
					map.put("synchronization", cursor.getString(12));
					listRecord.add(map);
					if (listRecord.size() != 0) {
						mMyBaseAdapter = new MyBaseAdapter(listRecord);
						mListView.setAdapter(mMyBaseAdapter);
						if (mListView.getVisibility() == View.GONE) {
							mListView.setVisibility(View.VISIBLE);
							rl_startLoad.setVisibility(View.GONE);
						}
					}

				}
				cursor.close();
				db.close();
				tv_queryTime.setText(Year + "-" + Month + "-" + Day);
			}
		}
	};

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.fragment_query_record, null);
		// ����PopupWindow�Ĳ���
		PopupWindowView = LayoutInflater.from(getActivity()).inflate(R.layout.popupwindow_custom_view, null);
		init();
		// �󶨷���
		Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
		// ���������¼�
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// ������ʾ����
				mPopupWindow = new PopupWindow(PopupWindowView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				mPopupWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);
				return false;
			}
		});

		return v;
	}

	private void init() {
		mListView = (ListView) v.findViewById(R.id.lv_queryRecord);
		btn_checkall = (Button) v.findViewById(R.id.btn_selectAll);
		btn_checkall = (Button) v.findViewById(R.id.btn_selectAll);
		btn_synchronization = (Button) PopupWindowView.findViewById(R.id.btn_synchronization);
		rl_back = (RelativeLayout) v.findViewById(R.id.rl_back);
		rl_query_time = (RelativeLayout) v.findViewById(R.id.rl_query_time);
		rl_startLoad = (RelativeLayout) v.findViewById(R.id.rl_startLoad);
		tv_queryTime = (TextView) v.findViewById(R.id.tv_queryTime);
		mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
		btn_upload = (Button) PopupWindowView.findViewById(R.id.btn_upload);
		btn_print = (Button) PopupWindowView.findViewById(R.id.btn_print);
		btn_back = (Button) PopupWindowView.findViewById(R.id.btn_back);
		rl_popup_container = (RelativeLayout) PopupWindowView.findViewById(R.id.rl_popup_container);
		btn_checkall.setSelected(true);
		rl_back.setOnClickListener(this);
		btn_upload.setOnClickListener(this);
		btn_back.setOnClickListener(this);
		rl_popup_container.setOnClickListener(this);
		rl_query_time.setOnClickListener(this);
		rl_startLoad.setOnClickListener(this);
		// �Ȼ�ȡ��ǰ�����գ���ʾ�ڱ�����
		Calendar c = Calendar.getInstance();
		Year = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.YEAR)));
		Month = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.MONTH) + 1));
		Day = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.DATE)));
		tv_queryTime.setText(Year + "-" + Month + "-" + Day);
		// ��ʼ��MySQLiteHelper
		mMySQLiteHelper = new MySQLiteHelper(getActivity());
		// ��ʼ���㲥������
		broadcastManager = LocalBroadcastManager.getInstance(getActivity());
		// ����ˢ�¼���
		mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.lightBlue));
		mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				// ����ˢ��ʱɾ�����ݿ�������������»�ȡ
				db = mMySQLiteHelper.getWritableDatabase();
				String DATE = tv_queryTime.getText().toString();
				String year = DATE.substring(2, 4);
				String month = DATE.substring(5, 7);
				String day = DATE.substring(8);
				db.delete("record", "month=? and data=? and synchronization=?", new String[] { month, day, "true" });
				db.close();
				// ɾ�������¶�ȡ��������
				// ʱ��(byte����)װ��
				String FinalData = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(year))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(month)))
						+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(day)));
				byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
				byte[] queryByte = new byte[9];
				byte[] headByte = new byte[] { 0x7e, 0x10, 0x03, 0x00 };
				byte[] tailByte = new byte[] { 0x00, (byte) 0xaa };
				System.arraycopy(headByte, 0, queryByte, 0, 4);
				System.arraycopy(DataByte, 0, queryByte, 4, 3);
				System.arraycopy(tailByte, 0, queryByte, 7, 2);
				mBluetoothLeService.write(queryByte);
				//����8s���Զ��ر�ˢ��
				final Handler handler=new Handler();
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						if(mSwipeRefreshLayout.isRefreshing()){
							mSwipeRefreshLayout.setRefreshing(false);
//							Toast.makeText(getActivity(), "��ʱû�и�������",0).show();
						}
					}
				}, 8000);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		// ע��㲥
		getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.example.bluetooth.le.UPDATA");
		broadcastManager.registerReceiver(mUpdataBrocast, intentFilter);
		Log.e("AAAAAAAA", "ע��mUpdataBrocast�㲥��mGattUpdateReceiver�㲥");
	}

	@Override
	public void onPause() {
		super.onPause();
		// getActivity().unregisterReceiver(mGattUpdateReceiver);
		broadcastManager.unregisterReceiver(mUpdataBrocast);
		Log.e("QRFragment", "onPause()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// ������ͽ�ע��㲥
		getActivity().unbindService(mServiceConnection);
		getActivity().unregisterReceiver(mGattUpdateReceiver);
	}

	// @Override
	// public void onHiddenChanged(boolean hidden) {
	// super.onHiddenChanged(hidden);
	// if(hidden){
	// Log.e("AAAAA","QRFragment�����أ���ע��㲥");
	// // ��ע��㲥
	// getActivity().unregisterReceiver(mGattUpdateReceiver);
	// }else{
	// getActivity().registerReceiver(mGattUpdateReceiver,
	// makeGattUpdateIntentFilter());
	// Log.e("AAAAA","QRFragment����ʾ��ע��㲥");
	// }
	// }

	// �Զ���һ��������
	@SuppressLint("UseSparseArrays")
	public class MyBaseAdapter extends BaseAdapter {
		private List<Map<String, Object>> list;

		public MyBaseAdapter(List<Map<String, Object>> listData) {
			isSelected = new HashMap<Integer, Boolean>();
			list = new ArrayList<Map<String, Object>>();
			list = listData;
			for (int i = 0; i < list.size(); i++) {
				isSelected.put(i, false);
			}
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

		@SuppressWarnings("deprecation")
		@SuppressLint("ResourceAsColor")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.lv_queryrecord_item, null);
				viewHolder.tv_Number = (TextView) convertView.findViewById(R.id.tv_Number);
				viewHolder.tv_Name = (TextView) convertView.findViewById(R.id.tv_Name);
				viewHolder.tv_DetectionDate = (TextView) convertView.findViewById(R.id.tv_DetectionDate);
				viewHolder.tv_DetectionTime = (TextView) convertView.findViewById(R.id.tv_DetectionTime);
				viewHolder.tv_InhibitionRate = (TextView) convertView.findViewById(R.id.tv_InhibitionRate);
				viewHolder.tv_Result = (TextView) convertView.findViewById(R.id.tv_Result);
				viewHolder.cb_selectRecord = (CheckBox) convertView.findViewById(R.id.cb_selectRecord);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			// �����ͬ��������������ʾΪ��ɫ��û�еĻ�����ʾΪ��ɫ
			if (list.get(position).get("synchronization").toString().equals("true")) {
				viewHolder.tv_Number.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_Name.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_DetectionDate.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_DetectionTime.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_InhibitionRate.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_Result.setTextColor(getResources().getColor(R.color.gray));
				viewHolder.tv_Number.setText(list.get(position).get("number").toString());
				viewHolder.tv_Name.setText(list.get(position).get("name").toString());
				viewHolder.tv_DetectionDate.setText(list.get(position).get("year") + "/" + list.get(position).get("month") + "/" + list.get(position).get("data"));
				viewHolder.tv_DetectionTime.setText(list.get(position).get("hour") + ":" + list.get(position).get("min") + ":" + list.get(position).get("second"));
				viewHolder.tv_InhibitionRate.setText(list.get(position).get("InhibitionRate") + "%");
				viewHolder.tv_Result.setText(list.get(position).get("result") + "");
			} else {
				viewHolder.tv_Number.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_Name.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_DetectionDate.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_DetectionTime.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_InhibitionRate.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_Result.setTextColor(getResources().getColor(R.color.lightBlue));
				viewHolder.tv_Number.setText(list.get(position).get("number").toString());
				viewHolder.tv_Name.setText(list.get(position).get("name").toString());
				viewHolder.tv_DetectionDate.setText(list.get(position).get("year") + "/" + list.get(position).get("month") + "/" + list.get(position).get("data"));
				viewHolder.tv_DetectionTime.setText(list.get(position).get("hour") + ":" + list.get(position).get("min") + ":" + list.get(position).get("second"));
				viewHolder.tv_InhibitionRate.setText(list.get(position).get("InhibitionRate") + "%");
				viewHolder.tv_Result.setText(list.get(position).get("result") + "");
			}

			// checkbox�ĵ���¼�,���һ�ξͲű���һ��״̬
			viewHolder.cb_selectRecord.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					isSelected.put(position, isChecked);
				}
			});
			viewHolder.cb_selectRecord.setChecked(isSelected.get(position));

			// ȫѡ��ť����
			btn_checkall.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (btn_checkall.isSelected()) {
						btn_checkall.setSelected(false);
						for (int i = 0; i < list.size(); i++) {
							isSelected.put(i, true);
							mMyBaseAdapter.notifyDataSetChanged();
						}
					} else {
						btn_checkall.setSelected(true);
						for (int i = 0; i < list.size(); i++) {
							isSelected.put(i, false);
							mMyBaseAdapter.notifyDataSetChanged();
						}
					}

				}
			});

			// ��ӡ��ť�ļ���
			btn_print.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					for (int i = 0; i < list.size(); i++) {
						if (isSelected.get(i)) {
							byte[] flag = new byte[] { 0x7e, 0x38, 0x10, 0x00 };
							byte[] data = (byte[]) listRecord.get(i).get("printByte");
							byte[] end = new byte[] { 0x00, (byte) 0xAA };
							// ƴ������byte[]���һ���µ�byte[]
							byte[] sendData = new byte[22];
							System.arraycopy(flag, 0, sendData, 0, 4);
							System.arraycopy(data, 0, sendData, 4, 16);
							System.arraycopy(end, 0, sendData, 20, 2);
							// ���ݴ���20���ֽڣ���������
							if (sendData.length >= 20) {
								mPopupWindow.dismiss(); 
								try {
									byte[] b1 = new byte[20];
									System.arraycopy(sendData, 0, b1, 0, 20);
									mBluetoothLeService.write(b1);
									Thread.sleep(50);
									byte[] b2 = new byte[sendData.length - 20];
									System.arraycopy(sendData, 20, b2, 0, sendData.length - 20);
									mBluetoothLeService.write(b2);
									Thread.sleep(2500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

							}
						}
					}
					
				}
			});

			// ͬ����ť�����¼�
			btn_synchronization.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					int count = 0;//��ǰ�ǵڼ�������
					int countRecord = 0;//ͬ���ļ�¼��
					ArrayList<byte[]> listallSynchronizationByte = new ArrayList<byte[]>();
					// ��ȡ�ܹ�Ҫͬ���ļ�¼����
					for (int i = 0; i < list.size(); i++) {
						if (isSelected.get(i)) {
							countRecord = countRecord + 1;
						}
					}
					// ƴ��ÿһ��ͬ����¼
					for (int i = 0; i < list.size(); i++) {
						if (isSelected.get(i)) {
							mPopupWindow.dismiss();
							count = count + 1;
							try {
								String year = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("year").toString())));
								String month = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("month").toString())));
								String data = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("data").toString())));
								String hour = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("hour").toString())));
								String min = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("min").toString())));
								String second = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(list.get(i).get("second").toString())));
								String FinalData = year + month + data + hour + min + second;
								// ʱ��(byte����)
								byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
								// �߲˵�ASII��(byte����)
								byte[] SampleByte =new byte[6];
								int nameLength=list.get(i).get("name").toString().length();
								if(nameLength==6){
									SampleByte = DecodeUtils.gbkToString(list.get(i).get("name").toString());
								}else{
									ByteArrayOutputStream outputStream=new ByteArrayOutputStream();		
									outputStream.write(DecodeUtils.gbkToString(list.get(i).get("name").toString()));
									for(int j=outputStream.toByteArray().length;j<6;j++){
										outputStream.write(0x20);
									}
									SampleByte=outputStream.toByteArray();
								}
								// ������(byte����)
								int FYiZhiLvVal = (int) (Double.valueOf(list.get(i).get("InhibitionRate").toString()) * 100);
								String FYiZhiLvVal1 = DecodeUtils.AddZeroToFour(Integer.toHexString(FYiZhiLvVal));
								String FYiZhiLvVal1Head = (String) FYiZhiLvVal1.substring(0, 2);
								String FYiZhiLvVal1Tail = (String) FYiZhiLvVal1.substring(2);
								String FinalFYiZhiLvVal = FYiZhiLvVal1Tail + FYiZhiLvVal1Head;
								byte[] FYiZhiLvValByte = DecodeUtils.HexString2Bytes(FinalFYiZhiLvVal);
								//ƴ������byte
								byte[] synchronizationbyte = new byte[26];
								byte[] byteHead = new byte[] { 0x7E, 0x3A, 0x14, 0x00,0x01,0x00,0x01,0x00};
								byte[] byteNum = new byte[] {0x02, 0x00};
								byte[] byteTail = new byte[] {0x00, (byte) 0xAA };
								System.arraycopy(byteHead, 0, synchronizationbyte, 0, 8);
								System.arraycopy(byteNum, 0, synchronizationbyte, 8, 2);
								System.arraycopy(SampleByte, 0, synchronizationbyte, 10, 6);
								System.arraycopy(DataByte, 0, synchronizationbyte, 16, 6);
								System.arraycopy(FYiZhiLvValByte, 0, synchronizationbyte, 22, 2);
								System.arraycopy(byteTail, 0, synchronizationbyte, 24, 2);
								//�ְ�����
								byte[] firstByte=new byte[20];
								System.arraycopy(synchronizationbyte, 0, firstByte, 0, 20);
								mBluetoothLeService.write(firstByte);
								Thread.sleep(100);
								byte[] secondByte=new byte[6];
								System.arraycopy(synchronizationbyte, 20, secondByte, 0, 6);
								mBluetoothLeService.write(secondByte);
								Thread.sleep(2500);
//								// ƴ������byte
//								byte[] synchronizationbyte = new byte[26];
//								byte[] byteHead = new byte[] { 0x7E, 0x3A, 0x14, 0x00 };
//								byte[] byteallcount = DecodeUtils.HexString2Bytes(DecodeUtils.AddZeroToTwo(Integer.toHexString(countRecord)) + "00");
//								byte[] byteRecordNo = DecodeUtils.HexString2Bytes(DecodeUtils.AddZeroToTwo(Integer.toHexString(count)) + "00");
//								byte[] byteNum = new byte[] { 0x02, 0x00 };
//								byte[] byteTail = new byte[] { 0x00, (byte) 0xAA };
//								System.arraycopy(byteHead, 0, synchronizationbyte, 0, 4);
//								System.arraycopy(byteallcount, 0, synchronizationbyte, 4, 2);
//								System.arraycopy(byteRecordNo, 0, synchronizationbyte, 6, 2);
//								System.arraycopy(byteNum, 0, synchronizationbyte, 8, 2);
//								System.arraycopy(SampleByte, 0, synchronizationbyte, 10, 6);
//								System.arraycopy(DataByte, 0, synchronizationbyte, 16, 6);
//								System.arraycopy(FYiZhiLvValByte, 0, synchronizationbyte, 22, 2);
//								System.arraycopy(byteTail, 0, synchronizationbyte, 24, 2);
//								listallSynchronizationByte.add(synchronizationbyte);
//								Log.e("AAAAAAA", DecodeUtils.byte2HexStr(synchronizationbyte) + "");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					// ������ͬ����¼ƴ��Ϊһ��
//					allsynchronizationbyte = new byte[26 * countRecord];
//					for (int i = 0; i < listallSynchronizationByte.size(); i++) {
//						System.arraycopy(listallSynchronizationByte.get(i), 0, allsynchronizationbyte, 26 * i, 26);
//					}
//					Log.e("ͬ����¼", DecodeUtils.byte2HexStr(allsynchronizationbyte) + "");
//					// ͬ����ʼ����Ϊ����20���ֽڣ��������ͣ�����20��20�����ͣ������ʣ�µ�
//					for (int i = 0; i < 26 * countRecord / 20; i++) {
//						try {
//							byte[] firstByte = new byte[20];
//							System.arraycopy(allsynchronizationbyte, 20 * i, firstByte, 0, 20);
//							mBluetoothLeService.write(firstByte);
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//					int remainder = (int) (26 * countRecord % 20);
//					byte[] firstByte = new byte[remainder];
//					System.arraycopy(allsynchronizationbyte, 20 * listallSynchronizationByte.size(), firstByte, 0, remainder);
//					mBluetoothLeService.write(firstByte);
//					mPopupWindow.dismiss();
				}
			});
			return convertView;
		}

		public class ViewHolder {
			TextView tv_Number;
			TextView tv_Name;
			TextView tv_DetectionDate;
			TextView tv_DetectionTime;
			TextView tv_InhibitionRate;
			TextView tv_Result;
			CheckBox cb_selectRecord;
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
		case R.id.btn_upload:

			break;

		case R.id.btn_back:
			mPopupWindow.dismiss();
			break;
		case R.id.rl_back:
			getActivity().finish();
			break;
		case R.id.rl_popup_container:
			mPopupWindow.dismiss();
			break;
		case R.id.rl_startLoad:
			mListView.setVisibility(View.VISIBLE);
			rl_startLoad.setVisibility(View.GONE);
			String DATE = tv_queryTime.getText().toString();
			String year = DATE.substring(2, 4);
			String month = DATE.substring(5, 7);
			String day = DATE.substring(8);
			// ��ʼ��ѯ
			// ��ѯ����α�,�жϱ����Ƿ��Ѿ��������ˣ��еĻ�ֱ�Ӵӱ��ж�ȡ��û�еĻ����Ͳ�ѯָ��
			db=mMySQLiteHelper.getWritableDatabase();
			Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { month, day }, null, null, null);
			if (cursor.getCount() == 0) {
				// ʱ��(byte����)
				String FinalData = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(year))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(month)))
						+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(day)));
				byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
				byte[] queryByte = new byte[9];
				byte[] headByte = new byte[] { 0x7e, 0x10, 0x03, 0x00 };
				byte[] tailByte = new byte[] { 0x00, (byte) 0xaa };
				System.arraycopy(headByte, 0, queryByte, 0, 4);
				System.arraycopy(DataByte, 0, queryByte, 4, 3);
				System.arraycopy(tailByte, 0, queryByte, 7, 2);
				mBluetoothLeService.write(queryByte);
			} else {
				// ֱ�Ӵ����ݿ��ȡ��ѯ��¼
				Log.e("AAAAA", "ֱ�ӴӲ�ѯ��¼����ȡ����");
				listRecord = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					// �����е�����ֱ�Ӷ�ȡ �����2�е�ֵ
					Log.e("AAAAAA", cursor.getString(2));
					Map<String, Object> map = new HashMap<String, Object>();
					// ID
					map.put("id", cursor.getString(0));
					// ���
					map.put("number", cursor.getString(1));
					// ����
					map.put("name", cursor.getString(2));
					// ������
					map.put("year", cursor.getString(3));
					map.put("month", cursor.getString(4));
					map.put("data", cursor.getString(5));
					// ʱ����
					map.put("hour", cursor.getString(6));
					map.put("min", cursor.getString(7));
					map.put("second", cursor.getString(8));
					// ������
					map.put("InhibitionRate", cursor.getString(9));
					// ���
					map.put("result", cursor.getString(10));
					// ��ÿ���߲˵�ʱ����,�����ʣ��߲�����ȵ�ԭbyteװ��һ���¼��ϱ��ڴ�ӡ
					map.put("printByte", cursor.getBlob(11));
					// �Ƿ�ͬ��
					map.put("synchronization", cursor.getString(12));
					listRecord.add(map);
					if (listRecord.size() != 0) {
						mMyBaseAdapter = new MyBaseAdapter(listRecord);
						mListView.setAdapter(mMyBaseAdapter);
					}

				}
				cursor.close();
				db.close();
			}
			break;
		case R.id.rl_query_time:
			db=mMySQLiteHelper.getWritableDatabase();
			Calendar c = Calendar.getInstance();
			new DatePickerDialog(getActivity(), new OnDateSetListener() {

				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
					// ��������ʱ�Ȱ�֮ǰ����ʾ����ȥ��
					if (!listRecord.isEmpty()) {
						listRecord.clear();
						mMyBaseAdapter.notifyDataSetChanged();
					}
					mYear = String.valueOf(year).substring(2, 4);
					mMonth = DecodeUtils.AddZeroToTwo(String.valueOf(monthOfYear + 1));
					mDay = DecodeUtils.AddZeroToTwo(String.valueOf(dayOfMonth));
					tv_queryTime.setText(year + "-" + mMonth + "-" + mDay);
					// ��ʼ��ѯ
					// ��ѯ����α�,�жϱ����Ƿ��Ѿ��������ˣ��еĻ�ֱ�Ӵӱ��ж�ȡ��û�еĻ����Ͳ�ѯָ��
					Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { mMonth, mDay }, null, null, null);
					if (cursor.getCount() == 0) {
						// ʱ��(byte����)
						String FinalData = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(mYear)))
								+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(mMonth))) + DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(mDay)));
						byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
						byte[] queryByte = new byte[9];
						byte[] headByte = new byte[] { 0x7e, 0x10, 0x03, 0x00 };
						byte[] tailByte = new byte[] { 0x00, (byte) 0xaa };
						System.arraycopy(headByte, 0, queryByte, 0, 4);
						System.arraycopy(DataByte, 0, queryByte, 4, 3);
						System.arraycopy(tailByte, 0, queryByte, 7, 2);
						mBluetoothLeService.write(queryByte);
					} else {
						// ֱ�Ӵ����ݿ��ȡ��ѯ��¼
						Log.e("AAAAA", "ֱ�ӴӲ�ѯ��¼����ȡ����");
						listRecord = new ArrayList<Map<String, Object>>();
						while (cursor.moveToNext()) {
							// �����е�����ֱ�Ӷ�ȡ �����2�е�ֵ
							Log.e("AAAAAA", cursor.getString(2));
							Map<String, Object> map = new HashMap<String, Object>();
							// ID
							map.put("id", cursor.getString(0));
							// ���
							map.put("number", cursor.getString(1));
							// ����
							map.put("name", cursor.getString(2));
							// ������
							map.put("year", cursor.getString(3));
							map.put("month", cursor.getString(4));
							map.put("data", cursor.getString(5));
							// ʱ����
							map.put("hour", cursor.getString(6));
							map.put("min", cursor.getString(7));
							map.put("second", cursor.getString(8));
							// ������
							map.put("InhibitionRate", cursor.getString(9));
							// ���
							map.put("result", cursor.getString(10));
							// ��ÿ����ʱ����,�����ʣ��߲�����ȵ�ԭbyteװ��һ���¼��ϱ��ڴ�ӡ
							map.put("printByte", cursor.getBlob(11));
							// �Ƿ�ͬ��
							map.put("synchronization", cursor.getString(12));
							listRecord.add(map);
							if (listRecord.size() != 0) {
								mMyBaseAdapter = new MyBaseAdapter(listRecord);
								mListView.setAdapter(mMyBaseAdapter);
								if (mListView.getVisibility() == View.GONE) {
									mListView.setVisibility(View.VISIBLE);
									rl_startLoad.setVisibility(View.GONE);
								}
							}

						}
						cursor.close();
						db.close();
					}

				}
			}, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE)).show();

			break;
		}

	}

}
