package fr.loria.parole.artimate;

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
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

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

	public void synthesizeSequence(UnitSequence targets) {
		// get candidates from UnitDB
		UnitSequence candidates = synthesize(targets);

		// reset animation layer
		AnimationLayer layer = resetAnimationLayer();

		// create iterators
		assert targets.size() == candidates.size();
		PeekingIterator<Unit> targetIterator = Iterators.peekingIterator(targets.iterator());
		ListIterator<Unit> candidateIterator = candidates.iterator();

		// interleaved iteration over targets, candidates
		while (targetIterator.hasNext() && candidateIterator.hasNext()) {
			Unit target = targetIterator.next();
			Unit candidate = candidateIterator.next();

			copyAnimation(candidate, target);
			SteadyState state = (SteadyState) target.getAnimation();

			// add end transition so that state jumps to next in sequence at end (except for last)
			if (targetIterator.hasNext()) {
				Unit nextUnit = targetIterator.peek();
				String nextAnimationName = ((SteadyState) nextUnit.getAnimation()).getName();
				state.setEndTransition(new ImmediateTransitionState(nextAnimationName));
			}
			layer.addSteadyState(state);
		}

		// play animation layer by setting current to first state
		layer.setCurrentState((SteadyState) candidates.get(0).getAnimation(), true);
	}

	private void copyAnimation(Unit source, Unit target) {
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
		ClipSource targetClipSource = new ClipSource(targetClip, manager);

		// get clip instance for clip, which allows us to...
		AnimationClipInstance targetClipInstance = manager.getClipInstance(targetClip);
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
