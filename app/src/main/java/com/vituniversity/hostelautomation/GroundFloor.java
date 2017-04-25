package com.vituniversity.hostelautomation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class GroundFloor extends Activity implements View.OnClickListener {

    Button bRoom1, bRoom2, bRoom3, bRoom4, bBathroom1, bBathroom2, bCorridor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ground_floor);

        bRoom1 = (Button) findViewById(R.id.bRoom1);
        bRoom2 = (Button) findViewById(R.id.bRoom2);
        bRoom3 = (Button) findViewById(R.id.bRoom3);
        bRoom4 = (Button) findViewById(R.id.bRoom4);
        bBathroom1 = (Button) findViewById(R.id.bBathroom1);
        bBathroom2 = (Button) findViewById(R.id.bBathroom2);
        bCorridor = (Button) findViewById(R.id.bCorridor);

        bRoom1.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

    }
}
