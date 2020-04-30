package com.example.sequencemultiplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class RoomActivity extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference playersRef;
    DatabaseReference roomsRef;

    String roomID;
    String playerID;

    SharedPreferencesHelper sharedpreferences;

    ArrayList<PlayerDetails> playerDetailsList;
    private PlayersInRoomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_activity);

        sharedpreferences = new SharedPreferencesHelper(this);

        sharedpreferences.putString("last_activity", "RoomActivity");

        roomID = sharedpreferences.getString("roomID", "0");
        playerID = sharedpreferences.getString("playerID", "0");
        Log.d("Room", roomID + " " + playerID);

        TextView roomIDTextView = findViewById(R.id.roomIDTextView);
        roomIDTextView.setText("Room ID : "+roomID);

        playerDetailsList = new ArrayList<>();

        database = FirebaseDatabase.getInstance();
        playersRef = database.getReference("rooms/"+roomID+"/players");
        addPlayersValueEventListener();

    }

    void addPlayersValueEventListener() {
        playersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                playerDetailsList.clear();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String playerID = ds.getKey();
                    String playerName = ds.child("name").getValue().toString();
                    String imgProfilePicURL = ds.child("img_url").getValue().toString();
                    playerDetailsList.add(new PlayerDetails(playerID, playerName, imgProfilePicURL));
                }
                Log.d("Adapter", "Codebase 1");
                RoomActivity.this.adapter = new PlayersInRoomAdapter(RoomActivity.this,
                                R.layout.player_in_roomlist_row, playerDetailsList);

                Log.d("Adapter", "Codebase 2");
                ListView playersInRoomListView = (ListView)findViewById(R.id.playersInRoomListView);
                Log.d("Adapter", "Codebase 3");
                playersInRoomListView.setAdapter(RoomActivity.this.adapter);
                Log.d("Adapter", "Codebase 4");
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
                        && currentData.child(roomID).child("players").child(playerID).getValue() != null) {
                    if((Long)currentData.child(roomID).child("players_count").getValue() == 1) {
                        currentData.child(roomID).setValue(null);
                    }
                    else {
                        currentData.child(roomID).child("players_count")
                                .setValue((Long) currentData.child(roomID).child("players_count").getValue() - 1);
                        currentData.child(roomID).child("players").child(playerID).setValue(null);
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

                    sharedpreferences.clear();

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
                else {
                    Log.d("Room", "Firebase transaction to delete player aborted");
                }
            }
        });
    }
}
