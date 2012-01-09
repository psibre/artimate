package fr.loria.parole.artimate.io;

import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
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
		SteadyState baseState1 = getBaseAnimationLayer().getSteadyState("foo");
		ClipSource clipSource1 = (ClipSource) baseState1.getSourceTree();
		SteadyState baseState2 = getBaseAnimationLayer().getSteadyState("baz");
		ClipSource clipSource2 = (ClipSource) baseState2.getSourceTree();

		AnimationClip clip1 = clipSource1.getClip();
		AnimationClip clip2 = clipSource2.getClip();
		AnimationClipInstance clipInstance1 = getClipInstance(clip1);
		AnimationClipInstance clipInstance2 = getClipInstance(clip2);

		clipInstance1.setTimeScale(2);
		clipInstance2.setTimeScale(0.5);

		clipInstance1.setLoopCount(0);
		clipInstance2.setLoopCount(0);

		SteadyState state1 = new SteadyState("foo1");
		state1.setSourceTree(clipSource1);
		SteadyState state2 = new SteadyState("baz1");
		state2.setSourceTree(clipSource2);

		clearAnimationLayer(_animation);

		_animation.addSteadyState(state1);
		_animation.addSteadyState(state2);

		state1.setEndTransition(new ImmediateTransitionState("baz1"));

		_animation.setCurrentState(state1, true);

		return;
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
