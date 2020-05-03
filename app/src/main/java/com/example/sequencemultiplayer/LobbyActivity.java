package com.example.sequencemultiplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.FirebaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.example.sequencemultiplayer.FirebaseAuthUser.*;

public class LobbyActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    String playerName="";
    String playerID="";
    Boolean isRoomHost;

    // Sign Out button.
    Button signOutButton;

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

    SharedPreferencesHelper sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //For full screen mode.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.lobby_activity);

        sharedpreferences = new SharedPreferencesHelper(this);

        sharedpreferences.putString("last_activity", "LobbyActivity");

        signOutButton = findViewById(R.id.signOutButton);
        imgProfilePic = findViewById(R.id.imgProfilePic);
        welcomeText = findViewById(R.id.welcomeText);
        createRoomButton = findViewById(R.id.createRoomButton);
        fillRoomID = findViewById(R.id.fillRoomID);
        fillRoomID.setText("");
        joinRoomByRoomIDButton = findViewById(R.id.joinRoomByRoomIDButton);

        welcomeText.setText("Welcome " + getDisplayName());

        if(getImgProfilePicURL() != "") {
            Picasso.with(this).load(getImgProfilePicURL()).into(imgProfilePic);
//            new LoadProfileImage(imgProfilePic).execute(getImgProfilePicURL());
        }
        else {
            Picasso.with(this).load(R.drawable.user_default).into(imgProfilePic);
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

        //OnClick Listener for sign out button.
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getIntent().hasExtra("googleSignIn")) {
                    googleSignOut();
                }
            }
        });
    }

    // Increment players_count by 1 and add player to room.
    private void incrementPlayersCountAndJoinRoom(final Boolean roomHost) {
        playerName = getDisplayName();
        playerID = getPlayerID();
        roomRef = database.getReference("rooms/" + roomID);
        Log.d("Lobby", "Codebase 1");
        addRoomEventListener();
        Log.d("Lobby", "Codebase 2");
        if(roomHost) {
            isRoomHost = true;
            roomRef.setValue(0);
        }
        else {
            isRoomHost = false;
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
//                if(roomHost) {
//                    currentData.child("is_host_present").setValue(true);
//                }
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("Lobby","Firebase counter increment failed.");
                } else if(committed) {
                    Log.d("Lobby","Firebase counter increment succeeded.");
                    goToRoomActivity();
                }
                else {
                    Log.d("Lobby", "Firebase transaction to increment player aborted");
                }
            }
        });
    }

    // Go to room activity.
    private void goToRoomActivity() {
        Log.d("Lobby", "Codebase 7");
        Intent intent = new Intent(getApplicationContext(), RoomActivity.class);

        sharedpreferences.putString("roomID", roomID);
        sharedpreferences.putString("playerID", playerID);
        sharedpreferences.putString("isRoomHost", isRoomHost.toString());

        startActivity(intent);
    }

    // Event listener for room when user returns back from inside a room after leaving it.
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

    // SignOut method for Google Auth.
    private void googleSignOut() {
        Log.d("LobbyLogout", "Codebase 0");
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        Log.d("LobbyLogout", "Codebase 1");
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        final GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(LobbyActivity.this)
                .enableAutoManage(LobbyActivity.this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();
        Log.d("LobbyLogout", "Codebase 2");
        mGoogleApiClient.connect();
        Log.d("LobbyLogout", "Codebase 3");
        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                if(mGoogleApiClient.isConnected()) {
                    Log.d("LobbyLogout", "Codebase 4");
                    Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                Log.d("LobbyLogout", "Codebase 5. Logged Out.");
                                clearFirebaseUserFromGoogleAuth();

                                sharedpreferences.clear();

                                Intent intent = new Intent(LobbyActivity.this, MainActivity.class);
                                startActivity(intent);
                                Log.d("LobbyLogout", "Codebase 6");
                                finish();
                            }
                        }
                    });
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d("LobbyLogout", "Google API Client Connection Suspended");
            }
        });
    }
    // Helper method for Google sign out.
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d("Gmail", "onConnectionFailed:" + connectionResult);
    }

    /**
     * Background Async task class to load user profile picture from url
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
