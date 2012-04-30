#!/usr/bin/env praat

# arguments
input_directory$ = "${src.wav.directory}"
wav_out$ = "${target.wav.file}"
output_directory$ = left$(wav_out$, rindex(wav_out$, "/"))

# glob input files to array, exit if none found
list = Create Strings as file list... fileList 'input_directory$'/*.wav
Sort
wav.size = Get number of strings
if ! wav.size
  exit No wav files found in 'input_directory$'
endif
for w to wav.size
  wav$[w] = Object_'list'$[w]
endfor
Remove

# append each input wav file
for w to wav.size
  wav_in$ = "'input_directory$'/" + wav$[w]

  # memory mapping for wav file
  ls = Open long sound file... 'wav_in$'
  if w == 1
    # first file creates new output wav
    Save as WAV file... 'wav_out$'
    echo Created 'wav_out$'
  else
    # the others append directly
    Append to existing sound file... 'wav_out$'
  endif
  printline Appended 'wav_in$'

  # cleanup
  Remove
endfor

printline Done
