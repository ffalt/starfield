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

public class ShapeBatcher {
    public static final int POINTS = 2;
    public static final int LINES = 4;

    private static final int BRIGHTNESS_LEVELS = 32;
    private static final int BRIGHTNESS_MIN_PERCENT = 60;
    private static final int BRIGHTNESS_PERCENT_RANGE = 40;
    private static final float BRIGHTNESS_LEVEL_STEP = BRIGHTNESS_PERCENT_RANGE / (float) (BRIGHTNESS_LEVELS - 1);
    private static final int SIZE_SLOTS = 64;
    private static final int SIZE_SHIFT = 6;
    private static final int SIZE_MASK = SIZE_SLOTS - 1;
    private static final float SIZE_MIN = 0.5f;
    private static final float SIZE_RATIO = 1.12f;
    private static final double INV_LOG_SIZE_RATIO = 1.0 / Math.log(SIZE_RATIO);
    private static final int BUCKET_COUNT = BRIGHTNESS_LEVELS * SIZE_SLOTS;
    private static final float[] SLOT_SIZE = new float[SIZE_SLOTS];

    static {
        for (int i = 0; i < SIZE_SLOTS; i++) {
            SLOT_SIZE[i] = (float) (SIZE_MIN * Math.pow(SIZE_RATIO, i));
        }
    }

    private final int stride;
    private final Paint[] levelPaints = new Paint[BRIGHTNESS_LEVELS];
    private final int[] bucketCount = new int[BUCKET_COUNT];
    private final int[] bucketCursor = new int[BUCKET_COUNT];
    private float[] pendingCoords = new float[0];
    private int[] pendingKey = new int[0];
    private float[] coords = new float[0];
    private int pending = 0;
    private Paint.Cap cap = null;

    public ShapeBatcher(Paint[] brightnessPaints, int stride) {
        this.stride = stride;
        for (int i = 0; i < BRIGHTNESS_LEVELS; i++) {
            Paint p = new Paint(brightnessPaints[BRIGHTNESS_MIN_PERCENT + Math.round(i * BRIGHTNESS_LEVEL_STEP)]);
            p.setStyle(Paint.Style.STROKE);
            levelPaints[i] = p;
        }
    }

    public void resize(int capacity) {
        if (pendingKey.length == capacity) {
            return;
        }
        pendingCoords = new float[capacity * stride];
        pendingKey = new int[capacity];
        coords = new float[capacity * stride];
        pending = 0;
    }

    public void setCap(Paint.Cap newCap) {
        if (newCap == cap) {
            return;
        }
        cap = newCap;
        for (int i = 0; i < BRIGHTNESS_LEVELS; i++) {
            levelPaints[i].setStrokeCap(newCap);
        }
    }

    public void add(float x, float y, float size, int brightness) {
        int i = pending;
        if (i >= pendingKey.length) {
            return;
        }
        int o = i * stride;
        pendingCoords[o] = x;
        pendingCoords[o + 1] = y;
        commit(i, size, brightness);
    }

    public void add(float x0, float y0, float x1, float y1, float size, int brightness) {
        int i = pending;
        if (i >= pendingKey.length) {
            return;
        }
        int o = i * stride;
        pendingCoords[o] = x0;
        pendingCoords[o + 1] = y0;
        pendingCoords[o + 2] = x1;
        pendingCoords[o + 3] = y1;
        commit(i, size, brightness);
    }

    private void commit(int i, float size, int brightness) {
        int level = (brightness - BRIGHTNESS_MIN_PERCENT) * (BRIGHTNESS_LEVELS - 1) / BRIGHTNESS_PERCENT_RANGE;
        if (level < 0) {
            level = 0;
        } else if (level >= BRIGHTNESS_LEVELS) {
            level = BRIGHTNESS_LEVELS - 1;
        }
        int slot = (int) (Math.log(size / SIZE_MIN) * INV_LOG_SIZE_RATIO + 0.5);
        if (slot < 0) {
            slot = 0;
        } else if (slot > SIZE_MASK) {
            slot = SIZE_MASK;
        }
        int key = (level << SIZE_SHIFT) | slot;
        pendingKey[i] = key;
        bucketCount[key]++;
        pending = i + 1;
    }

    public void flush(Canvas c) {
        if (pending == 0) {
            return;
        }
        final int[] counts = bucketCount;
        final int[] cursor = bucketCursor;
        final int s = stride;
        int total = 0;
        for (int k = 0; k < BUCKET_COUNT; k++) {
            cursor[k] = total;
            total += counts[k] * s;
        }
        final float[] dst = coords;
        final float[] src = pendingCoords;
        final int[] keys = pendingKey;
        for (int i = 0; i < pending; i++) {
            int key = keys[i];
            int o = cursor[key];
            int p = i * s;
            for (int j = 0; j < s; j++) {
                dst[o + j] = src[p + j];
            }
            cursor[key] = o + s;
        }
        for (int k = 0; k < BUCKET_COUNT; k++) {
            int count = counts[k];
            if (count == 0) {
                continue;
            }
            counts[k] = 0;
            int floats = count * s;
            int start = cursor[k] - floats;
            Paint p = levelPaints[k >>> SIZE_SHIFT];
            p.setStrokeWidth(SLOT_SIZE[k & SIZE_MASK]);
            if (s == POINTS) {
                c.drawPoints(dst, start, floats, p);
            } else {
                c.drawLines(dst, start, floats, p);
            }
        }
        pending = 0;
    }
}
