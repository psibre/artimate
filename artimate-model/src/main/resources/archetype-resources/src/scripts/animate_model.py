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
import logging
blenderloglevel = "${blender.log.level}"
loglevel = getattr(logging, blenderloglevel.upper(), None)
if not isinstance(loglevel, int):
    raise ValueError('Invalid log level: %s' % blenderloglevel)
logging.basicConfig(format='[blender] [%(levelname)s] %(message)s', level=loglevel)

# import custom modules
sys.path.append("${script.directory}")
import ema, lab

# load EMA sweep
generatedposfile = "${generated.pos.file}"
logging.info("Loading %s" % generatedposfile)
sweep = ema.Sweep(generatedposfile)
