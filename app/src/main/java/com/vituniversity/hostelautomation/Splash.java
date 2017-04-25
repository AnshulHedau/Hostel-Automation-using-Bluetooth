package com.vituniversity.hostelautomation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        Thread timer = new Thread(){
            public void run(){
                try{
                    sleep(0);
                    //sleep(4000);
                }catch(InterruptedException e){
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
                }finally {
                    Intent openMenu;
                    openMenu = new Intent(getBaseContext(),MainActivity.class);
                    startActivity(openMenu);
                }
            }
        };
        timer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
