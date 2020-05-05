package com.example.sequencemultiplayer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;


public class GameActivity extends Activity {

    //Database Instance
    FirebaseDatabase database;
    //Database reference to game (containing player colors, and moves as children)
    DatabaseReference gameRef;
    //Database reference to list of moves performed by the players
    DatabaseReference movesRef;
    //Database reference to room
    DatabaseReference roomRef;
    //Database reference to winner
    DatabaseReference winnerRef;

    String roomID;
    String playerID;
    Long myColor;
    //Keeps track of the last move updated in the UI of the board from the list of moves
    //stored in the 'moves' node of the database.
    Long lastMovePerformed;

    //Makes the screen unclickable during opponent's turns
    boolean screenClickable;

    SharedPreferencesHelper sharedpreferences;

    //View object for the board
    GameView gameView;

    //Count of total sequences of this player.
    int sequencesCount;

    //Next card to pick after turn.
    int nextCardInDeckIndex;

    //Play card selected index
    int playCardTouched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("GameStart", "Started");
        super.onCreate(savedInstanceState);
        //For full screen mode.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        gameView = new GameView(this);
        setContentView(gameView);
        sequencesCount = 0;

        sharedpreferences = new SharedPreferencesHelper(this);
        sharedpreferences.putString("last_activity", "LobbyActivity");

        sharedpreferences.putString("lastMovePerformed", "-1");
        lastMovePerformed = -1L;

        roomID = sharedpreferences.getString("roomID", "0");
        playerID = sharedpreferences.getString("playerID", "0");

        database = FirebaseDatabase.getInstance();

        // Room reference.
        roomRef = database.getReference("rooms/" + roomID);

        // Game reference.
        gameRef = database.getReference("rooms/" + roomID + "/game");
        Log.d("GameStart", "Going to allot color");

        // Moves reference
        movesRef = database.getReference("rooms/" + roomID + "/game/moves");
        newMoveEventListener();
        myColor = -1L;

        // Winner reference
        winnerRef = database.getReference("rooms/" + roomID + "/game/winner");
        winnerEventListener();

        // Assign colors to players.
        allotColorToPlayer();
    }

    // Transaction to allot colors to players, copy deck to local shared pref storage
    // and after commit, call the function to insert "TURN 0" in moves database
    protected void allotColorToPlayer() {
        Log.d("GameStart", "Come to allot color");
        gameRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if(currentData.child("allot_player_color").getValue() != null) {
                    myColor = (Long)currentData.child("allot_player_color").getValue()+1;
                    currentData.child("color_"+myColor).setValue(playerID);
                    currentData.child("allot_player_color").setValue(myColor);
                    for(int i=0; i<Deck.totalCards; i++) {
                        String card =
                                currentData.child("deck").child("cards").child(""+i).getValue().toString();
                        if(currentData.child("deck").child("cards").child(""+i).getValue() == null) {
                            return Transaction.success(null);
                        }
                        sharedpreferences.putString("deckCard" + i, card);
                    }
                    for(int i=0; i<6; i++) {
                        gameView.playCards[i].value =
                                currentData.child("deck").child("cards").child(""+(myColor*6 + i)).getValue().toString();
                    }
                }
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(DatabaseError firebaseError, boolean committed, DataSnapshot currentData) {
                if (firebaseError != null) {
                    Log.d("GameStart","Firebase allotting color failed.");
                } else if(committed) {
                    Log.d("GameStart","Firebase allotting color succeeded.");
                    for(int i=0; i<6; i++) {
                        Log.d("DeckGame", gameView.playCards[i].value);
                    }
                    gameView.updateplayCards();
                    if(myColor == 0) {
                        addInitGameCommands();
                    }
                }
                else {
                    Log.d("GameStart", "Firebase transaction to allot color aborted");
                }
            }
        });
    }

    void addInitGameCommands() {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                mutableData.child("moves").child("0").setValue("TURN 0 TAKE 12");
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {

            }
        });
    }

    // Listens if the game has ended
    void winnerEventListener() {
        winnerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    Log.d("GameMove", "Game ended. Winner is " + dataSnapshot.getValue());
                    String winner = "You win!";
                    if(!dataSnapshot.getValue().toString().equals(Long.toString(myColor))) {
                        winner = "Your opponent wins!";
                    }
                    new AlertDialog.Builder(GameActivity.this)
                            .setMessage(winner+" Press OK to return to Lobby.")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // Add won other player to database
                                    deletePlayerFromRoom(true);
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

    // Listens to a new move and performs actions accordingly
    void newMoveEventListener() {
        movesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                lastMovePerformed = Long.parseLong(sharedpreferences.getString
                        ("lastMovePerformed", Long.toString(lastMovePerformed)));
                Log.d("GameMove", lastMovePerformed.toString());
                while(dataSnapshot.child(Long.toString(lastMovePerformed+1)).getValue() != null) {
                    String move = dataSnapshot.child(Long.toString(lastMovePerformed+1)).getValue().toString();
                    Log.d("GameMove", move+ " " + (lastMovePerformed+1));
                    if(move.substring(0,6).equals("TURN " + myColor)) {
                        gameView.whoseTurn.setText("Your Turn");
                        gameView.border.setColor(Color.GREEN); //white background
                        gameView.border.setStroke(1, Color.GREEN); //black border with full opacity
                        // Perform turn and add "PUT NEXT".
                        playCardTouched = -1;
                        // Check if any card in player's hand is occupied on both instances.
                        // If yes, then replace with a new card from the deck.
                        nextCardInDeckIndex = Integer.parseInt(move.split(" ")[3]);
                        // Check if all the cards in hand have atleast one empty occurence
                        checkIfAllHandCardsHaveAnEmptyOccurrence();
                        setScreenClickable();
                        ++lastMovePerformed;
                        break;
                        // TODO_LATER : If next doesn't reply then next, ..
                    }
                    if(move.substring(0,3).equals("PUT")) {
                        // Update GameBoard and put appropriately.
                        String tokens[] = move.split(" ");
                        updateGameBoardPut(tokens);
                    }
                    if(move.substring(0,3).equals("REM")) {
                        // Update GameBoard and remove appropriately.
                        String tokens[] = move.split(" ");
                        updateGameBoardRemove(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                    }
                    ++lastMovePerformed;
                }
                sharedpreferences.putString("lastMovePerformed", Long.toString(lastMovePerformed));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkIfAllHandCardsHaveAnEmptyOccurrence() {
        for(int i=0; i<6; i++) {
            int card1 = Integer.parseInt(gameView.playCards[i].value.split(" ")[0]);
            int card2 = Integer.parseInt(gameView.playCards[i].value.split(" ")[1]);
            while (card1 < 100
                    && gameView.cards[card1 / 10][card1 % 10].isOccupied
                    && gameView.cards[card2 / 10][card2 % 10].isOccupied) {
                gameView.playCards[i].value =
                        sharedpreferences.getString("deckCard" + nextCardInDeckIndex, "null");
                ++nextCardInDeckIndex;
                card1 = Integer.parseInt(gameView.playCards[i].value.split(" ")[0]);
                card2 = Integer.parseInt(gameView.playCards[i].value.split(" ")[1]);
                Log.d("GameRepCard", nextCardInDeckIndex + " " + card1 + " " + card2);
            }
        }
        gameView.updateplayCards();
    }

    // Return the card which the player touched
    public int getTouchedCard(float touchX, float touchY) {
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                if(i == 0 && j == 0 || i == 0 && j == 9 || i == 9 && j == 0 || i == 9 && j == 9)
                    continue;
                if(gameView.cards[i][j].x1 <= touchX && gameView.cards[i][j].x2 >= touchX
                        && gameView.cards[i][j].y1 <= touchY && gameView.cards[i][j].y2 >= touchY) {
                    return i*10+j;
                }
            }
        }
        return -1;
    }

    // Return the play card which the player touched
    public int getTouchedPlayCard(float touchX, float touchY) {
        for(int i=0; i<6; i++) {
                if(gameView.playCards[i].x1 <= touchX && gameView.playCards[i].x2 >= touchX
                        && gameView.playCards[i].y1 <= touchY && gameView.playCards[i].y2 >= touchY) {
                    return i;
            }
        }
        return -1;
    }

    // To detect player touch
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int touchX = (int)event.getX();
        int touchY = (int)event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                int card = getTouchedCard(touchX, touchY);
                if(getTouchedPlayCard(touchX, touchY) != -1) {
                    playCardTouched = getTouchedPlayCard(touchX, touchY);
                }
                if(playCardTouched != -1 && screenClickable) {
                    Log.d("GameView", "Touching down play card number: " + playCardTouched);
                    // Make other cards dim
                    for(int i=0; i<6; i++) {
                        if(i == playCardTouched) {
                            gameView.playCards[i].setOpacity(255);
                        }
                        else {
                            gameView.playCards[i].setOpacity(100);
                        }
                    }
                    String tokens[] = gameView.playCards[playCardTouched].value.split(" ");
                    for(int i=0; i<10; i++) {
                        for(int j=0; j<10; j++) {
                            // One-eyed jack
                            if(tokens[0].equals("100") || tokens[0].equals("101")) {
                                if(gameView.cards[i][j].isOccupied
                                        && gameView.cards[i][j].occupier != myColor
                                        && !gameView.cards[i][j].isInSequence) {
                                    gameView.cards[i][j].setOpacity(255);
                                }
                                else {
                                    gameView.cards[i][j].setOpacity(100);
                                }
                            }
                            // Two-eyed jack
                            else if(tokens[0].equals("110") || tokens[0].equals("111")) {
                                if(!gameView.cards[i][j].isOccupied) {
                                    gameView.cards[i][j].setOpacity(255);
                                }
                                else {
                                    gameView.cards[i][j].setOpacity(100);
                                }
                            }
                            // Others
                            else {
                                if ((i * 10 + j == Integer.parseInt(tokens[0])
                                        || i * 10 + j == Integer.parseInt(tokens[1]))
                                        && !gameView.cards[i][j].isOccupied) {
                                    gameView.cards[i][j].setOpacity(255);
                                } else {
                                    gameView.cards[i][j].setOpacity(100);
                                }
                            }
                        }
                    }
                }
                if(playCardTouched!= -1 && card != -1 && screenClickable) {
                    Log.d("GameView", "Touching down card number: " + (card / 10) + ", " + (card % 10));
                    touchX = card/10;
                    touchY = card%10;
                    String tokens[] = gameView.playCards[playCardTouched].value.split(" ");
                    Log.d("GameView", "Touching " + tokens[0] + ", " + tokens[1] + " Card = " + card);
                    if(Integer.parseInt(tokens[0]) == card || Integer.parseInt(tokens[1]) == card) {
                        if(!gameView.cards[touchX][touchY].isOccupied) {
                            screenClickable = false;
                            updateGameBoardPut(new String[]{"COL", Long.toString(myColor), Integer.toString(touchX), Integer.toString(touchY)});
                            takeNextCardAndAddTurnToDatabase("PUT", touchX, touchY);
                        }
                    }
                    // One eyed-jack
                    else if(tokens[0].equals("100") || tokens[0].equals("101")) {
                        if(gameView.cards[touchX][touchY].isOccupied
                                && gameView.cards[touchX][touchY].occupier != myColor
                                && !gameView.cards[touchX][touchY].isInSequence) {
                            screenClickable = false;
                            updateGameBoardRemove(touchX, touchY);
                             takeNextCardAndAddTurnToDatabase("REMOVE", touchX, touchY);
                        }
                    }
                    // Two eyed-jack
                    else if(tokens[0].equals("110") || tokens[0].equals("111")) {
                        if(!gameView.cards[touchX][touchY].isOccupied) {
                            screenClickable = false;
                            updateGameBoardPut(new String[]{"COL", Long.toString(myColor), Integer.toString(touchX), Integer.toString(touchY)});
                            takeNextCardAndAddTurnToDatabase("PUT", touchX, touchY);
                        }
                    }
                }
                else
                    Log.d("GameView", "Touched outside");
                break;
            default:
                break;
        }
        return true;
    }

    // Set screenClickable to true so that the player can perform its turn
    void setScreenClickable() {
        Log.d("GameMove", "Come to perform turn");
        screenClickable = true;
    }

    // Add the user's turn and the notification for the next user to perform turn in the database
    void takeNextCardAndAddTurnToDatabase(String op, int touchX, int touchY) {
        // Pick next card and call updatePlayCards()
        // Add your move to database.
        Long localLastMovePerformed = Long.parseLong(sharedpreferences.getString("lastMovePerformed", "0"));
        if(op.equals("PUT")) {
            movesRef.child(Long.toString(localLastMovePerformed + 1)).setValue("PUT " + myColor
                    + " " + touchX + " " + touchY);
        }
        else if(op.equals("REMOVE")) {
            movesRef.child(Long.toString(localLastMovePerformed + 1)).setValue("REMOVE " + touchX + " " + touchY);
        }
        Log.d("GameMove", "Performed move at " + touchX + " " + touchY);

        gameView.whoseTurn.setText("Other's Turn");
        gameView.border.setColor(Color.RED); //white background
        gameView.border.setStroke(1, Color.RED); //black border with full opacity
        // Pick next card.
        gameView.playCards[playCardTouched].value =
                sharedpreferences.getString("deckCard" + nextCardInDeckIndex, "null");
        ++nextCardInDeckIndex;

        // Resetting opacity
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                gameView.cards[i][j].setOpacity(255);
            }
        }
        for(int i=0; i<6; i++)
            gameView.playCards[i].setOpacity(255);
        gameView.updateplayCards();

        if(!hasWon()) {
            movesRef.child(Long.toString(localLastMovePerformed + 2))
                    .setValue("TURN " + ((myColor + 1) % 2 + " TAKE " + nextCardInDeckIndex));
        }
        else {
            gameRef.child("winner").setValue(myColor);
        }
    }

    // Updates the gameboard by occupying card touchx, touchy with color.
    void updateGameBoardPut(String tokens[]) {
        Long color = Long.parseLong(tokens[1]);
        int touchX = Integer.parseInt(tokens[2]);
        int touchY = Integer.parseInt(tokens[3]);
        gameView.updateBoardPut(color, touchX, touchY);
        markIsSequenceForOpponent();
    }

    // Check the opponent's pieces to find it there is a sequence.
    private void markIsSequenceForOpponent() {
        // Checking all rows for 5 length sequence
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j + 4 < 10; j++) {
                Long occupier = gameView.cards[i][j].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if(gameView.cards[i][j].occupier == occupier
                        && gameView.cards[i][j + 1].occupier == occupier
                        && gameView.cards[i][j + 2].occupier == occupier
                        && gameView.cards[i][j + 3].occupier == occupier
                        && gameView.cards[i][j + 4].occupier == occupier) {
                    gameView.cards[i][j].isInSequence = true;
                    gameView.cards[i][j + 1].isInSequence = true;
                    gameView.cards[i][j + 2].isInSequence = true;
                    gameView.cards[i][j + 3].isInSequence = true;
                    gameView.cards[i][j + 4].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }


        // Checking all columns for 5 length sequence
        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Long occupier = gameView.cards[i][j].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if (gameView.cards[i][j].occupier == occupier
                        && gameView.cards[i + 1][j].occupier == occupier
                        && gameView.cards[i + 2][j].occupier == occupier
                        && gameView.cards[i + 3][j].occupier == occupier
                        && gameView.cards[i + 4][j].occupier == occupier) {
                    gameView.cards[i][j].isInSequence = true;
                    gameView.cards[i + 1][j].isInSequence = true;
                    gameView.cards[i + 2][j].isInSequence = true;
                    gameView.cards[i + 3][j].isInSequence = true;
                    gameView.cards[i + 4][j].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }

        // Checking all diagonals for length 5 sequence
        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 0; j + 4 < 10; j++) {
                Long occupier = gameView.cards[i][j].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if (gameView.cards[i][j].occupier == occupier
                        && gameView.cards[i + 1][j + 1].occupier == occupier
                        && gameView.cards[i + 2][j + 2].occupier == occupier
                        && gameView.cards[i + 3][j + 3].occupier == occupier
                        && gameView.cards[i + 4][j + 4].occupier == occupier) {
                    gameView.cards[i][j].isInSequence = true;
                    gameView.cards[i + 1][j + 1].isInSequence = true;
                    gameView.cards[i + 2][j + 2].isInSequence = true;
                    gameView.cards[i + 3][j + 3].isInSequence = true;
                    gameView.cards[i + 4][j + 4].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }

        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 9; j - 4 >= 0; j--) {
                Long occupier = gameView.cards[i][j].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if (gameView.cards[i][j].occupier == occupier
                        && gameView.cards[i + 1][j - 1].occupier == occupier
                        && gameView.cards[i + 2][j - 2].occupier == occupier
                        && gameView.cards[i + 3][j - 3].occupier == occupier
                        && gameView.cards[i + 4][j - 4].occupier == occupier) {
                    gameView.cards[i][j].isInSequence = true;
                    gameView.cards[i + 1][j - 1].isInSequence = true;
                    gameView.cards[i + 2][j - 2].isInSequence = true;
                    gameView.cards[i + 3][j - 3].isInSequence = true;
                    gameView.cards[i + 4][j - 4].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }

        // Now special cases with 4 length sequences.
        // Horizontal
        for(int i = 0; i < 10; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                Long occupier = gameView.cards[i][j].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if (gameView.cards[i][j].occupier == occupier
                        && gameView.cards[i][j + 1].occupier == occupier
                        && gameView.cards[i][j + 2].occupier == occupier
                        && gameView.cards[i][j + 3].occupier == occupier) {
                    gameView.cards[i][j].isInSequence = true;
                    gameView.cards[i][j + 1].isInSequence = true;
                    gameView.cards[i][j + 2].isInSequence = true;
                    gameView.cards[i][j + 3].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }
        // Vertical
        for(int i = 0; i < 10; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                Long occupier = gameView.cards[j][i].occupier;
                if(occupier == -1 || occupier == myColor) {
                    continue;
                }
                if (gameView.cards[j][i].occupier == occupier
                        && gameView.cards[j + 1][i].occupier == occupier
                        && gameView.cards[j + 2][i].occupier == occupier
                        && gameView.cards[j + 3][i].occupier == occupier) {
                    gameView.cards[j][i].isInSequence = true;
                    gameView.cards[j + 1][i].isInSequence = true;
                    gameView.cards[j + 2][i].isInSequence = true;
                    gameView.cards[j + 3][i].isInSequence = true;
                    Log.d("GameMove", "Found sequence");
                }
            }
        }
        // 11 22 33 44, 18 27 36 45, 55 66 77 88, 54 63 72 81
        for(int i = 1; i < 6; i += 4) {
            Long occupier = gameView.cards[i][i].occupier;
            if (occupier != -1  && occupier != myColor && gameView.cards[i][i].occupier == occupier
                    && gameView.cards[i + 1][i + 1].occupier == occupier
                    && gameView.cards[i + 2][i + 2].occupier == occupier
                    && gameView.cards[i + 3][i + 3].occupier == occupier) {
                gameView.cards[i][i].isInSequence = true;
                gameView.cards[i + 1][i + 1].isInSequence = true;
                gameView.cards[i + 2][i + 2].isInSequence = true;
                gameView.cards[i + 3][i + 3].isInSequence = true;
                Log.d("GameMove", "Found sequence");
            }
            occupier = gameView.cards[i][9-i].occupier;
            if (occupier != -1 && occupier != myColor && gameView.cards[i][9 - i].occupier == occupier
                    && gameView.cards[i + 1][9 - i - 1].occupier == occupier
                    && gameView.cards[i + 2][9 - i - 2].occupier == occupier
                    && gameView.cards[i + 3][9 - i - 3].occupier == occupier) {
                gameView.cards[i][9 - i].isInSequence = true;
                gameView.cards[i + 1][9 - i - 1].isInSequence = true;
                gameView.cards[i + 2][9 - i - 2].isInSequence = true;
                gameView.cards[i + 3][9 - i - 3].isInSequence = true;
                Log.d("GameMove", "Found sequence");
            }
        }
    }

    // Updates the gameboard by removing when one-eyed jack is played
    void updateGameBoardRemove(int touchX, int touchY) {
        gameView.updateBoardRemove(touchX, touchY);
    }

    // Checks if the player has won
    boolean hasWon() {
        boolean result = false;

        // Checking all rows for 5 length sequence
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j + 4 < 10; j++) {
                if(gameView.cards[i][j].occupier == myColor
                        && gameView.cards[i][j + 1].occupier == myColor
                        && gameView.cards[i][j + 2].occupier == myColor
                        && gameView.cards[i][j + 3].occupier == myColor
                        && gameView.cards[i][j + 4].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for(int k = j; k < j + 5; k++) {
                        if(gameView.cards[i][k].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if(isInSequenceCount <= 1) {
                        gameView.cards[i][j].isInSequence = true;
                        gameView.cards[i][j + 1].isInSequence = true;
                        gameView.cards[i][j + 2].isInSequence = true;
                        gameView.cards[i][j + 3].isInSequence = true;
                        gameView.cards[i][j + 4].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }


        // Checking all columns for 5 length sequence
        for(int i = 0; i + 4 < 10 && !result; i++) {
            for (int j = 0; j < 10; j++) {
                if (gameView.cards[i][j].occupier == myColor
                        && gameView.cards[i + 1][j].occupier == myColor
                        && gameView.cards[i + 2][j].occupier == myColor
                        && gameView.cards[i + 3][j].occupier == myColor
                        && gameView.cards[i + 4][j].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for (int k = 0; k < 5; k++) {
                        if (gameView.cards[i + k][j].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if (isInSequenceCount <= 1) {
                        gameView.cards[i][j].isInSequence = true;
                        gameView.cards[i + 1][j].isInSequence = true;
                        gameView.cards[i + 2][j].isInSequence = true;
                        gameView.cards[i + 3][j].isInSequence = true;
                        gameView.cards[i + 4][j].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }

        // Checking all diagonals for length 5 sequence
        for(int i = 0; i + 4 < 10 && !result; i++) {
            for (int j = 0; j + 4 < 10; j++) {
                if (gameView.cards[i][j].occupier == myColor
                        && gameView.cards[i + 1][j + 1].occupier == myColor
                        && gameView.cards[i + 2][j + 2].occupier == myColor
                        && gameView.cards[i + 3][j + 3].occupier == myColor
                        && gameView.cards[i + 4][j + 4].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for (int k = 0; k < 5; k++) {
                        if (gameView.cards[i + k][j + k].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if (isInSequenceCount <= 1) {
                        gameView.cards[i][j].isInSequence = true;
                        gameView.cards[i + 1][j + 1].isInSequence = true;
                        gameView.cards[i + 2][j + 2].isInSequence = true;
                        gameView.cards[i + 3][j + 3].isInSequence = true;
                        gameView.cards[i + 4][j + 4].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }

        for(int i = 0; i + 4 < 10 && !result; i++) {
            for (int j = 9; j - 4 >= 0; j--) {
                if (gameView.cards[i][j].occupier == myColor
                        && gameView.cards[i + 1][j - 1].occupier == myColor
                        && gameView.cards[i + 2][j - 2].occupier == myColor
                        && gameView.cards[i + 3][j - 3].occupier == myColor
                        && gameView.cards[i + 4][j - 4].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for (int k = 0; k < 5; k++) {
                        if (gameView.cards[i + k][j - k].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if (isInSequenceCount <= 1) {
                        gameView.cards[i][j].isInSequence = true;
                        gameView.cards[i + 1][j - 1].isInSequence = true;
                        gameView.cards[i + 2][j - 2].isInSequence = true;
                        gameView.cards[i + 3][j - 3].isInSequence = true;
                        gameView.cards[i + 4][j - 4].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }

        // Now special cases with 4 length sequences.
        // Horizontal
        for(int i = 0; i < 10 && !result; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                if (gameView.cards[i][j].occupier == myColor
                        && gameView.cards[i][j + 1].occupier == myColor
                        && gameView.cards[i][j + 2].occupier == myColor
                        && gameView.cards[i][j + 3].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for (int k = 0; k < 4; k++) {
                        if (gameView.cards[i][j + k].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if (isInSequenceCount <= 1) {
                        gameView.cards[i][j].isInSequence = true;
                        gameView.cards[i][j + 1].isInSequence = true;
                        gameView.cards[i][j + 2].isInSequence = true;
                        gameView.cards[i][j + 3].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                        }
                    }
                }
            }
        }
        // Vertical
        for(int i = 0; i < 10 && !result; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                if (gameView.cards[j][i].occupier == myColor
                        && gameView.cards[j + 1][i].occupier == myColor
                        && gameView.cards[j + 2][i].occupier == myColor
                        && gameView.cards[j + 3][i].occupier == myColor) {
                    int isInSequenceCount = 0;
                    for (int k = 0; k < 4; k++) {
                        if (gameView.cards[j + k][i].isInSequence) {
                            ++isInSequenceCount;
                        }
                    }
                    if (isInSequenceCount <= 1) {
                        gameView.cards[j][i].isInSequence = true;
                        gameView.cards[j + 1][i].isInSequence = true;
                        gameView.cards[j + 2][i].isInSequence = true;
                        gameView.cards[j + 3][i].isInSequence = true;
                        ++sequencesCount;
                        Log.d("GameMove", "Found sequence");
                        if (sequencesCount == 2) {
                            result = true;
                        }
                    }
                }
            }
        }
        // 11 22 33 44, 18 27 36 45, 55 66 77 88, 54 63 72 81
        for(int i = 1; i < 6 && !result; i += 4) {
            if (gameView.cards[i][i].occupier == myColor
                    && gameView.cards[i + 1][i + 1].occupier == myColor
                    && gameView.cards[i + 2][i + 2].occupier == myColor
                    && gameView.cards[i + 3][i + 3].occupier == myColor) {
                int isInSequenceCount = 0;
                for (int k = 0; k < 4; k++) {
                    if (gameView.cards[i + k][i + k].isInSequence) {
                        ++isInSequenceCount;
                    }
                }
                if (isInSequenceCount <= 1) {
                    gameView.cards[i][i].isInSequence = true;
                    gameView.cards[i + 1][i + 1].isInSequence = true;
                    gameView.cards[i + 2][i + 2].isInSequence = true;
                    gameView.cards[i + 3][i + 3].isInSequence = true;
                    ++sequencesCount;
                    Log.d("GameMove", "Found sequence");
                    if (sequencesCount == 2) {
                        result = true;
                    }
                }
            }
            if (gameView.cards[i][9 - i].occupier == myColor
                    && gameView.cards[i + 1][9 - i - 1].occupier == myColor
                    && gameView.cards[i + 2][9 - i - 2].occupier == myColor
                    && gameView.cards[i + 3][9 - i - 3].occupier == myColor) {
                int isInSequenceCount = 0;
                for (int k = 0; k < 4; k++) {
                    if (gameView.cards[i + k][9 - i - k].isInSequence) {
                        ++isInSequenceCount;
                    }
                }
                if (isInSequenceCount <= 1) {
                    gameView.cards[i][9 - i].isInSequence = true;
                    gameView.cards[i + 1][9 - i - 1].isInSequence = true;
                    gameView.cards[i + 2][9 - i - 2].isInSequence = true;
                    gameView.cards[i + 3][9 - i - 3].isInSequence = true;
                    ++sequencesCount;
                    Log.d("GameMove", "Found sequence");
                    if (sequencesCount == 2) {
                        result = true;
                    }
                }
            }
        }
        if(sequencesCount == 1) {
            Log.d("GameMove", "My sequences 1");
        }
        if(sequencesCount == 2) {
            Log.d("GameMove", "My sequences 2");
            result = true;
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        Log.d("GameMove", "onBackPressed Called");
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to leave the game?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Add won other player to database
                        deletePlayerFromRoom(false);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deletePlayerFromRoom(final boolean thisPlayerHasWon) {
        //Transaction to delete player (and the room if the player is the host).
        DatabaseReference roomsRef = database.getReference("rooms");
        Log.d("Room", "Deleting player");
        roomRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData currentData) {
                if(currentData.child("players_count").getValue() != null
                        && currentData.child("players").child(playerID).getValue() != null) {
                    if((Long)currentData.child("players_count").getValue() == 1) {
                        currentData.setValue(null);
                    }
                    else {
                        currentData.child("players_count")
                                .setValue((Long) currentData.child("players_count").getValue() - 1);
                        currentData.child("players").child(playerID).setValue(null);
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
                    if(!thisPlayerHasWon) {
                        gameRef.child("winner").setValue((myColor + 1) % 2);
                    }
                    sharedpreferences.clear();
                    Intent intent = new Intent(GameActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Log.d("Room", "Firebase transaction to delete player aborted");
                }
            }
        });
    }
}
