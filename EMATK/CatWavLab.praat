clearinfo
form Input directory
  sentence Input_directory /Users/ingmar/projects/ema/supine_and_upright_noise_and_clear08062011
  sentence Output_directory /Users/ingmar/projects/tonguebuilding
endform

output_basename$ = "all"

assert input_directory$ != output_directory$

# glob *.wav to array
list = Create Strings as file list... fileList 'input_directory$'/wav/*.wav
wav.size = Get number of strings
assert wav.size
for w to wav.size
  wav$[w] = Object_'list'$[w]
endfor
Remove

# iterate
createDirectory("'output_directory$'/wav")
xmin = 0
for w to wav.size
  wav_in$ = input_directory$ + "/wav/" + wav$[w]
  ls = Open long sound file... 'wav_in$'
  wav_out$ = "'output_directory$'/wav/'output_basename$'.wav"
  if w == 1
    Save as WAV file... 'wav_out$'
  else
    Append to existing sound file... 'wav_out$'
  endif
  printline 'wav_in$' >> 'wav_out$'
  xmin += Object_'ls'.xmax

  lab_in$ = wav_in$ - "wav" + "lab"
  lab_out$ = wav_out$ - "wav" + "lab"
  delete = 0
  if ! fileReadable(lab_in$)
    tg = To TextGrid... dummy
    it = Extract tier... 1
    Save as Xwaves label file... 'lab_in$'
    lab[w] = Read Strings from raw text file... 'lab_in$'
    select tg
    plus it
    delete = 1
  endif
  Remove

  if w == 1
    lab[w] = Read Strings from raw text file... 'lab_in$'
  else
    tmp = Read Strings from raw text file... 'lab_in$'
    if delete
      deleteFile(lab_in$)
    endif
    numLines = Get number of strings
    for line to numLines
      if index_regex(Object_'tmp'$[line], "^\s*#")
        line = numLines ;; break
      endif
    endfor
    line -= 1
    lab = Extract part... line numLines
    numLines -= line - 1
    for line to numLines
      end = extractNumber(Object_'lab'$[line], "")
      end2 = end + xmin
      line$ = replace$(Object_'lab'$[line], "'end'", "'end2:6'", 1)
      Set string... line 'line$'
    endfor
    lab[w] = lab
    select tmp
    Remove
  endif
endfor

# append labs
for w to wav.size
  plus lab[w]
endfor
lab = Append
Save as raw text file... 'output_directory$'/wav/'output_basename$'.lab

# for debugging, also create TextGrid
it = Read IntervalTier from Xwaves... 'output_directory$'/wav/'output_basename$'.lab
Into TextGrid
Save as text file... 'output_directory$'/wav/'output_basename$'.TextGrid

# final cleanup
plus it
plus lab
for w to wav.size
  plus lab[w]
endfor
Remove
