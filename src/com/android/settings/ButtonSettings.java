/*
* Copyright 2014-2015 The Euphoria-OS Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioSystem;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.internal.util.cm.ScreenType;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ButtonSettings";

    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_VOLUME = "volume_keys";

    private static final String KEY_NAVIGATION_BAR_ENABLED = "navigation_bar_show";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_VOLUME_WAKE_DEVICE = "volume_wake_screen";
    private static final String KEY_VOLUME_MEDIA_CONTROLS = "volbtn_music_controls";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_DEFAULT = "volume_default_screen";

    private SwitchPreference mNavbarLeft;
    private SwitchPreference mPowerEndCall;
    private Preference mPowerMenu;
    private SwitchPreference mVolumeKeyWakeControl;
    private SwitchPreference mVolumeKeyMediaControl;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mSwapVolumeButtons;
    private ListPreference mVolumeDefault;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory navbarCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_NAVBAR);
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);

        mNavbarLeft = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);
        if (!ScreenType.isPhone(getActivity())) {
            navbarCategory.removePreference(mNavbarLeft);
            mNavbarLeft = null;
        }

        // Power button ends calls.
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);
        mPowerMenu = (Preference) findPreference(KEY_POWER_MENU);
        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        if (mPowerEndCall == null && mPowerMenu == null) {
            prefScreen.removePreference(powerCategory);
        }

        if (Utils.hasVolumeRocker(getActivity())) {
            mVolumeKeyWakeControl = (SwitchPreference) findPreference(KEY_VOLUME_WAKE_DEVICE);
            mVolumeKeyMediaControl = (SwitchPreference) findPreference(KEY_VOLUME_MEDIA_CONTROLS);
            if (!res.getBoolean(R.bool.config_supports_volumeKeyScreenOffOptions)) {
                volumeCategory.removePreference(mVolumeKeyWakeControl);
                volumeCategory.removePreference(mVolumeKeyMediaControl);
            } else {
                int wakeControlAction = Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0);
                mVolumeKeyWakeControl = initSwitch(KEY_VOLUME_WAKE_DEVICE, (wakeControlAction == 1));
            }

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (SwitchPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            
            mVolumeDefault = (ListPreference) findPreference(KEY_VOLUME_DEFAULT);
            String currentDefault = Settings.System.getString(resolver,
                Settings.System.VOLUME_KEYS_DEFAULT);
            boolean linkNotificationWithVolume = Settings.Secure.getInt(resolver,
                Settings.Secure.VOLUME_LINK_NOTIFICATION, 1) == 1;
            if (mVolumeDefault != null && !Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_RING));
            } else if (mVolumeDefault != null && linkNotificationWithVolume
                    && Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_NOTIFICATION));
            }
            if (mVolumeDefault != null) {
                if (currentDefault == null) {
                    currentDefault = mVolumeDefault.getEntryValues()
                        [mVolumeDefault.getEntryValues().length - 1].toString();
                    mVolumeDefault.setSummary(getString(R.string.volume_default_summary));
                }
                mVolumeDefault.setValue(currentDefault);
                mVolumeDefault.setSummary(mVolumeDefault.getEntry());
                mVolumeDefault.setOnPreferenceChangeListener(this);
            }
        } else {
            prefScreen.removePreference(volumeCategory);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                      (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private SwitchPreference initSwitch(String key, boolean checked) {
        SwitchPreference switchPreference = (SwitchPreference) getPreferenceManager()
                .findPreference(key);
        if (switchPreference != null) {
            switchPreference.setChecked(checked);
            switchPreference.setOnPreferenceChangeListener(this);
        }
        return switchPreference;
    }

    private void handleSwitchChange(SwitchPreference pref, Object newValue, String setting) {
        Boolean value = (Boolean) newValue;
        int intValue = (value) ? 1 : 0;
        Settings.System.putInt(getContentResolver(), setting, intValue);
    }

    private void updateVolumeDefault(Object newValue) {
        int index = mVolumeDefault.findIndexOfValue((String) newValue);
        int value = Integer.valueOf((String) newValue);
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.System.VOLUME_KEYS_DEFAULT, value);
        mVolumeDefault.setSummary(mVolumeDefault.getEntries()[index]);
    }

    public void removeListEntry(ListPreference list, String valuetoRemove) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        for (int i = 0; i < list.getEntryValues().length; i++) {
            if (list.getEntryValues()[i].toString().equals(valuetoRemove)) {
                continue;
            } else {
                entries.add(list.getEntries()[i]);
                values.add(list.getEntryValues()[i]);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mVolumeKeyWakeControl) {
            handleSwitchChange(mVolumeKeyWakeControl, newValue,
                    Settings.System.VOLUME_WAKE_SCREEN);
            return true;
        } else if (preference == mVolumeDefault) {
            String value = (String) newValue;
            Settings.System.putString(getActivity().getContentResolver(),
                Settings.System.VOLUME_KEYS_DEFAULT, value);
            updateVolumeDefault(newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (ScreenType.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.button_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return new ArrayList<String>();
                }
            };
}
