package com.example.smartbulb;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final String TAG = "smartBulb";

	private Timer timer = new Timer();
	private TimerTask timerTask1;
	private TimerTask timerTask2;

	private Handler timerHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				int a = new Random().nextInt() % 256;
				int r = new Random().nextInt() % 256;
				int g = new Random().nextInt() % 256;
				int b = new Random().nextInt() % 256;
				Log.i(TAG, " " + a + " " + r + " " + g + " " + b);
				main_screen.setBackgroundColor(Color.argb(a, r, g, b));

				tv_color.setText("Random");
				tv_mode.setText("partyMode");
			}
			if (msg.what == 2) {
				tv_color.setText("Random");
				tv_mode.setText("off");
				main_screen.setBackgroundColor(Color.argb(255, 0, 0, 0));
			}
		}
	};

	// Message types sent from Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Receive Message Type,send from WriteMessage
	public static final String Intensity_READMESSAGE = "Intensity";
	public static final String Color_READMESSAGE = "Color";
	public static final String Mode_READMESSAGE = "Mode";

	// Key name -bundle from Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes onActivityforResult
	private static final int REQUEST_ENABLE_BT = 1;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services

	private BlueToothThread mBluetoothThread = null;

	// Name of the connected device
	private String mConnectedDeviceName = null;

	private TextView tv_status;
	private TextView tv_intensity;
	private TextView tv_color;
	private TextView tv_mode;

	private LinearLayout main_screen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_status = (TextView) findViewById(R.id.tv_status);
		tv_intensity = (TextView) findViewById(R.id.tv_intensity);
		tv_color = (TextView) findViewById(R.id.tv_color);
		tv_mode = (TextView) findViewById(R.id.tv_mode);

		main_screen = (LinearLayout) findViewById(R.id.main_screen);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mBluetoothThread == null) {
				mBluetoothThread = new BlueToothThread(this, mHandler);
			}
		}

		// 停止自动调节屏幕亮度
		if (BrightnessTools.isAutoBrightness(this) == 1) {
			BrightnessTools.stopAutoBrightness(this);
		}
		
		if (mBluetoothThread != null) {
			mBluetoothThread.start();
			Toast.makeText(this, "Start listening", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

	}

	/*@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		if (mBluetoothThread == null) {
			mBluetoothThread = new BlueToothThread(this, mHandler);
		}
	}*/

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mBluetoothThread != null) {
			mBluetoothThread.stop();
		}
		timer.cancel();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				mBluetoothThread = new BlueToothThread(this, mHandler);
			} else {
				Toast.makeText(this, "BT not enabled", Toast.LENGTH_LONG)
						.show();
				finish();
			}

		default:
			break;
		}
	}

	private final void setStatus(String subTitle) {
		tv_status.setText(subTitle);
	}

	// The Handler that gets information from BluetoothThread
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BlueToothThread.STATE_CONNECTED:
					setStatus("Connected to " + mConnectedDeviceName);
					break;
				case BlueToothThread.STATE_CONNECTING:
					setStatus("Connecting .....");
					break;
				case BlueToothThread.STATE_LISTEN:
					setStatus("Listening .....");
					break;
				case BlueToothThread.STATE_NONE:
					setStatus("Not Available");
					break;
				default:
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = new String(writeBuf);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				String[] Message = readMessage.split("/");
				String Type = Message[0].trim();
				String Value = Message[1];
				if (Type.equals(Intensity_READMESSAGE)) {
					try {
						tv_intensity.setText(Value);
						int Brightness = Integer.parseInt(Value.trim());
						BrightnessTools.setBrightness(MainActivity.this,
								Brightness);
						tv_mode.setText(R.string.light_mode_value);
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), "Unknow error",
								Toast.LENGTH_SHORT).show();
					}
					Log.i(TAG, Value);
				} else if (Type.equals(Color_READMESSAGE)) {
					try {
						tv_color.setText(Value);
						main_screen.setBackgroundColor(Integer.parseInt(Value
								.trim()));
						tv_mode.setText(R.string.light_mode_value);
					} catch (Exception e) {
						Toast.makeText(getApplicationContext(), "Unknow error",
								Toast.LENGTH_SHORT).show();
					}
					Log.i(TAG, Value);
				} else if (Type.equals(Mode_READMESSAGE)) {
					// set intensity
					String mode = Value.trim();
					if (mode.equals("dailyMode")) {
						setLightMode(60, "#ffffff", 0xffffffff, "dailyMode");
					} else if (mode.equals("sleepMode")) {
						setLightMode(35, "#f5fca4", 0xfff5fca4, "sleepMode");
					} else if (mode.equals("workMode")) {
						setLightMode(75, "#cffffb", 0xffcffffb, "workMode");
					} else if (mode.equals("restMode")) {
						setLightMode(50, "#ffcd76", 0xffffcd76, "restMode");
					} else if (mode.equals("loveMode")) {
						setLightMode(40, "#ffcccc", 0xffffcccc, "loveMode");
					} else if (mode.equals("musicMode")) {
						setLightMode(60, "#8cff5f", 0xff8cff5f, "musicMode");
					} else if (mode.equals("partyMode")) {
						timerTask1 = new TimerTask() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								Message msg = new Message();
								msg.what = 1;
								timerHandler.sendMessage(msg);

							}
						};

						timer.schedule(timerTask1, 1000, 1000);

						/*
						 * new Thread() { public void run() { for (int i = 0; i
						 * < 400; i++) { try { sleep(100); } catch
						 * (InterruptedException e) { e.printStackTrace(); } if
						 * (i % 20 == 0) { int a = new Random().nextInt(256);
						 * int r = new Random().nextInt(256); int g = new
						 * Random().nextInt(256); int b = new
						 * Random().nextInt(256);
						 * main_screen.setBackgroundColor(Color .argb(a, r, g,
						 * b)); } } } }.start();
						 */
					}
					/*
					 * tv_intensity.setText("60");
					 * BrightnessTools.setBrightness(MainActivity.this, 60); //
					 * set color tv_color.setText("#ffffff");
					 * main_screen.setBackgroundColor(0xffffff); // set mode
					 * tv_mode.setText("dailyMode");
					 */

				} else if (Type.equals("setTime")) {
					if (timerTask2 != null) {
						timerTask2.cancel();
						timerTask2 = null;
						timer.purge();
					}

					timerTask2 = new TimerTask() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							Message msg = new Message();
							msg.what = 2;
							timerHandler.sendMessage(msg);
						}

					};

					int time = Integer.parseInt(Value.trim());
					timer.schedule(timerTask2, time);
				}

				break;
			case MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"connected to :" + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
			default:
				break;
			}
		}

	};

	protected void setLightMode(int intensity, String textColor, int color,
			String Mode) {
		// TODO Auto-generated method stu

		if (timerTask1 != null) {
			timerTask1.cancel();
			timerTask1 = null;
			timer.purge();
			Log.i(TAG, "cancel this task");
		}

		try {
			tv_intensity.setText(String.valueOf(intensity));
			BrightnessTools.setBrightness(MainActivity.this, intensity);
			// set color
			tv_color.setText(textColor);
			main_screen.setBackgroundColor(color);
			// set mode
			tv_mode.setText(Mode);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), e.toString(),
					Toast.LENGTH_SHORT).show();
		}
	}
}
