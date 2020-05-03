package com.example.sequencemultiplayer;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


public class MainActivity extends FragmentActivity {

    //Google auth fragment object
    private Fragment googleAuthFragment;

    //Shared Preferences instance
    SharedPreferencesHelper sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Gmail","Codebase 0");
        super.onCreate(savedInstanceState);
        //For full screen mode.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        sharedpreferences = new SharedPreferencesHelper(this);

        String last_activity = sharedpreferences.getString("last_activity", "MainActivity");

        if(last_activity.equals("RoomActivity")) {
            Log.d("Room", "Going to room");
            Intent intent = new Intent(getApplicationContext(), RoomActivity.class);
            startActivity(intent);
        }
        else {
            sharedpreferences.putString("last_activity", "MainActivity");

            FragmentManager fragmentManager = getSupportFragmentManager();
            googleAuthFragment = fragmentManager.findFragmentById(R.id.googleAuthFragment);

            if (googleAuthFragment == null) {
                googleAuthFragment = new GoogleAuthFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.googleAuthFragment, googleAuthFragment)
                        .commit();
            }
        }
    }
}
