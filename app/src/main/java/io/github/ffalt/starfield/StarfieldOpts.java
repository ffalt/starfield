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

package io.github.ffalt.starfield;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class StarfieldOpts {
    public static final float defaultMaxV = 20;
    public static final float defaultMinV = 1;
    public static final float defaultStarSize = 2;
    public static final int defaultNumStars = 1000;
    public static final boolean defaultStarsCircle = true;
    public static final boolean defaultStarsTrail = true;
    public static final boolean defaultFollowScreen = true;
    public static final boolean defaultFollowSensor = false;
    public static final int defaultFollowSensorIntensity = 10;
    public static final boolean defaultFollowRestore = true;
    public static final int defaultStarColor = Color.WHITE;
    public static final int defaultTrailColorStart = Color.WHITE;
    public static final int defaultTrailColorEnd = Color.parseColor("#B3B3B3");
    public static final boolean defaultMeteorsEnabled = true;
    public static final int defaultMeteorColorStart = Color.parseColor("#FFEB3B");
    public static final int defaultMeteorColorEnd = Color.parseColor("#E98E1E");
    public static final float defaultMeteorSpawnProb = 0.0005f; // per-frame
    public static final int meteorMaxCount = 3;
    public static final float defaultDepth = 2f;
    public static final boolean defaultBatterySpeed = false;
    public float W = 100f;
    public float H = 100f;
    public float hW = 50f;
    public float hH = 50f;
    public float initialZ = 200f;
    public float minV = StarfieldOpts.defaultMinV;
    public float maxV = StarfieldOpts.defaultMaxV;
    public float starSize = StarfieldOpts.defaultStarSize;
    public int numStars = StarfieldOpts.defaultNumStars;
    public int starColor = StarfieldOpts.defaultStarColor;
    public int trailColorStart = StarfieldOpts.defaultTrailColorStart;
    public int trailColorEnd = StarfieldOpts.defaultTrailColorEnd;
    public boolean meteorsEnabled = StarfieldOpts.defaultMeteorsEnabled;
    public float meteorSpawnProb = StarfieldOpts.defaultMeteorSpawnProb;
    public int meteorColorStart = StarfieldOpts.defaultMeteorColorStart;
    public int meteorColorEnd = StarfieldOpts.defaultMeteorColorEnd;
    public boolean followScreen = StarfieldOpts.defaultFollowScreen;
    public boolean followRestore = StarfieldOpts.defaultFollowRestore;
    public boolean followSensor = StarfieldOpts.defaultFollowSensor;
    public int followSensorIntensity = StarfieldOpts.defaultFollowSensorIntensity;
    public boolean trails = StarfieldOpts.defaultStarsTrail;
    public boolean circle = StarfieldOpts.defaultStarsCircle;
    public float depth = StarfieldOpts.defaultDepth;
    public boolean batterySpeed = StarfieldOpts.defaultBatterySpeed;
    public int fps = 60;
    public long drawTime = 1000 / 60;

    public void updateFPS(int fps) {
        this.fps = fps;
        this.drawTime = 1000 / fps;
    }

    public void updateDepth() {
        this.initialZ = Math.min(W, H) * depth;
    }

    public void updateBounds(int W, int H) {
        this.W = W;
        this.H = H;
        this.hW = W / 2f;
        this.hH = H / 2f;
        updateDepth();
    }

    public static SharedPreferences getPreferences(Context context) {
        Context directBootContext = context.createDeviceProtectedStorageContext();
        return directBootContext.getSharedPreferences(StarfieldPrefs.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }
}
