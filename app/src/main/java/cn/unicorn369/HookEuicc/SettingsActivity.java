package cn.unicorn369.HookEuicc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.View;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        checkEdXposed();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new SettingsFragment()).commit();
        }
    }

    @SuppressLint("WorldReadableFiles")
    private void checkEdXposed() {
        try {
            getSharedPreferences("conf", Context.MODE_WORLD_READABLE);
        } catch (SecurityException exception) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.not_supported))
                    .setPositiveButton(android.R.string.ok, (dialog12, which) -> finish())
                    .setNegativeButton(R.string.ignore, null)
                    .show();
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SwitchPreference enableHookPref;
        //private SwitchPreference noEuiccPref;
        private SwitchPreference bypassOmapiPref;
        private SwitchPreference hideIconPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("conf");
            addPreferencesFromResource(R.xml.prefs);

            enableHookPref = (SwitchPreference) findPreference("enable_hook");
            //noEuiccPref = (SwitchPreference) findPreference("no_euicc");
            bypassOmapiPref = (SwitchPreference) findPreference("bypass_omapi");
            hideIconPref = (SwitchPreference) findPreference("hide_icon");
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            View list = view.findViewById(android.R.id.list);
            list.setOnApplyWindowInsetsListener((v, insets) -> {
                list.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getStableInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            prefs.registerOnSharedPreferenceChangeListener(this);
            boolean hide = prefs.getBoolean("hide_icon", false);
            hideIconPref.setChecked(hide);
            applyIconVisibility(hide);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("hide_icon".equals(key)) {
                boolean hide = sharedPreferences.getBoolean("hide_icon", false);
                applyIconVisibility(hide);
            }
        }

        private void applyIconVisibility(boolean hide) {
            Activity activity = getActivity();
            if (activity == null) return;
            PackageManager pm = activity.getPackageManager();
            ComponentName aliasComponent = new ComponentName(activity, BuildConfig.APPLICATION_ID + ".MainActivityAlias");
            int newState = hide ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            pm.setComponentEnabledSetting(aliasComponent, newState, PackageManager.DONT_KILL_APP);
        }
    }
}