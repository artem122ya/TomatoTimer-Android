package artem122ya.tomatotimer;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ThemeManager{

    private SharedPreferences sharedPreferences;
    private Context context;
    private int lightTheme;
    private int darkTheme;

    public ThemeManager(Context context){
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public ThemeManager setLightTheme(int id){
        lightTheme = id;
        return this;
    }

    public ThemeManager setDarkTheme(int id){
        darkTheme = id;
        return this;
    }

    public void applyTheme(){
        boolean darkModeEnabled = sharedPreferences.getBoolean(
                context.getString(R.string.dark_mode_preference_key)
                , false);
        int currentTheme = darkModeEnabled ? darkTheme : lightTheme;
        context.setTheme(currentTheme);
    }
}
