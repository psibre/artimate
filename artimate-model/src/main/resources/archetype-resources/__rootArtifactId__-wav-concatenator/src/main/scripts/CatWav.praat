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
