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
import android.util.Log;
import android.view.View;

import study.br1221.productivitytimer.R;

/**
 * TODO: document your custom view class.
 */
public class TimerView extends View {
    private int arcWidth = 40;
    private Paint backgroundArcPaint, foregroundArcPaint;
    private RectF arcRect;
    private int timerAngle = 0;



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





    }


    @Override
    protected void onDraw(Canvas canvas) {

        arcRect = new RectF(100, 100, getWidth() - 100, getHeight() - 100);
        Path arcPath = new Path();
        arcPath.arcTo(arcRect, 135, 270);
        canvas.drawPath(arcPath, backgroundArcPaint);

        Path timerArcPath = new Path();
        timerArcPath.arcTo(arcRect, 135, timerAngle);
        canvas.drawPath(timerArcPath, foregroundArcPaint);

    }

    public void setTime(double millis){
        timerAngle =(int) (270 * (100 / (60000/millis) / 100));
        invalidate();


    }
}
