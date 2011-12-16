#!/usr/bin/env praat

form Input directory
  sentence Input_directory /Users/steiner/projects/ema/supine_and_upright_noise_and_clear08062011
  sentence Output_directory /Users/steiner/projects/tonguebuilding
endform

output_basename$ = "all"

assert input_directory$ != output_directory$

# glob *.wav to array
list = Create Strings as file list... fileList 'input_directory$'/wav/*.wav
Sort
wav.size = Get number of strings
assert wav.size
for w to wav.size
  wav$[w] = Object_'list'$[w]
endfor
Remove

# mkdir if it doesn't exist
createDirectory("'output_directory$'/wav")

# iterate
offset = 0
for w to wav.size
  wav_in$ = input_directory$ + "/wav/" + wav$[w]

  # memory mapping for wav file
  ls = Open long sound file... 'wav_in$'
  wav_out$ = "'output_directory$'/wav/'output_basename$'.wav"
  if w == 1
    # first file creates new output wav
    Save as WAV file... 'wav_out$'
    echo 'wav_in$' > 'wav_out$'
  else
    # the others append directly
    Append to existing sound file... 'wav_out$'
    printline 'wav_in$' >> 'wav_out$'
  endif

  # TextGrid handling
  tg_in$ = wav_in$ - "wav" + "TextGrid"
  if ! fileReadable(tg_in$)
    # if there is no TextGrid file, create one for the Sound
    tg[w] = To TextGrid... prompts
  else
    tg[w] = Read from file... 'tg_in$'
  endif

  # adjust time domain
  Shift times by... offset
  offset += Object_'ls'.xmax

  # cleanup
  select ls
  Remove
endfor

for w to wav.size
  plus tg[w]
endfor
tg = Merge
tg_out$ = "'output_directory$'/wav/'output_basename$'.TextGrid"
Save as chronological text file... 'tg_out$'

# flatten hack
system perl FlattenChronoTextGrid.pl 'tg_out$'
printline /dev/null > 'tg_out$'

# extract lab
tg_flat = Read from file... 'tg_out$'
Extract tier... 1
lab_out$ = tg_out$ - "TextGrid" + "lab"
Save as Xwaves label file... 'lab_out$'
printline 'tg_out$' > 'lab_out$'

# final cleanup
plus tg
plus tg_flat
for w to wav.size
  plus tg[w]
endfor
Remove
