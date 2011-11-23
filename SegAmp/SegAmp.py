#!/usr/bin/env python

from optparse import OptionParser, OptionGroup
from numpy import fromfile, loadtxt
import wave
from cStringIO import StringIO
import os.path
from glob import glob

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
        start *= SAMP_RATE
        stop *= SAMP_RATE
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
            # tmin, tmax should be in seconds!
            types = zip(header, ['f', 'S8', 'f'])
            self.segs = loadtxt(segfile, dtype = types)
    
    def __str__(self):
        header = '\t'.join(self.segs.dtype.names)
        data = '\n'.join(['\t'.join([str(field) for field in line])
                          for line in self.segs.tolist()])
        return "%s\n%s" % (header, data)

class Wav:
    def __init__(self, wavfile = None):
        if wavfile is not None:
            self.load(wavfile)

    def load(self, wavfile):
        self.wav = wave.open(wavfile)

    def save(self, wavfile):
        wav = wave.open(wavfile, 'w')
        wav.setparams(self.wav.getparams())
        self.wav.rewind()
        wav.writeframes(self.wav.readframes(self.wav.getnframes()))
        wav.close()

    def slice(self, start = 0, stop = None):
        if stop == None:
            stop = self.wav.getnsamples()
        start *= self.wav.getframerate()
        stop *= self.wav.getframerate()
        io = StringIO()
        wav = wave.open(io, 'w')
        wav.setsampwidth(self.wav.getsampwidth())
        wav.setnchannels(self.wav.getnchannels())
        wav.setframerate(self.wav.getframerate())
        self.wav.setpos(start)
        wav.writeframes(self.wav.readframes(int(stop - start)))
        wav.close()
        io.seek(0)
        slice = Wav(io)
        return slice

def parse_options():
    parser = OptionParser()
    parser.add_option("-a", "--ampfile", dest = "ampsname", metavar = "AMP",
        help = "single input .amp file (default: %default)")
    parser.add_option("-s", "--segfile", dest = "segsname", metavar = "SEG",
        help = "single input .seg file (default: %default)")
    parser.add_option("-w", "--wavfile", dest = "wavname", metavar = "WAV",
        help = "single input .wav file (default: %default)")
    
    group = OptionGroup(parser, "Batch processing")
    group.add_option("-i", "--input-dir", dest = "indir",
        help = "input directory; must contain wav/ and amps/ subdirectories"
            " with matching filesets.")
    group.add_option("-o", "--output-dir", dest = "outdir",
        help = "output directory (will be created if it doesn't exist)")
    parser.add_option_group(group)

    parser.set_defaults(ampsname = "test.amp",
                         segsname = "test.seg",
                         wavname = "test.wav")

    return parser.parse_args()
    
if __name__ == '__main__':
    options, args = parse_options()
    
    if options.indir == None:
        amps = Amps(options.ampsname)
        segs = Segs(options.segsname)
        wav = Wav(options.wavname)
    else:
        # TODO for now, hackily support only a single input sweep
        ampsname = glob(options.indir + "/amps/*.amp")[-1]
        wavbase = "%s/wav/%s" % (options.indir,
            os.path.splitext(os.path.basename(ampsname))[0])
        segsname = wavbase + ".seg"
        wavname = wavbase + ".wav"
        amps = Amps(ampsname)
        segs = Segs(segsname)
        wav = Wav(wavname)
    
    if options.outdir == None:
        ampsdir = os.path.split(os.path.realpath(options.ampsname))[0]
        wavdir = os.path.split(os.path.realpath(options.wavname))[0]
    else:
        ampsdir = options.outdir + "/amps"
        wavdir = options.outdir + "/wav"
        for dir in [ampsdir, wavdir]:
            if not os.path.isdir(dir):
                print "creating", dir
                os.makedirs(dir)
    
    for s, seg in enumerate(segs.segs):
        newamps = "%s/%04d.amp" % (ampsdir, s + 1)
        newwav = "%s/%04d.wav" % (wavdir, s + 1)
        amps.slice(seg['tmin'], seg['tmax']).save(newamps)
        wav.slice(seg['tmin'], seg['tmax']).save(newwav)
        print "wrote", newamps, newwav
