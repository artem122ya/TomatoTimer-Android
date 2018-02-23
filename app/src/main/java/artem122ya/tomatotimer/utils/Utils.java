package artem122ya.tomatotimer.utils;


import java.util.concurrent.TimeUnit;

public class Utils {

    public static String getTimeString(int millis){
        if (millis < 0) return "00:00";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d",
                minutes,TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes));
    }

}
