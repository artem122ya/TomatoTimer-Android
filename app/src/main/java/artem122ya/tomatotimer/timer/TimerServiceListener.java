package artem122ya.tomatotimer.timer;


public interface TimerServiceListener {

    void onTimeChange(int totalMillis, int millisLeft, TimerService.TimerState timerState);

}
