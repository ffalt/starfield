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

package io.github.ffalt.starfield.painting.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.concurrent.ThreadLocalRandom;

import io.github.ffalt.starfield.StarfieldOpts;

public class NebulaPaint {
    private final StarfieldOpts opts;
    private float[] blobX = new float[0];
    private float[] blobY = new float[0];
    private float[] blobVx = new float[0];
    private float[] blobVy = new float[0];
    private float[] blobRadius = new float[0];
    private int[] blobBaseAlpha = new int[0];
    private float[] blobAge = new float[0];
    private float[] blobInvFadeIn = new float[0];
    private float[] blobRespawnDelay = new float[0];
    private Bitmap blobBitmap;
    private final Paint blobPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destRect = new RectF();
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetTX = 0;
    private float offsetTY = 0;
    private float tiltOffsetX = 0;
    private float tiltOffsetY = 0;
    private float tiltTargetX = 0;
    private float tiltTargetY = 0;
    private float cachedShiftX = 0;
    private float cachedShiftY = 0;
    private static final int BLOB_TEX_SIZE = 64;
    private static final float DRIFT_SPEED = 0.4f;
    private static final float WARP_SPEED = 0.001f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float PARALLAX = 0.4f;
    private static final float SMOOTHING = 0.01f;
    private static final float FADE_IN_MIN = 120f;
    private static final float FADE_IN_MAX = 300f;
    private static final float ALPHA_SCALE = 255f / 100f;
    private static final float RESPAWN_DELAY_MIN = 120f;
    private static final float RESPAWN_DELAY_MAX = 600f;
    private static final float MAX_TILT_ANGLE = (float) Math.toRadians(50.0);
    private static final float INV_MAX_TILT_ANGLE = 1f / MAX_TILT_ANGLE;
    private float speedModifier = 1.0f;

    public NebulaPaint(StarfieldOpts opts) {
        this.opts = opts;
    }

    public void init() {
        buildBitmap();
        int n = opts.nebulaCount;
        blobX = new float[n];
        blobY = new float[n];
        blobVx = new float[n];
        blobVy = new float[n];
        blobRadius = new float[n];
        blobBaseAlpha = new int[n];
        blobAge = new float[n];
        blobInvFadeIn = new float[n];
        blobRespawnDelay = new float[n];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            spawnBlob(i, rng);
            blobAge[i] = 1f / blobInvFadeIn[i];
            blobRespawnDelay[i] = 0f;
        }
    }

    public void move() {
        if (opts.followScreen) {
            float dx = offsetTX - offsetX;
            if (dx > SMOOTHING || dx < -SMOOTHING) {
                offsetX += dx * SMOOTHING;
            }
            float dy = offsetTY - offsetY;
            if (dy > SMOOTHING || dy < -SMOOTHING) {
                offsetY += dy * SMOOTHING;
            }
            if (opts.followRestore) {
                offsetTX -= offsetTX * SMOOTHING;
                offsetTY -= offsetTY * SMOOTHING;
            }
        }
        if (opts.followSensor) {
            float tx = tiltTargetX - tiltOffsetX;
            if (tx > SMOOTHING || tx < -SMOOTHING) {
                tiltOffsetX += tx * SMOOTHING;
            }
            float ty = tiltTargetY - tiltOffsetY;
            if (ty > SMOOTHING || ty < -SMOOTHING) {
                tiltOffsetY += ty * SMOOTHING;
            }
        }
        float parallaxM = PARALLAX * opts.nebulaMovement;
        float shiftX = (offsetX + tiltOffsetX) * parallaxM;
        float shiftY = (offsetY + tiltOffsetY) * parallaxM;
        cachedShiftX = shiftX;
        cachedShiftY = shiftY;
        final float[] sx = blobX;
        final float[] sy = blobY;
        final float[] svx = blobVx;
        final float[] svy = blobVy;
        final float[] sr = blobRadius;
        final float[] sa = blobAge;
        final float[] sif = blobInvFadeIn;
        final float[] srd = blobRespawnDelay;
        int n = sx.length;
        float w = opts.width;
        float h = opts.height;
        float hW = opts.hW;
        float hH = opts.hH;
        float warpK = WARP_SPEED * speedModifier;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            if (srd[i] > 0f) {
                srd[i] -= 1f;
                if (srd[i] <= 0f) {
                    srd[i] = 0f;
                    spawnBlob(i, rng);
                }
                continue;
            }
            sx[i] += (sx[i] - hW) * warpK + svx[i];
            sy[i] += (sy[i] - hH) * warpK + svy[i];
            if (sa[i] * sif[i] < 1f) {
                sa[i] += 1f;
            }
            float r = sr[i];
            float vx = sx[i] - shiftX;
            float vy = sy[i] - shiftY;
            if (vx < -r * 2 || vx > w + r * 2 || vy < -r * 2 || vy > h + r * 2) {
                srd[i] = RESPAWN_DELAY_MIN + rng.nextFloat() * (RESPAWN_DELAY_MAX - RESPAWN_DELAY_MIN);
            }
        }
    }

    public void draw(Canvas c) {
        if (blobBitmap == null) {
            return;
        }
        float shiftX = cachedShiftX;
        float shiftY = cachedShiftY;
        float w = opts.width;
        float h = opts.height;
        int colorRgb = opts.nebulaColor & 0x00FFFFFF;
        final float[] sx = blobX;
        final float[] sy = blobY;
        final float[] sr = blobRadius;
        final int[] sba = blobBaseAlpha;
        final float[] sa = blobAge;
        final float[] sif = blobInvFadeIn;
        final float[] srd = blobRespawnDelay;
        int n = sx.length;
        for (int i = 0; i < n; i++) {
            if (srd[i] > 0f) {
                continue;
            }
            float cx = sx[i] - shiftX;
            float cy = sy[i] - shiftY;
            float fullR = sr[i];
            if (cx + fullR < 0 || cx - fullR > w || cy + fullR < 0 || cy - fullR > h) {
                continue;
            }
            float rawFade = sa[i] * sif[i];
            float fade = Math.min(rawFade, 1f);
            int alpha = (int) (sba[i] * fade);
            if (alpha < 2) {
                continue;
            }
            float r = fullR * fade;
            destRect.set(cx - r, cy - r, cx + r, cy + r);
            blobPaint.setColor(colorRgb | (alpha << 24));
            c.drawBitmap(blobBitmap, null, destRect, blobPaint);
        }
    }

    private void buildBitmap() {
        if (blobBitmap != null) {
            blobBitmap.recycle();
        }
        blobBitmap = Bitmap.createBitmap(BLOB_TEX_SIZE, BLOB_TEX_SIZE, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(blobBitmap);
        float half = BLOB_TEX_SIZE / 2f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(half, half, half,
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(half, half, half, p);
    }

    private void spawnBlob(int i, ThreadLocalRandom rng) {
        float w = opts.width;
        float h = opts.height;
        float minDim = Math.min(w, h);
        blobRadius[i] = minDim * (0.15f + rng.nextFloat() * 0.35f);
        float margin = blobRadius[i] * 0.5f;
        float spawnW = w - margin * 2;
        float spawnH = h - margin * 2;
        if (spawnW <= 0 || spawnH <= 0) {
            blobX[i] = w * 0.5f;
            blobY[i] = h * 0.5f;
        } else {
            blobX[i] = margin + rng.nextFloat() * spawnW;
            blobY[i] = margin + rng.nextFloat() * spawnH;
        }
        initBlobDrift(i, rng);
    }


    private void initBlobDrift(int i, ThreadLocalRandom rng) {
        float angle = rng.nextFloat() * TWO_PI;
        float speed = DRIFT_SPEED * (0.5f + rng.nextFloat());
        blobVx[i] = (float) Math.cos(angle) * speed;
        blobVy[i] = (float) Math.sin(angle) * speed;
        int baseAlpha = opts.nebulaOpacity;
        int variance = Math.max(1, baseAlpha / 3);
        int pct = Math.max(1, Math.min(100,
                baseAlpha + rng.nextInt(variance * 2 + 1) - variance));
        blobBaseAlpha[i] = (int) (pct * ALPHA_SCALE);
        float fadeIn = FADE_IN_MIN + rng.nextFloat() * (FADE_IN_MAX - FADE_IN_MIN);
        blobInvFadeIn[i] = 1f / fadeIn;
        blobAge[i] = 0f;
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    public void clearOffsets() {
        offsetTX = 0;
        offsetX = 0;
        offsetTY = 0;
        offsetY = 0;
        tiltTargetX = 0;
        tiltTargetY = 0;
    }

    public void setTilt(float tiltX, float tiltY) {
        float clampedPitch = Math.max(-MAX_TILT_ANGLE, Math.min(MAX_TILT_ANGLE, tiltX));
        float clampedRoll = Math.max(-MAX_TILT_ANGLE, Math.min(MAX_TILT_ANGLE, tiltY));
        float nPitch = clampedPitch * INV_MAX_TILT_ANGLE;
        float nRoll = clampedRoll * INV_MAX_TILT_ANGLE;
        float targetX = -nRoll * opts.width;
        float targetY = nPitch * opts.height;
        float intensity = opts.followSensorIntensity * 0.01f;
        tiltTargetX += (targetX - tiltTargetX) * intensity;
        tiltTargetY += (targetY - tiltTargetY) * intensity;
    }

    public void setOffsets(float diffX, float diffY) {
        float scale = 0.25f;
        if (opts.followRestore) {
            scale = 1f;
        }
        float intensity = opts.followScreenIntensity * 0.1f;
        offsetTX += diffX * scale * intensity;
        offsetTY += diffY * scale * intensity;
    }
}
