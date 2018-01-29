package artem122ya.tomatotimer.views;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.TimeUnit;

import artem122ya.tomatotimer.R;



public class TimerView extends View {
    private int arcWidth;
    private Paint backgroundArcPaint, foregroundArcPaint, timerTextPaint;
    private RectF arcRect;
    private int viewWidth, viewHeight;

    private float anglePercentage = 1;

    private Path backgroundArcPath, foregroundArcPath;


    private Rect viewRect = new Rect();

    private int initialArcSweepAngle = 270;
    private int initialArcStartAngle = 135;

    private float arcSweepAngle = 0;

    private ValueAnimator timerAnimator, drawAnimator;
    boolean animatingTimer = false;
    int animationDrawDuration = 300;

    private String currentTimerString = "00:00";

    private enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;

    private boolean firstDraw = true;

    private int viewMarginPixels;



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
        //backgroundArcPaint.setShadowLayer(2, 0.0f, 2.0f, 0x38212121); ------------shadow

        foregroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundArcPaint.setStyle(Paint.Style.STROKE);
        foregroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        timerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timerTextPaint.setColor(Color.BLACK);
        timerTextPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Roboto-Regular.ttf"));

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
        drawCenter(canvas, timerTextPaint, currentTimerString);
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

        //create margin
        viewMarginPixels = viewWidth / 14;

        //create arc rect with 100px margin
        arcRect = new RectF(viewMarginPixels, viewMarginPixels, viewWidth - viewMarginPixels, viewHeight - viewMarginPixels);

        //create background arc
        backgroundArcPath = new Path();
        backgroundArcPath.arcTo(arcRect, initialArcStartAngle, initialArcSweepAngle);

        //create foreground arc
        foregroundArcPath = new Path();
        foregroundArcPath.arcTo(arcRect, initialArcStartAngle, arcSweepAngle);

        //set arcWidth
        arcWidth = viewWidth / 22;
        backgroundArcPaint.setStrokeWidth(arcWidth*0.95f); // *0.95f so there is no antialiasing overlap
        foregroundArcPaint.setStrokeWidth(arcWidth);
    }

    private void setArcSweepAngle(float angle){
        arcSweepAngle = angle;
        postInvalidate();
    }


    public void setTime(int timeMillisTotal, int timeMillisLeft){
        if (isAnimationEnabled()) {
            switch (timerState) {
                case STARTED:
                    drawWhenStarted(timeMillisTotal, timeMillisLeft);
                    break;
                case PAUSED:
                case STOPPED:
                    drawWhenStopped(timeMillisTotal, timeMillisLeft);
                    break;
            }
        } else setArcSweepAngle(getNewSweepAngle(timeMillisTotal, timeMillisLeft));
        setCurrentTimerString(getTimeString(timeMillisLeft));

    }


    private boolean isAnimationEnabled(){
        return getAnimationDurationScale() != 0;
    }



    private void drawWhenStarted(int timeMillisTotal, int timeMillisLeft){
        if (!animatingTimer){
            int scaledDrawDurationMillis =(int) (animationDrawDuration / getAnimationDurationScale());
            timeMillisLeft = timeMillisLeft <= 0 ? 0 : timeMillisLeft - scaledDrawDurationMillis;
            float newSweepAngle = getNewSweepAngle(timeMillisTotal, timeMillisLeft);
            animateDrawArc(scaledDrawDurationMillis, newSweepAngle);
            animateArc(timeMillisLeft, scaledDrawDurationMillis, newSweepAngle);
            animatingTimer = true;
        }
    }


    private void drawWhenStopped(int timeMillisTotal, int timeMillisLeft){
        if (animatingTimer){
            stopAnimation();
        }
        float newSweepAngle = getNewSweepAngle(timeMillisTotal, timeMillisLeft);
        animateDrawArc((int) (animationDrawDuration / getAnimationDurationScale()), newSweepAngle);
    }


    private float getNewSweepAngle(int millisTotal, int millisLeft){
        anglePercentage = millisLeft > 0 ? (float) millisLeft / (float) millisTotal : 0;
        return initialArcSweepAngle * anglePercentage;
    }


    private void animateDrawArc(int durationMillis, float destinationAngle){
        if (firstDraw) {
            setArcSweepAngle(destinationAngle);
            firstDraw = false;
        } else {
            drawAnimator = ValueAnimator.ofFloat(arcSweepAngle, destinationAngle);
            drawAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    float animatedValue = (float) updatedAnimation.getAnimatedValue();
                    setArcSweepAngle(animatedValue);
                }
            });
            drawAnimator.setInterpolator(new TimeInterpolator() {
                @Override
                public float getInterpolation(float v) {
                    return v;
                }
            });
            drawAnimator.setDuration(durationMillis);
            drawAnimator.start();
        }
    }

    private void animateArc(int durationMillis, int delayMillis, float startingAngle){
        // animator that animates timer itself
        timerAnimator = ValueAnimator.ofFloat(startingAngle, 0);
        timerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float)updatedAnimation.getAnimatedValue();
                setArcSweepAngle(animatedValue);
            }
        });
        timerAnimator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float v) {
                return v;
            }
        });
        timerAnimator.setDuration((long) (durationMillis / getAnimationDurationScale()));
        timerAnimator.setStartDelay(delayMillis);
        timerAnimator.start();
    }

    public float getAnimationDurationScale(){
        return Settings.Global.getFloat(
                getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1);
    }



    public void stopAnimation(){
       if (timerAnimator != null) timerAnimator.cancel();
        animatingTimer = false;
    }

    private void setCurrentTimerString(String currentTime){
        currentTimerString = currentTime;
    }


    private String getTimeString(int millis){
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d",
                minutes,TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes));
    }


    public void timerStarted(int millisTotal, int millisLeft){
        timerState = TimerState.STARTED;
        setTime(millisTotal, millisLeft);
    }


    public void timerPaused(int millisTotal, int millisLeft){
        timerState = TimerState.PAUSED;
        setTime(millisTotal, millisLeft);

    }

    public void timerStopped(int millisTotal, int millisLeft){
        timerState = TimerState.STOPPED;
        setTime(millisTotal, millisLeft);

    }

    public void updateTimer(int millisTotal, int millisLeft){
        setTime(millisTotal, millisLeft);
    }


}
