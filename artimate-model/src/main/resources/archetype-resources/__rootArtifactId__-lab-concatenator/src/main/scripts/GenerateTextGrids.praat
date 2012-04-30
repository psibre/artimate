#!/usr/bin/env praat

# arguments
input_directory$ = "${src.lab.directory}"
output_directory$ = "${project.build.outputDirectory}"

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

  # TextGrid handling
  tg_in$ = wav_in$ - "wav" + "TextGrid"
  tg_out$ = "'output_directory$'/" + wav$[w] - "wav" + "TextGrid"
  if ! fileReadable(tg_in$)
    # if there is no TextGrid file, create one for the Sound
    To TextGrid... "phones prompts"
    printline Creating new TextGrid 'tg_out$'
  else
    Read from file... 'tg_in$'
    call ensurePromptTier
    printline Filtering TextGrid 'tg_in$''newline$' -> 'tg_out$'
  endif
  Write to text file... 'tg_out$'

  # cleanup
  plus ls
  Remove
endfor

printline Done

procedure ensurePromptTier
  .numTiers = Get number of tiers
  .hasPromptTier = 0
  for .t to .numTiers
    .tier$ = Get tier name... .t
    if .tier$ == "prompts"
      .hasPromptTier = 1
    endif
  endfor
  if not .hasPromptTier
    Insert interval tier... .numTiers+1 prompts
  endif
endproc
