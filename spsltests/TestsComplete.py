import serverRequests as sreq
import time
import utils
import os
import json
import itertools

sreq.quiet = True

######Configuration############
####Change paths of datasets and parameters####
# Datasets to test
dataset2011Training = "D:\Drive\Pamplona\Training Areas\LiDAR_2011"
dataset2017Training = "D:\Drive\Pamplona\Training Areas\LiDAR_2017"
dataset2017500MillPoints = "C:\\Datasets\\Drive\\Pamplona\\Combined5"
dataset20171000MillPoints = "C:\\Datasets\\Drive\\Pamplona\\Combined10"

datasets = []
#datasets.append(dataset2011Training)
#datasets.append(dataset2017Training)
datasets.append(dataset2017500MillPoints)
datasets.append(dataset20171000MillPoints)


# Datablocks max size to use
testsMaxDatablockSizes = []
testsMaxDatablockSizes.append(5000000)
#testsMaxDatablockSizes.append(2500000)
testsMaxDatablockSizes.append(1000000)
#testsMaxDatablockSizes.append(500000)
testsMaxDatablockSizes.append(100000)
#testsMaxDatablockSizes.append(50000)
#testsMaxDatablockSizes.append(10000)

# MaxOctreeSizes
testsMaxOctreeSizes = []
testsMaxOctreeSizes.append(8)

parameters = list(itertools.product(datasets, testsMaxDatablockSizes, testsMaxOctreeSizes))
######################################

global f
workspaceName = "Navarra"
datasetName = "City of Pamplona"


def createProcess(datasetToPut, datablockSize, octreeSize):
    # Reset DB, insert workspace and dataset
    sreq.resetDatabase()
    time.sleep(20)
    global f

    workspace = {
        "name": workspaceName,
        "description": "Workspace with the datasets of Navarra",
        "cellSize": 10000
    }

    dataset = {
        "name": datasetName,
        "description": "Dataset of the city of Pamplona 01",
        "dateOfAcquisition": "2017-01-01T12:00:00Z",
        "boundingBox": {
            "southWestBottom": {
                "easting": "606000",
                "northing": "4736000",
                "zone": "30N"
            },
            "northEastTop": {
                "easting": "614000",
                "northing": " 4744000",
                "zone": "30N"
            }
        },
        "dataBlockSize": datablockSize,
        "dataBlockFormat": ".laz"
    }

    sreq.postWorkspace(workspace)
    sreq.postDataset(workspaceName, dataset)
    sreq.modifyMaxDepthOctree(octreeSize)

    files = utils.loadDirectory(datasetToPut)
    start = time.time()
    sreq.putData(workspaceName, datasetName, files)
    end = time.time()

    write = "Dataset: " + datasetToPut + "\n" + \
            "Max depth: " + str(octreeSize) + "\n" + \
            "Datablock max size: " + str(datablockSize) + "\n" + \
            " time: " + str((end - start)) + "\n"
    f.write(write)
    print(write)


def retrieveProcess():

    global numberOfPointsToSurpass
    global filesDownloaded
    global totalPointsDownloaded
    global totalDownloadRequestTime
    global f

    numberOfPointsToSurpass = 20000000
    filesDownloaded = 0
    totalPointsDownloaded = 0
    totalDownloadRequestTime = 0

    rootDBs = sreq.getDatablock(workspaceName, datasetName, str(0), None)
    jsondata = json.loads(rootDBs)

    for rootDB in jsondata:
        traverseOctree(rootDB)

        if totalPointsDownloaded > numberOfPointsToSurpass:
            writeNumberOfFiles = "Number of files: " + str(filesDownloaded) + "\n"
            f.write(writeNumberOfFiles)
            print(writeNumberOfFiles)

            writeTotalTimeDownload = "Total time in download: " + str(totalDownloadRequestTime) + "\n"
            f.write(writeTotalTimeDownload)
            print(writeTotalTimeDownload)

            writePointsDownload = "Number of points downloaded: " + str(totalPointsDownloaded) + "\n"
            f.write(writePointsDownload)
            print(writePointsDownload)

            writeAvgTimeOfFileDownload = "Average time of file: " + str(
                totalDownloadRequestTime / filesDownloaded) + "\n"
            f.write(writeAvgTimeOfFileDownload)
            print(writeAvgTimeOfFileDownload)

            writeAvgTimeOfPoint = "Average time of point: " + str(
                totalDownloadRequestTime / totalPointsDownloaded) + "\n"
            f.write(writeAvgTimeOfPoint)
            print(writeAvgTimeOfPoint)

            files_in_directory = os.listdir()
            filtered_files = [file for file in files_in_directory if file.endswith(".laz")]
            for file in filtered_files:
                os.remove(file)

            return 0


def traverseOctree(rootDB):
    # Define the local Grid of the nodes
    swBottom = rootDB["cell"]["southWestBottom"]
    neTop = rootDB["cell"]["northEastTop"]
    swBottomStr = swBottom["zone"] + str(swBottom["easting"]) + str(swBottom["northing"])
    neTopStr = neTop["zone"] + str(neTop["easting"]) + str(neTop["northing"])

    dict = {
        "sw_coord": swBottomStr,
        "ne_coord": neTopStr
    }

    return downloadFile(0, dict)


def downloadFile(nextId, dict):
    global numberOfPointsToSurpass
    global filesDownloaded
    global totalPointsDownloaded
    global totalDownloadRequestTime

    text = sreq.getDatablock(workspaceName, datasetName, str(nextId), dict)
    tmpNode = json.loads(text)
    start = time.time()
    sreq.getDatablockFile(workspaceName, datasetName, str(nextId), dict)
    end = time.time()
    totalDownloadRequestTime += (end - start)
    filesDownloaded += 1
    totalPointsDownloaded += int(tmpNode[0]["size"])
    time.sleep(0.1)

    children = tmpNode[0]["children"]

    for child in children:
        if totalPointsDownloaded < numberOfPointsToSurpass:
            downloadFile(child, dict)
        else:
            pass


#### Main loop ####
for parameter in parameters:
    global f
    f = open(str(parameter[0]) + str(parameter[1]) + str(parameter[2]) + ".txt", "a")
    f.write("TEST")

    createProcess(parameter[0], parameter[1], parameter[2])
    time.sleep(10)
    retrieveProcess()

    writeNumberOfDblocksGenerated = "Number of datablocks generated: " + sreq.getOctreeSize(workspaceName,
                                                                                            datasetName) + "\n"
    f.write(writeNumberOfDblocksGenerated)
    print(writeNumberOfDblocksGenerated)

    writeMaxDepthInOctree = "Max depth in octree: " + sreq.getOctreeMaxDepth(workspaceName, datasetName) + "\n"
    f.write(writeMaxDepthInOctree)
    print(writeMaxDepthInOctree)


    f.write("------------------------------------------------\n")
    f.close()
