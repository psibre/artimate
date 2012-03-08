package fr.loria.parole.artimate.demo;

import java.io.IOException;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.screen.ScreenExporter;
import com.ardor3d.util.stat.StatCollector;

import fr.loria.parole.artimate.data.io.ColladaTextGridModel;
import fr.loria.parole.artimate.data.io.XWavesSegmentation;
import fr.loria.parole.artimate.engine.Ardor3DWrapper;
import fr.loria.parole.artimate.synthesis.AnimationSynthesizer;

/**
 * Borrowing heavily from <a href=
 * "http://ardorlabs.trac.cvsdude.com/Ardor3Dv1/browser/trunk/ardor3d-examples/src/main/java/com/ardor3d/example/basic/OrbitCamExample.java?rev=1393"
 * >OrbitCamExample</a> and <a href=
 * "http://ardorlabs.trac.cvsdude.com/Ardor3Dv1/browser/trunk/ardor3d-examples/src/main/java/com/ardor3d/example/pipeline/SimpleColladaExample.java?rev=1557"
 * >SimpleColladaExample</a>
 * 
 * @author steiner
 * 
 */
public class DemoApp implements Runnable, Updater, Scene {

	private static final Logger logger = Logger.getLogger(DemoApp.class.getName());

	/** If true (the default) we will call System.exit on end of demo. */
	public static boolean QUIT_VM_ON_EXIT = true;

	public Ardor3DWrapper ardor3d;

	public AnimationSynthesizer synthesizer;

	protected void initExample(String modelFileName) {
		ardor3d._canvas.setTitle("OrbitCam DemoApp");

		// Load the collada scene
		try {
			ColladaTextGridModel model = new ColladaTextGridModel(ardor3d.animation, modelFileName);
			synthesizer = new AnimationSynthesizer(model.getUnitDB(), ardor3d);

			ardor3d._control.setLookAtSpatial(model.getScene());
			ardor3d._root.attachChild(model.getScene());

			try {
				XWavesSegmentation testsegmentation = new XWavesSegmentation("test.lab");
				synthesizer.synthesizeAnimation(testsegmentation);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	public void run() {
		try {
			ardor3d._frameHandler.init();

			while (!ardor3d._exit) {
				ardor3d._frameHandler.updateFrame();
				Thread.yield();
			}
			ardor3d.quit();
			if (QUIT_VM_ON_EXIT) {
				System.exit(0);
			}
		} catch (final Throwable t) {
			System.err.println("Throwable caught in MainThread - exiting");
			t.printStackTrace(System.err);
		}
	}

	@MainThread
	public void init() {
		final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
		logger.info("Display Vendor: " + caps.getDisplayVendor());
		logger.info("Display Renderer: " + caps.getDisplayRenderer());
		logger.info("Display Version: " + caps.getDisplayVersion());
		logger.info("Shading Language Version: " + caps.getShadingLanguageVersion());

		ardor3d.registerInputTriggers(this);

		AWTImageLoader.registerLoader();

		/**
		 * Create a ZBuffer to display pixels closest to the camera above farther ones.
		 */
		final ZBufferState buf = new ZBufferState();
		buf.setEnabled(true);
		buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		ardor3d._root.setRenderState(buf);

		// ---- LIGHTS
		/** Set up a basic, default light. */
		final PointLight light = new PointLight();
		light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
		light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
		light.setLocation(new Vector3(100, 100, 100));
		light.setEnabled(true);

		/** Attach the light to a lightState and the lightState to rootNode. */
		ardor3d._lightState = new LightState();
		ardor3d._lightState.setEnabled(true);
		ardor3d._lightState.attach(light);
		ardor3d._root.setRenderState(ardor3d._lightState);

		ardor3d._wireframeState = new WireframeState();
		ardor3d._wireframeState.setEnabled(false);
		ardor3d._root.setRenderState(ardor3d._wireframeState);

		ardor3d._root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);

		// TODO make this flexible!
		// initExample("flexiquad.dae", "Cube", "Cube_001-mesh");
		initExample("Tongue.dae");

		ardor3d._root.updateGeometricState(0);
	}

	@MainThread
	public void update(final ReadOnlyTimer timer) {
		if (ardor3d._canvas.isClosing()) {
			ardor3d._exit = true;
		}

		/** update stats, if enabled. */
		if (Constants.stats) {
			StatCollector.update();
		}

		updateLogicalLayer(timer);

		// Execute updateQueue item
		GameTaskQueueManager.getManager(ardor3d._canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.UPDATE)
				.execute();

		/** Call simpleUpdate in any derived classes of ExampleBase. */
		ardor3d.update(timer);

		/** Update controllers/render states/transforms/bounds for rootNode. */
		ardor3d._root.updateGeometricState(timer.getTimePerFrame(), true);
	}

	protected void updateLogicalLayer(final ReadOnlyTimer timer) {
		// check and execute any input triggers, if we are concerned with input
		if (ardor3d._logicalLayer != null) {
			ardor3d._logicalLayer.checkTriggers(timer.getTimePerFrame());
		}
	}

	@MainThread
	public boolean renderUnto(final Renderer renderer) {
		// Execute renderQueue item
		GameTaskQueueManager.getManager(ardor3d._canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.RENDER)
				.execute(renderer);

		// Clean up card garbage such as textures, vbos, etc.
		ContextGarbageCollector.doRuntimeCleanup(renderer);

		/** Draw the rootNode and all its children. */
		if (!ardor3d._canvas.isClosing()) {
			/** Call renderExample in any derived classes. */
			renderDemo(renderer);
			renderDebug(renderer);

			if (ardor3d._doShot) {
				// force any waiting scene elements to be rendered.
				renderer.renderBuckets();
				ScreenExporter.exportCurrentScreen(ardor3d._canvas.getCanvasRenderer().getRenderer(), ardor3d._screenShotExp);
				ardor3d._doShot = false;
			}
			return true;
		} else {
			return false;
		}
	}

	protected void renderDemo(final Renderer renderer) {
		ardor3d._root.onDraw(renderer);
	}

	protected void renderDebug(final Renderer renderer) {
		if (ardor3d._showNormals) {
			Debugger.drawNormals(ardor3d._root, renderer);
			Debugger.drawTangents(ardor3d._root, renderer);
		}
	}

	public PickResults doPick(final Ray3 pickRay) {
		return null;
	}

	public static void main(final String[] args) {
		start(DemoApp.class);
	}

	public static void start(final Class<? extends DemoApp> demoClazz) {

		DemoApp demo;
		try {
			demo = demoClazz.newInstance();
		} catch (final Exception ex) {
			ex.printStackTrace();
			return;
		}

		// get our framework
		demo.ardor3d = new Ardor3DWrapper(demo);

		// Register our example as an updater.
		demo.ardor3d._frameHandler.addUpdater(demo);

		new Thread(demo).start();
	}
}
