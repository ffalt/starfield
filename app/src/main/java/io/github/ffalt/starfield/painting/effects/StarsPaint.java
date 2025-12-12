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
    private float[] stars_x = new float[0];
    private float[] stars_y = new float[0];
    private float[] stars_z = new float[0];
    private float[] stars_v = new float[0];
    private float[] stars_radius = new float[0];
    private float[] stars_last_x = new float[0];
    private float[] stars_last_y = new float[0];
    private float[] stars_current_x = new float[0];
    private float[] stars_current_y = new float[0];
    private float[] stars_current_radius = new float[0];
    private float[] stars_current_brightness = new float[0];
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
     private static final float maxTiltAngle = (float) Math.toRadians(50.0);
     private float speedModifier = 1.0f;

     public StarsPaint(StarfieldOpts opts) {
         this.opts = opts;
         starPaints = new StarPaintCache(opts);
         starTrailPaints = new StarTrailPaintCache(opts);
     }

     public void init() {
         int n = opts.numStars;
         stars_x = new float[n];
         stars_y = new float[n];
         stars_z = new float[n];
         stars_v = new float[n];
         stars_radius = new float[n];
         stars_last_x = new float[n];
         stars_last_y = new float[n];
         stars_current_x = new float[n];
         stars_current_y = new float[n];
         stars_current_radius = new float[n];
         stars_current_brightness = new float[n];

         for (int i = 0; i < n; i++) {
             stars_z[i] = 0f;
             stars_current_radius[i] = 0f;
             stars_current_brightness[i] = 0f;
             randomStarPosition(i);
             stars_z[i] = rng.nextFloat() * opts.initialZ;
         }
     }

     public void move() {
         if (opts.followScreen) {
             float dx = offsetTX - offsetX;
             if (dx > 0.1f || dx < -0.1f) {
                 offsetX += dx * 0.1f;
             }
             float dy = offsetTY - offsetY;
             if (dy > 0.1f || dy < -0.1f) {
                 offsetY += dy * 0.1f;
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
         float W = opts.W;
         float H = opts.H;
         float initialZ = opts.initialZ;
         float starSize = opts.starSize;

         int n = stars_x.length;
         for (int i = 0; i < n; i++) {
             moveStar(i, totalOffsetX, totalOffsetY, hW, hH, W, H, initialZ, starSize);
         }
     }

     public void draw(Canvas c) {
         float W = opts.W;
         float H = opts.H;
         int n = stars_x.length;
         for (int i = 0; i < n; i++) {
             drawStar(c, i, W, H);
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
         float clampedPitch = Math.max(-maxTiltAngle, Math.min(maxTiltAngle, tiltX));
         float clampedRoll = Math.max(-maxTiltAngle, Math.min(maxTiltAngle, tiltY));
         float nPitch = clampedPitch / maxTiltAngle;
         float nRoll = clampedRoll / maxTiltAngle;
         float targetX = -nRoll * opts.W;
         float targetY = nPitch * opts.H;
         float intensity = opts.followSensorIntensity / 100f;
         tiltTargetX += (targetX - tiltTargetX) * intensity;
         tiltTargetY += (targetY - tiltTargetY) * intensity;
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

     private void moveStar(int i, float totalOffsetX, float totalOffsetY, float hW, float hH, float W, float H, float initialZ, float starSize) {
         // Move along Z
         stars_z[i] -= (stars_v[i] * 0.1f * speedModifier);
         if (stars_z[i] <= 0f) {
             resetStar(i);
             return; // resetStar will reinitialize values for this index
         } else {
             stars_last_x[i] = stars_current_x[i];
             stars_last_y[i] = stars_current_y[i];
             // speed up
             stars_v[i] += 0.001f;
         }

         // Update x and y
         stars_current_x[i] = hW + (W * (stars_x[i] / stars_z[i]) - totalOffsetX);
         stars_current_y[i] = hH + (H * (stars_y[i] / stars_z[i]) - totalOffsetY);

         // Calculate a new radius based on Z
         stars_current_radius[i] = (1 - mapNumberToRange(stars_z[i], initialZ, 1)) * stars_radius[i] * starSize;

         // Calculate a new brightness based on Z
         float mapped = mapNumberToRange(stars_z[i], initialZ, 40);
         int bidx = 100 - (int) (mapped + 0.5f);
         stars_current_brightness[i] = bidx;
     }

     private void drawStar(Canvas c, int i, float W, float H) {
         float lastX = stars_last_x[i];
         float lastY = stars_last_y[i];
         if (lastX < 0 || lastX > W || lastY < 0 || lastY > H) {
             return;
         }
         float currentX = stars_current_x[i];
         float currentY = stars_current_y[i];
         float currentBrightness = stars_current_brightness[i];
         float currentRadius = stars_current_radius[i];
         int bri = (int) currentBrightness; // reuse the integer brightness index
         if (opts.trails) {
             float dx = lastX - currentX;
             float dy = lastY - currentY;
             float distanceSquared = dx * dx + dy * dy;
             // avoid costly sqrt by comparing squared distance
             if (distanceSquared > 16f) { // equivalent to distance > 4f
                 Paint trailPaint = starTrailPaints.get(bri);
                 trailPaint.setStrokeWidth(currentRadius);
                 c.drawLine(lastX, lastY, currentX, currentY, trailPaint);
             }
         }
         Paint starPaint = starPaints.get(bri);
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
         stars_z[i] = opts.initialZ;
     }

     private void randomStarPosition(int i) {
         stars_x[i] = rng.nextFloat() * opts.W - opts.hW;
         stars_y[i] = rng.nextFloat() * opts.H - opts.hH;
         stars_v[i] = rng.nextFloat() * (opts.maxV - opts.minV) + opts.minV;
         stars_radius[i] = roundToPrecision(rng.nextFloat() * 2f + 1f);
         stars_last_x[i] = -1f;
         stars_last_y[i] = -1f;
         stars_current_x[i] = -1f;
         stars_current_y[i] = -1f;
     }
 }
