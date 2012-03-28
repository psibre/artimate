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
