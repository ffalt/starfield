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
    public static final float DEFAULT_MAX_V = 20;
    public static final float DEFAULT_MIN_V = 1;
    public static final float DEFAULT_STAR_SIZE = 2;
    public static final int DEFAULT_NUM_STARS = 1000;
    public static final boolean DEFAULT_STARS_CIRCLE = true;
    public static final boolean DEFAULT_STARS_TRAIL = true;
    public static final boolean DEFAULT_FOLLOW_SCREEN = true;
    public static final int DEFAULT_FOLLOW_SCREEN_INTENSITY = 10;
    public static final boolean DEFAULT_FOLLOW_SENSOR = false;
    public static final int DEFAULT_FOLLOW_SENSOR_INTENSITY = 10;
    public static final boolean DEFAULT_FOLLOW_RESTORE = true;
    public static final int DEFAULT_STAR_COLOR = Color.WHITE;
    public static final int DEFAULT_TRAIL_COLOR_START = Color.WHITE;
    public static final int DEFAULT_TRAIL_COLOR_END = Color.parseColor("#B3B3B3");
    public static final boolean DEFAULT_METEORS_ENABLED = true;
    public static final int DEFAULT_METEOR_COLOR_START = Color.parseColor("#FFEB3B");
    public static final int DEFAULT_METEOR_COLOR_END = Color.parseColor("#E98E1E");
    public static final float DEFAULT_METEOR_SPAWN_PROB = 0.0005f; // per-frame
    public static final int METEOR_MAX_COUNT = 3;
    public static final float DEFAULT_DEPTH = 2f;
    public static final boolean DEFAULT_BATTERY_SPEED = false;
    public static final int DEFAULT_BG_COLOR = Color.BLACK;
    public static final boolean DEFAULT_BG_GRADIENT = false;
    public static final int DEFAULT_BG_GRADIENT_INNER_COLOR = Color.parseColor("#0A0A2E");
    public static final int DEFAULT_BG_GRADIENT_RADIUS = 70;
    public float width = 100f;
    public float height = 100f;
    public float hW = 50f;
    public float hH = 50f;
    public float initialZ = 200f;
    public float minV = StarfieldOpts.DEFAULT_MIN_V;
    public float maxV = StarfieldOpts.DEFAULT_MAX_V;
    public float starSize = StarfieldOpts.DEFAULT_STAR_SIZE;
    public int numStars = StarfieldOpts.DEFAULT_NUM_STARS;
    public int starColor = StarfieldOpts.DEFAULT_STAR_COLOR;
    public int trailColorStart = StarfieldOpts.DEFAULT_TRAIL_COLOR_START;
    public int trailColorEnd = StarfieldOpts.DEFAULT_TRAIL_COLOR_END;
    public boolean meteorsEnabled = StarfieldOpts.DEFAULT_METEORS_ENABLED;
    public float meteorSpawnProb = StarfieldOpts.DEFAULT_METEOR_SPAWN_PROB;
    public int meteorColorStart = StarfieldOpts.DEFAULT_METEOR_COLOR_START;
    public int meteorColorEnd = StarfieldOpts.DEFAULT_METEOR_COLOR_END;
    public boolean followScreen = StarfieldOpts.DEFAULT_FOLLOW_SCREEN;
    public int followScreenIntensity = StarfieldOpts.DEFAULT_FOLLOW_SCREEN_INTENSITY;
    public boolean followRestore = StarfieldOpts.DEFAULT_FOLLOW_RESTORE;
    public boolean followSensor = StarfieldOpts.DEFAULT_FOLLOW_SENSOR;
    public int followSensorIntensity = StarfieldOpts.DEFAULT_FOLLOW_SENSOR_INTENSITY;
    public boolean trails = StarfieldOpts.DEFAULT_STARS_TRAIL;
    public boolean circle = StarfieldOpts.DEFAULT_STARS_CIRCLE;
    public float depth = StarfieldOpts.DEFAULT_DEPTH;
    public boolean batterySpeed = StarfieldOpts.DEFAULT_BATTERY_SPEED;
    public int bgColor = StarfieldOpts.DEFAULT_BG_COLOR;
    public boolean bgGradient = StarfieldOpts.DEFAULT_BG_GRADIENT;
    public int bgGradientInnerColor = StarfieldOpts.DEFAULT_BG_GRADIENT_INNER_COLOR;
    public int bgGradientRadius = StarfieldOpts.DEFAULT_BG_GRADIENT_RADIUS;
    public int fps = 60;
    public long drawTime = Math.round(1000.0 / 60);

    public void updateFPS(int newFps) {
        this.fps = Math.max(1, newFps);
        this.drawTime = Math.round(1000.0 / this.fps);
    }

    public void updateDepth() {
        this.initialZ = Math.min(width, height) * depth;
    }

    public void updateBounds(int w, int h) {
        this.width = w;
        this.height = h;
        this.hW = w / 2f;
        this.hH = h / 2f;
        updateDepth();
    }

    private static volatile SharedPreferences sPreferences;

    public static SharedPreferences getPreferences(Context context) {
        if (sPreferences == null) {
            synchronized (StarfieldOpts.class) {
                if (sPreferences == null) {
                    Context directBootContext = context.createDeviceProtectedStorageContext();
                    sPreferences = directBootContext.getSharedPreferences(StarfieldPrefs.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                }
            }
        }
        return sPreferences;
    }
}
