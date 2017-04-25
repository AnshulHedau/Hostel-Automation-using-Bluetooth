/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vituniversity.hostelautomation.bluetoothchat;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vituniversity.hostelautomation.R;
import com.vituniversity.hostelautomation.common.activities.SampleActivityBase;
import com.vituniversity.hostelautomation.common.logger.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.vituniversity.hostelautomation.R.drawable.btn_off;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private String IOT_URL;
    Context context;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button bR1;
    private Button bR2;
    private Button bCR;
    private Button bBR;
    private Button bAll;

    //button status
    private boolean isR1ON;
    private boolean isR2ON;
    private boolean isCRON;
    private boolean isBRON;
    private boolean isAllON;

    private TextView tvConnectionStatus;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            SampleActivityBase act = (SampleActivityBase) getActivity();
            act.initializeLogging();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            //mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getActivity().getBaseContext();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        bR1 = (Button) view.findViewById(R.id.bR1);
        bR2 = (Button) view.findViewById(R.id.bR2);
        bCR = (Button) view.findViewById(R.id.bCR);
        bBR = (Button) view.findViewById(R.id.bBR);
        bAll = (Button) view.findViewById(R.id.bAll);

        bR1.setText("Room 1 ON");
        bR2.setText("Room 2 ON");
        bCR.setText("Corridor ON");
        bBR.setText("Bathroom ON");
        bAll.setText("All ON");

        isR1ON = false;
        isR2ON = false;
        isCRON = false;
        isBRON = false;
        isAllON = false;

        //tvConnectionStatus = (TextView) view.findViewById(R.id.tvConnectionStatus);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        bR1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                View view = getView();
                if (null != view) {
                    if(isR1ON) {

                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field1=1");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);

                        //when R1 is On
                        message = "9";
                        sendMessage(message);
                        bR1.setText("Room 1 ON");
                        bR1.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        isR1ON = false;
                    } else {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field1=0");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when R1 is OFF
                        message = "1";
                        sendMessage(message);
                        bR1.setText("Room 1 OFF");
                        bR1.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        isR1ON = true;
                    }
                }
            }
        });

        bR2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                View view = getView();
                if (null != view) {
                    if(isR2ON) {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field2=1");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when R2 is On
                        message = "8";
                        sendMessage(message);
                        bR2.setText("Room 2 ON");
                        bR2.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        isR2ON = false;
                    } else {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field2=0");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when R2 is OFF
                        message = "2";
                        sendMessage(message);
                        bR2.setText("Room 2 OFF");
                        bR2.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        isR2ON = true;
                    }
                }
            }
        });

        bCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                View view = getView();
                if (null != view) {
                    if(isCRON) {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field3=1");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when Corridor is On
                        message = "6";
                        sendMessage(message);
                        bCR.setText("Corridor ON");
                        bCR.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        isCRON = false;
                    } else {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field3=0");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when Corridor is OFF
                        message = "4";
                        sendMessage(message);
                        bCR.setText("Corridor OFF");
                        bCR.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        isCRON = true;
                    }
                }
            }
        });

        bBR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                View view = getView();
                if (null != view) {
                    if(isBRON) {
                        IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field4=1");

                        StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                        }

                                        catch (Exception e){
                                            Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                        //Add the server request to the queue
                        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                        requestQueue.add(stringRequest);
                        //when Bathroom is On
                        message = "7";
                        sendMessage(message);
                        bBR.setText("Bathroom ON");
                        bBR.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        isBRON = false;
                    } else {
                            IOT_URL = ("https://api.thingspeak.com/update?api_key=ER9TMXHACM6GFUKI&field4=0");

                            StringRequest stringRequest = new StringRequest(Request.Method.GET, IOT_URL,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            try {
                                                Toast.makeText(context, "Data send successfully!", Toast.LENGTH_LONG).show();
                                            }

                                            catch (Exception e){
                                                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                                        }
                                    });

                            //Add the server request to the queue
                            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                            requestQueue.add(stringRequest);
                        //when Bathroom is OFF
                        message = "3";
                        sendMessage(message);
                        bBR.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        bBR.setText("Bathroom OFF");
                        isBRON = true;
                    }
                }
            }
        });

        bAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                View view = getView();
                if (null != view) {
                    if(isAllON) {
                        //when All is On
                        message = "Z";
                        sendMessage(message);
                        bAll.setText("All ON");
                        bAll.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        isAllON = false;

                        bR1.setText("Room 2 ON");
                        isR1ON = false;
                        bR1.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        //message = "8";
                        //sendMessage(message);
                        bR2.setText("Room 2 ON");
                        isR2ON = false;
                        bR2.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        //message = "6";
                        //sendMessage(message);
                        bCR.setText("Corridor ON");
                        isCRON = false;
                        bCR.setBackground(getResources().getDrawable(R.drawable.btn_on));
                        //message = "7";
                        //sendMessage(message);
                        bBR.setText("Bathroom ON");
                        isBRON = false;
                        bBR.setBackground(getResources().getDrawable(R.drawable.btn_on));
                    } else {
                        //when All is OFF
                        message = "A";
                        sendMessage(message);
                        bAll.setText("All OFF");
                        isAllON = true;
                        bAll.setBackground(getResources().getDrawable(R.drawable.btn_off));


                        bR1.setText("Room 2 OFF");
                        isR1ON = true;
                        bR1.setBackground(getResources().getDrawable(R.drawable.btn_off));

                        //message = "2";
                        //sendMessage(message);
                        bR2.setText("Room 2 OFF");
                        isR2ON = true;
                        bR2.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        //message = "4";
                        //sendMessage(message);
                        bCR.setText("Corridor OFF");
                        isCRON = true;
                        bCR.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        //message = "3";
                        //sendMessage(message);
                        bBR.setText("Bathroom OFF");
                        bBR.setBackground(getResources().getDrawable(R.drawable.btn_off));
                        isBRON = true;
                    }
                }
            }
        });


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (true) {//message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
        tvConnectionStatus.setText(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}