package com.example.sequencemultiplayer;


import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import android.content.DialogInterface;
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

        FragmentManager fragmentManager = getSupportFragmentManager();
        googleAuthFragment = fragmentManager.findFragmentById(R.id.googleAuthFragment);

        if (googleAuthFragment == null) {
            googleAuthFragment = new GoogleAuthFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.googleAuthFragment, googleAuthFragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("MainActivity", "onBackPressed Called");
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Add won other player to database
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
