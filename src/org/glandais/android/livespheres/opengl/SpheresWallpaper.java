package org.glandais.android.livespheres.opengl;

import java.util.List;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import org.jbox2d.common.Vec2;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class SpheresWallpaper extends GLWallpaperService {

	public static final String SHARED_PREFS_NAME = "spheressettings";
	public static final String KEY_BACKGROUND = "background";

	public SpheresWallpaper() {
		super();
	}

	public Engine onCreateEngine() {
		SpheresEngine engine = new SpheresEngine(this);
		return engine;
	}

	public class SpheresEngine extends GLEngine implements SensorEventListener {

		private SpheresRenderer renderer;

		private PhysicsWorld mWorld;

		private final Handler mHandler = new Handler();

		private boolean scheduled = false;

		private final Runnable mDrawCube = new Runnable() {
			public void run() {
				mHandler.removeCallbacks(mDrawCube);
				if (scheduled) {
					mWorld.update();
					mHandler.postDelayed(mDrawCube,
							PhysicsWorld.DRAWFRAME_STEP_MS);
				}
			}
		};

		public SpheresEngine(SpheresWallpaper spheresWallpaper) {
			super();
			mWorld = new PhysicsWorld(spheresWallpaper.getApplication());
			// handle prefs, other initialization
			renderer = new SpheresRenderer(spheresWallpaper, true, true, mWorld);
			setRenderer(renderer);
			setRenderMode(RENDERMODE_CONTINUOUSLY);
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		public void onDestroy() {
			super.onDestroy();
			unschedule();
			if (renderer != null) {
				renderer.shutdown(); // assuming yours has this method - it
										// should!
			}
			renderer = null;
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				schedule();
			} else {
				unschedule();
			}
		}

		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				float sensorX = event.values[SensorManager.DATA_X];
				float sensorY = event.values[SensorManager.DATA_Y];

				float xAxis = - sensorY;
				float yAxis = - sensorX;

				mWorld.setGravity(xAxis, yAxis, 4.0f);
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			if (mWorld.recreateWorld(width, height)) {
				renderer.updateSpriteArray();
			}
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
				mWorld.touch(new Vec2(event.getX(), event.getY()));
			}
			super.onTouchEvent(event);
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
