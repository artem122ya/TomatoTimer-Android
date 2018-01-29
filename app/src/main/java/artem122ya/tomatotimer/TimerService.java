package artem122ya.tomatotimer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.TimeUnit;

public class TimerService extends Service {

    public static String ACTION_SEND_TIME = "time_send";
    public static String INT_TIME_MILLIS_LEFT = "time_extra_millis_left";
    public static String INT_TIME_MILLIS_TOTAL = "time_extra_millis_total";
    public static String ENUM_TIMER_STATE = "timer_state";


    public enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;


    private Thread timerThread;
    private final Object timerThreadLock = new Object();

    int timerNotificationId = 135001;

    private FocusTimerActionReceiver actionReceiver = new FocusTimerActionReceiver();

    private final IBinder iBinder = new LocalBinder();

    public static TimerService thisTimerService;



    public enum PeriodState {FOCUS, BREAK, BIG_BREAK, NOT_INITIALIZED}
    public volatile PeriodState currentPeriod = PeriodState.NOT_INITIALIZED;
    public int consecutiveFocusPeriod = 1;
    private int periodsUntilBreak = 3;

    private volatile int timeMillisLeft = 0;
    private volatile long timeMillisStarted = 0;
    private volatile int totalMillis = 0;
    private volatile long stopTimeMillis = 0;


    private static String startActionIntentString = "com.example.app.ACTION_TIMER_START";
    private static String pauseActionIntentString = "com.example.app.ACTION_TIMER_PAUSE";
    private static String stopActionIntentString = "com.example.app.ACTION_TIMER_STOP";
    private static String skipActionIntentString = "com.example.app.ACTION_TIMER_SKIP";

    private PendingIntent startActionIntent, stopActionIntent, pauseActionIntent,
            openMainActivityIntent, skipActionIntent;



    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }



    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        thisTimerService = this;

        createIntentFilter();
        initControlIntents();
        checkNumberOfSessionsUntilBreak();

        return START_NOT_STICKY;
    }

    private void createIntentFilter(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(startActionIntentString);
        intentFilter.addAction(pauseActionIntentString);
        intentFilter.addAction(stopActionIntentString);
        intentFilter.addAction(skipActionIntentString);
        registerReceiver(actionReceiver, intentFilter);
    }

    private void initControlIntents(){
        Intent intentMainActivity = new Intent(this, MainActivity.class);
        intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openMainActivityIntent = PendingIntent.getActivity(this, 1, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent(startActionIntentString);
        startActionIntent = PendingIntent.getBroadcast(this, 100, startIntent, 0);

        Intent pauseIntent = new Intent(pauseActionIntentString);
        pauseActionIntent = PendingIntent.getBroadcast(this, 100, pauseIntent, 0);

        Intent stopIntent = new Intent(stopActionIntentString);
        stopActionIntent = PendingIntent.getBroadcast(this, 100, stopIntent, 0);

        Intent skipIntent = new Intent(skipActionIntentString);
        skipActionIntent = PendingIntent.getBroadcast(this, 100, skipIntent, 0);

    }

    @Override
    public void onDestroy() {
        stopTimer();
        try {
            unregisterReceiver(actionReceiver);
        } catch (IllegalArgumentException e){

        }
    }

    public void startTimer(){

        thisTimerService = this;

        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                moveToNextPeriod();
                checkNumberOfSessionsUntilBreak();
                timeMillisStarted = System.currentTimeMillis();
                totalMillis = getTimeLeftMillis(currentPeriod);
                timeMillisLeft = totalMillis;
                stopTimeMillis = timeMillisStarted + totalMillis;

                timerThread = new Thread(new TimerRunnable());
                timerState = TimerState.STARTED;
                timerThread.start();
            } else if (timerState == TimerState.PAUSED) {
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }


    private void moveToNextPeriod(){
        currentPeriod = getNextPeriod();
        if (currentPeriod == PeriodState.FOCUS){
            consecutiveFocusPeriod++;
        } else if (currentPeriod == PeriodState.BIG_BREAK){
            consecutiveFocusPeriod = 1;
        }
    }


    private void checkNumberOfSessionsUntilBreak(){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        periodsUntilBreak = sharedPrefs.getInt("sessions_until_big_break",
                Integer.valueOf(getString(R.string.sessions_until_big_break_default_value)) + 1);
    }




    public void pauseTimer() {
        synchronized (timerThreadLock) {
            if (timerState == TimerState.STARTED) {
                timerState = TimerState.PAUSED;
                timerThreadLock.notifyAll();
                sendTime(totalMillis, timeMillisLeft);
            }
        }
    }

    public void stopTimer() {

        if (timerState != TimerState.STOPPED) {
            synchronized (timerThreadLock) {
                timerState = TimerState.STOPPED;
                timerThreadLock.notifyAll();
            }

            sendTime(getTimeLeftMillis(getNextPeriod()), getTimeLeftMillis(getNextPeriod()));
        }
    }


    public void onStopButtonClick(){
        currentPeriod = PeriodState.NOT_INITIALIZED;
        consecutiveFocusPeriod = 1;
        stopTimer();
        stopForeground(true);
    }

    public void onStartPauseButtonClick(){
        if(timerState == TimerState.STARTED) pauseTimer();
        else startTimer();
    }

    public void onSkipButtonClick(){
        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                moveToNextPeriod();
                sendTime(getTimeLeftMillis(getNextPeriod()), getTimeLeftMillis(getNextPeriod()));
            } else {
                stopTimer();
            }

        }
    }


    public PeriodState getNextPeriod(){
        switch (currentPeriod){
            case FOCUS:
                if (periodsUntilBreak != 0 && consecutiveFocusPeriod >= periodsUntilBreak) return PeriodState.BIG_BREAK;
                else return PeriodState.BREAK;
            case NOT_INITIALIZED:
            case BREAK:
            case BIG_BREAK:
                return PeriodState.FOCUS;
        }
        return PeriodState.NOT_INITIALIZED;
    }

    public int getTimeLeftMillis(PeriodState period){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int timeLeft = 0;
        switch (period){
            case NOT_INITIALIZED:
            case FOCUS:
                timeLeft = sharedPrefs.getInt("focus_time_minutes",
                        Integer.valueOf(getString(R.string.focus_time_minutes_default_value)) );
                break;
            case BREAK:
                timeLeft = sharedPrefs.getInt("small_break_time_minutes",
                        Integer.valueOf(getString(R.string.small_break_time_minutes_default_value)));
                break;
            case BIG_BREAK:
                timeLeft = sharedPrefs.getInt("big_break_period_minutes",
                        Integer.valueOf(getString(R.string.big_break_time_minutes_default_value)));
                break;
        }
        return timeLeft*60000;

    }




    private void sendTime(int totalMillis, int millisLeft){
        /* Method sends total time and time left in milliseconds */
        Intent intent = new Intent(ACTION_SEND_TIME);
        intent.putExtra(ENUM_TIMER_STATE, timerState);
        intent.putExtra(INT_TIME_MILLIS_LEFT, millisLeft);
        intent.putExtra(INT_TIME_MILLIS_TOTAL, totalMillis);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public int getMillisLeft(){
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(getNextPeriod());
        } else return timeMillisLeft;
    }

    public int getMillisTotal(){
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(getNextPeriod());
        } else return totalMillis;
    }


    public TimerState getCurrentTimerState(){
        return timerState;
    }


    private void timerRunningNotification(){
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_pause_black_24dp,
                                getString(R.string.pause_notification_action_title), pauseActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setProgress(totalMillis, timeMillisLeft, false)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        startForeground(timerNotificationId, builder.build());
    }

    private void timerPausedNotification(){
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                getString(R.string.resume_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        startForeground(timerNotificationId, builder.build());
    }

    private void timerFinishedNotification(){
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod) + getString(R.string.finished_notification_title))
                        .setContentText(getString(R.string.finished_notification_text) + getSessionName(getNextPeriod()) + "?")
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                getString(R.string.start_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        startForeground(timerNotificationId, builder.build());
    }

    private String getSessionName(PeriodState period){
        switch(period){
            case FOCUS: return getString(R.string.work_time_notification_text);
            case BIG_BREAK: return getString(R.string.big_break_notification_text);
            case BREAK: return getString(R.string.break_time_notification_text);
        }
        return "";
    }




    private String getTimeString(int millis){
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d",
                minutes,TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes));
    }


    private class TimerRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (timerThreadLock) {
                while (timerState == TimerState.STARTED) {

                    timeMillisLeft = (int)(stopTimeMillis - System.currentTimeMillis());

                    sendTime(totalMillis, timeMillisLeft);
                    timerRunningNotification();
                    if(timeMillisLeft <= 0){
                        stopTimer();
                        timerFinishedNotification();
                    }

                    try {
                        timerThreadLock.wait(995);
                    } catch (InterruptedException e) {

                    }
                    while (timerState == TimerState.PAUSED) {
                        timerPausedNotification();
                        try {
                            timerThreadLock.wait();
                        } catch (InterruptedException e) {

                        }
                        stopTimeMillis = System.currentTimeMillis() + timeMillisLeft;
                    }
                }
            }
        }
    }


    public static class FocusTimerActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equalsIgnoreCase(startActionIntentString)){
                if(thisTimerService != null) thisTimerService.startTimer();
            } else if(action.equalsIgnoreCase(pauseActionIntentString)){
                if(thisTimerService != null) thisTimerService.pauseTimer();
            } else if(action.equalsIgnoreCase(stopActionIntentString)){
                if(thisTimerService != null) thisTimerService.onStopButtonClick();
            } else if(action.equalsIgnoreCase(skipActionIntentString)){
                if(thisTimerService != null) {
                    thisTimerService.onSkipButtonClick();
                    thisTimerService.startTimer();
                }
            }
        }
    }
}