package com.example.sequencemultiplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class GameView extends SurfaceView implements android.view.SurfaceHolder.Callback{

    private MainThread thread;
    private int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    private int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    GameBoardCard[][] cards;
    PlayCard[] playCards;
    Context context;


    public GameView(Context context) {
        super(context);
        this.context = context;
        Log.d("GameView","Creating new GameView");
        getHolder().addCallback(this);
        //Defining main thread.
        thread = new MainThread(getHolder(), this);
        //This will help GameView handle events.
        setFocusable(true);
        //Getting card
        int cardWidth = screenWidth/13;
        int leftBorder = cardWidth/2;
        int cardHeight = screenHeight/(2*11);
        int topBorder = screenHeight/4;
        int paddingBetweenCardsHorizontal = (2*cardWidth)/9;
        int paddingBetweenCardsVertical = (cardHeight)/9;
        cards = new GameBoardCard[10][10];
        for( int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                cards[i][j] = new GameBoardCard(
                        BitmapFactory.decodeResource(getResources(),
                        context.getResources().getIdentifier(
                                "card_" + i + "_" + j, "drawable",
                                context.getPackageName())),
                        cardWidth,
                        cardHeight);
            }
        }
        //Initializing playCards
        // Drawing play cards
        leftBorder = leftBorder + (cardWidth + paddingBetweenCardsHorizontal)*4;
        topBorder = screenHeight*3/4 + paddingBetweenCardsVertical*5;
        int rightBorder = screenWidth*25/26;
        int bottomBorder = screenHeight*49/50;
        paddingBetweenCardsHorizontal = (rightBorder - leftBorder)/(4*2);
        paddingBetweenCardsVertical = (bottomBorder - topBorder)/15;
        cardWidth = (rightBorder - leftBorder - paddingBetweenCardsHorizontal*2)/3;
        cardHeight = (bottomBorder - topBorder - paddingBetweenCardsVertical)/2;
        playCards = new PlayCard[6];
        for(int i = 0; i < 6; i++) {
            playCards[i] = new PlayCard(
                    BitmapFactory.decodeResource(getResources(), R.drawable.play_b2fv),
                    cardWidth, cardHeight, "");

        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.rgb(60,110,100));
            int cardWidth = screenWidth/13;
            int leftBorder = cardWidth/2;
            int cardHeight = screenHeight/(2*11);
            int topBorder = screenHeight/4;
            int paddingBetweenCardsHorizontal = (2*cardWidth)/9;
            int paddingBetweenCardsVertical = (cardHeight)/9;
            for(int i=0;i<10;i++){
                for(int j=0;j<10;j++) {
                    int left = leftBorder + (cardWidth + paddingBetweenCardsHorizontal) * j;
                    int top = topBorder + (cardHeight + paddingBetweenCardsVertical) * i;
                    int right = left + cardWidth;
                    int bottom = top + cardHeight;
                    cards[i][j].setCoordinates(left, top,
                            right + paddingBetweenCardsHorizontal,
                            bottom + paddingBetweenCardsVertical);
                    cards[i][j].draw(canvas, left, top);

                    if(cards[i][j].isOccupied) {
                        Paint paint = new Paint();
                        Long occupier = cards[i][j].occupier;
                        paint.setARGB(255,
                                       cards[i][j].R[occupier.intValue()],
                                       cards[i][j].G[occupier.intValue()],
                                       cards[i][j].B[occupier.intValue()]);
                        canvas.drawCircle((left + right) / 2, (top + bottom) / 2,
                                Math.min(cardWidth, cardHeight) * 10 / 28, paint);
                        if(true) {
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setColor(Color.BLACK);
                            paint.setStrokeWidth(5);
                            canvas.drawCircle((left + right) / 2, (top + bottom) / 2,
                                    Math.min(cardWidth, cardHeight) * 10 / 28, paint);
                        }
                    }
                }
            }
            // Drawing play cards
            leftBorder = leftBorder + (cardWidth + paddingBetweenCardsHorizontal)*4;
            topBorder = screenHeight*3/4 + paddingBetweenCardsVertical*5;
            int rightBorder = screenWidth*25/26;
            int bottomBorder = screenHeight*49/50;
            paddingBetweenCardsHorizontal = (rightBorder - leftBorder)/(4*2);
            paddingBetweenCardsVertical = (bottomBorder - topBorder)/15;
            cardWidth = (rightBorder - leftBorder - paddingBetweenCardsHorizontal*2)/3;
            cardHeight = (bottomBorder - topBorder - paddingBetweenCardsVertical)/2;

            for(int i=0; i<6; i++) {
                int left = leftBorder + (cardWidth + paddingBetweenCardsHorizontal)*(i%3);
                int right = left + cardWidth;
                int top = topBorder;
                if(i>=3)
                    top += cardHeight + paddingBetweenCardsVertical;
                int bottom = top + cardHeight;
                playCards[i].setCoordinates(left, top, right, bottom);
                playCards[i].draw(canvas, left, top);
            }
            //void drawRect(float left, float top, float right, float bottom, Paint paint)
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("GameView", "Surface Created " + thread.toString());
        if(!thread.isAlive()) {
            thread = new MainThread(getHolder(), this);
            Log.d("GameView", "Creating thread " + thread.toString());
        }
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        Log.d("GameView", "Surface Destroyed");
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    // Update board to mark (touchX, touchY) as occupied by "color".
    public void updateBoardPut(Long color, int touchX, int touchY) {
        cards[touchX][touchY].isOccupied = true;
        cards[touchX][touchY].occupier = color;
    }

    // Update to mark (touchX, touchY) as unoccupied.
    public void updateBoardRemove(int touchX, int touchY) {
        cards[touchX][touchY].isOccupied = false;
        cards[touchX][touchY].occupier = -1L;
    }

    // Update playCards

    public void updateplayCards() {
        for(int i=0; i<6; i++) {
            String tokens[] = playCards[i].value.split(" ");
            int x = playCards[i].value.charAt(0) - '0';
            int y = playCards[i].value.charAt(1) - '0';
            if(tokens[0].length() == 3) {
                x = Integer.parseInt(tokens[0].substring(0,2));
                y = Integer.parseInt(tokens[0].substring(2,3));
            }
            playCards[i].setImage(BitmapFactory.decodeResource(getResources(),
                                    context.getResources().getIdentifier(
                                    "play_card_" + x + "_" + y, "drawable",
                                            context.getPackageName())));
        }
    }

    public void update() {

    }
}
