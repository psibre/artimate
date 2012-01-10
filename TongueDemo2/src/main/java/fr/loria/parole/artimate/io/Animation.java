package fr.loria.parole.artimate.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import fr.loria.parole.artimate.data.UnitDB;
import fr.loria.parole.artimate.data.UnitSequence;
import fr.loria.parole.artimate.data.io.XWavesSegmentation;

public class Animation extends AnimationManager {

	private static final Logger logger = Logger.getLogger(Animation.class.getName());

	private AnimationLayer _animation = new AnimationLayer("animation");

	private UnitDB unitDB;

	public Animation(ReadOnlyTimer globalTimer) {
		super(globalTimer);
		addAnimationLayer(_animation);
		// Add our "applier logic".
		setApplier(new SimpleAnimationApplier());
	}

	public void setupAnimations(ColladaStorage storage) {
		// Check if there is any animationdata in the file
		if (storage.getJointChannels().isEmpty() || storage.getSkins().isEmpty()) {
			logger.warning("No animations found!");
			return;
		}

		List<SkinData> skinDatas = storage.getSkins();

		addPose(skinDatas.get(0).getPose());

		XWavesSegmentation segmentation = null;
		try {
			segmentation = new XWavesSegmentation("all.lab");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Unit segment : segmentation.getUnits()) {
			final AnimationClip clip = new AnimationClip(segment.getLabel());

			for (final JointChannel channel : storage.getJointChannels()) {
				float start = (float) segment.getStart();
				float end = (float) segment.getEnd();
				JointChannel subChannel = (JointChannel) channel.getSubchannelByTime(start, end);
				// add it to a clip
				clip.addChannel(subChannel);
			}

			// Add the state directly to the unit in the DB
			final SteadyState animState = new SteadyState(segment.getLabel());
			animState.setSourceTree(new ClipSource(clip, this));
			segment.setAnimation(animState);
		}
		unitDB = new UnitDB(segmentation);
	}

	public void synthesize(UnitSequence unitSequence) {
		// create sequence of states
		ArrayList<SteadyState> stateSequence = new ArrayList<SteadyState>();

		for (int u = 0; u < unitSequence.size(); u++) {
			Unit unit = unitSequence.get(u);
			String animationID = unit.toString();
			// get animation state from base layer
			SteadyState baseState = (SteadyState) unitDB.getUnitList(unit.getLabel()).get(0).getAnimation();

			// get clip source and clip from base layer
			ClipSource baseClipSource = (ClipSource) baseState.getSourceTree();
			AnimationClip baseClip = baseClipSource.getClip();

			// create new clip and clip source
			AnimationClip clip = new AnimationClip(animationID);
			for (AbstractAnimationChannel channel : baseClip.getChannels()) {
				clip.addChannel(channel);
			}
			ClipSource clipSource = new ClipSource(clip, this);

			// get clip instance for clip, which allows us to...
			AnimationClipInstance clipInstance = getClipInstance(clip);
			// ...set the time scale
			double requestedDuration = unitSequence.get(u).getDuration();
			float baseDuration = baseClip.getMaxTimeIndex();
			double timeScale = requestedDuration / baseDuration;
			clipInstance.setTimeScale(timeScale);

			// create new state using this clip source
			SteadyState state = new SteadyState(unit.toString());
			state.setSourceTree(clipSource);

			// add state to sequence
			stateSequence.add(state);
			logger.info(String.format(Locale.US, "Added unit [%s] to animation timeline\n" + "\tsource duration:\t%f\n"
					+ "\ttarget duration:\t%f\n" + "\tscaling factor: \t%f", animationID, baseDuration, requestedDuration,
					timeScale));

			// add end transition so that state jumps to next in sequence at end (except for last)
			if (u < unitSequence.size() - 1) {
				String nextAnimationID = unitSequence.get(u + 1).toString();
				state.setEndTransition(new ImmediateTransitionState(nextAnimationID));
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

	private void clearAnimationLayer(AnimationLayer layer) {
		_animation.clearCurrentState();
		for (String stateName : _animation.getSteadyStateNames()) {
			SteadyState state = _animation.getSteadyState(stateName);
			_animation.removeSteadyState(state);
		}
	}

}
