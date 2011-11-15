package fr.loria.parole.tonguedemo2;

import java.io.IOException;
import java.util.List;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.input.control.FirstPersonControl;
import com.ardor3d.input.control.OrbitCamControl;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;

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
public class TongueDemo extends ExampleBase {

	/** Our orbiter control. */
	private OrbitCamControl control;
	private ColladaStorage storage;
	private AnimationManager manager;

	public static void main(final String[] args) {
		start(TongueDemo.class);
	}

	@Override
	protected void updateExample(final ReadOnlyTimer timer) {
		// update orbiter
		control.update(timer.getTimePerFrame());

		if (manager != null) {
			manager.update();
		}
	}

	@Override
	protected void initExample() {
		_canvas.setTitle("OrbitCam TongueDemo");

		// Load the collada scene
		try {
			storage = new ColladaImporter().load("flexiquad.dae");
			Node cube = (Node) storage.getScene().getChild("Cube");
			Node geom = (Node) cube.getChild("geometry");
			SkinnedMesh mesh = (SkinnedMesh) geom.getChild("Cube_001-mesh");
			control.setLookAtSpatial(mesh);
			_root.attachChild(storage.getScene());
			setupAnimations(storage);
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	protected void registerInputTriggers() {
		super.registerInputTriggers();

		// clean out the first person handler
		FirstPersonControl.removeTriggers(_logicalLayer, _controlHandle);

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
			return;
		}

		List<SkinData> skinDatas = storage.getSkins();

		// Make our manager
		manager = new AnimationManager(_timer, skinDatas.get(0).getPose());

		final AnimationClip clipA = new AnimationClip("clipA");
		for (final JointChannel channel : storage.getJointChannels()) {
			// add it to a clip
			clipA.addChannel(channel);
		}

		// Set some clip instance specific data - repeat, time scaling
		manager.getClipInstance(clipA).setLoopCount(Integer.MAX_VALUE);

		// Add our "applier logic".
		manager.setApplier(new SimpleAnimationApplier());

		// Add our clip as a state in the default animation layer
		final SteadyState animState = new SteadyState("anim_state");
		animState.setSourceTree(new ClipSource(clipA, manager));
		manager.getBaseAnimationLayer().addSteadyState(animState);

		// Set the current animation state on default layer
		manager.getBaseAnimationLayer().setCurrentState("anim_state", true);
	}
}
