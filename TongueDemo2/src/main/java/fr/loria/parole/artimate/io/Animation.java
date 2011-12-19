package fr.loria.parole.artimate.io;

import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.blendtree.SimpleAnimationApplier;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.util.ReadOnlyTimer;
import fr.loria.parole.artimate.segmentation.Segment;
import fr.loria.parole.artimate.segmentation.Segmentation;

public class Animation extends AnimationManager {

	private static final Logger logger = Logger.getLogger(Animation.class.getName());

	private Segmentation _segmentation;
	private int _animationIndex;

	public Animation(ReadOnlyTimer globalTimer) {
		super(globalTimer);
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
			Segment segment = _segmentation.get(s);
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
		manager.getBaseAnimationLayer().setCurrentState(_segmentation.get(0).getLabel(), true);
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
