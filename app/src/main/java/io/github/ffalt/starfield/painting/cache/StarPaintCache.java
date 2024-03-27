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

import android.graphics.Paint;

import io.github.ffalt.starfield.StarfieldOpts;

public class StarPaintCache extends PaintCache {
    public StarPaintCache(StarfieldOpts opts) {
        super(opts, 100);
    }

    public Paint build(int index) {
        Paint result = new Paint();
        result.setStyle(Paint.Style.FILL);
        result.setColor(adjustBrightness(opts.starColor, index));
        result.setAntiAlias(true);
        return result;
    }
}
