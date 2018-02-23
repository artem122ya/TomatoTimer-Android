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


import artem122ya.tomatotimer.R;
import artem122ya.tomatotimer.TimerService.TimerState;

import static artem122ya.tomatotimer.utils.Utils.getTimeString;


public class TimerView extends View {
    private Paint backgroundArcPaint, foregroundArcPaint, timerTextPaint;
    private int viewWidth, viewHeight;
    private Path backgroundArcPath, foregroundArcPath;
    private Rect viewRect = new Rect();

    private int initialArcSweepAngle = 270;
    private int initialArcStartAngle = 135;
    private volatile float currentArcSweepAngle = initialArcSweepAngle;

    private ValueAnimator timerAnimator, drawAnimator;
    boolean animatingTimer = false;
    int animationDrawDuration = 300;

    private String displayedTime = "00:00";
    private volatile TimerState currentTimerState = TimerState.STOPPED;
    private boolean firstDraw = true;


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

        TypedArray a = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.TimerView, 0, 0);
        applyAttrs(a);
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
        timerTextPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Roboto-Regular.ttf"));

    }


    private void applyAttrs(TypedArray a){
        foregroundArcPaint.setColor(a.getColor(R.styleable.TimerView_colorTimerViewForegroundArc, Color.GRAY));
        backgroundArcPaint.setColor(a.getColor(R.styleable.TimerView_colorTimerViewBackgroundArc, Color.RED));
        timerTextPaint.setColor(a.getColor(R.styleable.TimerView_colorTimerViewText, Color.BLACK));
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
        drawStringInCenterOfView(canvas, timerTextPaint, displayedTime);
    }


    private void drawStringInCenterOfView(Canvas canvas, Paint paint, String text) {
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
        int viewMarginPixels = viewWidth / 14;

        //create arc rect with 100px margin
        RectF arcRect = new RectF(viewMarginPixels, viewMarginPixels,
                viewWidth - viewMarginPixels, viewHeight - viewMarginPixels);

        //create background arc
        backgroundArcPath = new Path();
        backgroundArcPath.arcTo(arcRect, initialArcStartAngle, initialArcSweepAngle);

        //create foreground arc
        foregroundArcPath = new Path();
        foregroundArcPath.arcTo(arcRect, initialArcStartAngle, currentArcSweepAngle);

        //set arcWidth
        int arcWidth = viewWidth / 22;
        backgroundArcPaint.setStrokeWidth(arcWidth *0.95f); // *0.95f so there is no antialiasing overlap
        foregroundArcPaint.setStrokeWidth(arcWidth);
    }


    private void setCurrentArcSweepAngleAndInvalidate(float angle){
        currentArcSweepAngle = angle;
        postInvalidate();
    }


    private void updateTimer(int timeMillisTotal, int timeMillisLeft){
        if (isAnimationEnabled()) {
            animateWidget(timeMillisTotal,timeMillisLeft);
        } else setCurrentArcSweepAngleAndInvalidate(getNewSweepAngle(timeMillisTotal, timeMillisLeft));
        
        setDisplayedTime(getTimeString(timeMillisLeft));

    }


    private void animateWidget(int timeMillisTotal, int timeMillisLeft){
        switch (currentTimerState) {
            case STARTED:
                drawWhenStarted(timeMillisTotal, timeMillisLeft);
                break;
            case PAUSED:
            case STOPPED:
                drawWhenStopped(timeMillisTotal, timeMillisLeft);
                break;
        }
    }


    private boolean isAnimationEnabled(){
        return getAnimationDurationScale() != 0;
    }


    private void drawWhenStarted(int timeMillisTotal, int timeMillisLeft){
        if (!animatingTimer){
            int scaledDrawDurationMillis =(int) (animationDrawDuration / getAnimationDurationScale());
            timeMillisLeft = timeMillisLeft <= 0 ? 0 : timeMillisLeft - scaledDrawDurationMillis;
            float newSweepAngle = getNewSweepAngle(timeMillisTotal, timeMillisLeft);
            startDrawingAnimation(scaledDrawDurationMillis, newSweepAngle);
            startAnimatingTimer(timeMillisLeft, scaledDrawDurationMillis, newSweepAngle);
            animatingTimer = true;
        }
    }


    private void drawWhenStopped(int timeMillisTotal, int timeMillisLeft){
        if (animatingTimer){
            stopAnimation();
        }
        float newSweepAngle = getNewSweepAngle(timeMillisTotal, timeMillisLeft);
        startDrawingAnimation((int) (animationDrawDuration / getAnimationDurationScale()), newSweepAngle);
    }


    private float getNewSweepAngle(int millisTotal, int millisLeft){
        float anglePercentage = millisLeft > 0 ? (float) millisLeft / (float) millisTotal : 0;
        return initialArcSweepAngle * anglePercentage;
    }


    private void startDrawingAnimation(int durationMillis, float destinationAngle){
        if (firstDraw) {
            setCurrentArcSweepAngleAndInvalidate(destinationAngle);
            firstDraw = false;
        } else {
            drawAnimator = ValueAnimator.ofFloat(currentArcSweepAngle, destinationAngle);
            drawAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    float animatedValue = (float) updatedAnimation.getAnimatedValue();
                    setCurrentArcSweepAngleAndInvalidate(animatedValue);
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


    private void startAnimatingTimer(int durationMillis, int delayMillis, float startingAngle){
        // animator that animates timer itself
        timerAnimator = ValueAnimator.ofFloat(startingAngle, 0);
        timerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float)updatedAnimation.getAnimatedValue();
                setCurrentArcSweepAngleAndInvalidate(animatedValue);
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


    private void setDisplayedTime(String currentTime){
        displayedTime = currentTime;
    }



    public void onTimerStarted(int millisTotal, int millisLeft){
        currentTimerState = TimerState.STARTED;
        updateTimer(millisTotal, millisLeft);
    }


    public void onTimerPaused(int millisTotal, int millisLeft){
        currentTimerState = TimerState.PAUSED;
        updateTimer(millisTotal, millisLeft);

    }


    public void onTimerStopped(int millisTotal, int millisLeft){
        currentTimerState = TimerState.STOPPED;
        updateTimer(millisTotal, millisLeft);

    }


    public void onTimerUpdate(int millisTotal, int millisLeft){
        updateTimer(millisTotal, millisLeft);
    }


    public float getCurrentArcSweepAngle() {
        return currentArcSweepAngle;
    }

    public String getDisplayedTime(){
        return displayedTime;
    }

}
