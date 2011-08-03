#!/usr/bin/env python

from numpy import fromfile, loadtxt

TRANS_COILS = 6
SAMP_RATE = 200.0

class Amp:
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
    
    def slice(self, start = 0, stop = -1, step = 1):
        stop = self.data.shape[0] + 1 if stop == -1 else stop
        slice = Amp(channels = self.channels)
        slice.data = self.data.copy()[start:stop:step]
        return slice

    def __str__(self):
        return "Amp channels, samples: %s\n%s" % (self.data.shape, self.data)

class Seg:
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

def extractSegment(amp, seg, index):
    return amp.slice(seg['tmin'][index], seg['tmax'][index])

if __name__ == '__main__':
    ampname = 'test.amp'
    amp = Amp(ampname)
    segfile = Seg('test.Table')
    for s, seg in enumerate(segfile.segs):
        newamp = "amps/%04d.amp" % (s + 1)
        amp.slice(seg['tmin'], seg['tmax']).save(newamp)
