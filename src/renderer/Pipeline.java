package renderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import renderer.Scene.Polygon;

/**
 * The Pipeline class has method stubs for all the major components of the
 * rendering pipeline, for you to fill in.
 * 
 * Some of these methods can get quite long, in which case you should strongly
 * consider moving them out into their own file. You'll need to update the
 * imports in the test suite if you do.
 */
public class Pipeline {

	/**
	 * Returns true if the given polygon is facing away from the camera (and so
	 * should be hidden), and false otherwise.
	 */
	public static boolean isHidden(Polygon poly) {
		return poly.getNormal().z > 1e-10;
	}

	/**
	 * Computes the colour of a polygon on the screen, once the lights, their
	 * angles relative to the polygon's face, and the reflectance of the polygon
	 * have been accounted for.
	 * 
	 * @param lightDirection
	 *            The Vector3D pointing to the directional light read in from
	 *            the file.
	 * @param lightColor
	 *            The color of that directional light.
	 * @param ambientLight
	 *            The ambient light in the scene, i.e. light that doesn't depend
	 *            on the direction.
	 */
	public static Color getShading(Polygon poly, Vector3D lightDirection, Color lightColor, Color ambientLight) {
		Vector3D normal = poly.getNormal();
		double cos = normal.cosTheta(lightDirection);

		int r, g, b;

		// if directional light
		if (cos > 0) {
			r = (int) ((ambientLight.getRed() + lightColor.getRed() * cos) *
						poly.reflectance.getRed() / 255.0f);
			g = (int) ((ambientLight.getGreen()+ lightColor.getGreen() * cos) *
						poly.reflectance.getGreen()  / 255.0f);
			b = (int) ((ambientLight.getBlue()+ lightColor.getBlue() * cos) *
						poly.reflectance.getBlue() / 255.0f);
		}
		else {
			r = (int) (poly.reflectance.getRed() / 255.0f * ambientLight.getRed());
			g = (int) (poly.reflectance.getGreen() / 255.0f * ambientLight.getGreen());
			b = (int) (poly.reflectance.getBlue() / 255.0f * ambientLight.getBlue());
		}

		r = r > 255 ? 255 : r;
		g = g > 255 ? 255 : g;
		b = b > 255 ? 255 : b;

		return new Color(r, g, b);
	}

	/**
	 * This method should rotate the polygons and light such that the viewer is
	 * looking down the Z-axis. The idea is that it returns an entirely new
	 * Scene object, filled with new Polygons, that have been rotated.
	 * 
	 * @param scene
	 *            The original Scene.
	 * @param xRot
	 *            An angle describing the viewer's rotation in the YZ-plane (i.e
	 *            around the X-axis).
	 * @param yRot
	 *            An angle describing the viewer's rotation in the XZ-plane (i.e
	 *            around the Y-axis).
	 * @return A new Scene where all the polygons and the light source have been
	 *         rotated accordingly.
	 */
	public static Scene rotateScene(Scene scene, float xRot, float yRot) {
		Transform rotationMatrix = Transform.newXRotation(xRot).compose(Transform.newYRotation(yRot));

		return processSceneWithMatrix(scene, rotationMatrix);
	}

	/**
	 * This should translate the scene by the appropriate amount.
	 * 
	 * @param scene
	 * @return
	 */
	public static Scene translateScene(Scene scene) {
		// TODO fill this in.
		return null;
	}

	/**
	 * This should scale the scene.
	 * 
	 * @param scene
	 * @return
	 */
	public static Scene scaleScene(Scene scene) {
		// TODO fill this in.
		return null;
	}

	/**
	 * Processes the scene (polygons & light) with a given matrix.
	 *
	 * @param scene
	 * @param matrix   given rotation, translation, or scale
	 * @return
	 */
	private static Scene processSceneWithMatrix(Scene scene, Transform matrix) {
		Vector3D processedLightPos = matrix.multiply(scene.getLight());

		List<Polygon> processedPolygons = new ArrayList<>();
		for (Polygon p : scene.getPolygons()) {
			Vector3D[] processedVectors = new Vector3D[3];
			for (int i = 0; i < processedVectors.length; i++) {
				processedVectors[i] = matrix.multiply(p.vertices[i]);
			}
			Polygon processedPolygon = new Polygon(processedVectors[0], processedVectors[1],
					processedVectors[2], p.reflectance);
			processedPolygons.add(processedPolygon);
		}

		return new Scene(processedPolygons, processedLightPos);
	};

	/**
	 * Computes the edgelist of a single provided polygon, as per the lecture
	 * slides.
	 */
	public static EdgeList computeEdgeList(Polygon poly) {
		Vector3D vertex1 = poly.vertices[0];
		Vector3D vertex2 = poly.vertices[1];
		Vector3D vertex3 = poly.vertices[2];
		int startY = (int) Math.min(Math.min(vertex1.y, vertex2.y), vertex3.y);
		int endY = (int) Math.max(Math.max(vertex1.y, vertex2.y), vertex3.y);

		EdgeList edgeList = new EdgeList(startY, endY);

		for (int i = 0; i < poly.vertices.length; i++) {
			int j = i + 1;
			j = j == 3 ? 0 : j; // to prevent index out of boundary exception
			Vector3D vertexUp;
			Vector3D vertexDown;

			if (poly.vertices[i].y == poly.vertices[j].y) {
				continue; // these two vertices have same y value
			} else if (poly.vertices[i].y < poly.vertices[j].y) {
				vertexUp = poly.vertices[i];
				vertexDown = poly.vertices[j];
			} else {
				vertexUp = poly.vertices[j];
				vertexDown = poly.vertices[i];
			}

			float mx = (vertexDown.x - vertexUp.x) / (vertexDown.y - vertexUp.y);
			float mz = (vertexDown.z - vertexUp.z) / (vertexDown.y - vertexUp.y);

			float x = vertexUp.x;
			float z = vertexUp.z;

			int y = (int) vertexUp.y;
			int yEnd = (int) vertexDown.y;

			for ( ; y < yEnd; y++, x += mx, z += mz) {
				edgeList.addRow(y - startY, x, z);
			}
		}

		return edgeList;
	}

	/**
	 * Fills a zbuffer with the contents of a single edge list according to the
	 * lecture slides.
	 * 
	 * The idea here is to make zbuffer and zdepth arrays in your main loop, and
	 * pass them into the method to be modified.
	 * 
	 * @param zbuffer
	 *            A double array of colours representing the Color at each pixel
	 *            so far.
	 * @param zdepth
	 *            A double array of floats storing the z-value of each pixel
	 *            that has been coloured in so far.
	 * @param polyEdgeList
	 *            The edgelist of the polygon to add into the zbuffer.
	 * @param polyColor
	 *            The colour of the polygon to add into the zbuffer.
	 */
	public static void computeZBuffer(Color[][] zbuffer, float[][] zdepth, EdgeList polyEdgeList, Color polyColor) {
		// TODO fill this in.
	}
}

// code for comp261 assignments
