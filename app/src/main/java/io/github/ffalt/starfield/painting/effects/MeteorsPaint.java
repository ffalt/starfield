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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import java.util.concurrent.ThreadLocalRandom;

import io.github.ffalt.starfield.StarfieldOpts;

public class MeteorsPaint {
    private final StarfieldOpts opts;
    private final Paint meteorPaint;
    private final Paint meteorFillPaint;
    private boolean[] meteorsActive = new boolean[0];
    private float[] meteorsX = new float[0];
    private float[] meteorsY = new float[0];
    private float[] meteorsVx = new float[0];
    private float[] meteorsVy = new float[0];
    private float[] meteorsLife = new float[0];
    private float[] meteorsInitLife = new float[0];
    private float[] meteorsLength = new float[0];
    private float[] meteorsDirX = new float[0];
    private float[] meteorsDirY = new float[0];
    private float[] meteorsTailLen = new float[0];
    private float speedModifier = 1.0f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private int cachedColorStartRGB;
    private static final float CORE_FRACTION = 0.25f;
    private static final float TAIL_WIDTH_FACTOR = 0.10f;
    private static final float HEAD_RADIUS_FACTOR = 0.09f;
    private static final int METEOR_SEGMENTS = 20;
    private static final int EDGE_POINTS = METEOR_SEGMENTS + 1;
    private static final float[] EDGE_T = new float[EDGE_POINTS];
    private static final float[] EDGE_HALF_WIDTH = new float[EDGE_POINTS];
    private static final float[] EDGE_ALPHA = new float[EDGE_POINTS];
    private static final float[] EDGE_COLOR_T = new float[EDGE_POINTS];
    private final int[] gradientColors = new int[EDGE_POINTS];
    private static final Path TAIL_PATH = new Path();
    private final Matrix gradientMatrix = new Matrix();
    private final Paint tailPaint;

    static {
        for (int j = 0; j < EDGE_POINTS; j++) {
            float t = (float) j / (float) METEOR_SEGMENTS;
            EDGE_T[j] = t;
            EDGE_HALF_WIDTH[j] = (0.95f * (1f - (float) Math.pow(t, 0.9)) + 0.05f) * 0.5f;
            float oneMinus = 1f - t;
            EDGE_ALPHA[j] = oneMinus * oneMinus * oneMinus;
            EDGE_COLOR_T[j] = (float) Math.pow(t, 0.7);
        }
        for (int j = 0; j < EDGE_POINTS; j++) {
            if (j == 0) {
                TAIL_PATH.moveTo(EDGE_T[j], EDGE_HALF_WIDTH[j]);
            } else {
                TAIL_PATH.lineTo(EDGE_T[j], EDGE_HALF_WIDTH[j]);
            }
        }
        for (int j = EDGE_POINTS - 1; j >= 0; j--) {
            TAIL_PATH.lineTo(EDGE_T[j], -EDGE_HALF_WIDTH[j]);
        }
        TAIL_PATH.close();
    }

    public MeteorsPaint(StarfieldOpts opts) {
        this.opts = opts;
        meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorPaint.setStyle(Paint.Style.STROKE);
        meteorPaint.setStrokeCap(Paint.Cap.ROUND);
        meteorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorFillPaint.setStyle(Paint.Style.FILL);
        tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setStyle(Paint.Style.FILL);
    }

    public void init() {
        int n = StarfieldOpts.METEOR_MAX_COUNT;
        meteorsActive = new boolean[n];
        meteorsX = new float[n];
        meteorsY = new float[n];
        meteorsVx = new float[n];
        meteorsVy = new float[n];
        meteorsLife = new float[n];
        meteorsInitLife = new float[n];
        meteorsLength = new float[n];
        meteorsDirX = new float[n];
        meteorsDirY = new float[n];
        meteorsTailLen = new float[n];
        this.refreshColors();
    }

    public void refreshColors() {
        int aRStart = (opts.meteorColorStart >> 16) & 0xFF;
        int aGStart = (opts.meteorColorStart >> 8) & 0xFF;
        int aBStart = opts.meteorColorStart & 0xFF;
        int aREnd = (opts.meteorColorEnd >> 16) & 0xFF;
        int aGEnd = (opts.meteorColorEnd >> 8) & 0xFF;
        int aBEnd = opts.meteorColorEnd & 0xFF;
        cachedColorStartRGB = opts.meteorColorStart & 0x00FFFFFF;
        for (int j = 0; j < EDGE_POINTS; j++) {
            float tColor = EDGE_COLOR_T[j];
            float inv = 1f - tColor;
            int rr = (int) (aRStart * inv + aREnd * tColor);
            int rg = (int) (aGStart * inv + aGEnd * tColor);
            int rb = (int) (aBStart * inv + aBEnd * tColor);
            int alpha = (int) (255f * EDGE_ALPHA[j]);
            gradientColors[j] = (alpha << 24) | (rr << 16) | (rg << 8) | rb;
        }
        tailPaint.setShader(new LinearGradient(0f, 0f, 1f, 0f, gradientColors, EDGE_T,
                Shader.TileMode.CLAMP));
    }

    public void move() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float ts = opts.timeScale;
        float spawnProb = opts.meteorSpawnProb * ts;
        if (spawnProb > 0f && rng.nextFloat() < spawnProb) {
            spawnFreeMeteor(rng);
        }
        final boolean[] active = meteorsActive;
        final float[] mx = meteorsX;
        final float[] my = meteorsY;
        final float[] mvx = meteorsVx;
        final float[] mvy = meteorsVy;
        final float[] ml = meteorsLife;
        float sm = speedModifier * ts;
        float w = opts.width;
        float h = opts.height;
        int n = active.length;
        for (int i = 0; i < n; i++) {
            if (!active[i]) {
                continue;
            }
            mx[i] += mvx[i] * sm;
            my[i] += mvy[i] * sm;
            ml[i] -= ts;
            float x = mx[i];
            float y = my[i];
            if (ml[i] <= 0f || x < -50f || x > w + 50f || y < -50f || y > h + 50f) {
                active[i] = false;
            }
        }
    }

    public void draw(Canvas c) {
        final boolean[] active = meteorsActive;
        int n = active.length;
        for (int i = 0; i < n; i++) {
            if (active[i]) {
                drawMeteor(c, i);
            }
        }
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private void drawMeteor(Canvas c, int i) {
        float initLife = meteorsInitLife[i];
        float lifeRatio = initLife > 0f ? meteorsLife[i] / initLife : 0f;
        if (lifeRatio < 0f) {
            lifeRatio = 0f;
        } else if (lifeRatio > 1f) {
            lifeRatio = 1f;
        }
        float lifeAlpha = lifeRatio * 255f;
        float mx = meteorsX[i];
        float my = meteorsY[i];
        float mlen = meteorsLength[i];
        float dirX = meteorsDirX[i];
        float dirY = meteorsDirY[i];
        float tailLen = meteorsTailLen[i];
        float baseStroke = mlen * TAIL_WIDTH_FACTOR;
        if (baseStroke < 1f) {
            baseStroke = 1f;
        } else if (baseStroke > 16f) {
            baseStroke = 16f;
        }

        gradientMatrix.setSinCos(dirY, dirX);
        gradientMatrix.preScale(tailLen, baseStroke);
        gradientMatrix.postTranslate(mx, my);
        tailPaint.setAlpha((int) lifeAlpha);
        c.save();
        c.concat(gradientMatrix);
        c.drawPath(TAIL_PATH, tailPaint);
        c.restore();
        float dx = dirX * tailLen;
        float dy = dirY * tailLen;
        meteorPaint.setColor(cachedColorStartRGB | ((int) lifeAlpha << 24));
        meteorPaint.setStrokeWidth(Math.max(1f, baseStroke * 0.35f));
        c.drawLine(mx, my, mx + dx * CORE_FRACTION, my + dy * CORE_FRACTION, meteorPaint);
        int headAlpha = Math.min(255, (int) (lifeAlpha * 1.05f));
        meteorFillPaint.setColor(cachedColorStartRGB | (headAlpha << 24));
        c.drawCircle(mx, my, Math.max(1f, Math.min(12f, mlen * HEAD_RADIUS_FACTOR)), meteorFillPaint);
    }

    private void spawnFreeMeteor(ThreadLocalRandom rng) {
        int n = meteorsActive.length;
        for (int i = 0; i < n; i++) {
            if (!meteorsActive[i]) {
                spawnMeteor(i, rng);
                return;
            }
        }
    }

    private void spawnMeteor(int i, ThreadLocalRandom rng) {
        float speedBase = rng.nextFloat() * 26f + 10f;
        float angle;
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0: // top
                meteorsX[i] = rng.nextFloat() * opts.width;
                meteorsY[i] = -10f;
                angle = (20f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            case 1: // right
                meteorsX[i] = opts.width + 10f;
                meteorsY[i] = rng.nextFloat() * opts.height;
                angle = (110f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            case 2: // bottom
                meteorsX[i] = rng.nextFloat() * opts.width;
                meteorsY[i] = opts.height + 10f;
                angle = (200f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            default: // left
                meteorsX[i] = -10f;
                meteorsY[i] = rng.nextFloat() * opts.height;
                angle = (-70f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
        }

        meteorsVx[i] = (float) Math.cos(angle) * speedBase;
        meteorsVy[i] = (float) Math.sin(angle) * speedBase;

        meteorsInitLife[i] = 40f + rng.nextFloat() * 80f;
        meteorsLife[i] = meteorsInitLife[i];
        meteorsLength[i] = 18f + rng.nextFloat() * 26f;
        meteorsDirX[i] = -(float) Math.cos(angle);
        meteorsDirY[i] = -(float) Math.sin(angle);
        meteorsTailLen[i] = speedBase * meteorsLength[i];
        meteorsActive[i] = true;
    }
}
