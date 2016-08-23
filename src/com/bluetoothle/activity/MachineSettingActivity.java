package com.bluetoothle.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bluetoothle.utils.DataCleanManager;

import cm.example.lz_4000tbluetooth.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MachineSettingActivity extends Activity {
	private ListView lv_machineSetting;
	private SimpleAdapter simpleAdapter;
	private RelativeLayout rl_back;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private String[] stringItem = new String[] { "样品名称", "检测时间", "仪器时间", "wifi无线网", "UDP服务器", "以太网","清除本地样品表" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_machinesetting);
		ActivityManager.getActivityManager().addActivity(this);
		lv_machineSetting = (ListView) this.findViewById(R.id.lv_machineSetting);
		rl_back = (RelativeLayout) this.findViewById(R.id.rl_back);
		// 装载数据
		for (int i = 0; i < stringItem.length; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("item", stringItem[i]);
			list.add(map);
		}
		simpleAdapter = new SimpleAdapter(this, list, R.layout.lv_machinesetting_item, new String[] { "item" }, new int[] { R.id.tv_title });
		lv_machineSetting.setAdapter(simpleAdapter);
		rl_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				MachineSettingActivity.this.finish();

			}
		});

		// listview点击监听
		lv_machineSetting.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				switch (position) {
				case 0:
					Intent intent0= new Intent(MachineSettingActivity.this, SampleSettingActivity.class);
					startActivity(intent0);
					break;
				case 1:
					Intent intent1 = new Intent(MachineSettingActivity.this, DetectionTimeSettingActivity.class);
					startActivity(intent1);
					break;
				case 2:
					Intent intent2 = new Intent(MachineSettingActivity.this, MachineTimeSettingActivity.class);
					startActivity(intent2);
					break;
				case 3:
					Intent intent3 = new Intent(MachineSettingActivity.this, WifiSettingActivity.class);
					startActivity(intent3);
					break;
				case 4:
					Intent intent4= new Intent(MachineSettingActivity.this, UdpSettingActivity.class);
					startActivity(intent4);
					break;
				case 5:
					Intent intent5 = new Intent(MachineSettingActivity.this, EthernetSettingActivity.class);
					startActivity(intent5);
					break;
				case 6:
                 DataCleanManager.cleanDatabases(MachineSettingActivity.this);
                 Toast.makeText(MachineSettingActivity.this, "清除成功 ", 0).show();
					break;

				}
			}
		});
	}

}
