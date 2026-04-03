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

public class MeteorsPaint {
    private final StarfieldOpts opts;
    private final Paint meteorPaint;
    private boolean[] meteorsActive = new boolean[0];
    private float[] meteorsX = new float[0];
    private float[] meteorsY = new float[0];
    private float[] meteorsVx = new float[0];
    private float[] meteorsVy = new float[0];
    private float[] meteorsLife = new float[0];
    private float[] meteorsInitLife = new float[0];
    private float[] meteorsLength = new float[0];
    private static final int METEOR_SEGMENTS = 20;
    private static final float[] SEG_F0 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_F1 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_FALLOFF = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_07 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_09 = new float[METEOR_SEGMENTS];
    private float speedModifier = 1.0f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private final Random rng = new Random();
    private int cachedARStart;
    private int cachedAGStart;
    private int cachedABStart;
    private int cachedAREnd;
    private int cachedAGEnd;
    private int cachedABEnd;

    static {
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float f0 = (float) s / (float) METEOR_SEGMENTS;
            float f1 = (float) (s + 1) / (float) METEOR_SEGMENTS;
            float tMid = (f0 + f1) * 0.5f;
            SEG_F0[s] = f0;
            SEG_F1[s] = f1;
            // precompute (1 - tMid)^3 without Math.pow
            float oneMinus = 1f - tMid;
            SEG_FALLOFF[s] = oneMinus * oneMinus * oneMinus;
            // precompute tMid^0.7 and tMid^0.9 used for color/stroke
            SEG_TMID_POW_07[s] = (float) Math.pow(tMid, 0.7);
            SEG_TMID_POW_09[s] = (float) Math.pow(tMid, 0.9);
        }
    }

    public MeteorsPaint(StarfieldOpts opts) {
        this.opts = opts;
        meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorPaint.setStyle(Paint.Style.STROKE);
        meteorPaint.setStrokeCap(Paint.Cap.ROUND);
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
        for (int i = 0; i < n; i++) {
            meteorsActive[i] = false;
            meteorsX[i] = 0f;
            meteorsY[i] = 0f;
            meteorsVx[i] = 0f;
            meteorsVy[i] = 0f;
            meteorsLife[i] = 0f;
            meteorsInitLife[i] = 0f;
            meteorsLength[i] = 0f;
        }
        this.refreshColors();
    }

    public void refreshColors() {
        cachedARStart = (opts.meteorColorStart >> 16) & 0xFF;
        cachedAGStart = (opts.meteorColorStart >> 8) & 0xFF;
        cachedABStart = opts.meteorColorStart & 0xFF;
        cachedAREnd = (opts.meteorColorEnd >> 16) & 0xFF;
        cachedAGEnd = (opts.meteorColorEnd >> 8) & 0xFF;
        cachedABEnd = opts.meteorColorEnd & 0xFF;
    }

    public void move() {
        if (rng.nextFloat() < opts.meteorSpawnProb) {
            spawnFreeMeteor();
        }
        int n = meteorsActive.length;
        for (int i = 0; i < n; i++) {
            if (meteorsActive[i]) {
                moveMeteor(i);
            }
        }
    }

    public void draw(Canvas c) {
        int n = meteorsActive.length;
        for (int i = 0; i < n; i++) {
            if (meteorsActive[i]) {
                drawMeteor(c, i);
            }
        }
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private void moveMeteor(int i) {
        meteorsX[i] += meteorsVx[i] * speedModifier;
        meteorsY[i] += meteorsVy[i] * speedModifier;
        meteorsLife[i] -= 1f;
        float mx = meteorsX[i];
        float my = meteorsY[i];
        if (meteorsLife[i] <= 0f || mx < -50f || mx > opts.width + 50f || my < -50f || my > opts.height + 50f) {
            meteorsActive[i] = false;
        }
    }

    private void drawMeteor(Canvas c, int i) {
        // hoist repeated computations out of the inner loop
        float mx = meteorsX[i];
        float my = meteorsY[i];
        float mvx = meteorsVx[i];
        float mvy = meteorsVy[i];
        float mlen = meteorsLength[i];
        float lifeRatio = meteorsLife[i] / (meteorsInitLife[i] + 1e-6f);
        if (lifeRatio < 0f) lifeRatio = 0f;
        else if (lifeRatio > 1f) lifeRatio = 1f;
        float tx = mx - mvx * mlen;
        float ty = my - mvy * mlen;
        float dx = tx - mx; // usually -mvx * mlen
        float dy = ty - my; // usually -mvy * mlen
        float baseStroke = mlen * 0.16f;
        if (baseStroke < 1f) baseStroke = 1f;
        else if (baseStroke > 16f) baseStroke = 16f;

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
            if (segAlpha < 0f) segAlpha = 0f;
            else if (segAlpha > 1f) segAlpha = 1f;
            int a = (int) (segAlpha * 255f);
            float tColor = SEG_TMID_POW_07[s];
            float inv = 1f - tColor;
            int rr = (int) (this.cachedARStart * inv + this.cachedAREnd * tColor);
            int rg = (int) (this.cachedAGStart * inv + this.cachedAGEnd * tColor);
            int rb = (int) (this.cachedABStart * inv + this.cachedABEnd * tColor);
            int segColor = (rr << 16) | (rg << 8) | rb;
            meteorPaint.setColor((segColor & 0x00FFFFFF) | (a << 24));
            float stroke = baseStroke * (0.95f * (1f - SEG_TMID_POW_09[s]) + 0.05f);
            meteorPaint.setStrokeWidth(stroke);
            c.drawLine(x0, y0, x1, y1, meteorPaint);
        }

        // thin bright core streak from head into tail (short), gives a sharp core
        float coreLen = meteorsLength[i] * 0.25f;
        if (coreLen < 2f) coreLen = 2f;
        float hx = meteorsX[i];
        float hy = meteorsY[i];
        float cx = hx - meteorsVx[i] * (coreLen / (meteorsLength[i] + 0.0001f));
        float cy = hy - meteorsVy[i] * (coreLen / (meteorsLength[i] + 0.0001f));
        int coreA = (int) (lifeRatio * 255f);
        meteorPaint.setColor((opts.meteorColorStart & 0x00FFFFFF) | (coreA << 24));
        meteorPaint.setStrokeWidth(Math.max(1f, baseStroke * 0.35f));
        c.drawLine(hx, hy, cx, cy, meteorPaint);

        // head: filled circle with a bit more radius and opacity
        float headRatio = lifeRatio * 1.05f;
        if (headRatio < 0f) headRatio = 0f;
        else if (headRatio > 1f) headRatio = 1f;
        int headAlpha = (int) (headRatio * 255f);
        meteorPaint.setColor((opts.meteorColorStart & 0x00FFFFFF) | (headAlpha << 24));
        float headRadius = Math.max(1f, Math.min(12f, meteorsLength[i] * 0.14f));
        meteorPaint.setStyle(Paint.Style.FILL);
        c.drawCircle(meteorsX[i], meteorsY[i], headRadius, meteorPaint);
        meteorPaint.setStyle(Paint.Style.STROKE);
    }

    private void spawnFreeMeteor() {
        int n = meteorsActive.length;
        for (int i = 0; i < n; i++) {
            if (!meteorsActive[i]) {
                spawnMeteor(i);
                return;
            }
        }
    }

    private void spawnMeteor(int i) {
        float speedBase = (rng.nextFloat() * 26f + 10f) * speedModifier;
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
        meteorsActive[i] = true;
    }
}
