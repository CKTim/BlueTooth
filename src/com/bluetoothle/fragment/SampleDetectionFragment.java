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
	private double nYingPinVal;// ��Ʒ����ֵ
	private double nCompareVal;// ����ֵ
	private double fYiZhiLvVal;// ������
	private int year;
	private String month;
	private String data;
	private String hour;
	private String min;
	private String second;
	private String result;// ���
	private Timer timer_compare;
	private Timer timer_sample;
	private Map<Integer, Object> map_compare = new HashMap<Integer, Object>();
	private Map<Integer, Object> map_sample = new HashMap<Integer, Object>();
	private SharedPreferences sp;
	private String DetectionTime;
	private MyThread myThread = new MyThread();
	private boolean flag = true;
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
				// ���ռ���ȡֵ����
				if (byteData[0] == 0x7e && byteData[1] == 0x37) {
					// ��ʮ�����Ƶ���Ʒ���ֵ�ͼ��ͨ����������
					int accessway = Integer.parseInt(DecodeUtils.bytesToHexString(new byte[] { byteData[4] }));
					boolean ifcompare = (boolean) list.get(accessway - 1).get("ifcompare");
					if (ifcompare) {
						try {
							Log.e("����ֵ����", DecodeUtils.byte2HexStr(byteData) + "");
							// ������ֵ�������
							Editor editor = sp.edit();
							String CompareVal = DecodeUtils.bytesToAllHex(new byte[] { byteData[5] }, 10);
							editor.putFloat("CompareValue", Float.parseFloat(CompareVal));
							editor.commit();
							// ��������������
							if (map_sample.size() == 0 && map_compare.size() == 0) {
								flag = false;
								// ֹͣԶ�̿���
								byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
								mBluetoothLeService.write(stopControlByte);
								Log.e("AAAAAAA", "ֹͣԶ�̿���");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// ��Ʒ����ȡֵ����
						if (byteData[0] == 0x7e && byteData[1] == 0x37) {
							try {
								Log.e("���ֵ����", DecodeUtils.byte2HexStr(byteData) + "");
								String YingPinVal = DecodeUtils.bytesToAllHex(new byte[] { byteData[5] }, 10);
								Log.e("���ֵΪ:", YingPinVal);
								Log.e("���ض���ֵΪ", sp.getFloat("CompareValue",0)+"");
								nYingPinVal = Double.valueOf(YingPinVal);
								// ��ʼ���������ʣ��Ȼ�ȡ���浽���صĶ���ֵ
								nCompareVal = (double) sp.getFloat("CompareValue", 0);
								Log.e("AAAAAA", "���صĶ���ֵΪ" + nCompareVal);
								fYiZhiLvVal = CalInhiRateUtils.CalInhiRate(nCompareVal, nYingPinVal);
								Log.e("������Ϊ", fYiZhiLvVal + "%");
								// װ�ص�list��
								if (fYiZhiLvVal > 50) {
									list.get(accessway - 1).put("result", "����");
									result = "����";
								} else {
									list.get(accessway - 1).put("result", "����");
									result = "����";
								}
								list.get(accessway - 1).put("fYiZhiLvVal", String.format("%.2f", fYiZhiLvVal) + "%");
								mMyBaseAdapter.notifyDataSetChanged();
								// ��������������
								Log.e("map_sample.size()", map_sample.size() + "");
								if (map_sample.size() == 0 && map_compare.size() == 0) {
									flag = false;
									// ֹͣԶ�̿���
									byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
									mBluetoothLeService.write(stopControlByte);
									Log.e("AAAAAAA", "ֹͣԶ�̿���");
								}
								// ���������һ���㲥�ؼ���¼fragment��������
								// �Ȼ�ȡ��ǰ������
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
								// ʱ��(byte����)
								byte[] DataByte = DecodeUtils.HexString2Bytes(FinalData);
								// �߲˵�ASII��(byte����)
								String vegetableName=list.get(accessway - 1).get("SampleName").toString();
								if(vegetableName.equals("ѡ����Ʒ")){
									vegetableName="��";
								}
								ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
								outputStream.write(DecodeUtils.gbkToString(vegetableName));
								for(int i=vegetableName.length()*2;i<6;i++){
									outputStream.write(0x20);
								}
								byte[] SampleByte = outputStream.toByteArray();
								// ������(byte����)
								int FYiZhiLvVal = (int) (fYiZhiLvVal * 100);
								String FYiZhiLvVal1 = DecodeUtils.AddZeroToFour(Integer.toHexString(FYiZhiLvVal));
								String FYiZhiLvVal1Head = (String) FYiZhiLvVal1.substring(0, 2);
								String FYiZhiLvVal1Tail = (String) FYiZhiLvVal1.substring(2);
								String FinalFYiZhiLvVal = FYiZhiLvVal1Tail + FYiZhiLvVal1Head;
								byte[] FYiZhiLvValByte = DecodeUtils.HexString2Bytes(FinalFYiZhiLvVal);
								// ƴ������byte
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
								if (list.get(accessway - 1).get("SampleName").toString().equals("ѡ����Ʒ")) {
									list.get(accessway - 1).put("SampleName", "��");
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
								Log.e("Aaaaa", "������Ϣ��¼�Ĺ㲥");
							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					}

				}
				// ��ʼԶ�̿�����Ӧ
				if (byteData[0] == 0x7e && byteData[1] == 0x31) {
					if (!myThread.isAlive()) {
						flag = true;
						myThread = new MyThread();
						myThread.start();
						Log.e("AAAAAA", "�������߳̿�ʼ");
					}
				}
			}
		}
	};
	// ���ռ��Handler
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int sec = (int) msg.obj;
			list.get(msg.what).put("CountDown", sec);
			mMyBaseAdapter.notifyDataSetChanged();
			// ����0sʱ���Ͷ�ȡ���ֵָ��
			if (sec == 0) {
				if (mBluetoothLeService == null) {
					Log.e("SampleDetectionFragment", "mBluetoothLeServiceΪ�գ����ܻ�û�н��а�");
				} else {
					// �Ȼ�ȡ������һ��ͨ�����Ķ��ռ��
					String AccessWay = DecodeUtils.AddZeroToTwo(list.get(msg.what).get("AccessWayNum").toString());
					// װ������
					String sendData = "7e360100" + AccessWay + "00aa";
					byte[] sendbyte = DecodeUtils.HexString2Bytes(sendData);
					mBluetoothLeService.write(sendbyte);
				}
			}

		};
	};
	// ��Ʒ���Handler
	private Handler mHandler2 = new Handler() {
		public void handleMessage(Message msg) {
			int sec = (int) msg.obj;
			list.get(msg.what).put("CountDown", sec);
			mMyBaseAdapter.notifyDataSetChanged();
			// ����0sʱ���Ͷ�ȡ���ֵָ��
			if (sec == 0) {
				if (mBluetoothLeService == null) {
					Log.e("SampleDetectionFragment", "mBluetoothLeServiceΪ�գ����ܻ�û�н��а�");
				} else {
					// �Ȼ�ȡ������һ��ͨ�����Ķ��ռ��
					String AccessWay = DecodeUtils.AddZeroToTwo(list.get(msg.what).get("AccessWayNum").toString());
					// װ�������
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
		// �󶨷���
		Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
		getActivity().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
		// ���Ĭ�����ݵ�list
		for (int i = 0; i < 10; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("AccessWayNum", i + 1);
			map.put("SampleName", "ѡ����Ʒ");
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
		//��ñ����Ĭ�ϼ��ʱ��
		sp = getActivity().getSharedPreferences("LZ-4000(T)", getActivity().MODE_PRIVATE);
		DetectionTime=sp.getString("DetectionTime","480");
	}

	@Override
	public void onResume() {
		super.onResume();
		// ע��㲥
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
	// Log.e("AAAAA", "SDFragment�����أ���ע��㲥");
	// // ��ע��㲥
	// getActivity().unregisterReceiver(mGattUpdateReceiver);
	// } else {
	// getActivity().registerReceiver(mGattUpdateReceiver,
	// makeGattUpdateIntentFilter());
	// Log.e("AAAAA", "SDFragment����ʾ��ע��㲥");
	// }
	// }

	// �Զ���һ��������
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
			// ���õ���ʱ��ʱ��
			if (Integer.parseInt((list.get(position).get("CountDown").toString())) <= 0) {
				// �ر�֮ǰ�Ȼ�ȡ��Ҫ�ر���һ��
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

			// ѡ����Ʒ�����¼�
			viewHolder.btn_SelectName.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), SampleSearchActivity.class);
					startActivityForResult(intent, position);
				}
			});

			// ���հ�ť�����¼�
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
						// �ر�֮ǰ�Ȼ�ȡ��Ҫ�ر���һ��
						Timer timer = (Timer) map_compare.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// ֹͣԶ�̿���
						flag = false;
						byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
						mBluetoothLeService.write(stopControlByte);
					} else {
						// һ������԰�ť�Ϳ�ʼ����Զ�̿���(���ж��Ƿ��Ѿ�������������)
							String controlData = "7e30000000aa";
							byte[] controlbyte = DecodeUtils.HexString2Bytes(controlData);
							mBluetoothLeService.write(controlbyte);
						// ifcompare = true;
						// ������Ʒǰ��Ҫ�Ƚ����ļ�ʱ���ص�
						Timer timer = (Timer) map_sample.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// �޸�list���������
						list.get(position).put("SampleButton", false);
						list.get(position).put("CompareButton", true);
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("result", "");
						list.get(position).put("ifcompare", true);
						// ���ö�ʱ������
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.tv_InhibitionRate.setText("");
						viewHolder.tv_Result.setText("");
						// �����հ�ť����Ϊ����
						viewHolder.btn_Compare.setSelected(true);
						// �����ѡ�м�ⰴť����Ҫ�ȹرղ�����״̬
						viewHolder.btn_Sample.setSelected(false);
						// ��ʼ����ʱ����
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
						// ��timer��������
						map_compare.put(position, timer_compare);
					}
				}
			});

			// ��Ʒ��ť�����¼�
			viewHolder.btn_Sample.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (viewHolder.btn_Sample.isSelected()) {
						viewHolder.btn_Sample.setSelected(false);
						// �޸�list���������
						list.get(position).put("SampleButton", false);
						list.get(position).put("CompareButton", false);
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("result", "");
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.tv_InhibitionRate.setText("");
						viewHolder.tv_Result.setText("");
						// �ر�֮ǰ�Ȼ�ȡ��Ҫ�ر���һ��
						Timer timer = (Timer) map_sample.get(position);
						if (timer != null) {
							timer.cancel();
						}
						// ֹͣԶ�̿���
						flag = false;
						byte[] stopControlByte = new byte[] { 0x7e, 0x34, 0x00, 0x00, 0x00, (byte) 0xAA };
						mBluetoothLeService.write(stopControlByte);
					} else {
						// һ������԰�ť�Ϳ�ʼ����Զ�̿���(���ж��Ƿ��Ѿ�������������)
							String controlData = "7e30000000aa";
							byte[] controlbyte = DecodeUtils.HexString2Bytes(controlData);
							mBluetoothLeService.write(controlbyte);
						// ifcompare = false;
						// �����Ʒǰ��Ҫ�Ƚ����յļ�ʱ���ص�
						Timer timer = (Timer) map_compare.get(position);
						if (timer != null) {
							timer.cancel();
							list.get(position).put("CountDown", DetectionTime);
						}
						// �޸�list���������
						list.get(position).put("SampleButton", true);
						list.get(position).put("CompareButton", false);
						list.get(position).put("fYiZhiLvVal", "");
						list.get(position).put("CountDown", DetectionTime);
						list.get(position).put("result", "");
						list.get(position).put("ifcompare", false);
						viewHolder.tv_Time.setText(list.get(position).get("CountDown").toString() + "s");
						viewHolder.btn_Sample.setSelected(true);
						// �����ѡ�ж��հ�ť����Ҫ�ȹرղ�����״̬
						viewHolder.btn_Compare.setSelected(false);
						// ��ʼ����ʱ����
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
						// ��timer��������
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
			// �ָ�Ĭ�����ݲ��ҽ����ж�ʱ���ص�
			for (int i = 0; i < 10; i++) {
				Map<String, Object> map = new HashMap<String, Object>();
				list.get(i).put("SampleName", "ѡ����Ʒ");
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

	// ����һ�����̣߳�����ÿ3s����һ��������
	class MyThread extends Thread {
		public void run() {
			while (flag) {
				try {
					byte[] heartByte = new byte[] { 0x7e, 0x32, 0x00, 0x00, 0x00, (byte) 0xAA };
					Log.e("AAAAAAAAAA", "����������");
					mBluetoothLeService.write(heartByte);
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
