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

import android.graphics.Canvas;
import android.graphics.Paint;

import io.github.ffalt.starfield.StarfieldOpts;
import io.github.ffalt.starfield.painting.cache.StarPaintCache;
import io.github.ffalt.starfield.painting.cache.StarTrailPaintCache;

public class Starfield {
    private float offsetX = 0;
    private float offsetY = 0;
    private float offsetTX = 0;
    private float offsetTY = 0;
    private final StarPaintCache starPaints;
    private final StarTrailPaintCache starTrailPaints;
    private final StarfieldOpts opts;
    // we don't use objects for the stars to save some memory
    private float[][] stars = new float[0][11];
    public static final int INDEX_x = 0;
    public static final int INDEX_y = 1;
    public static final int INDEX_z = 2;
    public static final int INDEX_v = 3;
    public static final int INDEX_radius = 4;
    public static final int INDEX_lastX = 5;
    public static final int INDEX_lastY = 6;
    public static final int INDEX_currentX = 7;
    public static final int INDEX_currentY = 8;
    public static final int INDEX_currentRadius = 9;
    public static final int INDEX_currentBrightness = 10;

    public Starfield(StarfieldOpts opts) {
        this.opts = opts;
        starPaints = new StarPaintCache(opts);
        starTrailPaints = new StarTrailPaintCache(opts);
        this.init();
    }

    private void init() {
        stars = new float[opts.numStars][11];
        for (int i = 0, l = stars.length; i < l; i++) {
            float[] star = stars[i];
            star[INDEX_z] = 0;
            star[INDEX_currentRadius] = 0;
            star[INDEX_currentBrightness] = 0;
            randomStarPosition(i);
            star[INDEX_z] = (float) (Math.random() * opts.initialZ);
        }
    }

    public void move() {
        if (opts.followScreen && Math.abs(offsetX - offsetTX) > 0.1) {
            offsetX += (float) ((offsetTX - offsetX) * 0.1);
            offsetY += (float) ((offsetTY - offsetY) * 0.1);
        }
        for (int i = 0, l = stars.length; i < l; i++) {
            moveStar(i);
        }
    }

    public void draw(Canvas c) {
        for (int i = 0, l = stars.length; i < l; i++) {
            drawStar(c, i);
        }
    }

    public void clearOffsets() {
        offsetTX = 0;
        offsetX = 0;
        offsetTY = 0;
        offsetY = 0;
    }

    public void setOffsets(float diffX, float diffY) {
        if (opts.followRestore) {
            offsetTX = diffX * 20;
            offsetTY = diffY * 20;
        } else {
            offsetTX += diffX;
            offsetTY += diffY;
        }
    }

    private static float mapNumberToRange(float input, float inputRangeMax, float outputRangeMax) {
        return (input * outputRangeMax / inputRangeMax);
    }

    public static float roundToPrecision(float value) {
        return (Math.round(value * 100) / 100f);
    }

    public void moveStar(int i) {
        float[] star = stars[i];
        final float speedModifier = 0.1f;
        star[INDEX_z] -= (star[INDEX_v] * speedModifier);
        if (star[INDEX_z] <= 0) {
            resetStar(i);
        } else {
            star[INDEX_lastX] = star[INDEX_currentX];
            star[INDEX_lastY] = star[INDEX_currentY];
            // speed up
            star[INDEX_v] += 0.001f;
        }
        // Update x and y
        star[INDEX_currentX] = opts.hW + (opts.W * (star[INDEX_x] / star[INDEX_z]) - offsetX);
        star[INDEX_currentY] = opts.hH + (opts.H * (star[INDEX_y] / star[INDEX_z]) - offsetY);
        // Calculate a new radius based on Z
        star[INDEX_currentRadius] =
                (1 - mapNumberToRange(star[INDEX_z], opts.initialZ, 1))
                        * star[INDEX_radius] * opts.starSize;
        // Calculate a new brightness based on Z
        star[INDEX_currentBrightness] = 100 -
                Math.round(mapNumberToRange(star[INDEX_z], opts.initialZ, 40));
    }

    public void drawStar(Canvas c, int i) {
        float[] star = stars[i];
        float lastX = star[INDEX_lastX];
        float lastY = star[INDEX_lastY];
        if (lastX < 0 || lastX > opts.W || lastY < 0 || lastY > opts.H) {
            return;
        }
        float currentX = star[INDEX_currentX];
        float currentY = star[INDEX_currentY];
        float currentBrightness = star[INDEX_currentBrightness];
        float currentRadius = star[INDEX_currentRadius];
        if (opts.trails) {
            float distance = (float) Math.sqrt(Math.pow(lastX - currentX, 2) + Math.pow(lastY - currentY, 2));
            if (Math.abs(distance) > 4f) {
                Paint trailPaint = starTrailPaints.get((int) currentBrightness);
                trailPaint.setStrokeWidth(currentRadius);
                c.drawLine(lastX, lastY, currentX, currentY, trailPaint);
            }
        }
        Paint starPaint = starPaints.get((int) currentBrightness);
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

    public void resetStar(int i) {
        randomStarPosition(i);
        stars[i][INDEX_z] = opts.initialZ;
    }

    public void randomStarPosition(int i) {
        float[] star = stars[i];
        star[INDEX_x] = (float) (Math.random() * opts.W) - opts.hW;
        star[INDEX_y] = (float) (Math.random() * opts.H) - opts.hH;
        star[INDEX_v] = (float) (Math.random() * (opts.maxV - opts.minV) + opts.minV);
        star[INDEX_radius] = roundToPrecision((float) (Math.random() * 2 + 1));
        star[INDEX_lastX] = -1;
        star[INDEX_lastY] = -1;
        star[INDEX_currentX] = -1;
        star[INDEX_currentY] = -1;
    }
}