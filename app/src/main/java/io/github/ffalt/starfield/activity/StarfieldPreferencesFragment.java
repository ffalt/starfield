/*
 * Starfield - Android Live Wallpaper
 * Copyright (C) 2024 https://github.com/ffalt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * --
 *
 * Starfield may be used and distributed under the terms of the GPLv3, which
 * are available at: http://www.gnu.org/licenses/gpl-3.0.html
 *
 * If you would like to embed or publish Starfield into as a commercial application or
 * redistribute it in a modified binary form, contact ffalt at
 * https://github.com/ffalt/starfield
 */

package io.github.ffalt.starfield.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import io.github.ffalt.starfield.R;
import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.StarfieldPrefs;

public class StarfieldPreferencesFragment extends PreferenceFragmentCompat {
    public static final String RESET_PREFERENCE_KEY = "reset";
    public Preference minVPref;
    public Preference maxVPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setStorageDeviceProtected();
        preferenceManager.setSharedPreferencesName(StarfieldPrefs.SHARED_PREFS_NAME);
        setPreferencesFromResource(R.xml.preferences, rootKey);
        minVPref = findPreference(StarfieldPrefs.SHARED_PREFS_MIN_V);
        maxVPref = findPreference(StarfieldPrefs.SHARED_PREFS_MAX_V);
        Activity parent = getActivity();
        if (parent != null) {
            SharedPreferences prefs = StarfieldOpts.getPreferences(parent);
            if (minVPref != null) {
                minVPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    int minV = (int) newValue;
                    int maxV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MAX_V, Math.round(StarfieldOpts.DEFAULT_MAX_V));
                    updateVLabels(minV, maxV);
                    return true;
                });
            }
            if (maxVPref != null) {
                maxVPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    int minV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MIN_V, Math.round(StarfieldOpts.DEFAULT_MIN_V));
                    int maxV = (int) newValue;
                    updateVLabels(minV, maxV);
                    return true;
                });
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        if (preference.getKey().equals(RESET_PREFERENCE_KEY)) {
            askForResetPreference();
            return true;
        }
        return false;
    }

    private void updateVLabels(int minV, int maxV) {
        if (minV > maxV) {
            minVPref.setTitle(getString(R.string.settings_max_speed));
            maxVPref.setTitle(getString(R.string.settings_min_speed));
        } else {
            minVPref.setTitle(getString(R.string.settings_min_speed));
            maxVPref.setTitle(getString(R.string.settings_max_speed));
        }
    }

    public void askForResetPreference() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.reset_settings_title)
                .setMessage(R.string.reset_settings_confirmation)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> resetPreference())
                .setNegativeButton(R.string.no, null).show();
    }

    public void resetPreference() {
        Activity parent = getActivity();
        if (parent == null) {
            return;
        }
        SharedPreferences prefs = StarfieldOpts.getPreferences(parent);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        parent.finish();
        startActivity(parent.getIntent());
    }
}
