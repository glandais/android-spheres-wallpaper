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

		private final Paint mPaint = new Paint();
		private final Paint bit_paint = new Paint();
		// radians
		private float shade_angle = 0.0f;

		private int width = 1;
		private int height = 1;

		private boolean scheduled = false;
		private SharedPreferences mPrefs;

		private BitmapDrawable bitmap_background;
		private Bitmap bitmap_ball;
		private Bitmap bitmap_shade;

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

		private float[] accelerometerValues;
		private float[] geomagneticMatrix;
		private boolean acceleroOk = false;
		private boolean magneticOk = false;

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

			bitmap_ball = BitmapFactory.decodeResource(getResources(),
					R.drawable.ball);
			bitmap_shade = BitmapFactory.decodeResource(getResources(),
					R.drawable.shade128);

			mPrefs = SpheresWallpaper.this.getSharedPreferences(
					SHARED_PREFS_NAME, MODE_PRIVATE);
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			String background = prefs.getString(KEY_BACKGROUND, null);
			if (background != null) {
				Bitmap bitmap = BitmapFactory.decodeFile(background);
				bitmap_background = new BitmapDrawable(bitmap);
				bitmap_background.setTileModeXY(Shader.TileMode.REPEAT,
						Shader.TileMode.REPEAT);
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
				accelerometerValues = event.values.clone();
				acceleroOk = true;

				float sensorX = event.values[SensorManager.DATA_X];
				float sensorY = event.values[SensorManager.DATA_Y];

				float xAxis = sensorY;
				float yAxis = -sensorX;

				mWorld.setGravity(xAxis, yAxis, 4.0f);
			}
			// if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			// geomagneticMatrix = event.values.clone();
			// magneticOk = true;
			// }

			// if (acceleroOk && magneticOk) {
			// float[] R = new float[16];
			// float[] I = new float[16];
			//
			// SensorManager.getRotationMatrix(R, I, accelerometerValues,
			// geomagneticMatrix);
			//
			// float[] actual_orientation = new float[3];
			// SensorManager.getOrientation(R, actual_orientation);
			//
			// shade_angle = - actual_orientation[0];
			// }
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);

			int rw = Math.max(width, height);
			int rh = Math.min(width, height);

			// int orientation = getOrientation();
			// if (orientation == Surface.ROTATION_0 || orientation ==
			// Surface.ROTATION_180) {
			// rw = width;
			// rh = height;
			// } else {
			// rw = height;
			// rh = width;
			// }

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
			if (event.getAction() == MotionEvent.ACTION_UP) {
				Vec2 worldPosition = toWorld(new Vec2(event.getX(),
						event.getY()));
				// mWorld.touchUp(worldPosition);
				mWorld.touch(worldPosition);
			}
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				Vec2 worldPosition = toWorld(new Vec2(event.getX(),
						event.getY()));
				// mWorld.touchDown(worldPosition);
				mWorld.touch(worldPosition);
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				Vec2 worldPosition = toWorld(new Vec2(event.getX(),
						event.getY()));
				// mWorld.touchMove(worldPosition);
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
				float radius = ((Float) body.getUserData()) * scaleFactor;

				Matrix matrix = new Matrix();
				matrix.reset();
				matrix.postTranslate(-bitmap_ball.getWidth() / 2.0f,
						-bitmap_ball.getHeight() / 2.0f);
				matrix.postRotate(body.getAngle() * 57.296f);
				matrix.postScale(2.0f * radius / bitmap_ball.getWidth(), 2.0f
						* radius / bitmap_ball.getHeight());
				matrix.postTranslate(screenPos.x, screenPos.y);
				c.drawBitmap(bitmap_ball, matrix, bit_paint);

				matrix = new Matrix();
				matrix.reset();
				matrix.postTranslate(-bitmap_ball.getWidth() / 2.0f,
						-bitmap_ball.getHeight() / 2.0f);
				matrix.postRotate(shade_angle * 57.296f);
				matrix.postScale(2.0f * radius / bitmap_ball.getWidth(), 2.0f
						* radius / bitmap_ball.getHeight());
				matrix.postTranslate(screenPos.x, screenPos.y);
				c.drawBitmap(bitmap_shade, matrix, bit_paint);

				// Log.i("World", "Draw ball " + screenPos + " / " + radius);
				// c.drawCircle(screenPos.x, screenPos.y, radius, mPaint);
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
			// sensors = getSensorManager().getSensorList(
			// Sensor.TYPE_MAGNETIC_FIELD);
			// if (sensors.size() > 0) {
			// getSensorManager().unregisterListener(this, sensors.get(0));
			// }

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
			// sensors = getSensorManager().getSensorList(
			// Sensor.TYPE_MAGNETIC_FIELD);
			// if (sensors.size() > 0) {
			// getSensorManager().registerListener(this, sensors.get(0),
			// SensorManager.SENSOR_DELAY_UI);
			// }
		}
	}
}
