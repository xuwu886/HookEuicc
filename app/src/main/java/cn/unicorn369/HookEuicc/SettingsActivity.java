package cn.unicorn369.HookEuicc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
        //private SwitchPreference enableNoEuiccPref;
        private SwitchPreference enableBypassOmapiPref;
        private SwitchPreference enableHideIconPref;

        private SwitchPreference enableFakeEidPref;
        private EditTextPreference valueFakeEidPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("conf");
            addPreferencesFromResource(R.xml.prefs);

            enableHookPref = (SwitchPreference) findPreference("enable_hook");
            //enableNoEuiccPref = (SwitchPreference) findPreference("enable_no_euicc");
            enableBypassOmapiPref = (SwitchPreference) findPreference("enable_bypass_omapi");
            enableHideIconPref = (SwitchPreference) findPreference("enable_hide_icon");

            enableFakeEidPref = (SwitchPreference) findPreference("enable_fake_eid");
            valueFakeEidPref = (EditTextPreference) findPreference("value_fake_eid");

            valueFakeEidPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = (String) newValue;
                if (value.isEmpty()) {
                    preference.setSummary(getString(R.string.summary_fake_eid_value));
                } else {
                    preference.setSummary(value);
                }
                return true;
            });

            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            String currentEid = prefs.getString("value_fake_eid", "");
            if (!currentEid.isEmpty()) {
                valueFakeEidPref.setSummary(currentEid);
            }
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
            boolean hide = prefs.getBoolean("enable_hide_icon", false);
            enableHideIconPref.setChecked(hide);
            applyIconVisibility(hide);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("enable_hide_icon".equals(key)) {
                boolean hide = sharedPreferences.getBoolean("enable_hide_icon", false);
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