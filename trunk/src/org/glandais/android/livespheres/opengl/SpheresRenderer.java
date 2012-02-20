package org.glandais.android.livespheres.opengl;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import org.glandais.android.livespheres.R;
import org.glandais.android.livespheres.opengl.sprites.GLSprite;
import org.glandais.android.livespheres.opengl.sprites.Grid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLUtils;
import android.util.Log;

// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class SpheresRenderer implements GLWallpaperService.Renderer {

	// Specifies the format our textures should be converted to upon load.
	private static BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
	// An array of things to draw every frame.
	private GLSprite[] mSprites;
	protected GLSprite[] ballSprites;
	// Pre-allocated arrays to use at runtime so that allocation during the
	// test can be avoided.
	private int[] mTextureNameWorkspace;
	private int[] mCropWorkspace;
	// A reference to the application context.
	private Context mContext;
	// Determines the use of vertex arrays.
	private boolean mUseVerts;
	// Determines the use of vertex buffer objects.
	private boolean mUseHardwareBuffers;
	private PhysicsWorld mWorld;

	public SpheresRenderer(Context context, boolean useVerts,
			boolean useHardwareBuffers, PhysicsWorld world) {
		super();
		mSprites = new GLSprite[0];
		ballSprites = new GLSprite[0];
		// Pre-allocate and store these objects so we can use them at runtime
		// without allocating memory mid-frame.
		mTextureNameWorkspace = new int[1];
		mCropWorkspace = new int[4];

		// Set our bitmaps to 16-bit, 565 format.
		sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

		mContext = context;

		mUseVerts = useVerts;
		mUseHardwareBuffers = useVerts ? useHardwareBuffers : false;

		mWorld = world;

		updateSpriteArray();
	}

	public void updateSpriteArray() {
		mSprites = new GLSprite[1 + mWorld.getBallCount()];
		ballSprites = new GLSprite[mWorld.getBallCount()];

		mSprites[0] = new GLSprite(R.drawable.background);
		BitmapDrawable backgroundImage = (BitmapDrawable) mContext
				.getResources().getDrawable(R.drawable.background);
		Bitmap backgoundBitmap = backgroundImage.getBitmap();
		mSprites[0].width = backgoundBitmap.getWidth();
		mSprites[0].height = backgoundBitmap.getHeight();
		if (mUseVerts) {
			// Setup the background grid. This is just a quad.
			Grid backgroundGrid = new Grid(2, 2, false);
			backgroundGrid.set(0, 0, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, null);
			backgroundGrid.set(1, 0, mSprites[0].width, 0.0f, 0.0f, 1.0f, 1.0f,
					null);
			backgroundGrid.set(0, 1, 0.0f, mSprites[0].height, 0.0f, 0.0f,
					0.0f, null);
			backgroundGrid.set(1, 1, mSprites[0].width, mSprites[0].height,
					0.0f, 1.0f, 0.0f, null);
			mSprites[0].setGrid(backgroundGrid);
		}

		// This list of things to move. It points to the same content as the
		// sprite list except for the background.
		for (int x = 0; x < mWorld.getBallCount(); x++) {
			float ball_size = mWorld.getBallRadius(x) * 2;
			GLSprite ball = new GLSprite(R.drawable.ball);

			ball.width = ball_size;
			ball.height = ball_size;

			// Pick a random location for this sprite.
			ball.x = (float) (Math.random() * 100.0);
			ball.y = (float) (Math.random() * 100.0);

			if (mUseVerts) {
				Grid ballGrid = new Grid(2, 2, false);
				ballGrid.set(0, 0, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, null);
				ballGrid.set(1, 0, ball_size, 0.0f, 0.0f, 1.0f, 1.0f, null);
				ballGrid.set(0, 1, 0.0f, ball_size, 0.0f, 0.0f, 0.0f, null);
				ballGrid.set(1, 1, ball_size, ball_size, 0.0f, 1.0f, 0.0f, null);
				ball.setGrid(ballGrid);
			}

			mSprites[x + 1] = ball;
			ballSprites[x] = ball;
		}
	}

	public void onDrawFrame(GL10 gl) {
		if (mSprites != null) {
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			if (mUseVerts) {
				Grid.beginDrawing(gl, true, false);
			}
			mWorld.setBallCoords(ballSprites);
			for (int x = 0; x < mSprites.length; x++) {
				mSprites[x].draw(gl);
			}
			if (mUseVerts) {
				Grid.endDrawing(gl);
			}
		}
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);

		/*
		 * Set our projection matrix. This doesn't have to be done each time we
		 * draw, but usually a new projection needs to be set when the viewport
		 * is resized.
		 */
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);
		gl.glEnable(GL10.GL_TEXTURE_2D);
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		/*
		* Some one-time OpenGL initialization can be made here probably based
		* on features of this particular context
		*/
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

		gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		/*
		 * By default, OpenGL enables features that improve quality but reduce
		 * performance. One might want to tweak that especially on software
		 * renderer.
		 */
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_LIGHTING);

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		updateSpriteArray();

		// If we are using hardware buffers and the screen lost context
		// then the buffer indexes that we recorded previously are now
		// invalid. Forget them here and recreate them below.
		if (mUseHardwareBuffers) {
			if (mSprites != null) {
				for (int x = 0; x < mSprites.length; x++) {
					// Ditch old buffer indexes.
					GLSprite glSprite = mSprites[x];
					if (glSprite != null) {
						Grid grid = glSprite.getGrid();
						if (grid != null) {
							grid.invalidateHardwareBuffers();
						}
					}
				}
			}
		}

		// Load our texture and set its texture name on all sprites.

		// To keep this sample simple we will assume that sprites that share
		// the same texture are grouped together in our sprite list. A real
		// app would probably have another level of texture management,
		// like a texture hash.

		int lastLoadedResource = -1;
		int lastTextureId = -1;

		for (int x = 0; x < mSprites.length; x++) {
			int resource = mSprites[x].getResourceId();
			if (resource != lastLoadedResource) {
				lastTextureId = loadBitmap(mContext, gl, resource);
				lastLoadedResource = resource;
			}
			mSprites[x].setTextureName(lastTextureId);
			if (mUseHardwareBuffers) {
				// Grid currentGrid = mSprites[x].getGrid();
				// if (!currentGrid.usingHardwareBuffers()) {
				// currentGrid.generateHardwareBuffers(gl);
				// }
				mSprites[x].getGrid().generateHardwareBuffers(gl);
			}
		}
	}

	/**
	 * Called when the rendering thread shuts down. This is a good place to
	 * release OpenGL ES resources.
	 * 
	 * @param gl
	 */
	public void shutdown() {
		if (mSprites != null) {

			int lastFreedResource = -1;
			int[] textureToDelete = new int[1];

			for (int x = 0; x < mSprites.length; x++) {
				int resource = mSprites[x].getResourceId();
				if (resource != lastFreedResource) {
					textureToDelete[0] = mSprites[x].getTextureName();
					// gl.glDeleteTextures(1, textureToDelete, 0);
					mSprites[x].setTextureName(0);
				}
				if (mUseHardwareBuffers) {
					// mSprites[x].getGrid().releaseHardwareBuffers(gl);
				}
			}
		}
	}

	/**
	 * Loads a bitmap into OpenGL and sets up the common parameters for 2D
	 * texture maps.
	 */
	protected int loadBitmap(Context context, GL10 gl, int resourceId) {
		int textureName = -1;
		if (context != null && gl != null) {
			gl.glGenTextures(1, mTextureNameWorkspace, 0);

			textureName = mTextureNameWorkspace[0];
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textureName);

			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
					GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
					GL10.GL_LINEAR);

			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
					GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
					GL10.GL_CLAMP_TO_EDGE);

			gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
					GL10.GL_REPLACE);

			InputStream is = context.getResources().openRawResource(resourceId);
			Bitmap bitmap;
			try {
				bitmap = BitmapFactory.decodeStream(is, null, sBitmapOptions);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					// Ignore.
				}
			}

			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

			mCropWorkspace[0] = 0;
			mCropWorkspace[1] = bitmap.getHeight();
			mCropWorkspace[2] = bitmap.getWidth();
			mCropWorkspace[3] = -bitmap.getHeight();

			bitmap.recycle();

			((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D,
					GL11Ext.GL_TEXTURE_CROP_RECT_OES, mCropWorkspace, 0);

			int error = gl.glGetError();
			if (error != GL10.GL_NO_ERROR) {
				Log.e("SpriteMethodTest", "Texture Load GLError: " + error);
			}

		}

		return textureName;
	}

}
