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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.github.ffalt.starfield.R;
import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.StarfieldPrefs;
import io.github.ffalt.starfield.painting.effects.ConstellationCatalog;

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
    private final Choreographer.FrameCallback frameCallback = this::doFrame;
    private Choreographer choreographer;
    private long lastVsyncNanos = 0;
    private long lastRenderNanos = 0;
    private long frameAccumulatorNanos = 0;
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
    private volatile boolean tiltPending = false;

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
        loadPreferences(context, StarfieldOpts.getPreferences(context));
    }

    public void registerOnSharedPreferenceChanged(Context context) {
        SharedPreferences prefs = StarfieldOpts.getPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadPreferences(context, prefs);
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
        if (mContext != null) {
            loadPreferences(mContext, prefs);
        }
    }

    // Defaults come from res/values/opts.xml and res/values/colors.xml, the same resources the settings
    // screen declares as android:defaultValue. Never fall back to the current value: that would make a
    // cleared preference file a no-op instead of a reset.
    private void loadPreferences(Context context, SharedPreferences prefs) {
        Resources res = context.getResources();
        boolean update = false;
        int starCount = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_COUNT, res.getInteger(R.integer.star_count_default));
        if (starCount != opts.numStars) {
            opts.numStars = starCount;
            update = true;
        }
        int storedMinV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MIN_V, res.getInteger(R.integer.min_v_default));
        int storedMaxV = prefs.getInt(StarfieldPrefs.SHARED_PREFS_MAX_V, res.getInteger(R.integer.max_v_default));
        int minV = Math.min(storedMinV, storedMaxV);
        int maxV = Math.max(storedMinV, storedMaxV);
        if (minV != opts.minV || maxV != opts.maxV) {
            opts.minV = minV;
            opts.maxV = maxV;
            update = true;
        }
        boolean starTrail = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_TRAIL, res.getBoolean(R.bool.star_trail_default));
        if (starTrail != opts.trails) {
            opts.trails = starTrail;
        }
        int trailIntensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRAIL_INTENSITY, res.getInteger(R.integer.trail_intensity_default));
        if (trailIntensity != opts.trailIntensity) {
            opts.trailIntensity = trailIntensity;
        }
        boolean starCircle = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_STAR_CIRCLE, res.getBoolean(R.bool.star_round_default));
        if (starCircle != opts.circle) {
            opts.circle = starCircle;
        }
        boolean followScreen = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SCREEN, res.getBoolean(R.bool.follow_screen_default));
        if (followScreen != opts.followScreen) {
            opts.followScreen = followScreen;
            if (starfield != null) {
                starfield.clearScreenOffsets();
            }
        }
        int followScreenIntensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FOLLOW_SCREEN_INTENSITY, res.getInteger(R.integer.follow_screen_intensity_default));
        if (followScreenIntensity != opts.followScreenIntensity) {
            opts.followScreenIntensity = followScreenIntensity;
        }
        boolean followSensor = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR, res.getBoolean(R.bool.follow_sensor_default));
        if (followSensor != opts.followSensor) {
            opts.followSensor = followSensor;
            updateSensorListener();
            update = true;
        }
        int followSensorIntensity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FOLLOW_SENSOR_INTENSITY, res.getInteger(R.integer.follow_sensor_intensity_default));
        if (followSensorIntensity != opts.followSensorIntensity) {
            opts.followSensorIntensity = followSensorIntensity;
        }
        boolean followRestore = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_FOLLOW_RESTORE, res.getBoolean(R.bool.follow_restore_default));
        if (followRestore != opts.followRestore) {
            opts.followRestore = followRestore;
        }
        float starSize = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_SIZE, res.getInteger(R.integer.star_size_default)) / 10f;
        if (starSize != opts.starSize) {
            opts.starSize = starSize;
        }
        float depth = prefs.getInt(StarfieldPrefs.SHARED_PREFS_DEPTH, res.getInteger(R.integer.depth_default)) / 10f;
        if (depth != opts.depth) {
            opts.depth = depth;
            opts.updateDepth();
            update = true;
        }
        int starColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_STAR_COLOR, context.getColor(R.color.star_color_default));
        if (starColor != opts.starColor) {
            opts.starColor = starColor;
            update = true;
        }
        int trailColorStart = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRAIL_COLOR_START, context.getColor(R.color.trail_color_start_default));
        if (trailColorStart != opts.trailColorStart) {
            opts.trailColorStart = trailColorStart;
            update = true;
        }
        int trailColorEnd = prefs.getInt(StarfieldPrefs.SHARED_PREFS_TRAIL_COLOR_END, context.getColor(R.color.trail_color_end_default));
        if (trailColorEnd != opts.trailColorEnd) {
            opts.trailColorEnd = trailColorEnd;
            update = true;
        }
        boolean meteorsEnabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_METEORS_ENABLED, res.getBoolean(R.bool.meteors_enabled_default));
        if (meteorsEnabled != opts.meteorsEnabled) {
            opts.meteorsEnabled = meteorsEnabled;
            update = true;
        }
        int meteorColorStart = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_START, context.getColor(R.color.meteor_color_start_default));
        if (meteorColorStart != opts.meteorColorStart) {
            opts.meteorColorStart = meteorColorStart;
            update = true;
        }
        int meteorColorEnd = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEOR_COLOR_END, context.getColor(R.color.meteor_color_end_default));
        if (meteorColorEnd != opts.meteorColorEnd) {
            opts.meteorColorEnd = meteorColorEnd;
            update = true;
        }
        float meteorSpawnProb = prefs.getInt(StarfieldPrefs.SHARED_PREFS_METEORS_PROBABILITY, res.getInteger(R.integer.meteor_spawn_probability_default)) / 10000f;
        if (meteorSpawnProb != opts.meteorSpawnProb) {
            opts.meteorSpawnProb = meteorSpawnProb;
        }
        int storedFps = prefs.getInt(StarfieldPrefs.SHARED_PREFS_FPS, res.getInteger(R.integer.fps_default));
        int fps = Math.max(res.getInteger(R.integer.fps_min), Math.min(res.getInteger(R.integer.fps_max), storedFps));
        if (fps != opts.fps) {
            opts.updateFPS(fps);
        }
        boolean batterySpeed = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_BATTERY_SPEED, res.getBoolean(R.bool.battery_speed_default));
        if (batterySpeed != opts.batterySpeed) {
            opts.batterySpeed = batterySpeed;
            updateBatteryListener();
            update = true;
        }
        int bgColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_COLOR, context.getColor(R.color.bg_color_default));
        if (bgColor != opts.bgColor) {
            opts.bgColor = bgColor;
            bgPaintDirty = true;
        }
        boolean bgGradient = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT, res.getBoolean(R.bool.bg_gradient_default));
        if (bgGradient != opts.bgGradient) {
            opts.bgGradient = bgGradient;
            bgPaintDirty = true;
        }
        int bgGradientInnerColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT_INNER_COLOR, context.getColor(R.color.bg_gradient_inner_color_default));
        if (bgGradientInnerColor != opts.bgGradientInnerColor) {
            opts.bgGradientInnerColor = bgGradientInnerColor;
            bgPaintDirty = true;
        }
        int bgGradientRadius = prefs.getInt(StarfieldPrefs.SHARED_PREFS_BG_GRADIENT_RADIUS, res.getInteger(R.integer.bg_gradient_radius_default));
        if (bgGradientRadius != opts.bgGradientRadius) {
            opts.bgGradientRadius = bgGradientRadius;
            bgPaintDirty = true;
        }
        boolean constellationsEnabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_CONSTELLATIONS_ENABLED, res.getBoolean(R.bool.constellations_enabled_default));
        if (constellationsEnabled != opts.constellationsEnabled) {
            opts.constellationsEnabled = constellationsEnabled;
            update = true;
        }
        boolean constellationsLarge = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_CONSTELLATIONS_LARGE, res.getBoolean(R.bool.constellations_large_default));
        if (constellationsLarge != opts.constellationsLarge) {
            opts.constellationsLarge = constellationsLarge;
            update = true;
        }
        boolean constellationsLines = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_CONSTELLATIONS_LINES, res.getBoolean(R.bool.constellations_lines_default));
        if (constellationsLines != opts.constellationsLines) {
            opts.constellationsLines = constellationsLines;
        }
        boolean nebulaEnabled = prefs.getBoolean(StarfieldPrefs.SHARED_PREFS_NEBULA_ENABLED, res.getBoolean(R.bool.nebula_enabled_default));
        if (nebulaEnabled != opts.nebulaEnabled) {
            opts.nebulaEnabled = nebulaEnabled;
            update = true;
        }
        int nebulaColor = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_COLOR, context.getColor(R.color.nebula_color_default));
        if (nebulaColor != opts.nebulaColor) {
            opts.nebulaColor = nebulaColor;
            update = true;
        }
        int nebulaCount = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_COUNT, res.getInteger(R.integer.nebula_count_default));
        if (nebulaCount != opts.nebulaCount) {
            opts.nebulaCount = nebulaCount;
            update = true;
        }
        int nebulaOpacity = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_OPACITY, res.getInteger(R.integer.nebula_opacity_default));
        if (nebulaOpacity != opts.nebulaOpacity) {
            opts.nebulaOpacity = nebulaOpacity;
            update = true;
        }
        int nebulaMovement = prefs.getInt(StarfieldPrefs.SHARED_PREFS_NEBULA_MOVEMENT, res.getInteger(R.integer.nebula_movement_default));
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

    private void releaseSensor() {
        sensorManager = null;
        sensor = null;
        isSensorAvailable = false;
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
        ConstellationCatalog constellations = opts.constellationsEnabled && mContext != null
                ? ConstellationCatalog.getInstance(mContext) : null;
        starfield = new Starfield(opts, constellations);
        updateSpeedModifier();
    }

    private void updateSpeedModifier() {
        if (starfield == null) {
            return;
        }

        if (opts.batterySpeed) {
            starfield.setSpeedModifier(0.1f + batteryLevel * 0.9f);
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
        stopFrames();
        if (newVisible) {
            startFrames();
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

    public void onSurfaceDestroyed() {
        visible = false;
        stopFrames();
        unregisterSensorListener();
    }

    public void onDestroy(Context context) {
        onSurfaceDestroyed();
        releaseSensor();
        unregisterOnSharedPreferenceChanged(context);
        unregisterBatteryListener(context);
        recycleBgBitmap();
        mContext = null;
    }

    private Choreographer getChoreographer() {
        if (choreographer == null) {
            choreographer = Choreographer.getInstance();
        }
        return choreographer;
    }

    private void startFrames() {
        lastVsyncNanos = 0;
        lastRenderNanos = 0;
        frameAccumulatorNanos = 0;
        getChoreographer().postFrameCallback(frameCallback);
    }

    private void stopFrames() {
        if (choreographer != null) {
            choreographer.removeFrameCallback(frameCallback);
        }
    }

    private void doFrame(long frameTimeNanos) {
        if (!visible) {
            return;
        }
        getChoreographer().postFrameCallback(frameCallback);
        long interval = opts.frameIntervalNanos;
        frameAccumulatorNanos += lastVsyncNanos == 0 ? interval : frameTimeNanos - lastVsyncNanos;
        lastVsyncNanos = frameTimeNanos;
        if (frameAccumulatorNanos < interval) {
            return;
        }
        frameAccumulatorNanos -= interval;
        if (frameAccumulatorNanos > interval) {
            // The display cannot reach the requested rate; do not build up credit for a catch-up burst.
            frameAccumulatorNanos = interval;
        }
        opts.updateTimeScale(lastRenderNanos == 0 ? interval : frameTimeNanos - lastRenderNanos);
        lastRenderNanos = frameTimeNanos;
        drawFrame();
    }

    private void drawFrame() {
        if (starfield == null || !sizeInitialized) {
            return;
        }

        final SurfaceHolder holder = getSurface();
        Canvas c = null;
        try {
            c = holder.lockHardwareCanvas();
            if (c != null) {
                drawBackground(c);
                starfield.draw(c);
            }
        } catch (IllegalStateException ignored) {
            // surface released before this frame could be drawn
        } finally {
            if (c != null) {
                holder.unlockCanvasAndPost(c);
            }
        }
        if (visible) {
            starfield.move();
        }
    }
}
