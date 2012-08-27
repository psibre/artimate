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
import re

class Segmentation:
    def __init__(self, lab_file=None):
        try:
            self.segments = self.parse(lab_file)
        except TypeError:
            self.segments = []
    
    def parse(self, lab_file):
        header = True
        segments = []
        start = 0
        for line in lab_file:
            if line.strip() == '#':
                header = False
                continue
            if header:
                continue
            match = re.match(r'\s*(?P<end>\d+(\.\d+)?)\s+\d+\s+(?P<label>.*)\s*', line)
            segment = Segment(start, match.group('end'), match.group('label'))
            segments.append(segment)
            start = match.group('end')
        return segments

    def __str__(self):
        return '\n'.join(['start\tend\tlabel'] + [str(segment) for segment in self.segments])

class Segment:
    def __init__(self, start, end, label):
        self.start = float(start)
        self.end = float(end)
        self.label = label

    def startframe(self):
        # TODO set this from context
        return int(self.start * 200.0)
    startframe = property(startframe)

    def endframe(self):
        # TODO set this from context
        return int(self.end * 200.0)
    endframe = property(endframe)

    def __str__(self):
        return "%s\t%s\t%s" % (self.start, self.end, self.label)
