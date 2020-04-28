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
    String roomID="";

    FirebaseDatabase database;
    DatabaseReference roomRef;
    DatabaseReference roomsRef;
    DatabaseReference playerIDRef;

    ImageView imgProfilePic;
    TextView welcomeText;
    Button createRoomButton;
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

    private void incrementPlayersCountAndJoinRoom(Boolean roomHost) {
        playerName = getDisplayName();
        playerID = getPlayerID();
        roomRef = database.getReference("rooms/" + roomID + "/players_count");
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
                if (currentData.getValue() == null) {
                    currentData.setValue(1);
                }
                else {
                    currentData.setValue((Long) currentData.getValue() + 1);
                    if((Long)currentData.getValue() > 4)
                        Transaction.abort();
                }
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
        database.getReference(
                "rooms/" + roomID + "/players/" + playerID + "/name").setValue(playerName);
        database.getReference(
                "rooms/" + roomID + "/players/" + playerID + "/imgProfilePicURL")
                 .setValue(getImgProfilePicURL());
    }

    private void addRoomEventListener() {
        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Join the room
                Log.d("Lobby", "Codebase 4");
//                createRoomButton.setText("CREATE ROOM");
//                createRoomButton.setEnabled(true);
//                Intent intent = new Intent(getApplicationContext(), Main3Activity.class);
//                intent.putExtra("roomID", roomID);
//                startActivity(intent);
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

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
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
        //Transaction to delete player
        playerIDRef = database.getReference("rooms/" + roomID + "/players/" + playerID);
        playerIDRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                currentData.setValue(null);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("Lobby","Firebase player deletion failed.");
                } else if(committed) {
                    Log.d("Lobby","Firebase player deletion succeeded.");
                }
                else {
                    Log.d("Lobby", "Firebase transaction to delete player aborted");
                }
            }
        });

        //Transaction to decrement players count
        Toast.makeText(LobbyActivity.this, roomID, Toast.LENGTH_SHORT).show();
        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if (currentData.getValue() != null) {
                    currentData.setValue((Long) currentData.getValue() - 1);
                }
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("Lobby","Firebase counter decrement failed.");
                } else if(committed) {
                    Log.d("Lobby","Firebase counter decrement succeeded.");
                }
                else {
                    Log.d("Lobby", "Firebase transaction to decrement player aborted");
                }
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
