package de.holoscope.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class ZoomableImageView extends View {
    private static final int INVALID_POINTER_ID = -1;
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 20.0f;

    private Drawable image;
    private float xPos;
    private float yPos;
    private float lastXPos;
    private float lastYPos;
    private int activePointerId = INVALID_POINTER_ID;

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;

    public ZoomableImageView(Context context) {
        this(context, null, 0);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setImage(Bitmap bmp) {
        image = new BitmapDrawable(getContext().getResources(), bmp);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // pass all events through ScaleGestureDetector
        scaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastXPos = ev.getX();
                lastYPos = ev.getY();
                activePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);

                if (!scaleDetector.isInProgress()) {
                    xPos += x - lastXPos;
                    yPos += y - lastYPos;

                    invalidate();
                }

                lastXPos = x;
                lastYPos = y;

                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int idx = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                int pointerId = ev.getPointerId(idx);
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = idx == 0 ? 1 : 0;
                    lastXPos = ev.getX(newPointerIndex);
                    lastYPos = ev.getY(newPointerIndex);
                    activePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            case MotionEvent.ACTION_UP:
                activePointerId = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_CANCEL:
                activePointerId = INVALID_POINTER_ID;
                break;
        }

        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (image != null) {
            canvas.save();
            canvas.translate(xPos, yPos);
            canvas.scale(scaleFactor, scaleFactor);
            canvas.translate(-image.getIntrinsicWidth()/2, -image.getIntrinsicHeight()/2);
            image.draw(canvas);
            canvas.restore();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

            invalidate();
            return true;
        }
    }
}
