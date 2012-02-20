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
            
            fcurves.new(data_path="delta_location", index=0)
            fcurves.new(data_path="delta_location", index=1)
            fcurves.new(data_path="delta_location", index=2)
            fcurves.new(data_path="delta_rotation_euler", index=0)
            fcurves.new(data_path="delta_rotation_euler", index=1)
            fcurves.new(data_path="delta_rotation_euler", index=2)
            # TODO fix rotation value wrapping
            
            for fc, fcurve in enumerate(fcurves):
                fcurve.keyframe_points.add(sweep.size)
                
                for fn in range(1, sweep.size):
                    # there should be a better way to set interpolation...
                    fcurve.keyframe_points[fn].interpolation = 'LINEAR'
                    value = sweep.getValue(coilname, fc, fn)
                    prevvalue = sweep.getValue(coilname, fc, fn - 1)
                    delta = value - prevvalue
                    if fc < 3:
                        # convert mm to cm for x, y, z
                        delta /= 10
                    fcurve.keyframe_points[fn].co = fn, delta
    
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
        
        # assign EMA coil animation to IK target
        coiltargetname = iktargetname.replace("Target", "Armature")
        coiltarget = bpy.data.objects[coiltargetname]
        iktargetanimation = iktarget.animation_data_create()
        iktargetanimation.action = coiltarget.animation_data.action
        
        # add bone (in edit mode)
        bpy.ops.object.mode_set(mode='EDIT')
        editbone = iktargetarmature.edit_bones.new(name="Bone")
        editbone.head.z -= BONESIZE
        bpy.ops.object.mode_set(mode='OBJECT')
        
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
    # create armature
    rigname = "TongueArmature"
    rigarm = bpy.data.armatures.new(name=rigname)
    
    # create rig object, link to scene, activate, and position
    rig = bpy.data.objects.new(name=rigname, object_data=rigarm)
    bpy.context.scene.objects.link(rig)
    bpy.context.scene.objects.active = rig
    root = bpy.data.objects["Root"]
    rig.location = root.location
    
    # set type to bezier bone
    rig.data.draw_type = 'BBONE'
    
    # enter edit mode to assemble armature
    bpy.ops.object.mode_set(mode='EDIT')
    
    # root bone
    rootbone = rigarm.edit_bones.new(name="RootBone")
    rootbone.tail.z += 1

    # TODO temporarily hard-coded bone hierarchy
    # maybe replace with graphviz dot file (parsed with networkx?)
    
    # bone hierarchy
    #digraph TongueArmature {
    #    Root -> Channel08 -> Channel06 -> Channel01;
    #    Channel08 -> Channel05 -> Channel03;
    #    Channel08 -> Channel10 -> Channel11;
    #}
    
    addbone("RootBone", "Channel08Target")
    addbone("Channel08TargetBone", "Channel06Target")
    addbone("Channel06TargetBone", "Channel01Target")
    
    addbone("Channel08TargetBone", "Channel05Target")
    addbone("Channel05TargetBone", "Channel03Target")
    
    addbone("Channel08TargetBone", "Channel10Target")
    addbone("Channel10TargetBone", "Channel11Target")
    
    bpy.ops.object.mode_set(mode='OBJECT')
    
    # more IK config
    rig.pose.ik_solver = 'ITASC'
    # ITASC params
    # see also
    # http://wiki.blender.org/index.php/Dev:Source/GameEngine/RobotIKSolver
    # http://www.blender.org/documentation/blender_python_api_2_60_0/bpy.types.Itasc.html
    rig.pose.ik_param.mode = 'SIMULATION'
    
    # select only the rig and tongue, with the rig active
    tongue = bpy.data.objects["Tongue"]
    bpy.ops.object.select_all(action='DESELECT')
    tongue.select = True
    rig.select = True
    bpy.context.scene.objects.active = rig
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')
    rig.select = False
    
    bpy.context.scene.objects.active = tongue
    # remove root vertex group
    bpy.context.object.vertex_groups.remove(tongue.vertex_groups["RootBone"])
    
    logging.debug("Rigged model to armature")

def save_model(blendfile):
    logging.info("Saving %s" % blendfile)
    bpy.ops.wm.save_as_mainfile(filepath=blendfile)

if __name__ == '__main__':
    sweep = load_sweep("${generated.pos.file}", "${copied.header.file}", "${generated.lab.file}")
    process_sweep()
    create_coils()
    animate_coils()
    #clean_animation_data()
    create_ik_targets()
    create_rig()
    save_model("${generated.blend.file}")
