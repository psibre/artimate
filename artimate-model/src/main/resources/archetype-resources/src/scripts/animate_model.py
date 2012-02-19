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

def process_sweep(sweep):
    before = sweep.size
    sweep.subsample()
    after = sweep.size
    logging.debug("downsampled from %d to %d frames" % (before, after))

def create_coils(sweep):
    # create dummy material
    material = bpy.data.materials.new(name="DUMMY")
    
    # create coil objects
    for coilname in sweep.coils:
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
        armaturebone.tail.z = 1
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
        armature.show_name = True

def animate_coils():
    # set frame range
    bpy.context.scene.frame_end = sweep.size
    logging.debug("Setting up animation for %d frames" % sweep.size)
    start = time.time()

    for coilname in sweep.coils[::-1]:
            # get armature
            armaturename = coilname + "Armature"
            armature = bpy.data.objects[armaturename]
            
            # transform armature to channel position and rotation
            armature.location = sweep.getLoc(coilname, scale=0.1)
            armature.rotation_euler = sweep.getRot(coilname)
            
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
                fcurve.keyframe_points.add(sweep.size)
                
                for fn in range(sweep.size):
                    # there should be a better way to set interpolation...
                    fcurve.keyframe_points[fn].interpolation = 'LINEAR'
                    value = sweep.getValue(coilname, fc, fn)
                    if fc < 3:
                        value /= 10
                    fcurve.keyframe_points[fn].co = fn, value
    finish = time.time()
    processingtime = finish - start
    logging.debug("Finished in %.3f s" % processingtime)

def save_model(blendfile):
    logging.info("Saving %s" % blendfile)
    bpy.ops.wm.save_as_mainfile(filepath=blendfile)

if __name__ == '__main__':
    sweep = load_sweep("${generated.pos.file}", "${copied.header.file}", "${generated.lab.file}")
    process_sweep(sweep)
    create_coils(sweep)
    animate_coils()
    save_model("${generated.blend.file}")
