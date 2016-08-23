package com.bluetoothle.activity;

import com.bluetoothle.fragment.QueryRecordFragment;
import com.bluetoothle.fragment.SampleDetectionFragment;
import com.bluetoothle.fragment.SystemSettingFragment;

import cm.example.lz_4000tbluetooth.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class FunctionActivity extends FragmentActivity implements OnClickListener {
	private Button btn_SampleDetection, btn_QueryRecord, btn_SystemSettings;
	private FragmentManager mManager;
	private FragmentTransaction mTransaction;
	private SampleDetectionFragment mSampleDetectionFragment = new SampleDetectionFragment();
	private QueryRecordFragment mQueryRecordFragment = new QueryRecordFragment();
	private SystemSettingFragment mSystemSettingFragment = new SystemSettingFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_function);
		ActivityManager.getActivityManager().addActivity(this);
		init();
		addFragment();
	}

	private void addFragment() {
		mTransaction.add(R.id.rl_fragment_container, mSampleDetectionFragment);
		mTransaction.add(R.id.rl_fragment_container, mQueryRecordFragment);
		mTransaction.add(R.id.rl_fragment_container, mSystemSettingFragment);
		// 判断是要加载哪一个fragment，并将相应的按钮设置为蓝色
		String type = getIntent().getStringExtra("type").toString();
		if (type.equals("a")) {
			showFragment(mSampleDetectionFragment);
		}
		if (type.equals("b")) {
			showFragment(mQueryRecordFragment);
		}
		if (type.equals("c")) {
			showFragment(mSystemSettingFragment);
		}
		mTransaction.commit();
	}

	private void init() {
		btn_QueryRecord = (Button) this.findViewById(R.id.btn_QueryRecord);
		btn_SampleDetection = (Button) this.findViewById(R.id.btn_SampleDetection);
		btn_SystemSettings = (Button) this.findViewById(R.id.btn_SystemSettings);
		btn_QueryRecord.setOnClickListener(this);
		btn_SampleDetection.setOnClickListener(this);
		btn_SystemSettings.setOnClickListener(this);
		mManager = getSupportFragmentManager();
		mTransaction = mManager.beginTransaction();
	}

	@Override
	public void onClick(View v) {
		mTransaction = mManager.beginTransaction();
		switch (v.getId()) {
		case R.id.btn_SampleDetection:
			showFragment(mSampleDetectionFragment);
			break;

		case R.id.btn_QueryRecord:
			showFragment(mQueryRecordFragment);
			break;

		case R.id.btn_SystemSettings:
			showFragment(mSystemSettingFragment);
			break;
		}
		mTransaction.commit();

	}

	// 动态隐藏显示fragment
	private void showFragment(Fragment fragment) {
		mTransaction.hide(mSampleDetectionFragment);
		mTransaction.hide(mQueryRecordFragment);
		mTransaction.hide(mSystemSettingFragment);
		btn_SampleDetection.setSelected(false);
		btn_QueryRecord.setSelected(false);
		btn_SystemSettings.setSelected(false);
		if (fragment == mSampleDetectionFragment) {
			mTransaction.show(mSampleDetectionFragment);
			btn_SampleDetection.setSelected(true);
		}
		if (fragment == mQueryRecordFragment) {
			mTransaction.show(mQueryRecordFragment);
			btn_QueryRecord.setSelected(true);
		}
		if (fragment == mSystemSettingFragment) {
			mTransaction.show(mSystemSettingFragment);
			btn_SystemSettings.setSelected(true);
		}
	}
}
