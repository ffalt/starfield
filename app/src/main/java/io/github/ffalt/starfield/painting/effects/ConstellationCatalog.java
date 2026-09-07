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

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// The star patterns in that asset come from d3-celestial by Olaf Frohn (BSD-3-Clause):
// https://github.com/ofrohn/d3-celestial
// magnitudes matched by position against its Hipparcos star catalogue.
//
// Star patterns of the IAU constellations. Each star is [right ascension in hours, declination in degrees (J2000), apparent visual magnitude].
// positions: https://github.com/ofrohn/d3-celestial/blob/master/data/constellations.lines.json
// magnitudes": https://github.com/ofrohn/d3-celestial/blob/master/data/stars.6.json
// names https://github.com/ofrohn/d3-celestial/blob/master/data/constellations.json
// license: BSD-3-Clause, Copyright 2015-2021 Olaf Frohn
// note: Right ascension converted from degrees to hours

public final class ConstellationCatalog {
    private static final String ASSET_NAME = "constellations/constellations.json";
    private static final int VALUES_PER_STAR = 3;
    private static final int READ_BUFFER = 8192;
    private static final float[][] EMPTY = new float[0][];
    private static final int[][] NO_SEGMENTS = new int[0][];
    private static final int[] NO_SEGMENT = new int[0];

    private static final double HOURS_TO_RADIANS = Math.PI / 12.0;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
    private static final float RADIUS_AT_ZERO_MAGNITUDE = 2.8f;
    private static final float RADIUS_PER_MAGNITUDE = 0.38f;
    private static final float RADIUS_MIN = 0.9f;
    private static final float RADIUS_MAX = 3.4f;

    private static volatile ConstellationCatalog instance;

    private final float[][] patterns;
    private final int[][] segments;
    private final int maxStars;

    private ConstellationCatalog(Context context) {
        float[][] readPatterns = EMPTY;
        int[][] readSegments = NO_SEGMENTS;
        int max = 0;
        try {
            JSONArray list = new JSONArray(read(context));
            int n = list.length();
            if (n > 0) {
                float[][] parsedPatterns = new float[n][];
                int[][] parsedSegments = new int[n][];
                for (int i = 0; i < n; i++) {
                    JSONObject entry = list.getJSONObject(i);
                    parsedPatterns[i] = readStars(entry.getJSONArray("stars"));
                    parsedSegments[i] = readLines(entry.optJSONArray("lines"));
                    max = Math.max(max, parsedPatterns[i].length / VALUES_PER_STAR);
                }
                readPatterns = parsedPatterns;
                readSegments = parsedSegments;
            }
        } catch (Exception ignored) {
            readPatterns = EMPTY;
            readSegments = NO_SEGMENTS;
            max = 0;
        }
        patterns = readPatterns;
        segments = readSegments;
        maxStars = max;
    }

    public static ConstellationCatalog getInstance(Context context) {
        if (instance == null) {
            synchronized (ConstellationCatalog.class) {
                if (instance == null) {
                    instance = new ConstellationCatalog(context);
                }
            }
        }
        return instance;
    }

    public int size() {
        return patterns.length;
    }

    public int[] getSegments(int index) {
        return segments[index];
    }

    public int getMaxStars() {
        return maxStars;
    }

    public int project(int index, float[] outX, float[] outY, float[] outRadius) {
        float[] data = patterns[index];
        int n = data.length / VALUES_PER_STAR;
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        for (int i = 0; i < n; i++) {
            double ra = data[i * VALUES_PER_STAR] * HOURS_TO_RADIANS;
            double dec = data[i * VALUES_PER_STAR + 1] * DEGREES_TO_RADIANS;
            double cosDec = Math.cos(dec);
            sumX += cosDec * Math.cos(ra);
            sumY += cosDec * Math.sin(ra);
            sumZ += Math.sin(dec);
        }
        double ra0 = Math.atan2(sumY, sumX);
        double dec0 = Math.atan2(sumZ, Math.sqrt(sumX * sumX + sumY * sumY));
        double sinDec0 = Math.sin(dec0);
        double cosDec0 = Math.cos(dec0);
        float maxAbs = 0f;
        for (int i = 0; i < n; i++) {
            double ra = data[i * VALUES_PER_STAR] * HOURS_TO_RADIANS;
            double dec = data[i * VALUES_PER_STAR + 1] * DEGREES_TO_RADIANS;
            double cosDec = Math.cos(dec);
            double sinDec = Math.sin(dec);
            double cosDeltaRa = Math.cos(ra - ra0);
            double cosC = sinDec0 * sinDec + cosDec0 * cosDec * cosDeltaRa;
            float px = (float) (-cosDec * Math.sin(ra - ra0) / cosC);
            float py = (float) (-(cosDec0 * sinDec - sinDec0 * cosDec * cosDeltaRa) / cosC);
            outX[i] = px;
            outY[i] = py;
            float mag = data[i * VALUES_PER_STAR + 2];
            float starRadius = RADIUS_AT_ZERO_MAGNITUDE - mag * RADIUS_PER_MAGNITUDE;
            outRadius[i] = Math.max(RADIUS_MIN, Math.min(RADIUS_MAX, starRadius));
            maxAbs = Math.max(maxAbs, Math.max(Math.abs(px), Math.abs(py)));
        }
        if (maxAbs > 0f) {
            float scale = 1f / maxAbs;
            for (int i = 0; i < n; i++) {
                outX[i] *= scale;
                outY[i] *= scale;
            }
        }
        return n;
    }

    private static float[] readStars(JSONArray stars) throws JSONException {
        int starCount = stars.length();
        float[] data = new float[starCount * VALUES_PER_STAR];
        for (int s = 0; s < starCount; s++) {
            JSONArray star = stars.getJSONArray(s);
            data[s * VALUES_PER_STAR] = (float) star.getDouble(0);
            data[s * VALUES_PER_STAR + 1] = (float) star.getDouble(1);
            data[s * VALUES_PER_STAR + 2] = (float) star.getDouble(2);
        }
        return data;
    }

    private static int[] readLines(JSONArray polylines) throws JSONException {
        if (polylines == null) {
            return NO_SEGMENT;
        }
        int total = 0;
        for (int i = 0; i < polylines.length(); i++) {
            total += Math.max(0, polylines.getJSONArray(i).length() - 1);
        }
        int[] pairs = new int[total * 2];
        int at = 0;
        for (int i = 0; i < polylines.length(); i++) {
            JSONArray poly = polylines.getJSONArray(i);
            for (int j = 1; j < poly.length(); j++) {
                pairs[at] = poly.getInt(j - 1);
                pairs[at + 1] = poly.getInt(j);
                at += 2;
            }
        }
        return pairs;
    }

    private static String read(Context context) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(ASSET_NAME), StandardCharsets.UTF_8))) {
            char[] buffer = new char[READ_BUFFER];
            int amount = reader.read(buffer);
            while (amount > 0) {
                sb.append(buffer, 0, amount);
                amount = reader.read(buffer);
            }
        }
        return sb.toString();
    }
}
