package artem122ya.tomatotimer.settings;


import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;


import android.view.MenuItem;


import androidx.appcompat.app.AppCompatActivity;
import artem122ya.tomatotimer.R;
import artem122ya.tomatotimer.utils.ThemeManager;


public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ThemeManager(this)
                .setDarkTheme(R.style.AppThemeDark)
                .setLightTheme(R.style.AppThemeLight)
                .applyTheme();
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Fragment existingFragment = getFragmentManager().findFragmentById(android.R.id.content);
        if (existingFragment == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new SettingsFragment()).commit();
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
