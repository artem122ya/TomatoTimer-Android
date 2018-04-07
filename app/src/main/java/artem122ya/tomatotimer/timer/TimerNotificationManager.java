package artem122ya.tomatotimer.timer;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import artem122ya.tomatotimer.R;

import static artem122ya.tomatotimer.utils.Utils.getTimeString;

public class TimerNotificationManager extends BroadcastReceiver {

    private Service timerService;

    private int timerNotificationId;

    private static String startActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_START";
    private static String pauseActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_PAUSE";
    private static String stopActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_STOP";
    private static String skipActionIntentString = "artem122ya.tomatotimer.ACTION_TIMER_SKIP";

    private PendingIntent startActionIntent, stopActionIntent, pauseActionIntent,
            openMainActivityIntent, skipActionIntent;


    public TimerNotificationManager(TimerService timerService, int timerNotificationId) {
        this.timerService = timerService;
        this.timerNotificationId = timerNotificationId;
        initControlIntents();
    }

    public void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(startActionIntentString);
        intentFilter.addAction(pauseActionIntentString);
        intentFilter.addAction(stopActionIntentString);
        intentFilter.addAction(skipActionIntentString);
        timerService.registerReceiver(this, intentFilter);
    }

    public void unregisterReceiver(){
        timerService.unregisterReceiver(this);
    }


    private void initControlIntents(){
        Intent intentMainActivity = new Intent(timerService, TimerActivity.class);
        intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openMainActivityIntent = PendingIntent.getActivity(timerService, 1, intentMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startIntent = new Intent(startActionIntentString);
        startActionIntent = PendingIntent.getBroadcast(timerService, 100, startIntent, 0);

        Intent pauseIntent = new Intent(pauseActionIntentString);
        pauseActionIntent = PendingIntent.getBroadcast(timerService, 100, pauseIntent, 0);

        Intent stopIntent = new Intent(stopActionIntentString);
        stopActionIntent = PendingIntent.getBroadcast(timerService, 100, stopIntent, 0);

        Intent skipIntent = new Intent(skipActionIntentString);
        skipActionIntent = PendingIntent.getBroadcast(timerService, 100, skipIntent, 0);

    }

    private void timerRunningNotification(TimerService.PeriodState currentPeriod, int totalMillis, int timeMillisLeft){
        Notification.Builder builder =
                new Notification.Builder(timerService)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(timerService.getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + timerService.getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_pause_black_24dp,
                                timerService.getString(R.string.pause_notification_action_title), pauseActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                timerService.getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                timerService.getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setProgress(totalMillis, timeMillisLeft, false)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        timerService.startForeground(timerNotificationId, builder.build());
    }

    private void timerPausedNotification(TimerService.PeriodState currentPeriod, int totalMillis, int timeMillisLeft){
        Notification.Builder builder =
                new Notification.Builder(timerService)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(timerService.getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod))
                        .setContentText(getTimeString(timeMillisLeft) + timerService.getString(R.string.notification_text))
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                timerService.getString(R.string.resume_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                timerService.getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                timerService.getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        timerService.startForeground(timerNotificationId, builder.build());
    }

    private void timerFinishedNotification(TimerService.PeriodState currentPeriod, TimerService.PeriodState nextPeriod){
        Notification.Builder builder =
                new Notification.Builder(timerService)
                        .setSmallIcon(R.mipmap.ic_notification_icon)
                        .setColor(timerService.getResources().getColor(R.color.colorPrimary))
                        .setContentTitle(getSessionName(currentPeriod) + timerService.getString(R.string.finished_notification_title))
                        .setContentText(timerService.getString(R.string.finished_notification_text)
                                + getSessionName(nextPeriod) + "?")
                        .setContentIntent(openMainActivityIntent)
                        .addAction(new Notification.Action(R.drawable.ic_play_arrow_black_24dp,
                                timerService.getString(R.string.start_notification_action_title), startActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,
                                timerService.getString(R.string.skip_notification_action_title), skipActionIntent))
                        .addAction(new Notification.Action(R.drawable.ic_stop_black_24dp,
                                timerService.getString(R.string.stop_notification_action_title), stopActionIntent))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_ALARM);

        timerService.startForeground(timerNotificationId, builder.build());
    }

    private String getSessionName(TimerService.PeriodState period){
        switch(period){
            case WORK: return timerService.getString(R.string.work_time_text);
            case BIG_BREAK: return timerService.getString(R.string.big_break_text);
            case BREAK: return timerService.getString(R.string.break_time_text);
        }
        return "";
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if(timerService != null) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(startActionIntentString)) {
                timerService.startTimer();
            } else if (action.equalsIgnoreCase(pauseActionIntentString)) {
                timerService.pauseTimer();
            } else if (action.equalsIgnoreCase(stopActionIntentString)) {
                timerService.onStopButtonClick();
            } else if (action.equalsIgnoreCase(skipActionIntentString)) {
                timerService.skipPeriod();
                timerService.startTimer();
            }
        }
    }


}
