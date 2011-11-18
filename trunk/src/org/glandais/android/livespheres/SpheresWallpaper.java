/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glandais.android.livespheres;

import java.util.List;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import android.app.Application;
import android.app.Service;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class SpheresWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "spheressettings";

	@Override
	public Engine onCreateEngine() {
		return new CubeEngine(this.getApplication());
	}

	class CubeEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener,
			SensorEventListener {

		private Application application;

		// SCALE_FACTOR = pixels / 1m
		private float scaleFactor = 150.0f;

		// private static final float SCALE_FACTOR = 200.0f;
		// pixels
		private static final float BALL_RADIUS = 50.0f;

		private PhysicsWorld mWorld;

		private final Handler mHandler = new Handler();

		private final Paint mPaint = new Paint();
		private int width = 800;
		private int height = 600;

		private boolean scheduled = false;
		private SharedPreferences mPrefs;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				mHandler.removeCallbacks(mDrawCube);
				if (scheduled) {
					mWorld.update();
					drawFrame();
					mHandler.postDelayed(mDrawCube, PhysicsWorld.TIME_STEP_MS);
				}
			}
		};

		CubeEngine(Application application) {
			this.application = application;
			mWorld = new PhysicsWorld();

			// Create a Paint to draw the lines for our cube
			final Paint paint = mPaint;
			paint.setColor(0xff000000);
			paint.setAntiAlias(true);
			paint.setStrokeWidth(2);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);

			mPrefs = SpheresWallpaper.this.getSharedPreferences(
					SHARED_PREFS_NAME, 0);
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			// String shape = prefs.getString("cube2_shape", "cube");
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			unschedule();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				drawFrame();
				schedule();
			} else {
				unschedule();
			}
		}

		public Vec2 toScreen(Vec2 worldPos) {
			int orientation = getOrientation();
			Vec2 screenPos = new Vec2();
			if (orientation == Surface.ROTATION_0) {
				screenPos.x = worldPos.y * scaleFactor;
				screenPos.y = worldPos.x * scaleFactor;
			} else if (orientation == Surface.ROTATION_90) {
				screenPos.x = worldPos.x * scaleFactor;
				screenPos.y = height - worldPos.y * scaleFactor;
			} else if (orientation == Surface.ROTATION_180) {
				screenPos.x = height - worldPos.y * scaleFactor;
				screenPos.y = width - worldPos.x * scaleFactor;
			} else if (orientation == Surface.ROTATION_270) {
				screenPos.x = width - worldPos.x * scaleFactor;
				screenPos.y = worldPos.y * scaleFactor;
			}

			return screenPos;
		}

		public Vec2 toWorld(Vec2 screenPos) {
			Vec2 worldPos = new Vec2();
			int orientation = getOrientation();
			if (orientation == Surface.ROTATION_0) {
				worldPos.y = screenPos.x / scaleFactor;
				worldPos.x = screenPos.y / scaleFactor;
			} else if (orientation == Surface.ROTATION_90) {
				worldPos.x = screenPos.x / scaleFactor;
				worldPos.y = (height - screenPos.y) / scaleFactor;
			} else if (orientation == Surface.ROTATION_180) {
				worldPos.x = (height - screenPos.y) / scaleFactor;
				worldPos.y = (width - screenPos.x) / scaleFactor;
			} else if (orientation == Surface.ROTATION_270) {
				worldPos.x = (width - screenPos.x) / scaleFactor;
				worldPos.y = screenPos.y / scaleFactor;
			}

			return worldPos;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			float sensorX = event.values[SensorManager.DATA_X];
			float sensorY = event.values[SensorManager.DATA_Y];

			float xAxis = sensorY;
			float yAxis = -sensorX;

			mWorld.setGravity(xAxis, yAxis);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			int rw = Math.max(width, height);
			int rh = Math.min(width, height);

			if (this.width != rw && this.height != rh) {
				// DisplayMetrics metrics = new DisplayMetrics();
				// getWindowManager().getDefaultDisplay().getMetrics(metrics);
				// scaleFactor = 100.0f * (metrics.density / 2.54f);
				// Log.i("Spheres", "scaleFactor = " + scaleFactor);

				this.width = rw;
				this.height = rh;
				Log.i("Spheres", "width/height = " + width + "/" + height);
				mWorld.recreateWorld(rw / scaleFactor, rh / scaleFactor);
			}
			drawFrame();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			unschedule();
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				Vec2 worldPosition = toWorld(new Vec2(event.getX(),
						event.getY()));
				mWorld.addBall(worldPosition, BALL_RADIUS / scaleFactor);
			}
			super.onTouchEvent(event);
		}

		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					draw(c);
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}
		}

		public void draw(Canvas c) {
			List<Body> balls = mWorld.getBalls();
			c.drawARGB(255, 255, 255, 255);

			for (Body body : balls) {
				Vec2 screenPos = toScreen(body.getPosition());
				c.drawCircle(screenPos.x, screenPos.y, BALL_RADIUS, mPaint);
			}
		}

		private WindowManager getWindowManager() {
			return ((WindowManager) application
					.getSystemService(Service.WINDOW_SERVICE));
		}

		private int getOrientation() {
			return getWindowManager().getDefaultDisplay().getOrientation();
		}

		private SensorManager getSensorManager() {
			return (SensorManager) getSystemService(SENSOR_SERVICE);
		}

		private void unschedule() {
			scheduled = false;
			mHandler.removeCallbacks(mDrawCube);

			List<Sensor> sensors = getSensorManager().getSensorList(
					Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				getSensorManager().unregisterListener(this, sensors.get(0));
			}
		}

		private void schedule() {
			scheduled = true;
			mHandler.postDelayed(mDrawCube, PhysicsWorld.TIME_STEP_MS);
			List<Sensor> sensors = getSensorManager().getSensorList(
					Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				getSensorManager().registerListener(this, sensors.get(0),
						SensorManager.SENSOR_DELAY_UI);
			}
		}
	}
}
