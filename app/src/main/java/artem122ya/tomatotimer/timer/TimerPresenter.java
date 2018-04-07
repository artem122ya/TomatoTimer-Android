package artem122ya.tomatotimer.timer;


public class TimerPresenter implements TimerServiceListener {

    private TimerActivity timerActivity;

    private TimerService timerService;


    public TimerPresenter(TimerActivity timerActivity) {
        this.timerActivity = timerActivity;
    }


    public void registerService(TimerService timerService){
        this.timerService = timerService;
    }

    public void unregisterService(){
        timerService = null;
    }


    @Override
    public void onTimerStart(int totalMillis) {

    }


    @Override
    public void onTimerPause(int totalMillis, int millisLeft) {

    }


    @Override
    public void onTimerTick(int totalMillis, int millisLeft) {

    }


    @Override
    public void onTimerStop(int totalMillis) {

    }


    @Override
    public void onPeriodsCountChange(int periodsLeft) {

    }


    @Override
    public void onPeriodStateChange(TimerService.PeriodState state) {

    }

    public void onStartPauseButtonClick() {
    }

    public void onStopButtonClick() {
    }

    public void onSkipButtonClickInActivity() {
    }
}
