package artem122ya.tomatotimer.timer;


import artem122ya.tomatotimer.timer.Timer.TimerState;

public interface TimerContract {

    interface Timer {

        void registerListener(TimerListener timerListener);

        void unregisterListener();

        void startTimer(int milliseconds);

        void stopTimer();

        void pauseTimer();

        void resumeTimer();

        TimerState getTimerState();

    }

    interface TimerListener {

        void onTimerFinish();

        void onTimerTick(int millisecondsLeft);

    }

    interface TimerSevice {

    }

    interface TimerNotificationManager {

    }

    interface TimerPresenter {

        void onTimerStart(int millisecondsTotal);

        void onTimerPause(int millisecondsTotal, int millisLeft);

        void onTimerTick(int millisecondsTotal, int millisLeft);

        void onTimerStop(int millisecondsTotal);

        void onPeriodsCountChange(int periodsLeft);

        void onPeriodStateChange(TimerService.PeriodState state);

        void onTimeChange(int millisecondsTotal, int millisLeft, TimerService.TimerState timerState);

    }

    interface TimerActivity {

    }









}
