package org.glandais.android.livespheres;

import java.util.ArrayList;
import java.util.List;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;

import android.util.Log;

public class PhysicsWorld {

	public static final float FRAMERATE = 15;
	public static final float TIME_STEP_SEC = (1f / FRAMERATE);
	public static final int TIME_STEP_MS = Math.round(1000.0f / FRAMERATE);
	public static final int VEL_ITER = 3;
	public static final int POS_ITER = 8;

	private List<Body> bodies = new ArrayList<Body>();

	private World world;

	public PhysicsWorld() {
		recreateWorld(16.0f, 12.0f);
	}

	public void recreateWorld(float xmax, float ymax) {
		Log.i("World", "RecreateWorld " + xmax + " / " + ymax);
		bodies.clear();

		Vec2 gravity = new Vec2(0.0f, 0.0f);
		world = new World(gravity, false);

		BodyDef def = new BodyDef();
		def.type = BodyType.STATIC;
		Body groundBody = world.createBody(def);
		createEdge(0.0f, 0.0f, xmax, 0.0f, groundBody);
		createEdge(xmax, 0.0f, xmax, ymax, groundBody);
		createEdge(xmax, ymax, 0.0f, ymax, groundBody);
		createEdge(0.0f, ymax, 0.0f, 0.0f, groundBody);
	}

	private void createEdge(float x1, float y1, float x2, float y2,
			Body groundBody) {
		Vec2 v1 = new Vec2(x1, y1);
		Vec2 v2 = new Vec2(x2, y2);

		PolygonShape groundShapeDef = new PolygonShape();
		groundShapeDef.setAsEdge(v1, v2);

		groundBody.createFixture(groundShapeDef, 1.0f);
	}

	public void addBall(Vec2 position, float radius) {
		if (bodies.size() > 10) {
			return;
		}
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DYNAMIC;
		bodyDef.position.set(position);
		Body body = world.createBody(bodyDef);

		CircleShape shape = new CircleShape();
		shape.m_radius = radius;

		body.createFixture(shape, 1.0f);
		body.resetMassData();

		bodies.add(body);
	}

	public List<Body> getBalls() {
		return bodies;
	}

	public void setGravity(float x, float y) {
		world.setGravity(new Vec2(x, y));
	}

	public void update() {
		world.setContinuousPhysics(true);
		world.setWarmStarting(true);
		world.step(TIME_STEP_SEC, VEL_ITER, POS_ITER);
	}

}
