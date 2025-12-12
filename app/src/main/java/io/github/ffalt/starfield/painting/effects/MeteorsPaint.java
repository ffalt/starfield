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
    private boolean[] meteors_active = new boolean[0];
    private float[] meteors_x = new float[0];
    private float[] meteors_y = new float[0];
    private float[] meteors_vx = new float[0];
    private float[] meteors_vy = new float[0];
    private float[] meteors_life = new float[0];
    private float[] meteors_init_life = new float[0];
    private float[] meteors_length = new float[0];
    private static final int METEOR_SEGMENTS = 20;
    private static final float[] SEG_F0 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_F1 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_FALLOFF = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_07 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_09 = new float[METEOR_SEGMENTS];
    private float speedModifier = 1.0f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private final Random rng = new Random();

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
        int n = StarfieldOpts.meteorMaxCount;
        meteors_active = new boolean[n];
        meteors_x = new float[n];
        meteors_y = new float[n];
        meteors_vx = new float[n];
        meteors_vy = new float[n];
        meteors_life = new float[n];
        meteors_init_life = new float[n];
        meteors_length = new float[n];
        for (int i = 0; i < n; i++) {
            meteors_active[i] = false;
            meteors_x[i] = 0f;
            meteors_y[i] = 0f;
            meteors_vx[i] = 0f;
            meteors_vy[i] = 0f;
            meteors_life[i] = 0f;
            meteors_init_life[i] = 0f;
            meteors_length[i] = 0f;
        }
    }

    public void move() {
        if (rng.nextFloat() < opts.meteorSpawnProb) {
            spawnFreeMeteor();
        }
        int n = meteors_active.length;
        for (int i = 0; i < n; i++) {
            if (meteors_active[i]) {
                moveMeteor(i);
            }
        }
    }

    public void draw(Canvas c) {
        int n = meteors_active.length;
        for (int i = 0; i < n; i++) {
            if (meteors_active[i]) {
                drawMeteor(c, i);
            }
        }
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private void moveMeteor(int i) {
        meteors_x[i] += meteors_vx[i] * speedModifier;
        meteors_y[i] += meteors_vy[i] * speedModifier;
        meteors_life[i] -= 1f;
        float mx = meteors_x[i];
        float my = meteors_y[i];
        if (meteors_life[i] <= 0f || mx < -50f || mx > opts.W + 50f || my < -50f || my > opts.H + 50f) {
            meteors_active[i] = false;
        }
    }

    private void drawMeteor(Canvas c, int i) {
        // hoist repeated computations out of the inner loop
        float mx = meteors_x[i];
        float my = meteors_y[i];
        float mvx = meteors_vx[i];
        float mvy = meteors_vy[i];
        float mlen = meteors_length[i];
        float lifeRatio = meteors_life[i] / (meteors_init_life[i] + 1e-6f);
        if (lifeRatio < 0f) lifeRatio = 0f;
        else if (lifeRatio > 1f) lifeRatio = 1f;
        float tx = mx - mvx * mlen;
        float ty = my - mvy * mlen;
        float dx = tx - mx; // usually -mvx * mlen
        float dy = ty - my; // usually -mvy * mlen
        float baseStroke = mlen * 0.16f;
        if (baseStroke < 1f) baseStroke = 1f;
        else if (baseStroke > 16f) baseStroke = 16f;

        // Precompute meteor color channels to avoid repeated bit ops in the inner loop
        int arStart = (opts.meteorColorStart >> 16) & 0xFF;
        int agStart = (opts.meteorColorStart >> 8) & 0xFF;
        int abStart = opts.meteorColorStart & 0xFF;
        int arEnd = (opts.meteorColorEnd >> 16) & 0xFF;
        int agEnd = (opts.meteorColorEnd >> 8) & 0xFF;
        int abEnd = opts.meteorColorEnd & 0xFF;

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
            int rr = (int) (arStart * inv + arEnd * tColor);
            int rg = (int) (agStart * inv + agEnd * tColor);
            int rb = (int) (abStart * inv + abEnd * tColor);
            int segColor = (rr << 16) | (rg << 8) | rb;
            meteorPaint.setColor((segColor & 0x00FFFFFF) | (a << 24));
            float stroke = baseStroke * (0.95f * (1f - SEG_TMID_POW_09[s]) + 0.05f);
            meteorPaint.setStrokeWidth(stroke);
            c.drawLine(x0, y0, x1, y1, meteorPaint);
        }

        // thin bright core streak from head into tail (short), gives a sharp core
        float coreLen = meteors_length[i] * 0.25f;
        if (coreLen < 2f) coreLen = 2f;
        float hx = meteors_x[i];
        float hy = meteors_y[i];
        float cx = hx - meteors_vx[i] * (coreLen / (meteors_length[i] + 0.0001f));
        float cy = hy - meteors_vy[i] * (coreLen / (meteors_length[i] + 0.0001f));
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
        float headRadius = Math.max(1f, Math.min(12f, meteors_length[i] * 0.14f));
        meteorPaint.setStyle(Paint.Style.FILL);
        c.drawCircle(meteors_x[i], meteors_y[i], headRadius, meteorPaint);
        meteorPaint.setStyle(Paint.Style.STROKE);
    }

    private void spawnFreeMeteor() {
        int n = meteors_active.length;
        for (int i = 0; i < n; i++) {
            if (!meteors_active[i]) {
                spawnMeteor(i);
                return;
            }
        }
    }

    private void spawnMeteor(int i) {
        float speedBase = rng.nextFloat() * 26f + 10f * speedModifier;
        float angle;
        int edge = rng.nextInt(4);
        switch (edge) {
            case 0: // top
                meteors_x[i] = rng.nextFloat() * opts.W;
                meteors_y[i] = -10f;
                angle = (20f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            case 1: // right
                meteors_x[i] = opts.W + 10f;
                meteors_y[i] = rng.nextFloat() * opts.H;
                angle = (110f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            case 2: // bottom
                meteors_x[i] = rng.nextFloat() * opts.W;
                meteors_y[i] = opts.H + 10f;
                angle = (200f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
            default: // left
                meteors_x[i] = -10f;
                meteors_y[i] = rng.nextFloat() * opts.H;
                angle = (-70f + rng.nextFloat() * 140f) * DEG_TO_RAD;
                break;
        }

        meteors_vx[i] = (float) Math.cos(angle) * speedBase;
        meteors_vy[i] = (float) Math.sin(angle) * speedBase;

        meteors_init_life[i] = 40f + rng.nextFloat() * 80f;
        meteors_life[i] = meteors_init_life[i];
        meteors_length[i] = 18f + rng.nextFloat() * 26f;
        meteors_active[i] = true;
    }
}
