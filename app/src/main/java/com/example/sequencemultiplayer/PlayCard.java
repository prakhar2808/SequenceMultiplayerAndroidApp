package com.example.sequencemultiplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class PlayCard {

    public Bitmap image;
    public String value;
    public int x1, y1, x2, y2;
    int cardWidth, cardHeight;
    Paint opacity;

    public PlayCard(Bitmap image, int cardWidth, int cardHeight, String value) {
        this.cardHeight = cardHeight;
        this.cardWidth = cardWidth;
        this.image = Bitmap.createScaledBitmap(image, cardWidth, cardHeight, true);
        this.value = value;
        x1 = x2 = y1 = y2 = -1;
        opacity = new Paint();
        opacity.setAlpha(255);
    }

    public void setImage(Bitmap image) {
        this.image = Bitmap.createScaledBitmap(image, cardWidth, cardHeight, true);;
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
