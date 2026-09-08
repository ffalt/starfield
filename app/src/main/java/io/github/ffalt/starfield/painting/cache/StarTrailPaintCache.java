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

import android.graphics.Bitmap;
import android.graphics.Paint;

import io.github.ffalt.starfield.StarfieldOpts;

public class StarTrailPaintCache extends PaintCache {
    // A comet sprite rendered once: head at x=0 with the full sprite height, tapering and
    // fading to nothing at x=TEX_W. Drawn per star as a rotated/scaled bitmap, so the trail
    // gets a soft cross section and a real tail instead of a flat stroked line.
    private static final int TEX_W = 96;
    private static final int TEX_H = 16;
    private static final float COLOR_EXPONENT = 0.7f;

    private static Bitmap cachedTrail;
    private static int cachedStart;
    private static int cachedEnd;

    private final Bitmap trail;

    public StarTrailPaintCache(StarfieldOpts opts) {
        // cache size - brightness is 0..100 in Starfield
        super(opts, 100);
        trail = trailBitmap(opts.trailColorStart, opts.trailColorEnd);
        buildCache();
    }

    public Bitmap getBitmap() {
        return trail;
    }

    public Paint build(int index) {
        Paint result = new Paint(Paint.FILTER_BITMAP_FLAG);
        result.setAlpha(index * 255 / 100);
        return result;
    }

    private static Bitmap trailBitmap(int colorStart, int colorEnd) {
        if (cachedTrail == null || cachedStart != colorStart || cachedEnd != colorEnd) {
            cachedTrail = buildTrail(colorStart, colorEnd);
            cachedStart = colorStart;
            cachedEnd = colorEnd;
        }
        return cachedTrail;
    }

    private static Bitmap buildTrail(int colorStart, int colorEnd) {
        int aStart = colorStart >>> 24;
        int rStart = (colorStart >> 16) & 0xFF;
        int gStart = (colorStart >> 8) & 0xFF;
        int bStart = colorStart & 0xFF;
        int aEnd = colorEnd >>> 24;
        int rEnd = (colorEnd >> 16) & 0xFF;
        int gEnd = (colorEnd >> 8) & 0xFF;
        int bEnd = colorEnd & 0xFF;
        int[] pixels = new int[TEX_W * TEX_H];
        for (int x = 0; x < TEX_W; x++) {
            float t = (x + 0.5f) / TEX_W;
            float rest = 1f - t;
            float halfWidth = (float) Math.sqrt(rest);
            float colorT = (float) Math.pow(t, COLOR_EXPONENT);
            float inv = 1f - colorT;
            float alphaX = rest * (aStart * inv + aEnd * colorT) / 255f;
            int rgb = ((int) (rStart * inv + rEnd * colorT) << 16)
                    | ((int) (gStart * inv + gEnd * colorT) << 8)
                    | (int) (bStart * inv + bEnd * colorT);
            if (halfWidth <= 0f || alphaX <= 0f) {
                continue;
            }
            float invHalfWidth = 1f / halfWidth;
            for (int y = 0; y < TEX_H; y++) {
                float across = Math.abs((y + 0.5f) * 2f / TEX_H - 1f) * invHalfWidth;
                if (across >= 1f) {
                    continue;
                }
                int alpha = (int) (alphaX * Math.sqrt(1f - across * across) * 255f + 0.5f);
                if (alpha <= 0) {
                    continue;
                }
                pixels[y * TEX_W + x] = (alpha << 24) | rgb;
            }
        }
        Bitmap sprite = Bitmap.createBitmap(pixels, TEX_W, TEX_H, Bitmap.Config.ARGB_8888);
        // Stars only ever draw on the surface hardware canvas, and a GPU-resident sprite
        // measurably cuts the per-trail drawBitmap cost.
        Bitmap uploaded = sprite.copy(Bitmap.Config.HARDWARE, false);
        return uploaded == null ? sprite : uploaded;
    }
}
