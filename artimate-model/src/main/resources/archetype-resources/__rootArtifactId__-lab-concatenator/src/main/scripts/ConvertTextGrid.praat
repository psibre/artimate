#!/usr/bin/env praat

# arguments
input_TextGrid$ = "${target.textgrid.file}"
output_labFile$ = "${target.lab.file}"

# tier to extract
tier = 1

Read from file... 'input_TextGrid$'
Extract tier... tier
Save as Xwaves label file... 'output_labFile$'

# verbosity
echo 'input_TextGrid$' -> 'output_labFile$'
