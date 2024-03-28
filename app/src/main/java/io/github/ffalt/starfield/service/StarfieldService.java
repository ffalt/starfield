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

package io.github.ffalt.starfield.service;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import io.github.ffalt.starfield.painting.StarfieldScene;

public class StarfieldService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new StarFieldEngine();
    }

    class StarFieldEngine extends Engine {
        private final StarfieldScene scene = new StarfieldScene() {
            @Override
            public SurfaceHolder getSurface() {
                return getSurfaceHolder();
            }
        };
        private float screenXOffset = 0;
        private float screenDesiredWidth = 0;

        StarFieldEngine() {
            setOffsetNotificationsEnabled(true);
            scene.onCreate(StarfieldService.this);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            scene.onVisibilityChanged(visible);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            screenDesiredWidth = getDesiredMinimumHeight();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            screenDesiredWidth = getDesiredMinimumHeight();
            scene.onUpdateSize(width, height);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            scene.onDestroy(StarfieldService.this);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep, int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
            if (scene.visible && scene.opts.followScreen) {
                float diff = (screenXOffset - xOffset) * (screenDesiredWidth / 4);
                scene.onUpdateOffset(-diff, 0);
            }
            screenXOffset = xOffset;
        }
    }
}
