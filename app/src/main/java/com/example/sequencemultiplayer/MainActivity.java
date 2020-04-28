package com.example.sequencemultiplayer;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;


public class MainActivity extends FragmentActivity {

    //Google auth fragment object
    private Fragment googleAuthFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Gmail","Codebase 0");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
