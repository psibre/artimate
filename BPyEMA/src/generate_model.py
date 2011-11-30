import ema
try:
    import bpy
except ImportError:
    pass

DEBUG = True
ORIGIN = (0, 0, 0)
BBONE_SEGMENTS = 8

#temporarily clean out scene
if DEBUG:
    if bpy.context.mode != 'OBJECT':
        bpy.ops.object.mode_set(mode='OBJECT')
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete()
    bpy.context.scene.frame_current = 1

# hardcoded args for now:
posfile = "../../testUtt_test/pos/0012.pos"

# load sweep from pos file
if DEBUG:
    print("loading", posfile)
sweep = ema.Sweep(posfile, ema.generate_header())
channels = sweep.coils

# set end frame to number of samples in first data channel
channel0name = next(iter(sweep.data.keys()))
numframes = len(sweep.data[channel0name])
bpy.context.scene.frame_end = numframes
if DEBUG:
    print("set end frame to", bpy.context.scene.frame_end)

# create ema root node and link it to scene
emaroot = bpy.data.objects.new(name="EMARoot", object_data=None)
bpy.context.scene.objects.link(emaroot)

for channel in channels:
    # create armature
    armaturename = channel + "Armature"
    armaturearm = bpy.data.armatures.new(name=armaturename)
    
    # create armature object and link to scene
    armature = bpy.data.objects.new(name=armaturename, object_data=armaturearm)
    bpy.context.scene.objects.link(armature)
    bpy.context.scene.objects.active = armature
    
    # add bone
    bpy.ops.object.mode_set(mode='EDIT')
    armaturebone = armaturearm.edit_bones.new(name="Bone")
    armaturebone.tail.z = 1
    bpy.ops.object.mode_set(mode='OBJECT')
    
    # parent armature to ema root
    armature.parent = emaroot
    
    # create coil object and name it
    depth = 10
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius=2.5, depth=depth, location=(0, 0, depth / 2))
    coil = bpy.context.active_object
    coil.name = channel + "Coil"
    
    # parent coil to armature
    coil.parent = armature
    
    # transform armature to channel position and rotation
    bpy.data.objects[armaturename].location = sweep.getLoc(channel)
    bpy.data.objects[armaturename].rotation_euler = sweep.getRot(channel)
    
    # add animation to armature
    armature.animation_data_create()
    armature.animation_data.action = bpy.data.actions.new(armaturename + "Action")
    fcurves = armature.animation_data.action.fcurves
    
    fcurves.new(data_path="location", index=0)
    fcurves.new(data_path="location", index=1)
    fcurves.new(data_path="location", index=2)
    fcurves.new(data_path="rotation_euler", index=0)
    fcurves.new(data_path="rotation_euler", index=1)
    
    for fc, fcurve in enumerate(fcurves):
        fcurve.keyframe_points.add(numframes)
        
        for fn in range(numframes):
            # there should be a better way to set interpolation...
            fcurve.keyframe_points[fn].interpolation = 'LINEAR'
            value = sweep.getValue(channel, fc, fn)
            fcurve.keyframe_points[fn].co = fn, value

# scale down ema
emaroot.scale /= 10

# arbitrary origin for tongue and ik targets
tongueloc = (-5, -3, 1)

# generate ik targets tracking armatures with offset
for channel in channels:
    # create target armature
    targetname = channel + "Target"
    targetarm = bpy.data.armatures.new(name=targetname)
    
    # create target object and link to scene
    target = bpy.data.objects.new(name=targetname, object_data=targetarm)
    bpy.context.scene.objects.link(target)
    bpy.context.scene.objects.active = target
    
    # add bone
    bpy.ops.object.mode_set(mode='EDIT')
    editbone = targetarm.edit_bones.new(name="Bone")
    editbone.head = editbone.tail = tongueloc
    editbone.tail.z += 1
    
    # add constraints
    bpy.ops.object.mode_set(mode='POSE')
    posebone = target.pose.bones[0]
    coiltarget = bpy.data.objects[channel + "Armature"]
    
    locconstraint = posebone.constraints.new(type='COPY_LOCATION')
    locconstraint.target = coiltarget
    locconstraint.subtarget = "Bone"
    locconstraint.use_offset = True
    
    rotconstraint = posebone.constraints.new(type='COPY_ROTATION')
    rotconstraint.target = coiltarget
    rotconstraint.subtarget = "Bone"
    
    bpy.ops.object.mode_set(mode='OBJECT')

# add tongue rig

# create armature
rigname = "TongueArmature"
rigarm = bpy.data.armatures.new(name=rigname)

# create rig object and link to scene
rig = bpy.data.objects.new(name=rigname, object_data=rigarm)
bpy.context.scene.objects.link(rig)
bpy.context.scene.objects.active = rig

# set type to bezier bone
rig.data.draw_type = 'BBONE'

# enter edit mode to assemble armature
bpy.ops.object.mode_set(mode='EDIT')

# root bone (very short)
rootbone = rigarm.edit_bones.new(name="Root")
rootbone.head = rootbone.tail = tongueloc
rootbone.tail.z -= 3
rootbone.head.z = rootbone.tail.z + 0.1

# utility function to add bone and set all parameters
def addbone(parentbonename, targetobjectname):
    # add bone
    bonename = targetobjectname
    rig.data.edit_bones.new(name=bonename)
    editbone = rig.data.edit_bones[bonename]
    
    # parent to root bone
    editbone.parent = rig.data.edit_bones[parentbonename]
    
    # set number of bezier bone segments
    editbone.bbone_segments = BBONE_SEGMENTS
    
    # set head to parent tail
    editbone.head = editbone.parent.tail
    if parentbonename != "Root":
        editbone.use_connect = True
    
    # set tail to target
    target = bpy.data.objects[targetobjectname]
    editbone.tail = target.pose.bones[0].head
    
    # setup constraints (must be in pose mode)
    bpy.ops.object.mode_set(mode='POSE')
    posebone = rig.pose.bones[bonename]
    
    # ik constraint
    constraint = posebone.constraints.new(type='IK')
    constraint.target = target
    constraint.subtarget = "Bone"
    # ik chain length goes back up to root
    constraint.chain_count = len(posebone.parent_recursive)
    # allow full stretching (legacy ik solver)
    posebone.ik_stretch = 1
    
    # volume constraint
    posebone.constraints.new(type='MAINTAIN_VOLUME')
    
    # cleanup
    bpy.ops.object.mode_set(mode='EDIT')
    return

# TODO temporarily hard-coded bone hierarchy
# maybe replace with graphviz dot file (parsed with networkx?)

# bone hierarchy
#digraph TongueArmature {
#    Root -> Channel05 -> Channel03;
#    Root -> Channel08 -> Channel06 -> Channel01;
#    Root -> Channel10 -> Channel11;
#}

addbone("Root", "Channel05Target")
addbone("Channel05Target", "Channel03Target")

addbone("Root", "Channel08Target")
addbone("Channel08Target", "Channel06Target")
addbone("Channel06Target", "Channel01Target")

addbone("Root", "Channel10Target")
addbone("Channel10Target", "Channel11Target")

# finished with tongue armature
bpy.ops.object.mode_set(mode='OBJECT')
