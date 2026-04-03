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

import java.util.Random;

import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.painting.cache.StarPaintCache;
import io.github.ffalt.starfield.painting.cache.StarTrailPaintCache;

public class StarsPaint {
    private final StarfieldOpts opts;
    private final Random rng = new Random();
    private float[] starsX = new float[0];
    private float[] starsY = new float[0];
    private float[] starsZ = new float[0];
    private float[] starsV = new float[0];
    private float[] starsRadius = new float[0];
    private float[] starsLastX = new float[0];
    private float[] starsLastY = new float[0];
    private float[] starsCurrentX = new float[0];
    private float[] starsCurrentY = new float[0];
    private float[] starsCurrentRadius = new float[0];
    private int[] starsCurrentBrightness = new int[0];
    private final StarPaintCache starPaints;
    private final StarTrailPaintCache starTrailPaints;
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetTX = 0;
    private float offsetTY = 0;
    private float tiltOffsetX = 0;
    private float tiltOffsetY = 0;
    private float tiltTargetX = 0;
    private float tiltTargetY = 0;
    private float speedModifier = 1.0f;
    private static final float MAX_TILT_ANGLE = (float) Math.toRadians(50.0);

    public StarsPaint(StarfieldOpts opts) {
        this.opts = opts;
        starPaints = new StarPaintCache(opts);
        starTrailPaints = new StarTrailPaintCache(opts);
    }

    public void init() {
        int n = opts.numStars;
        starsX = new float[n];
        starsY = new float[n];
        starsZ = new float[n];
        starsV = new float[n];
        starsRadius = new float[n];
        starsLastX = new float[n];
        starsLastY = new float[n];
        starsCurrentX = new float[n];
        starsCurrentY = new float[n];
        starsCurrentRadius = new float[n];
        starsCurrentBrightness = new int[n];

        for (int i = 0; i < n; i++) {
            starsZ[i] = 0f;
            starsCurrentRadius[i] = 0f;
            starsCurrentBrightness[i] = 0;
            randomStarPosition(i);
            starsZ[i] = rng.nextFloat() * opts.initialZ;
        }
    }

    public void move() {
        if (opts.followScreen) {
            float dx = offsetTX - offsetX;
            if (dx > 0.01f || dx < -0.01f) {
                offsetX += dx * 0.01f;
            }
            float dy = offsetTY - offsetY;
            if (dy > 0.01f || dy < -0.01f) {
                offsetY += dy * 0.01f;
            }
            if (opts.followRestore) {
                // slowly move offsetTY and offsetTX back to zero
                offsetTX -= offsetTX * 0.01f;
                offsetTY -= offsetTY * 0.01f;
            }
        }
        if (opts.followSensor) {
            float tx = tiltTargetX - tiltOffsetX;
            if (tx > 0.01f || tx < -0.01f) {
                tiltOffsetX += tx * 0.01f;
            }
            float ty = tiltTargetY - tiltOffsetY;
            if (ty > 0.01f || ty < -0.01f) {
                tiltOffsetY += ty * 0.01f;
            }
        }
        // Cache commonly used opts values for the hot loop to avoid repeated field access
        float totalOffsetX = offsetX + tiltOffsetX;
        float totalOffsetY = offsetY + tiltOffsetY;
        float hW = opts.hW;
        float hH = opts.hH;
        float width = opts.width;
        float height = opts.height;
        float initialZ = opts.initialZ;
        float starSize = opts.starSize;

        int n = starsX.length;
        for (int i = 0; i < n; i++) {
            moveStar(i, totalOffsetX, totalOffsetY, hW, hH, width, height, initialZ, starSize);
        }
    }

    public void draw(Canvas c) {
        float width = opts.width;
        float height = opts.height;
        int n = starsX.length;
        for (int i = 0; i < n; i++) {
            drawStar(c, i, width, height);
        }
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
        float nPitch = clampedPitch / MAX_TILT_ANGLE;
        float nRoll = clampedRoll / MAX_TILT_ANGLE;
        float targetX = -nRoll * opts.width;
        float targetY = nPitch * opts.height;
        float intensity = opts.followSensorIntensity / 100f;
        tiltTargetX += (targetX - tiltTargetX) * intensity;
        tiltTargetY += (targetY - tiltTargetY) * intensity;
    }

    public void setOffsets(float diffX, float diffY) {
        float scale = 0.25f;
        if (opts.followRestore) {
            scale = 1f;
        }
        float intensity = opts.followScreenIntensity / 10f;
        offsetTX += diffX * scale * intensity;
        offsetTY += diffY * scale * intensity;
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private static float mapNumberToRange(float input, float inputRangeMax, float outputRangeMax) {
        return (input * outputRangeMax / inputRangeMax);
    }

    private static float roundToPrecision(float value) {
        int v = (int) (value * 100f + 0.5f);
        return v / 100f;
    }

    private void moveStar(int i, float totalOffsetX, float totalOffsetY, float hW, float hH, float width, float height, float initialZ, float starSize) {
        // Move along Z
        starsZ[i] -= (starsV[i] * 0.1f * speedModifier);
        if (starsZ[i] <= 0f) {
            resetStar(i);
            return; // resetStar will reinitialize values for this index
        } else {
            starsLastX[i] = starsCurrentX[i];
            starsLastY[i] = starsCurrentY[i];
            // speed up
            starsV[i] += 0.001f;
        }

        // Update x and y
        starsCurrentX[i] = hW + (width * (starsX[i] / starsZ[i]) - totalOffsetX);
        starsCurrentY[i] = hH + (height * (starsY[i] / starsZ[i]) - totalOffsetY);

        // Calculate a new radius based on Z
        starsCurrentRadius[i] = (1 - mapNumberToRange(starsZ[i], initialZ, 1)) * starsRadius[i] * starSize;

        // Calculate a new brightness based on Z
        float mapped = mapNumberToRange(starsZ[i], initialZ, 40);
        int bidx = 100 - (int) (mapped + 0.5f);
        starsCurrentBrightness[i] = bidx;
    }

    private void drawStar(Canvas c, int i, float width, float height) {
        float lastX = starsLastX[i];
        float lastY = starsLastY[i];
        if (lastX < 0 || lastX > width || lastY < 0 || lastY > height) {
            return;
        }
        float currentX = starsCurrentX[i];
        float currentY = starsCurrentY[i];
        int currentBrightness = starsCurrentBrightness[i];
        float currentRadius = starsCurrentRadius[i];
        if (opts.trails) {
            float dx = lastX - currentX;
            float dy = lastY - currentY;
            float distanceSquared = dx * dx + dy * dy;
            // avoid costly sqrt by comparing squared distance
            if (distanceSquared > 16f) { // equivalent to distance > 4f
                Paint trailPaint = starTrailPaints.get(currentBrightness);
                trailPaint.setStrokeWidth(currentRadius);
                c.drawLine(lastX, lastY, currentX, currentY, trailPaint);
            }
        }
        Paint starPaint = starPaints.get(currentBrightness);
        if (opts.circle) {
            c.drawCircle(currentX, currentY, currentRadius, starPaint);
        } else {
            float newRadiusH = currentRadius / 2f;
            c.drawRect(
                    currentX - newRadiusH, currentY - newRadiusH,
                    currentX + newRadiusH, currentY + newRadiusH, starPaint
            );
        }
    }

    private void resetStar(int i) {
        randomStarPosition(i);
        starsZ[i] = opts.initialZ;
    }

    private void randomStarPosition(int i) {
        starsX[i] = rng.nextFloat() * opts.width - opts.hW;
        starsY[i] = rng.nextFloat() * opts.height - opts.hH;
        starsV[i] = rng.nextFloat() * (opts.maxV - opts.minV) + opts.minV;
        starsRadius[i] = roundToPrecision(rng.nextFloat() * 2f + 1f);
        starsLastX[i] = -1f;
        starsLastY[i] = -1f;
        starsCurrentX[i] = -1f;
        starsCurrentY[i] = -1f;
    }
}
