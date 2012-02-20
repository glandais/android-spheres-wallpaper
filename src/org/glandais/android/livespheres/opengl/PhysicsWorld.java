package org.glandais.android.livespheres.opengl;

import java.util.ArrayList;
import java.util.List;

import org.glandais.android.livespheres.opengl.sprites.GLSprite;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import android.app.Application;
import android.app.Service;
import android.view.Surface;
import android.view.WindowManager;

public class PhysicsWorld {

	// SCALE_FACTOR = pixels / 1m
	public static final float SCALE_FACTOR = 40.0f;

	// Ball radius in smallest side ratio
	public static final float BALL_RADIUS = 0.11f;

	public static final float DRAWFRAME_FRAMERATE = 25;
	public static final float PHYSIC_FRAMERATE = 100;

	public static final int SIM_COUNT = (int) Math.round(PHYSIC_FRAMERATE
			/ DRAWFRAME_FRAMERATE);

	public static final float PHYSIC_STEP_SEC = (1f / PHYSIC_FRAMERATE);
	public static final int DRAWFRAME_STEP_MS = Math
			.round(1000.0f / DRAWFRAME_FRAMERATE);
	public static final int VEL_ITER = 3;
	public static final int POS_ITER = 8;
	private static final float MAX_SPEED = 2.0f;

	private List<Body> balls = new ArrayList<Body>();

	private World world;
	private float minaxis;

	private Application application;

	private int width = 1;
	private int height = 1;

	private float[] cacheX;
	private float[] cacheY;
	private float[] cacheAngle;

	public PhysicsWorld(Application application) {
		super();
		this.application = application;
		recreateWorld(640, 480);
	}

	public boolean recreateWorld(int newWidth, int newHeight) {
		int realWidth = Math.max(newWidth, newHeight);
		int realHeight = Math.min(newWidth, newHeight);

		if (this.width != realWidth || this.height != realHeight) {
			this.width = realWidth;
			this.height = realHeight;

			float xmax = realWidth / SCALE_FACTOR;
			float ymax = realHeight / SCALE_FACTOR;

			this.minaxis = Math.min(xmax, ymax);

			// Log.i("World", "RecreateWorld " + xmax + " / " + ymax);

			Vec2 gravity = new Vec2(0.0f, 0.0f);
			world = new World(gravity, false);
			world.setContinuousPhysics(true);
			world.setWarmStarting(true);

			BodyDef def = new BodyDef();
			def.type = BodyType.STATIC;
			Body groundBody = world.createBody(def);
			createEdge(0.0f, 0.0f, xmax, 0.0f, groundBody);
			createEdge(xmax, 0.0f, xmax, ymax, groundBody);
			createEdge(xmax, ymax, 0.0f, ymax, groundBody);
			createEdge(0.0f, ymax, 0.0f, 0.0f, groundBody);

			balls.clear();
			float random = (float) Math.random();
			float ballsize = (BALL_RADIUS * minaxis) * (0.9f + 0.2f * random);

			cacheX = new float[10];
			cacheY = new float[10];
			cacheAngle = new float[10];

			for (int i = 0; i < 10; i++) {

				float x = (float) (ballsize + random * (xmax - ballsize));
				float y = (float) (ballsize + random * (ymax - ballsize));

				Vec2 position = new Vec2(x, y);
				addBall(position, ballsize);
			}
			return true;
		}
		return false;
	}

	private void createEdge(float x1, float y1, float x2, float y2,
			Body groundBody) {
		Vec2 v1 = new Vec2(x1, y1);
		Vec2 v2 = new Vec2(x2, y2);

		PolygonShape groundShapeDef = new PolygonShape();
		groundShapeDef.setAsEdge(v1, v2);

		FixtureDef def = new FixtureDef();
		def.density = 1.0f;
		// 1 =
		def.friction = 0.9f;
		// 1 = rebond
		def.restitution = 0.7f;
		def.shape = groundShapeDef;

		groundBody.createFixture(def);
	}

	public void touch(Vec2 screenPosition) {
		Vec2 worldPosition = toWorld(screenPosition);
		for (Body ball : balls) {
			Vec2 ballPosition = ball.getPosition();
			Vec2 dist = ballPosition.sub(worldPosition);
			float length = dist.normalize();
			float radius = minaxis / 2.0f;
			if (length < radius) {
				float speed = MAX_SPEED * (1.0f - length / radius);
				dist.mul(speed);
				ball.setLinearVelocity(ball.getLinearVelocity().add(dist));
			}
		}
	}

	private void addBall(Vec2 worldPosition, float radius) {
		// Log.i("World", "Add ball " + position + " / " + radius);

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(worldPosition);
		Body body = world.createBody(bodyDef);

		CircleShape shape = new CircleShape();
		shape.m_radius = radius;

		// body.createFixture(shape, 1.0f);

		FixtureDef def = new FixtureDef();
		def.density = 1.0f;
		// 1 =
		def.friction = 0.9f;
		// 1 = rebond
		def.restitution = 0.7f;
		def.shape = shape;
		body.createFixture(def);

		body.resetMassData();

		body.setUserData(new Float(radius));

		balls.add(body);
	}

	public void setGravity(float x, float y, float ratio) {
		world.setGravity(new Vec2(x * ratio, y * ratio));
	}

	public void update() {
		for (int i = 0; i < SIM_COUNT; i++) {
			world.step(PHYSIC_STEP_SEC, VEL_ITER, POS_ITER);
		}
		synchronized (this) {
			for (int i = 0; i < balls.size(); i++) {
				Body ball = balls.get(i);
				cacheX[i] = ball.getPosition().x;
				cacheY[i] = ball.getPosition().y;
				cacheAngle[i] = ball.getAngle();
			}
		}
	}

	private WindowManager getWindowManager() {
		return ((WindowManager) application
				.getSystemService(Service.WINDOW_SERVICE));
	}

	private int getOrientation() {
		return getWindowManager().getDefaultDisplay().getOrientation();
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
			screenPos.x = worldPos.y * SCALE_FACTOR;
			screenPos.y = worldPos.x * SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_90) {
			screenPos.x = width - worldPos.x * SCALE_FACTOR;
			screenPos.y = worldPos.y * SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_180) {
			screenPos.x = height - worldPos.y * SCALE_FACTOR;
			screenPos.y = width - worldPos.x * SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_270) {
			screenPos.x = worldPos.x * SCALE_FACTOR;
			screenPos.y = height - worldPos.y * SCALE_FACTOR;
		}

		return screenPos;
	}

	public Vec2 toWorld(Vec2 screenPos) {
		Vec2 worldPos = new Vec2();
		int orientation = getOrientation();
		if (orientation == Surface.ROTATION_0) {
			worldPos.y = screenPos.x / SCALE_FACTOR;
			worldPos.x = screenPos.y / SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_90) {
			worldPos.x = (width - screenPos.x) / SCALE_FACTOR;
			worldPos.y = screenPos.y / SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_180) {
			worldPos.x = (height - screenPos.y) / SCALE_FACTOR;
			worldPos.y = (width - screenPos.x) / SCALE_FACTOR;
		} else if (orientation == Surface.ROTATION_270) {
			worldPos.x = screenPos.x / SCALE_FACTOR;
			worldPos.y = (height - screenPos.y) / SCALE_FACTOR;
		}
		return worldPos;
	}

	public int getBallCount() {
		return balls.size();
	}

	public float getBallRadius(int i) {
		return ((Float) balls.get(i).getUserData()) * SCALE_FACTOR;
	}

	public void setBallCoords(GLSprite[] ballSprites) {
		synchronized (this) {
			for (int i = 0; i < balls.size(); i++) {
				float ballRadius = getBallRadius(i);
				Vec2 position = new Vec2(cacheX[i], cacheY[i]);
				Vec2 screen = toScreen(position);
				ballSprites[i].x = screen.x - ballRadius;
				ballSprites[i].y = screen.y - ballRadius;
				ballSprites[i].angle = cacheAngle[i];
			}
		}
	}
}
