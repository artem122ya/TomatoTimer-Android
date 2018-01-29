package artem122ya.tomatotimer;

import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;




@RunWith(AndroidJUnit4.class)
public class TimerServiceTest {

    TimerService timerService;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();


    @Before
    public void bindService() throws TimeoutException{
        Intent intent = new Intent(InstrumentationRegistry.getTargetContext(), TimerService.class);

        IBinder binder = mServiceRule.bindService(intent);

        timerService = ((TimerService.LocalBinder) binder).getService();
    }


    @Test
    public void startTimer_shouldChangeStateToStarted_whenCalled(){
        timerService.startTimer();
        assertEquals(TimerService.TimerState.STARTED, timerService.getCurrentTimerState());
    }


    @Test
    public void stopTimer_shouldChangeStateToStopped_whenCalled(){
        timerService.startTimer();
        timerService.stopTimer();
        assertEquals(TimerService.TimerState.STOPPED, timerService.getCurrentTimerState());
    }


    @Test
    public void pauseTimer_shouldChangeStateToPaused_whenCalled(){
        timerService.startTimer();
        timerService.pauseTimer();
        assertEquals(TimerService.TimerState.PAUSED, timerService.getCurrentTimerState());
    }


    @Test
    public void startTimer_shouldIncrementConsecutiveFocusPeriod_whenCalled(){
        timerService.startTimer();
        assertEquals(1, timerService.consecutiveFocusPeriod);
    }


    @Test
    public void onStopButtonClick_shouldResetConsecutiveFocusPeriod_whenCalled(){
        timerService.onStopButtonClick();
        assertEquals(TimerService.TimerState.STOPPED, timerService.getCurrentTimerState());
        assertEquals(0, timerService.consecutiveFocusPeriod);
    }


    @Test
    public void onSkipButtonClick_shouldSkipToNextPeriod_whenCalled(){
        timerService.onSkipButtonClick();
        assertEquals(TimerService.PeriodState.FOCUS, timerService.currentPeriod);
    }

    @Test
    public void onSkipButtonClick_shouldIncrementConsecutiveFocusPeriod_whenCalled(){
        timerService.onSkipButtonClick();
        assertEquals(1, timerService.consecutiveFocusPeriod);
    }

}
