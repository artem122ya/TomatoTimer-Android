package artem122ya.tomatotimer.settings;


import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import artem122ya.tomatotimer.R;


public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setDarkTheme(sharedPreferences.getBoolean(getString(R.string.dark_mode_preference_key)
                , false));
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment existingFragment = getFragmentManager().findFragmentById(android.R.id.content);
        if (existingFragment == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new TimerPreferenceFragment()).commit();
        }
    }

    @Override
    public void onBackPressed() {
        // Turn off activity close animation
        super.onBackPressed();
        overridePendingTransition(0,0);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.dark_mode_preference_key))) {
            recreate();
        }
    }

    private void setDarkTheme(boolean darkThemeEnabled){
        setTheme(darkThemeEnabled ? R.style.AppThemeDark : R.style.AppThemeLight);
    }

    @Override
    protected void onResume() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
