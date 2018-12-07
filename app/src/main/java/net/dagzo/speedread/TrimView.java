package net.dagzo.speedread;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class TrimView extends View {

    public float _x = 0, _y = 0;
    Paint paint1;
    Paint paint2;
    Paint paint3;

    class TouchMode {
        private static final int NONE = 0;
        private static final int MOVE = 1;
        private static final int SCALE_TOP = 2;
        private static final int SCALE_LEFT = 3;
        private static final int SCALE_BOTTOM = 4;
        private static final int SCALE_RIGHT = 5;
    }

    int touchMode = TouchMode.NONE;
    int padding = 20;
    int circleRadius = 12;

    int width = 0;
    int height = 0;

    int trimTop = 0;
    int trimLeft = 0;
    int trimBottom = 0;
    int trimRight = 0;

    public TrimView(Context context) {
        super(context);
        init();
    }

    public TrimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TrimView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        paint1 = new Paint();
        paint1.setColor(0xcc000000);
        paint1.setAntiAlias(true);

        paint2 = new Paint();
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setColor(Color.LTGRAY);

        paint3 = new Paint();
        paint3.setAntiAlias(true);
        paint3.setColor(Color.LTGRAY);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void sizeSet(int w, int h) {
        width = w;
        height = h;

        trimTop = padding;
        trimLeft = padding;
        trimBottom = height - padding;
        trimRight = width - padding;
    }

    public ArrayList<Integer> getTrimData() {
        ArrayList<Integer> _arl = new ArrayList<Integer>();
        _arl.add(trimLeft);
        _arl.add(trimTop);
        _arl.add(trimRight);
        _arl.add(trimBottom);
        return _arl;
    }

    protected void onDraw(Canvas canvas) {
        // トリミング領域の外の黒背景部分を描画
        canvas.drawRect(0, 0, width, trimTop, paint1);
        canvas.drawRect(0, trimTop, trimLeft, trimBottom, paint1);
        canvas.drawRect(trimRight, trimTop, width, trimBottom, paint1);
        canvas.drawRect(0, trimBottom, width, height, paint1);

        canvas.drawRect(trimLeft, trimTop, trimRight, trimBottom, paint2);

        int xHalf = (trimLeft + trimRight) / 2;
        int yHalf = (trimTop + trimBottom) / 2;
        canvas.drawCircle(xHalf, trimTop, circleRadius, paint3);
        canvas.drawCircle(xHalf, trimBottom, circleRadius, paint3);
        canvas.drawCircle(trimLeft, yHalf, circleRadius, paint3);
        canvas.drawCircle(trimRight, yHalf, circleRadius, paint3);
    }

    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                _x = e.getX();
                _y = e.getY();
                if (trimLeft + padding < _x && trimRight - padding > _x) {
                    if (trimTop + padding < _y && trimBottom - padding > _y) {
                        touchMode = TouchMode.MOVE;
                    } else if (trimTop - padding < _y && trimTop + padding > _y) {
                        touchMode = TouchMode.SCALE_TOP;
                    } else if (trimBottom - padding < _y && trimBottom + padding > _y) {
                        touchMode = TouchMode.SCALE_BOTTOM;
                    }
                } else if (trimTop + padding < _y && trimBottom - padding > _y) {
                    if (trimLeft - padding < _x && trimLeft + padding > _x) {
                        touchMode = TouchMode.SCALE_LEFT;
                    } else if (trimRight - padding < _x && trimRight + padding > _x) {
                        touchMode = TouchMode.SCALE_RIGHT;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int disX = (int) (e.getX() - _x);
                int disY = (int) (e.getY() - _y);

                if (touchMode == TouchMode.MOVE) {

                    // 画面外にはみ出さないようにする
                    int tempL = trimLeft + disX;
                    int tempR = trimRight + disX;
                    if (tempL >= 0 && tempR <= width) {
                        trimLeft = tempL;
                        trimRight = tempR;
                    }
                    int tempT = trimTop + disY;
                    int tempB = trimBottom + disY;
                    if (tempT >= 0 && tempB <= height) {
                        trimTop = tempT;
                        trimBottom = tempB;
                    }

                } else if (touchMode == TouchMode.SCALE_TOP) {
                    int tempT = trimTop + disY;
                    if (tempT >= 0 && tempT <= (trimBottom - padding)) {
                        trimTop = tempT;
                    }
                } else if (touchMode == TouchMode.SCALE_BOTTOM) {
                    int tempB = trimBottom + disY;
                    if (tempB >= (trimTop + padding) && tempB < height) {
                        trimBottom = tempB;
                    }
                } else if (touchMode == TouchMode.SCALE_LEFT) {
                    int tempL = trimLeft + disX;
                    if (tempL >= 0 && tempL <= (trimRight - padding)) {
                        trimLeft = tempL;
                    }
                } else if (touchMode == TouchMode.SCALE_RIGHT) {
                    int tempR = trimRight + disX;
                    if (tempR >= (trimLeft + padding) && tempR < width) {
                        trimRight = tempR;
                    }
                }
                _x = e.getX();
                _y = e.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchMode = TouchMode.NONE;
                break;
            default:
                break;
        }
        return true;
    }
}