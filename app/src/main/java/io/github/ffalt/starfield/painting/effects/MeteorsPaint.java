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
    private static final int METEOR_SEGMENTS = 20;
    private static final float[] SEG_F1 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_FALLOFF = new float[METEOR_SEGMENTS];
    private static final float[] SEG_TMID_POW_07 = new float[METEOR_SEGMENTS];
    private static final float[] SEG_STROKE_FACTOR = new float[METEOR_SEGMENTS];
    private float speedModifier = 1.0f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private int cachedColorStartRGB;
    private final int[] segColorRGB = new int[METEOR_SEGMENTS];
    private static final float CORE_FRACTION = 0.25f;

    static {
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float f0 = (float) s / (float) METEOR_SEGMENTS;
            float f1 = (float) (s + 1) / (float) METEOR_SEGMENTS;
            float tMid = (f0 + f1) * 0.5f;
            SEG_F1[s] = f1;
            float oneMinus = 1f - tMid;
            SEG_FALLOFF[s] = oneMinus * oneMinus * oneMinus;
            float pow07 = (float) Math.pow(tMid, 0.7);
            float pow09 = (float) Math.pow(tMid, 0.9);
            SEG_TMID_POW_07[s] = pow07;
            SEG_STROKE_FACTOR[s] = 0.95f * (1f - pow09) + 0.05f;
        }
    }

    public MeteorsPaint(StarfieldOpts opts) {
        this.opts = opts;
        meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorPaint.setStyle(Paint.Style.STROKE);
        meteorPaint.setStrokeCap(Paint.Cap.ROUND);
        meteorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        meteorFillPaint.setStyle(Paint.Style.FILL);
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
        int aRStart = (opts.meteorColorStart >> 16) & 0xFF;
        int aGStart = (opts.meteorColorStart >> 8) & 0xFF;
        int aBStart = opts.meteorColorStart & 0xFF;
        int aREnd = (opts.meteorColorEnd >> 16) & 0xFF;
        int aGEnd = (opts.meteorColorEnd >> 8) & 0xFF;
        int aBEnd = opts.meteorColorEnd & 0xFF;
        cachedColorStartRGB = opts.meteorColorStart & 0x00FFFFFF;
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float tColor = SEG_TMID_POW_07[s];
            float inv = 1f - tColor;
            int rr = (int) (aRStart * inv + aREnd * tColor);
            int rg = (int) (aGStart * inv + aGEnd * tColor);
            int rb = (int) (aBStart * inv + aBEnd * tColor);
            segColorRGB[s] = (rr << 16) | (rg << 8) | rb;
        }
    }

    public void move() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // Skip the RNG call entirely when spawning is disabled (meteorSpawnProb == 0 is common).
        if (opts.meteorSpawnProb > 0f && rng.nextFloat() < opts.meteorSpawnProb) {
            spawnFreeMeteor(rng);
        }
        // Inline moveMeteor and cache array refs to avoid per-call overhead.
        final boolean[] active = meteorsActive;
        final float[] mx = meteorsX;
        final float[] my = meteorsY;
        final float[] mvx = meteorsVx;
        final float[] mvy = meteorsVy;
        final float[] ml = meteorsLife;
        float sm = speedModifier;
        float w = opts.width;
        float h = opts.height;
        int n = active.length;
        for (int i = 0; i < n; i++) {
            if (!active[i]) {
                continue;
            }
            mx[i] += mvx[i] * sm;
            my[i] += mvy[i] * sm;
            ml[i] -= 1f;
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
        float mx = meteorsX[i];
        float my = meteorsY[i];
        float mvx = meteorsVx[i];
        float mvy = meteorsVy[i];
        float mlen = meteorsLength[i];
        float initLife = meteorsInitLife[i];
        float lifeRatio = initLife > 0f ? meteorsLife[i] / initLife : 0f;
        if (lifeRatio < 0f) {
            lifeRatio = 0f;
        } else if (lifeRatio > 1f) {
            lifeRatio = 1f;
        }
        float lifeAlpha = lifeRatio * 255f;
        float dx = -mvx * mlen;
        float dy = -mvy * mlen;
        float baseStroke = mlen * 0.16f;
        if (baseStroke < 1f) {
            baseStroke = 1f;
        } else if (baseStroke > 16f) {
            baseStroke = 16f;
        }

        // Cache Paint refs and segment color array as locals to avoid repeated field reads.
        final Paint mp = meteorPaint;
        final int[] colors = segColorRGB;
        float x0 = mx;
        float y0 = my;
        for (int s = 0; s < METEOR_SEGMENTS; s++) {
            float f1 = SEG_F1[s];
            float x1 = mx + dx * f1;
            float y1 = my + dy * f1;
            int a = (int) (lifeAlpha * SEG_FALLOFF[s]);
            mp.setColor((a << 24) | colors[s]);
            mp.setStrokeWidth(baseStroke * SEG_STROKE_FACTOR[s]);
            c.drawLine(x0, y0, x1, y1, mp);
            x0 = x1;
            y0 = y1;
        }

        // thin bright core streak
        int coreA = (int) lifeAlpha;
        mp.setColor(cachedColorStartRGB | (coreA << 24));
        mp.setStrokeWidth(Math.max(1f, baseStroke * 0.35f));
        c.drawLine(mx, my, mx + dx * CORE_FRACTION, my + dy * CORE_FRACTION, mp);

        // head: filled circle
        final Paint mfp = meteorFillPaint;
        int headAlpha = Math.min(255, (int) (lifeAlpha * 1.05f));
        mfp.setColor(cachedColorStartRGB | (headAlpha << 24));
        c.drawCircle(mx, my, Math.max(1f, Math.min(12f, mlen * 0.14f)), mfp);
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
