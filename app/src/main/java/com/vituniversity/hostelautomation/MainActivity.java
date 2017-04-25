package com.vituniversity.hostelautomation;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vituniversity.hostelautomation.bluetoothchat.BluetoothActivity;
import com.vituniversity.hostelautomation.bluetoothchat.BluetoothChatFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bKBlock = (Button) findViewById(R.id.bKBlock);
        bKBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.vituniversity.hostelautomation.KBlock");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

        Button bBluetooth = (Button) findViewById(R.id.bBluetooth);
        bBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), BluetoothActivity.class);
                startActivity(intent);
            }
        });

    }
}
