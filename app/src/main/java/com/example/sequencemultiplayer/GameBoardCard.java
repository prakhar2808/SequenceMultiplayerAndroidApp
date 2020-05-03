package com.example.sequencemultiplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class GameBoardCard {

    private Bitmap image;

    // The image is from (x1,y1) to (x2,y2) --> Top left to Bottom Right
    public int x1, y1, x2, y2;

    // Is occupied or not?
    boolean isOccupied;

    // If occupied, then occupied by? -1 if unoccupied.
    Long occupier;

    // Is a part of a sequence?
    boolean isInSequence;

    // RGB values for blocks.
    int R[];
    int G[];
    int B[];

    // Opacity of the tile
    Paint opacity;

    public GameBoardCard(Bitmap image, int cardWidth, int cardHeight) {
        this.image = Bitmap.createScaledBitmap(image, cardWidth, cardHeight, true);
        x1 = x2 = y1 = y2 = -1;
        isOccupied = false;
        occupier = -1L;
        isInSequence = false;
        R = new int[3];
        G = new int[3];
        B = new int[3];
        R[0] = 150; R[1] = 50; R[2] = 40;
        G[0] = 0; G[1] = 120; G[2] = 0;
        B[0] = 50; B[1] = 50; B[2] = 180;
        opacity = new Paint();
        opacity.setAlpha(255);
    }

    public void setCoordinates(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public void setOpacity(int op) {
        opacity.setAlpha(op);
    }

    public void draw(Canvas canvas, int left, int top) {
        canvas.drawBitmap(image, left, top, opacity);
    }
}
