package artem122ya.tomatotimer;

import android.app.Activity;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeoutException;

import artem122ya.tomatotimer.views.TimerView;

import static org.junit.Assert.assertEquals;


@RunWith(RobolectricTestRunner.class)
public class TimerViewTest {

    private TimerView timerView;

    @Before
    public void setUp() throws TimeoutException {
        Robolectric.buildService(TimerService.class);
        Activity mainActivity = Robolectric.setupActivity(Activity.class);
        timerView = (TimerView)View.inflate(mainActivity, R.layout.sample_timer_view, null);
    }

    @Test
    public void timerView_shouldInitializeSweepAngle_whenCreated(){
        assertEquals(270 , timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerStarted_shouldSetSweepAngleToZero_whenPassedTimeThatEnds(){
        timerView.onTimerStarted(10000, 300);
        assertEquals(0, timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerStarted_shouldSetSweepAngleToHalf_whenPassedHalfOfTime(){
        timerView.onTimerStarted(10000, 5300);
        assertEquals(135, timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerStarted_shouldSetSweepAngleToFull_whenPassedFullTime(){
        timerView.onTimerStarted(10000, 10300);
        assertEquals(270, timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerStarted_shouldSetSweepAngleToZero_whenNegativeTime(){
        timerView.onTimerStarted(10000, -1000);
        assertEquals(0, timerView.getCurrentArcSweepAngle(), 0);
    }


    @Test
    public void stopAnimation_shouldStopAnimatingAngle_whenCalled() throws InterruptedException {
        timerView.onTimerStarted(10000, 5300);
        timerView.stopAnimation();
        Thread.sleep(1000);
        assertEquals(135, timerView.getCurrentArcSweepAngle(), 0);
    }


    @Test
    public void onTimerPaused_shouldSetSweepAngleToHalf_whenPassedHalfOfTime(){
        timerView.onTimerPaused(10000, 5000);
        assertEquals(135, timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerStopped_shouldSetSweepAngleToHalf_whenPassedHalfOfTime(){
        timerView.onTimerStopped(10000, 5000);
        assertEquals(135, timerView.getCurrentArcSweepAngle(), 0);
    }

    @Test
    public void onTimerUpdate_shouldSetSweepAngleToHalf_whenPassedHalfOfTime(){
        timerView.onTimerStopped(10000, 5000);
        assertEquals(135, timerView.getCurrentArcSweepAngle(), 0);
    }


    @Test
    public void onTimerUpdate_shouldSetDisplayedTime_whenPassedTime(){
        timerView.onTimerStopped(61000, 61000);
        assertEquals("01:01", timerView.getDisplayedTime());
    }


    @Test
    public void onTimerUpdate_shouldSetDisplayedTimeToZero_whenPassedNegativeTime(){
        timerView.onTimerStopped(1000, -1000);
        assertEquals("00:00", timerView.getDisplayedTime());
    }

    @Test
    public void onTimerUpdate_shouldSetDisplayedTimeToTripleDigits_whenPassedTime(){
        timerView.onTimerStopped(6000000, 6000000);
        assertEquals("100:00", timerView.getDisplayedTime());
    }

}
