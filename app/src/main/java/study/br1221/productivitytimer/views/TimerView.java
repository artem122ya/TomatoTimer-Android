package study.br1221.productivitytimer.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import study.br1221.productivitytimer.R;

/**
 * TODO: document your custom view class.
 */
public class TimerView extends View {
    int arcWidth = 40;
    Paint backgroundArcPaint, foregroundArcPaint;
    RectF arcRect;
    Path arcPath;



    public TimerView(Context context) {
        super(context);
        init(null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setColor(Color.RED);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);
        backgroundArcPaint.setStrokeWidth(arcWidth);

        foregroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundArcPaint.setColor(Color.GREEN);
        foregroundArcPaint.setStyle(Paint.Style.STROKE);
        foregroundArcPaint.setStrokeCap(Paint.Cap.ROUND);
        foregroundArcPaint.setStrokeWidth(arcWidth);

        arcRect = new RectF(0 + 100, 0 + 100, getWidth() - 100, getHeight() - 100);

        arcPath = new Path();
    }


    @Override
    protected void onDraw(Canvas canvas) {

        arcPath.arcTo(arcRect, 135, 270);
        canvas.drawPath(arcPath, backgroundArcPaint);

    }

    public void setTime(){

    }

}
