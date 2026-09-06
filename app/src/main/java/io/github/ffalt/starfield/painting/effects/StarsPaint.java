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

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.concurrent.ThreadLocalRandom;

import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.painting.ShapeBatcher;
import io.github.ffalt.starfield.painting.cache.StarPaintCache;
import io.github.ffalt.starfield.painting.cache.StarTrailPaintCache;

public class StarsPaint {
    private static final float MAX_TILT_ANGLE = (float) Math.toRadians(50.0);
    private static final float INV_MAX_TILT_ANGLE = 1f / MAX_TILT_ANGLE;
    private static final float SMOOTHING = 0.01f;

    private final StarfieldOpts opts;
    private final StarArrays stars = new StarArrays();

    private final ShapeBatcher bodyBatcher;
    private final ShapeBatcher trailBatcher;

    // Screen offset and gyroscope tilt, both eased towards their targets in move().
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetTX = 0;
    private float offsetTY = 0;
    private float tiltOffsetX = 0;
    private float tiltOffsetY = 0;
    private float tiltTargetX = 0;
    private float tiltTargetY = 0;
    private float speedModifier = 1.0f;

    // Structure of arrays: one array per star attribute rather than one object per star, so the
    // update loop walks contiguous memory. cur* hold the projected values draw() renders.
    private static final class StarArrays {
        private float[] x = new float[0];
        private float[] y = new float[0];
        private float[] z = new float[0];
        private float[] v = new float[0];
        private float[] radius = new float[0];
        private float[] lastX = new float[0];
        private float[] lastY = new float[0];
        private float[] curX = new float[0];
        private float[] curY = new float[0];
        private float[] curRadius = new float[0];
        private int[] curBrightness = new int[0];

        private void alloc(int n) {
            x = new float[n];
            y = new float[n];
            z = new float[n];
            v = new float[n];
            radius = new float[n];
            lastX = new float[n];
            lastY = new float[n];
            curX = new float[n];
            curY = new float[n];
            curRadius = new float[n];
            curBrightness = new int[n];
        }
    }

    public StarsPaint(StarfieldOpts opts) {
        this.opts = opts;
        bodyBatcher = new ShapeBatcher(new StarPaintCache(opts).getArray(), ShapeBatcher.POINTS);
        trailBatcher = new ShapeBatcher(new StarTrailPaintCache(opts).getArray(), ShapeBatcher.LINES);
    }

    public void init() {
        int n = opts.numStars;
        stars.alloc(n);
        bodyBatcher.resize(n);
        trailBatcher.resize(n);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            randomStarPosition(i, rng);
            stars.z[i] = rng.nextFloat() * opts.initialZ;
        }
    }

    public void move() {
        float ts = opts.timeScale;
        float smoothing = SMOOTHING * ts;
        if (opts.followScreen) {
            float dx = offsetTX - offsetX;
            if (dx > SMOOTHING || dx < -SMOOTHING) {
                offsetX += dx * smoothing;
            }
            float dy = offsetTY - offsetY;
            if (dy > SMOOTHING || dy < -SMOOTHING) {
                offsetY += dy * smoothing;
            }
            if (opts.followRestore) {
                offsetTX -= offsetTX * smoothing;
                offsetTY -= offsetTY * smoothing;
            }
        }
        if (opts.followSensor) {
            float tx = tiltTargetX - tiltOffsetX;
            if (tx > SMOOTHING || tx < -SMOOTHING) {
                tiltOffsetX += tx * smoothing;
            }
            float ty = tiltTargetY - tiltOffsetY;
            if (ty > SMOOTHING || ty < -SMOOTHING) {
                tiltOffsetY += ty * smoothing;
            }
        }
        // Cache all loop-invariant values; avoids repeated field reads inside the hot loop.
        float totalOffsetX = offsetX + tiltOffsetX;
        float totalOffsetY = offsetY + tiltOffsetY;
        float hW = opts.hW;
        float hH = opts.hH;
        float width = opts.width;
        float height = opts.height;
        float initialZ = opts.initialZ;
        float invInitialZ = 1f / initialZ;
        float starSize = opts.starSize;
        float speedFactor = 0.1f * speedModifier * ts;
        float vGain = 0.001f * ts;
        final float[] sX = stars.x;
        final float[] sY = stars.y;
        final float[] sZ = stars.z;
        final float[] sV = stars.v;
        final float[] sR = stars.radius;
        final float[] sLX = stars.lastX;
        final float[] sLY = stars.lastY;
        final float[] sCX = stars.curX;
        final float[] sCY = stars.curY;
        final float[] sCR = stars.curRadius;
        final int[] sCB = stars.curBrightness;
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        int n = sX.length;
        float vRange = opts.maxV - opts.minV;
        float minV = opts.minV;
        for (int i = 0; i < n; i++) {
            float sz = sZ[i] - sV[i] * speedFactor;
            if (sz <= 0f) {
                // Inline randomStarPosition using cached locals to avoid field re-reads.
                sX[i] = rng.nextFloat() * width - hW;
                sY[i] = rng.nextFloat() * height - hH;
                sV[i] = rng.nextFloat() * vRange + minV;
                sR[i] = rng.nextFloat() * 2f + 1f;
                sLX[i] = -1f;
                sLY[i] = -1f;
                sCX[i] = -1f;
                sCY[i] = -1f;
                sZ[i] = initialZ;
                continue;
            }
            sZ[i] = sz;
            sLX[i] = sCX[i];
            sLY[i] = sCY[i];
            sV[i] += vGain;
            float invZ = 1f / sz;
            sCX[i] = hW + (width * sX[i] * invZ - totalOffsetX);
            sCY[i] = hH + (height * sY[i] * invZ - totalOffsetY);
            float zRatio = sz * invInitialZ;
            sCR[i] = (1 - zRatio) * sR[i] * starSize;
            int b = 100 - (int) (zRatio * 40 + 0.5f);
            sCB[i] = b < 0 ? 0 : Math.min(b, 100);
        }
    }

    public void draw(Canvas c) {
        float width = opts.width;
        float height = opts.height;
        int n = stars.x.length;
        if (opts.trails) {
            drawLoopTrails(c, n, width, height);
        } else {
            drawLoopPoints(c, n, width, height);
        }
    }

    private void drawLoopPoints(Canvas c, int n, float width, float height) {
        final float[] sCX = stars.curX;
        final float[] sCY = stars.curY;
        final float[] sCR = stars.curRadius;
        final int[] sCB = stars.curBrightness;
        // drawCircle took a radius while drawRect drew a square of side r, so round stars are twice as wide.
        final float sizeScale = opts.circle ? 2f : 1f;
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            float cx = sCX[i];
            float cy = sCY[i];
            if (r >= 0.5f && cx >= 0 && cx <= width && cy >= 0 && cy <= height) {
                bodyBatcher.add(cx, cy, r * sizeScale, sCB[i]);
            }
        }
        bodyBatcher.setCap(opts.circle ? Paint.Cap.ROUND : Paint.Cap.SQUARE);
        bodyBatcher.flush(c);
    }

    // Trails and bodies are collected separately and flushed in two passes, so every trail lands under
    // every body. Before, each star drew its own trail then its own body, which left a later star's trail
    // on top of an earlier star's body wherever the two crossed.
    private void drawLoopTrails(Canvas c, int n, float width, float height) {
        final float[] sLX = stars.lastX;
        final float[] sLY = stars.lastY;
        final float[] sCX = stars.curX;
        final float[] sCY = stars.curY;
        final float[] sCR = stars.curRadius;
        final int[] sCB = stars.curBrightness;
        final float sizeScale = opts.circle ? 2f : 1f;
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            float lx = sLX[i];
            float ly = sLY[i];
            if (r >= 0.5f && lx >= 0 && lx <= width && ly >= 0 && ly <= height) {
                float cx = sCX[i];
                float cy = sCY[i];
                int b = sCB[i];
                float dx = lx - cx;
                float dy = ly - cy;
                if (dx * dx + dy * dy > 16f) {
                    trailBatcher.add(lx, ly, cx, cy, r, b);
                }
                bodyBatcher.add(cx, cy, r * sizeScale, b);
            }
        }
        trailBatcher.flush(c);
        bodyBatcher.setCap(opts.circle ? Paint.Cap.ROUND : Paint.Cap.SQUARE);
        bodyBatcher.flush(c);
    }

    public void clearScreenOffsets() {
        offsetTX = 0;
        offsetX = 0;
        offsetTY = 0;
        offsetY = 0;
    }

    public void clearOffsets() {
        clearScreenOffsets();
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

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private void randomStarPosition(int i, ThreadLocalRandom rng) {
        stars.x[i] = rng.nextFloat() * opts.width - opts.hW;
        stars.y[i] = rng.nextFloat() * opts.height - opts.hH;
        stars.v[i] = rng.nextFloat() * (opts.maxV - opts.minV) + opts.minV;
        stars.radius[i] = rng.nextFloat() * 2f + 1f;
        stars.lastX[i] = -1f;
        stars.lastY[i] = -1f;
        stars.curX[i] = -1f;
        stars.curY[i] = -1f;
    }
}
