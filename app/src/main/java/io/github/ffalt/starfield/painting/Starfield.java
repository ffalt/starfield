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
    private float tiltOffsetX = 0;
    private float tiltOffsetY = 0;
    private float tiltTargetX = 0;
    private float tiltTargetY = 0;
    private float speedModifier = 1.0f;

    private static final float maxTiltAngle = (float) Math.toRadians(50.0);

    private final StarfieldOpts opts;

    private final StarPaintCache starPaints;
    private final StarTrailPaintCache starTrailPaints;
    private float[][] stars = new float[0][11];
    public static final int STARS_INDEX_X = 0;
    public static final int STARS_INDEX_Y = 1;
    public static final int STARS_INDEX_Z = 2;
    public static final int STARS_INDEX_V = 3;
    public static final int STARS_INDEX_RADIUS = 4;
    public static final int STARS_INDEX_LAST_X = 5;
    public static final int STARS_INDEX_LAST_Y = 6;
    public static final int STARS_INDEX_CURRENT_X = 7;
    public static final int STARS_INDEX_CURRENT_Y = 8;
    public static final int STARS_INDEX_CURRENT_RADIUS = 9;
    public static final int STARS_INDEX_CURRENT_BRIGHTNESS = 10;

    // meteors
    private final Paint meteorPaint;
    private float[][] meteors = new float[0][8];
    public static final int METEORS_INDEX_ACTIVE = 0; // 1=active (0/1)
    public static final int METEORS_INDEX_X = 1;
    public static final int METEORS_INDEX_Y = 2;
    public static final int METEORS_INDEX_VX = 3;
    public static final int METEORS_INDEX_VY = 4;
    public static final int METEORS_INDEX_LIFE = 5;
    public static final int METEORS_INDEX_INIT_LIFE = 6;
    public static final int METEORS_INDEX_LENGTH = 7;
    private static final int METEOR_SEGMENTS = 20;
    private static final float[] SEG_F0 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_F1 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_FALLOFF = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_07 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_09 = new float[METEOR_SEGMENTS];
    static {
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float f0 = (float) s / (float) METEOR_SEGMENTS;
            float f1 = (float) (s + 1) / (float) METEOR_SEGMENTS;
            float tMid = (f0 + f1) * 0.5f;
            SEG_F0[s] = f0;
            SEG_F1[s] = f1;
            // precompute (1 - tMid)^3
            SEG_FALLOFF[s] = (float) Math.pow(1f - tMid, 3.0);
            // precompute tMid^0.7 and tMid^0.9 used for color/stroke
            SEG_TMID_POW_07[s] = (float) Math.pow(tMid, 0.7);
            SEG_TMID_POW_09[s] = (float) Math.pow(tMid, 0.9);
        }
    }

    public Starfield(StarfieldOpts opts) {
        this.opts = opts;
        starPaints = new StarPaintCache(opts);
        starTrailPaints = new StarTrailPaintCache(opts);
        meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorPaint.setStyle(Paint.Style.STROKE);
        meteorPaint.setStrokeCap(Paint.Cap.ROUND);
        this.init();
    }

    private void init() {
        initStars();
        if (opts.meteorsEnabled) {
            initMeteors();
        }
    }

    public void move() {
        moveStars();
        if (opts.meteorsEnabled) {
            moveMeteors();
        }
    }

    public void draw(Canvas c) {
        drawStars(c);
        if (opts.meteorsEnabled) {
            drawMeteors(c);
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
        return (Math.round(value * 100) / 100f);
    }

    private void initStars() {
        stars = new float[opts.numStars][11];
        for (int i = 0, l = stars.length; i < l; i++) {
            float[] star = stars[i];
            star[STARS_INDEX_Z] = 0;
            star[STARS_INDEX_CURRENT_RADIUS] = 0;
            star[STARS_INDEX_CURRENT_BRIGHTNESS] = 0;
            randomStarPosition(i);
            star[STARS_INDEX_Z] = (float) (Math.random() * opts.initialZ);
        }
    }

    private void moveStars() {
        if (opts.followScreen) {
            if (Math.abs(offsetX - offsetTX) > 0.1f) {
                offsetX += ((offsetTX - offsetX) * 0.1f);
            }
            if (Math.abs(offsetX - offsetTY) > 0.1f) {
                offsetY += ((offsetTY - offsetY) * 0.1f);
            }
        }
        if (opts.followSensor) {
            if (Math.abs(tiltOffsetX - tiltTargetX) > 0.01f) {
                tiltOffsetX += (tiltTargetX - tiltOffsetX) * 0.01f;
            }
            if (Math.abs(tiltOffsetY - tiltTargetY) > 0.01f) {
                tiltOffsetY += (tiltTargetY - tiltOffsetY) * 0.01f;
            }
        }
        for (int i = 0, l = stars.length; i < l; i++) {
            moveStar(i);
        }
    }

    private void drawStars(Canvas c) {
        for (int i = 0, l = stars.length; i < l; i++) {
            drawStar(c, i);
        }
    }

    private void moveStar(int i) {
        float[] star = stars[i];
        star[STARS_INDEX_Z] -= (star[STARS_INDEX_V] * 0.1f * speedModifier);
        if (star[STARS_INDEX_Z] <= 0) {
            resetStar(i);
        } else {
            star[STARS_INDEX_LAST_X] = star[STARS_INDEX_CURRENT_X];
            star[STARS_INDEX_LAST_Y] = star[STARS_INDEX_CURRENT_Y];
            // speed up
            star[STARS_INDEX_V] += 0.001f;
        }
        // Update x and y
        float totalOffsetX = offsetX + tiltOffsetX;
        float totalOffsetY = offsetY + tiltOffsetY;
        star[STARS_INDEX_CURRENT_X] = opts.hW + (opts.W * (star[STARS_INDEX_X] / star[STARS_INDEX_Z]) - totalOffsetX);
        star[STARS_INDEX_CURRENT_Y] = opts.hH + (opts.H * (star[STARS_INDEX_Y] / star[STARS_INDEX_Z]) - totalOffsetY);
        // Calculate a new radius based on Z
        star[STARS_INDEX_CURRENT_RADIUS] =
                (1 - mapNumberToRange(star[STARS_INDEX_Z], opts.initialZ, 1))
                        * star[STARS_INDEX_RADIUS] * opts.starSize;
        // Calculate a new brightness based on Z
        star[STARS_INDEX_CURRENT_BRIGHTNESS] = 100 -
                Math.round(mapNumberToRange(star[STARS_INDEX_Z], opts.initialZ, 40));
    }

    private void drawStar(Canvas c, int i) {
        float[] star = stars[i];
        float lastX = star[STARS_INDEX_LAST_X];
        float lastY = star[STARS_INDEX_LAST_Y];
        if (lastX < 0 || lastX > opts.W || lastY < 0 || lastY > opts.H) {
            return;
        }
        float currentX = star[STARS_INDEX_CURRENT_X];
        float currentY = star[STARS_INDEX_CURRENT_Y];
        float currentBrightness = star[STARS_INDEX_CURRENT_BRIGHTNESS];
        float currentRadius = star[STARS_INDEX_CURRENT_RADIUS];
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

    private void resetStar(int i) {
        randomStarPosition(i);
        stars[i][STARS_INDEX_Z] = opts.initialZ;
    }

    private void randomStarPosition(int i) {
        float[] star = stars[i];
        star[STARS_INDEX_X] = (float) (Math.random() * opts.W) - opts.hW;
        star[STARS_INDEX_Y] = (float) (Math.random() * opts.H) - opts.hH;
        star[STARS_INDEX_V] = (float) (Math.random() * (opts.maxV - opts.minV) + opts.minV);
        star[STARS_INDEX_RADIUS] = roundToPrecision((float) (Math.random() * 2 + 1));
        star[STARS_INDEX_LAST_X] = -1;
        star[STARS_INDEX_LAST_Y] = -1;
        star[STARS_INDEX_CURRENT_X] = -1;
        star[STARS_INDEX_CURRENT_Y] = -1;
    }

    private void initMeteors() {
        meteors = new float[StarfieldOpts.meteorMaxCount][8];
        for (int i = 0, l = meteors.length; i < l; i++) {
            meteors[i][METEORS_INDEX_ACTIVE] = 0;
            meteors[i][METEORS_INDEX_X] = 0f;
            meteors[i][METEORS_INDEX_Y] = 0f;
            meteors[i][METEORS_INDEX_VX] = 0f;
            meteors[i][METEORS_INDEX_VY] = 0f;
            meteors[i][METEORS_INDEX_LIFE] = 0f;
            meteors[i][METEORS_INDEX_INIT_LIFE] = 0f;
            meteors[i][METEORS_INDEX_LENGTH] = 0f;
        }
    }

    private void moveMeteor(int i) {
        float[] meteor = meteors[i];
        meteor[METEORS_INDEX_X] += meteor[METEORS_INDEX_VX] * speedModifier;
        meteor[METEORS_INDEX_Y] += meteor[METEORS_INDEX_VY] * speedModifier;
        meteor[METEORS_INDEX_LIFE] -= 1f;
        float mx = meteor[METEORS_INDEX_X];
        float my = meteor[METEORS_INDEX_Y];
        if (meteor[METEORS_INDEX_LIFE] <= 0f || mx < -50f || mx > opts.W + 50f || my < -50f || my > opts.H + 50f) {
            meteor[METEORS_INDEX_ACTIVE] = 0;
        }
    }

    private void moveMeteors() {
        if (Math.random() < opts.meteorSpawnProb) {
            spawnFreeMeteor();
        }
        for (int i = 0, l = meteors.length; i < l; i++) {
            if (meteors[i][METEORS_INDEX_ACTIVE] == 1) {
                moveMeteor(i);
            }
        }
    }

    private void drawMeteor(Canvas c, int i) {
        float[] meteor = meteors[i];
        // hoist repeated computations out of the inner loop
        float mx = meteor[METEORS_INDEX_X];
        float my = meteor[METEORS_INDEX_Y];
        float mvx = meteor[METEORS_INDEX_VX];
        float mvy = meteor[METEORS_INDEX_VY];
        float mlen = meteor[METEORS_INDEX_LENGTH];
        float lifeRatio = Math.max(0f, Math.min(1f, meteor[METEORS_INDEX_LIFE] / meteor[METEORS_INDEX_INIT_LIFE]));
        float tx = mx - mvx * mlen;
        float ty = my - mvy * mlen;
        float dx = tx - mx; // usually -mvx * mlen
        float dy = ty - my; // usually -mvy * mlen
        float baseStroke = Math.max(1f, Math.min(16f, mlen * 0.16f));

        // Use the precomputed segment lookup arrays to avoid Math.pow per-frame
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float f0 = SEG_F0[s];
            float f1 = SEG_F1[s];
            float x0 = mx + dx * f0;
            float y0 = my + dy * f0;
            float x1 = mx + dx * f1;
            float y1 = my + dy * f1;
            float falloff = SEG_FALLOFF[s];
            float segAlpha = lifeRatio * falloff;
            int a = (int) (Math.max(0f, Math.min(1f, segAlpha)) * 255f);
            float tColor = SEG_TMID_POW_07[s];
            int segColor = lerpColor(opts.meteorColorStart, opts.meteorColorEnd, tColor);
            meteorPaint.setColor((segColor & 0x00FFFFFF) | (a << 24));
            float stroke = baseStroke * (0.95f * (1f - SEG_TMID_POW_09[s]) + 0.05f);
            meteorPaint.setStrokeWidth(stroke);
            c.drawLine(x0, y0, x1, y1, meteorPaint);
        }

        // thin bright core streak from head into tail (short), gives a sharp core
        float coreLen = Math.max(2f, meteor[METEORS_INDEX_LENGTH] * 0.25f);
        float hx = meteor[METEORS_INDEX_X];
        float hy = meteor[METEORS_INDEX_Y];
        float cx = hx - meteor[METEORS_INDEX_VX] * (coreLen / (meteor[METEORS_INDEX_LENGTH] + 0.0001f));
        float cy = hy - meteor[METEORS_INDEX_VY] * (coreLen / (meteor[METEORS_INDEX_LENGTH] + 0.0001f));
        int coreA = (int) (Math.max(0f, Math.min(1f, lifeRatio)) * 255f);
        meteorPaint.setColor((opts.meteorColorStart & 0x00FFFFFF) | (coreA << 24));
        meteorPaint.setStrokeWidth(Math.max(1f, baseStroke * 0.35f));
        c.drawLine(hx, hy, cx, cy, meteorPaint);

        // head: filled circle with a bit more radius and opacity
        int headAlpha = (int) (Math.max(0f, Math.min(1f, lifeRatio * 1.05f)) * 255f);
        meteorPaint.setColor((opts.meteorColorStart & 0x00FFFFFF) | (headAlpha << 24));
        float headRadius = Math.max(1f, Math.min(12f, meteor[METEORS_INDEX_LENGTH] * 0.14f));
        meteorPaint.setStyle(Paint.Style.FILL);
        c.drawCircle(meteor[METEORS_INDEX_X], meteor[METEORS_INDEX_Y], headRadius, meteorPaint);
        meteorPaint.setStyle(Paint.Style.STROKE);
    }

    private void drawMeteors(Canvas c) {
        for (int i = 0, l = meteors.length; i < l; i++) {
            if (meteors[i][METEORS_INDEX_ACTIVE] == 1) {
                drawMeteor(c, i);
            }
        }
    }

    private void spawnFreeMeteor() {
        for (int i = 0; i < meteors.length; i++) {
            if (meteors[i][METEORS_INDEX_ACTIVE] == 0) {
                spawnMeteor(i);
                return;
            }
        }
    }

    private void spawnMeteor(int i) {
        float[] meteor = meteors[i];
        float speedBase = (float) (Math.random() * 26f + 10f * speedModifier);
        float angle;
        int edge = (int) (Math.random() * 4);
        switch (edge) {
            case 0: // top
                meteor[METEORS_INDEX_X] = (float) (Math.random() * opts.W);
                meteor[METEORS_INDEX_Y] = -10f;
                angle = (float) Math.toRadians(20 + Math.random() * 140);
                break;
            case 1: // right
                meteor[METEORS_INDEX_X] = opts.W + 10f;
                meteor[METEORS_INDEX_Y] = (float) (Math.random() * opts.H);
                angle = (float) Math.toRadians(110 + Math.random() * 140);
                break;
            case 2: // bottom
                meteor[METEORS_INDEX_X] = (float) (Math.random() * opts.W);
                meteor[METEORS_INDEX_Y] = opts.H + 10f;
                angle = (float) Math.toRadians(200 + Math.random() * 140);
                break;
            default: // left
                meteor[METEORS_INDEX_X] = -10f;
                meteor[METEORS_INDEX_Y] = (float) (Math.random() * opts.H);
                angle = (float) Math.toRadians(-70 + Math.random() * 140);
                break;
        }

        meteor[METEORS_INDEX_VX] = (float) Math.cos(angle) * speedBase;
        meteor[METEORS_INDEX_VY] = (float) Math.sin(angle) * speedBase;

        meteor[METEORS_INDEX_INIT_LIFE] = (float) (40 + Math.random() * 80);
        meteor[METEORS_INDEX_LIFE] = meteor[METEORS_INDEX_INIT_LIFE];
        meteor[METEORS_INDEX_LENGTH] = (18f + (float) (Math.random() * 26f));
        meteor[METEORS_INDEX_ACTIVE] = 1;
    }

    // Linear interpolate between two ARGB colors (a and b) by t in [0,1]
    private int lerpColor(int a, int b, float t) {
        float inv = 1f - t;
        int aa = (a >> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int ra = (int) (aa * inv + ba * t);
        int rr = (int) (ar * inv + br * t);
        int rg = (int) (ag * inv + bg * t);
        int rb = (int) (ab * inv + bb * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}

