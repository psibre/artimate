#!/usr/bin/env praat

###
# #%L
# Artimate Model Compiler
# %%
# Copyright (C) 2011 - 2012 INRIA
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as
# published by the Free Software Foundation, either version 3 of the 
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public 
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/gpl-3.0.html>.
# #L%
###

# arguments
form Input directory
  sentence Input_directory
  sentence Output_directory
endform

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

# output files
output_basename$ = "all"
wav_out$ = "'output_directory$'/'output_basename$'.wav"
tg_out$ = "'output_directory$'/'output_basename$'.TextGrid"

# append each input wav file
offset = 0
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

  # TextGrid handling
  tg_in$ = wav_in$ - "wav" + "TextGrid"
  if ! fileReadable(tg_in$)
    # if there is no TextGrid file, create one for the Sound
    tg[w] = To TextGrid... "phones prompts"
  else
    tg[w] = Read from file... 'tg_in$'
    call ensurePromptTier
  endif

  # adjust time domain
  Shift times by... offset
  offset += Object_'ls'.xmax

  # cleanup
  select ls
  Remove
endfor

# merge TextGrids
for w to wav.size
  plus tg[w]
endfor
tg = Merge
Save as chronological text file... 'tg_out$'

# flatten hack
system perl FlattenChronoTextGrid.pl 'tg_out$'
printline Created 'tg_out$'

# extract lab
tg_flat = Read from file... 'tg_out$'
Extract tier... 1
lab_out$ = tg_out$ - "TextGrid" + "lab"
Save as Xwaves label file... 'lab_out$'
printline Created 'lab_out$'

# final cleanup
plus tg
plus tg_flat
for w to wav.size
  plus tg[w]
endfor
Remove
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
