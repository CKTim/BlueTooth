package com.bluetoothle.fragment;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.bluetoothle.activity.SampleSearchActivity;
import com.bluetoothle.service.BluetoothLeService;
import com.bluetoothle.utils.CalInhiRateUtils;
import com.bluetoothle.utils.DecodeUtils;

import cm.example.lz_4000tbluetooth.R;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SampleDetectionFragment extends Fragment implements OnClickListener {
	private View v;
	private RelativeLayout rl_back;
	private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	private MyBaseAdapter mMyBaseAdapter = new MyBaseAdapter();
	private BluetoothLeService mBluetoothLeService;
	private ListView mListview;
	private Button btn_clearAll;
	private double nYingPinVal;// 样品测试值
	private double nCompareVal;// 对照值
	private double fYiZhiLvVal;// 抑制率
	private int year;
	private String month;
	private String data;
	private String hour;
	private String min;
	private String second;
	private String result;// 结果
	private Timer timer_compare;
	private Timer timer_sample;
	private Map<Integer, Object> map_compare = new HashMap<Integer, Object>();
	private Map<Integer, Object> map_sample = new HashMap<Integer, Object>();
	private SharedPreferences sp;
	private String DetectionTime;
	private MyThread myThread = new MyThread();
	private boolean flag = true;
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
				// 对照检测读取值返回
				if (byteData[0] == 0x7e && byteData[1] == 0x37) {
					// 将十六进制的样品检测值和检测通道解析出来
					int accessway = Integer.parseInt(DecodeUtils.bytesToHexString(new byte[] { byteData[4] }));
					boolean ifcompare = (boolean) list.get(accessway - 1).get("ifcompare");
					if (ifcompare) {
						try {
							Log.e("对照值返回", DecodeUtils.byte2HexStr(byteData) + "");
							// 将对照值存放起来
							Editor editor = sp.edit();
							String CompareVal = DecodeUtils.bytesToAllHex(new byte[] { byteData[5] }, 10);
							editor.putFloat("CompareValue", Float.parseFloat(CompareVal));
							editor.commit();
							// 心跳包结束发送
							if (map_sample.size() == 0 && map_compare.size() == 0) {
								flag = false;
								// 停止远程控制
								byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
								mBluetoothLeService.write(stopControlByte);
								Log.e("AAAAAAA", "停止远程控制");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// 样品检测读取值返回
						if (byteData[0] == 0x7e && byteData[1] == 0x37) {
							try {
								Log.e("检测值返回", DecodeUtils.byte2HexStr(byteData) + "");
								String YingPinVal = DecodeUtils.bytesToAllHex(new byte[] { byteData[5] }, 10);
								Log.e("检测值为:", YingPinVal);
								Log.e("本地对照值为", sp.getFloat("CompareValue",0)+"");
								nYingPinVal = Double.valueOf(YingPinVal);
								// 开始计算抑制率，先获取保存到本地的对照值
								nCompareVal = (double) sp.getFloat("CompareValue", 0);
								Log.e("AAAAAA", "本地的对照值为" + nCompareVal);
								fYiZhiLvVal = CalInhiRateUtils.CalInhiRate(nCompareVal, nYingPinVal);
								Log.e("抑制率为", fYiZhiLvVal + "%");
								// 装载到list中
								if (fYiZhiLvVal > 50) {
									list.get(accessway - 1).put("result", "阳性");
									result = "阳性";
								} else {
									list.get(accessway - 1).put("result", "阴性");
									result = "阴性";
								}
								list.get(accessway - 1).put("fYiZhiLvVal", String.format("%.2f", fYiZhiLvVal) + "%");
								mMyBaseAdapter.notifyDataSetChanged();
								// 心跳包结束发送
								Log.e("map_sample.size()", map_sample.size() + "");
								if (map_sample.size() == 0 && map_compare.size() == 0) {
									flag = false;
									// 停止远程控制
									byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
									mBluetoothLeService.write(stopControlByte);
									Log.e("AAAAAAA", "停止远程控制");
								}
								// 做完检测后发送一个广播回检测记录fragment更新数据
								// 先获取当前年月日
								Calendar c = Calendar.getInstance();
								year = c.get(Calendar.YEAR);
								month = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.MONTH) + 1));
								data = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.DATE)));
								hour = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.HOUR)));
								min = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.MINUTE)));
								second = DecodeUtils.AddZeroToTwo(String.valueOf(c.get(Calendar.SECOND)));
								String year1 = String.valueOf(year);
								String year2 = (String) year1.substring(2);
								String FinalData = DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(year2)))
										+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(month)))
										+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(data)))
										+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(hour)))
										+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(min)))
										+ DecodeUtils.AddZeroToTwo(Integer.toHexString(Integer.parseInt(second)));
								// 时间(byte类型)
								byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
								// 蔬菜的ASII码(byte类型)
								String vegetableName=list.get(accessway - 1).get("SampleName").toString();
								if(vegetableName.equals("选择样品")){
									vegetableName="空";
								}
								ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
								outputStream.write(DecodeUtils.gbkToString(vegetableName));
								for(int i=vegetableName.length()*2;i<6;i++){
									outputStream.write(0x20);
								}
								byte[] SampleByte = outputStream.toByteArray();
								// 抑制率(byte类型)
								int FYiZhiLvVal = (int) (fYiZhiLvVal * 100);
								String FYiZhiLvVal1 = DecodeUtils.AddZeroToFour(Integer.toHexString(FYiZhiLvVal));
								String FYiZhiLvVal1Head = (String) FYiZhiLvVal1.substring(0, 2);
								String FYiZhiLvVal1Tail = (String) FYiZhiLvVal1.substring(2);
								String FinalFYiZhiLvVal = FYiZhiLvVal1Tail + FYiZhiLvVal1Head;
								byte[] FYiZhiLvValByte = DecodeUtils.HexString2Bytes(FinalFYiZhiLvVal);
								// 拼接所有byte
								byte[] printebyte = new byte[16];
								byte[] byteHead = new byte[] { 0x10, 0x00 };
								System.arraycopy(byteHead, 0, printebyte, 0, 2);
								System.arraycopy(SampleByte, 0, printebyte, 2, 6);
								System.arraycopy(DataByte, 0, printebyte, 8, 6);
								System.arraycopy(FYiZhiLvValByte, 0, printebyte, 14, 2);
								Intent UpdataIntent = new Intent();
								UpdataIntent.setAction("com.example.bluetooth.le.UPDATA");
								Bundle Updatabundle = new Bundle();
								Updatabundle.putString("number", "0010");
								if (list.get(accessway - 1).get("SampleName").toString().equals("选择样品")) {
									list.get(accessway - 1).put("SampleName", "空");
								}
								Updatabundle.putString("name", list.get(accessway - 1).get("SampleName") + "");
								Updatabundle.putString("year", year2);
								Updatabundle.putString("month", month);
								Updatabundle.putString("data", data);
								Updatabundle.putString("hour", hour);
								Updatabundle.putString("min", min);
								Updatabundle.putString("second", second);
								Updatabundle.putString("InhibitionRate", String.format("%.2f", fYiZhiLvVal) + "");
								Updatabundle.putString("result", result);
								Updatabundle.putByteArray("printByte", printebyte);
								UpdataIntent.putExtras(Updatabundle);
								LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(UpdataIntent);
								Log.e("Aaaaa", "发送消息记录的广播");
							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					}

				}
				// 开始远程控制响应
				if (byteData[0] == 0x7e && byteData[1] == 0x31) {
					if (!myThread.isAlive()) {
						flag = true;
						myThread = new MyThread();
						myThread.start();
						Log.e("AAAAAA", "心跳包线程开始");
					}
				}
			}
		}
	};
	// 对照检测Handler
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int sec = (int) msg.obj;
			list.get(msg.what).put("CountDown", sec);
			mMyBaseAdapter.notifyDataSetChanged();
			// 到达0s时发送读取检测值指令
			if (sec == 0) {
				if (mBluetoothLeService == null) {
					Log.e("SampleDetectionFragment", "mBluetoothLeService为空，可能还没有进行绑定");
				} else {
					// 先获取是在哪一个通道做的对照检测
					String AccessWay = DecodeUtils.AddZeroToTwo(list.get(msg.what).get("AccessWayNum").toString());
					// 装填数据
					String sendData = "7e360100" + AccessWay + "00aa";
					byte[] sendbyte = DecodeUtils.HexString2Bytes(sendData);
					mBluetoothLeService.write(sendbyte);
				}
			}

		};
	};
	// 样品检测Handler
	private Handler mHandler2 = new Handler() {
		public void handleMessage(Message msg) {
			int sec = (int) msg.obj;
			list.get(msg.what).put("CountDown", sec);
			mMyBaseAdapter.notifyDataSetChanged();
			// 到达0s时发送读取检测值指令
			if (sec == 0) {
				if (mBluetoothLeService == null) {
					Log.e("SampleDetectionFragment", "mBluetoothLeService为空，可能还没有进行绑定");
				} else {
					// 先获取是在哪一个通道做的对照检测
					String AccessWay = DecodeUtils.AddZeroToTwo(list.get(msg.what).get("AccessWayNum").toString());
					// 装填发送数据
					String sendData = "7e360100" + AccessWay + "00aa";
					byte[] sendbyte = DecodeUtils.HexString2Bytes(sendData);
					mBluetoothLeService.write(sendbyte);
				}
			}
		};
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.fragment_sample_detection, null);
		init();
		// 绑定服务
		Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
		// 添加默认数据到list
		for (int i = 0; i < 10; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("AccessWayNum", i + 1);
			map.put("SampleName", "选择样品");
			map.put("CountDown",DetectionTime);
			map.put("CompareButton", false);
			map.put("SampleButton", false);
			map.put("fYiZhiLvVal", "");
			map.put("result", "");
			map.put("ifcompare", false);
			list.add(map);
		}
		mListview.setAdapter(mMyBaseAdapter);
		return v;
	}

	private void init() {
		mListview = (ListView) v.findViewById(R.id.lv_sampleDetection);
		btn_clearAll = (Button) v.findViewById(R.id.btn_clearAll);
		rl_back = (RelativeLayout) v.findViewById(R.id.rl_back);
		btn_clearAll.setOnClickListener(this);
		rl_back.setOnClickListener(this);
		//获得保存的默认检测时间
		sp = getActivity().getSharedPreferences("LZ-4000(T)", getActivity().MODE_PRIVATE);
		DetectionTime=sp.getString("DetectionTime","480");
	}

	@Override
	public void onResume() {
		super.onResume();
		// 注册广播
		getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mGattUpdateReceiver);
		getActivity().unbindService(mServiceConnection);
	}

	//
	// @Override
	// public void onHiddenChanged(boolean hidden) {
	// super.onHiddenChanged(hidden);
	// if (hidden) {
	// Log.e("AAAAA", "SDFragment被隐藏，解注册广播");
	// // 解注册广播
	// getActivity().unregisterReceiver(mGattUpdateReceiver);
	// } else {
	// getActivity().registerReceiver(mGattUpdateReceiver,
	// makeGattUpdateIntentFilter());
	// Log.e("AAAAA", "SDFragment被显示，注册广播");
	// }
	// }

	// 自定义一个适配器
	public class MyBaseAdapter extends BaseAdapter {

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

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.lv_sampledetection_item, null);
				viewHolder.tv_Number = (TextView) convertView.findViewById(R.id.tv_Number);
				viewHolder.tv_AccessWay = (TextView) convertView.findViewById(R.id.tv_AccessWay);
				viewHolder.btn_SelectName = (Button) convertView.findViewById(R.id.btn_SelectName);
				viewHolder.tv_Time = (TextView) convertView.findViewById(R.id.tv_Time);
				viewHolder.tv_InhibitionRate = (TextView) convertView.findViewById(R.id.tv_InhibitionRate);
				viewHolder.tv_Result = (TextView) convertView.findViewById(R.id.tv_Result);
				viewHolder.btn_Compare = (Button) convertView.findViewById(R.id.btn_Compare);
				viewHolder.btn_Sample = (Button) convertView.findViewById(R.id.btn_Sample);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tv_AccessWay.setText(list.get(position).get("AccessWayNum").toString());
			viewHolder.btn_SelectName.setText(list.get(position).get("SampleName").toString());
			viewHolder.tv_InhibitionRate.setText(list.get(position).get("fYiZhiLvVal").toString());
			viewHolder.tv_Result.setText(list.get(position).get("result").toString());
			viewHolder.btn_Compare.setSelected((boolean) list.get(position).get("CompareButton"));
			viewHolder.btn_Sample.setSelected((boolean) list.get(position).get("SampleButton"));
			// 设置倒计时的时间
			if (Integer.parseInt((list.get(position).get("CountDown").toString())) <= 0) {
				// 关闭之前先获取是要关闭哪一个
				Timer timer = (Timer) map_sample.get(position);
				Timer timer2 = (Timer) map_compare.get(position);
				if (timer != null) {
					timer.cancel();
					map_sample.remove(position);
				}
				if (timer2 != null) {
					timer2.cancel();
					map_compare.remove(position);
				}
				viewHolder.tv_Time.setText("0s");
			} else {
				viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
			}

			// 选择样品监听事件
			viewHolder.btn_SelectName.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), SampleSearchActivity.class);
					startActivityForResult(intent, position);
				}
			});

			// 对照按钮监听事件
			viewHolder.btn_Compare.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (viewHolder.btn_Compare.isSelected()) {
						viewHolder.btn_Compare.setSelected(false);
						list.get(position).put("CompareButton", false);
						list.get(position).put("SampleButton", false);
						list.get(position).put("CountDown", DetectionTime);
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.tv_InhibitionRate.setText("");
						viewHolder.tv_Result.setText("");
						// 关闭之前先获取是要关闭哪一个
						Timer timer = (Timer) map_compare.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// 停止远程控制
						flag = false;
						byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
						mBluetoothLeService.write(stopControlByte);
					} else {
						// 一点击测试按钮就开始进行远程控制(先判断是否已经在心跳请求了)
							String controlData = "7e30000000aa";
							byte[] controlbyte = DecodeUtils.HexString2Bytes(controlData);
							mBluetoothLeService.write(controlbyte);
						// ifcompare = true;
						// 对照样品前需要先将检测的计时器关掉
						Timer timer = (Timer) map_sample.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// 修改list里面的数据
						list.get(position).put("SampleButton", false);
						list.get(position).put("CompareButton", true);
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("result", "");
						list.get(position).put("ifcompare", true);
						// 重置定时器秒数
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.tv_InhibitionRate.setText("");
						viewHolder.tv_Result.setText("");
						// 将对照按钮设置为高亮
						viewHolder.btn_Compare.setSelected(true);
						// 如果有选中检测按钮，需要先关闭并保存状态
						viewHolder.btn_Sample.setSelected(false);
						// 开始倒计时操作
						TimerTask task = new TimerTask() {
							public void run() {
								int countdown = Integer.parseInt(list.get(position).get("CountDown").toString()) - 1;
								Message message = new Message();
								message.what = position;
								message.obj = countdown;
								mHandler.sendMessage(message);
							}
						};
						timer_compare = new Timer(true);
						timer_compare.schedule(task, 1000, 1000);
						// 将timer储存起来
						map_compare.put(position, timer_compare);
					}
				}
			});

			// 样品按钮监听事件
			viewHolder.btn_Sample.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (viewHolder.btn_Sample.isSelected()) {
						viewHolder.btn_Sample.setSelected(false);
						// 修改list里面的数据
						list.get(position).put("SampleButton", false);
						list.get(position).put("CompareButton", false);
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("result", "");
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.tv_InhibitionRate.setText("");
						viewHolder.tv_Result.setText("");
						// 关闭之前先获取是要关闭哪一个
						Timer timer = (Timer) map_sample.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// 停止远程控制
						flag = false;
						byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
						mBluetoothLeService.write(stopControlByte);
					} else {
						// 一点击测试按钮就开始进行远程控制(先判断是否已经在心跳请求了)
							String controlData = "7e30000000aa";
							byte[] controlbyte = DecodeUtils.HexString2Bytes(controlData);
							mBluetoothLeService.write(controlbyte);
						// ifcompare = false;
						// 检测样品前需要先将对照的计时器关掉
						Timer timer = (Timer) map_compare.get(position);
						if (timer != null) {
							timer.cancel();
							list.get(position).put("CountDown", DetectionTime);
						}
						// 修改list里面的数据
						list.get(position).put("SampleButton", true);
						list.get(position).put("CompareButton", false);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("result", "");
						list.get(position).put("ifcompare", false);
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.btn_Sample.setSelected(true);
						// 如果有选中对照按钮，需要先关闭并保存状态
						viewHolder.btn_Compare.setSelected(false);
						// 开始倒计时操作
						TimerTask task = new TimerTask() {
							public void run() {
								int countdown = Integer.parseInt(list.get(position).get("CountDown").toString()) - 1;
								Message message = new Message();
								message.what = position;
								message.obj = countdown;
								mHandler2.sendMessage(message);
							}
						};
						timer_sample = new Timer(true);
						timer_sample.schedule(task, 1000, 1000);
						// 将timer储存起来
						map_sample.put(position, timer_sample);
					}
				}
			});

			return convertView;
		}

		public class ViewHolder {
			TextView tv_AccessWay;
			TextView tv_Number;
			Button btn_SelectName;
			TextView tv_Time;
			TextView tv_InhibitionRate;
			TextView tv_Result;
			Button btn_Compare;
			Button btn_Sample;
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == getActivity().RESULT_OK) {

			if (data.getStringExtra("AccessWaySelect").toString().equals("true")) {
				for (int i = 0; i < 10; i++) {
					list.get(i).put("SampleName", data.getStringExtra("SampleName").toString());
				}

			} else {
				list.get(requestCode).put("SampleName", data.getStringExtra("SampleName").toString());

			}
			mMyBaseAdapter.notifyDataSetChanged();

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.rl_back:
			getActivity().finish();
			break;
		case R.id.btn_clearAll:
			// 恢复默认数据并且将所有定时器关掉
			for (int i = 0; i < 10; i++) {
				Map<String, Object> map = new HashMap<String, Object>();
				list.get(i).put("SampleName", "选择样品");
				list.get(i).put("CountDown", DetectionTime);
				list.get(i).put("CompareButton", false);
				list.get(i).put("SampleButton", false);
				list.get(i).put("fYiZhiLvVal", "");
				list.get(i).put("result", "");
				Timer timer_compare = (Timer) map_compare.get(i);
				if (timer_compare != null) {
					timer_compare.cancel();
				}
				Timer timer_sample = (Timer) map_sample.get(i);
				if (timer_sample != null) {
					timer_sample.cancel();
				}
			}
			mMyBaseAdapter.notifyDataSetChanged();
			break;
		}

	}

	// 建立一个新线程，用于每3s发送一个心跳包
	class MyThread extends Thread {
		public void run() {
			while (flag) {
				try {
					byte[] heartByte = new byte[] { 0x7e, 0x32, 0x00, 0x00, 0x00, (byte) 0xAA };
					Log.e("AAAAAAAAAA", "发送心跳包");
					mBluetoothLeService.write(heartByte);
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
