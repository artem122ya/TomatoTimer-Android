package artem122ya.tomatotimer.timer;



public interface TimerContract {

    interface TimerServiceListener {

        void onTimerStart(int totalMillis);

        void onTimerPause(int totalMillis, int millisLeft);

        void onTimerTick(int totalMillis, int millisLeft);

        void onTimerStop(int totalMillis);

        void onPeriodsCountChange(int periodsLeft);

        void onPeriodStateChange(TimerService.PeriodState state);

    }

    interface TimerPresenter {

    }


}
