#!/usr/bin/env blender --background --python

# workaround for working directory issues and module search path
# when running this script with blender through mvn exec:exec
import os, sys
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

# BEGIN CLI option parsing
# (somewhat adapted from $BLENDER/$VERSION/scripts/templates/background_job.py)
import argparse  # to parse options for us and print a nice help message

# get the args passed to blender after "--", all of which are ignored by
# blender so scripts may receive their own arguments
argv = sys.argv

if "--" not in argv:
    argv = []  # as if no args are passed
else:
    argv = argv[argv.index("--") + 1:]  # get all args after "--"

# When --help or no args are given, print this help
usage_text = "Run blender in background mode with this script:\n\
blender --background --python %s -- [options]" % __file__

parser = argparse.ArgumentParser(description=usage_text,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument("-e", "--ema", dest="posfile",
                    help="EMA sweep to load as animation data (AG500 .pos format)")
parser.add_argument("-l", "--lab", dest="labfile",
                    help="label file to load for animation timeline (XWaves .lab format)")
parser.add_argument("--header", dest="header",
                    help="header file from which to load EMA channel names (header.txt)")
parser.add_argument("-m", "--mesh", dest="meshfile",
                    help="This file will be imported as the tongue mesh (Stanford .ply format)")
parser.add_argument("-c", "--collada", dest="daefile",
                    help="Output COLLADA model file (.dae)")
# this is no longer needed when EMA samples are decimated upon load
#parser.add_argument("-s", "--smooth", dest="smooth", action="store_true",
#                    help="Smooth EMA fcurves (not working in batch mode currently)")
parser.add_argument("-b", "--batch", dest="batch", action="store_true",
                    help="batch mode (run non-interactively)")

args = parser.parse_args(argv)  # In this example we wont use the args

if not argv:
    parser.print_help()
    sys.exit()

# END CLI option parsing

DEBUG = True
SCALE = 0.1
OFFSET = 1
BBONE_SEGMENTS = 8
BATCH = args.batch

import ema, lab
try:
    import bpy
    from mathutils import Vector
except ImportError:
    sys.exit()

ORIGIN = Vector((0, 0, 0))

def cleanup():
    if bpy.context.mode != 'OBJECT':
        bpy.ops.object.mode_set(mode='OBJECT')
    for object in bpy.data.objects:
        if object.type in ['MESH', 'ARMATURE', 'EMPTY']:
            try:
                bpy.context.scene.objects.unlink(object)
            except RuntimeError:
                pass
            bpy.data.objects.remove(object)
    for armature in bpy.data.armatures:
        bpy.data.armatures.remove(armature)
    for mesh in bpy.data.meshes:
        bpy.data.meshes.remove(mesh)
    for action in bpy.data.actions:
        bpy.data.actions.remove(action)
    bpy.context.scene.frame_current = 1

#temporarily clean out scene
if DEBUG:
    cleanup()

# load sweep from pos file (with lab file, if available)
if DEBUG:
    print("loading", args.posfile)
if not args.header:
    header = ema.generate_header()
else:
    header = args.header

segmentation = None
if args.labfile:
    with open(args.labfile) as labfile:
        segmentation = lab.Segmentation(labfile)
sweep = ema.Sweep(args.posfile, header, segmentation)

# downsample EMA data
if DEBUG:
    print("downsampling EMA data")
sweep.subsample()

channels = sweep.coils

# set end frame to number of samples in first data channel
channel0name = next(iter(sweep.data.keys()))
numframes = len(sweep.data[channel0name])
bpy.context.scene.frame_end = numframes
if DEBUG:
    print("set end frame to", bpy.context.scene.frame_end)

# set framerate to 25 Hz (PAL)
bpy.context.scene.render.fps = 25

# create ema root node and link it to scene
emaroot = bpy.data.objects.new(name="EMARoot", object_data=None)
bpy.context.scene.objects.link(emaroot)

# create dummy material
material = bpy.data.materials.new(name="MyMaterial")

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
    
    # assign material
    bpy.ops.object.material_slot_add()
    coil.material_slots[0].material = material
    
    # select none
    bpy.ops.object.select_all(action='DESELECT')
    
    # parent coil to armature
    coil.parent = armature
    
    # DEBUG
    coil.hide = True
    armature.show_name = True
    
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
    
    # TODO fix rotation value wrapping
    
    for fc, fcurve in enumerate(fcurves):
        fcurve.keyframe_points.add(numframes)
        
        for fn in range(numframes):
            # there should be a better way to set interpolation...
            fcurve.keyframe_points[fn].interpolation = 'LINEAR'
            value = sweep.getValue(channel, fc, fn)
            fcurve.keyframe_points[fn].co = fn, value

# add markers from segmentation
if sweep.segmentation:
    for s, segment in enumerate(sweep.segmentation.segments):
        # add marker and position it
        bpy.context.scene.timeline_markers.new(name="%d_%s" % (s, segment.label))
        bpy.context.scene.timeline_markers[s].frame = segment.start * bpy.context.scene.render.fps

# arbitrary origin for tongue and ik targets
a = bpy.data.objects["Channel06Armature"].location
b = bpy.data.objects["Channel08Armature"].location
tongueloc = ((2 * b.x - a.x) * SCALE,
             (2 * b.y - a.y) * SCALE,
             (2 * b.z - a.z) * SCALE)

# scale down ema root
emaroot.scale *= SCALE

# smooth function curves
# TODO figure out how to do this when running script non-interactively (if BATCH == True)
#if args.smooth and not BATCH:
#    oldcontexttype = bpy.context.area.type
#    bpy.context.area.type = 'GRAPH_EDITOR'
#    # select armatures
#    for channel in channels:
#        bpy.data.objects[channel + "Armature"].select = True
#    bpy.ops.graph.smooth()
#    # deselect armatures
#    for channel in channels:
#        bpy.data.objects[channel + "Armature"].select = False
#    bpy.context.area.type = oldcontexttype

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
    editbone.head = editbone.tail = ORIGIN
    editbone.head.z -= OFFSET
    
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
rootbone.head.x -= OFFSET
rootbone.tail.x -= OFFSET
rootbone.head.z -= 2 * OFFSET
rootbone.tail.z = rootbone.head.z + 0.1

# utility function to add bone and set all parameters
def addbone(parentbonename, targetobjectname):
    # add bone
    bonename = targetobjectname
    rig.data.edit_bones.new(name=bonename)
    editbone = rig.data.edit_bones[bonename]
    
    # parent bone
    editbone.parent = rig.data.edit_bones[parentbonename]
    
    # set number of bezier bone segments
    editbone.bbone_segments = BBONE_SEGMENTS
    
    # connect to parent
    if parentbonename == "Root":
        editbone.head = editbone.parent.head
    else:
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
    # axis reference
    constraint.reference_axis = 'TARGET'
    # TODO consider adding pole targets 
    # allow full stretching
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

# more ik config
rig.pose.ik_solver = 'ITASC'
# itasc params
# see also
# http://wiki.blender.org/index.php/Dev:Source/GameEngine/RobotIKSolver
# http://www.blender.org/documentation/blender_python_api_2_60_0/bpy.types.Itasc.html
rig.pose.ik_param.mode = 'SIMULATION'

# import tongue mesh
print("Loading mesh from file", args.meshfile)
bpy.ops.import_mesh.ply(filepath=args.meshfile)
tongue = bpy.context.active_object

# create and assign tongue material
bpy.ops.object.material_slot_add()
pink = bpy.data.materials.new(name="pink")
# a kind of pink
pink.diffuse_color = (0.8, 0.075, 0.6)
pink.emit = 0.1
tongue.material_slots[0].material = pink

# transform tongue
tongue.scale *= SCALE
tongue.location = tongueloc

# HACK
tongue.location = (2.9370, 2.2104, -1.7978)

# remove duplicate vertices
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.remove_doubles(limit=0.0001)
bpy.ops.object.mode_set(mode='OBJECT')

# parent to tongue rig with automatic vertex groups and weights
bpy.ops.object.select_name(name=rigname, extend=True)
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
bpy.ops.object.select_name(name=tongue.name)
# remove root vertex group
bpy.context.object.vertex_groups.remove(tongue.vertex_groups["Root"])

if args.daefile:
    if DEBUG:
        # save .blend file before baking actions for later inspection and debuggin
        bpy.ops.wm.save_as_mainfile(filepath=args.daefile.replace("dae", "blend"))
        print("exporting to COLLADA file", args.daefile)
    # bake animation
    bpy.ops.object.select_name(name=rigname)
    bpy.ops.nla.bake(frame_end=bpy.context.scene.frame_end, only_selected=False)
    
    # export collada
    bpy.ops.wm.collada_export(filepath=args.daefile)

print("DONE")

if BATCH:
    cleanup()
    bpy.ops.wm.quit_blender()
    # this spews much garbage to STDERR, so consider redirecting that to /dev/null
    # see http://projects.blender.org/tracker/?func=detail&group_id=9&aid=23215&atid=264
