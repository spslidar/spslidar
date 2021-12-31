# SPSLIDAR
A multi-purpose repository and restful API for tera-scale LIDAR data based on MongoDB. 
### Dependencies
- A Java 11 distribution
- Python 3.8
- LasTools (https://rapidlasso.com/lastools/)
- MongoDB 4.2

Compile and run project using the provided maven pom.xml.

### Tests
Once the project has started, run the test scripts provided in the folder `spsltests`.
Modify the paths to the folder with the LAZ files in the `TestsComplete.py` script. Configure the datasets to be tested in the `datasets` array, the maximum data block sizes in the `testsMaxDatablockSize` array, and the max depth levels of the octrees to be created in the `testsMaxOctreeSizes` array. The insert and query tests are run using all the combinations of parameters in these arrays, showing the stats when they are completed.

The script for the concurrency tests can be installed by running:

```shell
npm loadtests (https://www.npmjs.com/package/loadtest)
```

Run the script as: 

```shell
loadtest [-n requests] [-c concurrency] URL
```


Make sure that there is at least a dataset in the system. The tests are performed using three different datablock values: 10.000, 100.000 and 1.000.000.

**Example**: retrieving 1000 files for 10 concurrent users using the root datablock of one of the octrees (which will have a number of points similar to the maximum datablock value defined):

```shell
loadtest -n 1000 -c 10 http://localhost:8080/spslidar/workspaces/Navarra/datasets/City+of+Pamplona/datablocks/0/data?sw_coord=30N6000004740000&ne_coord=30N6100004750000
```

### Tests datasets

Any LIDAR datasets can be used, but we run our experiments on the following datasets from the publicly available Land Information System of Navarra (SITNA), using IDENA (Spatial Data Infrastructure of Navarra); they can be acessed from https://filescartografia.navarra.es/.

![Pamplona datasets](https://github.com/spslidar/spslidar/blob/main/Distribucion_Tiles_LiDAR_Training_Areas.png?raw=true)

Once the files have been downloaded, a merging operation can be done by using the LasTools:

```shell
lasmerge -i *.laz -o combined.laz
```

The original dataset has around 3.000 million points but reduced versions can be generated using the LasTools. For instance, a dataset of around 500 million points can be obtained by keeping one out of each 6 samples:

```shell
las2las -i combined.laz -o comb534Mill.laz -keep_every_nth 6 
```

Specify a different argument for -keep_every_nth to obtain a different number of points in the reduced dataset.

Finally, some datasets may need to be specified a UTM Zone as a EVLR of the LAS/LAZ file. This can be done by the following command:

```shell
las2las -i comb534Mill.laz -o dataset3 -utm 30N
```

### Authors

- Antonio Jesús Rueda Ruíz - ajrueda@ujaen.es
- Rafael Jesús Segura Sánchez - rsegura@ujaen.es
- Carlos Javier Ogayar Anguita - cogayar@ujaen.es
- Jorge Delgado García - jdelgado@ujaen.es
- Juan Antonio Béjar Martos - jabm0010@red.ujaen.es
