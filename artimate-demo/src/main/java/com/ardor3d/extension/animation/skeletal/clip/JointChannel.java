/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.google.common.collect.Lists;

/**
 * Transform animation channel, specifically geared towards describing the motion of skeleton joints.
 */
@SavableFactory(factoryMethod = "initSavable")
public class JointChannel extends TransformChannel {

    /** A name prepended to joint indices to identify them as joint channels. */
    public static final String JOINT_CHANNEL_NAME = "_jnt";

    /** The human readable version of the name. */
    private final String _jointName;

    /** The joint index. */
    private int _jointIndex;

    /**
     * Construct a new JointChannel.
     * 
     * @param joint
     *            the joint to pull name and index from.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, rotations, translations, scales);
        _jointName = joint.getName();
        _jointIndex = joint.getIndex();
    }

    /**
     * Construct a new JointChannel.
     * 
     * @param jointName
     *            the human readable name of the joint
     * @param jointIndex
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public JointChannel(final String jointName, final int jointIndex, final float[] times,
            final ReadOnlyQuaternion[] rotations, final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + jointIndex, times, rotations, translations, scales);
        _jointName = jointName;
        _jointIndex = jointIndex;
    }

    /**
     * Construct a new JointChannel.
     * 
     * @param joint
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param transforms
     *            the transform to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyTransform[] transforms) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, transforms);
        _jointName = joint.getName();
        _jointIndex = joint.getIndex();
    }

    /**
     * @return the human readable version of the associated joint's name.
     */
    public String getJointName() {
        return _jointName;
    }

    @Override
    public AbstractAnimationChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime > endTime");
        }
        final List<Float> times = Lists.newArrayList();
        final List<ReadOnlyQuaternion> rotations = Lists.newArrayList();
        final List<ReadOnlyVector3> translations = Lists.newArrayList();
        final List<ReadOnlyVector3> scales = Lists.newArrayList();

        final JointData jData = new JointData();

        // Add start sample
        updateSample(startTime, jData);
        times.add(0f);
        rotations.add(jData.getRotation());
        translations.add(jData.getTranslation());
        scales.add(jData.getScale());

        // Add mid samples
        for (int i = 0; i < getSampleCount(); i++) {
            final float time = _times[i];
            if (time > startTime && time < endTime) {
                times.add(time - startTime);
                rotations.add(_rotations[i]);
                translations.add(_translations[i]);
                scales.add(_scales[i]);
            }
        }

        // Add end sample
        updateSample(endTime, jData);
        times.add(endTime - startTime);
        rotations.add(jData.getRotation());
        translations.add(jData.getTranslation());
        scales.add(jData.getScale());

        final float[] timesArray = new float[times.size()];
        int i = 0;
        for (final float time : times) {
            timesArray[i++] = time;
        }
        // return
        return newChannel(name, timesArray, rotations.toArray(new ReadOnlyQuaternion[rotations.size()]),
                translations.toArray(new ReadOnlyVector3[translations.size()]),
                scales.toArray(new ReadOnlyVector3[scales.size()]));
    }

    @Override
    protected JointChannel newChannel(final String name, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        return new JointChannel(_jointName, _jointIndex, times, rotations, translations, scales);
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        super.setCurrentSample(sampleIndex, progressPercent, applyTo);

        final JointData jointData = (JointData) applyTo;
        jointData.setJointIndex(_jointIndex);
    }

    public JointData getJointData(final int index, final JointData store) {
        JointData rVal = store;
        if (rVal == null) {
            rVal = new JointData();
        }
        super.getTransformData(index, store);
        rVal.setJointIndex(_jointIndex);
        return rVal;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends JointChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_jointName, "jointName", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final String jointName = capsule.readString("jointName", null);
        try {
            final Field field1 = JointChannel.class.getDeclaredField("_jointName");
            field1.setAccessible(true);
            field1.set(this, jointName);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (_channelName.startsWith(JointChannel.JOINT_CHANNEL_NAME)) {
            _jointIndex = Integer.parseInt(_channelName.substring(JointChannel.JOINT_CHANNEL_NAME.length()));
        } else {
            _jointIndex = -1;
        }
    }

    @Override
    public JointData createStateDataObject(final AnimationClipInstance instance) {
        return new JointData();
    }

    public static JointChannel initSavable() {
        return new JointChannel();
    }

    protected JointChannel() {
        super();
        _jointName = null;
        _jointIndex = -1;
    }
}
