package com.vituniversity.hostelautomation;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by SHAAN on 05-03-17.
 */
public class BluetoothConnectionService {
    private static final String TAG = "BTConnectionService";

    private static final String appName = "MyBluetoothApp";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
                             //00001101-0000-1000-8000-00805F9B34FB
    private BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        this.mContext = context;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new Listening server socket
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                try {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName,
                            MY_UUID_INSECURE);
                    Log.i(TAG, "AcceptThread: Setting up the Server using: " + MY_UUID_INSECURE);
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: ERROR in Setting up the Server -> " + e.getMessage());
                }
            }
            mServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "RUN: AcceptThread Running...");

            BluetoothSocket socket = null;
            try {
                Log.d(TAG, "RUN: RFCOM Server Socket start...");
                socket = mServerSocket.accept();
                Log.d(TAG, "RUN: RFCOM Server Socket accepted Connection...");
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: ERROR in Setting up the Server -> " + e.getMessage());
            }

            if (socket != null) {
                connected(socket,mDevice);
            }

            Log.d(TAG, "END: mAcceptThread...");
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close() of AcceptThread ServerSocket failed" + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: STARTED");
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "RUN: mConnectThread");

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " +
                        MY_UUID_INSECURE);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Couldn't create InsecureRfcommSocket using UUID: " +
                        MY_UUID_INSECURE);
            }
            mSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
                Log.d(TAG, "ConnectThread: CONNECTED");
            } catch (IOException e) {
                try {
                    mSocket.close();
                    Log.e(TAG, "ConnectThread: Socket Closed..." + e.getMessage());
                } catch (IOException e1) {
                    Log.e(TAG, "ConnectThread: Unable to Close connection in socket..."+ e1
                            .getMessage());
                }
                Log.e(TAG, "ConnectThread: Couldn't connect to UUID" + MY_UUID_INSECURE);
            }
            connected(mSocket,mDevice);
        }

        public void cancel() {
            try {
                Log.e(TAG, "cancel: Closing the Client Socket.");
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Couldn't close the Client Socket.");
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public  ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: STARTING");
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                mProgressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;

            while(true) {
                try {
                    bytes = mInStream.read(buffer);
                    String incomingMsg = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMsg);

                    Intent incomingMsgIntent = new Intent("incomingMsg");
                    incomingMsgIntent.putExtra("theMessage", incomingMsg);

                } catch (IOException e) {
                    Log.e(TAG, "Error reading from the InputStream");
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                String text = new String(bytes, Charset.defaultCharset());
                Log.d(TAG, "OutputStream: Writing ..." + text);
                try {
                    mOutStream.write(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "OutputStream: ERROR while Writing ..." + text);
                }
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mDevice) {
        Log.d(TAG, "connected(): STARTING ...");

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        Log.d(TAG, "write(byte[]): STARTING ...");
        mConnectedThread.write(out);
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        if(mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: STARTED");
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...",
                true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();

    }
}
