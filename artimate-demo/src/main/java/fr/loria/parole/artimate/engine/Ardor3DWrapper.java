package fr.loria.parole.artimate.engine;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
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
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.lwjgl.LwjglTextureRendererProvider;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;

import fr.loria.parole.artimate.data.io.XWavesSegmentation;
import fr.loria.parole.artimate.demo.DemoApp;

public class Ardor3DWrapper {

	public final LogicalLayer _logicalLayer = new LogicalLayer();
	public volatile boolean _exit = false;
	private MouseManager _mouseManager = new LwjglMouseManager();
	public boolean _showNormals = false;
	public WireframeState _wireframeState;
	public boolean _showSkeleton = false;
	public boolean _doShot = false;
	public final Node _root = new Node();
	public LwjglCanvas _canvas;
	public final Timer _timer = new Timer();
	public final FrameHandler _frameHandler = new FrameHandler(_timer);
	private PhysicalLayer _physicalLayer;
	public LightState _lightState;
	public ScreenShotImageExporter _screenShotExp = new ScreenShotImageExporter();
	/** Our orbiter _control. */
	public OrbitCamControl _control;

	// animation
	public AnimationManager animation = new AnimationManager(_timer);

	public Ardor3DWrapper(Scene scene) {
		// TODO make this configurable
		DisplaySettings settings = new DisplaySettings(800, 600, 24, -1, 0, 8, 0, 0, false, false);
		final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(scene);
		_canvas = new LwjglCanvas(canvasRenderer, settings);

		_physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(), _canvas);
		_logicalLayer.registerInput(_canvas, _physicalLayer);

		TextureRendererFactory.INSTANCE.setProvider(new LwjglTextureRendererProvider());

		// register our native canvas
		_frameHandler.addCanvas(_canvas);
	}

	public void registerInputTriggers(final DemoApp demoApp) {
		// check if this example worries about input at all
		if (_logicalLayer == null) {
			return;
		}

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				_exit = true;
			}
		}));

		_logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.A), new TriggerAction() {
			public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
				try {
					XWavesSegmentation testsegmentation = new XWavesSegmentation("test.lab");
					demoApp.synthesizer.playSequence(testsegmentation);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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

		// add Orbit handler - set it up to _control the main camera
		_control = new OrbitCamControl(_canvas.getCanvasRenderer().getCamera(), new Mesh());
		_control.setupMouseTriggers(_logicalLayer, true);
		// adjust world up to match that of model (like blender)
		_control.setInvertedY(true);
		_control.setWorldUpVec(new Vector3(0, 0, 1));
		_control.setSphereCoords(15, 0, 0);
	}

	public void update(final ReadOnlyTimer timer) {
		// update orbiter
		_control.update(timer.getTimePerFrame());

		if (animation != null) {
			animation.update();
		}
	}

	public void quit() {
		// grab the graphics context so cleanup will work out.
		final CanvasRenderer cr = _canvas.getCanvasRenderer();
		cr.makeCurrentContext();
		ContextGarbageCollector.doFinalCleanup(cr.getRenderer());
		_canvas.close();
		cr.releaseCurrentContext();
	}

}
