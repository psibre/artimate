import math
from array import array

class Sweep:
    def __init__(self, pos_file_name, header, segmentation=None):
        self.header = header
        self.data = self.load(pos_file_name)
        self.segmentation = segmentation
        try:
            print("loaded %d segments" % len(segmentation.segments))
        except AttributeError:
            pass

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
        self.size = int(len(arr) / len(self.header))
        return data

    def decimate(self):
        '''not implemented'''
        for channel in self.data.keys():
            decimated = decimate(self.data[channel])
            self.data[channel] = decimated

    def coils(self):
        coils = [channel.split('_')[0]
                 for channel in self.header if channel.endswith('X')]
        return coils
    coils = property(coils)

    def getLoc(self, coil, frame=0):
        x = self.data[coil + "_X"][frame]
        y = self.data[coil + "_Y"][frame]
        z = self.data[coil + "_Z"][frame]
        return x, y, z

    def getRot(self, coil, frame=0):
        phi_deg = self.data[coil + "_phi"][frame]
        theta_deg = self.data[coil + "_theta"][frame]
        phi_rad = math.radians(phi_deg)
        theta_rad = math.radians(theta_deg)
        return phi_rad, theta_rad, 0

    def getValue(self, coil, index, frame=0):
        values = self.getLoc(coil, frame) + self.getRot(coil, frame)
        return values[index]

def generate_header(num_coils=12):
    # hard-coded for AG500
    dimensions = ["X", "Y", "Z", "phi", "theta", "RMS", "Extra"]
    channels = []
    for coil in range(num_coils):
        channels.extend(["Channel%02d_%s" % (coil + 1, dimension)
                         for dimension in dimensions])
    return channels
