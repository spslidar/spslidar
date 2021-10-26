import pylas
import sys

with pylas.open(sys.argv[1]) as fh:
    print(fh.header.x_min)
    print(fh.header.y_min)
    print(fh.header.z_min)
    print(fh.header.x_max)
    print(fh.header.y_max)
    print(fh.header.z_max)

