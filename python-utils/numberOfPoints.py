import pylas
import sys

with pylas.open(sys.argv[1]) as fh:
    print(fh.header.point_count)
