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
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.concurrent.ThreadLocalRandom;

import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.painting.cache.StarPaintCache;
import io.github.ffalt.starfield.painting.cache.StarTrailPaintCache;

public class StarsPaint {
    private static final float MAX_TILT_ANGLE = (float) Math.toRadians(50.0);
    private static final float INV_MAX_TILT_ANGLE = 1f / MAX_TILT_ANGLE;
    private static final float SMOOTHING = 0.01f;

    private static final float TRAIL_MIN_LENGTH = 16f;
    private static final float TRAIL_WIDTH_CIRCLE = 1.5f;
    private static final float TRAIL_MIN_WIDTH_SQ = 2.89f;
    private static final float TRAIL_WINDOW_MS = 60f;
    private static final float TRAIL_WINDOW_FRAMES = TRAIL_WINDOW_MS * 60f / 1000f;


    private final StarfieldOpts opts;
    private final StarArrays stars = new StarArrays();
    private final DrawList draws = new DrawList();

    private final StarPaintCache starPaints;
    private final StarTrailPaintCache starTrailPaints;
    private final Matrix trailMatrix = new Matrix();
    private final float[] trailMatrixValues = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f};

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
    private float lastTotalOffsetX = 0;
    private float lastTotalOffsetY = 0;

    private final ConstellationPaint constellations;
    private boolean constellationsActive = false;

    private static final class StarArrays {
        private float[] x = new float[0];
        private float[] y = new float[0];
        private float[] z = new float[0];
        private float[] v = new float[0];
        private float[] radius = new float[0];

        private void alloc(int n) {
            x = new float[n];
            y = new float[n];
            z = new float[n];
            v = new float[n];
            radius = new float[n];
        }
    }

    public StarsPaint(StarfieldOpts opts, ConstellationPaint constellations) {
        this.opts = opts;
        this.constellations = constellations;
        starPaints = new StarPaintCache(opts);
        starTrailPaints = new StarTrailPaintCache(opts);
    }

    public void init() {
        int n = opts.numStars;
        int constellationStars = constellations == null ? 0 : constellations.getMaxStars();
        constellationsActive = constellationStars > 0;
        stars.alloc(n);
        draws.alloc(n + constellationStars);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            randomStarPosition(i, rng);
            stars.z[i] = rng.nextFloat() * opts.initialZ;
        }
        if (constellationsActive) {
            constellations.init();
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
        float totalOffsetX = offsetX + tiltOffsetX;
        float totalOffsetY = offsetY + tiltOffsetY;
        final boolean trails = opts.trails;
        float trailWindow = TRAIL_WINDOW_FRAMES * opts.trailIntensity * 0.01f;
        float trailDz = 0.1f * speedModifier * trailWindow;
        float trailFrames = trailWindow / ts;
        float trailOffsetX = totalOffsetX - (totalOffsetX - lastTotalOffsetX) * trailFrames;
        float trailOffsetY = totalOffsetY - (totalOffsetY - lastTotalOffsetY) * trailFrames;
        lastTotalOffsetX = totalOffsetX;
        lastTotalOffsetY = totalOffsetY;
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
        final float[] dX = draws.x;
        final float[] dY = draws.y;
        final float[] dR = draws.radius;
        final int[] dB = draws.brightness;
        final float[] dTX = draws.trailX;
        final float[] dTY = draws.trailY;
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        int n = sX.length;
        float vRange = opts.maxV - opts.minV;
        float minV = opts.minV;
        int visible = 0;
        for (int i = 0; i < n; i++) {
            float sz = sZ[i] - sV[i] * speedFactor;
            if (sz <= 0f) {
                // Inline randomStarPosition using cached locals to avoid field re-reads.
                sX[i] = rng.nextFloat() * width - hW;
                sY[i] = rng.nextFloat() * height - hH;
                sV[i] = rng.nextFloat() * vRange + minV;
                sR[i] = rng.nextFloat() * 2f + 1f;
                sZ[i] = initialZ;
                continue;
            }
            sZ[i] = sz;
            sV[i] += vGain;
            float zRatio = sz * invInitialZ;
            float r = (1 - zRatio) * sR[i] * starSize;
            if (r < 0.5f) {
                continue;
            }
            float invZ = 1f / sz;
            float cx = hW + (width * sX[i] * invZ - totalOffsetX);
            float cy = hH + (height * sY[i] * invZ - totalOffsetY);
            if (trails) {
                // The trail path culls on where the star was, so one leaving the screen still streaks.
                float invZT = 1f / (sz + sV[i] * trailDz);
                float lx = hW + (width * sX[i] * invZT - trailOffsetX);
                float ly = hH + (height * sY[i] * invZT - trailOffsetY);
                if (lx < 0f || lx > width || ly < 0f || ly > height) {
                    continue;
                }
                dTX[visible] = lx;
                dTY[visible] = ly;
            } else if (cx < 0f || cx > width || cy < 0f || cy > height) {
                continue;
            }
            dX[visible] = cx;
            dY[visible] = cy;
            dR[visible] = r;
            int b = 100 - (int) (zRatio * 40 + 0.5f);
            dB[visible] = b < 0 ? 0 : Math.min(b, 100);
            visible++;
        }
        if (constellationsActive) {
            visible = constellations.move(draws, visible, ts, speedFactor, trailDz,
                    totalOffsetX, totalOffsetY, trailOffsetX, trailOffsetY);
        }
        draws.count = visible;
    }

    public void draw(Canvas c) {
        if (constellationsActive && opts.constellationsLines) {
            constellations.drawLines(c, starPaints.getArray());
        }
        if (opts.trails) {
            drawLoopTrails(c);
        } else {
            drawLoopPoints(c);
        }
    }

    private void drawLoopPoints(Canvas c) {
        final float[] dX = draws.x;
        final float[] dY = draws.y;
        final float[] dR = draws.radius;
        final int[] dB = draws.brightness;
        final Paint[] sp = starPaints.getArray();
        final boolean circle = opts.circle;
        int n = draws.count;
        for (int i = 0; i < n; i++) {
            float r = dR[i];
            float cx = dX[i];
            float cy = dY[i];
            if (circle) {
                c.drawCircle(cx, cy, r, sp[dB[i]]);
            } else {
                float rH = r * 0.5f;
                c.drawRect(cx - rH, cy - rH, cx + rH, cy + rH, sp[dB[i]]);
            }
        }
    }

    private void drawLoopTrails(Canvas c) {
        final float[] dX = draws.x;
        final float[] dY = draws.y;
        final float[] dR = draws.radius;
        final int[] dB = draws.brightness;
        final float[] dTX = draws.trailX;
        final float[] dTY = draws.trailY;
        final Paint[] stp = starTrailPaints.getArray();
        final Bitmap tex = starTrailPaints.getBitmap();
        final float[] m = trailMatrixValues;
        final float invTexW = 1f / tex.getWidth();
        final float invTexH = 1f / tex.getHeight();
        final float widthScale = opts.circle ? TRAIL_WIDTH_CIRCLE : 1f;
        int n = draws.count;
        for (int i = 0; i < n; i++) {
            float cx = dX[i];
            float cy = dY[i];
            float dx = dTX[i] - cx;
            float dy = dTY[i] - cy;
            float length2 = dx * dx + dy * dy;
            float w = dR[i] * widthScale;
            // Skip trails shorter than 1.7 trail widths: they sit under the star head as a
            // smudge, costing a draw call and showing nothing.
            float minLength2 = w * w * TRAIL_MIN_WIDTH_SQ;
            if (minLength2 < TRAIL_MIN_LENGTH) {
                minLength2 = TRAIL_MIN_LENGTH;
            }
            if (length2 <= minLength2) {
                continue;
            }
            float length = (float) Math.sqrt(length2);
            // Map the sprite onto the trail: x spans head to tail, y is centred on the axis.
            float invLength = 1f / length;
            float ux = dx * invLength;
            float uy = dy * invLength;
            float scaleX = length * invTexW;
            float scaleY = w * invTexH;
            float halfW = w * 0.5f;
            m[0] = ux * scaleX;
            m[1] = -uy * scaleY;
            m[2] = cx + uy * halfW;
            m[3] = uy * scaleX;
            m[4] = ux * scaleY;
            m[5] = cy - ux * halfW;
            trailMatrix.setValues(m);
            c.drawBitmap(tex, trailMatrix, stp[dB[i]]);
        }
        drawLoopPoints(c);
    }

    public void clearScreenOffsets() {
        offsetTX = 0;
        offsetX = 0;
        offsetTY = 0;
        offsetY = 0;
        lastTotalOffsetX = tiltOffsetX;
        lastTotalOffsetY = tiltOffsetY;
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
    }
}
