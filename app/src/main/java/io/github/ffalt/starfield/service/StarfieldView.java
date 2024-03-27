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

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import io.github.ffalt.starfield.painting.StarfieldScene;

public class StarfieldView extends SurfaceView implements SurfaceHolder.Callback {
    private final StarfieldScene scene = new StarfieldScene() {
        @Override
        public SurfaceHolder getSurface() {
            return getHolder();
        }
    };

    public StarfieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSizeChangeListener();
        scene.onCreate(this.getContext());
        this.getHolder().addCallback(this);
    }

    private void initSizeChangeListener() {
        this.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            scene.onUpdateSize(width, height);
        });
    }

    @Override
    public void surfaceCreated(@NonNull final SurfaceHolder holder) {
        scene.reset();
        scene.onVisibilityChanged(true);
        scene.updateFromSharedPreference(this.getContext());
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        scene.onDestroy(this.getContext());
    }
}
