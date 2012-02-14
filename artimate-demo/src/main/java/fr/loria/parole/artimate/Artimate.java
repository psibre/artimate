package fr.loria.parole.artimate;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AbstractAnimationChannel;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClipInstance;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.ardor3d.extension.animation.skeletal.state.ImmediateTransitionState;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.synthesis.Synthesizer;

public class Artimate extends Synthesizer {

	private static final Logger logger = Logger.getLogger(Artimate.class.getName());

	private AnimationManager manager;

	public Artimate(UnitDB unitDB, AnimationManager manager) {
		super(unitDB);
		this.manager = manager;
		// Add our "applier logic".
		manager.setApplier(new SimpleAnimationApplier());
	}

	public void playSequence(UnitSequence targets) {
		// create sequence of states
		ArrayList<SteadyState> stateSequence = new ArrayList<SteadyState>();

		ListIterator<Unit> units = targets.iterator();
		while (units.hasNext()) {
			Unit unit = units.next();

			SteadyState state = copyAnimation(unit);
			// add state to sequence
			stateSequence.add(state);

			// add end transition so that state jumps to next in sequence at end (except for last)
			if (units.hasNext()) {
				Unit nextUnit = targets.get(units.nextIndex());
				String nextAnimationID = Integer.toString(nextUnit.getIndex());
				state.setEndTransition(new ImmediateTransitionState(nextAnimationID));
			}
		}

		// reset animation layer and add states
		AnimationLayer layer = resetAnimationLayer();
		for (SteadyState state : stateSequence) {
			layer.addSteadyState(state);
		}

		// play animation layer by setting current to first state
		layer.setCurrentState(stateSequence.get(0), true);
	}

	private SteadyState copyAnimation(Unit unit) {
		// get animation state from base layer
		List<Unit> baseUnits = db.getUnitList(unit.getLabel());
		if (baseUnits.isEmpty()) {
			// TODO hacky fallback to silence unit (which for now is assumed to exist)
			baseUnits = db.getUnitList("");
		}
		SteadyState baseState = (SteadyState) baseUnits.get(0).getAnimation();

		// get clip source and clip from base layer
		ClipSource oldClipSource = (ClipSource) baseState.getSourceTree();
		AnimationClip oldClip = oldClipSource.getClip();

		// create new clip and clip source
		AnimationClip newClip = new AnimationClip(Integer.toString(unit.getIndex()));
		for (AbstractAnimationChannel channel : oldClip.getChannels()) {
			newClip.addChannel(channel);
		}
		ClipSource newClipSource = new ClipSource(newClip, manager);

		// get clip instance for clip, which allows us to...
		AnimationClipInstance newClipInstance = manager.getClipInstance(newClip);
		// ...set the time scale
		double requestedDuration = unit.getDuration();
		float baseDuration = oldClip.getMaxTimeIndex();
		double timeScale = baseDuration / requestedDuration;
		newClipInstance.setTimeScale(timeScale);

		// create new state using this clip source
		SteadyState state = new SteadyState(Integer.toString(unit.getIndex()));
		state.setSourceTree(newClipSource);

		return state;
	}

	private AnimationLayer resetAnimationLayer() {
		String layerName = "-ANIMATION_LAYER-";
		// remove layer if it exists
		AnimationLayer layer = manager.findAnimationLayer(layerName);
		manager.removeAnimationLayer(layer);
		// create new layer
		layer = new AnimationLayer(layerName);
		manager.addAnimationLayer(layer);
		return layer;
	}

}
