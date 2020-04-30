package com.example.sequencemultiplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.example.sequencemultiplayer.FirebaseAuthUser.setFirebaseUserFromGoogleAuth;

public class GoogleAuthFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    // Request sign in code. Could be anything as you required.
    public static final int requestSignInCode = 7;
    // Google API Client object.
    private GoogleApiClient mGoogleApiClient;
    //Sign in Button
    public com.google.android.gms.common.SignInButton googleSignInButton;
    private GoogleSignInOptions gso;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API,gso)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d("Gmail", "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.google_auth_fragment, parent, false);

        googleSignInButton = v.findViewById(R.id.googleSignInButton);
        googleSignInButton.setVisibility(View.GONE);
        // OnClick Listener for sign in button.
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, requestSignInCode);
            }

        });
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == requestSignInCode) {
            GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(googleSignInResult);
        }
    }


    private void handleSignInResult(GoogleSignInResult googleSignInResult) {
        Log.d("Gmail", "handleSignInResult:" + googleSignInResult.isSuccess());
        if (googleSignInResult.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount googleSignInAccount = googleSignInResult.getSignInAccount();
            Log.d("Gmail","Token = " + googleSignInAccount.getIdToken());

            setFirebaseUserFromGoogleAuth(this, googleSignInAccount);

            updateUI(true);
        }
        else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    public void insertPlayerIDInDatabase() {
        Log.d("Gmail","Codebase 0.5");
        String playerID = FirebaseAuthUser.getPlayerID();
        Log.d("Gmail", "PlayerID = " + playerID);
        if(!playerID.equals("")) {
            DatabaseReference playerRef = FirebaseDatabase.getInstance()
                    .getReference("players/"+playerID);
            addEventListener(playerRef, playerID);
            playerRef.setValue(FirebaseAuthUser.getDisplayName());
        }
        Log.d("Gmail","Codebase 0.8");
    }

    private void addEventListener(DatabaseReference playerRef, final String playerID) {
        //Read from database
        playerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Success --> Continue to next screen after saving player name.
                if(!playerID.equals("")) {
                    Log.d("Gmail", "Codebase 14");
                    Intent intent = new Intent(getActivity().getApplicationContext(),
                            LobbyActivity.class);
                    intent.putExtra("googleSignIn", true);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateUI(boolean signedIn) {
        if (!signedIn) {
            googleSignInButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d("Gmail", "onConnectionFailed:" + connectionResult);
    }
}
