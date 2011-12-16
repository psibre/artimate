package fr.loria.parole.artimate.io;

import java.util.ArrayList;
import java.util.HashMap;
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

public class Animation extends AnimationManager {

	private static final Logger logger = Logger.getLogger(Animation.class.getName());

	private HashMap<String, Segment> _segments = new HashMap<String, Segment>();
	private ArrayList<String> _animationLabels = new ArrayList<String>();
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
			loadSegments("all.lab");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String label : _animationLabels) {
			final AnimationClip clip = new AnimationClip(label);

			Segment segment = _segments.get(label);
			for (final JointChannel channel : storage.getJointChannels()) {
				JointChannel subChannel = (JointChannel) channel.getSubchannelByTime(segment.getStart(), segment.getEnd());
				// add it to a clip
				clip.addChannel(subChannel);
			}

			// Set some clip instance specific data - repeat, time scaling
			manager.getClipInstance(clip).setLoopCount(Integer.MAX_VALUE);

			// Add our "applier logic".
			manager.setApplier(new SimpleAnimationApplier());

			// Add our clip as a state in the default animation layer
			final SteadyState animState = new SteadyState(label);
			animState.setSourceTree(new ClipSource(clip, manager));
			manager.getBaseAnimationLayer().addSteadyState(animState);
		}

		// Set the current animation state on default layer
		manager.getBaseAnimationLayer().setCurrentState(_animationLabels.get(0), true);
	}

	private void loadSegments(String labFileName) throws Exception {
		XWavesSegmentation labFile = new XWavesSegmentation(labFileName);
		ArrayList<Segment> segments = labFile.getSegments();
		for (int s = 0; s < segments.size(); s++) {
			Segment segment = segments.get(s);
			String key = String.format("%d_%s", s, segment.getLabel());
			if (_segments.containsKey(key)) {
				logger.warning(String.format("Animation labeled %s already exists, will overwrite!", key));
			} else {
				_animationLabels.add(key);
			}
			_segments.put(key, segment);
		}
	}

	public void cycleAnimation() {
		_animationIndex++;
		if (_animationIndex >= _animationLabels.size()) {
			_animationIndex = 0;
		}
		logger.info("Switched to animation " + _animationLabels.get(_animationIndex));
		getBaseAnimationLayer().setCurrentState(_animationLabels.get(_animationIndex), true);
	}

}
