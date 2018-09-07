package renderer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Renderer extends GUI {

	private Scene scene;
	private Scene centeredScene;

	// the viewing position for translation
	private Vector3D viewerPosition = new Vector3D(0f, 0f, 0f);

	// zoom scale
	private float currentScale = 1.0f;

	// the viewing angle for rotation
	private float xRot = 0f, yRot = 0f;

	// rotation angle value
	private final float ROTATION_ANGLE = 0.1f;

	@Override
	protected void onLoad(File file) {

		currentScale = 1.0f;
		List<Scene.Polygon> polygons = new ArrayList<>();

		viewerPosition = new Vector3D(0f, 0f, 0f);

		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			// sanity check
			if (line == null) {
				System.out.println("Empty file.");
				br.close();
				return;
			}
			String[] tokens = line.split(" ");

			// parse the light source
			float lightPosX = Float.parseFloat(tokens[0]);
			float lightPosY = Float.parseFloat(tokens[1]);
			float lightPosZ = Float.parseFloat(tokens[2]);
			Vector3D lightPos = new Vector3D(lightPosX, lightPosY, lightPosZ);

			line = br.readLine();

			// parse each polygon
			while(line != null){
				tokens = line.split(" ");

				// construct vertice coordinate array for this polygon
				float v1x = Float.parseFloat(tokens[0]);
				float v1y = Float.parseFloat(tokens[1]);
				float v1z = Float.parseFloat(tokens[2]);
				float v2x = Float.parseFloat(tokens[3]);
				float v2y = Float.parseFloat(tokens[4]);
				float v2z = Float.parseFloat(tokens[5]);
				float v3x = Float.parseFloat(tokens[6]);
				float v3y = Float.parseFloat(tokens[7]);
				float v3z = Float.parseFloat(tokens[8]);
				float[] points = new float[]{v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z};

				// construct RGB color for this polygon
				int r = Integer.parseInt(tokens[9]);
				int g = Integer.parseInt(tokens[10]);
				int b = Integer.parseInt(tokens[11]);
				int[] color = new int[]{r, g, b};

				polygons.add(new Scene.Polygon(points, color));

				line = br.readLine();
			}
			br.close();
			this.scene = new Scene(polygons, lightPos);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onKeyPress(KeyEvent ev) {
		int keyPress = ev.getKeyCode();

		if (keyPress == KeyEvent.VK_UP) {
			rotateX(-ROTATION_ANGLE);
		} else if (keyPress == KeyEvent.VK_DOWN) {
			rotateX(ROTATION_ANGLE);
		} else if (keyPress == KeyEvent.VK_LEFT) {
			rotateY(ROTATION_ANGLE);
		} else if (keyPress == KeyEvent.VK_RIGHT) {
			rotateY(-ROTATION_ANGLE);
		}
	}

	private void rotateX(float angle){
		xRot += angle;
	}

	private void rotateY(float angle){
		yRot += angle;
	}

	@Override
	protected BufferedImage render() {

		Color[][] zBuffer = new Color[CANVAS_WIDTH][CANVAS_HEIGHT];
		float[][] zDepth = new float[CANVAS_WIDTH][CANVAS_HEIGHT];

		// initialise background
		Color bgColor = Color.LIGHT_GRAY;
		for (int i = 0; i < zBuffer.length; i++) {
			for (int j = 0; j < zBuffer[i].length; j++) {
				zBuffer[i][j] = bgColor;
			}
		}

		// initialise pixel depth
		for (int i = 0; i < zDepth.length; i++) {
			for (int j = 0; j < zDepth[i].length; j++) {
				zDepth[i][j] = Float.POSITIVE_INFINITY;
			}
		}

		if (scene == null) {
			return convertBitmapToImage(zBuffer);
		}

		// center scene
		Dimension dimension = new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
		if (centeredScene == null) {
			// scale the scene to fit in the canvas
			float[] boundary = scene.getBoundingBox();
			centeredScene = Pipeline.autoScaleAndTranslate(scene, boundary, dimension);
		}

		// rotate scene
		Scene rotatedScene = Pipeline.rotateScene(centeredScene, xRot, yRot);

		// scale the scene
		Scene scaledScene = Pipeline.scaleScene(rotatedScene, currentScale, currentScale, currentScale);

		// recenter scene
		float[] newBoundary = scaledScene.getBoundingBox();
		Scene recenteredScene = Pipeline.autoTranslate(scaledScene, newBoundary, dimension);

		// translate scene to viewer
		Scene translatedScene = Pipeline.translateScene(recenteredScene, viewerPosition.x, viewerPosition.y, viewerPosition.z);

		//update zBuffer colors for none hidden polygons
		int[] lightColorArray = getDirectLight();
		Color lightColor = new Color(lightColorArray[0], lightColorArray[1], lightColorArray[2]);
		int[] ambientColorArray = getAmbientLight();
		Color ambientColor = new Color(ambientColorArray[0], ambientColorArray[1], ambientColorArray[2]);
		Vector3D lightVector = translatedScene.getLight();
		List<Scene.Polygon> polygons = translatedScene.getPolygons();
		for (Scene.Polygon p : polygons) {
			if (Pipeline.isHidden(p)) {
				continue;
			}

			Color shading = Pipeline.getShading(p, lightVector, lightColor, ambientColor);
			EdgeList edgeList = Pipeline.computeEdgeList(p);
			Pipeline.computeZBuffer(zBuffer, zDepth, edgeList, shading);
		}

		return convertBitmapToImage(zBuffer);
	}

	/**
	 * Converts a 2D array of Colors to a BufferedImage. Assumes that bitmap is
	 * indexed by column then row and has imageHeight rows and imageWidth
	 * columns. Note that image.setRGB requires x (col) and y (row) are given in
	 * that order.
	 */
	private BufferedImage convertBitmapToImage(Color[][] bitmap) {
		BufferedImage image = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < CANVAS_WIDTH; x++) {
			for (int y = 0; y < CANVAS_HEIGHT; y++) {
				image.setRGB(x, y, bitmap[x][y].getRGB());
			}
		}
		return image;
	}

	public static void main(String[] args) {
		new Renderer();
	}
}

// code for comp261 assignments
