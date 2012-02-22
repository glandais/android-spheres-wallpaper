package org.glandais.android.livespheres;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

//import android.util.Log;

public class PhysicsWorld {

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
	private Body touchedBody = null;
	private Vec2 touchedDelta;

	private World world;
	private float minaxis;

	public PhysicsWorld() {
		recreateWorld(16.0f, 12.0f, 0.1f);
	}

	public void recreateWorld(float xmax, float ymax, float ballratio) {
		touchedBody = null;
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
		float ballsize = ballratio * minaxis;
		for (int i = 0; i < 10; i++) {

			float x = (float) (ballsize + Math.random() * (xmax - ballsize));
			float y = (float) (ballsize + Math.random() * (ymax - ballsize));

			Vec2 position = new Vec2(x, y);
			addBall(position, ballsize * (0.9f + 0.2f * (float) Math.random()));
		}

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

	public void touch(Vec2 worldPosition) {
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

	public void addBall(Vec2 position, float radius) {
		// Log.i("World", "Add ball " + position + " / " + radius);

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(position);
		Body body = world.createBody(bodyDef);

		CircleShape shape = new CircleShape();
		shape.m_radius = radius;

		// body.createFixture(shape, 1.0f);

		FixtureDef def = new FixtureDef();
		def.density = 1.0f;
		// 1 =
		def.friction = 0.7f + 0.2f * (float) Math.random();
		// 1 = rebond
		def.restitution = 0.7f;
		def.shape = shape;
		body.createFixture(def);

		body.resetMassData();

		body.setUserData(new Float(radius));

		balls.add(body);
	}

	public List<Body> getBalls() {
		return balls;
	}

	public void setGravity(float x, float y, float ratio) {
		world.setGravity(new Vec2(x * ratio, y * ratio));
	}

	public void update() {
		for (int i = 0; i < SIM_COUNT; i++) {
			world.step(PHYSIC_STEP_SEC, VEL_ITER, POS_ITER);
		}
	}

	public void touchUp(Vec2 worldPosition) {
		// Log.i("World", "touchUp " + worldPosition);
		touchedBody = null;
	}

	public void touchDown(Vec2 worldPosition) {
		// Log.i("World", "touchDown " + worldPosition);
		float minDist = minaxis * 4.0f;
		for (Body ball : balls) {
			Vec2 ballPosition = ball.getPosition();
			Vec2 dist = ballPosition.sub(worldPosition);
			float length = dist.length();

			Float radius = (Float) ball.getUserData();
			// Log.i("World", "touchDown " + length + " / " + radius + " / "
			// + minDist);
			if (length < radius && length < minDist) {
				touchedBody = ball;
				minDist = length;
				touchedDelta = dist;
			}
		}
	}

	public void touchMove(Vec2 worldPosition) {
		// Log.i("World", "touchMove " + worldPosition);
		if (touchedBody != null) {
			Vec2 ballPosition = touchedBody.getPosition();
			Vec2 force = worldPosition.add(touchedDelta).sub(ballPosition);
			force.normalize();
			force.mul(50.0f);
			// Log.i("World", "touchMove " + force + " / " + ballPosition);
			touchedBody.applyLinearImpulse(force, ballPosition);
		}
	}

}
