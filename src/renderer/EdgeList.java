package renderer;

/**
 * EdgeList should store the data for the edge list of a single polygon in your
 * scene. A few method stubs have been provided so that it can be tested, but
 * you'll need to fill in all the details.
 *
 * You'll probably want to add some setters as well as getters or, for example,
 * an addRow(y, xLeft, xRight, zLeft, zRight) method.
 */
public class EdgeList {

	private int startY, endY;
	private float[] leftX, rightX, leftZ, rightZ;

	public EdgeList(int startY, int endY) {
		this.startY = startY;
		this.endY = endY;
		int ty = endY - startY;

		leftX = new float[ty];
		rightX = new float[ty];
		leftZ = new float[ty];
		rightZ = new float[ty];

		for (int i = 0; i < ty; i++) {
			leftX[i] = Float.POSITIVE_INFINITY;
			rightX[i] = Float.NEGATIVE_INFINITY;
			leftZ[i] = Float.POSITIVE_INFINITY;
			rightZ[i] = Float.POSITIVE_INFINITY;
		}
	}

	public int getStartY() {
		return startY;
	}

	public int getEndY() {
		return endY;
	}

	public float getLeftX(int y) {
		return leftX[y];
	}

	public float getRightX(int y) {
		return rightX[y];
	}

	public float getLeftZ(int y) {
		return leftZ[y];
	}

	public float getRightZ(int y) {
		return rightZ[y];
	}

	public void addRow(int y, float x, float z) {

        /*
         *  When the first vector is processed, both left and right values
         *  are overwritten by a same value. When the vector on the other
         *  side (either left or right) is processed, only one of them is
         *  updated.
         */

		if (x <= this.leftX[y]) {
			this.leftX[y] = x;
			this.leftZ[y] = z;
		}

		if (x >= this.rightX[y]) {
			this.rightX[y] = x;
			this.rightZ[y] = z;
		}
	}

}

// code for comp261 assignments
