#!/usr/bin/env python

import argparse, glob, os
#import ema

chunksize = 8192

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input-directory", dest="indir",
                    help="Input directory containing .pos files in a pos subdirectory")
parser.add_argument("-o", "--output-directory", dest="outdir",
                    help="Output directory")
parser.add_argument("-c", "--chunksize", dest="chunksize", type=int, default=chunksize,
                    help="Chunk size for binary I/O (default: %d)" % chunksize)
args = parser.parse_args()

posfiles = glob.glob("%s/pos/*.pos" % args.indir)
posfiles.sort()

try:
    os.makedirs("%s/pos" % args.outdir)
except OSError:
    pass

if not args.outdir:
    args.outdir = args.indir
outfilename = "%s/pos/all.pos" % args.outdir
outfile = open(outfilename, 'wb')
print("Opened %s for writing" % outfilename) 

for posfile in posfiles:
    # OO handling:
    #ema.Sweep(posfile).save(outfile)
    # but we just want raw speed here:
    with open(posfile, 'rb') as infile:
        while True:
            chunk = infile.read(args.chunksize)
            if not chunk:
                break
            outfile.write(chunk)
    print("Appended %s" % posfile)

outfile.close()
print("Done")
