#!/usr/bin/env praat

form Extract segmentation from TextGrid
	sentence TextGrid_file test.TextGrid
	natural Tier_number 1
	boolean Include_empty_intervals 0
	# padding duration (s)
	positive window 0.5
endform

if !fileReadable(textGrid_file$) && !startsWith("/", textGrid_file$)
  textGrid_file$ = "'shellDirectory$'/'textGrid_file$'"
endif

tg = Read from file... 'textGrid_file$'
tier = Extract one tier... tier_number
Down to Table... 0 6 0 'include_empy_intervals'
Formula... tmin if self - window > Object_'tier'.xmin then self - window else Object_'tier'.xmin fi
Formula... tmax if self + window < Object_'tier'.xmax then self + window else Object_'tier'.xmax fi
segFile$ = textGrid_file$ - "TextGrid" + "seg"
Save as tab-separated file... 'segFile$'

plus tier
plus tg
Remove
