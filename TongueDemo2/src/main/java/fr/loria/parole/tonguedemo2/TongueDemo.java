package fr.loria.parole.tonguedemo2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.image.util.ScreenShotImageExporter;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.input.logical.AnyKeyCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseManager;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.lwjgl.LwjglTextureRendererProvider;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.screen.ScreenExporter;
import com.ardor3d.util.stat.StatCollector;

import fr.loria.parole.tonguedemo2.io.XWavesSegmentation;
import fr.loria.parole.tonguedemo2.segmentation.Segment;

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
// public class TongueDemo extends ExampleBase {
public class TongueDemo implements Runnable, Updater, Scene {

	private static final Logger logger = Logger.getLogger(TongueDemo.class.getName());

	/** If true (the default) we will call System.exit on end of demo. */
	public static boolean QUIT_VM_ON_EXIT = true;

	protected final LogicalLayer _logicalLayer = new LogicalLayer();

	protected PhysicalLayer _physicalLayer;

	protected final Timer _timer = new Timer();
	protected final FrameHandler _frameHandler = new FrameHandler(_timer);

	protected DisplaySettings _settings;

	protected final Node _root = new Node();

	protected LightState _lightState;

	protected WireframeState _wireframeState;

	protected volatile boolean _exit = false;

	protected boolean _showNormals = false;

	protected boolean _doShot = false;

	protected LwjglCanvas _canvas;

	protected ScreenShotImageExporter _screenShotExp = new ScreenShotImageExporter();

	protected MouseManager _mouseManager;

	/** Our orbiter control. */
	private OrbitCamControl control;
	private ColladaStorage storage;
	private AnimationManager manager;

	private boolean _showSkeleton = false;

	protected ArrayList<Segment> _animations = new ArrayList<Segment>();
	protected int _animationIndex = 0;

	public static void main(final String[] args) {
		start(TongueDemo.class);
	}

	protected void initExample() {
		_canvas.setTitle("OrbitCam TongueDemo");

		// Load the collada scene
		try {
//			storage = new ColladaImporter().load("flexiquad.dae");
//			Node cube = (Node) storage.getScene().getChild("Cube");
//			Node geom = (Node) cube.getChild("geometry");
//			SkinnedMesh mesh = (SkinnedMesh) geom.getChild("Cube_001-mesh");
			storage = new ColladaImporter().load("Tongue.dae");
			Node cube = (Node) storage.getScene().getChild("TongueMesh");
			Node geom = (Node) cube.getChild("geometry");
			SkinnedMesh mesh = (SkinnedMesh) geom.getChild("grp9-mesh[mtl91]");
			control.setLookAtSpatial(mesh);
			_root.attachChild(storage.getScene());
			setupAnimations(storage);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void registerInputTriggers() {
		// check if this example worries about input at all
		if (_logicalLayer == null) {
			return;
		}

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				exit();
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.A), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_animationIndex++;
				if (_animationIndex >= _animations.size()) {
					_animationIndex = 0;
				}
				logger.info("Switched to animation " + _animations.get(_animationIndex).getLabel());
				manager.getBaseAnimationLayer().setCurrentState(_animations.get(_animationIndex).getLabel(), true);
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.T), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_wireframeState.setEnabled(!_wireframeState.isEnabled());
				// Either an update or a markDirty is needed here since we did not touch the affected spatial directly.
				_root.markDirty(DirtyType.RenderState);
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.C), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				System.out.println("Camera: " + _canvas.getCanvasRenderer().getCamera());
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.N), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_showNormals = !_showNormals;
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.S), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_showSkeleton = !_showSkeleton;
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F1), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_doShot = true;
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				if (_mouseManager.isSetGrabbedSupported()) {
					_mouseManager.setGrabbed(GrabbedState.GRABBED);
				}
			}
		}));
		_logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				if (_mouseManager.isSetGrabbedSupported()) {
					_mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
				}
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new AnyKeyCondition(), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				System.out.println("Key character pressed: "
						+ inputState.getCurrent().getKeyboardState().getKeyEvent().getKeyChar());
			}
		}));

		// add Orbit handler - set it up to control the main camera
		control = new OrbitCamControl(_canvas.getCanvasRenderer().getCamera(), new Mesh());
		control.setupMouseTriggers(_logicalLayer, true);
		control.setInvertedX(true);
		control.setInvertedY(true);
		control.setSphereCoords(15, 0, 0);
	}

	private void setupAnimations(final ColladaStorage storage) {
		// Check if there is any animationdata in the file
		if (storage.getJointChannels().isEmpty() || storage.getSkins().isEmpty()) {
			logger.warning("No animations found!");
			return;
		}

		List<SkinData> skinDatas = storage.getSkins();

		// Make our manager
		manager = new AnimationManager(_timer, skinDatas.get(0).getPose());

		try {
			String labFileName = "flexiquad.lab";
			XWavesSegmentation labFile = new XWavesSegmentation(labFileName);
			_animations = labFile.getSegments();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Segment segment : _animations) {
			final AnimationClip clip = new AnimationClip(segment.getLabel());

			for (final JointChannel channel : storage.getJointChannels()) {
				JointChannel subChannel = (JointChannel) channel.getSubchannelByTime(segment.getStart(), segment.getEnd());
				// add it to a clip
				clip.addChannel(subChannel);
			}

			// Set some clip instance specific data - repeat, time scaling
			manager.getClipInstance(clip).setLoopCount(Integer.MAX_VALUE);

			// Add our "applier logic".
			manager.setApplier(new SimpleAnimationApplier());

			// Add our clip as a state in the default animation layer
			final SteadyState animState = new SteadyState(segment.getLabel());
			animState.setSourceTree(new ClipSource(clip, manager));
			manager.getBaseAnimationLayer().addSteadyState(animState);
		}

		// Set the current animation state on default layer
		manager.getBaseAnimationLayer().setCurrentState(_animations.get(0).getLabel(), true);
	}

	public void run() {
		try {
			_frameHandler.init();

			while (!_exit) {
				_frameHandler.updateFrame();
				Thread.yield();
			}
			// grab the graphics context so cleanup will work out.
			final CanvasRenderer cr = _canvas.getCanvasRenderer();
			cr.makeCurrentContext();
			quit(cr.getRenderer());
			cr.releaseCurrentContext();
			if (QUIT_VM_ON_EXIT) {
				System.exit(0);
			}
		} catch (final Throwable t) {
			System.err.println("Throwable caught in MainThread - exiting");
			t.printStackTrace(System.err);
		}
	}

	public void exit() {
		_exit = true;
	}

	@MainThread
	public void init() {
		final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
		logger.info("Display Vendor: " + caps.getDisplayVendor());
		logger.info("Display Renderer: " + caps.getDisplayRenderer());
		logger.info("Display Version: " + caps.getDisplayVersion());
		logger.info("Shading Language Version: " + caps.getShadingLanguageVersion());

		registerInputTriggers();

		AWTImageLoader.registerLoader();

		/**
		 * Create a ZBuffer to display pixels closest to the camera above farther ones.
		 */
		final ZBufferState buf = new ZBufferState();
		buf.setEnabled(true);
		buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		_root.setRenderState(buf);

		// ---- LIGHTS
		/** Set up a basic, default light. */
		final PointLight light = new PointLight();
		light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
		light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
		light.setLocation(new Vector3(100, 100, 100));
		light.setEnabled(true);

		/** Attach the light to a lightState and the lightState to rootNode. */
		_lightState = new LightState();
		_lightState.setEnabled(true);
		_lightState.attach(light);
		_root.setRenderState(_lightState);

		_wireframeState = new WireframeState();
		_wireframeState.setEnabled(false);
		_root.setRenderState(_wireframeState);

		_root.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);

		initExample();

		_root.updateGeometricState(0);
	}

	@MainThread
	public void update(final ReadOnlyTimer timer) {
		if (_canvas.isClosing()) {
			exit();
		}

		/** update stats, if enabled. */
		if (Constants.stats) {
			StatCollector.update();
		}

		updateLogicalLayer(timer);

		// Execute updateQueue item
		GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.UPDATE).execute();

		/** Call simpleUpdate in any derived classes of ExampleBase. */
		updateExample(timer);

		/** Update controllers/render states/transforms/bounds for rootNode. */
		_root.updateGeometricState(timer.getTimePerFrame(), true);
	}

	protected void updateLogicalLayer(final ReadOnlyTimer timer) {
		// check and execute any input triggers, if we are concerned with input
		if (_logicalLayer != null) {
			_logicalLayer.checkTriggers(timer.getTimePerFrame());
		}
	}

	protected void updateExample(final ReadOnlyTimer timer) {
		// update orbiter
		control.update(timer.getTimePerFrame());

		if (manager != null) {
			manager.update();
		}
	}

	@MainThread
	public boolean renderUnto(final Renderer renderer) {
		// Execute renderQueue item
		GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.RENDER)
				.execute(renderer);

		// Clean up card garbage such as textures, vbos, etc.
		ContextGarbageCollector.doRuntimeCleanup(renderer);

		/** Draw the rootNode and all its children. */
		if (!_canvas.isClosing()) {
			/** Call renderExample in any derived classes. */
			renderDemo(renderer);
			renderDebug(renderer);

			if (_doShot) {
				// force any waiting scene elements to be rendered.
				renderer.renderBuckets();
				ScreenExporter.exportCurrentScreen(_canvas.getCanvasRenderer().getRenderer(), _screenShotExp);
				_doShot = false;
			}
			return true;
		} else {
			return false;
		}
	}

	protected void renderDemo(final Renderer renderer) {
		_root.onDraw(renderer);
	}

	protected void renderDebug(final Renderer renderer) {
		if (_showNormals) {
			Debugger.drawNormals(_root, renderer);
			Debugger.drawTangents(_root, renderer);
		}
		if (_showSkeleton) {
			SkeletalDebugger.drawSkeletons(_root, renderer, false, true);
		}
	}

	public PickResults doPick(final Ray3 pickRay) {
		return null;
	}

	protected void quit(final Renderer renderer) {
		ContextGarbageCollector.doFinalCleanup(renderer);
		_canvas.close();
	}

	public static void start(final Class<? extends TongueDemo> demoClazz) {
		// TODO make this configurable
		final DisplaySettings settings = new DisplaySettings(800, 600, 24, -1, 0, 8, 0, 0, false, false);

		TongueDemo demo;
		try {
			demo = demoClazz.newInstance();
			demo._settings = settings;
		} catch (final Exception ex) {
			ex.printStackTrace();
			return;
		}

		// get our framework
		final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(demo);
		demo._canvas = new LwjglCanvas(canvasRenderer, settings);
		demo._physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(), (LwjglCanvas) demo._canvas);
		demo._mouseManager = new LwjglMouseManager();
		TextureRendererFactory.INSTANCE.setProvider(new LwjglTextureRendererProvider());

		demo._logicalLayer.registerInput(demo._canvas, demo._physicalLayer);

		// Register our example as an updater.
		demo._frameHandler.addUpdater(demo);

		// register our native canvas
		demo._frameHandler.addCanvas(demo._canvas);

		new Thread(demo).start();
	}
}
