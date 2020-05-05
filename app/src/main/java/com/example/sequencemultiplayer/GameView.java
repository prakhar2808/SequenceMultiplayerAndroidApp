package com.example.sequencemultiplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


public class GameView extends SurfaceView implements android.view.SurfaceHolder.Callback{

    private MainThread thread;
    private int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    private int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    GameBoardCard[][] cards;
    PlayCard[] playCards;
    Context context;

    int lastCardModified;
    Paint lastCardModifiedMarker;

    Paint sequenceLinePaint;

    TextView whoseTurn;
    LinearLayout textViewlayout;

    GradientDrawable border;


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
        // Setting this as -1
        lastCardModified = -1;
        lastCardModifiedMarker = new Paint();
        lastCardModifiedMarker.setColor(Color.YELLOW);

        // Paint to draw a line on a sequence
        sequenceLinePaint = new Paint();
        sequenceLinePaint.setColor(Color.BLACK);
        sequenceLinePaint.setStrokeWidth(10);

        //Drawing whose turn it is
        textViewlayout = new LinearLayout(context);
        whoseTurn = new TextView(context);
        whoseTurn.setVisibility(View.VISIBLE);
        whoseTurn.setText("Other's Turn");
        whoseTurn.setTextSize(18);
        whoseTurn.setTextColor(Color.BLACK);
        textViewlayout.addView(whoseTurn);
        border = new GradientDrawable();
        border.setColor(Color.RED); //white background
        border.setStroke(1, Color.RED); //black border with full opacity
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            textViewlayout.setBackgroundDrawable(border);
        } else {
            textViewlayout.setBackground(border);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.rgb(60, 110, 100));
            drawBoardCards(canvas);
        }
    }

    // Draw board cards
    void drawBoardCards(Canvas canvas) {
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
                cards[i][j].cx = (left+right)/2;
                cards[i][j].cy = (top+bottom)/2;
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

                if(lastCardModified == i*10+j) {
                    int cx = (left+right)/2;
                    int cy = (top+bottom)/2;
                    canvas.drawRect(cx-10, cy-10, cx+10, cy+10, lastCardModifiedMarker);
                }

                markIsSequence(canvas);
            }
        }
        drawPlayCards(canvas, leftBorder, cardWidth, paddingBetweenCardsHorizontal, paddingBetweenCardsVertical);
    }

    // Draw play cards
    void drawPlayCards(Canvas canvas,
                       int leftBorder,
                       int cardWidth,
                       int paddingBetweenCardsHorizontal,
                       int paddingBetweenCardsVertical) {
        leftBorder = leftBorder + (cardWidth + paddingBetweenCardsHorizontal)*4;
        int topBorder = screenHeight*3/4 + paddingBetweenCardsVertical*5;
        int rightBorder = screenWidth*25/26;
        int bottomBorder = screenHeight*49/50;
        paddingBetweenCardsHorizontal = (rightBorder - leftBorder)/(4*2);
        paddingBetweenCardsVertical = (bottomBorder - topBorder)/15;
        cardWidth = (rightBorder - leftBorder - paddingBetweenCardsHorizontal*2)/3;
        int cardHeight = (bottomBorder - topBorder - paddingBetweenCardsVertical)/2;

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
        drawWhoseTurn(canvas, leftBorder, paddingBetweenCardsHorizontal, bottomBorder, topBorder, cardHeight, paddingBetweenCardsVertical);
    }

    // Write whose turn
    private void drawWhoseTurn(Canvas canvas,
                               int leftBorder,
                               int paddingBetweenCardsHorizontal,
                               int bottomBorder,
                               int topBorder,
                               int cardHeight,
                               int paddingBetweenCardsVertical) {

        textViewlayout.measure(leftBorder - paddingBetweenCardsHorizontal,
                bottomBorder - topBorder - cardHeight - paddingBetweenCardsVertical);
        textViewlayout.layout(0, 0, leftBorder - paddingBetweenCardsHorizontal,
                bottomBorder - topBorder - cardHeight - paddingBetweenCardsVertical*3/2);

        // To place the text view somewhere specific:
        canvas.translate(screenWidth/26,topBorder);

        textViewlayout.draw(canvas);
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
        lastCardModified = touchX*10 + touchY;
    }

    // Update to mark (touchX, touchY) as unoccupied.
    public void updateBoardRemove(int touchX, int touchY) {
        cards[touchX][touchY].isOccupied = false;
        cards[touchX][touchY].occupier = -1L;
        lastCardModified = touchX*10 + touchY;
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

    // Draw a line on a sequence
    private void markIsSequence(Canvas canvas) {
        // Checking all rows for 5 length sequence
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j + 4 < 10; j++) {
                Long occupier = cards[i][j].occupier;
                if(occupier == -1) {
                    continue;
                }
                if(cards[i][j].occupier == occupier
                        && cards[i][j + 1].occupier == occupier
                        && cards[i][j + 2].occupier == occupier
                        && cards[i][j + 3].occupier == occupier
                        && cards[i][j + 4].occupier == occupier) {
                    canvas.drawLine(cards[i][j].cx,
                                    cards[i][j].cy,
                                    cards[i][j+4].cx,
                                    cards[i][j+4].cy,
                                    sequenceLinePaint);
                }
            }
        }


        // Checking all columns for 5 length sequence
        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Long occupier = cards[i][j].occupier;
                if(occupier == -1) {
                    continue;
                }
                if (cards[i][j].occupier == occupier
                        && cards[i + 1][j].occupier == occupier
                        && cards[i + 2][j].occupier == occupier
                        && cards[i + 3][j].occupier == occupier
                        && cards[i + 4][j].occupier == occupier) {
                    canvas.drawLine(cards[i][j].cx,
                            cards[i][j].cy,
                            cards[i+4][j].cx,
                            cards[i+4][j].cy,
                            sequenceLinePaint);
                }
            }
        }

        // Checking all diagonals for length 5 sequence
        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 0; j + 4 < 10; j++) {
                Long occupier = cards[i][j].occupier;
                if(occupier == -1) {
                    continue;
                }
                if (cards[i][j].occupier == occupier
                        && cards[i + 1][j + 1].occupier == occupier
                        && cards[i + 2][j + 2].occupier == occupier
                        && cards[i + 3][j + 3].occupier == occupier
                        && cards[i + 4][j + 4].occupier == occupier) {
                    canvas.drawLine(cards[i][j].cx,
                            cards[i][j].cy,
                            cards[i+4][j+4].cx,
                            cards[i+4][j+4].cy,
                            sequenceLinePaint);
                }
            }
        }

        for(int i = 0; i + 4 < 10; i++) {
            for (int j = 9; j - 4 >= 0; j--) {
                Long occupier = cards[i][j].occupier;
                if(occupier == -1) {
                    continue;
                }
                if (cards[i][j].occupier == occupier
                        && cards[i + 1][j - 1].occupier == occupier
                        && cards[i + 2][j - 2].occupier == occupier
                        && cards[i + 3][j - 3].occupier == occupier
                        && cards[i + 4][j - 4].occupier == occupier) {
                    canvas.drawLine(cards[i][j].cx,
                            cards[i][j].cy,
                            cards[i+4][j-4].cx,
                            cards[i+4][j-4].cy,
                            sequenceLinePaint);
                }
            }
        }

        // Now special cases with 4 length sequences.
        // Horizontal
        for(int i = 0; i < 10; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                Long occupier = cards[i][j].occupier;
                if(occupier == -1) {
                    continue;
                }
                if (cards[i][j].occupier == occupier
                        && cards[i][j + 1].occupier == occupier
                        && cards[i][j + 2].occupier == occupier
                        && cards[i][j + 3].occupier == occupier) {
                    canvas.drawLine(cards[i][j].cx,
                            cards[i][j].cy,
                            cards[i][j+3].cx,
                            cards[i][j+3].cy,
                            sequenceLinePaint);
                }
            }
        }
        // Vertical
        for(int i = 0; i < 10; i += 9) {
            for(int j = 1; j < 10; j += 4) {
                Long occupier = cards[j][i].occupier;
                if(occupier == -1) {
                    continue;
                }
                if (cards[j][i].occupier == occupier
                        && cards[j + 1][i].occupier == occupier
                        && cards[j + 2][i].occupier == occupier
                        && cards[j + 3][i].occupier == occupier) {
                    canvas.drawLine(cards[j][i].cx,
                            cards[j][i].cy,
                            cards[j+3][i].cx,
                            cards[j+3][i].cy,
                            sequenceLinePaint);
                }
            }
        }
        // 11 22 33 44, 18 27 36 45, 55 66 77 88, 54 63 72 81
        for(int i = 1; i < 6; i += 4) {
            Long occupier = cards[i][i].occupier;
            if (occupier != -1 && cards[i][i].occupier == occupier
                    && cards[i + 1][i + 1].occupier == occupier
                    && cards[i + 2][i + 2].occupier == occupier
                    && cards[i + 3][i + 3].occupier == occupier) {
                canvas.drawLine(cards[i][i].cx,
                        cards[i][i].cy,
                        cards[i+3][i+3].cx,
                        cards[i+3][i+3].cy,
                        sequenceLinePaint);
            }
            occupier = cards[i][9-i].occupier;
            if (occupier != -1 && cards[i][9 - i].occupier == occupier
                    && cards[i + 1][9 - i - 1].occupier == occupier
                    && cards[i + 2][9 - i - 2].occupier == occupier
                    && cards[i + 3][9 - i - 3].occupier == occupier) {
                canvas.drawLine(cards[i][9-i].cx,
                        cards[i][9-i].cy,
                        cards[i+3][9-i-3].cx,
                        cards[i+3][9-i-3].cy,
                        sequenceLinePaint);
            }
        }
    }

    public void update() {

    }
}
