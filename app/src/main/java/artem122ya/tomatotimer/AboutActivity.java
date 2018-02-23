package artem122ya.tomatotimer;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import artem122ya.tomatotimer.utils.ThemeManager;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ThemeManager(this)
                .setDarkTheme(R.style.AppThemeDark)
                .setLightTheme(R.style.AppThemeLight)
                .applyTheme();
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new AboutPreferenceFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        // Turn off activity close animation
        super.onBackPressed();
        overridePendingTransition(0,0);
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

    public static class AboutPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.about);
            setVersionNumberAsSummary(findPreference(getString(R.string.about_app_version_key)));
        }

        private void setVersionNumberAsSummary(Preference preference){
            String version = BuildConfig.VERSION_NAME;
            preference.setSummary(version);

        }


    }
}
