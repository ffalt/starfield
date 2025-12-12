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

package io.github.ffalt.starfield.painting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.StarfieldPrefs;

public abstract class StarfieldScene implements SurfaceHolderParent, SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    private Starfield starfield;
    private final Paint mPaintFill = new Paint();
    public final StarfieldOpts opts = new StarfieldOpts();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDrawThread = this::drawFrame;
    public boolean visible = false;
    public boolean isSensorAvailable = false;
    private Sensor sensor;
    private SensorManager sensorManager;
    private BroadcastReceiver batteryReceiver;
    private float batteryLevel = 1.0f;
    private Context mContext;

    public StarfieldScene() {
        mPaintFill.setStyle(Paint.Style.FILL);
        mPaintFill.setColor(Color.BLACK);
    }

    public void onUpdateOffset(float offsetX, float offsetY) {
        starfield.setOffsets(offsetX, offsetY);
    }

    public void onUpdateSize(int width, int height) {
        opts.updateBounds(width, height);
        reset();
    }

    public void updateFromSharedPreference(Context context) {
        SharedPreferences prefs = StarfieldOpts.getPreferences(context);
        onSharedPreferenceChanged(prefs, null);
    }

    public void registerOnSharedPreferenceChanged(Context context) {
        SharedPreferences prefs = StarfieldOpts.getPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, null);
    }

    public void initSensor(Context context) {
        if (!opts.followSensor) return;
        if (sensorManager != null) return;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null && sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            isSensorAvailable = true;
        } else {
            isSensorAvailable = false;
        }
    }

    public void unregisterOnSharedPreferenceChanged(Context context) {
        SharedPreferences prefs = StarfieldOpts.getPreferences(context);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        boolean update = false;
        int starCount = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_COUNT, opts.numStars);
        if (starCount != opts.numStars) {
            opts.numStars = starCount;
            update = true;
        }
        int min_v = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MIN_V, Math.round(opts.minV));
        int max_v = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MAX_V, Math.round(opts.maxV));
        if (min_v != opts.minV || max_v != opts.maxV) {
            opts.minV = min_v;
            opts.maxV = max_v;
            if (min_v > max_v) {
                opts.minV = max_v;
                opts.maxV = min_v;
            }
            update = true;
        }
        boolean star_trail = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_TRAIL, opts.trails);
        if (star_trail != opts.trails) {
            opts.trails = star_trail;
        }
        boolean star_circle = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_CIRCLE, opts.circle);
        if (star_circle != opts.circle) {
            opts.circle = star_circle;
        }
        boolean follow_screen = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SCREEN, opts.followScreen);
        if (follow_screen != opts.followScreen) {
            opts.followScreen = follow_screen;
        }
        boolean follow_sensor = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR, opts.followSensor);
        if (follow_sensor != opts.followSensor) {
            opts.followSensor = follow_sensor;
            updateSensorListener();
            update = true;
        }
        int follow_sensor_intensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR_INTENSITY, opts.followSensorIntensity);
        if (follow_sensor_intensity != opts.followSensorIntensity) {
            opts.followSensorIntensity = follow_sensor_intensity;
        }
        boolean follow_restore = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_RESTORE, opts.followRestore);
        if (follow_restore != opts.followRestore) {
            opts.followRestore = follow_restore;
        }
        float star_size = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_SIZE, Math.round(opts.starSize * 10)) / 10f;
        if (star_size != opts.starSize) {
            opts.starSize = star_size;
        }
        float depth = prefs.getInt(StarfieldPrefs.SHARED_PREFS_DEPTH, Math.round(opts.depth * 10)) / 10f;
        if (depth != opts.depth) {
            opts.depth = depth;
            opts.updateDepth();
            update = true;
        }
        int star_color = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_COLOR, opts.starColor);
        if (star_color != opts.starColor) {
            opts.starColor = star_color;
            update = true;
        }
        int trail_color_start = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRIAL_COLOR_START, opts.trailColorStart);
        if (trail_color_start != opts.trailColorStart) {
            opts.trailColorStart = trail_color_start;
            update = true;
        }
        int trail_color_end = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRIAL_COLOR_END, opts.trailColorEnd);
        if (trail_color_end != opts.trailColorEnd) {
            opts.trailColorEnd = trail_color_end;
            update = true;
        }
        boolean meteors_enabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_METEORS_ENABLED, opts.meteorsEnabled);
        if (meteors_enabled != opts.meteorsEnabled) {
            opts.meteorsEnabled = meteors_enabled;
            update = true;
        }
        int meteor_color_start = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_START, opts.meteorColorStart);
        if (meteor_color_start != opts.meteorColorStart) {
            opts.meteorColorStart = meteor_color_start;
            update = true;
        }
        int meteor_color_end = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_END, opts.meteorColorEnd);
        if (meteor_color_end != opts.meteorColorEnd) {
            opts.meteorColorEnd = meteor_color_end;
            update = true;
        }
        float meteor_spawn_prob = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEORS_PROBABILITY, Math.round(opts.meteorSpawnProb * 10000)) / 10000f;
        if (meteor_spawn_prob != opts.meteorSpawnProb) {
            opts.meteorSpawnProb = meteor_spawn_prob;
        }
        int fps = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FPS, opts.fps);
        if (fps != opts.fps) {
            opts.updateFPS(fps);
        }
        boolean battery_speed = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_BATTERY_SPEED, opts.batterySpeed);
        if (battery_speed != opts.batterySpeed) {
            opts.batterySpeed = battery_speed;
            updateBatteryListener();
            update = true;
        }
        if (update) {
            reset();
        }
    }

    private void updateBatteryListener() {
        if (mContext != null) {
            if (opts.batterySpeed) {
                initBattery(mContext);
            } else {
                unregisterBatteryListener(mContext);
            }
            batteryLevel = 1.0f;
            updateSpeedModifier();
        }
    }

    private void updateSensorListener() {
        if (mContext != null) {
            if (opts.followSensor) {
                initSensor(mContext);
                if (visible && isSensorAvailable && sensorManager != null && sensor != null) {
                    sensorManager.unregisterListener(this);
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            } else {
                unregisterSensorListener();
            }
        }
    }

    private void unregisterSensorListener() {
        if (sensorManager != null && isSensorAvailable) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (starfield != null) {
            starfield.setTilt(event.values[0], event.values[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void reset() {
        starfield = new Starfield(opts);
    }

    private void updateSpeedModifier() {
        if (starfield == null) return;

        if (opts.batterySpeed) {
            starfield.setSpeedModifier(0.1f + batteryLevel);
        } else {
            starfield.setSpeedModifier(1.0f);
        }
    }

    private void drawBackground(Canvas c) {
        c.drawRect(0, 0, opts.W, opts.H, mPaintFill);
    }

    public void onCreate(Context context) {
        mContext = context;
        registerOnSharedPreferenceChanged(context);
        if (opts.followSensor) {
            initSensor(context);
        }
        if (opts.batterySpeed) {
            initBattery(context);
        }
    }

    public void onVisibilityChanged(boolean visible) {
        this.visible = visible;
        starfield.clearOffsets();
        mHandler.removeCallbacks(mDrawThread);
        if (visible) {
            drawFrame();
            if (isSensorAvailable && opts.followSensor && sensorManager != null && sensor != null) {
                unregisterSensorListener();
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {
            unregisterSensorListener();
        }
    }


    private void initBattery(Context context) {
        if (!opts.batterySpeed) return;
        if (batteryReceiver != null) return;

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (opts.batterySpeed) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (scale > 0) {
                        batteryLevel = level / (float) scale;
                    } else {
                        batteryLevel = 1.0f;
                    }
                    updateSpeedModifier();
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryReceiver, filter);
    }

    private void unregisterBatteryListener(Context context) {
        if (batteryReceiver != null) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            batteryReceiver = null;
        }
    }

    public void onDestroy(Context context) {
        visible = false;
        mHandler.removeCallbacks(mDrawThread);
        unregisterOnSharedPreferenceChanged(context);
        unregisterSensorListener();
        unregisterBatteryListener(context);
        mContext = null;
    }

    private void drawFrame() {
        final long start = System.currentTimeMillis();
        mHandler.removeCallbacks(mDrawThread);
        final SurfaceHolder holder = getSurface();
        Canvas c = null;
        try {
            c = holder.lockHardwareCanvas();
            if (c != null) {
                drawBackground(c);
                starfield.draw(c);
            }
        } finally {
            if (c != null) holder.unlockCanvasAndPost(c);
        }
        if (visible) {
            starfield.move();
            final long duration = System.currentTimeMillis() - start;
            mHandler.postDelayed(mDrawThread, Math.max(0, opts.drawTime - duration));
        }
    }
}
