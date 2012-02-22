package org.glandais.android.livespheres;

import java.util.List;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import android.app.Application;
import android.app.Service;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class SpheresWallpaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "spheressettings";
	public static final String KEY_BACKGROUND = "background";

	@Override
	public Engine onCreateEngine() {
		return new CubeEngine(this.getApplication());
	}

	class CubeEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener,
			SensorEventListener {

		private Application application;

		// SCALE_FACTOR = pixels / 1m
		private float scaleFactor = 40.0f;

		// Ball radius in smallest side ratio
		private static final float BALL_RADIUS = 0.11f;

		private PhysicsWorld mWorld;

		private final Handler mHandler = new Handler();

		private int width = 1;
		private int height = 1;

		private boolean scheduled = false;
		private SharedPreferences mPrefs;

		private final Paint mPaint = new Paint();

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				mHandler.removeCallbacks(mDrawCube);
				if (scheduled) {
					mWorld.update();
					drawFrame();
					mHandler.postDelayed(mDrawCube,
							PhysicsWorld.DRAWFRAME_STEP_MS);
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
			paint.setStrokeWidth(5);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStyle(Paint.Style.STROKE);

			mPrefs = SpheresWallpaper.this.getSharedPreferences(
					SHARED_PREFS_NAME, MODE_PRIVATE);
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			String background = prefs.getString(KEY_BACKGROUND, null);
			if (background != null) {
			}
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

		public float angleToScreen(float worldAngle) {
			int orientation = getOrientation();
			float screenAngle = 0.0f;
			if (orientation == Surface.ROTATION_0) {
				screenAngle = -worldAngle - 90;
			} else if (orientation == Surface.ROTATION_90) {
				screenAngle = -worldAngle - 180;
			} else if (orientation == Surface.ROTATION_180) {
				screenAngle = -worldAngle - 270;
			} else if (orientation == Surface.ROTATION_270) {
				screenAngle = -worldAngle;
			}
			return screenAngle;
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

		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				float sensorX = event.values[SensorManager.DATA_X];
				float sensorY = event.values[SensorManager.DATA_Y];

				float xAxis = sensorY;
				float yAxis = -sensorX;

				mWorld.setGravity(xAxis, yAxis, 4.0f);
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			int rw = Math.max(width, height);
			int rh = Math.min(width, height);

			if (this.width != rw || this.height != rh) {
				this.width = rw;
				this.height = rh;
				mWorld.recreateWorld(rw / scaleFactor, rh / scaleFactor,
						BALL_RADIUS);
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
			// Log.i("World", "onTouchEvent " + event);
			if (event.getAction() == MotionEvent.ACTION_UP
					|| event.getAction() == MotionEvent.ACTION_DOWN
					|| event.getAction() == MotionEvent.ACTION_MOVE) {
				Vec2 worldPosition = toWorld(new Vec2(event.getX(),
						event.getY()));
				mWorld.touch(worldPosition);
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

			// if (bitmap_background != null) {
			// bitmap_background.setBounds(0, 0, c.getWidth(), c.getHeight());
			// bitmap_background.draw(c);
			// } else {
			c.drawARGB(255, 255, 255, 255);
			// }

			for (Body body : balls) {
				Vec2 screenPos = toScreen(body.getPosition());
				Float worldRadius = (Float) body.getUserData();
				float screenRadius = worldRadius * scaleFactor;

				float bodyAngle = body.getAngle();
				float xball = (float) (worldRadius * Math.cos(bodyAngle));
				float yball = (float) (worldRadius * Math.sin(bodyAngle));
				Vec2 point = body.getPosition().add(new Vec2(xball, yball));
				Vec2 screenPos2 = toScreen(point);

				mPaint.setColor(0xff000000);
				c.drawCircle(screenPos.x, screenPos.y, screenRadius, mPaint);
				c.drawLine(screenPos.x, screenPos.y, screenPos2.x,
						screenPos2.y, mPaint);
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
			mHandler.postDelayed(mDrawCube, PhysicsWorld.DRAWFRAME_STEP_MS);
			List<Sensor> sensors = getSensorManager().getSensorList(
					Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				getSensorManager().registerListener(this, sensors.get(0),
						SensorManager.SENSOR_DELAY_UI);
			}
		}
	}
}
