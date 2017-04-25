package com.vituniversity.hostelautomation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class KBlock extends AppCompatActivity {

    Button bGroundFloor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kblock);

        bGroundFloor = (Button) findViewById(R.id.b0Floor);
        bGroundFloor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.vituniversity.hostelautomation.GroundFloor");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}
