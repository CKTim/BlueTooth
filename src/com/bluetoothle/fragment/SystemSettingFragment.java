package com.bluetoothle.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluetoothle.activity.DetectionTimeSettingActivity;
import com.bluetoothle.activity.EthernetSettingActivity;
import com.bluetoothle.activity.MachineSettingActivity;
import com.bluetoothle.activity.MachineTimeSettingActivity;
import com.bluetoothle.activity.SampleSettingActivity;
import com.bluetoothle.activity.UdpSettingActivity;
import com.bluetoothle.activity.WifiSettingActivity;
import com.bluetoothle.db.MySQLiteHelper;
import com.bluetoothle.utils.DataCleanManager;

import cm.example.lz_4000tbluetooth.R;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SystemSettingFragment extends Fragment implements OnClickListener {
	private ListView lv_machineSetting;
	private SimpleAdapter simpleAdapter;
	private RelativeLayout rl_back;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private String[] stringItem = new String[] { "样品名称", "检测时间", "仪器时间", "wifi无线网", "UDP服务器", "以太网", "清除本地样品表","清除本地记录表" };
	private View v;
	private MySQLiteHelper mMySQLiteHelper;
	private SQLiteDatabase mSQLiteDatabase;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.activity_machinesetting, null);
		lv_machineSetting = (ListView)v.findViewById(R.id.lv_machineSetting);
		rl_back = (RelativeLayout) v.findViewById(R.id.rl_back);
		// 装载数据
		for (int i = 0; i < stringItem.length; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("item", stringItem[i]);
			list.add(map);
		}
		simpleAdapter = new SimpleAdapter(getActivity(), list, R.layout.lv_machinesetting_item, new String[] { "item" }, new int[] { R.id.tv_title });
		lv_machineSetting.setAdapter(simpleAdapter);
		rl_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getActivity().finish();

			}
		});

		// listview点击监听
		lv_machineSetting.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
				case 0:
					Intent intent0 = new Intent(getActivity(), SampleSettingActivity.class);
					startActivity(intent0);
					break;
				case 1:
					Intent intent1 = new Intent(getActivity(), DetectionTimeSettingActivity.class);
					startActivity(intent1);
					break;
				case 2:
					Intent intent2 = new Intent(getActivity(), MachineTimeSettingActivity.class);
					startActivity(intent2);
					break;
				case 3:
					Intent intent3 = new Intent(getActivity(), WifiSettingActivity.class);
					startActivity(intent3);
					break;
				case 4:
					Intent intent4 = new Intent(getActivity(), UdpSettingActivity.class);
					startActivity(intent4);
					break;
				case 5:
					Intent intent5 = new Intent(getActivity(), EthernetSettingActivity.class);
					startActivity(intent5);
					break;
				case 6:
					mMySQLiteHelper=new MySQLiteHelper(getActivity());
					mSQLiteDatabase=mMySQLiteHelper.getWritableDatabase();
					mSQLiteDatabase.delete("sample", null, null);
					mSQLiteDatabase.close();
					Toast.makeText(getActivity(), "成功清除本地样品表", 0).show();
					break;
				case 7:
					mMySQLiteHelper=new MySQLiteHelper(getActivity());
					mSQLiteDatabase=mMySQLiteHelper.getWritableDatabase();
					mSQLiteDatabase.delete("record", null, null);
					mSQLiteDatabase.close();
					Toast.makeText(getActivity(), "成功清除本地记录表", 0).show();
					break;
				}
			}
		});
		// init();
		return v;
	}

	// private void init() {
	// rl_back = (RelativeLayout) v.findViewById(R.id.rl_back);
	// rl_WhiteBalance = (RelativeLayout) v.findViewById(R.id.rl_WhiteBalance);
	// rl_ChannelCalibration = (RelativeLayout)
	// v.findViewById(R.id.rl_ChannelCalibration);
	// rl_InstrumentSetting = (RelativeLayout)
	// v.findViewById(R.id.rl_InstrumentSetting);
	// rl_back.setOnClickListener(this);
	// rl_WhiteBalance.setOnClickListener(this);
	// rl_ChannelCalibration.setOnClickListener(this);
	// rl_InstrumentSetting.setOnClickListener(this);
	// }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_back:
			getActivity().finish();
			break;
		}

	}
}
