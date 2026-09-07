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

public class ConstellationPaint {
    private static final float SIZE = 0.25f;
    private static final float DRIFT = 0.2f;
    private static final float RADIUS_SCALE = 1.6f;
    private static final float DEPTH_SCALE = 0.85f;
    private static final float FADE = 0.25f;
    private static final float DELAY_MIN = 300f;
    private static final float DELAY_MAX = 900f;
    private static final int LINE_ALPHA = 90;
    private static final float LINE_WIDTH = 0.5f;

    private final StarfieldOpts opts;
    private final ConstellationCatalog catalog;
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] x = new float[0];
    private float[] y = new float[0];
    private float[] radius = new float[0];
    private float[] screenX = new float[0];
    private float[] screenY = new float[0];
    private int[] starSegments = null;
    private int brightness = 0;
    private int count = 0;
    private int index = -1;
    private float z = 0;
    private float v = 0;
    private float centerX = 0;
    private float centerY = 0;
    private float delay = 0;

    public ConstellationPaint(StarfieldOpts opts, ConstellationCatalog catalog) {
        this.opts = opts;
        this.catalog = catalog;
        linePaint.setStyle(Paint.Style.STROKE);
    }

    public int getMaxStars() {
        return catalog.getMaxStars();
    }

    public void init() {
        int n = catalog.getMaxStars();
        x = new float[n];
        y = new float[n];
        radius = new float[n];
        screenX = new float[n];
        screenY = new float[n];
        count = 0;
        spawn(ThreadLocalRandom.current());
    }

    public int move(DrawList draws, int visible, float ts, float speedFactor, float trailDz,
                    float totalOffsetX, float totalOffsetY, float trailOffsetX, float trailOffsetY) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (delay > 0f) {
            delay -= ts;
            if (delay > 0f) {
                return visible;
            }
            delay = 0f;
            spawn(rng);
        }
        float nextZ = z - v * speedFactor;
        if (nextZ <= 0f) {
            count = 0;
            brightness = 0;
            delay = DELAY_MIN + rng.nextFloat() * (DELAY_MAX - DELAY_MIN);
            return visible;
        }
        z = nextZ;
        float zRatio = nextZ / opts.initialZ;
        float fade = (1f - zRatio) / FADE;
        if (fade > 1f) {
            fade = 1f;
        }
        int b = (int) (100 * fade + 0.5f);
        if (b <= 0) {
            brightness = 0;
            return visible;
        }
        b = Math.min(b, 100);
        brightness = b;
        float width = opts.width;
        float height = opts.height;
        float hW = opts.hW;
        float hH = opts.hH;
        float starSize = opts.starSize;
        float projection = width / nextZ;
        float trailProjection = width / (nextZ + v * trailDz);
        final boolean trails = opts.trails;
        final float[] cX = x;
        final float[] cY = y;
        final float[] cR = radius;
        final float[] cSX = screenX;
        final float[] cSY = screenY;
        final float depthScale = opts.constellationsLarge ? DEPTH_SCALE : 1f;
        final float offX = centerX;
        final float offY = centerY;
        final float[] dX = draws.x;
        final float[] dY = draws.y;
        final float[] dR = draws.radius;
        final int[] dB = draws.brightness;
        final float[] dTX = draws.trailX;
        final float[] dTY = draws.trailY;
        int n = count;
        for (int i = 0; i < n; i++) {
            float px = cX[i] + offX;
            float py = cY[i] + offY;
            float cx = hW + (px * projection - totalOffsetX);
            float cy = hH + (py * projection - totalOffsetY);
            cSX[i] = cx;
            cSY[i] = cy;
            float r = (1f - zRatio * depthScale) * cR[i] * starSize;
            if (r < 0.5f) {
                continue;
            }
            if (trails) {
                float lx = hW + (px * trailProjection - trailOffsetX);
                float ly = hH + (py * trailProjection - trailOffsetY);
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
            dB[visible] = b;
            visible++;
        }
        return visible;
    }

    public void drawLines(Canvas c, Paint[] starPaints) {
        final int[] seg = starSegments;
        if (seg == null || seg.length == 0 || count == 0 || brightness <= 0) {
            return;
        }
        final Paint p = linePaint;
        p.setColor(starPaints[brightness].getColor());
        p.setAlpha(LINE_ALPHA);
        p.setStrokeWidth(Math.max(1f, opts.starSize * LINE_WIDTH));
        final float[] cSX = screenX;
        final float[] cSY = screenY;
        for (int i = 0; i < seg.length; i += 2) {
            int from = seg[i];
            int to = seg[i + 1];
            c.drawLine(cSX[from], cSY[from], cSX[to], cSY[to], p);
        }
    }

    private void spawn(ThreadLocalRandom rng) {
        int total = catalog.size();
        if (total == 0) {
            return;
        }
        int next = rng.nextInt(index < 0 ? total : total - 1);
        if (index >= 0 && next >= index) {
            next++;
        }
        index = next;
        count = catalog.project(index, x, y, radius);
        starSegments = catalog.getSegments(index);
        float scale = SIZE * Math.min(opts.hW, opts.hH);
        float radiusScale = opts.constellationsLarge ? RADIUS_SCALE : 1f;
        for (int i = 0; i < count; i++) {
            x[i] *= scale;
            y[i] *= scale;
            radius[i] *= radiusScale;
        }
        centerX = (rng.nextFloat() * 2f - 1f) * opts.hW * DRIFT;
        centerY = (rng.nextFloat() * 2f - 1f) * opts.hH * DRIFT;
        z = opts.initialZ;
        v = opts.minV + (opts.maxV - opts.minV) * 0.5f;
    }
}
