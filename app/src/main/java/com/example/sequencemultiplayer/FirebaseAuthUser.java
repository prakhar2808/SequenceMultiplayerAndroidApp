package com.example.sequencemultiplayer;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class FirebaseAuthUser {

    private static FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    private static FirebaseUser firebaseUser;

    private static String playerID = "";
    private static String displayName = "";
    private static String email = "";
    private static String imgProfilePicURL = "";

    public static FirebaseUser getFireBaseUser() {
        return firebaseUser;
    }

    public static String getPlayerID() {
        return playerID;
    }

    public static String getDisplayName() {
        return displayName;
    }

    public static String getEmail() {
        return email;
    }

    public static String getImgProfilePicURL() {
        return imgProfilePicURL;
    }

    public static void setFirebaseUserFromGoogleAuth(final GoogleAuthFragment googleAuthFragmentObject,
                                                     final GoogleSignInAccount googleSignInAccount) {
        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
        firebaseAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task AuthResultTask) {
                        if (AuthResultTask.isSuccessful()){
                            // Getting Current Login user details.
                            firebaseUser = firebaseAuth.getCurrentUser();
                            // Getting Player ID
                            playerID = firebaseUser.getUid();
                            displayName = firebaseUser.getDisplayName();
                            email = firebaseUser.getEmail();
                            if(googleSignInAccount.getPhotoUrl() != null)
                                imgProfilePicURL = googleSignInAccount.getPhotoUrl().toString();
                            Log.d("Gmail","UID = "+playerID);
                            Log.d("Gmail","Name = "+displayName);
                            Log.d("Gmail","Email = "+email);
                            Log.d("Gmail","Profile Pic URL = "+imgProfilePicURL);
                            googleAuthFragmentObject.insertPlayerIDInDatabase();
                        }
                        else {
                            Log.e("Gmail", "Error doing firebase auth.");
                            Log.d("Gmail", "Error doing firebase auth.");
                        }
                    }
                });
    }

    public static void clearFirebaseUserFromGoogleAuth() {
        firebaseAuth.signOut();
        Log.d("Gmail", "Signed Out!");
        firebaseUser = null;
        playerID = "";
    }
}
