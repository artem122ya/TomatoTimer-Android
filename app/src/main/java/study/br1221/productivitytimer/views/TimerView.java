package study.br1221.productivitytimer.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import study.br1221.productivitytimer.R;


public class TimerView extends View {
    private int arcWidth;
    private Paint backgroundArcPaint, foregroundArcPaint, timerTextPaint;
    private RectF arcRect;
    private int viewWidth, viewHeight;

    private float anglePercentage = 1;

    private Path backgroundArcPath, foregroundArcPath;

    private Context context;

    private Rect viewRect = new Rect();




    public TimerView(Context context) {
        super(context);
        init(context,null);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Make view square
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        if (height > width) setMeasuredDimension(width, width);
        else setMeasuredDimension(height, height);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        createPaint();




        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimerView, 0, 0);
        try {
            applyAttrs(a);
        } catch (EnumConstantNotPresentException e){
            applyDefaultAttrs();
        }

    }

    private void createPaint(){
        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        foregroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundArcPaint.setStyle(Paint.Style.STROKE);
        foregroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        timerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timerTextPaint.setColor(Color.BLACK);

    }

    private void applyAttrs(TypedArray a){
        foregroundArcPaint.setColor(a.getColor(R.styleable.TimerView_foreground_arc_color, Color.GRAY));
        backgroundArcPaint.setColor(a.getColor(R.styleable.TimerView_background_arc_color, Color.RED));
    }

    private void applyDefaultAttrs(){
        backgroundArcPaint.setColor(Color.RED);
        foregroundArcPaint.setColor(Color.GREEN);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        createArcs();

        //draw both arcs
        canvas.drawPath(backgroundArcPath, backgroundArcPaint);
        canvas.drawPath(foregroundArcPath, foregroundArcPaint);

        drawTimeText(canvas);


    }


    private void drawTimeText(Canvas canvas){
        float pixelTextSize = viewWidth/ 6;
        timerTextPaint.setTextSize(pixelTextSize);
        drawCenter(canvas, timerTextPaint, "11:00");
    }


    private void drawCenter(Canvas canvas, Paint paint, String text) {
        canvas.getClipBounds(viewRect);
        int cHeight = viewRect.height();
        int cWidth = viewRect.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), viewRect);
        float x = cWidth / 2f - viewRect.width() / 2f - viewRect.left;
        float y = cHeight / 2f + viewRect.height() / 2f - viewRect.bottom;
        canvas.drawText(text, x, y, paint);
    }


    private void createArcs(){
        //get view width and height
        viewWidth = getWidth();
        viewHeight = getHeight();

        //create arc rect
        arcRect = new RectF(100, 100, viewWidth - 100, viewHeight - 100);

        //create background arc
        backgroundArcPath = new Path();
        backgroundArcPath.arcTo(arcRect, 135, 270);

        //create foreground arc
        foregroundArcPath = new Path();
        foregroundArcPath.arcTo(arcRect, 135, 270 * anglePercentage);

        //set dynamic arcWidth
        arcWidth = viewWidth / 22;
        backgroundArcPaint.setStrokeWidth(arcWidth*0.95f); // *0.95f so there is no antialiasing overlap
        foregroundArcPaint.setStrokeWidth(arcWidth);
    }

    public void setTime(int timeMillisTotal, int timeMillisLeft){
        anglePercentage = timeMillisLeft > 0 ? (float) timeMillisLeft / (float) timeMillisTotal : 0;
        invalidate();
    }

//    private static class TimerThread implements Runnable {
//
//        @Override
//        public void run() {
//
//
//        }
//        public void setTime(int timeMillisTotal, int timeMillisLeft){
//
//        }
//    }
}
