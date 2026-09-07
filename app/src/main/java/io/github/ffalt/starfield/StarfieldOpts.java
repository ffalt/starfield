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

public class StarfieldOpts {
    public static final int METEOR_MAX_COUNT = 3;
    private static final float REFERENCE_FPS = 60f;
    public float width = 100f;
    public float height = 100f;
    public float hW = 50f;
    public float hH = 50f;
    public float initialZ = 200f;
    public float minV;
    public float maxV;
    public float starSize;
    public int numStars;
    public int starColor;
    public int trailColorStart;
    public int trailColorEnd;
    public boolean meteorsEnabled;
    public float meteorSpawnProb;
    public int meteorColorStart;
    public int meteorColorEnd;
    public boolean followScreen;
    public int followScreenIntensity;
    public boolean followRestore;
    public boolean followSensor;
    public int followSensorIntensity;
    public boolean trails;
    public int trailIntensity;
    public boolean circle;
    public float depth;
    public boolean batterySpeed;
    public int bgColor;
    public boolean bgGradient;
    public int bgGradientInnerColor;
    public int bgGradientRadius;
    public boolean nebulaEnabled;
    public int nebulaColor;
    public int nebulaCount;
    public int nebulaOpacity;
    public int nebulaMovement;
    public int fps;
    public long drawTime = Math.round(1000.0 / 60);
    public float timeScale = 1f;

    public void updateFPS(int newFps) {
        this.fps = Math.max(1, newFps);
        this.drawTime = Math.round(1000.0 / this.fps);
        this.timeScale = REFERENCE_FPS / this.fps;
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
