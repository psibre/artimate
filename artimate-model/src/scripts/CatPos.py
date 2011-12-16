#!/usr/bin/env python

import argparse, glob, os
#import ema

chunksize = 8192

# parse CLI options
parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input-directory", dest="indir",
                    help="Input directory containing .pos files in a pos subdirectory")
parser.add_argument("-o", "--output-file", dest="outfile",
                    help="Output file")
parser.add_argument("-c", "--chunksize", dest="chunksize", type=int, default=chunksize,
                    help="Chunk size for binary I/O (default: %d)" % chunksize)
args = parser.parse_args()

# glob input files to list, exit if none found
posfiles = glob.glob("%s/pos/*.pos" % args.indir)
posfiles.sort()
if not posfiles:
    raise Exception("No pos files found in %s/pos" % args.indir)

# output file
if args.outfile:
    outfilename = args.outfile
else:
    outfilename = "%s/pos/all.pos" % args.indir

# ensure output directory exists
try:
    outfiledir = os.path.dirname(os.path.abspath(outfilename))
    os.makedirs(outfiledir)
except OSError:
    pass

# open output file
outfile = open(outfilename, 'wb')
print("Created %s" % outfilename) 

# append each input file
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

# finish
outfile.close()
print("Done")
