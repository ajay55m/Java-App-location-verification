package com.app.fourscontracting;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

public class BlueprintGridDrawable extends Drawable {
    private final Paint paint = new Paint();
    private final int gridSize = 35; // Size of grid square in pixels (finer pattern)

    public BlueprintGridDrawable() {
        this(8); // Default 3% opacity
    }

    public BlueprintGridDrawable(int alpha) {
        paint.setColor(Color.parseColor("#E2E8F0")); 
        paint.setAlpha(alpha); 
        paint.setStrokeWidth(1.0f);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        // Solid white or very light grey background
        canvas.drawColor(Color.parseColor("#F8FAFC"));

        int width = getBounds().width();
        int height = getBounds().height();

        // Draw vertical lines
        for (int x = 0; x < width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, paint);
        }

        // Draw horizontal lines
        for (int y = 0; y < height; y += gridSize) {
            canvas.drawLine(0, y, width, y, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
