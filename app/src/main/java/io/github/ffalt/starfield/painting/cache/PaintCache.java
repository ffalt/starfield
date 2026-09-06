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

package io.github.ffalt.starfield.painting.cache;

import android.graphics.Color;
import android.graphics.Paint;

import io.github.ffalt.starfield.StarfieldOpts;

public class PaintCache {
    protected final Paint[] cache;
    protected final StarfieldOpts opts;
    private final float[] hsv = new float[3];

    public PaintCache(StarfieldOpts opts, int length) {
        this.opts = opts;
        cache = new Paint[length + 1];
    }

    protected final void buildCache() {
        for (int i = 0; i < cache.length; i++) {
            cache[i] = build(i);
        }
    }

    protected int adjustBrightness(int color, int index) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float scale = 0.01f * index;
        if (r == g && g == b) {
            // Fast path for achromatic (white/gray) colors: skip HSV round-trip.
            int v = Math.round(r * scale);
            return (color & 0xFF000000) | (v << 16) | (v << 8) | v;
        }
        Color.colorToHSV(color, hsv);
        hsv[2] *= scale;
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    public Paint build(int index) {
        return new Paint();
    }

    public Paint get(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= cache.length) {
            index = cache.length - 1;
        }
        return cache[index];
    }

    public Paint[] getArray() {
        return cache;
    }
}
