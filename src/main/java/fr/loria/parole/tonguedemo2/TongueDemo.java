package fr.loria.parole.tonguedemo2;

import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Borrowing heavily from <a href=
 * "http://ardorlabs.trac.cvsdude.com/Ardor3Dv1/browser/trunk/ardor3d-examples/src/main/java/com/ardor3d/example/basic/LwjglBasicExample.java?rev=1745"
 * >LwjglBasicExample</a> and <a href=
 * "http://ardorlabs.trac.cvsdude.com/Ardor3Dv1/browser/trunk/ardor3d-examples/src/main/java/com/ardor3d/example/pipeline/ColladaExample.java?rev=1745"
 * >ColladaExample</a>
 * 
 * @author steiner
 * 
 */
public class TongueDemo implements Scene {

	private LwjglCanvas canvas;
	private Timer timer = new Timer();
	private final Node root = new Node();
	private boolean exit;
	private Node colladaNode;

	TongueDemo() {
		canvas = initLwjgl();
		canvas.init();
	}

	private LwjglCanvas initLwjgl() {
		final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
		final DisplaySettings settings = new DisplaySettings(800, 600, 24, 0, 0, 8, 0, 0, false, false);
		return new LwjglCanvas(canvasRenderer, settings);
	}

	private void start() {
		initExample();

		// Run in this same thread.
		while (!exit) {
			updateExample();
			canvas.draw(null);
			Thread.yield();
		}
		canvas.getCanvasRenderer().makeCurrentContext();

		// Done, do cleanup
		ContextGarbageCollector.doFinalCleanup(canvas.getCanvasRenderer().getRenderer());
		canvas.close();

		canvas.getCanvasRenderer().releaseCurrentContext();
	}

	private void initExample() {
		ResourceSource model = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, "flexiquad.dae");
		loadColladaModel(model);
	}

	private void loadColladaModel(final ResourceSource source) {
		try {
			// detach the old colladaNode, if present.
			root.detachChild(colladaNode);

			final long time = System.currentTimeMillis();
			final ColladaImporter colladaImporter = new ColladaImporter();

			// Load the collada scene
			final ColladaStorage storage = colladaImporter.load(source);
			colladaNode = storage.getScene();

			System.out.println("Importing: " + source);
			System.out.println("Took " + (System.currentTimeMillis() - time) + " ms");

			// Add colladaNode to root
			root.attachChild(colladaNode);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	private void updateExample() {
		if (canvas.isClosing()) {
			exit = true;
			return;
		}

		timer.update();

		// Update controllers/render states/transforms/bounds for rootNode.
		root.updateGeometricState(timer.getTimePerFrame(), true);
	}

	public static void main(String args[]) {
		TongueDemo demo = new TongueDemo();
		demo.start();
	}

	@Override
	public boolean renderUnto(Renderer renderer) {
		if (!canvas.isClosing()) {

			// Draw the root and all its children.
			renderer.draw(root);

			return true;
		}
		return false;
	}

	@Override
	public PickResults doPick(Ray3 pickRay) {
		// Ignore
		return null;
	}
}
