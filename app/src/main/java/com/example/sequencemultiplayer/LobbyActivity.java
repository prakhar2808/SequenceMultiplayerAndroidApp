package com.example.sequencemultiplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.example.sequencemultiplayer.FirebaseAuthUser.*;

public class LobbyActivity extends AppCompatActivity {

    String playerName="";
    String playerID="";
    // RoomID the player either creates or joins in.
    String roomID="";

    FirebaseDatabase database;
    // Reference to the room the player wants to join.
    DatabaseReference roomRef;
    // Reference to the list of all the rooms existing.
    // Used to check if roomID entered by user is valid.
    DatabaseReference roomsRef;

    // Player details.
    ImageView imgProfilePic;
    TextView welcomeText;

    // To create a room
    Button createRoomButton;

    // To join a room using roomID
    TextView fillRoomID;
    Button joinRoomByRoomIDButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby_activity);

        imgProfilePic = findViewById(R.id.imgProfilePic);
        welcomeText = findViewById(R.id.welcomeText);
        createRoomButton = findViewById(R.id.createRoomButton);
        fillRoomID = findViewById(R.id.fillRoomID);
        fillRoomID.setText("");
        joinRoomByRoomIDButton = findViewById(R.id.joinRoomByRoomIDButton);

        welcomeText.setText("Welcome " + getDisplayName());

        if(getImgProfilePicURL() != "") {
            new LoadProfileImage(imgProfilePic).execute(getImgProfilePicURL());
        }
        else {
            imgProfilePic.setImageResource(R.drawable.user_default);
        }
        database = FirebaseDatabase.getInstance();

        joinRoomByRoomIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomID = fillRoomID.getText().toString();
                roomsRef = database.getReference("rooms");
                roomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(roomID)) {
                            Log.d("Lobby", "Room found " + roomID);
                            incrementPlayersCountAndJoinRoom(false);
                        }
                        else {
                            Log.d("Lobby", "Room not found " + roomID);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.d("Lobby", "Checking DB for room id cancelled");
                    }
                });
            }
        });

        createRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create room and add yourself a player1.
                createRoomButton.setText("CREATING ROOM");
                createRoomButton.setEnabled(false);
                roomID = Long.toString(System.currentTimeMillis()%100000000);
                incrementPlayersCountAndJoinRoom(true);
            }
        });
    }

    private void incrementPlayersCountAndJoinRoom(final Boolean roomHost) {
        playerName = getDisplayName();
        playerID = getPlayerID();
        roomRef = database.getReference("rooms/" + roomID);
        Log.d("Lobby", "Codebase 1");
        addRoomEventListener();
        Log.d("Lobby", "Codebase 2");
        if(roomHost) {
            roomRef.setValue(0);
        }
        Log.d("Lobby", "Codebase 3 Room id = " + roomID);
        Log.d("Lobby", "Codebase 6");
        //Transaction to increment players count
        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                //TODO: Check if the player is already not in some other room.
                if (currentData.child("players_count").getValue() == null) {
                    currentData.child("players_count").setValue(1);
                }
                else {
                    Long playerNumber = (Long) currentData.child("players_count").getValue() + 1;
                    currentData.child("players_count").setValue(playerNumber);
                    if(playerNumber > 4)
                        Transaction.abort();
                }
                currentData.child("players").child(playerID).child("name").setValue(playerName);
                currentData.child("players").child(playerID).child("img_url").setValue(getImgProfilePicURL());
                currentData.child("players").child(playerID).child("is_host").setValue(roomHost);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("Lobby","Firebase counter increment failed.");
                } else if(committed) {
                    Log.d("Lobby","Firebase counter increment succeeded.");
                    joinRoom();
                }
                else {
                    Log.d("Lobby", "Firebase transaction to increment player aborted");
                }
            }
        });
    }

    private void joinRoom() {
        Log.d("Lobby", "Codebase 7");
        Intent intent = new Intent(getApplicationContext(), RoomActivity.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("playerID", playerID);
        startActivity(intent);
    }

    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Join the room
                Log.d("Lobby", "Codebase 4");
                createRoomButton.setText("CREATE ROOM");
                createRoomButton.setEnabled(true);
                fillRoomID.setText("");
                Log.d("Lobby", "Codebase 5");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                createRoomButton.setText("CREATE ROOM");
                createRoomButton.setEnabled(false);
                Toast.makeText(LobbyActivity.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Background Async task to load user profile picture from url
     * */
    public class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... uri) {
            String url = uri[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                Bitmap resized = Bitmap.createScaledBitmap(result,200,200, true);
                bmImage.setImageBitmap(ImageHelper
                        .getRoundedCornerBitmap(LobbyActivity.this, resized,250,200,200,
                                false, false, false, false));
            }
        }
    }

}
