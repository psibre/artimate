#!/usr/bin/env perl

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
