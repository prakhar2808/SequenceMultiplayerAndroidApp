package com.example.sequencemultiplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
    DatabaseReference gameStartedRef;
    DatabaseReference playersCountRef;

    String roomID;
    String playerID;
    String isRoomHost;

    SharedPreferencesHelper sharedpreferences;

    Button playButton;

    ArrayList<PlayerDetails> playerDetailsList;
    private PlayersInRoomAdapter adapter;

    int playersCountInRoom;

    //Boolean to overcome some race conditions
    boolean leftRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //For full screen mode.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.room_activity);

        database = FirebaseDatabase.getInstance();

        playButton = findViewById(R.id.playButton);

        sharedpreferences = new SharedPreferencesHelper(this);

        sharedpreferences.putString("gameStarted", "false");

        playersCountInRoom = 1;
        leftRoom = false;

        roomID = sharedpreferences.getString("roomID", "0");
        playerID = sharedpreferences.getString("playerID", "0");
        Log.d("Room", roomID + " " + playerID);

        isRoomHost = sharedpreferences.getString("isRoomHost", "false");
        if(isRoomHost.equalsIgnoreCase("true")) {
            // Initialize deck and store it in db. Then only set visible after
            // transaction is committed so as to ensure that the game is started
            // only after the deck is present and visible to all players.
            initializeDeckAndShowPlayButton();
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAllotPlayerColor();
                }
            });
        }

        TextView roomIDTextView = findViewById(R.id.roomIDTextView);
        roomIDTextView.setText("Room ID : "+roomID);

        playerDetailsList = new ArrayList<>();
        playersRef = database.getReference("rooms/"+roomID+"/players");
        addPlayersValueEventListener();

        gameStartedRef = database.getReference("rooms/"+roomID+"/game/game_started");
        startMatchValueEventListener();
    }

    void initializeDeckAndShowPlayButton() {
        DatabaseReference deckReference = database.getReference("rooms/"+roomID+"/game/deck");
        final Deck deck = new Deck();
        deckReference.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                for(int i = 0; i < Deck.totalCards; i++) {
                    mutableData.child("cards").child(Integer.toString(i)).setValue(deck.deck[i]);
                }
                mutableData.child("last_card_taken").setValue(-1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if(committed) {
                    playButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    void startMatchValueEventListener() {
        gameStartedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()
                        && dataSnapshot.getValue() != null && !leftRoom) {
                    sharedpreferences.putString("gameStarted", "true");
                    if((boolean)dataSnapshot.getValue()) {
                        Intent intent = new Intent(getApplicationContext(), GameActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void addPlayersValueEventListener() {
        playersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                playerDetailsList.clear();
                boolean isHostPresent = false;
                boolean isSelfPresent = false;
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String playerID = ds.getKey();
                    if(playerID.equals(RoomActivity.this.playerID))
                        isSelfPresent = true;
                    String playerName = ds.child("name").getValue().toString();
                    String imgProfilePicURL = ds.child("img_url").getValue().toString();
                    isHostPresent |= (boolean)ds.child("is_host").getValue();
                    playerDetailsList.add(new PlayerDetails(playerID, playerName, imgProfilePicURL));
                }
                if(dataSnapshot != null) {
                    playersCountInRoom = playerDetailsList.size();
                }
                Log.d("Adapter", "Codebase 1");
                RoomActivity.this.adapter = new PlayersInRoomAdapter(RoomActivity.this,
                                R.layout.player_in_roomlist_row, playerDetailsList);

                Log.d("Adapter", "Codebase 2");
                ListView playersInRoomListView = (ListView)findViewById(R.id.playersInRoomListView);
                Log.d("Adapter", "Codebase 3");
                playersInRoomListView.setAdapter(RoomActivity.this.adapter);
                Log.d("Adapter", "Codebase 4");

                Log.d("Adapter", "HostPresent? " + isHostPresent +
                        " DS exists? " + dataSnapshot.exists() +
                        " isRoomHost? " + isRoomHost);

                if(!isHostPresent
                        && dataSnapshot.exists()
                        && isRoomHost.equalsIgnoreCase("false")
                        && isSelfPresent
                        && sharedpreferences.getString("gameStarted", "true").equals("false")) {
                    Log.d("Adapter", "In here : " +
                            "HostPresent? " + isHostPresent +
                            "DS exists? " + dataSnapshot.exists() +
                            " Is Self Present? " + isSelfPresent);
                    deletePlayerFromRoom();
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
                        leftRoom = true;
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

                    finish();
                }
                else {
                    Log.d("Room", "Firebase transaction to delete player aborted");
                }
            }
        });
    }

    void setAllotPlayerColor() {
        DatabaseReference allotPlayerColorRef =
                database.getReference("rooms/"+roomID+"/game/allot_player_color");
        allotPlayerColorRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                mutableData.setValue(-1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if(committed) {
                    Log.d("PlayersInRoom", playersCountInRoom + "");
                    if(playersCountInRoom > 1 && playersCountInRoom < 4) {
                        //TODO: Once game has started, no one should join. Hope that is ensured by
                        //TODO: aborting the transaction on >2.
                        sharedpreferences.putString("gameStarted", "true");
                        sharedpreferences.putString("numberOfPlayers", Integer.toString(playersCountInRoom));
                        database.getReference("rooms/" + roomID + "/game/game_started").setValue(true);
                    }
                }
            }
        });
    }
}
