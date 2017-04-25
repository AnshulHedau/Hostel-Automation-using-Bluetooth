package com.vituniversity.hostelautomation;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothModule extends Activity implements AdapterView.OnItemClickListener{

    BluetoothAdapter mBluetoothAdapter;
    Button bOnOff, bEnableDiscoverability, bDiscover;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;

    BluetoothConnectionService mBluetoothConnection;
    Button bStartConnection, bSend;
    EditText etSend;
    TextView incomingMsgs;
    StringBuilder msgs;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    BluetoothDevice mBTDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_module);
        init();
        addOnClickListeners();
        // Broadcasts when the BOND_STATE_CHANGED (Pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {

        mBluetoothConnection.startClient(device, uuid);
    }

    private void addOnClickListeners() {
        bOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BUTTON CLICKED: ", "TURNING ON/OFF BUTTON CLICKED");
                enableDisableBT();
            }
        });
        bEnableDiscoverability.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BUTTON CLICKED: ", "MAKE DISCOVERABLE BUTTON CLICKED");
                bEnableDisable_Discoverable();
            }
        });
        bDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("BUTTON CLICKED: ", "DISCOVER BUTTON CLICKED");
                mBTDevices = new ArrayList<>();
                btnDiscover();
            }
        });
        lvNewDevices.setOnItemClickListener(this);
        bStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnection();
            }
        });
        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = new byte[0];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                }
                mBluetoothConnection.write(bytes);
                etSend.setText("");
            }
        });
    }

    private void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    private void init() {
        bOnOff = (Button) findViewById(R.id.bOnOff);
        bEnableDiscoverability = (Button) findViewById(R.id.bEnableDiscoverability);
        bDiscover = (Button) findViewById(R.id.bDiscover);
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bStartConnection = (Button) findViewById(R.id.bStartConnection);
        bSend = (Button) findViewById(R.id.bSend);
        etSend = (EditText) findViewById(R.id.etSend);
        incomingMsgs = (TextView) findViewById(R.id.tvIncomingMsgs);
        msgs = new StringBuilder();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter
                ("incomingMsg"));

    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            msgs.append(text + "\n");
            incomingMsgs.setText(msgs);
        }
    };

    private void btnDiscover() {
        Log.i("DISCOVER: ", "LOOKING FOR UNPAIRED DEVICES...");
        //mBTDevices = new ArrayList<>();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.i("DISCOVER: ", "CANCELLING DISCOVERY...");

            // Check the Bluetooth permissions
            checkBTPermissions();

            try {
                mBluetoothAdapter.startDiscovery();
                Log.i("startDiscovery()", "DISCOVERY STARTED");
            } catch (Exception e) {
                Log.i("Exception", e.toString());
            }
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, intentFilter);
        }
        if (!mBluetoothAdapter.isDiscovering()) {
            // Check the Bluetooth permissions
            checkBTPermissions();

            try {
                mBluetoothAdapter.startDiscovery();
                Log.i("startDiscovery()", "DISCOVERY STARTED");
            } catch (Exception e) {
                Log.i("Exception", e.toString());
            }
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, intentFilter);
        }

    }

    private void bEnableDisable_Discoverable() {
        Log.i("ENABLE DISCOVERABILITY:", "MAKING THE  DEVICE DISCOVERABLE FOR 300 SECONDS");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);
    }

    private void enableDisableBT() {
        // If the device doesn't have bluetooth
        if (mBluetoothAdapter == null) {
            Log.i("BLUETOOTH: ", "BLUETOOTH NOT PRESENT!!!");
            Toast.makeText(getBaseContext(), "Device doesn't have BLUETOOTH capabilities", Toast
                    .LENGTH_SHORT).show();
        }
        // If the bluetooth is disabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTintent);
            Log.i("BLUETOOTH: ", "ENABLING BLUETOOTH !!!");
            IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTintent);
        }
        // If the bluetooth is enabled
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Log.i("BLUETOOTH: ", "DISABLING BLUETOOTH !!!");
            IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTintent);
        }
    }

    // Create a BroadcastReceiver for ACTION_BOND_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.i("mBroadcastReceiver4", "BOND_BONDED");
                    mBTDevice = mDevice;
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.i("mBroadcastReceiver4", "BOND_BONDING");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.i("mBroadcastReceiver4", "BOND_NONE");
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i("mBroadcastReceiver3: ", "ACTION_FOUND");

            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.i("mBroadcastReceiver: ", device.getName() + " : " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view,
                        mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_SCAN_MODE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        mBluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.i("mBroadcastReceiver2: ", "DISCOVERABILITY ENABLED");
                        Toast.makeText(getBaseContext(), "mBroadcastReceiver2: DISCOVERABILITY " +
                                "ENABLED", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.i("mBroadcastReceiver2: ", "DISCOVERABILITY DISABLED. ABLE TO RECEIVE" +
                                " CONNECTION");
                        Toast.makeText(getBaseContext(), "mBroadcastReceiver2: DISCOVERABILITY " +
                                "ENABLED. ABLE TO RECEIVE CONNECTION", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.i("mBroadcastReceiver2: ", "DISCOVERABILITY DISABLED. NOT ABLE TO " +
                                "RECEIVE CONNECTION.");
                        Toast.makeText(getBaseContext(), "mBroadcastReceiver2: DISCOVERABILITY " +
                                "ENABLED. NOT ABLE TO RECEIVE CONNECTION.", Toast.LENGTH_LONG)
                                .show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.i("mBroadcastReceiver2: ", "CONNECTING");
                        Toast.makeText(getBaseContext(), "mBroadcastReceiver2: CONNECTING",
                                Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.i("mBroadcastReceiver2: ", "CONNECTED");
                        Toast.makeText(getBaseContext(), "mBroadcastReceiver2: CONNECTED",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_STATE_CHANGED.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        mBluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("mBroadcastReceiver1: ", "BLUETOOTH: STATE OFF");
                        Toast.makeText(getBaseContext(), "BLUETOOTH: STATE OFF", Toast
                                .LENGTH_LONG).show();
                        bOnOff.setBackgroundColor(Color.RED);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i("mBroadcastReceiver1: ", "BLUETOOTH: STATE TURNING OFF");
                        Toast.makeText(getBaseContext(), "BLUETOOTH: STATE TURNING OFF", Toast
                                .LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i("mBroadcastReceiver1: ", "BLUETOOTH: STATE ON");
                        Toast.makeText(getBaseContext(), "BLUETOOTH: STATE ON", Toast.LENGTH_LONG).show();
                        bOnOff.setBackgroundColor(Color.GREEN);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i("mBroadcastReceiver1: ", "BLUETOOTH: STATE TURNING ON");
                        Toast.makeText(getBaseContext(), "BLUETOOTH: STATE TURNING ON", Toast
                                .LENGTH_LONG).show();
                        break;
                }
            }
        }
    };


    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission" +
                        ".ACCESS_FINE_LOCATION");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck += this.checkSelfPermission("Manifest.permission" +
                        ".ACCESS_COARSE_LOCATION");
            }
            if (permissionCheck != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
                }
            } else {
                Log.i("checkBTPermission: ", "NO NEED TO CHECK PERMISSIONS, SDK VERSION < " +
                        "LOLLIPOP");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
        } catch (Exception e) {
            Log.e("UNREGISTER RECEIVER", e.toString());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBluetoothAdapter.cancelDiscovery();
        Log.i("onItemClicked()", "YOU CLICKED ON A DEVICE!!!");
        String deviceName = mBTDevices.get(position).getName();
        String deviceAddress = mBTDevices.get(position).getAddress();
        Log.i("onItemClicked()", deviceName + " : " + deviceAddress);

        // Create the Bond.
        // NOTE: Requires API 17+...
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.i("TRYING TO PAIR WITH ", deviceName);
            mBTDevices.get(position).createBond();

            mBTDevice = mBTDevices.get(position);
            mBluetoothConnection = new BluetoothConnectionService(this);
        }
    }
}
