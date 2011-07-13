# adapted from /Applications/blender.app/Contents/MacOS/2.58/scripts/addons/io_import_images_as_planes.py

bl_info = {
	"name": "AG500 EMA pos file importer (.pos)",
	"author": "Ingmar Steiner",
	"version": (0,0,1),
	"blender": (2,5,8),
	"location": "File > Import > Import AG500 EMA sweep (.pos file)",
	"description": "Import AG500 EMA sweep from .pos file",
	"warning": "",
	"wiki_url" : "",
	"category": "Import-Export"}

import bpy, os, math
from array import array
from bpy.props import *
from add_utils import *
from bpy_extras.io_utils import ImportHelper

##### CLASSES #####

class Sweep:
	def __init__(self, pos_file_name, header):
		self.header = header
		self.data = self.load(pos_file_name)

	def load(self, pos_file_name):
		arr = array('f')
		with open(pos_file_name, 'rb') as pos_file:
			while True:
				try:
					arr.fromfile(pos_file, 4096)
				except EOFError:
					break
		data = {}
		for h, h_item in enumerate(self.header):
			data[h_item] = arr[h:len(arr):len(self.header)]
		self.size = len(arr) / len(self.header)
		return data

	def decimate(self):
		for channel in data.keys():
			decimated = decimate(data[channel])
			data[channel] = decimated

	def coils(self):
		coils = [channel.split('_')[0] \
				 for channel in self.header if channel.endswith('X')]
		return coils
	coils = property(coils)

	def getLoc(self, coil, frame = 0):
		x = self.data[coil + "_X"][frame]
		y = self.data[coil + "_Y"][frame]
		z = self.data[coil + "_Z"][frame]
		return x, y, z

	def getRot(self, coil, frame = 0):
		phi_deg = self.data[coil + "_phi"][frame]
		theta_deg = self.data[coil + "_theta"][frame]
		phi_rad = math.radians(phi_deg)
		theta_rad = math.radians(theta_deg)
		return phi_rad, theta_rad, 0

##### FUNCTIONS #####

def generate_paths(self):
	directory, file = os.path.split(self.filepath)
	if file:
		return directory, self.filepath
	else:
		return directory

def generate_header(self, num_coils = 12):
	# hard-coded for AG500
	dimensions = ['X', 'Y', 'Z', 'phi', 'theta', 'RMS', 'Extra']
	channels = []
	for coil in range(num_coils):
		channels.extend(["Channel%02d_%s" % (coil+1, dimension) \
						 for dimension in dimensions])
	return channels

def generate_coil_objects(sweep):
	for coil in sweep.coils:
		bpy.ops.object.add(location = sweep.getLoc(coil),\
						   rotation = sweep.getRot(coil))
		bpy.context.active_object.name = coil

def generate_keyframes(sweep):
	pass

def decimate(arr):
	# not yet implemented
	return arr

##### MAIN #####

def import_sweep(self, context):
	directory, pos_file_name = generate_paths(self)
	message = ""

	header = generate_header(self)
	try:
		headerfile = open("%s/%s" % (directory, self.header_file_name))
		header = headerfile.readline().strip().split()
	except IOError:
		message += "No header file found, generating default one\n"

	sweep = Sweep(pos_file_name, header)
	generate_coil_objects(sweep)
	generate_keyframes(sweep)

	message += "Done"
	self.report(type='INFO',
				message=message)

##### OPERATOR #####

class IMPORT_OT_image_to_plane(bpy.types.Operator, ImportHelper, AddObjectHelper):
	''''''
	bl_idname = "import.ema_sweep"
	bl_label = "Import EMA Sweep from .pos file"
	bl_description = "Import AG500 EMA sweep from .pos file"
	bl_options = {'REGISTER', 'UNDO'}

	## OPTIONS ##
	sampling_freq = IntProperty(name='Sampling frequency',
								description='Number of frames per second',
								min=1,
								default=200)
	header_file_name = StringProperty(name="Header file name",
									  description="List of EMA channel names",
									  default="headers.txt")

	## DRAW ##
	def draw(self, context):
		layout = self.layout
		box = layout.box()
		box.label('Import Options:', icon='FILTER')
		box.prop(self, 'sampling_freq')
		box.prop(self, 'header_file_name')

	## EXECUTE ##
	def execute(self, context):
		#the add utils don't work in this case
		#because many objects are added
		#disable relevant things beforehand
		editmode = context.user_preferences.edit.use_enter_edit_mode
		context.user_preferences.edit.use_enter_edit_mode = False
		if context.active_object\
		and context.active_object.mode == 'EDIT':
			bpy.ops.object.mode_set(mode='OBJECT')

		import_sweep(self, context)

		context.user_preferences.edit.use_enter_edit_mode = editmode
		return {'FINISHED'}

##### REGISTER #####

def import_ema_sweep_button(self, context):
	self.layout.operator(IMPORT_OT_image_to_plane.bl_idname, text="AG500 EMA Sweep", icon='PLUGIN')

def register():
	bpy.utils.register_module(__name__)
	bpy.types.INFO_MT_file_import.append(import_ema_sweep_button)

def unregister():
	bpy.utils.unregister_module(__name__)
	bpy.types.INFO_MT_file_import.remove(import_ema_sweep_button)

if __name__ == '__main__':
	register()
