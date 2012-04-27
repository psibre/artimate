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
package fr.loria.parole.artimate.data.io;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

import fr.loria.parole.artimate.data.Unit;
import fr.loria.parole.artimate.data.UnitDB;

public class ColladaTextGridModel {

	private static final Logger logger = Logger.getLogger(ColladaTextGridModel.class.getName());
	private UnitDB unitDB;
	private AnimationManager manager;
	private Node scene;

	public ColladaTextGridModel(AnimationManager manager, String modelFileName)
			throws IOException {
		ColladaStorage storage = new ColladaImporter().load(modelFileName);
		scene = storage.getScene();

		this.manager = manager;
		setupAnimations(storage);
	}

	private void setupAnimations(ColladaStorage storage) {
		// Check if there is any animationdata in the file
		if (storage.getJointChannels().isEmpty() || storage.getSkins().isEmpty()) {
			logger.warning("No animations found!");
			return;
		}

		List<SkinData> skinDatas = storage.getSkins();

		manager.addPose(skinDatas.get(0).getPose());

		XWavesSegmentation segmentation = null;
		try {
			segmentation = new XWavesSegmentation("all.lab");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (Unit segment : segmentation) {
			final AnimationClip clip = new AnimationClip(segment.getLabel());

			for (final JointChannel channel : storage.getJointChannels()) {
				float start = (float) segment.getStart();
				float end = (float) segment.getEnd();
				JointChannel subChannel = (JointChannel) channel.getSubchannelByTime(start, end);
				// add it to a clip
				clip.addChannel(subChannel);
			}

			// Add the state directly to the unit in the DB
			final SteadyState animState = new SteadyState(Integer.toString(segment.getIndex()));
			animState.setSourceTree(new ClipSource(clip, manager));
			segment.setAnimation(animState);
		}
		unitDB = new UnitDB(segmentation);
	}

	public UnitDB getUnitDB() {
		return unitDB;
	}

	public Spatial getScene() {
		return scene;
	}

}
