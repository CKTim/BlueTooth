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
	private HashMap<Integer, Boolean> isSelected; // 用于保存checkbox的状态
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
				db=mMySQLiteHelper.getWritableDatabase();
				Bundle bundle = intent.getBundleExtra(BluetoothLeService.EXTRA_DATA);
				final byte[] byteData = bundle.getByteArray(BluetoothLeService.EXTRA_DATA);
				// 查询记录返回结果数据转换装载
				if (byteData[0] == 0x7e && byteData[1] == 0x11) {
					listRecord = new ArrayList<Map<String, Object>>();
					Log.e("byteData.length",byteData.length+"");
					for (int i = 0; i < byteData.length; i++) {
						if (byteData[i] == 0x7e && byteData[i+1] == 0x11) {
						try {
							String number = DecodeUtils.AddZeroToFour(String.valueOf(byteData[i + 8] + byteData[i + 9] * 256));// 编号
							// 名称
							byte[] byteVegetableType = new byte[] { byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13], byteData[i + 14], byteData[i + 15] };
							String vegetableName = DecodeUtils.stringToGbk(DecodeUtils.bytesToHexString(byteVegetableType));
							// 年月日时分秒
							int year = byteData[i + 16];
							String month = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 17]));
							String day = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 18]));
							String hour = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 19]));
							String min = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 20]));
							String second = DecodeUtils.AddZeroToTwo(String.valueOf(byteData[i + 21]));
							// 抑制率
							double a = Integer.parseInt(DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 22] }, 10));
							double b = Integer.parseInt(DecodeUtils.bytesToAllHex(new byte[] { byteData[i + 23] }, 10)) * 256;
							double c = (a + b) / 100;
							String result = String.format("%.2f", c);
							// 结果
							String Finalresult = "";
							if ((byteData[i + 22] + byteData[i + 23] * 256) > 5000) {
								Finalresult = "阳性";
							} else {
								Finalresult = "阴性";
							}
							// 将每个蔬菜的时分秒,抑制率，蔬菜种类等的原byte装进一个新集合便于打印
							byte[] printByte = new byte[] { byteData[i + 8], byteData[i + 9], byteData[i + 10], byteData[i + 11], byteData[i + 12], byteData[i + 13],
									byteData[i + 14], byteData[i + 15], byteData[i + 16], byteData[i + 17], byteData[i + 18], byteData[i + 19], byteData[i + 20], byteData[i + 21],
									byteData[i + 22], byteData[i + 23] };
							// 储存到数据库
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
							//装载到list
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
							// 判断是否是刷新请求，如果是刷新的请求，则要将该日期下的未同步数据也显示出来
							if (mSwipeRefreshLayout.isRefreshing()) {
								// 更新数据
								Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { month, day }, null, null, null);
								listRecord = new ArrayList<Map<String, Object>>();
								while (cursor.moveToNext()) {
									// 根据列的索引直接读取 比如第2列的值
									Map<String, Object> map_updata = new HashMap<String, Object>();
									// ID
									map_updata.put("id", cursor.getString(0));
									// 编号
									map_updata.put("number", cursor.getString(1));
									// 名称
									map_updata.put("name", cursor.getString(2));
									// 年月日
									map_updata.put("year", cursor.getString(3));
									map_updata.put("month", cursor.getString(4));
									map_updata.put("data", cursor.getString(5));
									// 时分秒
									map_updata.put("hour", cursor.getString(6));
									map_updata.put("min", cursor.getString(7));
									map_updata.put("second", cursor.getString(8));
									// 抑制率
									map_updata.put("InhibitionRate", cursor.getString(9));
									// 结果
									map_updata.put("result", cursor.getString(10));
									// 将每个蔬菜的时分秒,抑制率，蔬菜种类等的原byte装进一个新集合便于打印
									map_updata.put("printByte", cursor.getBlob(11));
									// 是否同步
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

				// 检测记录同步成功返回
				if (byteData[0] == 0x7e && byteData[1] == 0x3B && byteData[4] == 0x01) {
					db = mMySQLiteHelper.getWritableDatabase();
					for (int i = 0; i < listRecord.size(); i++) {
						if (isSelected.get(i)) {
							// 同步数据库修改
							ContentValues values = new ContentValues();
							values.put("synchronization", "true");
							db.update("record", values, "_id=?", new String[] { listRecord.get(i).get("id").toString() });
							listRecord.get(i).put("synchronization", "true");
						}
					}
					mMyBaseAdapter.notifyDataSetChanged();
					db.close();
				}
//				 //打印结果，看有没有装载成功
//				 for (int i = 0; i < listRecord.size(); i++) {
//				 Log.e("AAAAAA", "list大小为:" + listRecord.size() + "编号为:"
//				 + listRecord.get(i).get("number") + "蔬菜种类为:"
//				 + listRecord.get(i).get("name") + "年份为:"
//				 + listRecord.get(i).get("year") + "抑制率为:"
//				 + listRecord.get(i).get("InhibitionRate"));
//				 }
				db.close();
			}
		}
	};

	// 建立一个广播来接收检测完的返回数据
	private BroadcastReceiver mUpdataBrocast = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("com.example.bluetooth.le.UPDATA")) {
				db=mMySQLiteHelper.getWritableDatabase();
				Log.e("AAAAAAAA", "收到刷新的广播");
				Bundle bundle = intent.getExtras();
				// 储存到数据库
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
				Log.e("AAAAA", "更新记录表 ");
				// 更新数据
				Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { bundle.getString("month"), bundle.getString("data") }, null, null, null);
				listRecord = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					// 根据列的索引直接读取 比如第2列的值
					Map<String, Object> map = new HashMap<String, Object>();
					// ID
					map.put("id", cursor.getString(0));
					// 编号
					map.put("number", cursor.getString(1));
					// 名称
					map.put("name", cursor.getString(2));
					// 年月日
					map.put("year", cursor.getString(3));
					map.put("month", cursor.getString(4));
					map.put("data", cursor.getString(5));
					// 时分秒
					map.put("hour", cursor.getString(6));
					map.put("min", cursor.getString(7));
					map.put("second", cursor.getString(8));
					// 抑制率
					map.put("InhibitionRate", cursor.getString(9));
					// 结果
					map.put("result", cursor.getString(10));
					// 将每个蔬菜的时分秒,抑制率，蔬菜种类等的原byte装进一个新集合便于打印
					map.put("printByte", cursor.getBlob(11));
					// 是否同步
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
		// 加载PopupWindow的布局
		PopupWindowView = LayoutInflater.from(getActivity()).inflate(R.layout.popupwindow_custom_view, null);
		init();
		// 绑定服务
		Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
		// 长按监听事件
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// 长按显示操作
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
		// 先获取当前年月日，显示在标题上
		Calendar c = Calendar.getInstance();
		Year = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.YEAR)));
		Month = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.MONTH) + 1));
		Day = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.DATE)));
		tv_queryTime.setText(Year + "-" + Month + "-" + Day);
		// 初始化MySQLiteHelper
		mMySQLiteHelper = new MySQLiteHelper(getActivity());
		// 初始化广播管理者
		broadcastManager = LocalBroadcastManager.getInstance(getActivity());
		// 下拉刷新监听
		mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.lightBlue));
		mSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				// 下拉刷新时删除数据库该天数据在重新获取
				db = mMySQLiteHelper.getWritableDatabase();
				String DATE = tv_queryTime.getText().toString();
				String year = DATE.substring(2, 4);
				String month = DATE.substring(5, 7);
				String day = DATE.substring(8);
				db.delete("record", "month=? and data=? and synchronization=?", new String[] { month, day, "true" });
				db.close();
				// 删除后重新读取当天数据
				// 时间(byte类型)装载
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
				//设置8s后自动关闭刷新
				final Handler handler=new Handler();
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						if(mSwipeRefreshLayout.isRefreshing()){
							mSwipeRefreshLayout.setRefreshing(false);
//							Toast.makeText(getActivity(), "暂时没有更多数据",0).show();
						}
					}
				}, 8000);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		// 注册广播
		getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.example.bluetooth.le.UPDATA");
		broadcastManager.registerReceiver(mUpdataBrocast, intentFilter);
		Log.e("AAAAAAAA", "注册mUpdataBrocast广播和mGattUpdateReceiver广播");
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
		// 解绑服务和解注册广播
		getActivity().unbindService(mServiceConnection);
		getActivity().unregisterReceiver(mGattUpdateReceiver);
	}

	// @Override
	// public void onHiddenChanged(boolean hidden) {
	// super.onHiddenChanged(hidden);
	// if(hidden){
	// Log.e("AAAAA","QRFragment被隐藏，解注册广播");
	// // 解注册广播
	// getActivity().unregisterReceiver(mGattUpdateReceiver);
	// }else{
	// getActivity().registerReceiver(mGattUpdateReceiver,
	// makeGattUpdateIntentFilter());
	// Log.e("AAAAA","QRFragment被显示，注册广播");
	// }
	// }

	// 自定义一个适配器
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
			// 如果是同步过的数据则显示为灰色，没有的话则显示为蓝色
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

			// checkbox的点击事件,点击一次就才保存一次状态
			viewHolder.cb_selectRecord.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					isSelected.put(position, isChecked);
				}
			});
			viewHolder.cb_selectRecord.setChecked(isSelected.get(position));

			// 全选按钮监听
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

			// 打印按钮的监听
			btn_print.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					for (int i = 0; i < list.size(); i++) {
						if (isSelected.get(i)) {
							byte[] flag = new byte[] { 0x7e, 0x38, 0x10, 0x00 };
							byte[] data = (byte[]) listRecord.get(i).get("printByte");
							byte[] end = new byte[] { 0x00, (byte) 0xAA };
							// 拼接三个byte[]组成一个新的byte[]
							byte[] sendData = new byte[22];
							System.arraycopy(flag, 0, sendData, 0, 4);
							System.arraycopy(data, 0, sendData, 4, 16);
							System.arraycopy(end, 0, sendData, 20, 2);
							// 数据大于20个字节，分批发送
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

			// 同步按钮监听事件
			btn_synchronization.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					int count = 0;//当前是第几条数据
					int countRecord = 0;//同步的记录数
					ArrayList<byte[]> listallSynchronizationByte = new ArrayList<byte[]>();
					// 获取总共要同步的记录条数
					for (int i = 0; i < list.size(); i++) {
						if (isSelected.get(i)) {
							countRecord = countRecord + 1;
						}
					}
					// 拼接每一条同步记录
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
								// 时间(byte类型)
								byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
								// 蔬菜的ASII码(byte类型)
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
								// 抑制率(byte类型)
								int FYiZhiLvVal = (int) (Double.valueOf(list.get(i).get("InhibitionRate").toString()) * 100);
								String FYiZhiLvVal1 = DecodeUtils.AddZeroToFour(Integer.toHexString(FYiZhiLvVal));
								String FYiZhiLvVal1Head = (String) FYiZhiLvVal1.substring(0, 2);
								String FYiZhiLvVal1Tail = (String) FYiZhiLvVal1.substring(2);
								String FinalFYiZhiLvVal = FYiZhiLvVal1Tail + FYiZhiLvVal1Head;
								byte[] FYiZhiLvValByte = DecodeUtils.HexString2Bytes(FinalFYiZhiLvVal);
								//拼接所有byte
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
								//分包发送
								byte[] firstByte=new byte[20];
								System.arraycopy(synchronizationbyte, 0, firstByte, 0, 20);
								mBluetoothLeService.write(firstByte);
								Thread.sleep(100);
								byte[] secondByte=new byte[6];
								System.arraycopy(synchronizationbyte, 20, secondByte, 0, 6);
								mBluetoothLeService.write(secondByte);
								Thread.sleep(2500);
//								// 拼接所有byte
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
					// 将所有同步记录拼接为一条
//					allsynchronizationbyte = new byte[26 * countRecord];
//					for (int i = 0; i < listallSynchronizationByte.size(); i++) {
//						System.arraycopy(listallSynchronizationByte.get(i), 0, allsynchronizationbyte, 26 * i, 26);
//					}
//					Log.e("同步记录", DecodeUtils.byte2HexStr(allsynchronizationbyte) + "");
//					// 同步开始（因为大于20个字节，分批发送），先20条20条发送，最后发送剩下的
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
			// 开始查询
			// 查询获得游标,判断表中是否已经有数据了，有的话直接从表中读取，没有的话发送查询指令
			db=mMySQLiteHelper.getWritableDatabase();
			Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { month, day }, null, null, null);
			if (cursor.getCount() == 0) {
				// 时间(byte类型)
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
				// 直接从数据库读取查询记录
				Log.e("AAAAA", "直接从查询记录表格读取数据");
				listRecord = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					// 根据列的索引直接读取 比如第2列的值
					Log.e("AAAAAA", cursor.getString(2));
					Map<String, Object> map = new HashMap<String, Object>();
					// ID
					map.put("id", cursor.getString(0));
					// 编号
					map.put("number", cursor.getString(1));
					// 名称
					map.put("name", cursor.getString(2));
					// 年月日
					map.put("year", cursor.getString(3));
					map.put("month", cursor.getString(4));
					map.put("data", cursor.getString(5));
					// 时分秒
					map.put("hour", cursor.getString(6));
					map.put("min", cursor.getString(7));
					map.put("second", cursor.getString(8));
					// 抑制率
					map.put("InhibitionRate", cursor.getString(9));
					// 结果
					map.put("result", cursor.getString(10));
					// 将每个蔬菜的时分秒,抑制率，蔬菜种类等的原byte装进一个新集合便于打印
					map.put("printByte", cursor.getBlob(11));
					// 是否同步
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
					// 跟换日期时先把之前的显示数据去掉
					if (!listRecord.isEmpty()) {
						listRecord.clear();
						mMyBaseAdapter.notifyDataSetChanged();
					}
					mYear = String.valueOf(year).substring(2, 4);
					mMonth = DecodeUtils.AddZeroToTwo(String.valueOf(monthOfYear + 1));
					mDay = DecodeUtils.AddZeroToTwo(String.valueOf(dayOfMonth));
					tv_queryTime.setText(year + "-" + mMonth + "-" + mDay);
					// 开始查询
					// 查询获得游标,判断表中是否已经有数据了，有的话直接从表中读取，没有的话发送查询指令
					Cursor cursor = db.query("record", null, "month=? and data=?", new String[] { mMonth, mDay }, null, null, null);
					if (cursor.getCount() == 0) {
						// 时间(byte类型)
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
						// 直接从数据库读取查询记录
						Log.e("AAAAA", "直接从查询记录表格读取数据");
						listRecord = new ArrayList<Map<String, Object>>();
						while (cursor.moveToNext()) {
							// 根据列的索引直接读取 比如第2列的值
							Log.e("AAAAAA", cursor.getString(2));
							Map<String, Object> map = new HashMap<String, Object>();
							// ID
							map.put("id", cursor.getString(0));
							// 编号
							map.put("number", cursor.getString(1));
							// 名称
							map.put("name", cursor.getString(2));
							// 年月日
							map.put("year", cursor.getString(3));
							map.put("month", cursor.getString(4));
							map.put("data", cursor.getString(5));
							// 时分秒
							map.put("hour", cursor.getString(6));
							map.put("min", cursor.getString(7));
							map.put("second", cursor.getString(8));
							// 抑制率
							map.put("InhibitionRate", cursor.getString(9));
							// 结果
							map.put("result", cursor.getString(10));
							// 将每个的时分秒,抑制率，蔬菜种类等的原byte装进一个新集合便于打印
							map.put("printByte", cursor.getBlob(11));
							// 是否同步
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
