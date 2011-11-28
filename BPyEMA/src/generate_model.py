import ema
try:
    import bpy
except ImportError:
    pass

DEBUG = True
ORIGIN = (-5, 0, 4)
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

# ensure 3d cursor is at origin
bpy.context.scene.cursor_location = (0, 0, 0)

# create ema root node
bpy.ops.object.add()
emarootname = "EMARoot"
bpy.context.active_object.name = emarootname

for channel in channels:
    # create armature and name it
    bpy.ops.object.armature_add()
    armaturename = channel + "Armature"
    bpy.context.active_object.name = armaturename
    
    # parent armature to ema root
    bpy.ops.object.select_name(name=emarootname, extend=True)
    bpy.ops.object.parent_set()

    # create coil object and name it
    depth = 10
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius=2.5, depth=depth, location=(0, 0, depth / 2))
    coilname = channel + "Coil"
    bpy.context.active_object.name = coilname
    
    # parent coil to armature
    bpy.ops.object.select_name(name=armaturename, extend=True)
    bpy.ops.object.parent_set()
    
    # transform armature to channel position and rotation
    bpy.data.objects[armaturename].location = sweep.getLoc(channel)
    bpy.data.objects[armaturename].rotation_euler = sweep.getRot(channel)

    # add animation to armature
    armature = bpy.data.objects[armaturename]
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
            # TODO change all this so that coils move on a path?

# scale down ema
bpy.data.objects[emarootname].scale /= 10

# generate ik target tracking armature with offset
for channel in channels:
    # HACK: set 3d cursor to some location (properly define later)
    targetloc = ORIGIN
    bpy.context.scene.cursor_location = targetloc
    
    bpy.ops.object.armature_add()
    targetname = channel + "Target"
    bpy.context.active_object.name = targetname
    coiltarget = bpy.data.objects[channel + "Armature"]
    
    # add constraints
    bpy.ops.object.mode_set(mode='POSE')
    
    bpy.ops.pose.constraint_add(type='COPY_LOCATION')
    locconstraint = bpy.data.objects[targetname].pose.bones["Bone"].constraints['Copy Location']
    locconstraint.target = coiltarget
    locconstraint.subtarget = "Bone"
    locconstraint.use_offset = True
    
    bpy.ops.pose.constraint_add(type='COPY_ROTATION')
    rotconstraint = bpy.data.objects[targetname].pose.bones["Bone"].constraints['Copy Rotation']
    rotconstraint.target = coiltarget
    rotconstraint.subtarget = "Bone"
    
    bpy.ops.object.mode_set(mode='OBJECT')

# add tongue rig
bpy.ops.object.armature_add()
bpy.context.active_object.name = "TongueArmature"

# set type to bezier bone
rig = bpy.context.active_object
rig.data.draw_type = 'BBONE'

# enter edit mode to assemble armature
bpy.ops.object.mode_set(mode='EDIT')

# root bone (very short)
rootbone = bpy.context.active_bone
rootbone.name = "Root"
rootbone.tail = (0, 0, 0.1)

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
    
    # ik
    posebone = rig.pose.bones[bonename]
    posebone.constraints.new(type='IK')
    # ik target
    posebone.constraints['IK'].target = target
    posebone.constraints['IK'].subtarget = "Bone"
    # ik chain length goes back up to root
    posebone.constraints['IK'].chain_count = len(posebone.parent_recursive)
    # allow full stretching (legacy ik solver)
    posebone.ik_stretch = 1
    
    # volume constraint
    posebone.constraints.new(type='MAINTAIN_VOLUME')
    
    bpy.ops.object.mode_set(mode='EDIT')

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
