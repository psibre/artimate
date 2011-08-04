#!/usr/bin/env python

from optparse import OptionParser
from numpy import fromfile, loadtxt

TRANS_COILS = 6
SAMP_RATE = 200.0

class Amps:
    def __init__(self, ampfile = None, channels = 12):
        self.channels = channels
        if ampfile is not None:
            self.load(ampfile)
    
    def load(self, ampfile):
        shape = (-1, self.channels * TRANS_COILS)
        amps = fromfile(ampfile, 'f').reshape(shape)
        self.data = amps
    
    def save(self, ampfile):
        self.data.tofile(ampfile, format = '%f')
    
    def slice(self, start = 0, stop = None, step = 1):
        if stop == None:
            stop = self.data.shape[0] + 1
        slice = Amps(channels = self.channels)
        slice.data = self.data[start:stop:step]
        return slice

    def __str__(self):
        return "Amp channels, samples: %s\n%s" \
            % (self.data.shape, self.data)

class Segs:
    def __init__(self, segfilename):
        self.load(segfilename)
    
    def load(self, segfilename):
        with open(segfilename) as segfile:
            header =  segfile.readline().strip().split()
            # WARNING: this assumes tmin, text, tmax ordering;
            # tmin, tmax should be in frame numbers!
            types = zip(header, ['i', 'S8', 'i'])
            self.segs = loadtxt(segfile, dtype = types)
    
    def __str__(self):
        header = '\t'.join(self.segs.dtype.names)
        data = '\n'.join(['\t'.join([str(field) for field in line])
                          for line in self.segs.tolist()])
        return "%s\n%s" % (header, data)

if __name__ == '__main__':
    options = OptionParser()
    options.add_option("-a", "--ampfile", dest="ampsname")
    options.add_option("-s", "--segfile", dest="segsname")
    options.set_defaults(ampsname = "test.amp",
                         segsname = "test.seg")
    options, args = options.parse_args()
    
    amps = Amps(options.ampsname)
    segs = Segs(options.segsname)
    for s, seg in enumerate(segs.segs):
        newamps = "amps/%04d.amp" % (s + 1)
        amps.slice(seg['tmin'], seg['tmax']).save(newamps)
