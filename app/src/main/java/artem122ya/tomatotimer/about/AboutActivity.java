package artem122ya.tomatotimer.about;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import artem122ya.tomatotimer.R;
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

}
