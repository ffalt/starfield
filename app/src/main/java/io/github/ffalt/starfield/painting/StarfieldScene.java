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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
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
    private Bitmap bgBitmap = null;
    private Canvas bgCanvas = null;
    private final RectF bgRect = new RectF();
    private final Paint mPaintBg = new Paint();
    private final Paint mPaintBgBitmap = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint mPaintBgGradient = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean bgPaintDirty = true;
    public final StarfieldOpts opts = new StarfieldOpts();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDrawThread = this::drawFrame;
    public boolean visible = false;
    public boolean isSensorAvailable = false;
    private boolean sizeInitialized = false;
    private Sensor sensor;
    private SensorManager sensorManager;
    private BroadcastReceiver batteryReceiver;
    private float batteryLevel = 1.0f;
    private Context mContext;
    private volatile float pendingTiltX;
    private volatile float pendingTiltY;
    private boolean tiltPending = false;

    private final Runnable applyTilt = () -> {
        tiltPending = false;
        if (starfield != null) {
            starfield.setTilt(pendingTiltX, pendingTiltY);
        }
    };

    protected StarfieldScene() {
        mPaintBg.setStyle(Paint.Style.FILL);
    }

    public void onUpdateOffset(float offsetX, float offsetY) {
        if (starfield != null) {
            starfield.setOffsets(offsetX, offsetY);
        }
    }

    public void onUpdateSize(int width, int height) {
        opts.updateBounds(width, height);
        bgRect.set(0, 0, width, height);
        sizeInitialized = true;
        bgPaintDirty = true;
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
        if (!opts.followSensor) {
            return;
        }
        if (sensorManager != null) {
            return;
        }

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
        int minV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MIN_V, Math.round(opts.minV));
        int maxV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MAX_V, Math.round(opts.maxV));
        if (minV != opts.minV || maxV != opts.maxV) {
            opts.minV = minV;
            opts.maxV = maxV;
            if (minV > maxV) {
                opts.minV = maxV;
                opts.maxV = minV;
            }
            update = true;
        }
        boolean starTrail = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_TRAIL, opts.trails);
        if (starTrail != opts.trails) {
            opts.trails = starTrail;
        }
        boolean starCircle = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_CIRCLE, opts.circle);
        if (starCircle != opts.circle) {
            opts.circle = starCircle;
        }
        boolean followScreen = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SCREEN, opts.followScreen);
        if (followScreen != opts.followScreen) {
            opts.followScreen = followScreen;
        }
        int followScreenIntensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FOLLOW_SCREEN_INTENSITY, opts.followScreenIntensity);
        if (followScreenIntensity != opts.followScreenIntensity) {
            opts.followScreenIntensity = followScreenIntensity;
        }
        boolean followSensor = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR, opts.followSensor);
        if (followSensor != opts.followSensor) {
            opts.followSensor = followSensor;
            updateSensorListener();
            update = true;
        }
        int followSensorIntensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR_INTENSITY, opts.followSensorIntensity);
        if (followSensorIntensity != opts.followSensorIntensity) {
            opts.followSensorIntensity = followSensorIntensity;
        }
        boolean followRestore = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_RESTORE, opts.followRestore);
        if (followRestore != opts.followRestore) {
            opts.followRestore = followRestore;
        }
        float starSize = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_SIZE, Math.round(opts.starSize * 10)) / 10f;
        if (starSize != opts.starSize) {
            opts.starSize = starSize;
        }
        float depth = prefs.getInt(StarfieldPrefs.SHARED_PREFS_DEPTH, Math.round(opts.depth * 10)) / 10f;
        if (depth != opts.depth) {
            opts.depth = depth;
            opts.updateDepth();
            update = true;
        }
        int starColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_COLOR, opts.starColor);
        if (starColor != opts.starColor) {
            opts.starColor = starColor;
            update = true;
        }
        int trailColorStart = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRAIL_COLOR_START, opts.trailColorStart);
        if (trailColorStart != opts.trailColorStart) {
            opts.trailColorStart = trailColorStart;
            update = true;
        }
        int trailColorEnd = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRAIL_COLOR_END, opts.trailColorEnd);
        if (trailColorEnd != opts.trailColorEnd) {
            opts.trailColorEnd = trailColorEnd;
            update = true;
        }
        boolean meteorsEnabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_METEORS_ENABLED, opts.meteorsEnabled);
        if (meteorsEnabled != opts.meteorsEnabled) {
            opts.meteorsEnabled = meteorsEnabled;
            update = true;
        }
        int meteorColorStart = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_START, opts.meteorColorStart);
        if (meteorColorStart != opts.meteorColorStart) {
            opts.meteorColorStart = meteorColorStart;
            update = true;
        }
        int meteorColorEnd = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_END, opts.meteorColorEnd);
        if (meteorColorEnd != opts.meteorColorEnd) {
            opts.meteorColorEnd = meteorColorEnd;
            update = true;
        }
        float meteorSpawnProb = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEORS_PROBABILITY, Math.round(opts.meteorSpawnProb * 10000)) / 10000f;
        if (meteorSpawnProb != opts.meteorSpawnProb) {
            opts.meteorSpawnProb = meteorSpawnProb;
        }
        int fps = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FPS, opts.fps);
        if (fps != opts.fps) {
            opts.updateFPS(fps);
        }
        boolean batterySpeed = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_BATTERY_SPEED, opts.batterySpeed);
        if (batterySpeed != opts.batterySpeed) {
            opts.batterySpeed = batterySpeed;
            updateBatteryListener();
            update = true;
        }
        int bgColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_COLOR, opts.bgColor);
        if (bgColor != opts.bgColor) {
            opts.bgColor = bgColor;
            bgPaintDirty = true;
        }
        boolean bgGradient = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT, opts.bgGradient);
        if (bgGradient != opts.bgGradient) {
            opts.bgGradient = bgGradient;
            bgPaintDirty = true;
        }
        int bgGradientInnerColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT_INNER_COLOR, opts.bgGradientInnerColor);
        if (bgGradientInnerColor != opts.bgGradientInnerColor) {
            opts.bgGradientInnerColor = bgGradientInnerColor;
            bgPaintDirty = true;
        }
        int bgGradientRadius = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT_RADIUS, opts.bgGradientRadius);
        if (bgGradientRadius != opts.bgGradientRadius) {
            opts.bgGradientRadius = bgGradientRadius;
            bgPaintDirty = true;
        }
        boolean nebulaEnabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_NEBULA_ENABLED, opts.nebulaEnabled);
        if (nebulaEnabled != opts.nebulaEnabled) {
            opts.nebulaEnabled = nebulaEnabled;
            update = true;
        }
        int nebulaColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_COLOR, opts.nebulaColor);
        if (nebulaColor != opts.nebulaColor) {
            opts.nebulaColor = nebulaColor;
            update = true;
        }
        int nebulaCount = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_COUNT, opts.nebulaCount);
        if (nebulaCount != opts.nebulaCount) {
            opts.nebulaCount = nebulaCount;
            update = true;
        }
        int nebulaOpacity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_OPACITY, opts.nebulaOpacity);
        if (nebulaOpacity != opts.nebulaOpacity) {
            opts.nebulaOpacity = nebulaOpacity;
            update = true;
        }
        int nebulaMovement = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_MOVEMENT, opts.nebulaMovement);
        if (nebulaMovement != opts.nebulaMovement) {
            opts.nebulaMovement = nebulaMovement;
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
                // ignore
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        pendingTiltX = event.values[0];
        pendingTiltY = event.values[1];
        if (!tiltPending) {
            tiltPending = true;
            mHandler.post(applyTilt);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor2, int accuracy2) {
        // nop
    }

    public void reset() {
        starfield = new Starfield(opts);
    }

    private void updateSpeedModifier() {
        if (starfield == null) {
            return;
        }

        if (opts.batterySpeed) {
            starfield.setSpeedModifier(0.1f + batteryLevel);
        } else {
            starfield.setSpeedModifier(1.0f);
        }
    }

    private void recycleBgBitmap() {
        if (bgBitmap != null) {
            bgBitmap.recycle();
            bgBitmap = null;
            bgCanvas = null;
        }
    }

    private void rebuildBgPaint() {
        if (opts.bgGradient && opts.width > 0 && opts.height > 0) {
            int bW = Math.max(32, Math.round(opts.width / 8f));
            int bH = Math.max(32, Math.round(opts.height / 8f));
            if (bgBitmap == null || bgBitmap.getWidth() != bW || bgBitmap.getHeight() != bH) {
                recycleBgBitmap();
                bgBitmap = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888);
                bgCanvas = new Canvas(bgBitmap);
            }
            float bhW = bW / 2f;
            float bhH = bH / 2f;
            float diag = (float) Math.sqrt(bhW * bhW + bhH * bhH);
            float radius = Math.max(1f, diag * (opts.bgGradientRadius / 100f));
            mPaintBgGradient.setStyle(Paint.Style.FILL);
            mPaintBgGradient.setShader(new RadialGradient(bhW, bhH, radius,
                    opts.bgGradientInnerColor, opts.bgColor, Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, bW, bH, mPaintBgGradient);
        } else {
            recycleBgBitmap();
            mPaintBg.setColor(opts.bgColor);
        }
        bgPaintDirty = false;
    }

    private void drawBackground(Canvas c) {
        if (bgPaintDirty) {
            rebuildBgPaint();
        }
        if (bgBitmap != null) {
            c.drawBitmap(bgBitmap, null, bgRect, mPaintBgBitmap);
        } else {
            c.drawRect(bgRect, mPaintBg);
        }
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

    public void onVisibilityChanged(boolean newVisible) {
        this.visible = newVisible;
        if (starfield != null) {
            starfield.clearOffsets();
        }
        mHandler.removeCallbacks(mDrawThread);
        if (newVisible) {
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
        if (!opts.batterySpeed) {
            return;
        }
        if (batteryReceiver != null) {
            return;
        }

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
                // ignore
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
        recycleBgBitmap();
        mContext = null;
    }

    private void drawFrame() {
        if (starfield == null || !sizeInitialized) {
            return;
        }

        final long startNs = System.nanoTime();
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
            if (c != null) {
                holder.unlockCanvasAndPost(c);
            }
        }
        if (visible) {
            starfield.move();
            final long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            mHandler.postDelayed(mDrawThread, Math.max(0, opts.drawTime - durationMs));
        }
    }
}
