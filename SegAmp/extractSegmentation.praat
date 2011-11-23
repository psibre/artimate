#!/usr/bin/env praat

# globals flags
verbose = 1

# input parameters
form Extract segmentation from TextGrid
	sentence TextGrid_file test.TextGrid
	natural Tier_number 1
	boolean Include_empty_intervals 0
	# padding duration (s)
	positive window 0.5
endform

# expand relative paths
if !fileReadable(textGrid_file$) && !startsWith("/", textGrid_file$)
  textGrid_file$ = "'shellDirectory$'/'textGrid_file$'"
endif

# verbose output
if verbose
  echo Extracting segmentation with the following parameters'newline$'
  ...TextGrid file:           'textGrid_file$''newline$'
  ...Tier number:             'tier_number''newline$'
  ...Include empty intervals? 'include_empty_intervals''newline$'
  ...Window:                  'window' s
endif

# read TextGrid
tg = Read from file... 'textGrid_file$'
# extract tier
tier = Extract one tier... tier_number
# convert to Table
Down to Table... 0 6 0 'include_empy_intervals'
# hack xmin...
Formula... tmin if self - window > Object_'tier'.xmin then self - window else Object_'tier'.xmin fi
# ... and xmax columns to take window into account
Formula... tmax if self + window < Object_'tier'.xmax then self + window else Object_'tier'.xmax fi
# output .seg file name
segFile$ = textGrid_file$ - "TextGrid" + "seg"
# save as text file
Save as tab-separated file... 'segFile$'

if verbose
  printline 'newline$'Saved segmentation to:   'segFile$'
endif

# cleanup
plus tier
plus tg
Remove
