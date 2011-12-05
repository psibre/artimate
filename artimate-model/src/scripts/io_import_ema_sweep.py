# <pep8-80 compliant>
# adapted from /Applications/blender.app/Contents/MacOS/2.58/scripts/addons/io_import_images_as_planes.py

bl_info = {
    "name": "AG500 EMA pos file importer (.pos)",
    "author": "Ingmar Steiner",
    "version": (0, 0, 1),
    "blender": (2, 6, 0),
    "location": "File > Import > Import AG500 EMA sweep (.pos file)",
    "description": "Import AG500 EMA sweep from .pos file",
    "warning": "",
    "wiki_url" : "",
    "category": "Import-Export"}

import bpy, os, re
import bpy_extras
import lab, ema

# namespace laziness
context = bpy.context
data = bpy.data
io_utils = bpy_extras.io_utils
ops = bpy.ops
props = bpy.props
types = bpy.types
utils = bpy.utils
    
##### CLASSES #####

##### FUNCTIONS #####

def generate_coil_objects(sweep):
    # add empty EMA node as parent of all coils:
    ops.object.add()
    context.active_object.name = "EMA"

    # make coil objects:
    for coil in sweep.coils:
        # remember, the size parameters are scaled down:
        depth = 10.0
        ops.mesh.primitive_cone_add(vertices=8,
                                        radius=2.5,
                                        depth=depth,
                                        location=(0, 0, depth / 2))
        context.active_object.name = coil + "Coil"
        context.active_object.show_name = True

        # move object origin so that it lies at center of cone base:
        context.scene.cursor_location = 0, 0, 0
        ops.object.origin_set(type='ORIGIN_CURSOR')

        # THEN locate and rotate:
        context.active_object.location = sweep.getLoc(coil)
        context.active_object.rotation_euler = sweep.getRot(coil)

        # make child of EMA node:
        ops.object.select_name(name="EMA", extend=True)
        ops.object.parent_set()

def generate_animation(self, sweep):
    if sweep.segmentation.segments == None:
        generate_action(sweep, self.start_frame, self.end_frame)
    else:
        for s, segment in enumerate(sweep.segmentation.segments):
            if segment.label != "sil":
                generate_action(sweep, segment.startframe, segment.endframe, "Action_%d_%s" % (s+1, segment.label))

def generate_action(sweep, start_frame=1, end_frame= -1, actionname = "Action"):
    end_frame = sweep.size if end_frame == -1 else end_frame
    assert end_frame - start_frame <= sweep.size

    for coil_name in sweep.coils:
        coil = data.objects[coil_name + "Coil"]

        if coil.animation_data is None:
            print("creating animation_data for coil", coil_name)
            coil.animation_data_create()
        track = coil.animation_data.nla_tracks.new()
        track.name = coil_name + actionname
        action = data.actions.new(coil_name + actionname)
        fcurves = action.fcurves

        fcurves.new(data_path="location", index=0)
        fcurves.new(data_path="location", index=1)
        fcurves.new(data_path="location", index=2)
        fcurves.new(data_path="rotation_euler", index=0)
        fcurves.new(data_path="rotation_euler", index=1)

        for f, fcurve in enumerate(fcurves):
            fcurve.keyframe_points.add(end_frame - start_frame)

            for frame_number in range(end_frame - start_frame):
                # there should be a better way to set interpolation...
                fcurve.keyframe_points[frame_number].interpolation = 'LINEAR'
                value = sweep.getValue(coil_name, f,
                                       start_frame - 1 + frame_number)
                fcurve.keyframe_points[frame_number].co = frame_number, value

        current_context = context.area.type
        context.area.type = 'NLA_EDITOR'
        ops.nla.actionclip_add(action=action.name)
        context.area.type = current_context

def decimate(arr):
    # not yet implemented
    return arr

##### MAIN #####

def import_sweep(self, context):
    directory, pos_file_name = os.path.split(self.filepath)
    message = ""

    header = generate_header(self)
    try:
        header_file = open("%s/%s" % (directory, self.header_file_name))
        header = header_file.readline().strip().split()
    except IOError:
        message += "No header file found, generating default header\n"
    
    if self.lab_file_name != "<auto>":
        try:
            label_file = open(self.lab_file_name)
        except IOError:
            message += "Label file could not be read, ignoring\n"
            label_file = None
    else:
        lab_file_name = "%s.lab" % os.path.splitext(pos_file_name)[0]
        try:
            label_file = open("%s/%s.lab" % (directory, lab_file_name))
        except IOError:
            try:
                label_file = open("%s/%s" % (re.sub(r'pos$', "wav", directory), lab_file_name))                
            except IOError:
                message += "No label file found, generating empty segmentation\n"
                label_file = None
    segmentation = lab.Segmentation(label_file)

    sweep = ema.Sweep(self.filepath, header, segmentation)
    generate_coil_objects(sweep)
    generate_animation(self, sweep)
    # hack: downscale
    data.objects["EMA"].scale = (0.1, 0.1, 0.1)

    # TODO: use API to set FPS to 25, Framerate Base to 0.125
    # for 200 Hz (adjust dependng on self.sampling_freq)
    # also set End of Frame Range to sweep.size
    # also set Time Remapping appropriately?
    # also figure out how to use bpy.ops.graph.smooth() in this script context

    message += "Done"
    self.report(type={'INFO'}, message=message)

##### OPERATOR #####

class IMPORT_OT_ema_sweep(types.Operator, io_utils.ImportHelper):
    ''''''
    bl_idname = "import.ema_sweep"
    bl_label = "Import EMA Sweep from .pos file"
    bl_description = "Import AG500 EMA sweep from .pos file"
    bl_options = {'REGISTER', 'UNDO'}

    ## OPTIONS ##
    sampling_freq = props.IntProperty(name="Sampling frequency",
                                description="Number of frames per second",
                                min=1,
                                default=200)
    start_frame = props.IntProperty(name="Start frame",
                              description="Number of first frame to import",
                              default=1)
    end_frame = props.IntProperty(name="End frame",
                              description="Number of last frame to import (-1 = last)",
                              default= -1)
    header_file_name = props.StringProperty(name="Header file name",
                                      description="List of EMA channel names",
                                      default="headers.txt")
    lab_file_name = props.StringProperty(name="Label file",
                                   description="Named like the .pos file",
                                   default="<auto>")

    ## DRAW ##
    def draw(self, context):
        layout = self.layout
        box = layout.box()
        box.label('Import Options:', icon='FILTER')
        box.prop(self, 'sampling_freq')
        box.prop(self, 'start_frame')
        box.prop(self, 'end_frame')
        box.prop(self, 'header_file_name')
        box.prop(self, 'lab_file_name')

    ## EXECUTE ##
    def execute(self, context):
        #the add utils don't work in this case
        #because many objects are added
        #disable relevant things beforehand
        editmode = context.user_preferences.edit.use_enter_edit_mode
        context.user_preferences.edit.use_enter_edit_mode = False
        if context.active_object and context.active_object.mode == 'EDIT':
            ops.object.mode_set(mode='OBJECT')

        import_sweep(self, context)

        context.user_preferences.edit.use_enter_edit_mode = editmode
        return {'FINISHED'}

##### REGISTER #####

def import_ema_sweep_button(self, context):
    self.layout.operator(IMPORT_OT_ema_sweep.bl_idname, text="AG500 EMA Sweep", icon='PLUGIN')

def register():
    utils.register_module(__name__)
    types.INFO_MT_file_import.append(import_ema_sweep_button)

def unregister():
    utils.unregister_module(__name__)
    types.INFO_MT_file_import.remove(import_ema_sweep_button)

if __name__ == '__main__':
    register()
