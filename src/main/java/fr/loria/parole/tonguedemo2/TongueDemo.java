package fr.loria.parole.tonguedemo2;

import java.io.IOException;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
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

	public static void main(final String[] args) {
		start(TongueDemo.class);
	}

	@Override
	protected void updateExample(final ReadOnlyTimer timer) {
		// update orbiter
		control.update(timer.getTimePerFrame());
	}

	@Override
	protected void initExample() {
		_canvas.setTitle("OrbitCam TongueDemo");

		// Load the collada scene
		try {
			final ColladaStorage storage = new ColladaImporter().load("flexiquad.dae");
			Node cube = (Node) storage.getScene().getChild("Cube");
			Node geom = (Node) cube.getChild("geometry");
			SkinnedMesh mesh = (SkinnedMesh) geom.getChild("Cube_001-mesh");
			control.setLookAtSpatial(mesh);
			_root.attachChild(storage.getScene());
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
}
