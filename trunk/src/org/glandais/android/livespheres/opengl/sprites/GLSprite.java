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

package org.glandais.android.livespheres.opengl.sprites;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11Ext;

/**
 * This is the OpenGL ES version of a sprite. It is more complicated than the
 * CanvasSprite class because it can be used in more than one way. This class
 * can draw using a grid of verts, a grid of verts stored in VBO objects, or
 * using the DrawTexture extension.
 */
public class GLSprite {

	// Position.
	public float x;
	public float y;
	public float angle;

	// Size.
	public float width;
	public float height;

	// The OpenGL ES texture handle to draw.
	private int mTextureName;
	// The id of the original resource that mTextureName is based on.
	private int mResourceId;
	private Grid mGrid;

	public GLSprite(int resourceId) {
		super();
		mResourceId = resourceId;
	}

	public void setTextureName(int name) {
		mTextureName = name;
	}

	public int getTextureName() {
		return mTextureName;
	}

	public void setResourceId(int id) {
		mResourceId = id;
	}

	public int getResourceId() {
		return mResourceId;
	}

	public void setGrid(Grid grid) {
		mGrid = grid;
	}

	public Grid getGrid() {
		return mGrid;
	}

	public void draw(GL10 gl) {
		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureName);

		if (mGrid == null) {
			// Draw using the DrawTexture extension.
			((GL11Ext) gl).glDrawTexfOES(x, y, 0.0f, width, height);
		} else {

			// Draw using verts or VBO verts.
			gl.glPushMatrix();
			gl.glLoadIdentity();
			// gl.glRotatef(angle, mTextureName, y, z)
			gl.glTranslatef(x, y, 0.0f);

			mGrid.draw(gl, true, false);

			gl.glPopMatrix();

		}
	}
}
