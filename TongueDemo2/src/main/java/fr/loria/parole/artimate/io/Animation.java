package fr.loria.parole.artimate.io;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AbstractAnimationChannel;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClipInstance;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.ardor3d.extension.animation.skeletal.state.ImmediateTransitionState;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.util.ReadOnlyTimer;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.data.io.XWavesSegmentation;

public class Animation extends AnimationManager {

	private static final Logger logger = Logger.getLogger(Animation.class.getName());

	private UnitSequence _segmentation;
	private int _animationIndex;
	private AnimationLayer _animation = new AnimationLayer("animation");

	public Animation(ReadOnlyTimer globalTimer) {
		super(globalTimer);
		addAnimationLayer(_animation);
		// TODO Auto-generated constructor stub
	}

	public void setupAnimations(Animation manager, final ColladaStorage storage) {
		// Check if there is any animationdata in the file
		if (storage.getJointChannels().isEmpty() || storage.getSkins().isEmpty()) {
			logger.warning("No animations found!");
			return;
		}

		List<SkinData> skinDatas = storage.getSkins();

		manager.addPose(skinDatas.get(0).getPose());

		try {
			_segmentation = new XWavesSegmentation("all.lab");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int s = 0; s < _segmentation.size(); s++) {
			Unit segment = _segmentation.get(s);
			if (findClipInstance(segment.getLabel()) != null) {
				logger.warning(String.format("Animation labeled \"%s\" already exists, not overwriting!", segment.getLabel()));
				continue;
			}

			final AnimationClip clip = new AnimationClip(segment.getLabel());

			for (final JointChannel channel : storage.getJointChannels()) {
				float start = (float) _segmentation.getStart(s);
				float end = (float) _segmentation.getEnd(s);
				JointChannel subChannel = (JointChannel) channel.getSubchannelByTime(start, end);
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
		// manager.getBaseAnimationLayer().setCurrentState(_segmentation.get(0).getLabel(), false);
	}

	public void playAnimationSequence() {
		// hard-coded animation sequence (for now)
		String[] animationSequence = new String[] { "foo", "foo" };
		double[] timeScales = new double[] { 2.0, 0.2 };

		// create sequence of states
		ArrayList<SteadyState> stateSequence = new ArrayList<SteadyState>();

		for (int a = 0; a < animationSequence.length; a++) {
			// get animation state from base layer
			String animationName = animationSequence[a];
			SteadyState baseState = getBaseAnimationLayer().getSteadyState(animationName);

			// get clip source and clip from base layer
			ClipSource baseClipSource = (ClipSource) baseState.getSourceTree();
			AnimationClip baseClip = baseClipSource.getClip();

			// create new clip and clip source
			AnimationClip clip = new AnimationClip(animationName);
			for (AbstractAnimationChannel channel : baseClip.getChannels()) {
				clip.addChannel(channel);
			}
			ClipSource clipSource = new ClipSource(clip, this);

			// get clip instance for clip, which allows us to...
			AnimationClipInstance clipInstance = getClipInstance(clip);
			// ...set the time scale...
			clipInstance.setTimeScale(timeScales[a]);
			// ...and loop count
			clipInstance.setLoopCount(0);

			// create new state using this clip source
			SteadyState state = new SteadyState(generateUnitKey(animationName, a));
			state.setSourceTree(clipSource);

			// add state to sequence
			stateSequence.add(state);

			// add end transition so that state jumps to next in sequence at end (except for last)
			if (a < animationSequence.length - 1) {
				String nextAnimationName = generateUnitKey(animationSequence[a + 1], a + 1);
				state.setEndTransition(new ImmediateTransitionState(nextAnimationName));
			}
		}

		// clear animation layer and add states
		clearAnimationLayer(_animation);
		for (SteadyState state : stateSequence) {
			_animation.addSteadyState(state);
		}

		// play animation layer by setting current to first state
		_animation.setCurrentState(stateSequence.get(0), true);
	}

	private String generateUnitKey(String unitName, int index) {
		return String.format("%d_%s", index, unitName);
	}

	private void clearAnimationLayer(AnimationLayer layer) {
		_animation.clearCurrentState();
		for (String stateName : _animation.getSteadyStateNames()) {
			SteadyState state = _animation.getSteadyState(stateName);
			_animation.removeSteadyState(state);
		}
	}

	public void cycleAnimation() {
		_animationIndex++;
		if (_animationIndex >= _segmentation.size()) {
			_animationIndex = 0;
		}
		logger.info(String.format("Switched to animation \"%s\"", _segmentation.get(_animationIndex).getLabel()));
		getBaseAnimationLayer().setCurrentState(_segmentation.get(_animationIndex).getLabel(), true);
	}

}
