#!/usr/bin/env praat

form Extract segmentation from TextGrid
	sentence TextGrid_file test.TextGrid
	natural Tier_number 1
	boolean Include_empty_intervals 0
endform

if !fileReadable(textGrid_file$) && !startsWith("/", textGrid_file$)
  textGrid_file$ = "'shellDirectory$'/'textGrid_file$'"
endif

Read from file... 'textGrid_file$'
tier = Extract one tier... tier_number
Down to Table... 0 6 0 'include_empy_intervals'
Formula... tmin floor(self * 200)
Formula... tmax floor(self * 200)
segFile$ = textGrid_file$ - "TextGrid" + "seg"
Save as tab-separated file... 'segFile$'

plus tier
Remove
