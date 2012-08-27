/*
 * #%L
 * Artimate Demo App
 * %%
 * Copyright (C) 2011 - 2012 INRIA
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fr.loria.parole.artimate.engine;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AbstractAnimationChannel;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClipInstance;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.ardor3d.extension.animation.skeletal.state.ImmediateTransitionState;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
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
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.data.io.XWavesSegmentation;
import fr.loria.parole.artimate.demo.DemoApp;

public class Ardor3DWrapper implements GameEngineWrapper {

	public final LogicalLayer _logicalLayer = new LogicalLayer();
	public volatile boolean _exit = false;
	public MouseManager _mouseManager = new LwjglMouseManager();
	public boolean _showNormals = false;
	public WireframeState _wireframeState;
	public boolean _showSkeleton = false;
	public boolean _doShot = false;
	public final Node _root = new Node();
	public LwjglCanvas _canvas;
	public final Timer _timer = new Timer();
	public final FrameHandler _frameHandler = new FrameHandler(_timer);
	public PhysicalLayer _physicalLayer;
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

		// Add our "applier logic".
		animation.setApplier(new SimpleAnimationApplier());
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
					demoApp.synthesizer.synthesizeAnimation(testsegmentation);
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

	/**
	 * Deep copies Animation from source Unit to Target unit.
	 * 
	 * @param source
	 * @param target
	 */
	@Override
	public void copyAnimation(Unit source, Unit target) {
		SteadyState sourceState = (SteadyState) source.getAnimation();

		// get clip source and clip from base layer
		ClipSource sourceClipSource = (ClipSource) sourceState.getSourceTree();
		AnimationClip sourceClip = sourceClipSource.getClip();

		// create new clip and clip source
		String targetName = Integer.toString(target.getIndex());
		AnimationClip targetClip = new AnimationClip(targetName);
		for (AbstractAnimationChannel channel : sourceClip.getChannels()) {
			targetClip.addChannel(channel);
		}
		ClipSource targetClipSource = new ClipSource(targetClip, animation);

		// get clip instance for clip, which allows us to...
		AnimationClipInstance targetClipInstance = animation.getClipInstance(targetClip);
		// ...set the time scale
		double targetDuration = target.getDuration();
		double sourceDuration = source.getDuration();
		double timeScale = sourceDuration / targetDuration;
		targetClipInstance.setTimeScale(timeScale);

		// create new state using this clip source
		SteadyState state = new SteadyState(targetName);
		state.setSourceTree(targetClipSource);

		// assign state to target
		target.setAnimation(state);
	}

	@Override
	public void playAnimation(UnitSequence units) {
		final String layerName = "-ANIMATION_LAYER-";
		// remove layer if it exists
		AnimationLayer layer = animation.findAnimationLayer(layerName);
		animation.removeAnimationLayer(layer);

		// create new layer
		layer = new AnimationLayer(layerName);
		animation.addAnimationLayer(layer);

		// iterate over units
		PeekingIterator<Unit> unitIterator = Iterators.peekingIterator(units.iterator());
		while (unitIterator.hasNext()) {
			// get animation state
			Unit unit = unitIterator.next();
			SteadyState state = (SteadyState) unit.getAnimation();

			// add end transition so that state jumps to next in sequence at end (except for last)
			if (unitIterator.hasNext()) {
				Unit nextUnit = unitIterator.peek();
				SteadyState nextState = (SteadyState) nextUnit.getAnimation();
				state.setEndTransition(new ImmediateTransitionState(nextState.getName()));
			}

			// add state to layer
			layer.addSteadyState(state);
		}

		layer.setCurrentState((SteadyState) units.get(0).getAnimation(), true);
	}

}
