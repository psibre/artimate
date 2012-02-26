#!${path.to.blender} ${copied.model.file} --background --python

# imports
import os, sys

# import blender modules
try:
    import bpy
    from mathutils import Vector
except ImportError:
    sys.exit("This script must be run with blender, not python!")

# logging
import time
import logging
blenderloglevel = "${blender.log.level}"
loglevel = getattr(logging, blenderloglevel.upper(), None)
if not isinstance(loglevel, int):
    raise ValueError('Invalid log level: %s' % blenderloglevel)
logging.basicConfig(format='[blender] [%(levelname)s] %(message)s', level=loglevel)

# import custom modules
sys.path.append("${script.directory}")
import ema, lab

# constants
ORIGIN = (0, 0, 0)

def load_sweep(posfile, headerfile, labfile):
    # load or generate header
    if os.path.isfile(headerfile):
        logging.info("Loading %s" % headerfile)
        header = [line.strip() for line in open(headerfile)]
    else:
        logging.debug("%s not found; generating default header" % headerfile)
        header = ema.generate_header()
    
    # load segmentation
    logging.info("Loading %s" % labfile)
    
    # load EMA sweep
    logging.info("Loading %s" % posfile)
    sweep = ema.Sweep(posfile, header, labfile)
    return sweep

def process_sweep():
    before = sweep.size
    sweep.subsample()
    after = sweep.size
    logging.debug("downsampled from %d to %d frames" % (before, after))

def create_coils():
    # create dummy material
    material = bpy.data.materials.new(name="DUMMY")
    
    # create coil objects (reversed so that actions are in ascending order)
    for coilname in reversed(sweep.coils):
        # create armature
        armaturename = coilname + "Armature"
        armaturearm = bpy.data.armatures.new(name=armaturename)
        
        # create armature object and link to scene
        armature = bpy.data.objects.new(name=armaturename, object_data=armaturearm)
        bpy.context.scene.objects.link(armature)
        bpy.context.scene.objects.active = armature
        logging.debug("Created %s" % armaturename)
        
        # add bone
        bpy.ops.object.mode_set(mode='EDIT')
        armaturebone = armaturearm.edit_bones.new(name="Bone")
        armaturebone.tail.z = 0.5
        
        # prettify display in blender
        armaturearm.draw_type = 'ENVELOPE'
        armaturebone.envelope_distance = 0
        armaturebone.head_radius = 0.2
        armaturebone.tail_radius = 0.01
        
        bpy.ops.object.mode_set(mode='OBJECT')
        
        # create coil object at temporary location and name it
        depth = 1
        bpy.ops.mesh.primitive_cone_add(vertices=8, radius=depth / 4, depth=depth, location=(0, 0, depth / 2))
        coil = bpy.context.active_object
        coil.name = coilname + "Coil"
        
        # assign material
        bpy.ops.object.material_slot_add()
        coil.material_slots[0].material = material

        # parent coil to armature
        coil.parent = armature
        
        # DEBUG
        coil.hide = True
        #armature.show_name = True

def normalize():
    ref1 = bpy.data.objects["Ref1Armature"]
    ref2 = bpy.data.objects["Ref2Armature"]
    ref3 = bpy.data.objects["Ref3Armature"]
    logging.debug("Normalizing wrt to %s, %s, and %s" % (ref1.name, ref2.name, ref3.name))
    
    # create root node
    bpy.ops.object.add()
    root = bpy.context.scene.objects.active
    root.name = "Root"
    root.show_axis = True
    
    # track ref1 location
    constraint = root.constraints.new(type='COPY_LOCATION')
    constraint.target = ref1
    constraint.subtarget = "Bone"
    
    # point x axis to ref2
    constraint = root.constraints.new(type='TRACK_TO')
    constraint.target = ref2
    constraint.subtarget = "Bone"
    constraint.track_axis = 'TRACK_X'
    
    # rotate around x axis so that ref3 intersects xy plane
    constraint = root.constraints.new(type='LOCKED_TRACK')
    constraint.target = ref3
    constraint.subtarget = "Bone"
    constraint.track_axis = 'TRACK_Y'
    constraint.lock_axis = 'LOCK_X'

def animate_coils():
    # set frame range
    bpy.context.scene.frame_end = sweep.size
    logging.debug("Setting up animation for %d frames" % sweep.size)
    start = time.time()

    for coilname in sweep.coils:
            # get armature
            armaturename = coilname + "Armature"
            armature = bpy.data.objects[armaturename]
            
            # transform armature to channel position and rotation
            armature.location = sweep.getLoc(coilname, scale=0.1)
            armature.rotation_euler = sweep.getRot(coilname)
            
            # add animation to armature
            armature.animation_data_create()
            action = bpy.data.actions.new(coilname + "Action")
            armature.animation_data.action = action
            fcurves = armature.animation_data.action.fcurves
            
            fcurves.new(data_path="location", index=0)
            fcurves.new(data_path="location", index=1)
            fcurves.new(data_path="location", index=2)
            fcurves.new(data_path="rotation_euler", index=0)
            fcurves.new(data_path="rotation_euler", index=1)
            # TODO fix rotation value wrapping
            
            for fc, fcurve in enumerate(fcurves):
                fcurve.keyframe_points.add(sweep.size)
                
                for fn in range(sweep.size):
                    # there should be a better way to set interpolation...
                    fcurve.keyframe_points[fn].interpolation = 'LINEAR'
                    value = sweep.getValue(coilname, fc, fn)
                    if fc < 3:
                        # convert mm to cm for x, y, z
                        value /= 10
                    fcurve.keyframe_points[fn].co = fn, value
    
    finish = time.time()
    processingtime = finish - start
    logging.debug("Finished in %.3f s" % processingtime)

def clean_animation_data(rmse_threshold=15):
    logging.debug("Cleaning animation by interpolating through frames with RMSE higher than %.1f" % rmse_threshold)
    start = time.time()

    for coilname in sweep.coils:
        # get action
        actionname = coilname + "ArmatureAction"
        action = bpy.data.actions[actionname]
        
        for fc in range(3):
            # because we pop them from the stack of fcurves, the next one is always the first
            fcurve = action.fcurves[0] 
            # pythonically store fcurve data in dict for all keyframes where RMSE is beneath threshold
            kfpoints = {}
            for fn in range(sweep.size):
                rmse = sweep.getRMSE(coilname, fn)
                if rmse < rmse_threshold:
                    kfpoint = fcurve.keyframe_points[fn]
                    kfpoints[kfpoint.co[0]] = kfpoint.co[1]
            
            # replace fcurve data
            action.fcurves.remove(fcurve)
            fcurve = action.fcurves.new(data_path="location", index=fc)
            fcurve.keyframe_points.add(len(kfpoints))
            for kf, fn in enumerate(sorted(kfpoints)):
                fcurve.keyframe_points[kf].interpolation = 'LINEAR'
                value = kfpoints[fn]
                fcurve.keyframe_points[kf].co = (fn, value)
    
    finish = time.time()
    processingtime = finish - start
    logging.debug("Finished in %.3f s" % processingtime)

def create_ik_targets():
    BONESIZE = 1
    Z_LIMIT = 1
    
    # get target seeds and tongue
    seeds = [obj for obj in bpy.data.objects if obj.name.endswith("TargetSeed")]
    tongue = bpy.data.objects["Tongue"]
    
    # iterate over seeds, sorted by their object name, reversed
    for seed in reversed(sorted(seeds, key=lambda obj: obj.name)):
        # select only the seed
        bpy.ops.object.select_all(action='DESELECT')
        seed.select = True
    
        # move the seed to the tongue surface (by applying the Shrinkwrap constraint)
        constraint = seed.constraints.new(type='SHRINKWRAP')
        constraint.target = tongue
        bpy.ops.object.visual_transform_apply()
        seed.constraints.remove(constraint)
        
        # create IK targets (create armature, object, link to scene, activate, position)
        iktargetname = seed.name.replace("Seed", "")
        iktargetarmature = bpy.data.armatures.new(name=iktargetname)
        iktarget = bpy.data.objects.new(name=iktargetname, object_data=iktargetarmature)
        bpy.context.scene.objects.link(iktarget)
        bpy.context.scene.objects.active = iktarget
        iktarget.location = seed.location
        
        # create IK target action and copy EMA coil animation
        iktargetanimation = iktarget.animation_data_create()
        ikaction = bpy.data.actions.new(iktargetname + "Action")
        iktargetanimation.action = ikaction
        coiltargetname = iktargetname.replace("Target", "Armature")
        coiltarget = bpy.data.objects[coiltargetname]
        distance = coiltarget.location - iktarget.location
        # TODO: for now we disregard rotation!
        for i in range(3):
            fcurve = ikaction.fcurves.new(data_path="location", index=i)
            fcurve.keyframe_points.add(sweep.size)
            coilfcurve = coiltarget.animation_data.action.fcurves[i]
            # set delta values for each frame
            for fn in range(sweep.size):
                fcurve.keyframe_points[fn].interpolation = 'LINEAR'
                value = coilfcurve.keyframe_points[fn].co[1]
                value -= distance[i]
                fcurve.keyframe_points[fn].co = fn, value
        
        # add bone (in edit mode)
        bpy.ops.object.mode_set(mode='EDIT')
        editbone = iktargetarmature.edit_bones.new(name="Bone")
        editbone.head.z -= BONESIZE
        bpy.ops.object.mode_set(mode='OBJECT')
        
        # add z constraints
        zconstraint = iktarget.constraints.new(type='LIMIT_LOCATION')
        zconstraint.min_z = iktarget.location.z - Z_LIMIT
        zconstraint.use_min_z = True
        zconstraint.max_z = iktarget.location.z + Z_LIMIT
        zconstraint.use_max_z = True
        
        # DEBUG
        iktarget.show_x_ray = True
        
        logging.debug("Created IK target tracking %s" % coiltargetname)

def addbone(parentbonename, targetobjectname):
    '''
    utility function to add bone and set all parameters
    '''
    BBONE_SEGMENTS = 8
    
    logging.debug("Creating bone from %s to %s" % (parentbonename, targetobjectname))
    
    # get parent bone
    rig = bpy.data.objects["TongueArmature"]
    parentbone = rig.data.edit_bones[parentbonename]
    
    # get target
    target = bpy.data.objects[targetobjectname]
    targethead = target.pose.bones['Bone'].head
    targetlocation = target.location - rig.location + targethead
    
    # create bone
    bonename = targetobjectname + "Bone"
    editbone = rig.data.edit_bones.new(name=bonename)
    editbone.parent = parentbone
    if parentbonename == "RootBone":
        editbone.head = editbone.parent.head
        editbone.bbone_in = 0
    else:
        editbone.use_connect = True
    editbone.tail = targetlocation
    
    # set number of bezier bone segments
    editbone.bbone_segments = BBONE_SEGMENTS
    
    # setup constraints (must be in pose mode)
    bpy.ops.object.mode_set(mode='POSE')
    posebone = rig.pose.bones[bonename]
    
    # IK constraint
    constraint = posebone.constraints.new(type='IK')
    constraint.target = target
    constraint.subtarget = "Bone"
    
    # IK chain length goes back up to root
    constraint.chain_count = len(posebone.parent_recursive)
    # axis reference
    constraint.reference_axis = 'TARGET'
    # TODO consider adding pole targets
    # allow full stretching
    posebone.ik_stretch = 1
    
    # volume constraint
    posebone.constraints.new(type='MAINTAIN_VOLUME')
    
    bpy.ops.object.mode_set(mode='EDIT')

def create_rig():
    # create tongue armature
    trigname = "TongueArmature"
    trigarm = bpy.data.armatures.new(name=trigname)
    
    # create rig object, link to scene, activate, and position
    trig = bpy.data.objects.new(name=trigname, object_data=trigarm)
    bpy.context.scene.objects.link(trig)
    bpy.context.scene.objects.active = trig
    troot = bpy.data.objects["TRoot"]
    trig.location = troot.location
    
    # set type to bezier bone
    trig.data.draw_type = 'BBONE'
    
    # enter edit mode to assemble armature
    bpy.ops.object.mode_set(mode='EDIT')
    
    # root bone
    trootbone = trigarm.edit_bones.new(name="RootBone")
    trootbone.tail.z += 1

    # TODO temporarily hard-coded bone hierarchy
    # maybe replace with graphviz dot file (parsed with networkx?)
    
    # bone hierarchy
    #digraph  TongueArmature {
    #    Root -> TBackC -> TMidC -> TTipC;
    #    TBackC -> TMidL -> TBladeL;
    #    TBackC -> TMidR -> TBladeR;
    #}
    
    addbone("RootBone", "TBackCTarget")
    addbone("TBackCTargetBone", "TMidCTarget")
    addbone("TMidCTargetBone", "TTipCTarget")
    
    addbone("TBackCTargetBone", "TMidLTarget")
    addbone("TMidLTargetBone", "TBladeLTarget")
    
    addbone("TBackCTargetBone", "TMidRTarget")
    addbone("TMidRTargetBone", "TBladeRTarget")
    
    bpy.ops.object.mode_set(mode='OBJECT')
    
    # more IK config
    trig.pose.ik_solver = 'ITASC'
    # ITASC params
    # see also
    # http://wiki.blender.org/index.php/Dev:Source/GameEngine/RobotIKSolver
    # http://www.blender.org/documentation/blender_python_api_2_60_0/bpy.types.Itasc.html
    trig.pose.ik_param.mode = 'SIMULATION'
    
    # select only the rig and tongue, with the rig active
    tongue = bpy.data.objects["Tongue"]
    bpy.ops.object.select_all(action='DESELECT')
    tongue.select = True
    trig.select = True
    bpy.context.scene.objects.active = trig
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')
    trig.select = False
    
    bpy.context.scene.objects.active = tongue
    # remove root vertex group
    bpy.context.object.vertex_groups.remove(tongue.vertex_groups["RootBone"])
    
    # DEBUG
    trig.show_x_ray = True
    
    # jaw rig
        # create tongue armature
    jrigname = "JawArmature"
    jrigarm = bpy.data.armatures.new(name=jrigname)
    jrig = bpy.data.objects.new(name=jrigname, object_data=jrigarm)
    bpy.context.scene.objects.link(jrig)
    bpy.context.scene.objects.active = jrig
    jrig.location = troot.location
    
    # enter edit mode to assemble armature
    bpy.ops.object.mode_set(mode='EDIT')
    
    # root bone
    jrootbone = jrigarm.edit_bones.new(name="RootBone")
    jrootbone.tail.z += 1
    
    bpy.ops.object.mode_set(mode='OBJECT')

    # add constraint to track EMA coil on lower incisor
    constraint = jrig.constraints.new(type='TRACK_TO')
    # TODO: this has to be configurable!
    constraint.target = bpy.data.objects["JawArmature"]
    constraint.subtarget = "Bone"
    
    # parent mandible to jaw armature
    jaw = bpy.data.objects["Mandible"]
    jaw.select = True
    bpy.ops.object.parent_set()
    
    # parent tongue armature to jaw armature
    jaw.select = False
    trig.select = True
    bpy.ops.object.parent_set()
    
    logging.debug("Rigged model to armature")

def save_model(blendfile):
    logging.info("Saving %s" % blendfile)
    bpy.ops.wm.save_as_mainfile(filepath=blendfile)

def generate_testsweeps():
    # setup test variables
    testdir = "${test.resources.directory}"
    if not os.path.exists(testdir):
        os.makedirs(testdir)
    coilposfile = "%s/coils.pos" % testdir
    iktargetposfile = "%s/ik_targets.pos" % testdir
    tongueposfile = "%s/tongue.pos" % testdir
    
    # initialize test sweeps
    coilsweep = ema.Sweep()
    iktargetsweep = ema.Sweep()
    tonguesweep = ema.Sweep()
    
    # tongue armature
    rig = bpy.data.objects["TongueArmature"]
    rigbones = rig.pose.bones["RootBone"].children_recursive

    # iterate over all animation frames in timeline
    for frame in range(bpy.context.scene.frame_start, bpy.context.scene.frame_end + 1):
        bpy.context.scene.frame_set(frame)
        
        # iterate over EMA channels
        for channel in sweep.coils:
            # process tongue armature bones
            #rigbonename = channel
            #for bone in rigbones:
            #    x, y, z = bone.tail
            #    print(frame, x, y, z)
            
            # actual EMA coil
            coilname = channel + "Armature"
            coil = bpy.data.objects[coilname]
            x, y, z = coil.location * 10 # convert back to mm
            phi, theta, psi = coil.rotation_euler
            
            coilsweep.appendFrame(channel, x, y, z, phi, theta)

            # get IK target armature
            targetname = channel + "Target"
            try:
                target = bpy.data.objects[targetname]
                x, y, z = target.location * 10 # convert back to mm
                phi, theta, psi = target.rotation_euler
                iktargetsweep.appendFrame(channel, x, y, z, phi, theta)
            except KeyError:
                iktargetsweep.appendFrame(channel)
            
            # tongue armature
            bone = None
            for rigbone in rigbones:
                if rigbone.name == targetname + "Bone":
                    bone = rigbone
                    break
            if bone != None:
                x, y, z = bone.tail * 10 # convert back to mm
                tonguesweep.appendFrame(channel, x, y, z)
            else:
                tonguesweep.appendFrame(channel)
                
    # upsample back to 200 Hz and save test sweeps
    coilsweep.upsample()
    coilsweep.save(coilposfile)
    logging.info("Saved EMA coil positions to %s" % coilposfile)
    iktargetsweep.upsample()
    iktargetsweep.save(iktargetposfile)
    logging.info("Saved IK target positions to %s" % iktargetposfile)
    tonguesweep.upsample()
    tonguesweep.save(tongueposfile)
    logging.info("Saved tongue armature bone positions to %s" % tongueposfile)

if __name__ == '__main__':
    sweep = load_sweep("${generated.pos.file}", "${copied.header.file}", "${generated.lab.file}")
    process_sweep()
    create_coils()
    normalize()
    animate_coils()
    #clean_animation_data()
    create_ik_targets()
    create_rig()
    save_model("${generated.blend.file}")
    #generate_testsweeps()
