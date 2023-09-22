package artem122ya.tomatotimer.timer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import artem122ya.tomatotimer.R;

import static artem122ya.tomatotimer.utils.Utils.getTimeString;

public class TimerService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_FOREGROUND_PERMISSION = 200;
    public static String ACTION_SEND_TIME = "artem122ya.tomatotimer.time_send";
    public static String INT_TIME_MILLIS_LEFT = "artem122ya.tomatotimer.time_extra_millis_left";
    public static String INT_TIME_MILLIS_TOTAL = "artem122ya.tomatotimer.time_extra_millis_total";
    public static String ENUM_TIMER_STATE = "artem122ya.tomatotimer.timer_state";


    public enum TimerState {STARTED, PAUSED, STOPPED}
    private volatile TimerState timerState = TimerState.STOPPED;


    private Thread timerThread;
    private final Object timerThreadLock = new Object();

    int timerNotificationId = 135001;
    String timerControlsNotificationChannelId = "artem122ya.tomatotimer.timer_controls";
    String timerFinishedNotificationChannelId = "artem122ya.tomatotimer.timer_finished";


    private WorkTimerActionReceiver actionReceiver = new WorkTimerActionReceiver();

    private final IBinder iBinder = new LocalBinder();

    public static TimerService thisTimerService;



    public enum PeriodState {WORK, BREAK, BIG_BREAK}
    private volatile PeriodState currentPeriod = PeriodState.WORK;
    private int consecutiveWorkPeriods = 0;
    private int periodsUntilBreak = 3;

    private volatile int timeMillisLeft = 0;
    private volatile long timeMillisStarted = 0;
    private volatile int totalMillis = 0;
    private volatile long stopTimeMillis = 0;


    private static String startActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_START";
    private static String pauseActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_PAUSE";
    private static String stopActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_STOP";
    private static String skipActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_SKIP";

    private PendingIntent startActionIntent, stopActionIntent, pauseActionIntent,
            openMainActivityIntent, skipActionIntent;

    SharedPreferences sharedPreferences;


    private TimerServiceListener serviceListener;


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
    public void onCreate() {
        super.onCreate();

        createIntentFilter();
        initControlIntents();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        checkNumberOfSessionsUntilBreak(sharedPreferences);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED) {
            // ����������ǰ̨����
        } else {
            // ���û�л��Ȩ�ޣ���������Ȩ��
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE}, REQUEST_FOREGROUND_PERMISSION);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        thisTimerService = this;

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
        Intent intentMainActivity = new Intent(this, TimerActivity.class);
        intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openMainActivityIntent = PendingIntent.getActivity(this, 1, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent startIntent = new Intent(startActionIntentString);
        startActionIntent = PendingIntent.getBroadcast(this, 100, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(pauseActionIntentString);
        pauseActionIntent = PendingIntent.getBroadcast(this, 100, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(stopActionIntentString);
        stopActionIntent = PendingIntent.getBroadcast(this, 100, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent skipIntent = new Intent(skipActionIntentString);
        skipActionIntent = PendingIntent.getBroadcast(this, 100, skipIntent, PendingIntent.FLAG_IMMUTABLE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel timerControlsNotificationChannel = new NotificationChannel(timerControlsNotificationChannelId,
                    "Timer controls", NotificationManager.IMPORTANCE_LOW);
            timerControlsNotificationChannel.setDescription("Pause, Resume os Stop timer with notification");
            notificationManager.createNotificationChannel(timerControlsNotificationChannel);

            NotificationChannel timerFinishNotificationChannel = new NotificationChannel(timerFinishedNotificationChannelId,
                    "Timer Finish Notification", NotificationManager.IMPORTANCE_HIGH);
            timerFinishNotificationChannel.setDescription("Get notified when cycle ends");
            timerFinishNotificationChannel.setLightColor(Color.RED);
            timerFinishNotificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(timerFinishNotificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        stopTimer();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        try {
            unregisterReceiver(actionReceiver);
        } catch (IllegalArgumentException e){

        }
    }

    public void startTimer(){

        thisTimerService = this;

        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                resetTimeCounter();

                timerState = TimerState.STARTED;

                restartThread();
            } else if (timerState == TimerState.PAUSED) {
                timerState = TimerState.STARTED;
                timerThreadLock.notifyAll();
            }
        }
    }


    private void moveToNextPeriod(){
        if (currentPeriod == PeriodState.WORK){
            consecutiveWorkPeriods++;
        } else if (currentPeriod == PeriodState.BIG_BREAK){
            consecutiveWorkPeriods = 0;
        }
        currentPeriod = getNextPeriod(consecutiveWorkPeriods);
    }


    private void checkNumberOfSessionsUntilBreak(SharedPreferences sharedPrefs){
        periodsUntilBreak = sharedPrefs.getInt(getString(R.string.sessions_until_big_break_preference_key),
                Integer.valueOf(getString(R.string.sessions_until_big_break_default_value)));
        if (consecutiveWorkPeriods > periodsUntilBreak) consecutiveWorkPeriods = periodsUntilBreak;
    }


    private void resetTimeCounter(){
        timeMillisStarted = System.currentTimeMillis();
        totalMillis = getTimeLeftMillis(currentPeriod);
        timeMillisLeft = totalMillis;
        stopTimeMillis = timeMillisStarted + totalMillis;
    }


    private void restartThread(){
        timerThread = new Thread(new TimerRunnable());
        timerThread.start();
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
                moveToNextPeriod();
                timerState = TimerState.STOPPED;
                timerThreadLock.notifyAll();
            }
            sendTime(getTimeLeftMillis(currentPeriod), getTimeLeftMillis(currentPeriod));
        }
    }

    public void skipPeriod(){
        synchronized (timerThreadLock) {
            if (timerState == TimerState.STOPPED) {
                moveToNextPeriod();
                sendTime(getTimeLeftMillis(currentPeriod), getTimeLeftMillis(currentPeriod));
            } else {
                stopTimer();
            }
        }
    }


    public void onStopButtonClick(){
        currentPeriod = PeriodState.BREAK;
        consecutiveWorkPeriods = 0;
        stopTimer();
        stopForeground(true);
    }

    public void onStartPauseButtonClick(){
        if(timerState == TimerState.STARTED) pauseTimer();
        else startTimer();
    }

    public void onSkipButtonClickInActivity(){
        skipPeriod();
        stopForeground(true);
    }


    public PeriodState getNextPeriod(int consecutiveWorkPeriods){
        switch (currentPeriod){
            case WORK:
                if (periodsUntilBreak != 0 && consecutiveWorkPeriods >= periodsUntilBreak) return PeriodState.BIG_BREAK;
                else return PeriodState.BREAK;
            case BREAK:
            case BIG_BREAK:
                return PeriodState.WORK;
        }
        return PeriodState.WORK;
    }

    public int getTimeLeftMillis(PeriodState period){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int timeLeft = 0;
        switch (period){
            case WORK:
                timeLeft = sharedPrefs.getInt(getString(R.string.work_time_minutes_preference_key),
                        Integer.valueOf(getString(R.string.work_time_minutes_default_value)) );
                break;
            case BREAK:
                timeLeft = sharedPrefs.getInt(getString(R.string.small_break_time_minutes_preference_key),
                        Integer.valueOf(getString(R.string.small_break_time_minutes_default_value)));
                break;
            case BIG_BREAK:
                timeLeft = sharedPrefs.getInt(getString(R.string.big_break_time_minutes_preference_key),
                        Integer.valueOf(getString(R.string.big_break_time_minutes_default_value)));
                break;
        }
        return timeLeft*60000;

    }




    private void sendTime(int totalMillis, int millisLeft){
        if (serviceListener != null) serviceListener.onTimeChange(totalMillis, millisLeft, timerState);
    }

    public void registerTimerListener(TimerServiceListener listener){
        serviceListener = listener;
    }

    public void unregisterTimerListener(){
        serviceListener = null;
    }


    public int getMillisLeft(){
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(currentPeriod);
        } else return timeMillisLeft;
    }

    public int getMillisTotal(){
        if (timerState == TimerState.STOPPED) {
            return getTimeLeftMillis(currentPeriod);
        } else return totalMillis;
    }


    public TimerState getCurrentTimerState(){
        return timerState;
    }

    public short getPeriodsLeftUntilBigBreak(){
        if (periodsUntilBreak - consecutiveWorkPeriods <= 0) return (short) periodsUntilBreak;
        else return (short) (periodsUntilBreak - consecutiveWorkPeriods);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(timerControlsNotificationChannelId);
        }

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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(timerControlsNotificationChannelId);
        }

        startForeground(timerNotificationId, builder.build());
    }

    private void timerFinishedNotification(){
        Notification.Builder builder =
                new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod) + getString(R.string.finished_notification_title))
                        .setContentText(getString(R.string.finished_notification_text)
                                + getSessionName(getNextPeriod(consecutiveWorkPeriods + 1)) + "?")
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(timerFinishedNotificationChannelId);
        }


        startForeground(timerNotificationId, builder.build());
    }

    private String getSessionName(PeriodState period){
        switch(period){
            case WORK: return getString(R.string.work_time_text);
            case BIG_BREAK: return getString(R.string.big_break_text);
            case BREAK: return getString(R.string.break_time_text);
        }
        return "";
    }




    public PeriodState getCurrentPeriod() {
        return currentPeriod;
    }


    public int getConsecutiveWorkPeriods() {
        return consecutiveWorkPeriods;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        checkNumberOfSessionsUntilBreak(sharedPreferences);
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
                        timerFinishedNotification();
                        stopTimer();
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


    public static class WorkTimerActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(thisTimerService != null) {
                String action = intent.getAction();
                if (action.equalsIgnoreCase(startActionIntentString)) {
                    thisTimerService.startTimer();
                } else if (action.equalsIgnoreCase(pauseActionIntentString)) {
                    thisTimerService.pauseTimer();
                } else if (action.equalsIgnoreCase(stopActionIntentString)) {
                    thisTimerService.onStopButtonClick();
                } else if (action.equalsIgnoreCase(skipActionIntentString)) {
                    thisTimerService.skipPeriod();
                    thisTimerService.startTimer();
                }
            }
        }

    }
}