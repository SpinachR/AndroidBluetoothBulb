package com.example.smartbulb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BlueToothThread {
	// for debug
	private static final String TAG = "BlueToothServer";

	// // Unique UUID
	// private static final UUID uuid = UUID
	// .fromString("00001101-0000-1000-8000-00805F9B34FA");

	// bluetoothAdapter
	private final BluetoothAdapter mBlueToothAdapter;

	// Handler for transfer message to UI Activity
	private final Handler mHandler;

	// Three Thread
	private AcceptThread mAcceptThread;// for serverSocket
	private ConnectThread mConnectThread;// for ClientSocket(BluetoothDevice)
	private ConnectedThread mConnectedThread;// for transfer message
	private int mState;

	// connection state
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;// accept thread
	public static final int STATE_CONNECTING = 2;// connect thread
	public static final int STATE_CONNECTED = 3;// connected thread

	/**
	 * 
	 * @param context
	 *            The UI Context
	 * @param handler
	 */

	public BlueToothThread(Context context, Handler handler) {
		mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		mState = STATE_NONE;
	}

	public synchronized void setState(int state) {
		Log.d(TAG, "setState()" + mState + "->" + state);
		mState = state;
		// send new state to UI
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the Accept Thread to begin a Listening mode. Called by UI
	 * onResume()
	 */
	public synchronized void start() {
		Log.d(TAG, "start");
		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.Cancel();
			mConnectedThread = null;
		}

		

		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread(); // listen an incoming connection
			mAcceptThread.start();
			setState(STATE_LISTEN);
		}

	}

	/**
	 * Start the ConnectThread(Client RemoteDevice)
	 */
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.Cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);

	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 */

	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		Log.d(TAG, "connected");
		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.Cancel();
			mConnectedThread = null;
		}

		// Cancel accept thread because we only want to connect to one device
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);

		mHandler.sendMessage(msg);
		setState(STATE_CONNECTED);
	}

	public synchronized void stop() {
		Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.Cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 */
	public void write(byte[] out) {
		ConnectedThread cedT;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			cedT = mConnectedThread;
		}
		cedT.write(out);

	}

	private void connectionFailed() {

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_NONE);

		// Start the service over to restart listening mode
		// BlueToothThread.this.start();

	}

	private void connectionLost() {
		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		setState(STATE_NONE);

		// Start the service over to restart listening mode
		// BlueToothThread.this.start();
	}

	/**
	 * Get BluetoothSocket from BluetoothServerSocker
	 * 
	 * @author 5560 As a server, Listening a incoming connection request
	 */
	private class AcceptThread extends Thread {

		private final BluetoothServerSocket mBluetoothServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			String MacAddress = mBlueToothAdapter.getAddress();
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-"
					+ MacAddress.replace(":", ""));
			try {
				tmp = mBlueToothAdapter.listenUsingRfcommWithServiceRecord(
						"bluetoothServer", uuid);
			} catch (IOException e) {
				Log.e(TAG, "listen() AcceptThread failed", e);
			}
			mBluetoothServerSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "Begin AcceptThread" + this);

			BluetoothSocket socket = null;
			while (true) {
				try {
					// Blocking call,will return on a connection or a exception
					socket = mBluetoothServerSocket.accept();
				} catch (IOException e) {
					setState(STATE_NONE);
					Log.e(TAG, "accept() failed", e);
					break;
				}
				// if a connection was accepted,start the connectedThread
				if (socket != null) {
					synchronized (BlueToothThread.this) {
						connected(socket, socket.getRemoteDevice());
					}

					try {
						mBluetoothServerSocket.close();
					} catch (IOException e) {
						Log.e(TAG, "could not close Server socket");
					}

					break;
				}
			}
			Log.i(TAG, "End AcceptThread");
		}

		private void cancel() {
			try {
				mBluetoothServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close Server failed");
			}
		}
	}

	// __________________________End AcceptThread___________

	/**
	 * Use BluetoothDevice get BluetoothSocket Initiate a conection as a client,
	 * attempt to connect
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mSocket;
		private final BluetoothDevice mDevice;

		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			mDevice = device;
			String MacAddress = device.getAddress();
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-"
					+ MacAddress.replace(":", ""));
			try {
				tmp = device.createRfcommSocketToServiceRecord(uuid);
				// get socket
			} catch (IOException e) {
				Log.e(TAG, "create ConnectThread failed", e);
			}
			mSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "Begin ConnectThread");
			mBlueToothAdapter.cancelDiscovery();

			try {
				// Blocking call, will only return on a successful connection or
				// an exception
				mSocket.connect();
			} catch (IOException e) {
				try {
					mSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"Client can't close socket during connectThread",
							e2);
				}
				connectionFailed();
				return;
			}
			// Reset the ConnectThread
			synchronized (BlueToothThread.this) {
				mConnectThread = null;
			}

			// if success,start the connected thread
			connected(mSocket, mDevice);
		}

		public void cancel() {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close socket failed in ConectThread");
			}
		}
	}

	// _____________________End ConnectThread_____________________
	/**
	 * Managing a connection, This thread runs during a connection with a remote
	 * device It handles all incoming and outgoing transmissions
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mSocket;
		private final InputStream mInStream;
		private final OutputStream mOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Stream Create failed", e);
			}
			mInStream = tmpIn;
			mOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "Begin ConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;
			// keep listen to the InputStream
			while (true) {
				try {

					bytes = mInStream.read(buffer);
					// send the buffer to UI
					mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes,
							-1, buffer).sendToTarget();

				} catch (IOException e) {
					Log.e(TAG, "disconnected" + mInStream, e);
					connectionLost();
					break;
				}
			}
		}

		public void write(byte[] bytes) {
			try {
				mOutStream.write(bytes);
				// Share the sent message back to the UI Activity
				mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
						bytes).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void Cancel() {
			try {
				mSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connected socket failed", e);
			}
		}
	}

}
