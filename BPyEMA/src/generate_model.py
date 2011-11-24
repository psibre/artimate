import ema
try:
    import bpy
except ImportError:
    pass

DEBUG = True

#temporarily clean out scene
if DEBUG:
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete()

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

# scale down ema
bpy.data.objects[emarootname].scale /= 10

# generate ik target tracking armature with offset
for channel in channels:
    # HACK: set 3d cursor to inverse location (properly define later)
    targetloc = bpy.data.objects[armaturename].location.copy()
    targetloc.negate()
    bpy.context.scene.cursor_location = targetloc
    
    bpy.ops.object.armature_add()
    targetname = channel + "Target"
    bpy.context.active_object.name = targetname
    coiltarget = bpy.data.objects[channel + "Armature"]
    
    # add constraints
    bpy.ops.object.mode_set(mode='POSE')
    
    bpy.ops.pose.constraint_add(type='COPY_LOCATION')
    locconstraint = bpy.data.objects[targetname].pose.bones["Bone"].constraints["Copy Location"]
    locconstraint.target = coiltarget
    locconstraint.subtarget = "Bone"
    locconstraint.use_offset = True
    
    bpy.ops.pose.constraint_add(type='COPY_ROTATION')
    rotconstraint = bpy.data.objects[targetname].pose.bones["Bone"].constraints["Copy Rotation"]
    rotconstraint.target = coiltarget
    rotconstraint.subtarget = "Bone"
    
    bpy.ops.object.mode_set(mode='OBJECT')
