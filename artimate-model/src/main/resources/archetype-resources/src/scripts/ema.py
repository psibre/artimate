import math
from array import array

class Sweep:
    def __init__(self, pos_file_name=None, header=None, segmentation=None):
        if header == None:
            header = generate_header()
        self.header = header
        if pos_file_name != None:
            self.data = self.load(pos_file_name)
            try:
                print("loaded %d segments" % len(segmentation.segments))
            except AttributeError:
                pass
        else:
            self.data = {}
            for channel in self.header:
                self.data[channel] = array('f')
        self.segmentation = segmentation

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
        return data
    
    def save(self, pos_file):
        arr = array('f', self.values())
        try:
            # append to provided fileobj
            arr.tofile(pos_file)
        except AttributeError:
            # or interpret as file name
            pos_file = open(pos_file, 'wb')
            arr.tofile(pos_file)
            pos_file.close()
    
    def extend(self, other):
        for channel in self.data:
            self.data[channel].extend(other.data[channel])

    def subsample(self, step=8):
        '''reduce the number of samples'''
        for channel in self.data.keys():
            newdata = self.data[channel][::step]
            self.data[channel] = newdata
    
    def upsample(self, rep=8):
        '''repeat each sample rep times'''
        for channel in self.data.keys():
            newdata = array('f')
            for sample in self.data[channel]:
                newdata.extend([sample] * rep)
            self.data[channel] = newdata
    
    def size(self):
        try:
            channel = next(iter(self.data.keys()))
            return len(self.data[channel])
        except AttributeError:
            return 0
    size = property(size)

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
    
    def getRMSE(self, coil, frame=0):
        rmse = self.data[coil + "_RMS"][frame]
        return rmse

    def getValue(self, coil, index, frame=0):
        values = self.getLoc(coil, frame) + self.getRot(coil, frame)
        return values[index]
    
    def values(self):
        for frame in range(self.size):
            for dimension in self.header:
                value = self.data[dimension][frame]
                yield value
    
    def appendFrame(self, channel, x=0, y=0, z=0, phi=0, theta=0, rms=0, extra=0):
        self.data[channel + "_X"].append(x)
        self.data[channel + "_Y"].append(y)
        self.data[channel + "_Z"].append(z)
        self.data[channel + "_phi"].append(phi)
        self.data[channel + "_theta"].append(theta)
        self.data[channel + "_RMS"].append(rms)
        self.data[channel + "_Extra"].append(extra)

def generate_header(num_coils=12):
    # hard-coded for AG500
    dimensions = ["X", "Y", "Z", "phi", "theta", "RMS", "Extra"]
    channels = []
    for coil in range(num_coils):
        channels.extend(["Channel%02d_%s" % (coil + 1, dimension)
                         for dimension in dimensions])
    return channels
