#!/usr/bin/env perl

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

# read to array
open IN, $ARGV[0];
@lines = <IN>;
close IN;

# time domain
($xmin, $xmax) = split /\s+/, $lines[1];

# number of tiers
($tiers) = split /\s+/, $lines[2];

# iterate over tiers in header
for($t = 0; $t < $tiers; $t++) {
    # could use Text::ParseWords but for now assume tier names contain no spaces
    ($tclass, $tname, $tmin, $tmax) = split /\s+/, $lines[$t+3];
    # append to list of tier names
    $tiers[$t+1] = $tname;
    # insert into tier name lookup dictionary if not already there
    $tiers{$tname} = $t+1 unless exists $tiers{$tname};
}
$headerend = $t+3;

# iterate over intervals
for($i = $headerend; $i < $#lines; $i++) {
    $lines[$i++] =~ s/\d+/$tiers{$tiers[$&]}/;
}

# hack header
@header = splice @lines, 0, $headerend;
# new number of tiers
$ntiers = keys %tiers;
$header[2] =~ s/\d+/$ntiers/;
# strip extraneous tiers
splice @header, 3+$ntiers;
$header[3] =~ s/[\d\.]+\s+[\d\.]+/$xmin $xmax/;

open OUT, ">$ARGV[0]";
print OUT @header, @lines;
close OUT;
