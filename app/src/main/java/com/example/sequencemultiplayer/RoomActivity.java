package com.example.sequencemultiplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class RoomActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference roomRef;
    DatabaseReference roomsRef;

    String roomID;
    String playerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_activity);
        roomID = getIntent().getStringExtra("roomID");
        playerID = getIntent().getStringExtra("playerID");
        Log.d("Room", roomID + " " + playerID);
        database = FirebaseDatabase.getInstance();
        roomRef = database.getReference("rooms/"+roomID);
        addRoomEventListener();
    }

    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    new AlertDialog.Builder(RoomActivity.this)
                            .setMessage("Oops! The host has deleted the room. Return to lobby.")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            })
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d("Room", "Back Pressed");
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit the room?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deletePlayerFromRoom();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deletePlayerFromRoom() {
        //Transaction to delete player (and the room if the player is the host).
        roomsRef = database.getReference("rooms");
        Log.d("Room", "Deleting player");
        roomsRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if(currentData.child(roomID).child("players_count").getValue() != null
                        && currentData.child(roomID).child("players").child(playerID).getValue() != null
                        && currentData.child(roomID).child("players")
                           .child(playerID).child("is_host").getValue() != null) {

                    if((Boolean)currentData.child(roomID).child("players")
                            .child(playerID).child("is_host").getValue() == true) {
                        Log.d("Room", "This player is host. Deleting room.");
                        currentData.child(roomID).setValue(null);
                    }

                    else {
                        currentData.child(roomID).child("players_count")
                                .setValue((Long) currentData.child(roomID).child("players_count").getValue() - 1);
                        currentData.child(roomID).child("players").child(playerID). setValue(null);
                    }
                }
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("Room","Firebase player deletion failed.");
                } else if(committed) {
                    Log.d("Room","Firebase player deletion succeeded.");
                    finish();
                }
                else {
                    Log.d("Room", "Firebase transaction to delete player aborted");
                }
            }
        });
    }
}
