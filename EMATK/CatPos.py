#!/usr/bin/env python

import argparse, ema, glob, os

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input-directory", dest="indir",
                    help="Input directory containing .pos files in a pos subdirectory")
parser.add_argument("-o", "--output-directory", dest="outdir",
                    help="Output directory")
args = parser.parse_args()

posfiles = glob.glob("%s/pos/*.pos" % args.indir)
posfiles.sort()

header = ema.generate_header()
for posfile in posfiles:
    print("Reading %s" % posfile)
    try:
        allpos.extend(ema.Sweep(posfile, header))
    except NameError:
        allpos = ema.Sweep(posfile, header)

try:
    os.makedirs("%s/pos" % args.outdir)
except OSError:
    pass

outfilename = "%s/pos/all.pos" % args.outdir 
print("Writing %s" % outfilename)
allpos.save(outfilename)
print("Done")
