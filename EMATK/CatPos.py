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

try:
    os.makedirs("%s/pos" % args.outdir)
except OSError:
    pass

outfilename = "%s/pos/all.pos" % args.outdir
outfile = open(outfilename, 'wb')
print("Opened %s for writing" % outfilename) 

for posfile in posfiles:
    ema.Sweep(posfile).save(outfile)
    print("Appended %s" % posfile)

outfile.close()
print("Done")
