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
import io.github.ffalt.starfield.painting.cache.StarPaintCache;
import io.github.ffalt.starfield.painting.cache.StarTrailPaintCache;

public class StarsPaint {
    private final StarfieldOpts opts;
    private float[] starsX = new float[0];
    private float[] starsY = new float[0];
    private float[] starsZ = new float[0];
    private float[] starsV = new float[0];
    private float[] starsRadius = new float[0];
    private float[] starsLastX = new float[0];
    private float[] starsLastY = new float[0];
    private float[] starsCurrentX = new float[0];
    private float[] starsCurrentY = new float[0];
    private float[] starsCurrentRadius = new float[0];
    private int[] starsCurrentBrightness = new int[0];
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
    private float speedModifier = 1.0f;
    private static final float MAX_TILT_ANGLE = (float) Math.toRadians(50.0);
    private static final float SMOOTHING = 0.01f;
    private static final float INV_MAX_TILT_ANGLE = 1f / MAX_TILT_ANGLE;

    public StarsPaint(StarfieldOpts opts) {
        this.opts = opts;
        starPaints = new StarPaintCache(opts);
        starTrailPaints = new StarTrailPaintCache(opts);
    }

    public void init() {
        int n = opts.numStars;
        starsX = new float[n];
        starsY = new float[n];
        starsZ = new float[n];
        starsV = new float[n];
        starsRadius = new float[n];
        starsLastX = new float[n];
        starsLastY = new float[n];
        starsCurrentX = new float[n];
        starsCurrentY = new float[n];
        starsCurrentRadius = new float[n];
        starsCurrentBrightness = new int[n];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < n; i++) {
            randomStarPosition(i, rng);
            starsZ[i] = rng.nextFloat() * opts.initialZ;
        }
    }

    public void move() {
        if (opts.followScreen) {
            float dx = offsetTX - offsetX;
            if (dx > SMOOTHING || dx < -SMOOTHING) {
                offsetX += dx * SMOOTHING;
            }
            float dy = offsetTY - offsetY;
            if (dy > SMOOTHING || dy < -SMOOTHING) {
                offsetY += dy * SMOOTHING;
            }
            if (opts.followRestore) {
                offsetTX -= offsetTX * SMOOTHING;
                offsetTY -= offsetTY * SMOOTHING;
            }
        }
        if (opts.followSensor) {
            float tx = tiltTargetX - tiltOffsetX;
            if (tx > SMOOTHING || tx < -SMOOTHING) {
                tiltOffsetX += tx * SMOOTHING;
            }
            float ty = tiltTargetY - tiltOffsetY;
            if (ty > SMOOTHING || ty < -SMOOTHING) {
                tiltOffsetY += ty * SMOOTHING;
            }
        }
        // Cache all loop-invariant values; avoids repeated field reads inside the hot loop.
        float totalOffsetX = offsetX + tiltOffsetX;
        float totalOffsetY = offsetY + tiltOffsetY;
        float hW = opts.hW;
        float hH = opts.hH;
        float width = opts.width;
        float height = opts.height;
        float initialZ = opts.initialZ;
        float invInitialZ = 1f / initialZ;
        float starSize = opts.starSize;
        float speedFactor = 0.1f * speedModifier;
        final float[] sX = starsX;
        final float[] sY = starsY;
        final float[] sZ = starsZ;
        final float[] sV = starsV;
        final float[] sR = starsRadius;
        final float[] sLX = starsLastX;
        final float[] sLY = starsLastY;
        final float[] sCX = starsCurrentX;
        final float[] sCY = starsCurrentY;
        final float[] sCR = starsCurrentRadius;
        final int[] sCB = starsCurrentBrightness;
        final ThreadLocalRandom rng = ThreadLocalRandom.current();
        int n = sX.length;
        float vRange = opts.maxV - opts.minV;
        float minV = opts.minV;
        for (int i = 0; i < n; i++) {
            float sz = sZ[i] - sV[i] * speedFactor;
            if (sz <= 0f) {
                // Inline randomStarPosition using cached locals to avoid field re-reads.
                sX[i] = rng.nextFloat() * width - hW;
                sY[i] = rng.nextFloat() * height - hH;
                sV[i] = rng.nextFloat() * vRange + minV;
                sR[i] = rng.nextFloat() * 2f + 1f;
                sLX[i] = -1f;
                sLY[i] = -1f;
                sCR[i] = -1f;
                sCY[i] = -1f;
                sZ[i] = initialZ;
                continue;
            }
            sZ[i] = sz;
            sLX[i] = sCX[i];
            sLY[i] = sCY[i];
            sV[i] += 0.001f;
            float invZ = 1f / sz;
            sCX[i] = hW + (width * sX[i] * invZ - totalOffsetX);
            sCY[i] = hH + (height * sY[i] * invZ - totalOffsetY);
            float zRatio = sz * invInitialZ;
            sCR[i] = (1 - zRatio) * sR[i] * starSize;
            int b = 100 - (int) (zRatio * 40 + 0.5f);
            sCB[i] = b < 0 ? 0 : Math.min(b, 100);
        }
    }

    public void draw(Canvas c) {
        float width = opts.width;
        float height = opts.height;
        int n = starsX.length;
        if (opts.trails) {
            if (opts.circle) {
                drawLoopTrailsCircle(c, n, width, height);
            } else {
                drawLoopTrailsRect(c, n, width, height);
            }
        } else {
            if (opts.circle) {
                drawLoopCircle(c, n, width, height);
            } else {
                drawLoopRect(c, n, width, height);
            }
        }
    }

    private void drawLoopCircle(Canvas c, int n, float width, float height) {
        final float[] sCX = starsCurrentX;
        final float[] sCY = starsCurrentY;
        final float[] sCR = starsCurrentRadius;
        final int[] sCB = starsCurrentBrightness;
        final Paint[] sp = starPaints.getArray();
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            if (r < 0.5f) {
                continue;
            }
            float cx = sCX[i];
            float cy = sCY[i];
            if (cx < 0 || cx > width || cy < 0 || cy > height) {
                continue;
            }
            c.drawCircle(cx, cy, r, sp[sCB[i]]);
        }
    }

    private void drawLoopRect(Canvas c, int n, float width, float height) {
        final float[] sCX = starsCurrentX;
        final float[] sCY = starsCurrentY;
        final float[] sCR = starsCurrentRadius;
        final int[] sCB = starsCurrentBrightness;
        final Paint[] sp = starPaints.getArray();
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            if (r < 0.5f) {
                continue;
            }
            float cx = sCX[i];
            float cy = sCY[i];
            if (cx < 0 || cx > width || cy < 0 || cy > height) {
                continue;
            }
            float rH = r * 0.5f;
            c.drawRect(cx - rH, cy - rH, cx + rH, cy + rH, sp[sCB[i]]);
        }
    }

    private void drawLoopTrailsCircle(Canvas c, int n, float width, float height) {
        final float[] sLX = starsLastX;
        final float[] sLY = starsLastY;
        final float[] sCX = starsCurrentX;
        final float[] sCY = starsCurrentY;
        final float[] sCR = starsCurrentRadius;
        final int[] sCB = starsCurrentBrightness;
        final Paint[] sp = starPaints.getArray();
        final Paint[] stp = starTrailPaints.getArray();
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            if (r < 0.5f) {
                continue;
            }
            float lx = sLX[i];
            float ly = sLY[i];
            if (lx < 0 || lx > width || ly < 0 || ly > height) {
                continue;
            }
            float cx = sCX[i];
            float cy = sCY[i];
            int b = sCB[i];
            float dx = lx - cx;
            float dy = ly - cy;
            if (dx * dx + dy * dy > 16f) {
                Paint tp = stp[b];
                tp.setStrokeWidth(r);
                c.drawLine(lx, ly, cx, cy, tp);
            }
            c.drawCircle(cx, cy, r, sp[b]);
        }
    }

    private void drawLoopTrailsRect(Canvas c, int n, float width, float height) {
        final float[] sLX = starsLastX;
        final float[] sLY = starsLastY;
        final float[] sCX = starsCurrentX;
        final float[] sCY = starsCurrentY;
        final float[] sCR = starsCurrentRadius;
        final int[] sCB = starsCurrentBrightness;
        final Paint[] sp = starPaints.getArray();
        final Paint[] stp = starTrailPaints.getArray();
        for (int i = 0; i < n; i++) {
            float r = sCR[i];
            if (r < 0.5f) {
                continue;
            }
            float lx = sLX[i];
            float ly = sLY[i];
            if (lx < 0 || lx > width || ly < 0 || ly > height) {
                continue;
            }
            float cx = sCX[i];
            float cy = sCY[i];
            int b = sCB[i];
            float dx = lx - cx;
            float dy = ly - cy;
            if (dx * dx + dy * dy > 16f) {
                Paint tp = stp[b];
                tp.setStrokeWidth(r);
                c.drawLine(lx, ly, cx, cy, tp);
            }
            float rH = r * 0.5f;
            c.drawRect(cx - rH, cy - rH, cx + rH, cy + rH, sp[b]);
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
        float clampedPitch = Math.max(-MAX_TILT_ANGLE, Math.min(MAX_TILT_ANGLE, tiltX));
        float clampedRoll = Math.max(-MAX_TILT_ANGLE, Math.min(MAX_TILT_ANGLE, tiltY));
        float nPitch = clampedPitch * INV_MAX_TILT_ANGLE;
        float nRoll = clampedRoll * INV_MAX_TILT_ANGLE;
        float targetX = -nRoll * opts.width;
        float targetY = nPitch * opts.height;
        float intensity = opts.followSensorIntensity * 0.01f;
        tiltTargetX += (targetX - tiltTargetX) * intensity;
        tiltTargetY += (targetY - tiltTargetY) * intensity;
    }

    public void setOffsets(float diffX, float diffY) {
        float scale = 0.25f;
        if (opts.followRestore) {
            scale = 1f;
        }
        float intensity = opts.followScreenIntensity * 0.1f;
        offsetTX += diffX * scale * intensity;
        offsetTY += diffY * scale * intensity;
    }

    public void setSpeedModifier(float mod) {
        this.speedModifier = mod;
    }

    private void randomStarPosition(int i, ThreadLocalRandom rng) {
        starsX[i] = rng.nextFloat() * opts.width - opts.hW;
        starsY[i] = rng.nextFloat() * opts.height - opts.hH;
        starsV[i] = rng.nextFloat() * (opts.maxV - opts.minV) + opts.minV;
        starsRadius[i] = rng.nextFloat() * 2f + 1f;
        starsLastX[i] = -1f;
        starsLastY[i] = -1f;
        starsCurrentX[i] = -1f;
        starsCurrentY[i] = -1f;
    }
}
